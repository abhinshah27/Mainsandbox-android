/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 1/26/2018.
 */
@RealmClass
public class HLIdentity implements RealmModel, RealmModelListener, Serializable,
		JsonHelper.JsonDeSerializer, Comparable<HLIdentity> {

	@SerializedName(value = "userID")
	@PrimaryKey private String id;
	private String name;
	private String firstName;
	private String lastName;
	private String avatarURL;
	@SerializedName("wallImageLink")
	private String wallPictureURL;

	private boolean isInterest;

	@SerializedName(value = "idInterest")
	private String idDBObject;

	private boolean isNonProfit;

	private boolean hasActiveGiveSupportInitiative;

	private int totHeartsAvailable;


	public HLIdentity() {}


	@Override
	public int hashCode() {
		if (Utils.isStringValid(getId()))
			return getId().hashCode();
		else
			return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof HLIdentity && Utils.areStringsValid(getId(), ((HLIdentity) obj).getId()) &&
				getId().equals(((HLIdentity) obj).getId());
	}

	@Override
	public int compareTo(@NonNull HLIdentity o) {
		if (Utils.areStringsValid(this.getName(), o.getName()))
			return this.getName().compareTo(o.getName());

		return 0;
	}


	public HLIdentity deserializeToClass(JSONObject json) {
		return (HLIdentity) deserialize(json, HLIdentity.class);
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


	//region == Getters and setters

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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

	public String getAvatarURL() {
		return avatarURL;
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public String getWallPictureURL() {
		return wallPictureURL;
	}
	public void setWallPictureURL(String wallPictureURL) {
		this.wallPictureURL = wallPictureURL;
	}

	public boolean isInterest() {
		return isInterest;
	}
	public void setInterest(boolean interest) {
		isInterest = interest;
	}

	public String getIdDBObject() {
		return idDBObject;
	}
	public void setIdDBObject(String idDBObject) {
		this.idDBObject = idDBObject;
	}

	public boolean isNonProfit() {
		return isNonProfit;
	}
	public void setNonProfit(boolean nonProfit) {
		isNonProfit = nonProfit;
	}

	public boolean hasActiveGiveSupportInitiative() {
		return hasActiveGiveSupportInitiative;
	}
	public void setHasActiveGiveSupportInitiative(boolean hasActiveGiveSupportInitiative) {
		this.hasActiveGiveSupportInitiative = hasActiveGiveSupportInitiative;
	}

	public int getTotHeartsAvailable() {
		return totHeartsAvailable;
	}
	public void setTotHeartsAvailable(int totHeartsAvailable) {
		this.totHeartsAvailable = totHeartsAvailable;
	}

	//endregion

}
