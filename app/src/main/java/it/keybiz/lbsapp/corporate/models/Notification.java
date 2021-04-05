/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

import it.keybiz.lbsapp.corporate.models.enums.NotificationTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.RequestsStatusEnum;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 12/5/2017.
 */
public class Notification extends HLModel implements Serializable, JsonHelper.JsonDeSerializer, Comparable<Notification> {

	@SerializedName("notificationID")
	private String id;

	@SerializedName(alternate = "idPost", value = "postID")
	private String postId;

	private String interactionID;

	@SerializedName("date")
	private String dateString;
	private transient Date date;

	@SerializedName("type")
	private String typeString;
	private transient NotificationTypeEnum type;

	@SerializedName("userID")
	private String userId;
	private String name;
	private String avatarURL;
	private String text;

	@SerializedName("status")
	private String statusString;
	private transient RequestsStatusEnum status;

	private boolean read;
	@SerializedName("isARequest")
	private boolean request;

	private String docUrl;

	@Override
	public int hashCode() {
		return getId().hashCode();
	}


	@Override
	public int compareTo(@NonNull Notification o) {
		long thisMillis = getDate() != null ? getDate().getTime() : Utils.getDateMillisFromDB(getDateString());
		long oMillis = o.getDate() != null ? o.getDate().getTime() : Utils.getDateMillisFromDB(o.getDateString());

		return Long.compare(oMillis, thisMillis);
	}


	public Notification returnUpdatedNotification(JSONObject json) {
		Notification n = (Notification) updateWithReturn(json);
		if (Utils.isStringValid(n.getDateString()))
			n.setDate(Utils.getDateFromDB(n.getDateString()));
		if (Utils.isStringValid(n.getTypeString()))
			n.setType(NotificationTypeEnum.toEnum(n.getTypeString()));
		if (n.isRequest() && Utils.isStringValid(n.getStatusString())) {
			n.setStatus(RequestsStatusEnum.toEnum(n.getStatusString()));
		}
		return n;
	}

	public Notification returnUpdatedNotification(Notification notification) {
		return (Notification) updateWithReturn(notification);
	}

	@Override
	public void update(Object object) {
		super.update(object);

		if (object instanceof Notification) {
			Notification n = ((Notification) object);

			this.id = n.getId();
			this.postId = n.getPostId();
			this.interactionID = n.getInteractionID();
			this.dateString = n.getDateString();
			this.date = n.getDate();
			this.typeString = n.getTypeString();
			this.type = n.getType();
			this.userId = n.getUserId();
			this.name = n.getName();
			this.avatarURL = n.getAvatarURL();
			this.text = n.getText();
			this.statusString = n.getStatusString();
			this.status = n.getStatus();
			this.read = n.isRead();
			this.request = n.isRequest();
		}
	}

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

	@Override
	public HLModel updateWithReturn(JSONObject json) {
		return (HLModel) deserialize(json, Notification.class);
	}


//region == Getters and setters ==

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getPostId() {
		return postId;
	}
	public void setPostId(String postId) {
		this.postId = postId;
	}

	public String getInteractionID() {
		return interactionID;
	}
	public void setInteractionID(String interactionID) {
		this.interactionID = interactionID;
	}

	public String getDateString() {
		return dateString;
	}
	public void setDateString(String dateString) {
		this.dateString = dateString;
	}

	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	public NotificationTypeEnum getType() {
		return type;
	}
	public void setType(NotificationTypeEnum type) {
		this.type = type;
	}

	public String getTypeString() {
		return typeString;
	}
	public void setTypeString(String typeString) {
		this.typeString = typeString;
	}

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
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

	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

	public boolean isRead() {
		return read;
	}
	public void setRead(boolean read) {
		this.read = read;
	}

	public boolean isRequest() {
		return request;
	}
	public void setRequest(boolean request) {
		this.request = request;
	}

	public String getStatusString() {
		return statusString;
	}
	public void setStatusString(String statusString) {
		this.statusString = statusString;
	}

	public RequestsStatusEnum getStatus() {
		return status;
	}
	public void setStatus(RequestsStatusEnum status) {
		this.status = status;
	}

	public String getDocUrl() {
		return docUrl;
	}
	public void setDocUrl(String docUrl) {
		this.docUrl = docUrl;
	}

	//endregion

}
