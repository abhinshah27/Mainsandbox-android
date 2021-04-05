/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.features.profile.ProfileHelper;
import it.keybiz.lbsapp.corporate.models.enums.FeedFilterTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.LegacyContactStatusEnum;
import it.keybiz.lbsapp.corporate.models.enums.RequestsStatusEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * The representation of an object User of the Highlanders app.
 *
 * @author mbaldrighi on 10/3/2017.
 */
@RealmClass
public class HLUser implements JsonHelper.JsonDeSerializer, RealmModel, RealmModelListener {

	@Expose
	@PrimaryKey
	private String _id = Constants.GUEST_USER_ID;

	@Expose private String firstName = "";
	@Expose private String lastName = "";

	@Expose private String email = "";
	@Expose private String phoneNumber;

	@Expose private String avatarURL;
	@Expose private String wallImageLink;

	@Expose private RealmList<String> circles = new RealmList<>();

	@Expose private RealmList<String> lists = new RealmList<>();

	@Expose private int totHeartsSinceSubscribed;
	@Expose private int totHearts;

	@Expose private HLUserAboutMe aboutMe;
	@Expose private HLUserAboutMeMore moreAboutMe;

	@Expose private RealmList<LifeEvent> lifeEvents;

	@Expose private RealmList<FamilyRelationship> familyRelationships;

	@Expose private HLUserSettings settings;

	private String avatarBase64;
	private String wallBase64;
	@Ignore private transient Bitmap avatar;
	@Ignore private transient Bitmap wall;


	private RealmList<HLIdentity> identities;
	private HLIdentity selectedIdentity;
	private String selectedIdentityId;

	@Ignore private RealmModel selectedObject;

	private String rawLegacyContactStatus;
	private boolean hasEnoughHeartsForWishes;
	private boolean hasAPreferredInterest;

	private boolean wantsFamilyFilter;

	private RealmList<HLCircle> circleObjects = new RealmList<>();
	private RealmList<HLCircle> circleObjectsWithEmpty = new RealmList<>();
	private RealmList<String> selectedFeedFilters = new RealmList<>();

	private boolean hasActiveGiveSupportInitiative;

	/**
	 * Property added to handle new classes documentation section.
	 */
	private String docUrl;


	@Override
	protected void finalize() throws Throwable {
		if (avatar != null && !avatar.isRecycled())
			avatar.recycle();
		if (wall != null && !wall.isRecycled())
			wall.recycle();

		super.finalize();
	}

	public HLUser() {}

	public HLUser(String id) {
		this._id = id;
	}

	public HLUser(String id, String firstName, String lastName, String email) {
		this._id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}


	//region == Class custom methods ==

	private void initLists() {
		if (this.circles == null)
			this.circles = new RealmList<>();
		this.circles.clear();

		this.circles.add("Inner Circle");

		if (this.lists == null)
			this.lists = new RealmList<>();
		this.lists.clear();

		if (this.lifeEvents == null)
			this.lifeEvents = new RealmList<>();
		this.lifeEvents.clear();

		if (this.identities == null)
			this.identities = new RealmList<>();
		this.identities.clear();
	}

	public HLUser readUser(@Nullable Realm realm) {
		Object read = read(realm);
		if (read instanceof HLUser && RealmUtils.isValid(realm)) {
			final HLUser user = (HLUser) read;

			setIdentityObjectFromPersistence(realm, user);

			return user;
		}
		else
			return null;
	}

	public void setIdentityObjectFromPersistence(@NonNull Realm realm, @NonNull HLUser user) {
		if (user.getSelectedIdentity() != null) {
			String id = user.getSelectedIdentity().getIdDBObject();

			if (user.getSelectedIdentity().isInterest())
				user.setSelectedObject(RealmUtils.readFirstFromRealmWithId(realm, Interest.class, "_id", id));
//			else if () {
//				// more cases
//			}
		}
	}

	public void setSelectedIdentityFromPersistence(@NonNull Realm realm, @NonNull HLUser user) {
		user.setSelectedIdentity(
				(HLIdentity) RealmUtils.readFirstFromRealmWithId(
						realm,
						HLIdentity.class,
						"id",
						user.getSelectedIdentityId()
				)
		);
	}

