/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;
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
public class InteractionComment implements Serializable, RealmModel, JsonHelper.JsonDeSerializer,
		RealmModelListener, Comparable<InteractionComment> {

	@PrimaryKey
	@SerializedName("commentID")
	@Expose private String id;

	@Expose private String date;
	private Date creationDate;

	@SerializedName("_id")
	@Expose private String postId;

	@SerializedName("userID")
	@Expose private String authorId;
	@SerializedName("name")
	@Expose private String author;
	@SerializedName("avatarURL")
	@Expose private String authorUrl;

	/**
	 * Defines an {@link InteractionComment} as COMMENT interaction.
	 */
	@Expose private String message = null;

	/**
	 * Defines whether the comment is a "main" comment or a sub-comment.
	 */
	@Expose private int level = 0;

	/**
	 * Defines whether a comment is visible to users or has been deleted by user.
	 */
	@SerializedName("isVisible")
	@Expose private boolean visible = true;

	private int totHearts;
	private boolean youLiked;

	@Expose private String parentCommentID;

	@Override
	public int compareTo(@NonNull InteractionComment comment) {
		long thisMillis = getCreationDate() != null ? getCreationDate().getTime() : Utils.getDateMillisFromDB(getDate());
		long oMillis = comment.getCreationDate() != null ? comment.getCreationDate().getTime() : Utils.getDateMillisFromDB(comment.getDate());

		return Long.compare(thisMillis, oMillis);
	}


	public InteractionComment() {}

	public InteractionComment(String id, Date date, String author, String authorUrl, String message) {
		this.id = id;
		this.creationDate = date;
		this.author = author;
		this.authorUrl = authorUrl;
		this.message = message;
	}

	@Override
	public int hashCode() {
		if (Utils.isStringValid(getId()))
			return getId().hashCode();

		return super.hashCode();
	}


	//region == Class custom methods ==

	public InteractionComment returnUpdatedInteraction(JSONObject json) {
		InteractionComment ic = (InteractionComment) updateWithReturn(json);
		ic.setCreationDate(Utils.getDateFromDB(ic.getDate()));
		return ic;
	}

	public boolean isActingUserCommentAuthor(HLUser user) {
		if (user == null) return false;

		String uid = user.getId();
		if (Utils.areStringsValid(uid, getAuthorId()))
			return uid.equals(getAuthorId());

		return false;
	}

	public boolean isSubComment() {
		return getLevel() > 0 && Utils.isStringValid(getParentCommentID());
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
	public void update(Object object) {
		if (object != null && object instanceof InteractionComment) {
			InteractionComment ic = ((InteractionComment) object);

			setId(ic.getId());
			setDate(ic.getDate());
			setCreationDate(ic.getCreationDate());
			setPostId(ic.getPostId());
			setAuthorId(ic.getAuthorId());
			setAuthor(ic.getAuthor());
			setAuthorUrl(ic.getAuthorUrl());
			setMessage(ic.getMessage());
			setLevel(ic.getLevel());
			setVisible(ic.isVisible());
			setTotHearts(ic.getTotHearts());
			setYouLiked(ic.isYouLiked());
			setParentCommentID(ic.getParentCommentID());
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
		return (RealmModelListener) deserialize(json, InteractionComment.class);
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

	public String getPostId() {
		return postId;
	}
	public void setPostId(String postId) {
		this.postId = postId;
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

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}

	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public int getTotHearts() {
		return totHearts;
	}
	public void setTotHearts(int totHearts) {
		this.totHearts = totHearts;
	}

	public boolean isYouLiked() {
		return youLiked;
	}
	public void setYouLiked(boolean youLiked) {
		this.youLiked = youLiked;
	}

	public String getParentCommentID() {
		return parentCommentID;
	}
	public void setParentCommentID(String parentCommentID) {
		this.parentCommentID = parentCommentID;
	}

	//endregion
}