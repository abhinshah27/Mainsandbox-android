/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import im.ene.toro.PlayerSelector
import im.ene.toro.ToroPlayer
import im.ene.toro.widget.Container
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.isManaged
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.*
import it.keybiz.lbsapp.corporate.connection.*
import it.keybiz.lbsapp.corporate.connection.realTime.RealTimeCommunicationHelperKotlin
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.NotificationUtils
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp.VoiceVideoCallType
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.models.chat.ChatMessageType
import it.keybiz.lbsapp.corporate.models.chat.ChatRecipientStatus
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom
import it.keybiz.lbsapp.corporate.utilities.*
import it.keybiz.lbsapp.corporate.utilities.helpers.ShareHelper
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper
import kotlinx.android.synthetic.main.fragment_chat_messages_toolbar.*
import kotlinx.android.synthetic.main.luiss_toolbar_chat.view.*
import org.json.JSONArray
import java.util.*

/**
 * @author mbaldrighi on 10/15/2018.
 */
class ChatMessagesFragment: HLFragment(), OnBackPressedListener, OnServerMessageReceivedListener,
        OnMissingConnectionListener, RealTimeChatListener, ShareHelper.ShareableProvider {

    companion object {

        val LOG_TAG = ChatMessagesFragment::class.qualifiedName

        fun newInstance(chatRoomId: String, participantName: String?, participantAvatar: String?,
                        canFetchMessages: Boolean = false, finishActivity: Boolean = false):
                ChatMessagesFragment {
            val fragment = ChatMessagesFragment()
            fragment.arguments = Bundle().apply {
                this.putString(Constants.EXTRA_PARAM_1, chatRoomId)
                this.putString(Constants.EXTRA_PARAM_2, participantName)
                this.putString(Constants.EXTRA_PARAM_3, participantAvatar)
                this.putBoolean(Constants.EXTRA_PARAM_4, canFetchMessages)
                this.putBoolean(Constants.EXTRA_PARAM_5, finishActivity)
            }

            return fragment
        }
    }

    private lateinit var adapter: ChatMessagesAdapter
    private var llm: LinearLayoutManager? = null
    private var chatHelper: ChatMessagesInteractionsHelper? = null


    private var scrollListener: LoadMoreScrollListener? = null
    private var scrollAnimationListener: OnToolbarScrollListener? = null

    private var fromLoadMore = false
    private var newItemsCount = 0


    private var chatRoom: ChatRoom? = null
    private var chatRoomId: String? = null
    private var participantName: String? = null
    private var canFetchMessages: Boolean = false
    private var finishActivity: Boolean = false
    var participantId: String? = null                   // public because access needed when RT is received¡
    var participantAvatar: String? = null

    private var isTyping: Boolean = false

    private val sendingMessagesIDs = mutableListOf<String>()    // support list for message error handling

    private val toolbarHeight by lazy { resources.getDimensionPixelSize(R.dimen.toolbar_height) }
    private var actionsBarY = 0
    private var isScrollIdle = true
    private var stopAnimating = false

    var lastScrollPosition: Int? = null

    val shareHelper by lazy { ShareHelper(context!!, this) }
    var currentMessageId: String? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        onRestoreInstanceState(savedInstanceState ?: arguments)

        return inflater.inflate(R.layout.fragment_chat_messages_toolbar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messages = (RealmUtils.readFromRealmWithIdSorted(realm, ChatMessage::class.java, "chatRoomID", chatRoomId ?: "", "creationDateObj", Sort.DESCENDING) as RealmResults<ChatMessage>)
        adapter = ChatMessagesAdapter(view.context, messages, this)

        chatHelper = ChatMessagesInteractionsHelper(view, this)
        configureLayout(view)
    }

    override fun onStart() {
        super.onStart()

        configureResponseReceiver()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(context, AnalyticsUtils.CHAT_MESSAGES)

        if (Utils.isContextValid(context))
            RealTimeCommunicationHelperKotlin.getInstance(context!!).listenerChat = this@ChatMessagesFragment

        (activity as? ChatActivity)?.backListener = this
        setLayout()

        callServer(CallType.UPDATE_CHAT)
        callServer(CallType.SET_READ)

        setMessages()

        shareHelper.onResume()
    }

    override fun onPause() {

        if (Utils.isContextValid(context))
            RealTimeCommunicationHelperKotlin.getInstance(context!!).listenerChat = null

        chatHelper?.onPause()

//        lastScrollPosition = llm?.findLastVisibleItemPosition()

        if (!Utils.hasNougat()) cleanAdapterMediaControllers()
        Utils.closeKeyboard(messageBox)

        super.onPause()
    }

    override fun onStop() {
        if (Utils.hasNougat()) cleanAdapterMediaControllers()

        shareHelper.onStop()

        super.onStop()
    }

    override fun onBackPressed() {
        if (chatHelper?.isSheetOpen() == true)
            chatHelper?.closeSheet()
        else {
            if (finishActivity)
                activity?.finish()
            else {
                (activity as? ChatActivity)?.backListener = null
                activity?.onBackPressed()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        chatHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.RESULT_FULL_VIEW_VIDEO) {
            val playBackInfo = data?.getLongExtra(Constants.EXTRA_PARAM_1, 0L) ?: 0
            adapter.videoView?.player?.seekTo(playBackInfo)
        }
        else chatHelper?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        participantName = savedInstanceState?.getString(Constants.EXTRA_PARAM_2)
        participantAvatar = savedInstanceState?.getString(Constants.EXTRA_PARAM_3)
        canFetchMessages = savedInstanceState?.getBoolean(Constants.EXTRA_PARAM_4, false) ?: false
        finishActivity = savedInstanceState?.getBoolean(Constants.EXTRA_PARAM_5, false) ?: false

        chatRoomId = savedInstanceState?.getString(Constants.EXTRA_PARAM_1, "")
        chatRoom = RealmUtils.readFirstFromRealmWithId(realm, ChatRoom::class.java, "chatRoomID", chatRoomId!!) as ChatRoom?
        participantId = chatRoom?.participantIDs?.get(0) ?: ""
    }

    override fun configureResponseReceiver() {
        if (serverMessageReceiver == null)
            serverMessageReceiver = ServerMessageReceiver()
        serverMessageReceiver.setListener(this)
    }

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        if (operationId == Constants.SERVER_OP_CHAT_FETCH_MESSAGES || operationId == Constants.SERVER_OP_CHAT_UPDATE_LIST) {
            when {
                responseObject == null -> return
                responseObject.length() == 0 -> {
                    if (operationId == Constants.SERVER_OP_CHAT_FETCH_MESSAGES)
                        scrollListener?.canFetch = false
                    else if (operationId == Constants.SERVER_OP_CHAT_UPDATE_LIST && Utils.isContextValid(activity)) {
                        // if rooms list is empty send error and exit
                        activityListener.showGenericError()
//                        activityListener.showAlert(R.string.error_generic_unexpected)
                        activity!!.finish()
                    }
                }
                else -> {
                    if (operationId == Constants.SERVER_OP_CHAT_FETCH_MESSAGES) {
                        newItemsCount = responseObject.length()
                        scrollListener?.canFetch = (newItemsCount == Constants.PAGINATION_AMOUNT)

                        // it seems that for now the server doesn't handle pagination, but this is valid fo first fetch as well
                        realm.executeTransactionAsync(
                                { realm ->
                                    for (i in 0 until responseObject.length()) {
                                        val j = responseObject.optJSONObject(i)
                                        if (j != null && j.length() > 0) {
                                            val message = ChatMessage.getMessage(j)
                                            realm.insertOrUpdate(message)
                                        }
                                    }
                                }
//                                , { restoreScrollPosition() }, { restoreScrollPosition() }
                        )
                        fromLoadMore = false
                    }
                    else if (operationId == Constants.SERVER_OP_CHAT_UPDATE_LIST && Utils.isContextValid(activity)) {
                        realm.executeTransactionAsync { realm ->
                            for (i in 0 until responseObject.length()) {
                                val j = responseObject.optJSONObject(i)
                                if (j != null && j.length() > 0) {
                                    val room = ChatRoom.getRoom(j)
                                    room.ownerID = HLUser().readUser(realm).userId
                                    realm.insertOrUpdate(room)

                                    if (room.chatRoomID == chatRoomId) {
                                        this@ChatMessagesFragment.activity?.runOnUiThread {
                                            setStatusIndicatorAndString(room.recipientStatus, room.date)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return
        }

        if (responseObject == null || responseObject.length() == 0 || responseObject.optJSONObject(0) == null) {
            handleErrorResponse(operationId, Constants.SERVER_ERROR_GENERIC); return
        }

        val jsonObj = responseObject.optJSONObject(0)
        when (operationId) {
            Constants.SERVER_OP_CHAT_SEND_MESSAGE -> {
                val id = jsonObj?.optString("messageID")
                val unixTS = jsonObj?.optLong("unixtimestamp")
                if (!id.isNullOrBlank()) {

                    // remove processed messageID
                    sendingMessagesIDs.remove(id)

                    val pair = adapter.getMessageByID(id)

                    realm.executeTransaction {
                        val message = pair.second
                        message?.unixtimestamp = unixTS
                        message?.sentDateObj = Date(unixTS!!)
                    }
                }
            }

            Constants.SERVER_OP_CHAT_SET_MESSAGES_READ -> {
                chatRoom = getValidRoom()
                realm.executeTransaction { chatRoom?.toRead = 0 }
                handleToReadValue()
                LogUtils.d (LOG_TAG, "All messages read for ChatRoom (id: ${chatRoomId}")
            }
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        super.handleErrorResponse(operationId, errorCode)

        when (operationId) {
            Constants.SERVER_OP_CHAT_SEND_MESSAGE -> {
                // TODO: 10/29/2018    build some retry logic + some UI reaction/interaction
            }

            Constants.SERVER_OP_CHAT_SET_MESSAGES_READ -> LogUtils.d(LOG_TAG, "Error setting all messages read for ChatRoom (id: $chatRoomId")
        }
    }

    override fun onMissingConnection(operationId: Int) {
        // TODO: 10/29/2018    build some retry logic + some UI reaction/interaction
    }

    override fun configureLayout(view: View) {

        toolbar.backArrow.setOnClickListener { onBackPressed() }

        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        actionsBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val lp = recordOverlay.layoutParams
                lp.height = actionsBar.height
                recordOverlay.layoutParams = lp

                val lp1 = disabledOverlay.layoutParams
                lp1.height = actionsBar.height
                disabledOverlay.layoutParams = lp1

                actionsBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        actionsBar.viewTreeObserver.addOnGlobalLayoutListener {
            val coords = intArrayOf(0, 0)
            actionsBar.getLocationOnScreen(coords)

            if (coords[1] < actionsBarY) {
                stopAnimating = true
                if (toolbarHeight > 0 && isScrollIdle)
                    hideToolbar()
            }
            else if (coords[1] > actionsBarY) stopAnimating = false

            actionsBarY = coords[1]
        }

        with(messageBox) {
            this.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 0) {
                        this@ChatMessagesFragment.btnSend.visibility = View.GONE
                        this@ChatMessagesFragment.containerActionsButtons.visibility = View.VISIBLE

                        if (isTyping) {
                            isTyping = false
                            callServer(type = CallType.UPDATE_ACTIVITY)
                        }
                    }
                    else {
                        this@ChatMessagesFragment.containerActionsButtons.visibility = View.GONE
                        this@ChatMessagesFragment.btnSend.visibility = View.VISIBLE

                        if (!isTyping) {
                            isTyping = true
                            callServer(type = CallType.UPDATE_ACTIVITY, userActivity = context.getString(R.string.chat_activity_typing))
                        }
                    }
                }
            })
            this.setOnClickListener {
                val first = llm?.findFirstVisibleItemPosition()
                if (first == 0) {
                    restoreScrollPosition(delay = 200)
                }
            }
        }
        btnSend.setOnClickListener {
            chatRoom = getValidRoom()
            if (!participantId.isNullOrBlank() && chatRoom?.isValid() == true) {

                (activity as ChatActivity).playSendTone()
                if (LBSLinkApp.canVibrate) vibrateForChat(context)

                addNewMessage(
                        ChatMessage().getNewOutgoingMessage(
                                mUser.userId,
                                participantId ?: "",
                                chatRoomId!!,
                                messageBox.text.toString()
                        )
                )
            }
        }
        btnVoice.setOnClickListener { callParticipant(VoiceVideoCallType.VOICE) }
        btnVideo.setOnClickListener { callParticipant(VoiceVideoCallType.VIDEO) }

        toBottomBtn.setOnClickListener { restoreScrollPosition() }
        toBottomBtn.hide()

        if (scrollListener == null)
            scrollListener = object: LoadMoreScrollListener(LoadMoreScrollListener.Type.CHAT) {
                override fun onLoadMore() {
                    fromLoadMore = true
                    callServer(CallType.FETCH_MESSAGES)
                }
            }
        if (scrollAnimationListener == null)
            scrollAnimationListener = OnToolbarScrollListener(toolbar, isScrollIdle)
        containerChat.addOnScrollListener(scrollListener as LoadMoreScrollListener)
        containerChat.addOnScrollListener(scrollAnimationListener as OnToolbarScrollListener)
        containerChat.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val firstVisibleItem = llm?.findFirstVisibleItemPosition()

                when {
                    dy < 0 && firstVisibleItem != null && firstVisibleItem >= 1 -> {
                        if (this@ChatMessagesFragment.toBottomBtn.isOrWillBeHidden)
                            this@ChatMessagesFragment.toBottomBtn.show()
                    }
                    dy > 0 && firstVisibleItem == 0 -> {
                        if (this@ChatMessagesFragment.toBottomBtn.isOrWillBeShown)
                            this@ChatMessagesFragment.toBottomBtn.hide()
                    }
                }
            }
        })
//        containerChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
//            if (bottom < oldBottom) restoreScrollPosition()
//        }
        if (containerChat is Container) {
            containerChat.cacheManager = adapter
            containerChat.setPlayerDispatcher { 250 }
            containerChat.playerSelector = if (isAutoPlayOn()) PlayerSelector.DEFAULT else {
                object: PlayerSelector {
                    val manualPlay = Container.Filter {
                        it.wantsToPlay() && it.playerOrder == adapter.manualItem.get()
                    }

                    override fun reverse(): PlayerSelector {
                        return this
                    }

                    override fun select(container: Container, items: MutableList<ToroPlayer>): MutableCollection<ToroPlayer> {
                        return container.filterBy(manualPlay)
                    }
                }
            }
        }

        disabledOverlay.setOnClickListener {
            //do nothing because if visible it is a click catcher
        }

//        setMessages()

        llm = null                                                                      // resets LinearLayoutManager
        llm = LinearLayoutManager(context, RecyclerView.VERTICAL, true)    // new instance
        llm!!.stackFromEnd = true                                                       //
        if (containerChat.layoutManager == null) containerChat.layoutManager = llm      // assigns new instance

        restoreScrollPosition()
    }


    override fun setLayout() {
        chatRoom = getValidRoom()

        disabledOverlay.visibility = if (chatRoom?.getParticipant()?.canChat() == true) { View.GONE } else View.VISIBLE

        frame.requestFocus()

        restoreScrollPosition(false)

        handleToReadValue()

        containerChat.adapter = adapter

        MediaHelper.loadProfilePictureWithPlaceholder(context, participantAvatar, profilePicture as ImageView)
        userName.text = participantName
        setStatusIndicatorAndString(
                chatRoom?.recipientStatus ?: ChatRecipientStatus.OFFLINE.value,
                dateObj = chatRoom?.getLastSeenDate(participantId)
        )

        // INFO: 2/11/19    SERVER FORCED BOOL to FALSE
        with (btnVideo) {
            if (Utils.hasDeviceCamera(context) && chatRoom?.canVideoCallParticipant() == true) this.show()
            else this.hide()
        }

        if (chatRoom?.canVoiceCallParticipant() == true) btnVoice.show() else btnVoice.hide()
    }


    //region == Class custom methods ==

    private fun restoreScrollPosition(forceToBottom: Boolean = true, delay: Long = 0) {
        val runnable = {
            if (adapter.itemCount > 0) {
                this@ChatMessagesFragment.containerChat?.scrollToPosition(
                        when {
                            forceToBottom -> 0
                            lastScrollPosition != null -> lastScrollPosition!!
                            else -> 0
                        }
                )
            }

            this@ChatMessagesFragment.toBottomBtn.hide()
        }

        if (delay > 0) containerChat?.postDelayed(runnable, delay)
        else containerChat?.post(runnable)
    }

    fun initializeMessage(message: ChatMessage): ChatMessage {
        return message.apply {
            this.chatRoomID = chatRoomId
            this.senderID = mUser.userId
            this.recipientID = participantId
        }
    }

    fun addNewMessage(newMessage: ChatMessage) {
        chatRoom = getValidRoom()

        restoreScrollPosition()

        realm.executeTransaction {
            it.insertOrUpdate(newMessage)

            if (chatRoom?.isManaged() == true) {
                chatRoom?.text = when (newMessage.getMessageType()) {
                    ChatMessageType.TEXT -> {
                        if (newMessage.hasWebLink()) {
                            "" //TODO  choose string
                        }
                        else newMessage.text
                    }
                    ChatMessageType.AUDIO -> getString(R.string.chat_room_first_line_audio_out)
                    ChatMessageType.VIDEO -> getString(R.string.chat_room_first_line_video_out)
                    ChatMessageType.PICTURE -> getString(R.string.chat_room_first_line_picture_out)
                    ChatMessageType.LOCATION -> getString(R.string.chat_room_first_line_location_out)
                    ChatMessageType.DOCUMENT -> getString(R.string.chat_room_first_line_document_out)
                }
            }
        }

        restoreScrollPosition(delay = 100)
        messageBox.setText("")

        if (newMessage.getDirectionType(mUser.userId) == ChatMessage.DirectionType.OUTGOING) {
            callServer(CallType.NEW_MESSAGE, newMessage)
        }

        // can never happen
//        else
//            callServer(CallType.SET_READ, setIncomingRT = true)
    }


    fun isAutoPlayOn(): Boolean {
        return mUser.feedAutoplayAllowed(activity)
    }

    private fun cleanAdapterMediaControllers() {
        if (adapter.mediaPlayer?.isPlaying == true) adapter.mediaPlayer?.pause()
        adapter.mediaPlayer?.stop()
        adapter.mediaPlayer?.release()
        adapter.mediaPlayer = null

        if (adapter.playAudioTask?.isCancelled == false)
            adapter.playAudioTask?.cancel(true)

        if (adapter.videoHelper?.isPlaying == true) adapter.videoHelper?.pause()
        adapter.videoHelper?.release()

    }

    enum class CallType { NEW_MESSAGE, SET_READ, UPDATE_ACTIVITY, FETCH_MESSAGES, UPDATE_CHAT }
    fun callServer(type: CallType, chatMessage: ChatMessage? = null, setIncomingRT: Boolean = false,
                   userActivity: String? = null,
                   fetchDirection: HLServerCallsChat.FetchDirection = HLServerCallsChat.FetchDirection.BEFORE) {

        var result: Array<Any?>? = null
        when (type) {
            CallType.NEW_MESSAGE -> {
                if (chatMessage != null) {
                    sendingMessagesIDs.add(chatMessage.messageID)
                    // INFO: 2/13/19    found out that Gson cannot serialize Realm-managed objects
                    result = HLServerCallsChat.sendMessage(if (chatMessage.isManaged()) realm.copyFromRealm(chatMessage) else chatMessage)
                }
            }
            CallType.SET_READ -> {
                if (!chatRoomId.isNullOrBlank() &&
                        (setIncomingRT || ChatRoom.areThereUnreadMessages(chatRoomId!!, realm))
                ) {
                    if (!participantId.isNullOrBlank())
                        result = HLServerCallsChat.setMessageRead(participantId!!, chatRoomId!!)
                }
            }
            CallType.UPDATE_ACTIVITY -> {
                if (!chatRoomId.isNullOrBlank() && !participantId.isNullOrBlank())
                    HLServerCallsChat.setUserActivity(participantId!!, chatRoomId!!, userActivity)
            }
            CallType.FETCH_MESSAGES -> {
                if (!chatRoomId.isNullOrBlank())
                    HLServerCallsChat.fetchMessages(
                            ChatRoom.getRightTimeStamp(chatRoomId!!, realm, fetchDirection) ?: 0L,
                            chatRoomId!!,
                            fetchDirection
                    )
            }
            CallType.UPDATE_CHAT -> HLServerCallsChat.updateRoomsList(mUser.userId)
        }

        HLRequestTracker.getInstance(activity?.application as? LBSLinkApp).handleCallResult(this, activity as? HLActivity, result, true, false)
    }


    private fun setMessages() {
        if (adapter.itemCount == 0 && canFetchMessages) {
            callServer(CallType.FETCH_MESSAGES, fetchDirection = HLServerCallsChat.FetchDirection.AFTER)
        }
    }

    private fun setStatusIndicatorAndString(status: Int, date: String? = null, dateObj: Date? = null) {
        with (status) {
            availabilityIndicator?.visibility = if (this == ChatRecipientStatus.ONLINE.value) View.VISIBLE else View.GONE
            participantLastSeen?.text =
                    if (this == ChatRecipientStatus.ONLINE.value)  {
                        participantLastSeen.isSelected = false
                        context?.getString(R.string.chat_status_online)
                    }
                    else {
                        participantLastSeen.isSelected = true
                        val dateToFormat =
                                if (!date.isNullOrBlank()) Utils.getDateFromDB(date)
                                else dateObj ?: Date()

                        if (dateToFormat != null)
                            context?.getString(
                                    R.string.chat_status_last_seen,
                                    Utils.formatDateWithTime(context, dateToFormat, "EEE, dd MMM", true)
                            )
                        else
                            context?.getString(R.string.chat_status_offline)
                    }
        }
    }

    private fun handleToReadValue() {
        val chats = RealmUtils.readFromRealm(realm, ChatRoom::class.java)

        var toRead = 0
        if (!chats.isEmpty()) {
            chats.forEach {
                if (it is ChatRoom) toRead += it.toRead }
        }

        toolbar.toReadCount.text = toRead.toString()
        toolbar.toReadCount.visibility = if (toRead > 0) View.VISIBLE else View.GONE
    }

    private fun callParticipant(callType: VoiceVideoCallType) {
        chatRoom = getValidRoom()
        if (activity is HLActivity && chatRoom?.getParticipant() != null) {
            NotificationUtils.sendCallNotificationAndGoToActivity(
                    activity as HLActivity,
                    mUser.userId,
                    mUser.userCompleteName,
                    chatRoom?.getParticipant()!!,
                    callType
            )
        }
    }

    /**
     * Refreshes [ChatRoom] instance if necessary
     */
    private fun getValidRoom(): ChatRoom? {
        if (chatRoomId.isNullOrBlank()) return null

        if (chatRoom == null || !RealmObject.isValid(chatRoom!!))
            chatRoom = RealmUtils.readFirstFromRealmWithId(realm, ChatRoom::class.java, "chatRoomID", chatRoomId!!) as? ChatRoom

        return chatRoom
    }


    fun getActivityListener(): BasicInteractionListener {
        return activityListener
    }

    //endregion


    //region == Real time callbacks ==

    override fun onNewMessage(newMessage: ChatMessage) {
        if (llm?.findFirstVisibleItemPosition() == 0)
            restoreScrollPosition()

        callServer(CallType.SET_READ, setIncomingRT = true)
    }

    override fun onStatusUpdated(userId: String, status: Int, date: String) {
        if (userId == participantId) setStatusIndicatorAndString(status, date)
    }

    var previousActivity: String? = null
    override fun onActivityUpdated(userId: String, chatId: String, activity: String) {
        if (chatId == chatRoomId) {
            if (!activity.isBlank()) {
                previousActivity = participantLastSeen?.text.toString()
                participantLastSeen?.text = activity
            }
            else if (!previousActivity.isNullOrBlank())
                participantLastSeen?.text = previousActivity!!
        }
    }

    override fun onMessageDelivered(chatId: String, userId: String, date: String) {}

    override fun onMessageRead(chatId: String, userId: String, date: String) {}

    override fun onMessageOpened(chatId: String, userId: String, date: String, messageID: String) {}

    //endregion


    //region == SHARE ==

    override fun getUserID(): String {
        return mUser.userId
    }

    override fun getPostOrMessageID(): String {
        return currentMessageId ?: ""
    }

    //endregion


    //region == Getters and setters ==

    fun getUser(): HLUser {
        return mUser
    }

    //endregion


    inner class OnToolbarScrollListener(private val toolbar: View, private var isScrollIdle: Boolean): RecyclerView.OnScrollListener() {

        private var animating = false

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            when {
                dy < 0 -> animateToolbar(false)
                dy > 0 -> if (!stopAnimating) animateToolbar(true)
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            isScrollIdle = newState == RecyclerView.SCROLL_STATE_IDLE
        }

        private fun animateToolbar(show: Boolean) {

            val anim = toolbar.baseAnimateHeight(200, show, toolbarHeight, customListener = object: Animator.AnimatorListener{
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    animating = false
                }

                override fun onAnimationStart(animation: Animator?) {
                    animating = true
                }
            }, start = false)

            if (!animating && (
                            (show && toolbar.height == 0) || (!show && toolbar.height > 0))
            ) anim?.start()
        }
    }

    private fun hideToolbar() {
        if (toolbar.height > 0) {
            toolbar.baseAnimateHeight(
                    200,
                    false,
                    toolbarHeight
            )
        }
    }

}