	public boolean isValid() {
		if (getSelectedIdentity() == null)
			return Utils.areStringsValid(getId(), getCompleteName()) && !hasFakeId();
		else
			return Utils.areStringsValid(getId(), getFirstName(), getLastName(), getEmail()) && !hasFakeId();
	}

	public boolean hasFakeId() {
		return Utils.isStringValid(getId()) && getId().equals(Constants.GUEST_USER_ID);
	}

	public boolean applyFamilyFilter() {
		return wantsFamilyFilter() && !isActingAsInterest();
	}

	public boolean hasActiveFilters() {
		return getSelectedFeedFilters() != null && !getSelectedFeedFilters().isEmpty();
	}

	public String getCompleteName() {
		if (getSelectedIdentity() == null)
			return getUserCompleteName();
		else
			return Utils.isStringValid(getSelectedIdentity().getName()) ? getSelectedIdentity().getName() : "";
	}

	public String getUserId() {
		return _id;
	}

	public String getUserAvatarURL() {
		return avatarURL;
	}

	public String getUserCoverPhotoURL() {
		return wallImageLink;
	}

	public String getUserCompleteName() {
		if (Utils.areStringsValid(getFirstName(), getLastName())) {
			return getFirstName().trim() + " " + getLastName().trim();
		} else {
			if (Utils.isStringValid(getFirstName()))
				return getFirstName().trim();
			else if (Utils.isStringValid(getLastName()))
				return getLastName().trim();
			else
				return "";
		}
	}

	public ProfileHelper.ProfileType getProfileType() {
		return isActingAsInterest() ? ProfileHelper.ProfileType.INTEREST_ME : ProfileHelper.ProfileType.ME;
	}

	public boolean isActingAsInterest() {
		return getSelectedIdentity() != null && getSelectedIdentity().isInterest();
	}

	public HLUser deserializeToClass(String jsonString) {
		return (HLUser) deserialize(jsonString, HLUser.class);
	}

	public HLUser deserializeToClass(JSONObject json) {
		return deserializeToClass(json.toString());
	}


	/* ABOUT ME */

	public String getAboutBirthday() {
		return aboutMe != null ? aboutMe.getFormattedDate() : null;
	}

	public String getAboutStatus() {
		return aboutMe != null ? aboutMe.getStatus() : null;
	}
	public int getAboutStatusSelection(Resources res) {
		return aboutMe != null ? aboutMe.getStatusPositionInArray(res) : -1;
	}

	public String getAboutCity() {
		return aboutMe != null ? aboutMe.getCity() : null;
	}

	public String getAboutDescription() {
		return aboutMe != null ? aboutMe.getDescription() : null;
	}


	/* MORE ABOUT ME */

	public String getMoreAboutBirthplace() {
		return moreAboutMe != null ? moreAboutMe.getBirthPlace() : null;
	}

	public String getMoreAboutGender() {
		return moreAboutMe != null ? moreAboutMe.getGender() : null;
	}
	public int getMoreAboutGenderSelection(Resources res) {
		return moreAboutMe != null ? moreAboutMe.getSpinnerPositionInArray(res, false) : 0;
	}

	public String getMoreAboutInterestedIn() {
		return moreAboutMe != null ? moreAboutMe.getInterestedIn() : null;
	}
	public int getMoreAboutInterestedInSelection(Resources res) {
		return moreAboutMe != null ? moreAboutMe.getSpinnerPositionInArray(res, true) : -1;
	}

	public String getMoreAboutLanguagesToString() {
		return moreAboutMe != null ? moreAboutMe.getSerializedLanguagesString() : null;
	}

	public List<String> getMoreAboutLanguages() {
		return moreAboutMe != null ? moreAboutMe.getLanguages() : null;
	}

	public String getMoreAboutAddress() {
		return moreAboutMe != null ? moreAboutMe.getAddress() : null;
	}

	public String getMoreAboutOtherNames() {
		return moreAboutMe != null ? moreAboutMe.getOtherNames() : null;
	}


