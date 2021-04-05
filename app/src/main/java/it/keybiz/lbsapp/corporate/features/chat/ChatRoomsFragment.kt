/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.realm.*
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.HLFragment
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener
import it.keybiz.lbsapp.corporate.connection.*
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper
import kotlinx.android.synthetic.main.fragment_chat_rooms.*
import kotlinx.android.synthetic.main.layout_search_plus_rv.view.*
import kotlinx.android.synthetic.main.profile_search_box.view.*
import org.json.JSONArray

/**
 * @author mbaldrighi on 10/15/2018.
 */
class ChatRoomsFragment: HLFragment(), OnBackPressedListener, OnServerMessageReceivedListener,
        OnMissingConnectionListener, RealTimeChatListener, SearchHelper.OnQuerySubmitted,
        ChatRoomsAdapter.OnDeleteRoomListener {

    companion object {

        val LOG_TAG = ChatRoomsFragment::class.qualifiedName

        @JvmStatic fun newInstance(): ChatRoomsFragment {
            val fragment = ChatRoomsFragment()
            fragment.arguments = Bundle().apply {
                //                this.put...
            }

            return fragment
        }
    }

    private lateinit var adapter: ChatRoomsAdapter
    private val llm: LinearLayoutManager by lazy {
        LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    }

    private var srl: SwipeRefreshLayout? = null

    private val searchHelper by lazy { SearchHelper(this) }

    private var chatForDeletion: String? = null

    private var savedSearchString: String? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        onRestoreInstanceState(savedInstanceState ?: arguments)

        return inflater.inflate(R.layout.fragment_chat_rooms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rooms = if (savedSearchString.isNullOrBlank())
            (RealmUtils.readFromRealmSorted(realm, ChatRoom::class.java, "dateObj", Sort.DESCENDING) as RealmResults<ChatRoom>)
        else {
            realm.where(ChatRoom::class.java)
                    .contains("roomName", (savedSearchString as String).toLowerCase().trim(), Case.INSENSITIVE)
                    .sort("dateObj", Sort.DESCENDING)
                    .findAll()
        }
        adapter = ChatRoomsAdapter(view.context, rooms, this, this)

        configureLayout(view)
    }

    override fun onStart() {
        super.onStart()

        configureResponseReceiver()
    }

    override fun onResume() {
        super.onResume()

        (activity as? ChatActivity)?.backListener = this
        setLayout()

        callServer(CallType.LIST)
    }

    override fun onPause() {
        onSaveInstanceState(Bundle())

        Utils.closeKeyboard(chatRoomIncluded1.search_field)

        super.onPause()
    }

    override fun onBackPressed() {
        (activity as? ChatActivity)?.backListener = null
        activity?.onBackPressed()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedSearchString = savedInstanceState?.getString(Constants.EXTRA_PARAM_1)
    }

    override fun configureResponseReceiver() {
        if (serverMessageReceiver == null)
            serverMessageReceiver = ServerMessageReceiver()
        serverMessageReceiver.setListener(this)
    }

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        (activity as? HLActivity)?.closeProgress()
        Utils.setRefreshingForSwipeLayout(srl, false)

        if (responseObject == null) {
            handleErrorResponse(operationId, Constants.SERVER_ERROR_GENERIC); return
        }

        val jsonObj = responseObject.optJSONObject(0)
        when (operationId) {
            Constants.SERVER_OP_CHAT_UPDATE_LIST -> {

                if (responseObject.length() == 0 || responseObject.optJSONObject(0) == null) {
                    handleErrorResponse(operationId, Constants.SERVER_ERROR_GENERIC); return
                }

                realm.executeTransactionAsync {

                    ChatRoom.handleDeletedRooms(it, responseObject)

                    for (i in 0 until responseObject.length()) {
                        val room = ChatRoom.getRoom(responseObject.optJSONObject(i))
                        if (room.isValid()) {
                            it.insertOrUpdate(room)
                        }
                    }
                }
            }
            Constants.SERVER_OP_CHAT_DELETE_ROOM -> {
                if (!chatForDeletion.isNullOrBlank()) {
                    val room = RealmUtils.readFirstFromRealmWithId(realm, ChatRoom::class.java, "chatRoomID", chatForDeletion!!)
                    realm.executeTransaction {
                        RealmObject.deleteFromRealm(room)
                        RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", chatForDeletion!!).deleteAllFromRealm()

                        this@ChatRoomsFragment.chatRoomIncluded1?.no_result?.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        super.handleErrorResponse(operationId, errorCode)

        (activity as? HLActivity)?.closeProgress()
        Utils.setRefreshingForSwipeLayout(srl, false)

        when (operationId) { }
    }

    override fun onMissingConnection(operationId: Int) {
        (activity as? HLActivity)?.closeProgress()
        Utils.setRefreshingForSwipeLayout(srl, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(Constants.EXTRA_PARAM_1, savedSearchString)
    }

    override fun configureLayout(view: View) {

        chatRoomIncluded1.generic_rv.layoutManager = llm
        chatRoomIncluded1.generic_rv.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        chatRoomIncluded1.no_result.setText(R.string.no_result_chat_rooms)

        srl = Utils.getGenericSwipeLayout(view) {
            Utils.setRefreshingForSwipeLayout(srl, true)
            callServer(CallType.LIST)
        }

        // TODO: 11/5/2018    DISABLED FOR NOW
        srl!!.isEnabled = false

        // TODO: 11/29/2018    searchLayout is DISABLED for now
        chatRoomIncluded1.search_box.visibility = View.GONE

        chatRoomIncluded1.search_field.hint = resources.getString(R.string.chat_rooms_search_hint)
        chatRoomIncluded1.search_field.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 0) adapter.filter.filter("")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        if (!savedSearchString.isNullOrBlank())
            chatRoomIncluded1.search_field.setText(savedSearchString)
        searchHelper.configureViews(chatRoomIncluded1.search_box, chatRoomIncluded1.search_field)

        createBtn.setOnClickListener {
            if (Utils.isContextValid(context))
                ChatActivity.openChatCreationFragment(context!!)
        }
    }

    override fun setLayout() {
        chatRoomIncluded1.focus_catcher.requestFocus()
        chatRoomIncluded1.setPaddingRelative(0, 0, 0, 0)
        chatRoomIncluded1.generic_rv.adapter = adapter
        chatRoomIncluded1.generic_rv.visibility = if (adapter.itemCount == 0) View.GONE else View.VISIBLE
        chatRoomIncluded1.no_result.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }


    //region == Class custom methods ==

    internal enum class CallType { LIST, DELETE /*SET_READ*/ }
    private fun callServer(type: CallType, roomId: String? = null, participantId: String? = null) {
        var result: Array<Any?>? = null

        when (type) {
            CallType.LIST -> {
                result = HLServerCallsChat.updateRoomsList(mUser.userId)
            }
            CallType.DELETE -> {
                if (!roomId.isNullOrBlank())
                    result = HLServerCallsChat.deleteRoom(mUser.userId, roomId)
            }

            // TODO: 11/4/2018    is it gonna be a setAsRead function?
//            CallType.SET_READ -> {
//                if (!roomId.isNullOrBlank() && !participantId.isNullOrBlank()
//                        (setIncomingRT || ChatRoom.areThereUnreadMessages(roomId, realm))
//                ) {
//                    if (!participantId.isNullOrBlank())
//                        result = HLServerCallsChat.setMessageRead(participantId!!, roomId)
//                }
//            }
        }

        HLRequestTracker.getInstance(activity?.application as? LBSLinkApp).handleCallResult(this, activity as? HLActivity, result, true, true)
    }

    //endregion


    //region == Real time callbacks ==

    override fun onNewMessage(newMessage: ChatMessage) {
        val roomId = newMessage.chatRoomID
        if (!roomId.isNullOrBlank()) {
            val room = adapter.getRoomByID(ChatRoomsAdapter.FilterBy.ROOM_ID, roomId).second

            if (room?.isValid() == true) {
                realm.executeTransaction {
                    room.text = when {
                        newMessage.hasAudio() -> getString(R.string.chat_room_first_line_audio_in)
                        newMessage.hasVideo() -> getString(R.string.chat_room_first_line_video_in)
                        newMessage.hasPicture() -> getString(R.string.chat_room_first_line_picture_in)
                        newMessage.hasLocation() -> getString(R.string.chat_room_first_line_location_in)
                        newMessage.hasDocument() -> getString(R.string.chat_room_first_line_document_in)
                        newMessage.hasWebLink() -> {
                            "" //TODO  change with server UNICODE
                        }
                        else -> newMessage.text
                    }
                    room.dateObj = newMessage.creationDateObj
                }
            }
        }
    }

    override fun onStatusUpdated(userId: String, status: Int, date: String) {

        val room = adapter.getRoomByID(ChatRoomsAdapter.FilterBy.PARTICIPANT, userId).second

        realm.executeTransaction {
            room?.participants?.get(0)?.lastSeenDate = date
            room?.participants?.get(0)?.chatStatus = status
            room?.recipientStatus = status
        }
    }

    private var activitiesMap = mutableMapOf<String, String>()
    override fun onActivityUpdated(userId: String, chatId: String, activity: String) {

        val room = adapter.getRoomByID(ChatRoomsAdapter.FilterBy.ROOM_ID, chatId).second
        val previousActivity = activitiesMap[chatId]

        realm.executeTransaction {
            if (!activity.isBlank()) {
                activitiesMap[chatId] = room?.text ?: ""
                room?.text = activity
            }
            else if (!previousActivity.isNullOrBlank())
                room?.text = previousActivity
        }
    }

    override fun onMessageDelivered(chatId: String, userId: String, date: String) {}

    override fun onMessageRead(chatId: String, userId: String, date: String) {}

    override fun onMessageOpened(chatId: String, userId: String, date: String, messageID: String) {}

    //endregion


    //region == Search listener ==

    override fun onQueryReceived(query: String) {
//        adapter.filter.filter(query)
//        {
//            chatRoomIncluded1.no_result.setText(
//                    if (query.isBlank()) R.string.no_result_chat_rooms
//                    else R.string.no_result_chat_rooms_search
//            )
//            chatRoomIncluded1.no_result.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
//            chatRoomIncluded1.generic_rv.visibility = if (adapter.itemCount == 0) View.GONE else View.VISIBLE
//        }

        savedSearchString = query
        adapter.filter.filter(query)
//        setAdapter(query)
    }

    //endregion

    //region == Delete room listener ==

    override fun onRoomDeleted(chatID: String) {
        chatForDeletion = chatID
        callServer(CallType.DELETE, chatID)
    }

    //endregion


    //region == Getters and setters ==

    fun getUser(): HLUser {
        return mUser
    }

    fun getRealm(): Realm {
        return realm
    }

    //endregion

}