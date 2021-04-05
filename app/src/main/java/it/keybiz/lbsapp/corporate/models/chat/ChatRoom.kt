/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.chat

import com.google.gson.JsonElement
import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import it.keybiz.lbsapp.corporate.connection.HLServerCallsChat
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.models.HLUserGeneric
import it.keybiz.lbsapp.corporate.models.JsonHelper
import it.keybiz.lbsapp.corporate.models.RealmModelListener
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.*

/**
 * Enum class representing all the potential statuses of a chat recipient.
 * @author mbaldrighi on 10/26/2018.
 */
enum class ChatRecipientStatus(val value: Int) { OFFLINE(0), ONLINE(1) }

/**
 * Class representing the instance of a chat room.
 * @author mbaldrighi on 10/26/2018.
 */
@RealmClass
open class ChatRoom(var ownerID: String? = null,
                    var participantIDs: RealmList<String> = RealmList(),
                    @PrimaryKey var chatRoomID: String? = null):
        RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer, Serializable, RealmChangeListener<ChatRoom> {

    companion object {
        @JvmStatic fun getRoom(json: JSONObject?): ChatRoom {
            val room = JsonHelper.deserialize(json, ChatRoom::class.java) as ChatRoom
            room.recipientStatus = room.participants[0]?.chatStatus ?: ChatRecipientStatus.OFFLINE.value
            for (it in room.participants)
                room.participantIDs.add(it.id)

            room.dateObj = Utils.getDateFromDB(room.date)
            room.roomName = room.getRoomName()

            return room
        }

        fun getRightTimeStamp(chatRoomId: String, realm: Realm, direction: HLServerCallsChat.FetchDirection): Long? {

            val messages = RealmUtils.readFromRealmWithIdSorted(realm, ChatMessage::class.java, "chatRoomID", chatRoomId, "creationDateObj", Sort.ASCENDING) as RealmResults<ChatMessage>

            return if (direction == HLServerCallsChat.FetchDirection.BEFORE) {
                messages[0]?.unixtimestamp
            }
            else {
                return when {
                    messages.isEmpty() -> 0

                    !messages.isEmpty() -> {
                        var result = 0L
                        // from the newest message down to the oldest store [ChatMessage.unixtimestamp] if message isn't READ
                        for (it in (messages.size - 1) downTo 0) {
                            val message = messages[it]
                            if (message?.isRead() == false && !message.isError) {
                                if (message.unixtimestamp != null) result = message.unixtimestamp!!
                            }
                            else break
                        }

//                        if (result == 0L) {
//                            result = (messages[0] as ChatMessage).unixtimestamp!!; result
//                        } else
                        result
                    }

                    else -> 0
                }
            }
        }

        fun areThereUnreadMessages(chatRoomId: String? = null, realm: Realm): Boolean {
            return if (!chatRoomId.isNullOrBlank()) {
                val messages = RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", chatRoomId)
                val incoming = messages.filter { (it as ChatMessage).getDirectionType(HLUser().readUser(realm).userId) == ChatMessage.DirectionType.INCOMING }
                val filtered = incoming.filter { (it as ChatMessage).getStatusEnum() == ChatMessage.Status.READ }
                incoming.size > filtered.size
            }
            else {
                var thereAreMessages = false
                RealmUtils.readFromRealm(realm, ChatRoom::class.java).forEach {
                    if (it is ChatRoom) {
                        if (it.toRead > 0) { thereAreMessages = true; return@forEach  }
                    }
                }

                thereAreMessages
            }
        }

        /**
         * Checks whether there are some rooms that need to be deleted from the local DB.
         * To be used already inside a Realm transaction.
         */
        fun handleDeletedRooms(realm: Realm, jsonArray: JSONArray?) {
            when {
                jsonArray == null -> return
                jsonArray.length() == 0 -> {
                    RealmUtils.deleteTable(realm, ChatRoom::class.java)
                    RealmUtils.deleteTable(realm, ChatMessage::class.java)
                    return
                }
                else -> {
                    val rooms = RealmUtils.readFromRealm(realm, ChatRoom::class.java)
                    if (rooms != null && !rooms.isEmpty()) {
                        rooms.forEach { room ->
                            val id = (room as? ChatRoom)?.chatRoomID
                            if (!id.isNullOrBlank()) {
                                var delete = true
                                for (i in 0 until jsonArray.length()) {
                                    val jID = jsonArray.optJSONObject(i)?.optString("chatRoomID", "")
                                    if (id == jID) { delete = false; break }
                                }

                                if (delete) {
                                    RealmObject.deleteFromRealm(room)
                                    RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", id).deleteAllFromRealm()
                                }
                            }
                        }
                    }
                }
            }
        }

        @JvmStatic fun checkRoom(chatRoomID: String?, realm: Realm?): Pair<Boolean, ChatRoom?> {
            if (!chatRoomID.isNullOrBlank() && RealmUtils.isValid(realm)) {
                // check if I have the same room
                val room = RealmUtils.readFirstFromRealmWithId(
                        realm,
                        ChatRoom::class.java,
                        "chatRoomID",
                        chatRoomID
                ) as? ChatRoom

                return (room?.isValid() == true) to room
            }

            return false to null
        }

    }

    var date: String = Utils.formatDateForDB(Date())
    var dateObj: Date? = null
    var text: String = ""
    var recipientStatus: Int = 0
    var participants = RealmList<HLUserGeneric>()
    var roomName: String? = null
    var toRead: Int = 0
//    var participantAvatar: String? = null
//    var lastSeenDate: String? = null



    //region == Class custom methods ==

    fun getRecipientStatus(): ChatRecipientStatus {
        return when (recipientStatus) {
            0 -> ChatRecipientStatus.OFFLINE
            1 -> ChatRecipientStatus.ONLINE
            else -> ChatRecipientStatus.OFFLINE
        }
    }

    fun isValid(): Boolean {
        return !chatRoomID.isNullOrBlank()/* && !ownerID.isNullOrBlank() && participantIDs.isNotEmpty()*/
    }

    fun getRoomAvatar(id: String? = null): String? {
        return getParticipant(id)?.avatarURL
    }

    fun getRoomName(id: String? = null): String? {
        return getParticipant(id)?.completeName
    }

    fun getLastSeenDate(id: String? = null): Date? {
        return Utils.getDateFromDB(getParticipant(id)?.lastSeenDate)
    }

    fun getParticipantId(only: Boolean = true): String? {
        return if (!participantIDs.isNullOrEmpty() && only) participantIDs[0] else null
    }

    fun canVoiceCallParticipant(id: String? = null): Boolean {
        return getParticipant(id)?.canAudiocall() ?: false
    }

    fun canVideoCallParticipant(id: String? = null): Boolean {
        return getParticipant(id)?.canVideocall() ?: false
    }

    fun getParticipant(id: String? = null): HLUserGeneric? {
        return if (!participants.isNullOrEmpty()) {
            if (id == null) participants[0]
            else {
                val filtered = participants.filter { it.id == id }
                if (!filtered.isNullOrEmpty() && filtered.size == 1)
                    filtered[0]
                else null
            }
        } else null
    }

    //endregion


    //region == Realm listener ==

    override fun onChange(t: ChatRoom) {
        update(t)
    }

    override fun reset() {}

    override fun read(realm: Realm?): Any? {
        return null
    }

    override fun read(realm: Realm?, model: Class<out RealmModel>?): RealmModel? {
        return null
    }

    override fun deserializeStringListFromRealm() {}

    override fun serializeStringListForRealm() {}

    override fun write(realm: Realm?) {
        RealmUtils.writeToRealm(realm, this)
    }

    override fun write(`object`: Any?) {}

    override fun write(json: JSONObject?) {
        update(json)
    }

    override fun write(realm: Realm?, model: RealmModel?) {}

    override fun update() {}

    override fun update(`object`: Any?) {
        if (`object` is ChatRoom) {
            chatRoomID = `object`.chatRoomID
            ownerID = `object`.ownerID
            participantIDs = `object`.participantIDs
            participants = `object`.participants
            date = `object`.date
            recipientStatus = `object`.recipientStatus
            toRead = `object`.toRead
        }
    }

    override fun update(json: JSONObject?) {
        deserialize(json, ChatMessage::class.java)
    }

    override fun updateWithReturn(): RealmModelListener? {
        return null
    }

    override fun updateWithReturn(`object`: Any?): RealmModelListener {
        update(`object`)
        return this
    }

    override fun updateWithReturn(json: JSONObject?): RealmModelListener {
        return deserialize(json.toString(), ChatMessage::class.java) as RealmModelListener
    }

    //endregion



    //region == Json De-Serialization ==

    override fun serializeWithExpose(): JsonElement {
        return JsonHelper.serializeWithExpose(this)
    }

    override fun serializeToStringWithExpose(): String {
        return JsonHelper.serializeToStringWithExpose(this)
    }

    override fun serialize(): JsonElement {
        return JsonHelper.serialize(this)
    }

    override fun serializeToString(): String {
        return JsonHelper.serializeToString(this)
    }

    override fun deserialize(json: JSONObject?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(json, myClass)
    }

    override fun deserialize(json: JsonElement?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(json, myClass)
    }

    override fun deserialize(jsonString: String?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(jsonString, myClass)
    }

    override fun getSelfObject(): Any {
        return this
    }

    //endregion

}