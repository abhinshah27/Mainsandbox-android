/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Type;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.models.enums.RequestsStatusEnum;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 4/30/2018.
 */
@RealmClass
public class FamilyRelationship implements RealmModel, RealmModelListener, Serializable, JsonHelper.JsonDeSerializer,
		Comparable<FamilyRelationship> {

	private String familyRelationshipID;
	private String familyRelationshipName;
	private String status;

	private String userID;
	private String name;
	private String avatarURL;


	public FamilyRelationship() {}

	public FamilyRelationship(String famRelID, String famRelName) {
		this.familyRelationshipID = famRelID;
		this.familyRelationshipName = famRelName;
	}


	public FamilyRelationship deserializeToClass(JSONObject json) {
		return (FamilyRelationship) deserialize(json, FamilyRelationship.class);
	}

	@Override
	public int compareTo(@NonNull FamilyRelationship o) {
		if (Utils.areStringsValid(name, o.getName()))
			return name.compareTo(o.getName());
		return 0;
	}


	public RequestsStatusEnum getStatusEnum() {
		return RequestsStatusEnum.toEnum(getStatus());
	}


	public void completeRelation(GenericUserFamilyRels user) {
		setUserID(user.getId());
		setAvatarURL(user.getAvatarURL());
		setName(user.getCompleteName());
		setStatus(RequestsStatusEnum.PENDING.toString());
	}


	//region == Serialization section ==

	@Override
	public JsonElement serializeWithExpose() {
		return JsonHelper.serializeWithExpose(this);
	}

	@Override
	public String serializeToStringWithExpose() {
		return JsonHelper.serializeToStringWithExpose(this);
	}

	@Override
	public JsonElement serialize() {
		return JsonHelper.serialize(this);
	}

	@Override
	public String serializeToString() {
		return JsonHelper.serializeToString(this);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JSONObject json, Class myClass) {
		return null;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		return null;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
		return null;
	}

	@Override
	public Object getSelfObject() {
		return null;
	}



	public FamilyRelationship deserializeComplete(JSONObject json) {
		return deserializeComplete(json.toString());
	}

	public FamilyRelationship deserializeComplete(String jsonString) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(FamilyRelationship.class, new FamilyRelationship.FamilyRelationshipDeserializer())
				.create();
		return gson.fromJson(jsonString, FamilyRelationship.class);
	}


	private class FamilyRelationshipDeserializer  implements JsonDeserializer<FamilyRelationship> {

		@Override
		public FamilyRelationship deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
			return new FamilyRelationship(
					jsonObject.get("_id").getAsString(),
					jsonObject.get("name").getAsString()
			);
		}
	}

	//endregion


	//region == Realm model listener ==

	@Override
	public void reset() {}

	@Override
	public Object read(@Nullable Realm realm) {
		return null;
	}

	@Override
	public RealmModel read(Realm realm, Class<? extends RealmModel> model) {
		return null;
	}

	@Override
	public void deserializeStringListFromRealm() {}

	@Override
	public void serializeStringListForRealm() {}

	@Override
	public void write(@Nullable Realm realm) {}

	@Override
	public void write(Object object) {}

	@Override
	public void write(JSONObject json) {}

	@Override
	public void write(Realm realm, RealmModel model) {}

	@Override
	public void update() {}

	@Override
	public void update(Object object) {}

	@Override
	public void update(JSONObject json) {}

	@Override
	public RealmModelListener updateWithReturn() {
		return null;
	}

	@Override
	public RealmModelListener updateWithReturn(Object object) {
		return null;
	}

	@Override
	public RealmModelListener updateWithReturn(JSONObject json) {
		return null;
	}

	//endregion


	//region == Getters and setters ==

	public String getFamilyRelationshipID() {
		return familyRelationshipID;
	}
	public void setFamilyRelationshipID(String familyRelationshipID) {
		this.familyRelationshipID = familyRelationshipID;
	}

	public String getFamilyRelationshipName() {
		return familyRelationshipName;
	}
	public void setFamilyRelationshipName(String familyRelationshipName) {
		this.familyRelationshipName = familyRelationshipName;
	}

	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getAvatarURL() {
		return avatarURL;
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

	//endregion

}
