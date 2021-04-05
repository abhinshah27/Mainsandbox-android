/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models

import android.content.res.Resources
import androidx.annotation.ColorInt
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import io.realm.Realm
import io.realm.RealmModel
import io.realm.annotations.RealmClass
import it.keybiz.lbsapp.corporate.models.enums.MemoryColorEnum
import it.keybiz.lbsapp.corporate.models.enums.MemoryTextPositionEnum
import it.keybiz.lbsapp.corporate.models.enums.MemoryTextSizeEnum
import org.json.JSONObject


@RealmClass
open class MemoryMessageObject: RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer {

    companion object {

        fun getMemoryMessage(json: JSONObject?): MemoryMessageObject {

            val message = JsonHelper.deserialize(json, MemoryMessageObject::class.java) as MemoryMessageObject

            // TODO  add any possible customization

            return message
        }

    }

    @Expose var message: String? = ""
    @Expose var textColor: String? = MemoryColorEnum.WHITE.value
    @Expose var textSize: String? = MemoryTextSizeEnum.TEXT_SIZE_MEDIUM.value
    @Expose var textPosition: String? = MemoryTextPositionEnum.TOP_LEFT.value
    @Expose private var frame: String? = ""



    @ColorInt fun getColor(resources: Resources): Int {
        return MemoryColorEnum.getColor(resources, textColor)
    }
    fun getSize(): Float {
        return MemoryTextSizeEnum.getSize(textSize)
    }
    fun getPosition(): Int {
        return MemoryTextPositionEnum.getGravityPosition(textPosition)
    }



    //region == Realm listener ==

    override fun reset() {
        message = null
        textColor = MemoryColorEnum.WHITE.value
        textSize = MemoryTextSizeEnum.TEXT_SIZE_MEDIUM.value
        textPosition = MemoryTextPositionEnum.TOP_LEFT.value
        frame = null
    }

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
        if (`object` is MemoryMessageObject) {
            message = `object`.message
            textColor = `object`.textColor
            textSize = `object`.textSize
            textPosition = `object`.textPosition
            frame = `object`.frame
        }
    }

    override fun update(json: JSONObject?) {
        deserialize(json, MemoryMediaObject::class.java)
    }

    override fun updateWithReturn(): RealmModelListener? {
        return null
    }

    override fun updateWithReturn(`object`: Any?): RealmModelListener {
        update(`object`)
        return this
    }

    override fun updateWithReturn(json: JSONObject?): RealmModelListener {
        return deserialize(json.toString(), MemoryMediaObject::class.java) as RealmModelListener
    }

    //endregion


    //region == De-Serializer ==

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