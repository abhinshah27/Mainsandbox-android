/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.Context;
import android.content.res.Resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Arrays;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * The nested object containing the user's info about gender, birthplace, "interest", languages, address, and other names.
 *
 * @author mbaldrighi on 4/28/2017.
 */
@RealmClass
public class HLUserAboutMeMore implements RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer {

	private String birthPlace;
	private String sex;
	private String interestedIn;
	private RealmList<String> languages = new RealmList<>();
	private String otherNames;
	private String address;


	public HLUserAboutMeMore() {}

	public HLUserAboutMeMore(Context context, String birthPlace, String sex, String interestedIn, String languages, String otherNames, String address) {
		this.birthPlace = birthPlace;
		this.sex = sex;
		this.interestedIn = interestedIn;
		this.languages = parseLanguagesString(context, languages);
		this.otherNames = otherNames;
		this.address = address;
	}

	public HLUserAboutMeMore(String birthPlace, String sex, String interestedIn, RealmList<String> languages, String otherNames, String address) {
		this.birthPlace = birthPlace;
		this.sex = sex;
		this.interestedIn = interestedIn;
		this.languages = languages;
		this.otherNames = otherNames;
		this.address = address;
	}


	public int getSpinnerPositionInArray(Resources res, boolean interest) {
		if (res != null && Utils.isStringValid(interest ? getInterestedIn() : getGender())) {
			String[] array = res.getStringArray(interest ? R.array.profile_more_interestedin : R.array.profile_more_gender);
			for (int i = 0; i < array.length; i++) {
				String s = getGender();
				if (interest)
					s = getInterestedIn();
				if (s.equalsIgnoreCase(array[i]))
					return interest ? (i + 1) : i;
			}
		}

		return interest ? -1 : 0;
	}

	private RealmList<String> parseLanguagesString(Context context, String serialized) {
		RealmList<String> result = new RealmList<>();
		if (Utils.isStringValid(serialized)) {
			if (!serialized.contains(context.getString(R.string.user_detail_hint_languages))) {
				String[] parsed = serialized.trim().split(",\\s");
				result.addAll(Arrays.asList(parsed));
			}
		}

		return result;
	}

	public String getSerializedLanguagesString() {
		return Utils.formatStringFromLanguages(languages);
	}


	//region == Realm model methods ==

	@Override
	public void reset() {}

	@Override
	public Object read(Realm realm) {
		return read(realm, HLUserAboutMeMore.class);
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


	public HLUserAboutMeMore deserializeComplete(JSONObject json) {
		return deserializeComplete(json.toString());
	}

	public HLUserAboutMeMore deserializeComplete(String jsonString) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(HLUserAboutMeMore.class, new MoreAboutMeDeserializer())
				.create();
		return gson.fromJson(jsonString, HLUserAboutMeMore.class);
	}


	private class MoreAboutMeDeserializer  implements JsonDeserializer<HLUserAboutMeMore> {

		@Override
		public HLUserAboutMeMore deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

			Gson gson = new Gson();
			HLUserAboutMeMore user = gson.fromJson(json, HLUserAboutMeMore.class);
			JsonObject jsonObject = json.getAsJsonObject();
			if (jsonObject.has("moreAboutMe")) {
				JsonObject elem = jsonObject.getAsJsonObject("moreAboutMe");
				if (elem != null && !elem.isJsonNull()) {
					user.setGender(elem.get("sex").getAsString());
					user.setInterestedIn(elem.get("interestedIn").getAsString());
					user.setLanguages(elem.get("Languages").getAsJsonArray());
					user.setBirthPlace(elem.get("birthPlace").getAsString());
					user.setOtherNames(elem.get("otherNames").getAsString());
					user.setAddress(elem.get("address").getAsString());
				}
			}

			return user;
		}
	}

	//endregion


	//region == Getters and setters ==

	public String getGender() {
		return sex;
	}

	public void setGender(String sex) {
		this.sex = sex;
	}

	public String getInterestedIn() {
		return interestedIn;
	}

	public void setInterestedIn(String interestedIn) {
		this.interestedIn = interestedIn;
	}

	public RealmList<String> getLanguages() {
		return languages;
	}
	public void setLanguages(RealmList<String> languages) {
		this.languages = languages;
	}
	public void setLanguages(JsonArray jsonArray) {
		if (languages == null)
			languages = new RealmList<>();
		else
			languages.clear();

		if (jsonArray != null && jsonArray.size() > 0) {
			for (JsonElement jEl : jsonArray) {
				if (jEl != null) {
					languages.add(jEl.getAsString());
				}
			}
		}

	}

	public String getOtherNames() {
		return otherNames;
	}

	public void setOtherNames(String otherNames) {
		this.otherNames = otherNames;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getBirthPlace() {
		return birthPlace;
	}
	public void setBirthPlace(String birthPlace) {
		this.birthPlace = birthPlace;
	}


	//endregion

}
