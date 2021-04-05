/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import com.google.gson.JsonElement;

import org.json.JSONObject;

import java.io.Serializable;

import io.realm.RealmModel;
import io.realm.annotations.RealmClass;

/**
 * @author mbaldrighi on 1/26/2018.
 */
@RealmClass
public class MoreInfoObject implements RealmModel, Serializable, JsonHelper.JsonDeSerializer {

	private String iconImageLink;
	private String text;


	public MoreInfoObject() {}

	public MoreInfoObject(String iconImageLink, String text) {
		setIconImageLink(iconImageLink);
		setText(text);
	}


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


	//region == Getters and setters ==

	public String getIconImageLink() {
		return iconImageLink;
	}
	public void setIconImageLink(String iconImageLink) {
		this.iconImageLink = iconImageLink;
	}

	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

	//endregion
}