	public void setIdentities(JSONArray array) {
		if (array != null && array.length() > 0) {
			if (getIdentities() == null)
				setIdentities(new RealmList<HLIdentity>());
			else
				getIdentities().clear();

			JSONArray identities = array.optJSONObject(0).optJSONArray("identities");
			for (int i = 0; i < identities.length(); i++) {
				JSONObject j = identities.optJSONObject(i);
				if (j != null) {
					getIdentities().add(new HLIdentity().deserializeToClass(j));
				}
			}
		}
	}

	public boolean hasIdentities() {
		return getIdentities() != null && !getIdentities().isEmpty();
	}

	public boolean hasLegacyContact() {
		return LegacyContactStatusEnum.toEnum(rawLegacyContactStatus) == LegacyContactStatusEnum.AUTHORIZED ||
				(settings != null && settings.hasLegacyContact());
	}

	public boolean canRequestLegacyContact() {
		return LegacyContactStatusEnum.toEnum(rawLegacyContactStatus) == LegacyContactStatusEnum.NONE ||
				(settings != null && settings.canRequestLegacyContact());
	}

	public boolean hasTwoStepVerificationContacts() {
		return settings != null && settings.hasTwoStepVerificationContacts();
	}

	public RealmList<HLUserGeneric> getTwoStepVerificationContacts() {
		return hasTwoStepVerificationContacts() ? settings.getTwoStepVerificationContacts() : new RealmList<HLUserGeneric>();
	}

	public boolean isTwoStepVerificationContact(String id) {
		if (hasTwoStepVerificationContacts() && Utils.isStringValid(id)) {
			for (HLUserGeneric u : getTwoStepVerificationContacts()) {
				if (u != null && u.getId().equals(id))
					return true;
			}
		}

		return false;
	}

	public int getSettingSortOrder() {
		return getSettingSortOrder(false);
	}

	public int getSettingSortOrder(boolean wantsRaw) {
		if (settings != null) {
			if (wantsRaw) return settings.getRawSortOrder();

			return settings.getOverriddenSortOrder() != null ?
					settings.getOverriddenSortOrder() : settings.getRawSortOrder();
		}

		return Constants.SERVER_SORT_TL_ENUM_DEFAULT;
	}

	public int getSettingAutoPlay() {
		return settings != null ? settings.getRawAutoplayVideo() : Constants.SERVER_AUTOPLAY_TL_ENUM_WIFI;
	}

	public HLUserGeneric getLegacyContact() {
		return settings != null ? settings.getLegacyContact() : null;
	}

	public Interest getPreferredInterest() {
		return settings != null ? settings.getPreferredInterest() : null;
	}

	public boolean wantsTwoStepVerification() {
		return settings != null && settings.isTwoStepVerificationEnabled();
	}

	public boolean wantsPaperCertificate() {
		return settings != null && settings.isPaperCertificateRequired();
	}

	public boolean wantsInactivityPeriod() {
		return settings != null && settings.isInactivityPeriodEnabled();
	}

	public int[] getInactivityPeriodValues() {
		return settings != null ? settings.getInactivityValues() : new int[2];
	}

	public PostVisibility getDefaultPostVisibility() {
		if (settings != null)
			return new PostVisibility(settings.getRawPostVisibility());

		return new PostVisibility();
	}


	public boolean feedAutoplayAllowed(Context context) {
		int autoplay = getSettingAutoPlay();
		if (autoplay == Constants.SERVER_AUTOPLAY_TL_ENUM_NEVER)
			return false;
		else if (Utils.isDeviceConnected(context)) {
			if (autoplay == Constants.SERVER_AUTOPLAY_TL_ENUM_WIFI && Utils.isConnectionWiFi(context))
				return true;
			else return autoplay == Constants.SERVER_AUTOPLAY_TL_ENUM_BOTH;
		}

		return false;
	}

	public boolean hasFamilyRelationships() {
		return getFamilyRelationships() != null && !getFamilyRelationships().isEmpty();
	}

