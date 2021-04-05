/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.chat

import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import it.keybiz.lbsapp.corporate.models.HLWebLink
import it.keybiz.lbsapp.corporate.models.JsonHelper
import it.keybiz.lbsapp.corporate.models.RealmModelListener
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType
import org.json.JSONObject
import java.util.*

/**
 * Enum class representing all the potential types of a [ChatMessage] object.
 * @author mbaldrighi on 10/26/2018.
 */
enum class ChatMessageType(val value: Int) {
    TEXT(0), VIDEO(1), PICTURE(2), LOCATION(3), AUDIO(4), DOCUMENT(6);

    fun toHLMediaTypeEnum(): HLMediaType? {
        return when (this) {
            VIDEO -> HLMediaType.VIDEO
            PICTURE -> HLMediaType.PHOTO
            AUDIO -> HLMediaType.AUDIO
            DOCUMENT -> HLMediaType.DOCUMENT
            else -> null
        }
    }
}

/**
 * Class representing the instance of a chat message.
 * @author mbaldrighi on 10/15/2018.
 */
@RealmClass
open class ChatMessage: RealmModel, RealmChangeListener<ChatMessage>, RealmModelListener, JsonHelper.JsonDeSerializer {

    enum class DirectionType { INCOMING, OUTGOING }
    enum class Status { SENDING, SENT, DELIVERED, READ, ERROR, OPENED }

    companion object {
        fun getMessage(json: JSONObject?): ChatMessage {
            val message = JsonHelper.deserialize(json, ChatMessage::class.java) as ChatMessage

            message.creationDateObj = Utils.getDateFromDB(message.creationDate)
            message.sentDateObj = Utils.getDateFromDB(message.sentDate)
            message.deliveryDateObj = Utils.getDateFromDB(message.deliveryDate)
            message.readDateObj = Utils.getDateFromDB(message.readDate)
            message.openedDateObj = Utils.getDateFromDB(message.openedDate)

            return message
        }

        fun handleUnsentMessages(realm: Realm): Boolean {
            val nullLong: Long? = null
            val unsent = realm.where(ChatMessage::class.java)
                    .equalTo("unixtimestamp", nullLong)
                    .or()
                    .equalTo("unixtimestamp", 0L)
                    .findAll()

            return if (unsent.isNotEmpty()) {
                realm.executeTransaction {
                    for (i in unsent) i.isError = true
                }
                true
            }
            else false
        }
    }



    /* IDs */
    @Expose @PrimaryKey var messageID: String = UUID.randomUUID().toString()
    @Expose @Index var senderID: String? = null
    @Expose var recipientID: String? = null
    @Expose @Index var chatRoomID: String? = null
    @Index var unixtimestamp: Long? = null

    @Expose var messageType: Int = 0

    var isError: Boolean = false

    /* Dates */
    @Expose var creationDate: String = Utils.formatDateForDB(System.currentTimeMillis())
    @Index var creationDateObj: Date = Date()
        set(value) {
            field = value
            if (creationDate.isBlank()) creationDate = Utils.formatDateForDB(value)
        }
    var sentDate: String? = null
    var sentDateObj: Date? = null
        set(value) {
            field = value
            if (sentDate.isNullOrBlank()) sentDate = Utils.formatDateForDB(value)
        }
    @Expose var deliveryDate: String? = null
    var deliveryDateObj: Date? = null
        set(value) {
            field = value
            if (deliveryDate.isNullOrBlank()) deliveryDate = Utils.formatDateForDB(value)
        }
    var readDate: String? = null
    var readDateObj: Date? = null
        set(value) {
            field = value
            if (readDate.isNullOrBlank()) readDate = Utils.formatDateForDB(value)
        }
    var openedDate: String? = null
    var openedDateObj: Date? = null
        set(value) {
            field = value
            if (openedDate.isNullOrBlank()) openedDate = Utils.formatDateForDB(value)
        }

    /* Content */
    @Expose var text: String = ""
    @Expose var mediaURL: String = ""
    var videoThumbnail: String? = null
    @Expose var location: String = ""
    @Expose var sharedDocumentFileName: String = ""
    var webLink: HLWebLink? = null


    //region == Class methods ==

    fun isSameDateAs(message: ChatMessage?): Boolean {
        val cal = Calendar.getInstance()
        val calOther = Calendar.getInstance()

        cal.time = creationDateObj
        if (message?.creationDateObj == null) return false
        calOther.time = message.creationDateObj

        return cal[Calendar.YEAR] == calOther[Calendar.YEAR] &&
                cal[Calendar.MONTH] == calOther[Calendar.MONTH] &&
                cal[Calendar.DAY_OF_MONTH] == calOther[Calendar.DAY_OF_MONTH]
    }

    fun isSameType(message: ChatMessage?, currentUserId: String): Boolean {
        return getDirectionType(currentUserId) == message?.getDirectionType(currentUserId)
    }

