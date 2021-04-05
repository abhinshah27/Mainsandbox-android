/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;

import org.json.JSONObject;

import java.io.Serializable;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 10/9/2017.
 */
@RealmClass
public class Tag implements Serializable, RealmModel, JsonHelper.JsonDeSerializer, RealmModelListener,
		Comparable<Tag>, Parcelable {

	@PrimaryKey
	@Expose private String userID;
	@Expose private String name;
	@Expose private String avatarURL;

	@Expose private boolean isInterest;

	@Ignore private transient boolean isInitiativeRecipient = false;


	@Override
	public int compareTo(@NonNull Tag o) {
		if (Utils.areStringsValid(getId(), o.getId())) {
			return getId().compareTo(o.getId());
		}
		return 0;
	}


	public Tag() {}

	public Tag(String userId, String userName, String userUrl, boolean isInterest) {
		this.userID = userId;
		this.name = userName;
		this.avatarURL = userUrl;
		this.isInterest = isInterest;
	}

	@Override
	public int hashCode() {
		if (Utils.isStringValid(getId()))
			return this.getId().hashCode();

		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Tag && Utils.areStringsValid(getId(), ((Tag) obj).getId()))
			return getId().equals(((Tag) obj).getId());
		return super.equals(obj);
	}


	//region == Parcelable interface ==

	public Tag(Parcel in) {
		this.userID = in.readString();
		this.name = in.readString();
		this.avatarURL = in.readString();
		this.isInterest = in.readByte() != 0;
	}


	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public Tag createFromParcel(Parcel in) {
			return new Tag(in);
		}

		public Tag[] newArray(int size) {
			return new Tag[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.userID);
		dest.writeString(this.name);
		dest.writeString(this.avatarURL);
		dest.writeByte((byte) (this.isInterest ? 1 : 0));
	}

	//endregion


	//region == Class custom methods ==

	public Tag returnUpdatedTag(JSONObject json) {
		return (Tag) updateWithReturn(json);
	}

	public static Tag convertFromGenericUser(@NonNull HLUserGeneric user) {
		return new Tag(user.getId(), user.getCompleteName(), user.getAvatarURL(), false);
	}

	public static Tag convertFromInterest(@NonNull Interest interest) {
		return new Tag(interest.getId(), interest.getName(), interest.getAvatarURL(), true);
	}

	//endregion


	//region == Serialization methods ==

	@Override
	public JsonElement serializeWithExpose() {
		return null;
	}

	@Override
	public String serializeToStringWithExpose() {
		return null;
	}

	@Override
	public JsonElement serialize() {
		return null;
	}

	@Override
	public String serializeToString() {
		return null;
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

	//region == Model listener methods ==

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
		return (RealmModelListener) deserialize(json, Tag.class);
	}

	//endregion

	public String getId() {
		return userID;
	}



	//region == Getters and setters ==

	public String getUserId() {
		return userID;
	}
	public void setUserId(String userId) {
		this.userID = userId;
	}

	public String getUserName() {
		return name;
	}
	public void setUserName(String userName) {
		this.name = userName;
	}

	public String getUserUrl() {
		return avatarURL;
	}
	public void setUserUrl(String userUrl) {
		this.avatarURL = userUrl;
	}

	public boolean isInterest() {
		return isInterest;
	}
	public void setInterest(boolean interest) {
		isInterest = interest;
	}

	public boolean isInitiativeRecipient() {
		return isInitiativeRecipient;
	}
	public void setInitiativeRecipient(boolean initiativeRecipient) {
		isInitiativeRecipient = initiativeRecipient;
	}

	//endregion

}
