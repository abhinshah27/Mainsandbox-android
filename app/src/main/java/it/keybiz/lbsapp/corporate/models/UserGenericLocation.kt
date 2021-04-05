/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models

import com.google.gson.JsonElement
import it.keybiz.lbsapp.corporate.utilities.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

/**
 * @author mbaldrighi on 2/18/2019.
 */
class UserGenericLocation : Serializable, JsonHelper.JsonDeSerializer, Comparable<UserGenericLocation> {

    companion object {

        @JvmStatic fun getUserWithLocation(json: JSONObject): UserGenericLocation {
            return JsonHelper.deserialize(json, UserGenericLocation::class.java) as UserGenericLocation
        }

        // INFO: 2/20/19    TEST
        @JvmStatic fun getTestList(): JSONArray {
            return JSONArray()
                    .put(0, UserGenericLocation().let { it.userID = "0"; it.name = "Ciccio0"; it.distance = "0.1 Km"; it.serializeToString()})
                    .put(1, UserGenericLocation().let { it.userID = "1"; it.name = "Ciccio1"; it.distance = "0.2 Km"; it.serializeToString()})
                    .put(2, UserGenericLocation().let { it.userID = "2"; it.name = "Ciccio2"; it.distance = "0.3 Km"; it.serializeToString()})
                    .put(3, UserGenericLocation().let { it.userID = "3"; it.name = "Ciccio3"; it.distance = "0.4 Km"; it.serializeToString()})
                    .put(4, UserGenericLocation().let { it.userID = "4"; it.name = "Ciccio4"; it.distance = "0.5 Km"; it.serializeToString()})
        }

    }

    var userID: String? = null
    var name: String? = null
    var avatarURL: String? = null
    var distance: String? = null


    override fun compareTo(other: UserGenericLocation): Int {
        return if (Utils.areStringsValid(this.name, other.name)) this.name!!.compareTo(other.name!!) else 0
    }

    override fun hashCode(): Int {
        return if (!userID.isNullOrBlank()) userID.hashCode() else super.hashCode()
    }


    //region == Serialization methods ==

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
        return JsonHelper.deserialize(json, myClass)
    }

    override fun deserialize(json: JsonElement, myClass: Class<*>): JsonHelper.JsonDeSerializer? {
        return null
    }

    override fun deserialize(jsonString: String, myClass: Class<*>): JsonHelper.JsonDeSerializer? {
        return JsonHelper.deserialize(jsonString, myClass)
    }

    override fun getSelfObject(): Any {
        return this
    }

    //endregion

}