	public boolean hasAuthorizedFamilyRelationships() {
		if (hasFamilyRelationships()) {
			long cnt = Stream.of(getFamilyRelationships()).filter(new Predicate<FamilyRelationship>() {
				@Override
				public boolean test(FamilyRelationship familyRelationship) {
					return familyRelationship.getStatusEnum() == RequestsStatusEnum.AUTHORIZED;
				}
			}).count();
			return cnt > 0;
		}
		return false;
	}

	public void updateFiltersForSingleCircle(HLCircle circle, boolean in) {
		if (in) {
			if (getCircleObjects() == null)
				setCircleObjects(new RealmList<>());
			if (getCircleObjectsWithEmpty() == null)
				setCircleObjectsWithEmpty(new RealmList<>());

			if (getCircleObjects() != null && !getCircleObjects().contains(circle))
				getCircleObjects().add(circle);
			if (getCircleObjectsWithEmpty() != null && !getCircleObjectsWithEmpty().contains(circle))
				getCircleObjectsWithEmpty().add(circle);

			if (getSelectedFeedFilters() != null &&
					getSelectedFeedFilters().contains(FeedFilterTypeEnum.getCallValue(Constants.INNER_CIRCLE_NAME)) &&
					!getSelectedFeedFilters().contains(circle.getName()))
				getSelectedFeedFilters().add(circle.getName());
		}
		else {
			if (getCircleObjects() != null)
				getCircleObjects().remove(circle);
			if (getCircleObjectsWithEmpty() != null)
				getCircleObjectsWithEmpty().remove(circle);

			// INFO: 3/4/19    It's uncorrect to remove IC from filters if circle is removed or become empty
			String filter = FeedFilterTypeEnum.getCallValue(circle.getName());
			if (getSelectedFeedFilters() != null) {
				getSelectedFeedFilters().remove(filter);
//				getSelectedFeedFilters().remove(FeedFilterTypeEnum.getCallValue(Constants.INNER_CIRCLE_NAME));
			}
		}
	}

	public void updateFilters() {
		if (getSelectedFeedFilters() != null && !getSelectedFeedFilters().isEmpty()) {
			for (String name : new ArrayList<>(getSelectedFeedFilters())) {
				FeedFilterTypeEnum type = FeedFilterTypeEnum.toEnum(name);
				if (type == FeedFilterTypeEnum.CIRCLE) {
					HLCircle circle = new HLCircle(name);
					if (!getCircleObjects().contains(circle)) {
						getSelectedFeedFilters().remove(name);

						// INFO: 3/4/19    It's uncorrect to remove IC from filters if circle is removed or become empty
//						getSelectedFeedFilters().remove(FeedFilterTypeEnum.getCallValue(Constants.INNER_CIRCLE_NAME));
					}
				}
			}
		}
	}


	public List<HLIdentity> getNonProfitIdentities() {
		if (hasIdentities()) {
			List<HLIdentity> result = Stream.of(getIdentities()).filter(new Predicate<HLIdentity>() {
				@Override
				public boolean test(HLIdentity value) {
					return value != null && value.isNonProfit();
				}
			}).collect(Collectors.toList());

			if (result != null && !result.isEmpty())
				return result;
		}

		return new ArrayList<>();
	}

	public void saveConfigurationData(JSONObject jOb) {
		boolean hasGSInitiative = jOb.optBoolean("hasGSInitiatives", false);
		if (getSelectedIdentity() == null) {
			// vars WISH-related
			setHasEnoughHeartsForWishes(jOb.optBoolean("hasEnoughHeartsForWishes", false));
			setHasAPreferredInterest(jOb.optBoolean("hasAPreferredInterest", false));
			setRawLegacyContactStatus(jOb.optString("rawLegacyContactStatus"));

			// var INITIATIVE-related
			setHasActiveGiveSupportInitiative(hasGSInitiative);
		}
		else getSelectedIdentity().setHasActiveGiveSupportInitiative(hasGSInitiative);

	}

	public void setUpdatedHearts(JSONObject jOb) {
		int totHearts = jOb.optInt("totHearts");
		if (getSelectedIdentity() == null)
			setTotHeartsAvailable(totHearts);
		else
			getSelectedIdentity().setTotHeartsAvailable(totHearts);
	}

	public boolean hasAvailableHearts() {
		return getTotHeartsAvailable() > 0;
	}

