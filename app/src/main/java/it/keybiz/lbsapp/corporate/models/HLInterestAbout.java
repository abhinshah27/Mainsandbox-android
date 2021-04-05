/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONObject;

import java.lang.reflect.Type;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;

/**
 * The nested object containing the user's info about birthday, relationship status, city, and description.
 *
 * @author mbaldrighi on 1/18/2018.
 */
@RealmClass
public class HLInterestAbout implements RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer {

	private String description;
	private String note;


	public HLInterestAbout() {}

	public HLInterestAbout(String description, String note) {
		setDescription(description);
		setNote(note);
	}

	//region == Realm model methods ==

	@Override
	public void reset() {}

	@Override
	public Object read(Realm realm) {
		return read(realm, HLInterestAbout.class);
	}

	@Override
	public RealmModel read(Realm realm, Class<? extends RealmModel> model) {
		return RealmUtils.readFirstFromRealm(realm, this.getClass());
	}

	@Override
	public void deserializeStringListFromRealm() {}

	@Override
	public void serializeStringListForRealm() {}

	@Override
	public void write(Realm realm) {
		RealmUtils.writeToRealm(realm,this);
	}

	@Override
	public void write(JSONObject json) {}

	@Override
	public void write(Object object) {}

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


	//region == Serialization methods ==

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
		return JsonHelper.deserialize(json, myClass);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		return JsonHelper.deserialize(json, myClass);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
		return JsonHelper.deserialize(jsonString, myClass);
	}

	@Override
	public Object getSelfObject() {
		return this;
	}


	public HLInterestAbout deserializeComplete(JSONObject json) {
		return deserializeComplete(json.toString());
	}

	public HLInterestAbout deserializeComplete(String jsonString) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(HLInterestAbout.class, new AboutMeDeserializer())
				.create();
		return gson.fromJson(jsonString, HLInterestAbout.class);
	}


	private class AboutMeDeserializer  implements JsonDeserializer<HLInterestAbout> {

		@Override
		public HLInterestAbout deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

			Gson gson = new Gson();
			HLInterestAbout user = gson.fromJson(json, HLInterestAbout.class);
			JsonObject jsonObject = json.getAsJsonObject();
			if (jsonObject.has("about")) {
				JsonObject elem = jsonObject.getAsJsonObject("about");
				if (elem != null && !elem.isJsonNull()) {

				}
			}

			return user;
		}
	}

	//endregion


	//region == Getters and setters ==

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}

	//endregion

}
