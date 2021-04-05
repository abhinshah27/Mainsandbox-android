/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;


import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 9/25/2017.
 */
@RealmClass
public class InteractionShare implements Serializable, RealmModel, JsonHelper.JsonDeSerializer,
		RealmModelListener {

	@PrimaryKey
	@SerializedName("idSharedPost")
	@Expose private String id;

	@Expose private String date;
	private transient Date creationDate;

	@SerializedName("idPost")
	@Expose private String originalPostId;

	@SerializedName("userID")
	@Expose private String authorId;
	@SerializedName("name")
	@Expose private String author;
	@SerializedName("avatarURL")
	@Expose private String authorUrl;

	@SerializedName("isVisible")
	private boolean visible = true;

	private boolean isASharing = true;


	public static Comparator<InteractionShare> DateComparator = new Comparator<InteractionShare>() {
		@Override
		public int compare(InteractionShare t1, InteractionShare t2) {
			if (t1 != null && t2 != null) {
				return Long.compare(t1.getCreationDate().getTime(), t2.getCreationDate().getTime());
			}
			return 0;
		}
	};


	public InteractionShare() {}

	public InteractionShare(String id, Date creationDate, String author, String authorUrl) {
		this.id = id;
		this.creationDate = creationDate;
		this.author = author;
		this.authorUrl = authorUrl;
	}

	@Override
	public int hashCode() {
		return this.getId().hashCode();
	}


	//region == Class custom methods ==

	public InteractionShare returnUpdatedInteraction(JSONObject json) {
		InteractionShare ishare = (InteractionShare) updateWithReturn(json);
		ishare.setCreationDate(Utils.getDateFromDB(ishare.getDate()));
		return ishare;
	}

	//endregion


	//region == Serialization methods ==

	@Override
	public JsonElement serializeWithExpose() {
		return null;
	}

	@Override
	public String serializeToStringWithExpose() {
		return JsonHelper.serializeToStringWithExpose(this);
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
	public void update(Object object) {
		if (object != null && object instanceof InteractionShare) {
			InteractionShare is = ((InteractionShare) object);

			setId(is.getId());
			setCreationDate(is.getCreationDate());
			setOriginalPostId(is.getOriginalPostId());
			setAuthorId(is.getAuthorId());
			setAuthor(is.getAuthor());
			setAuthorUrl(is.getAuthorUrl());
			setVisible(is.isVisible());
			setASharing(is.isASharing());
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
		return (RealmModelListener) deserialize(json, InteractionShare.class);
	}

	//endregion


	//region == Getters and setters ==

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}

	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getOriginalPostId() {
		return originalPostId;
	}
	public void setOriginalPostId(String originalPostId) {
		this.originalPostId = originalPostId;
	}

	public String getAuthorId() {
		return authorId;
	}
	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthorUrl() {
		return authorUrl;
	}
	public void setAuthorUrl(String authorUrl) {
		this.authorUrl = authorUrl;
	}

	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isASharing() {
		return isASharing;
	}
	public void setASharing(boolean ASharing) {
		isASharing = ASharing;
	}

	//endregion
}
