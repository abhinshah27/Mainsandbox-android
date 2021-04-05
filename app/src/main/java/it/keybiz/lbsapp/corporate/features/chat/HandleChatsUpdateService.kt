/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.realm.Realm
import it.keybiz.lbsapp.corporate.connection.HLServerCallsChat
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import org.json.JSONArray

/**
 * [Service] subclass whose duty is to subscribe client to Real-Time communication with socket.
 * @author mbaldrighi on 11/01/2017.
 */
class HandleChatsUpdateService : Service(), OnServerMessageReceivedListener {

    companion object {
        val LOG_TAG = HandleChatsUpdateService::class.qualifiedName

        @JvmStatic
        fun startService(context: Context) {
            try {
                context.startService(Intent(context, HandleChatsUpdateService::class.java))
            } catch (e: IllegalStateException) {
                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
            }
        }
    }

    private lateinit var receiver: ServerMessageReceiver
    private var realm: Realm? = null
    private val chatIds = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()
        receiver = ServerMessageReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        receiver.setListener(this)
        //		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))

        realm = RealmUtils.getCheckedRealm()
        val user = HLUser().readUser(realm!!)
        if (user != null && user.isValid && !user.isActingAsInterest) {
            val id = user.id
            callServer(CallType.CHATS, id)
        }
        else exitOps()

        return Service.START_STICKY
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        exitOps(false)
        RealmUtils.closeRealm(realm)

        super.onDestroy()
    }

    private enum class CallType { CHATS, MESSAGES, DELIVERY_HACK }
    private fun callServer(type: CallType, userId: String? = null, chatRoomId: String? = null) {
        val results = when (type) {
            CallType.CHATS -> {
                if (!userId.isNullOrBlank()) HLServerCallsChat.updateRoomsList(userId) else null
            }
            CallType.MESSAGES -> {
                if (!chatRoomId.isNullOrBlank() && RealmUtils.isValid(realm)) {
                    val unixTS = ChatRoom.getRightTimeStamp(chatRoomId, realm!!, HLServerCallsChat.FetchDirection.AFTER)
                    HLServerCallsChat.fetchMessages(unixTS!!, chatRoomId, HLServerCallsChat.FetchDirection.AFTER)
                } else null
            }
            CallType.DELIVERY_HACK -> {
                if (!userId.isNullOrBlank() && !chatRoomId.isNullOrBlank()) {
                    HLServerCallsChat.sendChatHack(HLServerCallsChat.HackType.DELIVERY, userId, chatRoomId)
                } else null
            }
        }

        if (results == null || !(results[0] as Boolean)) {
            exitOps()
        }
    }


    //region == Receiver Callback ==

    private val updateChatsLambda = { jsonArray: JSONArray?, realm: Realm ->
        if (jsonArray != null) {
            realm.executeTransaction {

                ChatRoom.handleDeletedRooms(realm, jsonArray)

                if (jsonArray.length() > 0) {
                    for (i in 0 until jsonArray.length()) {
                        val room = ChatRoom.getRoom(jsonArray.optJSONObject(i))
                        if (room.isValid()) {
                            it.insertOrUpdate(room)
                            chatIds.add(room.chatRoomID!!)
                            callServer(type = CallType.MESSAGES, chatRoomId = room.chatRoomID)
                        }
                    }
                } else exitOps()
            }
        }
    }

    private val updateMessagesLambda = { jsonArray: JSONArray?, realm: Realm ->
        if (jsonArray != null && jsonArray.length() > 0) {
            val userId = HLUser().readUser(realm).userId
            var chatRoomId = ""
            realm.executeTransaction {
                for (i in 0 until jsonArray.length()) {
                    val message = ChatMessage.getMessage(jsonArray.optJSONObject(i))
                    it.insertOrUpdate(message)
                    if (chatRoomId.isBlank()) chatRoomId = message.chatRoomID!!
                }
            }

            callServer(CallType.DELIVERY_HACK, userId, chatRoomId)
        }
    }

    var count = 0
    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray) {

        when (operationId) {

            Constants.SERVER_OP_CHAT_UPDATE_LIST -> {
                if (RealmUtils.isValid(realm)) updateChatsLambda(responseObject, realm!!)
//                for (it in chatIds) callServer(CallType.MESSAGES, it)
            }

            Constants.SERVER_OP_CHAT_FETCH_MESSAGES -> {
                if (RealmUtils.isValid(realm)) updateMessagesLambda(responseObject, realm!!)

                if (++count == chatIds.size) exitOps()
            }

            Constants.SERVER_OP_CHAT_HACK -> {
                LogUtils.d(LOG_TAG, "$count/${chatIds.size} delivery notifications sent to server")
            }
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {

        when (operationId) {

            Constants.SERVER_OP_CHAT_UPDATE_LIST -> exitOps()

            Constants.SERVER_OP_CHAT_FETCH_MESSAGES -> {
                if (++count == chatIds.size) exitOps()
            }

            Constants.SERVER_OP_CHAT_HACK -> {
                LogUtils.d(LOG_TAG, "Delivery hack failed @ no. $count")
            }

            0 -> {
                if (chatIds.isEmpty()) exitOps()
            }
        }
    }

    //endregion

    private fun exitOps(stop: Boolean = true) {
        try {
            //					unregisterReceiver(receiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            LogUtils.d(LOG_TAG, e.message)
        }
        if (stop) stopSelf()
    }

}
