package it.keybiz.lbsapp.corporate.models

import com.google.gson.JsonElement
import org.json.JSONObject

class WishEmail(val sender: String, val emailTo: String, val subject: String, val message: String): JsonHelper.JsonDeSerializer {

    var id: String? = null
    var profilePictureAttached: Boolean = false
    var attachments: List<String>? = ArrayList()


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

}