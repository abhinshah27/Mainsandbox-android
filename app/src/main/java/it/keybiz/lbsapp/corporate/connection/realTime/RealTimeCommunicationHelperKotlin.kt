/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.connection.realTime

import android.content.Context
import android.content.IntentFilter
import android.os.Handler
import android.os.Message
import io.realm.Realm
import io.realm.RealmList
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.BaseSingletonWithArgument
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.connection.*
import it.keybiz.lbsapp.corporate.fcm.FCMMessagingService
import it.keybiz.lbsapp.corporate.features.chat.ChatMessagesFragment
import it.keybiz.lbsapp.corporate.features.chat.ChatRoomsFragment
import it.keybiz.lbsapp.corporate.features.chat.HandleChatCreationService
import it.keybiz.lbsapp.corporate.features.chat.RealTimeChatListener
import it.keybiz.lbsapp.corporate.features.timeline.RealTimeCommunicationListener
import it.keybiz.lbsapp.corporate.features.timeline.TimelineFragment
import it.keybiz.lbsapp.corporate.features.timeline.interactions.InteractionsViewerActivity
import it.keybiz.lbsapp.corporate.models.*
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.models.chat.ChatRecipientStatus
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom
import it.keybiz.lbsapp.corporate.utilities.*
import it.keybiz.lbsapp.corporate.utilities.helpers.WebLinkRecognizer
import it.keybiz.lbsapp.corporate.utilities.media.BaseSoundPoolManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * @author mbaldrighi on 9/4/2018.
 */
