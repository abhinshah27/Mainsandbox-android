/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 12/27/2017.
 */
@RealmClass
public class Interest implements RealmModel, RealmModelListener, Serializable,
		JsonHelper.JsonDeSerializer, Comparable<Interest> {

	@SerializedName(value = "idInterest", alternate = {"_id", "interestID"})
	@Expose @PrimaryKey private String _id;
	@Expose private String name;
	@Expose private String avatarURL;
	@Expose private String headline;
	private RealmList<InterestCategory> categories = new RealmList<>();

	@Expose private String wallImageLink;
	private int totFollowers;
	private int totHeartsSinceSubscribed;
	private boolean isClaimed;
	private boolean isFollowed;
	private boolean isPreferred;
	private boolean isDiaryEmpty;
	private boolean claimedByYou;
	private boolean claimedPending;

	private HLInterestAbout about;
	private RealmList<MoreInfoObject> more_info = new RealmList<>();

	private RealmList<HLUserGeneric> followers = new RealmList<>();
	private RealmList<HLUserGeneric> preferredByUsers = new RealmList<>();

	private String claimedBy;


	public Interest() {}


	@Override
	public int hashCode() {
		if (Utils.isStringValid(getId()))
			return getId().hashCode();
		else
			return super.hashCode();
	}

	@Override
	public int compareTo(@NonNull Interest o) {
		if (Utils.areStringsValid(this.getName(), o.getName()))
			return this.getName().compareTo(o.getName());

		return 0;
	}



	public Interest deserializeToClass(JSONObject json) {
		return (Interest) deserialize(json, Interest.class);
	}

	public Interest deserializeToClass(JSONObject json, @NonNull String userId) {
		Interest interest = (Interest) deserialize(json, Interest.class);

		setClaimed(getClaimedBy() != null && getClaimedBy().equals(userId));
		setFollowed(getBooleanFromArray(getFollowers(), userId));
		setPreferred(getBooleanFromArray(getPreferredByUsers(), userId));

		return interest;
	}

	private boolean getBooleanFromArray(List<HLUserGeneric> list, final @NonNull String userId) {
		if (list != null && !list.isEmpty()) {
			List<HLUserGeneric> result = Stream.of(getFollowers()).filter(new Predicate<HLUserGeneric>() {
				@Override
				public boolean test(HLUserGeneric value) {
					return value != null && value.getId().equals(userId);
				}
			}).collect(Collectors.toList());

			return result != null && result.size() == 1;
		}

		return false;
	}

	/*
	public Interest deserializeComplete(JSONObject json, String userId) {
		return deserializeComplete(json.toString(), userId);
	}

	public Interest deserializeComplete(String jsonString, String userId) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Interest.class, new InterestDeserializer())
				.create();
		Interest u = gson.fromJson(jsonString, Interest.class);

		setClaimed(getClaimedBy() != null && getClaimedBy().equals(userId));
		setFollowed(getBooleanFromArray(getFollowers(), userId));
		setPreferred(getBooleanFromArray(getPreferredByUsers(), userId));
		return u;
	}

	private class InterestDeserializer implements JsonDeserializer<Interest> {

		@Override
		public Interest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			Gson gson = new Gson();
			Interest user = gson.fromJson(json, Interest.class);
			JsonObject jsonObject = json.getAsJsonObject();
			if (jsonObject.has("person")) {
				JsonObject elem = jsonObject.getAsJsonObject("person");
				if (elem != null && !elem.isJsonNull()) {
					if (elem.has("avatarURL"))
						user.setAvatarURL(elem.get("avatarURL").getAsString());
					if (elem.has("wallImageLink"))
						user.setWallImageLink(elem.get("wallImageLink").getAsString());
					if (elem.has("firstName"))
						user.setFirstName(elem.get("firstName").getAsString());
					if (elem.has("lastName"))
						user.setLastName(elem.get("lastName").getAsString());
					if (elem.has("totHeartsSinceSubscribed"))
						user.setTotHearts(elem.get("totHeartsSinceSubscribed").getAsInt());
				}
			}

			return user;
		}
	}
	*/

	public String getAboutDescription() {
		return (getAbout() != null) ? getAbout().getDescription() : "";
	}
	public String getAboutDescriptionNote() {
		return (getAbout() != null) ? getAbout().getNote() : "";
	}


	public String getFollowersWithNumber(Resources res) {
		return Utils.getReadableCount(res, totFollowers);
	}

	public String getHeartsWithNumber(Resources res) {
		return Utils.getReadableCount(res, totHeartsSinceSubscribed);
	}


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
		return null;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
		return JsonHelper.deserialize(jsonString, myClass);
	}

	@Override
	public Object getSelfObject() {
		return this;
	}

	//endregion


	//region == Getters and setters

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
	public void update(Object object) {
		if (object instanceof Interest) {
			Interest interest = ((Interest) object);

			setId(interest.getId());
			setName(interest.getName());
			setAvatarURL(interest.getAvatarURL());
			setHeadline(interest.getHeadline());
			setCategories((RealmList<InterestCategory>) interest.getCategories());
			setWallPictureURL(interest.getWallPictureURL());
			setTotFollowers(interest.getTotFollowers());
			setTotHearts(interest.getTotHearts());
			setClaimed(interest.isClaimed());
			setFollowed(interest.isFollowed());
			setPreferred(interest.isPreferred());
			setEmptyDiary(interest.isEmptyDiary());
			setClaimedByYou(interest.isClaimedByYou());
			setClaimedPending(interest.isClaimedPending());

			setAbout(interest.getAbout());
			setMoreInfo(interest.getMoreInfo());
			setFollowers(interest.getFollowers());
			setPreferredByUsers(interest.getPreferredByUsers());
			setClaimedBy(interest.getClaimedBy());
		}
	}

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


	//region == Getters and setters

	public String getId() {
		return _id;
	}
	public void setId(String id) {
		this._id = id;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getAvatarURL() {
		return avatarURL;
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public String getHeadline() {
		return headline;
	}
	public void setHeadline(String headline) {
		this.headline = headline;
	}

	public List<InterestCategory> getCategories() {
		return categories;
	}
	public void setCategories(RealmList<InterestCategory> categories) {
		this.categories = categories;
	}

	public String getWallPictureURL() {
		return wallImageLink;
	}
	public void setWallPictureURL(String wallPictureURL) {
		this.wallImageLink = wallPictureURL;
	}

	public int getTotFollowers() {
		return totFollowers;
	}
	public void setTotFollowers(int totFollowers) {
		this.totFollowers = totFollowers;
	}

	public int getTotHearts() {
		return totHeartsSinceSubscribed;
	}
	public void setTotHearts(int totHearts) {
		this.totHeartsSinceSubscribed = totHearts;
	}

	public boolean isClaimed() {
		return isClaimed;
	}
	public void setClaimed(boolean claimed) {
		this.isClaimed = claimed;
	}

	public boolean isFollowed() {
		return isFollowed;
	}
	public void setFollowed(boolean followed) {
		this.isFollowed = followed;
	}

	public boolean isPreferred() {
		return isPreferred;
	}
	public void setPreferred(boolean preferred) {
		this.isPreferred = preferred;
	}

	public boolean isEmptyDiary() {
		return isDiaryEmpty;
	}
	public void setEmptyDiary(boolean emptyDiary) {
		this.isDiaryEmpty = emptyDiary;
	}

	public RealmList<HLUserGeneric> getFollowers() {
		return followers;
	}
	public void setFollowers(RealmList<HLUserGeneric> followers) {
		this.followers = followers;
	}

	public RealmList<HLUserGeneric> getPreferredByUsers() {
		return preferredByUsers;
	}
	public void setPreferredByUsers(RealmList<HLUserGeneric> preferredByUsers) {
		this.preferredByUsers = preferredByUsers;
	}

	public String getClaimedBy() {
		return claimedBy;
	}
	public void setClaimedBy(String claimedBy) {
		this.claimedBy = claimedBy;
	}

	public HLInterestAbout getAbout() {
		return about;
	}
	public void setAbout(HLInterestAbout about) {
		this.about = about;
	}

	public RealmList<MoreInfoObject> getMoreInfo() {
		return more_info;
	}
	public void setMoreInfo(RealmList<MoreInfoObject> moreInfo) {
		this.more_info = moreInfo;
	}

	public boolean isClaimedByYou() {
		return claimedByYou;
	}
	public void setClaimedByYou(boolean claimedByYou) {
		this.claimedByYou = claimedByYou;
	}

	public boolean isClaimedPending() {
		return claimedPending;
	}
	public void setClaimedPending(boolean claimedPending) {
		this.claimedPending = claimedPending;
	}

	//endregion

}