	//endregion


	//region == Realm model methods ==

	@Override
	public void reset() {
		this._id = Constants.GUEST_USER_ID;
		this.firstName = "";
		this.lastName = "";
		this.email = "";
		this.avatarURL = null;

		initLists();
	}

	@Override
	public Object read(Realm realm) {
		return read(realm, HLUser.class);
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


	public HLUser deserializeComplete(JSONObject json) {
		return deserializeComplete(json.toString());
	}

	public HLUser deserializeComplete(String jsonString) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(HLUser.class, new AboutMeDeserializer())
				.create();
		return gson.fromJson(jsonString, HLUser.class);
	}


	private class AboutMeDeserializer  implements JsonDeserializer<HLUser> {

		@Override
		public HLUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

			/*
			Gson gson = new Gson();
			HLUser user = gson.fromJson(json, HLUser.class);
			JsonObject jsonObject = json.getAsJsonObject();
			if (jsonObject.has("aboutMe")) {
				JsonObject elem = jsonObject.getAsJsonObject("aboutMe");
				if (elem != null && !elem.isJsonNull()) {
					user.setBirthday(elem.get("birthday").getAsString());
					user.setStatus(elem.get("status").getAsString());
					user.setCity(elem.get("city").getAsString());
					user.setDescription(elem.get("description").getAsString());
				}
			}

			return user;
			*/

