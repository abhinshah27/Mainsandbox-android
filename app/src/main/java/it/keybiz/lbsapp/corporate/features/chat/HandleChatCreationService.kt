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
import io.realm.kotlin.isManaged
import it.keybiz.lbsapp.corporate.connection.HLServerCallsChat
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import org.json.JSONArray
import org.json.JSONException

/**
 * [Service] subclass whose duty is to handle new [ChatRoom] initialization on server.
 * @author mbaldrighi on 11/01/2017.
 */
class HandleChatCreationService : Service(), OnServerMessageReceivedListener {

    companion object {
        val LOG_TAG = HandleChatCreationService::class.qualifiedName

        @JvmStatic
        fun startService(context: Context, room: ChatRoom?, realm: Realm) {
            try {
                if (room?.isValid() == true) {
                    Companion.room = if (room.isManaged()) realm.copyFromRealm(room) else room
                    context.startService(Intent(context, HandleChatCreationService::class.java))
                }
            } catch (e: IllegalStateException) {
                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
            }
        }

        lateinit var room: ChatRoom
    }

    private lateinit var receiver: ServerMessageReceiver
    private var realm: Realm? = null

    override fun onCreate() {
        super.onCreate()
        receiver = ServerMessageReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        receiver.setListener(this)
        //		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))

        realm = RealmUtils.getCheckedRealm()

        if (room.isValid()) callServer()
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

    private fun callServer() {
        var result: Array<Any?>? = null
        try {
            result = HLServerCallsChat.initializeNewRoom(room)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (result == null || !(result[0] as Boolean)) {
            exitOps()
        }
    }


    //region == Receiver Callback ==

    var count = 0
    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray) {
        if (operationId == Constants.SERVER_OP_CHAT_INITIALIZE_ROOM) {
            val j = responseObject.optJSONObject(0)
            if (j != null && RealmUtils.isValid(realm)) {
                room = ChatRoom.getRoom(j)
                room.ownerID = HLUser().readUser(realm!!).userId

                realm!!.executeTransaction { it -> it.insertOrUpdate(room) }
            }
            exitOps()
        }

    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        if (operationId == Constants.SERVER_OP_CHAT_INITIALIZE_ROOM) {
            LogUtils.d(LOG_TAG, "Impossible initialize ChatRoom-${room.chatRoomID} at the moment")
            exitOps()
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
