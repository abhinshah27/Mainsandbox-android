/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.text.Spanned;

import com.google.gson.JsonElement;

import org.json.JSONObject;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Represents the basic element for the list item during the Help settings navigation.
 * @author mbaldrighi on 3/29/2018.
 */
public class SettingsHelpElement implements Serializable, JsonHelper.JsonDeSerializer {

	private String name = "";
	private String action = "";
	private String nextItem = "root";
	private String nextNavigationID = "";
	private String text = "";
	private String title = "";

	@Override
	public int hashCode() {
		if (Utils.isStringValid(name))
			return name.hashCode();
		else
			return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SettingsHelpElement) {
//			if (Utils.isStringValid(_id) && Utils.isStringValid(((SettingsHelpElement) obj).getId()))
//				return _id.equals(((SettingsHelpElement) obj).getId());
//			else
				if (Utils.isStringValid(name) && Utils.isStringValid(((SettingsHelpElement) obj).getName()))
				return name.equals(((SettingsHelpElement) obj).getName());
		}

		return super.equals(obj);
	}

	public SettingsHelpElement() {}

	public SettingsHelpElement(String nextItem) {
		this.nextItem = nextItem;
	}

	public SettingsHelpElement(JSONObject json) {
		deserializeToClass(json);
	}


	public SettingsHelpElement deserializeToClass(JSONObject json) {
		return (SettingsHelpElement) deserialize(json, SettingsHelpElement.class);
	}


	public boolean hasAction() {
		return Utils.isStringValid(action);
	}

	public boolean hasNextItem() {
		return Utils.isStringValid(nextItem);
	}

	public boolean hasNextNavigationID() {
		return Utils.isStringValid(nextNavigationID);
	}

	public boolean isRoot() {
		return hasNextItem() && nextItem.equals("root");
	}

	public Spanned getTextToShow() {
		return Utils.getFormattedHtml(text);
	}


	//region == Serialization section ==

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

	//endregion


	//region == Getters and setters ==

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}

	public String getNextItem() {
		return nextItem;
	}
	public void setNextItem(String nextItem) {
		this.nextItem = nextItem;
	}

	public String getNextNavigationID() {
		return nextNavigationID;
	}
	public void setNextNavigationID(String nextNavigationID) {
		this.nextNavigationID = nextNavigationID;
	}

	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	//endregion

}
