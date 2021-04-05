/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import com.google.gson.JsonElement;

import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.models.enums.LegacyContactStatusEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;

/**
 * The nested object containing the user's settings.
 *
 * @author mbaldrighi on 3/30/2018.
 */
@RealmClass
public class HLUserSettings implements RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer {

	private int rawSortOrder = Constants.SERVER_SORT_TL_ENUM_DEFAULT;
	private int rawAutoplayVideo = Constants.SERVER_AUTOPLAY_TL_ENUM_WIFI;

	private Integer overriddenSortOrder = null;

	private String legacyContactRawStatus = "none";

	private HLUserGeneric legacyContact;
	private Interest preferredInterest;

	private boolean twoStepVerificationEnabled = false;
	private RealmList<HLUserGeneric> twoStepVerificationContacts = new RealmList<>();

	private boolean inactivityPeriodEnabled = false;
	private int selectedEra = 0;
	private int selectedTime = 0;

	private boolean paperCertificateRequired = false;

	private int rawPostVisibility = 0;
	private RealmList<String> values = new RealmList<>();


	public HLUserSettings() {}


	public LegacyContactStatusEnum getLegacyContactStatus() {
		return LegacyContactStatusEnum.toEnum(legacyContactRawStatus);
	}

	public boolean hasLegacyContact() {
		return getLegacyContactStatus() == LegacyContactStatusEnum.AUTHORIZED && legacyContact != null;
	}

	public boolean canRequestLegacyContact() {
		return !hasLegacyContact() && getLegacyContactStatus() == LegacyContactStatusEnum.NONE;
	}

	public boolean hasTwoStepVerificationContacts() {
		return twoStepVerificationContacts != null && !twoStepVerificationContacts.isEmpty();
	}

	public int[] getInactivityValues() {
		return new int[] { selectedEra, selectedTime };
	}

	public boolean hasPreferredInterest() {
		return preferredInterest != null;
	}



	//region == Realm model methods ==

	@Override
	public void reset() {}

	@Override
	public Object read(Realm realm) {
		return read(realm, HLUserSettings.class);
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


//	public HLUserSettings deserializeComplete(JSONObject json) {
//		return deserializeComplete(json.toString());
//	}

//	public HLUserSettings deserializeComplete(String jsonString) {
//		Gson gson = new GsonBuilder()
//				.registerTypeAdapter(HLUserSettings.class, new AboutMeDeserializer())
//				.create();
//		return gson.fromJson(jsonString, HLUserSettings.class);
//	}
//
//
//	private class AboutMeDeserializer implements JsonDeserializer<HLUserSettings> {
//
//		@Override
//		public HLUserSettings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//
//			Gson gson = new Gson();
//			HLUserSettings user = gson.fromJson(json, HLUserSettings.class);
//			JsonObject jsonObject = json.getAsJsonObject();
//			if (jsonObject.has("aboutMe")) {
//				JsonObject elem = jsonObject.getAsJsonObject("aboutMe");
//				if (elem != null && !elem.isJsonNull()) {
//					user.setBirthday(elem.get("birthdate").getAsString());
//					if (Utils.isStringValid(user.getBirthday()))
//						user.setBirthdayDate(Utils.getDateFromDB(user.getBirthday()));
//					user.setStatus(elem.get("status").getAsString());
//					user.setCity(elem.get("birthplace").getAsString());
//					user.setDescription(elem.get("description").getAsString());
//				}
//			}
//
//			return user;
//		}
//	}

	//endregion


	//region == Getters and setters ==

	public int getRawSortOrder() {
		return rawSortOrder;
	}
	public void setRawSortOrder(int rawSortOrder) {
		this.rawSortOrder = rawSortOrder;
	}

	public int getRawAutoplayVideo() {
		return rawAutoplayVideo;
	}
	public void setRawAutoplayVideo(int rawAutoplayVideo) {
		this.rawAutoplayVideo = rawAutoplayVideo;
	}

	public Integer getOverriddenSortOrder() {
		return overriddenSortOrder;
	}
	public void setOverriddenSortOrder(Integer overriddenSortOrder) {
		this.overriddenSortOrder = overriddenSortOrder;
	}

	public String getLegacyContactRawStatus() {
		return legacyContactRawStatus;
	}
	public void setLegacyContactRawStatus(String legacyContactRawStatus) {
		this.legacyContactRawStatus = legacyContactRawStatus;
	}

	public HLUserGeneric getLegacyContact() {
		return legacyContact;
	}
	public void setLegacyContact(HLUserGeneric legacyContact) {
		this.legacyContact = legacyContact;
	}

	public Interest getPreferredInterest() {
		return preferredInterest;
	}
	public void setPreferredInterest(Interest preferredInterest) {
		this.preferredInterest = preferredInterest;
	}

	public boolean isTwoStepVerificationEnabled() {
		return twoStepVerificationEnabled;
	}
	public void setTwoStepVerificationEnabled(boolean twoStepVerificationEnabled) {
		this.twoStepVerificationEnabled = twoStepVerificationEnabled;
	}

	public RealmList<HLUserGeneric> getTwoStepVerificationContacts() {
		return twoStepVerificationContacts;
	}
	public void setTwoStepVerificationContacts(RealmList<HLUserGeneric> twoStepVerificationContacts) {
		this.twoStepVerificationContacts = twoStepVerificationContacts;
	}

	public boolean isInactivityPeriodEnabled() {
		return inactivityPeriodEnabled;
	}
	public void setInactivityPeriodEnabled(boolean inactivityPeriodEnabled) {
		this.inactivityPeriodEnabled = inactivityPeriodEnabled;
	}

	public int getSelectedEra() {
		return selectedEra;
	}
	public void setSelectedEra(int selectedEra) {
		this.selectedEra = selectedEra;
	}

	public int getSelectedTime() {
		return selectedTime;
	}
	public void setSelectedTime(int selectedTime) {
		this.selectedTime = selectedTime;
	}

	public boolean isPaperCertificateRequired() {
		return paperCertificateRequired;
	}
	public void setPaperCertificateRequired(boolean paperCertificateRequired) {
		this.paperCertificateRequired = paperCertificateRequired;
	}

	public int getRawPostVisibility() {
		return rawPostVisibility;
	}
	public void setRawPostVisibility(int rawPostVisibility) {
		this.rawPostVisibility = rawPostVisibility;
	}

	public RealmList<String> getValues() {
		return values;
	}
	public void setValues(RealmList<String> values) {
		this.values = values;
	}

	//endregion

}
