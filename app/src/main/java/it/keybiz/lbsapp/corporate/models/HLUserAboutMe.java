/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * The nested object containing the user's info about birthday, relationship status, city, and description.
 *
 * @author mbaldrighi on 12/7/2017.
 */
@RealmClass
public class HLUserAboutMe implements RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer {

	private String birthdate;
	private transient Date birthdayDate;
	private String status;
	private String birthplace;
	 private String description;


	public HLUserAboutMe() {}

	public HLUserAboutMe(String birthdate, String status, String birtplace, String description) {
		setBirthday(birthdate);
		if (Utils.isStringValid(getBirthday()))
			setBirthdayDate(Utils.getDateFromDB(getBirthday()));
		setStatus(status);
		setCity(birtplace);
		setDescription(description);
	}


	public String getFormattedDate() {
		if (getBirthdayDate() == null && Utils.isStringValid(getBirthday())) {
			Realm realm = null;
			try {
				realm = RealmUtils.getCheckedRealm();
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						setBirthdayDate(Utils.getDateFromDB(getBirthday()));
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RealmUtils.closeRealm(realm);
			}
		}

		if (getBirthdayDate() != null)
			return new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(getBirthdayDate());

		return "";
	}

	public static Date formatNewDate(String dateString) throws ParseException {
		if (Utils.isStringValid(dateString))
			return new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).parse(dateString);

		return null;
	}

	public int getStatusPositionInArray(Resources res) {
		if (res != null && Utils.isStringValid(getStatus())) {
			String[] array = res.getStringArray(R.array.relationship_statuses);
			for (int i = 0; i < array.length; i++) {
				if (getStatus().equalsIgnoreCase(array[i]))
					return i + 1;
			}
		}

		return -1;
	}


	//region == Realm model methods ==

	@Override
	public void reset() {}

	@Override
	public Object read(Realm realm) {
		return read(realm, HLUserAboutMe.class);
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


	public HLUserAboutMe deserializeComplete(JSONObject json) {
		return deserializeComplete(json.toString());
	}

	public HLUserAboutMe deserializeComplete(String jsonString) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(HLUserAboutMe.class, new AboutMeDeserializer())
				.create();
		return gson.fromJson(jsonString, HLUserAboutMe.class);
	}


	private class AboutMeDeserializer  implements JsonDeserializer<HLUserAboutMe> {

		@Override
		public HLUserAboutMe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

			Gson gson = new Gson();
			HLUserAboutMe user = gson.fromJson(json, HLUserAboutMe.class);
			JsonObject jsonObject = json.getAsJsonObject();
			if (jsonObject.has("aboutMe")) {
				JsonObject elem = jsonObject.getAsJsonObject("aboutMe");
				if (elem != null && !elem.isJsonNull()) {
					user.setBirthday(elem.get("birthdate").getAsString());
					if (Utils.isStringValid(user.getBirthday()))
						user.setBirthdayDate(Utils.getDateFromDB(user.getBirthday()));
					user.setStatus(elem.get("status").getAsString());
					user.setCity(elem.get("birthplace").getAsString());
					user.setDescription(elem.get("description").getAsString());
				}
			}

			return user;
		}
	}

	//endregion


	//region == Getters and setters ==


	public String getBirthday() {
		return birthdate;
	}
	public void setBirthday(String birthday) {
		if (Utils.isStringValid(birthday)) {
			try {
				Date d = formatNewDate(birthday);
				if (d != null)
					birthday = Utils.formatDateForDB(d);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		this.birthdate = birthday;
	}

	public Date getBirthdayDate() {
		return birthdayDate;
	}
	public void setBirthdayDate(Date birthdayDate) {
		this.birthdayDate = birthdayDate;
	}

	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

	public String getCity() {
		return birthplace;
	}
	public void setCity(String city) {
		this.birthplace = city;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	//endregion

}
