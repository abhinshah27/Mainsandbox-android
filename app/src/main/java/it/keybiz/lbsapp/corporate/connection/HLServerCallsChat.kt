/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.connection

import androidx.core.util.Pair
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.Utils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * @author mbaldrighi on 10/29/2018.
 */
object HLServerCallsChat: HLServerCalls() {

    @Throws(JSONException::class)
    @JvmStatic fun initializeNewRoom(room: ChatRoom): Array<Any?> {

        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            return resultsNotConnected()

        var jsonResult: Array<String?>
        val list = ArrayList<List<Pair<String, Any>>>()

        val pairs = ArrayList<Pair<String, Any>>()
        pairs.add(Pair("ownerID", room.ownerID))
        pairs.add(Pair("date", room.date))

        if (!room.participantIDs.isEmpty()) {
            val arr = JSONArray(room.participantIDs)
            pairs.add(Pair("participantIDs", arr))

        }
        list.add(pairs)

        jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_INITIALIZE_ROOM,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "INITIALIZE NEW ROOM call")
        LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)

        return arrayOf(true, jsonResult[0], jsonResult[1])
    }

    @Throws(JSONException::class)
    fun sendMessage(message: ChatMessage): Array<Any?> {

        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            return resultsNotConnected()

//        val list = ArrayList<List<Pair<String, Any>>>()
//        val pairs = ArrayList<Pair<String, Any>>().apply {
//            this.add(Pair("senderID", message.senderID))
//            this.add(Pair("recipientID", message.recipientID))
//            this.add(Pair("chatRoomID", message.chatRoomID))
//            this.add(Pair("messageType", message.messageType))
//            this.add(Pair("mediaURL", message.mediaURL))
//            this.add(Pair("creationDate", message.creationDate))
//            this.add(Pair("deliveryDate", ""))
//            this.add(Pair("text", message.text))
//            this.add(Pair("location", message.location))
//            this.add(Pair("messageID", message.messageID))
//            this.add(Pair("sharedDocumentFileName", message.sharedDocumentFileName))
//        }
//        list.add(pairs)

        val jObj = JSONObject(message.serializeToStringWithExpose())
        val jArray = JSONArray().put(jObj.put("deliveryDate", ""))

        val jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_SEND_MESSAGE,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, jArray,
                "SEND NEW MESSAGE call")
        LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)

        return arrayOf(true, jsonResult[0], jsonResult[1])
    }

    @Throws(JSONException::class)
    fun updateRoomsList(userId: String): Array<Any?> {

//        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
//            return resultsNotConnected()

        val list = ArrayList<List<Pair<String, Any>>>()
        val pairs = ArrayList<Pair<String, Any>>().apply {
            this.add(Pair("userID", userId))
        }
        list.add(pairs)

        val jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_UPDATE_LIST,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "UPDATE CHATS LIST call")

        return if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            arrayOf(false, jsonResult[0], jsonResult[1])
        else {
            LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)
            arrayOf(true, jsonResult[0], jsonResult[1])
        }
    }

    @Throws(JSONException::class)
    fun deleteRoom(userId: String, chatRoomId: String): Array<Any?> {

        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            return resultsNotConnected()

        val list = ArrayList<List<Pair<String, Any>>>()
        val pairs = ArrayList<Pair<String, Any>>().apply {
            this.add(Pair("userID", userId))
            this.add(Pair("chatRoomID", chatRoomId))
        }
        list.add(pairs)

        val jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_DELETE_ROOM,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "DELETE ROOM call")
        LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)

        return arrayOf(true, jsonResult[0], jsonResult[1])
    }

    enum class FetchDirection(val value: Int) { BEFORE(0), AFTER(1)}
    @Throws(JSONException::class)
    fun fetchMessages(unixTimeStamp: Long, chatRoomId: String, dir: FetchDirection): Array<Any?> {

//        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
//            return resultsNotConnected()

        val list = ArrayList<List<Pair<String, Any>>>()
        val pairs = ArrayList<Pair<String, Any>>().apply {
            this.add(Pair("unixtimestamp", unixTimeStamp))
            this.add(Pair("chatRoomID", chatRoomId))
            this.add(Pair("direction", dir.value))
        }
        list.add(pairs)

        val jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_FETCH_MESSAGES,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "FETCH MESSAGES call")

        return if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            arrayOf(false, jsonResult[0], jsonResult[1])
        else {
            LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)
            arrayOf(true, jsonResult[0], jsonResult[1])
        }
    }

    enum class HackType(val value: Int) { DELIVERY(0), DOC_OPENED(2) }
    @Throws(JSONException::class)
    fun sendChatHack(hackType: HackType, userId: String, chatRoomId: String, messageID: String? = null): Array<Any?> {

        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            return resultsNotConnected()

        val list = ArrayList<List<Pair<String, Any>>>()
        val pairs = ArrayList<Pair<String, Any>>().apply {
            this.add(Pair("ackType", hackType.value))
            this.add(Pair("userID", userId))
            this.add(Pair("roomID", chatRoomId))
            if (!messageID.isNullOrBlank()) this.add(Pair("messageID", messageID))
        }
        list.add(pairs)

        val jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_HACK,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "SEND HACK of type ${if (hackType == HackType.DELIVERY) "DELIVERY" else "null" } call")
        LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)

        return arrayOf(true, jsonResult[0], jsonResult[1])
    }

    @Throws(JSONException::class)
    fun setUserActivity(participantId: String, chatRoomId: String, activity: String? = null): Array<Any?> {

        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            return resultsNotConnected()

        val list = ArrayList<List<Pair<String, Any>>>()
        val pairs = ArrayList<Pair<String, Any>>().apply {
            this.add(Pair("userID", participantId))
            this.add(Pair("chatID", chatRoomId))
            if (!activity.isNullOrBlank()) this.add(Pair("activity", activity))
        }
        list.add(pairs)

        val jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_SEND_USER_ACTIVITY,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "SEND USER ACTIVITY call")
        LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)

        return arrayOf(true, jsonResult[0], jsonResult[1])
    }

    @Throws(JSONException::class)
    fun setMessageRead(participantId: String, chatRoomId: String): Array<Any?> {

        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            return resultsNotConnected()

        val list = ArrayList<List<Pair<String, Any>>>()
        val pairs = ArrayList<Pair<String, Any>>().apply {
            this.add(Pair("userID", participantId))
            this.add(Pair("chatRoomID", chatRoomId))
            this.add(Pair("date", Utils.formatDateForDB(System.currentTimeMillis())))
        }
        list.add(pairs)

        val jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_SET_MESSAGES_READ,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "SET MESSAGES READ FOR ROOM call")
        LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)

        return arrayOf(true, jsonResult[0], jsonResult[1])
    }

    @Throws(JSONException::class)
    @JvmStatic fun setUserOnline(userId: String): Array<Any?> {

        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            return resultsNotConnected()

        val jsonResult: Array<String?>
        val list = ArrayList<List<Pair<String, Any>>>()

        val pairs = ArrayList<Pair<String, Any>>()
        pairs.add(Pair("userID", userId))
        list.add(pairs)

        jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_SET_USER_ONLINE,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "SET USER ONLINE call")
        LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)

        return arrayOf(true, jsonResult[0], jsonResult[1])
    }

    @Throws(JSONException::class)
    @JvmStatic fun getUsersForNewChats(userId: String, page: Int): Array<Any?> {

//        if (!LBSLinkApp.getSocketConnection().isConnectedChat)
//            return resultsNotConnected()

        val jsonResult: Array<String?>
        val list = ArrayList<List<Pair<String, Any>>>()

        val pairs = ArrayList<Pair<String, Any>>()
        pairs.add(Pair("userID", userId))
        pairs.add(Pair("pageID", page))
        list.add(pairs)

        jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CHAT_GET_NEW_CHATS,
                Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_CHAT, list,
                "GET NEW CHATS call")
        LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)

        return if (!LBSLinkApp.getSocketConnection().isConnectedChat)
            arrayOf(false, jsonResult[0], jsonResult[1])
        else {
            LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]!!)
            arrayOf(true, jsonResult[0], jsonResult[1])
        }
    }

}