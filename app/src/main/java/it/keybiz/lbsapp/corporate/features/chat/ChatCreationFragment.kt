/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.realm.RealmList
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.HLFragment
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.connection.*
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.NotificationUtils
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp.VoiceVideoCallType
import it.keybiz.lbsapp.corporate.models.HLUserGeneric
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.helpers.ComputeAndPopulateHandlerThread
import kotlinx.android.synthetic.main.fragment_chat_creation.*
import kotlinx.android.synthetic.main.layout_search_plus_rv.view.*
import kotlinx.android.synthetic.main.luiss_toolbar_simple_back.*
import org.json.JSONArray
import java.util.*

class ChatCreationFragment:
        HLFragment(), OnServerMessageReceivedListener, OnMissingConnectionListener, Handler.Callback,
        ChatCreationAdapterDiff.NewChatCallback {

    companion object {

        val LOG_TAG = ChatCreationFragment::class.qualifiedName

        @JvmStatic fun newInstance(): ChatCreationFragment {
            val fragment = ChatCreationFragment()
            fragment.arguments = Bundle().apply {
                //                this.put...
            }

            return fragment
        }
    }

    var loadedData = mutableListOf<HLUserGeneric>()
    private var adapter: ChatCreationAdapterDiff? = null

    private var chatRoom: ChatRoom? = null
    private var currentContact: HLUserGeneric? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        onRestoreInstanceState(savedInstanceState ?: arguments)

        return inflater.inflate(R.layout.fragment_chat_creation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureLayout(view)
    }

    override fun onStart() {
        super.onStart()

        configureResponseReceiver()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(context, AnalyticsUtils.CHAT_CREATE_ROOM)

        callServer(CallType.GET)

        setLayout()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {}

    override fun configureResponseReceiver() {
        if (serverMessageReceiver == null)
            serverMessageReceiver = ServerMessageReceiver()
        serverMessageReceiver.setListener(this@ChatCreationFragment)
    }

    override fun configureLayout(view: View) {

        Utils.getGenericSwipeLayout(view) {
            Utils.setRefreshingForSwipeLayout(chatRoomIncluded1.swipe_refresh_layout, true)
            callServer(CallType.GET)
        }

        back_arrow.setOnClickListener { activity?.onBackPressed() }

        chatRoomIncluded1.generic_rv.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        if (chatRoomIncluded1.generic_rv.layoutManager == null)
            chatRoomIncluded1.generic_rv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        if (adapter == null)
            adapter = ChatCreationAdapterDiff(HLUserGenericDiffCallback(), this).apply { this.setHasStableIds(true); }
        chatRoomIncluded1.generic_rv.adapter = adapter
    }

    override fun setLayout() {
        chatRoomIncluded1.search_box.visibility = View.GONE
        chatRoomIncluded1.setPadding(0, 0, 0, 0)

        chatRoomIncluded1.no_result.setText(R.string.no_result_chat_creation)

        back_arrow.rotation = 270f

        setData()
    }


    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        with(operationId) {
            if (this == Constants.SERVER_OP_CHAT_GET_NEW_CHATS || this == Constants.SERVER_OP_CHAT_INITIALIZE_ROOM) {
                activityListener.closeProgress()
                Utils.setRefreshingForSwipeLayout(chatRoomIncluded1.swipe_refresh_layout, false)

                if (this == Constants.SERVER_OP_CHAT_GET_NEW_CHATS)
                    setData(responseObject)
                else
                    handleRoomInitialization(responseObject)
            }
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        super.handleErrorResponse(operationId, errorCode)

        if (operationId == Constants.SERVER_OP_CHAT_GET_NEW_CHATS || operationId == Constants.SERVER_OP_CHAT_INITIALIZE_ROOM) {
            Utils.setRefreshingForSwipeLayout(chatRoomIncluded1.swipe_refresh_layout, false)
            activityListener.closeProgress()
            activityListener.showGenericError()
        }
    }

    override fun onMissingConnection(operationId: Int) {
        if (operationId == Constants.SERVER_OP_CHAT_GET_NEW_CHATS || operationId == Constants.SERVER_OP_CHAT_INITIALIZE_ROOM) {
            activityListener.closeProgress()

            Utils.setRefreshingForSwipeLayout(chatRoomIncluded1.swipe_refresh_layout, false)
        }
    }


    private fun setData(responseObject: JSONArray? = null) {
        if (responseObject == null || responseObject.length() == 0 || responseObject.optJSONObject(0)?.length() == 0) {
            chatRoomIncluded1.generic_rv.visibility = View.GONE
            chatRoomIncluded1.no_result.visibility = View.VISIBLE

            loadedData.clear()
            adapter?.submitList(loadedData)

            return
        }

        chatRoomIncluded1.generic_rv.visibility = View.VISIBLE
        chatRoomIncluded1.no_result.visibility = View.GONE

        PopulateChatHandler(this, responseObject).start()
    }

    private fun handleRoomInitialization(responseObject: JSONArray?) {
        val j = responseObject?.optJSONObject(0)
        if (j != null) {
            chatRoom = ChatRoom.getRoom(j)
            chatRoom!!.ownerID = mUser.userId
        }

        if (activity != null && activity is HLActivity) {
            (activity as HLActivity).realm?.executeTransaction { realm -> realm.insertOrUpdate(chatRoom) }

            if (chatRoom?.isValid() == true && currentContact != null) {
                chatActivityListener.showChatMessagesFragment(
                        Objects.requireNonNull<String>(chatRoom!!.chatRoomID),
                        currentContact!!.completeName,
                        currentContact!!.avatarURL,
                        false,
                        loadedData.size == 1
                )
            }
        }
    }

    private enum class CallType { GET, INITIALIZE }
    private fun callServer(type: CallType, room: ChatRoom? = null) {

        LogUtils.d(LOG_TAG, "Calling server")

        // for now pagination value set to 0 by default
        val result: Array<Any?>? = if (type == CallType.INITIALIZE && room != null)
            HLServerCallsChat.initializeNewRoom(room)
        else if (type == CallType.GET)
            HLServerCallsChat.getUsersForNewChats(mUser.userId, 0)
        else null

        HLRequestTracker.getInstance(activity?.application as? LBSLinkApp).handleCallResult(this, activity as? HLActivity, result, true, true)
    }


    //region == Callbacks ==
    override fun onChatClicked(user: HLUserGeneric?) {
        if (user != null) {
            currentContact = user

            val ids = RealmList<String>()
            ids.add(user.id)
            chatRoom = ChatRoom(
                    mUser.userId,
                    ids,
                    null
            )

            callServer(CallType.INITIALIZE, chatRoom)
        }
    }

    override fun onCallClicked(user: HLUserGeneric?, callType: VoiceVideoCallType) {
        if (Utils.isContextValid(activity) && user != null) {
            currentContact = user
            NotificationUtils.sendCallNotificationAndGoToActivity(activity as HLActivity, mUser.userId, mUser.completeName, user, callType)
        }
        else
            activityListener.showAlert(R.string.error_calls_start)
    }

    //endregion


    private class PopulateChatHandler(private val fragment: ChatCreationFragment,
                                      private val response: JSONArray?):
            ComputeAndPopulateHandlerThread("populateChatCreation", fragment) {

        override fun onLooperPrepared() {
            super.onLooperPrepared()

            if (handler != null) {
                handler!!.post {
                    val users = response?.optJSONObject(0)
                    val list = users?.optJSONArray("users")
                    fragment.loadedData.clear()
                    if (list != null && list.length() > 0) {
                        for (i in 0 until list.length()) {
                            val user = HLUserGeneric().deserializeToClass(list.optJSONObject(i))
                            fragment.loadedData.add(user)
                        }
                        fragment.loadedData.sort()
                    }
                }

                exitOps()
            }
        }
    }


    override fun handleMessage(msg: Message): Boolean {
        if (loadedData.isNotEmpty())
           adapter?.submitList(loadedData)
        else {
            activity?.runOnUiThread {
                chatRoomIncluded1.generic_rv.visibility = View.GONE
                chatRoomIncluded1.no_result.visibility = View.VISIBLE
            }
        }
        return true
    }

}