    fun hasMedia(): Boolean {
        return !mediaURL.isBlank()
    }

    fun hasPicture(): Boolean {
        return getMessageType() == ChatMessageType.PICTURE
    }

    fun hasVideo(): Boolean {
        return getMessageType() == ChatMessageType.VIDEO
    }

    fun hasAudio(): Boolean {
        return getMessageType() == ChatMessageType.AUDIO
    }

    fun hasLocation(): Boolean {
        return getMessageType() == ChatMessageType.LOCATION && location.isNotBlank()
    }

    fun hasDocument(): Boolean {
        return getMessageType() == ChatMessageType.DOCUMENT && sharedDocumentFileName.isNotBlank()
    }

    fun hasWebLink(): Boolean {
        return webLink != null
    }

    fun getMessageType(): ChatMessageType {
        return when (messageType) {
            0 -> ChatMessageType.TEXT
            1 -> ChatMessageType.VIDEO
            2 -> ChatMessageType.PICTURE
            3 -> ChatMessageType.LOCATION
            4 -> ChatMessageType.AUDIO
//            5 -> SYSTEM_MESSAGES ??
            6 -> ChatMessageType.DOCUMENT
            else -> ChatMessageType.TEXT
        }
    }

    fun getDirectionType(currentUserId: String): DirectionType {
        return if (currentUserId == senderID) DirectionType.OUTGOING else DirectionType.INCOMING
    }

    fun getStatusEnum(): Status {
        return when {
            !openedDate.isNullOrBlank() -> Status.OPENED
            !readDate.isNullOrBlank() -> Status.READ
            !deliveryDate.isNullOrBlank() -> Status.DELIVERED
            unixtimestamp != null && unixtimestamp!! > 0 -> Status.SENT
            (unixtimestamp == null || unixtimestamp == 0L) && isError -> Status.ERROR
            else -> Status.SENDING
        }
    }

    fun isRead(): Boolean {
        return getStatusEnum() == Status.READ
    }

    fun isDelivered(): Boolean {
        return getStatusEnum() == Status.DELIVERED
    }

    fun isSent(): Boolean {
        return getStatusEnum() == Status.SENT
    }

    fun isOpened(): Boolean {
        return getStatusEnum() == Status.OPENED
    }

    fun getNewOutgoingMessage(senderId: String? = null,
                              recipientId: String? = null,
                              chatRoomId: String? = null,
                              text: String = "",
                              type: Int = 0,
                              mediaPayload: String = "",
                              initialized: Boolean = false,
                              fileName: String = ""): ChatMessage {

        return this.apply {

            if (!initialized) {
                this.senderID = senderId
                this.recipientID = recipientId
                this.chatRoomID = chatRoomId
            }
            this.messageType = type
            this.text = text

            if (mediaPayload.isNotBlank()) {
                when (type) {
                    ChatMessageType.LOCATION.value -> this.location = mediaPayload
                    ChatMessageType.DOCUMENT.value -> {
                        this.mediaURL = mediaPayload
                        this.sharedDocumentFileName = fileName
                    }
                    else -> this.mediaURL = mediaPayload
                }
            }
        }
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


    //region == Realm listener ==

    override fun onChange(t: ChatMessage) {
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

    override fun write(realm: Realm?) {}

    override fun write(`object`: Any?) {}

    override fun write(json: JSONObject?) {
        update(json)
    }

    override fun write(realm: Realm?, model: RealmModel?) {}

    override fun update() {}

    override fun update(`object`: Any?) {
        if (`object` is ChatMessage) {
            messageID = `object`.messageID
            senderID = `object`.senderID
            recipientID = `object`.recipientID
            chatRoomID = `object`.chatRoomID
            unixtimestamp = `object`.unixtimestamp

            messageType = `object`.messageType

            creationDate = `object`.creationDate
            creationDateObj = `object`.creationDateObj
            sentDate = `object`.sentDate
            sentDateObj = if (`object`.sentDateObj != null) `object`.sentDateObj else Utils.getDateFromDB(`object`.sentDate)
            deliveryDate = `object`.deliveryDate
            deliveryDateObj = if (`object`.deliveryDateObj != null) `object`.deliveryDateObj else Utils.getDateFromDB(`object`.deliveryDate)
            readDate = `object`.readDate
            readDateObj = if (`object`.readDateObj != null) `object`.readDateObj else Utils.getDateFromDB(`object`.readDate)
            openedDate = `object`.openedDate
            openedDateObj = if (`object`.openedDateObj != null) `object`.openedDateObj else Utils.getDateFromDB(`object`.openedDate)

            text = `object`.text
            mediaURL = `object`.mediaURL
            videoThumbnail = `object`.videoThumbnail
            location = `object`.location
            sharedDocumentFileName = `object`.sharedDocumentFileName
            webLink = `object`.webLink
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

}