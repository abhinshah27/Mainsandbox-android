/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 4/30/2018.
 */
//@RealmClass
public class GenericUserFamilyRels implements /*RealmModel, RealmModelListener, */Serializable, Comparable<GenericUserFamilyRels>, JsonHelper.JsonDeSerializer {

	private String _id;
	private String firstName, lastName;
	private String avatarURL;

	private List<FamilyRelationship> allowedFamilyRels = new ArrayList<>();


	@Override
	public int compareTo(@NonNull GenericUserFamilyRels o) {
		if (Utils.areStringsValid(lastName, o.getLastName()))
			return lastName.compareTo(o.getLastName());
		return 0;
	}

	@Override
	public int hashCode() {
		if (Utils.isStringValid(_id))
			return _id.hashCode();
		return super.hashCode();
	}


	public GenericUserFamilyRels deserializeToClass(JSONObject json) {
		return (GenericUserFamilyRels) deserialize(json, GenericUserFamilyRels.class);
	}

	public boolean hasAllowedRelationships() {
		return allowedFamilyRels != null && !allowedFamilyRels.isEmpty();
	}

	public String getCompleteName() {
		if (Utils.areStringsValid(getFirstName(), getLastName())) {
			return getFirstName() + " " + getLastName();
		} else {
			if (Utils.isStringValid(getFirstName()))
				return getFirstName();
			else if (Utils.isStringValid(getLastName()))
				return getLastName();
			else
				return "";
		}
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



	public GenericUserFamilyRels deserializeComplete(JSONObject json) {
		return deserializeComplete(json.toString());
	}

	public GenericUserFamilyRels deserializeComplete(String jsonString) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(GenericUserFamilyRels.class, new GenericUserFamilyRels.FamilyRelationshipDeserializer())
				.create();
		return gson.fromJson(jsonString, GenericUserFamilyRels.class);
	}


	private class FamilyRelationshipDeserializer  implements JsonDeserializer<GenericUserFamilyRels> {

		@Override
		public GenericUserFamilyRels deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

			if (!json.isJsonNull()) {
				GenericUserFamilyRels user = new GenericUserFamilyRels();

				JsonObject jsonObject = json.getAsJsonObject();
				if (jsonObject.has("_id"))
					user.setId(jsonObject.get("_id").getAsString());
				if (jsonObject.has("firstName"))
					user.setFirstName(jsonObject.get("firstName").getAsString());
				if (jsonObject.has("lastName"))
					user.setLastName(jsonObject.get("lastName").getAsString());
				if (jsonObject.has("avatarURL"))
					user.setAvatarURL(jsonObject.get("avatarURL").getAsString());

				if (jsonObject.has("allowedFamilyRels")) {
					JsonArray array = jsonObject.getAsJsonArray("allowedFamilyRels");
					user.setAllowedFamilyRels(array);
				}

				return user;
			}

			return null;
		}
	}

	//endregion


	/*
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
	public void deserializeStringListFromRealm() throws JSONException {}

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
	*/


	//region == Getters and setters ==

	public String getId() {
		return _id;
	}
	public void setId(String _id) {
		this._id = _id;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String name) {
		this.firstName = name;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String name) {
		this.lastName = name;
	}

	public String getAvatarURL() {
		return avatarURL;
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public List<FamilyRelationship> getAllowedFamilyRels() {
		return allowedFamilyRels;
	}
	public void setAllowedFamilyRels(List<FamilyRelationship> allowedFamilyRels) {
		this.allowedFamilyRels = allowedFamilyRels;
	}
	private void setAllowedFamilyRels(JsonArray array) {
		if (allowedFamilyRels == null)
			allowedFamilyRels = new ArrayList<>();
		else
			allowedFamilyRels.clear();

		if (!array.isJsonNull() && array.size() > 0) {
			for (JsonElement jel : array) {
				JsonObject jo = jel.getAsJsonObject();
				if (!jo.isJsonNull()) {
					FamilyRelationship fr = new FamilyRelationship(
							jo.get("_id").getAsString(),
							jo.get("name").getAsString()
					);

					allowedFamilyRels.add(fr);
				}
			}
		}
	}

	//endregion

}