class RealTimeCommunicationHelperKotlin private constructor(private var mContext: Context) :
        OnServerMessageReceivedListener, OnMissingConnectionListener, Handler.Callback {

    private val chatSoundPool by lazy {
        BaseSoundPoolManager(mContext, arrayOf(R.raw.chat_incoming_message), null)
    }

    var listener: RealTimeCommunicationListener? = null
    var listenerChat: RealTimeChatListener? = null
    val serverMessageReceiver: ServerMessageReceiver = ServerMessageReceiver()

    private var mRealm: Realm? = null

    private val mHandler = Handler(this)

    var hasInsert = false

    init {
        mRealm = RealmUtils.getCheckedRealm()

        chatSoundPool.init()

        serverMessageReceiver.setListener(this)
        registerReceiver()
    }

    companion object :
            BaseSingletonWithArgument<RealTimeCommunicationHelperKotlin, Context>(::RealTimeCommunicationHelperKotlin) {
        val LOG_TAG = RealTimeCommunicationHelperKotlin::class.qualifiedName
    }


    //region == Class custom methods ==

    //region == RTCOM methods ==

    private fun onPostAdded(json: JSONObject?) {
        if (json != null) {
            val pid = json.optString("_id")
            if (Utils.isStringValid(pid)) {
                val posts = HLPosts.getInstance()
                posts.setPost(json, mRealm, true)

                if (listener != null && listener is TimelineFragment) {
                    val p = posts.getPost(pid)
                    var position = posts.visiblePosts.indexOf(p)
                    if (position == -1) position = 0
                    listener!!.onPostAdded(p, position)
                }
            }
        }
    }

    private fun onPostUpdated(json: JSONObject?) {
        if (json != null) {
            val pid = json.optString("_id")
            if (Utils.isStringValid(pid)) {
                val posts = HLPosts.getInstance()
                posts.setPost(json, mRealm, posts.isPostToBePersisted(pid))

                if (listener != null && listener is TimelineFragment) {
                    val p = posts.getPost(pid)
                    val position = posts.visiblePosts.indexOf(p)
                    if (position != -1)
                        listener!!.onPostUpdated(pid, position)
                }
            }
        }
    }

    private fun onPostUpdatedInteractions(json: JSONObject?, operationId: Int) {
        if (json != null) {
            val pid = json.optString("postID")
            if (Utils.isStringValid(pid)) {
                val posts = HLPosts.getInstance()
                val p = posts.getPost(pid)
                if (p != null) {
                    when (operationId) {
                        Constants.SERVER_OP_RT_UPDATE_HEARTS -> {
                            val heart = InteractionHeart().returnUpdatedInteraction(json)
                            if (!Utils.isStringValid(heart.id))
                                return
                            if (p.interactionsHeartsPost == null)
                                p.interactionsHeartsPost = RealmList()
                            val ih = posts.getHeartInteractionById(pid, heart.id)
                            ih?.updateForRealTime(heart) ?: p.interactionsHeartsPost.add(heart)
                            p.countHeartsUser = p.countHeartsUser + heart.count!!
                            p.countHeartsPost = p.countHeartsPost + heart.count!!
                        }

                        Constants.SERVER_OP_RT_NEW_COMMENT -> {
                            val comment = InteractionComment().returnUpdatedInteraction(json)
                            if (!Utils.isStringValid(comment.id))
                                return
                            if (p.interactionsComments == null)
                                p.interactionsComments = RealmList()
                            val oldComment = posts.getCommentInteractionById(pid, comment.id)
                            if (oldComment == null) {
                                p.interactionsComments.add(comment)
                                p.countComments = p.countComments + 1
                            }
                        }

                        Constants.SERVER_OP_RT_UPDATE_COMMENTS -> {
                            val comment1 = InteractionComment().returnUpdatedInteraction(json)
                            if (!Utils.isStringValid(comment1.id))
                                return
                            if (p.interactionsComments == null)
                                p.interactionsComments = RealmList()
                            val oldComment1 = posts.getCommentInteractionById(pid, comment1.id)
                            oldComment1?.update(comment1)
                            if (!comment1.isVisible) {
                                p.countComments = p.countComments - 1
                                p.isYouLeftComments = p.checkYouLeftComments(HLUser().readUser(mRealm)?.id)
                            }
                        }

                        Constants.SERVER_OP_RT_NEW_SHARE -> {
                            val share = InteractionShare().returnUpdatedInteraction(json)
                            if (!Utils.isStringValid(share.id))
                                return
                            if (p.interactionsShares == null)
                                p.interactionsShares = RealmList()
                            p.interactionsShares.add(share)
                            p.countShares = p.countShares + 1
                        }

                        Constants.SERVER_OP_RT_UPDATE_TAGS -> {
                            val tag = Tag().returnUpdatedTag(json)
                            if (!Utils.isStringValid(tag.id))
                                return
                            if (p.tags == null)
                                p.tags = RealmList()
                            p.tags.add(tag)
                        }

                        Constants.SERVER_OP_RT_EDIT_POST -> {
                            val post = Post().returnUpdatedPost(json)
                            if (!Utils.isStringValid(pid))
                                return
                            val oldPost = posts.getPost(pid)
                            oldPost.update(post)
                        }
                    }

                    posts.setPost(p, mRealm, posts.isPostToBePersisted(pid))

                    if (listener != null) {
                        if (listener is TimelineFragment) {
                            val position = posts.visiblePosts.indexOf(p)
                            if (position != -1)
                                listener!!.onPostUpdated(pid, position)
                        }
                        else if (listener is InteractionsViewerActivity) {
                            listener!!.onPostUpdated(pid, -1)
                        }
                    }
                }
            }
        }
    }

    private fun onPostDeleted(json: JSONObject?) {
        if (json != null) {
            val pid = json.optString("_id")
            if (Utils.isStringValid(pid)) {
                val posts = HLPosts.getInstance()

                if (listener != null && listener is TimelineFragment) {
                    val p = posts.getPost(pid)
                    if (p != null) {
                        val position = posts.visiblePosts.indexOf(p)
                        if (position != -1)
                            listener!!.onPostDeleted(position)
                    }
                }

                posts.deletePost(pid, mRealm, true)
            }
        }
    }

    private fun onDataPushed(jsonArray: JSONArray?) {
        if (jsonArray != null)
            HandlePushedDataService.startService(mContext, mHandler, jsonArray.toString())
    }

    //endregion

    //region == CHAT methods ==

    private fun onNewMessage(json: JSONObject?) {
        if (json != null) {

            var room: ChatRoom? = null
            val message = ChatMessage.getMessage(json)
            message.deliveryDateObj = Date()
            mRealm?.executeTransaction {
                it.insertOrUpdate(message)

                // start web link recognition
                WebLinkRecognizer.getInstance(mContext).recognize(message.text, message.messageID)

                val roomPair = ChatRoom.checkRoom(message.chatRoomID!!, it)
                if (roomPair.first) {
                    room = roomPair.second
                    // increase by one the unread messages count
                    room?.toRead = room!!.toRead + 1
                }
                else {
                    val ids = RealmList<String>()
                    ids.add(message.senderID)
                    room = ChatRoom(
                            HLUser().readUser(it).userId,
                            RealmList<String>().apply { this.add(message.senderID) },
                            message.chatRoomID
                    )

                    HandleChatCreationService.startService(mContext, room, it)
                }
            }

            callSetNewMessageDelivered(message.chatRoomID!!)

            if (listenerChat != null) {
                when (listenerChat) {
                    is ChatRoomsFragment -> {
                        listenerChat!!.onNewMessage(message)

                        if (LBSLinkApp.canVibrate) vibrateForChat(mContext)
                        chatSoundPool.playOnce(R.raw.chat_incoming_message)
                    }
                    is ChatMessagesFragment -> {
                        listenerChat!!.onNewMessage(message)

                        if ((listenerChat as ChatMessagesFragment).participantId != message.senderID) {
                            FCMMessagingService.createChatChannelAndSendNotification(
                                    mContext,
                                    Constants.NOTIFICATION_CHAT_ID,
                                    "${room!!.getRoomName()}: ${message.text}",
                                    null
                            )
                        }
                        else {
                            if (LBSLinkApp.canVibrate) vibrateForChat(mContext)
                            chatSoundPool.playOnce(R.raw.chat_incoming_message)
                        }
                    }
                }
            } else {
                FCMMessagingService.createChatChannelAndSendNotification(
                        mContext,
                        Constants.NOTIFICATION_CHAT_ID,
                        "${room!!.getRoomName()}: ${message.text}",
                        null
                )
            }
        }
    }

    private fun onStatusUpdated(json: JSONObject?) {
        if (json != null) {

            val userId = json.optString("userID", "")
            val status = json.optInt("status", ChatRecipientStatus.OFFLINE.value)
            val date = json.optString("date", "")

            val user = RealmUtils.readFirstFromRealmWithId(mRealm, HLUserGeneric::class.java, "id", userId) as? HLUserGeneric
            mRealm?.executeTransaction {
                user?.chatStatus = status
                user?.lastSeenDate = date
            }

            if (listenerChat != null && Utils.areStringsValid(userId)) {
                when (listenerChat) {
                    is ChatMessagesFragment,
                    is ChatRoomsFragment -> {
                        listenerChat!!.onStatusUpdated(userId, status, date)
                    }
                }
            }
        }
    }

    private fun onActivityUpdated(json: JSONObject?) {
        if (json != null) {

            val userId = json.optString("userID", "")
            val chatId = json.optString("chatID", "")
            val activity = json.optString("activity", "")

            if (listenerChat != null && Utils.areStringsValid(userId, chatId)) {
                when (listenerChat) {
                    is ChatMessagesFragment,
                    is ChatRoomsFragment -> {
                        listenerChat!!.onActivityUpdated(userId, chatId, activity)
                    }
                }
            }
        }
    }

    private fun onMessageDeliveredOrRead(json: JSONObject?, read: Boolean = false) {
        if (json != null) {

            val chatId = json.optString("chatRoomID", "")
            val userId = json.optString("userID", "")
            val date = json.optString("date", "")

            val id = HLUser().readUser(mRealm)?.userId
            if (!id.isNullOrBlank()) {
                val messages = mRealm?.where(ChatMessage::class.java)
                        ?.equalTo("chatRoomID", chatId)
                        ?.and()
                        ?.equalTo("senderID", id)
                        ?.findAll()

                if (messages?.isNotEmpty() == true) {
                    mRealm?.executeTransaction {
                        for (message in messages) {
                            with(Utils.getDateFromDB(date)) {
                                if (message.getStatusEnum() != ChatMessage.Status.ERROR &&
                                        message.getStatusEnum() != ChatMessage.Status.SENDING) {
                                    if (read) {
                                        if (!message.isRead()) message.readDateObj = this
                                    } else {
                                        if (!message.isDelivered()) message.deliveryDateObj = this
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (listenerChat != null && Utils.areStringsValid(userId, chatId, date)) {
                when (listenerChat) {
                    is ChatMessagesFragment,
                    is ChatRoomsFragment -> {
                        if (read) listenerChat!!.onMessageRead(chatId, userId, date)
                        else listenerChat!!.onMessageDelivered(chatId, userId, date)
                    }
                }
            }
        }
    }

    private fun onDocumentOpened(json: JSONObject?) {
        if (json != null) {

            val chatId = json.optString("chatRoomID", "")
            val userId = json.optString("userID", "")
            val date = json.optString("date", "")
            val messageID = json.optString("messageID", "")

            val message = RealmUtils.readFirstFromRealmWithId(mRealm, ChatMessage::class.java, "messageID", messageID) as? ChatMessage
            if (message != null) {
                mRealm?.executeTransaction {
                    if (message.getStatusEnum() != ChatMessage.Status.ERROR &&
                            message.getStatusEnum() != ChatMessage.Status.SENDING &&
                            !message.isOpened()) {
                        message.openedDateObj = Utils.getDateFromDB(date)
                    }
                }
            }

            if (listenerChat != null && Utils.areStringsValid(userId, chatId, date)) {
                when (listenerChat) {
                    is ChatMessagesFragment,
                    is ChatRoomsFragment -> {
                        listenerChat!!.onMessageRead(chatId, userId, date)
                    }
                }
            }
        }
    }

    //endregion


    fun registerReceiver() {
        if (Utils.isContextValid(mContext))
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(mContext).registerReceiver(serverMessageReceiver, IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION))
    }

    fun unregisterReceiver() {
        if (Utils.isContextValid(mContext))
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(mContext).unregisterReceiver(serverMessageReceiver)
    }

    fun restoreRealmInstance() {
        mRealm = RealmUtils.getCheckedRealm()
    }

    fun closeRealmInstance() {
        RealmUtils.closeRealm(mRealm)
    }

    //endregion


    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        if (responseObject == null || responseObject.length() == 0) return

        try {
            val json = responseObject.optJSONObject(0)

            when (operationId) {

                /* RTCOM */
                Constants.SERVER_OP_RT_NEW_POST, Constants.SERVER_OP_RT_NEW_SHARE -> if (json != null) onPostAdded(json)
                Constants.SERVER_OP_RT_DELETE_POST -> if (json != null) onPostDeleted(json)
                Constants.SERVER_OP_RT_UPDATE_POST, Constants.SERVER_OP_RT_EDIT_POST -> if (json != null) onPostUpdated(json)
                Constants.SERVER_OP_RT_UPDATE_HEARTS, Constants.SERVER_OP_RT_UPDATE_SHARES,
                Constants.SERVER_OP_RT_UPDATE_TAGS, Constants.SERVER_OP_RT_UPDATE_COMMENTS,
                Constants.SERVER_OP_RT_NEW_COMMENT -> if (json != null) onPostUpdatedInteractions(json, operationId)
                Constants.SERVER_OP_RT_PUSH_DATA -> onDataPushed(responseObject)

                /* CHAT*/
                Constants.SERVER_OP_RT_CHAT_DELIVERY -> onNewMessage(json)
                Constants.SERVER_OP_RT_CHAT_UPDATE_STATUS -> onStatusUpdated(json)
                Constants.SERVER_OP_RT_CHAT_UPDATE_ACTIVITY -> onActivityUpdated(json)
                Constants.SERVER_OP_RT_CHAT_MESSAGE_DELIVERED -> onMessageDeliveredOrRead(json)
                Constants.SERVER_OP_RT_CHAT_MESSAGE_READ -> onMessageDeliveredOrRead(json, true)
                Constants.SERVER_OP_RT_CHAT_DOCUMENT_OPENED -> onDocumentOpened(json)
            }
        }
        catch (e: JSONException) {
            LogUtils.e(LOG_TAG, e.message, e)

//            if (listener is HLActivity)
//                (listener as HLActivity).showGenericError()
//            else if (listenerChat is HLActivity)
//                (listenerChat as HLActivity).showGenericError()
//            else if (listener is HLFragment && Utils.isContextValid((listener as HLFragment).activity)) {
//                if ((listener as HLFragment).activity is HLActivity)
//                    ((listener as HLFragment).activity as HLActivity).showGenericError()
//            }
//            else if (listenerChat is HLFragment && Utils.isContextValid((listenerChat as HLFragment).activity)) {
//                if ((listenerChat as HLFragment).activity is HLActivity)
//                    ((listenerChat as HLFragment).activity as HLActivity).showGenericError()
//            }
        }
    }


    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        LogUtils.e(LOG_TAG, "Real-Time communication ERROR for operation : " + operationId +
                " with code: " + errorCode)

//        if (listener is HLActivity)
//            (listener as HLActivity).showGenericError()
//        else if (listenerChat is HLActivity)
//            (listenerChat as HLActivity).showGenericError()
//        else if (listener is HLFragment && Utils.isContextValid((listener as HLFragment).activity)) {
//            if ((listener as HLFragment).activity is HLActivity)
//                ((listener as HLFragment).activity as HLActivity).showGenericError()
//        }
//        else if (listenerChat is HLFragment && Utils.isContextValid((listenerChat as HLFragment).activity)) {
//            if ((listenerChat as HLFragment).activity is HLActivity)
//                ((listenerChat as HLFragment).activity as HLActivity).showGenericError()
//        }
    }

    override fun onMissingConnection(operationId: Int) {}


    override fun handleMessage(msg: Message?): Boolean {

        HLPosts.getInstance().cleanRealmPosts(mRealm, mContext)

        callSetDataRead()

        hasInsert = msg?.what == 1

        if (listener != null && listener is TimelineFragment) {
            (listener as TimelineFragment).onNewDataPushed(hasInsert)
            return true
        }

        return false
    }

    private fun callSetDataRead() {
        val id = HLUser().readUser(mRealm)?.id

        var result: Array<Any>? = null

        try {
            result = HLServerCalls.setRTDataRead(id)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (mContext is HLActivity)
            HLRequestTracker.getInstance((mContext as HLActivity).application as LBSLinkApp)
                    .handleCallResult(this, mContext as HLActivity?, result)
    }

    private fun callSetNewMessageDelivered(chatRoomId: String) {
        var result: Array<Any?>? = null

        val id: String? = HLUser().readUser(mRealm)?.userId
        if (!id.isNullOrBlank()) {
            try {
                result = HLServerCallsChat.sendChatHack(HLServerCallsChat.HackType.DELIVERY, id, chatRoomId)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        if (mContext is HLActivity)
            HLRequestTracker.getInstance((mContext as HLActivity).application as LBSLinkApp)
                    .handleCallResult(this, mContext as HLActivity?, result, true, false)
    }

}
