/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models

import io.realm.DynamicRealm
import io.realm.DynamicRealmObject
import io.realm.RealmMigration
import it.keybiz.lbsapp.corporate.models.enums.*
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType

/**
 * @author mbaldrighi on 5/24/2018.
 */
class LBSLinkMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        var oldVersion = oldVersion

        // DynamicRealm exposes an editable schema
        val schema = realm.schema

        // INFO: 2/15/19   restores clean Realm DB
        // INFO: 3/7/19    applies first RealmMigration

        if (oldVersion == 0L) {
            if (schema.contains("Post")) {
                val post = schema.get("Post")
                if (post != null) {
                    post.addRealmListField("lists", String::class.java)
                    post.addRealmListField("containers", String::class.java)
                }
            }

            oldVersion++
        }

        if (oldVersion == 1L) {
            if (schema.contains("HLUser")) {
                val user = schema.get("HLUser")
                user?.addField("docUrl", String::class.java)
            }

            oldVersion++
        }

        //
        //		if (oldVersion == 1) {
        //			if (!schema.contains("HLCircle")) {
        //				RealmObjectSchema userGen = schema.get("HLUserGeneric");
        //				if (userGen != null) {
        //					schema.create("HLCircle")
        //							.addField("id", String.class)
        //							.addField("name", String.class)
        ////							.addRealmListField("users", userGen)
        //							.addField("sortOrder", int.class)
        //							.addField("moreData", boolean.class)
        //							.addField("nameToDisplay", String.class);
        //
        //				}
        //			}
        //
        //			if (schema.contains("HLUser")) {
        //				RealmObjectSchema user = schema.get("HLUser");
        //				RealmObjectSchema circle = schema.get("HLCircle");
        //				if (user != null && circle != null) {
        //					user.addRealmListField("selectedFeedFilters", String.class);
        //					user.addRealmListField("circleObjects", circle);
        //				}
        //			}
        //
        //			oldVersion++;
        //		}
        //
        //		if (oldVersion == 2) {
        //			if (!schema.contains("Initiative")) {
        //				schema.create("Initiative")
        //						.addField("type", String.class)
        //						.addField("dateUpUntil", String.class)
        //						.addField("heartsToTransfer", long.class)
        //						.addField("recipient", String.class)
        //						.addField("text", String.class)
        //						.addField("dateCreation", String.class);
        //			}
        //
        //			if (schema.contains("Post")) {
        //				RealmObjectSchema post = schema.get("Post");
        //				RealmObjectSchema initiative = schema.get("Initiative");
        //				if (post != null && initiative != null) {
        //					post.addRealmObjectField("initiative", initiative);
        //					post.addField("isInitiative", boolean.class);
        //					post.addField("GSMessage", String.class);
        //					post.addField("GSRecipientID", String.class);
        //				}
        //			}
        //
        //			if (schema.contains("HLUser")) {
        //				RealmObjectSchema user = schema.get("HLUser");
        //				if (user != null) {
        //					user.addField("hasActiveGiveSupportInitiative", boolean.class);
        //				}
        //			}
        //
        //			if (schema.contains("HLIdentity")) {
        //				RealmObjectSchema identity = schema.get("HLIdentity");
        //				if (identity != null) {
        //					identity.addField("hasActiveGiveSupportInitiative", boolean.class);
        //					identity.addField("totHeartsAvailable", int.class);
        //				}
        //			}
        //
        //			if (schema.contains("Tag")) {
        //				RealmObjectSchema tag = schema.get("Tag");
        //				if (tag != null) {
        //					tag.addField("isInterest", boolean.class);
        //				}
        //			}
        //
        //			oldVersion++;
        //		}
        //
        //		if (oldVersion == 3) {
        //			if (schema.contains("Post")) {
        //				RealmObjectSchema post = schema.get("Post");
        //				if (post != null) {
        //					post.addField("followedInterestId", String.class);  // id of the interest newly followed
        //					post.addField("creationDate", Date.class);  // needed to have timeStamp even if post is never downloaded
        //					post.addField("youFollow", boolean.class);  // self-explanatory if post is from Interest
        //				}
        //
        //				RealmObjectSchema user = schema.get("HLUserSettings");
        //				if (user != null) {
        //					user.addField("overriddenSortOrder", Integer.class);
        //				}
        //
        //				RealmObjectSchema userGen = schema.get("HLUserGeneric");
        //				RealmObjectSchema familyRel = schema.get("FamilyRelationship");
        //				if (userGen != null && familyRel != null) {
        //					userGen.addRealmListField("familyRelationships", familyRel);
        //				}
        //			}
        //
        //			oldVersion++;
        //		}
        //
        //		/*
        //		 * Breaking migration: the new classes are written in Kotlin. This means that new non-optional
        //		 * Kotlin properties must be declared as "required" through setRequired(boolean) method.
        //		 */
        //		if (oldVersion == 4) {
        //			RealmObjectSchema userGen = null;
        //			if (schema.contains("HLUserGeneric")) {
        //				userGen = schema.get("HLUserGeneric");
        //				if (userGen != null) {
        //					userGen.addField("chatRoomID", String.class);
        //					userGen.addField("chatStatus", int.class);
        //					userGen.addField("lastSeenDate", String.class);
        //					userGen.addField("canChat", boolean.class, FieldAttribute.INDEXED);
        //					userGen.addField("canVideocall", boolean.class);
        //					userGen.addField("canAudiocall", boolean.class);
        //				}
        //			}
        //
        //			if (!schema.contains("ChatRoom") && userGen != null) {
        //				schema.create("ChatRoom")
        //						.addField("chatRoomID", String.class, FieldAttribute.PRIMARY_KEY)
        //						.addField("ownerID", String.class)
        //						.addRealmListField("participantIDs", String.class)
        //						.addRealmListField("participants", userGen)
        //						.addField("date", String.class).setRequired("date", true)
        //						.addField("dateObj", Date.class)
        //						.addField("text", String.class).setRequired("text", true)
        //						.addField("roomName", String.class)
        //						.addField("recipientStatus", int.class)
        //						.addField("toRead", int.class);
        //			}
        //
        //			if (!schema.contains("ChatMessage")) {
        //				schema.create("ChatMessage")
        //						.addField("messageID", String.class, FieldAttribute.PRIMARY_KEY).setRequired("messageID", true)
        //						.addField("senderID", String.class, FieldAttribute.INDEXED)
        //						.addField("recipientID", String.class)
        //						.addField("chatRoomID", String.class, FieldAttribute.INDEXED)
        //						.addField("unixtimestamp", Long.class, FieldAttribute.INDEXED)
        //
        //						.addField("messageType", int.class)
        //						.addField("isError", boolean.class)
        //
        //						.addField("creationDate", String.class).setRequired("creationDate", true)
        //						.addField("creationDateObj", Date.class, FieldAttribute.INDEXED).setRequired("creationDateObj", true)
        //						.addField("sentDate", String.class)
        //						.addField("sentDateObj", Date.class)
        //						.addField("deliveryDate", String.class)
        //						.addField("deliveryDateObj", Date.class)
        //						.addField("readDate", String.class)
        //						.addField("readDateObj", Date.class)
        //
        //						.addField("text", String.class).setRequired("text", true)
        //						.addField("mediaURL", String.class).setRequired("mediaURL", true)
        //						.addField("videoThumbnail", String.class)
        //						.addField("location", String.class).setRequired("location", true);
        //			}
        //
        //			if (!schema.contains("HLCacheObject")) {
        //				schema.create("HLCacheObject")
        //						.addField("id", String.class, FieldAttribute.PRIMARY_KEY).setRequired("id", true)
        //						.addField("creationDate", long.class, FieldAttribute.INDEXED)
        //						.addField("size", long.class);
        //			}
        //
        //			oldVersion++;
        //		}
        //
        //		if (oldVersion == 5) {
        //			if (schema.contains("PostWebLink"))
        //				schema.rename("PostWebLink", "HLWebLink");
        //
        //			RealmObjectSchema webLink = null;
        //			RealmObjectSchema message;
        //			if (schema.contains("HLWebLink")) {
        //				webLink = schema.get("HLWebLink");
        //				if (webLink != null) {
        //					webLink.addField("messageID", String.class);
        //				}
        //			}
        //			if (schema.contains("ChatMessage")) {
        //				message = schema.get("ChatMessage");
        //				if (message != null && webLink != null) {
        //					message.addField("sharedDocumentFileName", String.class).setRequired("sharedDocumentFileName", true)
        //							.addRealmObjectField("webLink", webLink)
        //							.addField("openedDate", String.class)
        //							.addField("openedDateObj", Date.class);
        //				}
        //			}
        //
        //            oldVersion++;
        //		}
        //
        //		if (oldVersion == 6) {
        //			if (schema.contains("Post")) {
        //
        //				RealmObjectSchema media = null;
        //				if (!schema.contains("MemoryMediaObject"))
        //					media = schema.create("MemoryMediaObject")
        //							.addField("mediaURL", String.class)
        //							.addField("contentMode", int.class, FieldAttribute.REQUIRED)
        //							.addField("type", String.class)
        //							.addField("thumbnailURL", String.class)
        //							.addField("frame", String.class);
        //
        //				RealmObjectSchema message = null;
        //				if (!schema.contains("MemoryMessageObject"))
        //					message = schema.create("MemoryMessageObject")
        //							.addField("message", String.class)
        //							.addField("textColor", String.class)
        //							.addField("textSize", String.class)
        //							.addField("textPosition", String.class)
        //							.addField("frame", String.class);
        //
        //				RealmObjectSchema post = schema.get("Post");
        //				if (post != null && message != null && media != null) {
        //					post.addField("backgroundColor", String.class)
        //							.addRealmListField("mediaObjects", media)
        //							.addRealmObjectField("messageObject", message)
        //							.transform(obj -> transformPost(realm, obj))
        //							.removeField("content")
        //							.removeField("videoThumbnail")
        //							.removeField("caption");
        //				}
        //			}
        //
        //            oldVersion++;
        //		}
    }


    private fun transformPost(realm: DynamicRealm, obj: DynamicRealmObject) {

        // values of interest for the entire method
        val type = obj.getString("type")
        val content = obj.getString("content")
        val hasMedia = hasPostMedia(type, content)

        // CAPTION to MESSAGE_OBJECT
        var messageObj = obj.getObject("messageObject")
        if (messageObj == null)
            messageObj = realm.createObject("MemoryMessageObject")
        if (messageObj != null) {
            messageObj.setString("message", obj.getString("caption"))
            messageObj.setString("textColor", MemoryColorEnum.WHITE.value)
            messageObj.setString("textSize", MemoryTextSizeEnum.TEXT_SIZE_MEDIUM.value)
            messageObj.setString(
                    "textPosition",
                    if (hasMedia) MemoryTextPositionEnum.BOTTOM_LEFT.value else MemoryTextPositionEnum.CENTER_LEFT.value
            )

            obj.setObject("messageObject", messageObj)
        }

        // CONTENT to MEDIA_OBJECT
        if (hasMedia) {
            val mediaObj = realm.createObject("MemoryMediaObject")
            if (mediaObj != null) {

                mediaObj.setString("mediaURL", content)

                if (isRealmPostAudio(type)) {
                    mediaObj.setString("type", HLMediaType.AUDIO.toString())
                } else if (isRealmPostPhoto(type)) {
                    mediaObj.setString("type", HLMediaType.PHOTO.toString())
                } else if (isRealmPostVideo(type)) {
                    mediaObj.setString("type", HLMediaType.VIDEO.toString())
                    mediaObj.setString("thumbnailURL", obj.getString("videoThumbnail"))
                }
                mediaObj.setInt("contentMode", FitFillTypeEnum.FILL.value)
                obj.getList("mediaObjects").add(mediaObj)
            }
        }
    }

    private fun hasPostMedia(type: String, content: String): Boolean {
        return Utils.isStringValid(content) && (isRealmPostAudio(type) || isRealmPostPhoto(type) || isRealmPostVideo(type))
    }

    private fun isRealmPostAudio(type: String): Boolean {
        return Utils.isStringValid(type) && type == HLMediaType.AUDIO.toString()
    }

    private fun isRealmPostPhoto(type: String): Boolean {
        return Utils.isStringValid(type) && (type == PostTypeEnum.PHOTO.toString() ||
                type == PostTypeEnum.PHOTO_WALL.toString() ||
                type == PostTypeEnum.PHOTO_PROFILE.toString() ||
                type == PostTypeEnum.FOLLOW_INTEREST.toString())
    }

    private fun isRealmPostVideo(type: String): Boolean {
        return Utils.isStringValid(type) && type == HLMediaType.VIDEO.toString()
    }

}