			return null;
		}
	}


	//endregion


	//region == Getters and setters ==

	public String getId() {
		if (getSelectedIdentity() == null)
			return _id;
		else
			return getSelectedIdentity().getId();
	}
	public void setId(String id) {
		this._id = id;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getAvatarURL() {
		if (getSelectedIdentity() == null)
			return avatarURL;
		else
			return getSelectedIdentity().getAvatarURL();
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}
//	public void setAvatarURL(String avatarURL) {
//		if (getSelectedIdentity() == null)
//			this.avatarURL = avatarURL;
//		else
//			getSelectedIdentity().setAvatarURL(avatarURL);
//	}

	public RealmList<String> getCircles() {
		return circles;
	}
	public void setCircles(RealmList<String> circles) {
		this.circles = circles;
	}

	public RealmList<String> getFolders() {
		return lists;
	}
	public void setFolders(RealmList<String> lists) {
		this.lists = lists;
	}

	public int getTotHearts() {
		return totHeartsSinceSubscribed;
	}
	public void setTotHearts(int totHearts) {
		this.totHeartsSinceSubscribed = totHearts;
	}

	public int getTotHeartsAvailable() {
		if (getSelectedIdentity() == null)
			return totHearts;
		else
			return getSelectedIdentity().getTotHeartsAvailable();
	}
	public void setTotHeartsAvailable(int totHeartsAvailable) {
		this.totHearts = totHeartsAvailable;
	}

	public String getCoverPhotoURL() {
		return wallImageLink;
	}
	public void setCoverPhotoURL(String coverPhotoURL) {
		this.wallImageLink = coverPhotoURL;
	}
//	public void setCoverPhotoURL(String coverPhotoURL) {
//		if (getSelectedIdentity() == null)
//			this.wallImageLink = coverPhotoURL;
//		else
//			getSelectedIdentity().setWallPictureURL(coverPhotoURL);
//	}

	public RealmList<LifeEvent> getLifeEvents() {
		return lifeEvents;
	}
	public void setLifeEvents(RealmList<LifeEvent> lifeEvents) {
		this.lifeEvents = lifeEvents;
	}

	public String getAvatarBase64() {
		return avatarBase64;
	}
	public void setAvatarBase64(String avatarBase64) {
		this.avatarBase64 = avatarBase64;
	}

	public String getWallBase64() {
		return wallBase64;
	}
	public void setWallBase64(String wallBase64) {
		this.wallBase64 = wallBase64;
	}

	public Bitmap getAvatar() {
		return avatar;
	}
	public void setAvatar(Bitmap avatar) {
		this.avatar = avatar;
	}

	public Bitmap getWall() {
		return wall;
	}
	public void setWall(Bitmap wall) {
		this.wall = wall;
	}

	public HLUserAboutMe getAboutMe() {
		return aboutMe;
	}
	public void setAboutMe(HLUserAboutMe aboutMe) {
		this.aboutMe = aboutMe;
	}

	public HLUserAboutMeMore getMoreAboutMe() {
		return moreAboutMe;
	}
	public void setMoreAboutMe(HLUserAboutMeMore aboutMe) {
		this.moreAboutMe = aboutMe;
	}


	public RealmList<HLIdentity> getIdentities() {
		return identities;
	}
	public void setIdentities(RealmList<HLIdentity> identities) {
		this.identities = identities;
	}

	public HLIdentity getSelectedIdentity() {
		return selectedIdentity;
	}
	public void setSelectedIdentity(HLIdentity selectedIdentity) {
		this.selectedIdentity = selectedIdentity;
	}

	public String getSelectedIdentityId() {
		return selectedIdentityId;
	}
	public void setSelectedIdentityId(String selectedIdentityId) {
		this.selectedIdentityId = selectedIdentityId;
	}

	public RealmModel getSelectedObject() {
		return selectedObject;
	}
	public void setSelectedObject(RealmModel selectedObject) {
		this.selectedObject = selectedObject;
	}

	public String getRawLegacyContactStatus() {
		return rawLegacyContactStatus;
	}
	public void setRawLegacyContactStatus(String rawLegacyContactStatus) {
		this.rawLegacyContactStatus = rawLegacyContactStatus;
	}

	public boolean hasEnoughHeartsForWishes() {
		return hasEnoughHeartsForWishes;
	}
	public void setHasEnoughHeartsForWishes(boolean hasEnoughHeartsForWishes) {
		this.hasEnoughHeartsForWishes = hasEnoughHeartsForWishes;
	}

	public boolean hasAPreferredInterest() {
		return hasAPreferredInterest;
	}
	public void setHasAPreferredInterest(boolean hasAPreferredInterest) {
		this.hasAPreferredInterest = hasAPreferredInterest;
	}

	public HLUserSettings getSettings() {
		return settings;
	}
	public void setSettings(HLUserSettings settings) {
		this.settings = settings;
	}

	public RealmList<FamilyRelationship> getFamilyRelationships() {
		return familyRelationships;
	}
	public void setFamilyRelationships(RealmList<FamilyRelationship> familyRelationships) {
		this.familyRelationships = familyRelationships;
	}

	public boolean wantsFamilyFilter() {
		return wantsFamilyFilter;
	}
	public void setWantsFamilyFilter(boolean wantsFamilyFilter) {
		this.wantsFamilyFilter = wantsFamilyFilter;
	}

	public RealmList<HLCircle> getCircleObjects() {
		return circleObjects;
	}
	public void setCircleObjects(RealmList<HLCircle> circleObjects) {
		this.circleObjects = circleObjects;
	}

	public RealmList<HLCircle> getCircleObjectsWithEmpty() {
		return circleObjectsWithEmpty;
	}
	public void setCircleObjectsWithEmpty(RealmList<HLCircle> circleObjectsWithEmpty) {
		this.circleObjectsWithEmpty = circleObjectsWithEmpty;
	}

	public RealmList<String> getSelectedFeedFilters() {
		return selectedFeedFilters;
	}
	public void setSelectedFeedFilters(RealmList<String> selectedFeedFilters) {
		this.selectedFeedFilters = selectedFeedFilters;
	}

	public boolean hasActiveGiveSupportInitiative() {
		if (getSelectedIdentity() == null)
			return hasActiveGiveSupportInitiative;
		else
			return getSelectedIdentity().hasActiveGiveSupportInitiative();
	}
	public void setHasActiveGiveSupportInitiative(boolean hasActiveGiveSupportInitiative) {
		if (getSelectedIdentity() == null)
			this.hasActiveGiveSupportInitiative = hasActiveGiveSupportInitiative;
		else
			getSelectedIdentity().setHasActiveGiveSupportInitiative(hasActiveGiveSupportInitiative);
	}

	public String getDocUrl() {
		return docUrl;
	}
	public void setDocUrl(String docUrl) {
		this.docUrl = docUrl;
	}

	//endregion

}
