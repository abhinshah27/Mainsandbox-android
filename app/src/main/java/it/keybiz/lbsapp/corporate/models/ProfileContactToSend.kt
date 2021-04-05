package it.keybiz.lbsapp.corporate.models

import android.content.ContentUris
import android.content.Context
import android.provider.ContactsContract
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import it.keybiz.lbsapp.corporate.features.profile.ContactsAdapter
import it.keybiz.lbsapp.corporate.utilities.Utils
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*

class ProfileContactToSend constructor(id: String?, val name: String): JsonHelper.JsonDeSerializer, Comparable<ProfileContactToSend> {

    private var id: Long = id?.toLong() ?: 0

    constructor(context: Context, adapter: ContactsAdapter, id: Long, name: String): this(id.toString(), name) {
        this.id = id
//        setPhoto(context, adapter)
    }


    private var photo: ByteArray? = null

    @Expose
    private var phones: MutableList<String>? = ArrayList()

    @SerializedName("mails")
    @Expose
    private var emails: MutableList<String>? = ArrayList()

    private var processed: Boolean = false



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileContactToSend

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.toString().hashCode()
    }

    override fun compareTo(other: ProfileContactToSend): Int {
        return if (Utils.areStringsValid(name, other.name)) name.compareTo(other.name) else 0
    }


    fun hasPhones(): Boolean {
        return phones != null && !phones!!.isEmpty()
    }

    fun hasEmails(): Boolean {
        return emails != null && !emails!!.isEmpty()
    }

    fun getFirstNumber(): String? {
        return if (phones != null && !phones!!.isEmpty()) phones!![0] else null
    }

    fun getFirstAddress(): String? {
        if (emails != null && !emails!!.isEmpty()) {
            for (email in emails!!) {
                if (Utils.isEmailValid(email))
                    return email
            }
        }

        return null
    }

    fun addEmail(address: String) {
        var nAddress = address
        if (Utils.isStringValid(nAddress)) {
            if (emails == null) {
                emails = ArrayList()
            }
            nAddress = nAddress.replace("\\s".toRegex(), "").trim { it <= ' ' }
            emails!!.add(nAddress)
        }
    }

    fun addPhone(number: String) {
        var nNumber = number
        if (Utils.isStringValid(nNumber)) {
            if (phones == null) {
                phones = ArrayList()
            }
            nNumber = nNumber.replace("\\s".toRegex(), "").trim { it <= ' ' }
            phones!!.add(nNumber)
        }
    }

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

    override fun deserialize(json: JSONObject, myClass: Class<*>): JsonHelper.JsonDeSerializer? {
        return null
    }

    override fun deserialize(json: JsonElement, myClass: Class<*>): JsonHelper.JsonDeSerializer? {
        return null
    }

    override fun deserialize(jsonString: String, myClass: Class<*>): JsonHelper.JsonDeSerializer? {
        return null
    }

    override fun getSelfObject(): Any {
        return this
    }


    private fun getFormattedPhoneNumber(phoneNumber: String): String {
        // matches any non-digit character and replaces it with "".
        return phoneNumber.replace("[^\\d]".toRegex(), "").trim { it <= ' ' }
    }


    //region == Getters and setters ==

    fun getPhones(): List<String>? {
        if (!processed) {
            val filtered = ArrayList<String>()
            val helper = ArrayList<String>()
            if (phones != null && !phones!!.isEmpty()) {
                for (num in phones!!) {
                    val formatted = getFormattedPhoneNumber(num)
                    if (!helper.contains(formatted)) {
                        helper.add(formatted)
                        filtered.add(num)
                    }
                }
                phones!!.clear()
                phones!!.addAll(filtered)
                processed = true
            }
        }

        return phones
    }

    fun setPhones(phones: MutableList<String>) {
        this.phones = phones
    }

    fun getEmails(): List<String> {
        return ArrayList(HashSet(emails))
    }

    fun setEmails(emails: MutableList<String>) {
        this.emails = emails
    }

    fun getPhoto(): ByteArray? {
        return photo
    }

    @Throws(IOException::class)
    fun setPhoto(photo: ByteArrayInputStream) {
        val array = ByteArray(photo.available())
        photo.read(array)
        this.photo = array
    }

    private fun setPhoto(photo: ByteArray) {
        this.photo = photo
    }

    fun setPhoto(context: Context) {
        val stream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver,
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id))
        try {
            if (stream != null) {
                val array = ByteArray(stream.available())
                stream.read(array)
                setPhoto(array)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    //endregion

}