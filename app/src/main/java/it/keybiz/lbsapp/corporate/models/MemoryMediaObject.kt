/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models

import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import io.realm.Realm
import io.realm.RealmModel
import io.realm.annotations.RealmClass
import it.keybiz.lbsapp.corporate.models.enums.FitFillTypeEnum
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType
import org.json.JSONObject


@RealmClass
open class MemoryMediaObject: RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer {

    companion object {

        fun getMemoryMedia(json: JSONObject?): MemoryMediaObject {

            val media = JsonHelper.deserialize(json, MemoryMediaObject::class.java) as MemoryMediaObject

            // TODO  add any possible customization

            return media
        }

    }

    @Expose var mediaURL: String? = ""
    @Expose var thumbnailURL: String? = ""
    @Expose var type: String? = ""
    @Expose var contentMode: Int = FitFillTypeEnum.FIT.value
    @Expose private var frame: String? = ""


    fun isValid(): Boolean {
        return !mediaURL.isNullOrBlank() && !type.isNullOrBlank() &&
                (
                        type == HLMediaType.VIDEO.toString() ||
                                type == HLMediaType.PHOTO.toString() ||
                                type == HLMediaType.AUDIO.toString()
                        )
    }

    fun isFit(): Boolean { return FitFillTypeEnum.toEnum(contentMode) == FitFillTypeEnum.FIT }
    fun isFill(): Boolean { return FitFillTypeEnum.toEnum(contentMode) == FitFillTypeEnum.FILL }

    fun getTypeEnum(): HLMediaType? {
        return when (type) {
            HLMediaType.AUDIO.toString() -> HLMediaType.AUDIO
            HLMediaType.PHOTO.toString() -> HLMediaType.PHOTO
            HLMediaType.VIDEO.toString() -> HLMediaType.VIDEO
            else -> null
        }
    }

    fun isAudio(): Boolean {
        return type == HLMediaType.AUDIO.toString()
    }
    fun isPhoto(): Boolean {
        return type == HLMediaType.PHOTO.toString()
    }
    fun isVideo(): Boolean {
        return type == HLMediaType.VIDEO.toString()
    }

    fun setTypeFromPost(postType: String) {
        type = when (postType) {
            PostTypeEnum.AUDIO.value -> HLMediaType.AUDIO.value

            PostTypeEnum.PHOTO.value,
            PostTypeEnum.PHOTO_PROFILE.value,
            PostTypeEnum.PHOTO_WALL.value,
            PostTypeEnum.FOLLOW_INTEREST.value -> HLMediaType.PHOTO.value

            PostTypeEnum.VIDEO.value -> HLMediaType.VIDEO.value
            else -> null
        }
    }


    //region == Realm listener ==

    override fun reset() {
        mediaURL = ""
        contentMode = FitFillTypeEnum.FIT.value
        type = ""
        thumbnailURL = ""
        frame = ""
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
        if (`object` is MemoryMediaObject) {
            mediaURL = `object`.mediaURL
            contentMode = `object`.contentMode
            type = `object`.type
            thumbnailURL = `object`.thumbnailURL
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