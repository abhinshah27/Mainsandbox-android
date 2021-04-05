/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import com.google.gson.JsonElement;

import org.json.JSONObject;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Represents the basic element for the list item during the wish creation process.
 * @author mbaldrighi on 2/19/2018.
 */
public class WishListElement implements Serializable, JsonHelper.JsonDeSerializer {

	private int idWish;

	private String idStep = "";
	private String name = "";
	private String action = "";
	private String nextItem = "";
	private String nextNavigationID = "";
	private int step, substep;
	private int stepsTotal, substepTotal;

	private String avatarURL = "";
	private String friendID = "";
	private String id = "";
	private String date = "";

	private boolean isSelected = false;

	@Override
	public int hashCode() {
		if (Utils.isStringValid(name))
			return name.hashCode();
		else
			return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WishListElement) {
			if (Utils.areStringsValid(friendID, ((WishListElement) obj).getFriendID()))
				return friendID.equals(((WishListElement) obj).getFriendID());
			else if (Utils.areStringsValid(id, ((WishListElement) obj).getId()))
				return id.equals(((WishListElement) obj).getId());
			else {
				return Utils.areStringsValid(idStep, ((WishListElement) obj).getIdStep()) && idStep.equals(((WishListElement) obj).getIdStep()) &&
						Utils.areStringsValid(name, ((WishListElement) obj).getName()) && name.equals(((WishListElement) obj).getName()) &&
						Utils.areStringsValid(nextItem, ((WishListElement) obj).getNextItem()) && nextItem.equals(((WishListElement) obj).getNextItem()) &&
						Utils.areStringsValid(nextNavigationID, ((WishListElement) obj).getNextNavigationID()) && nextNavigationID.equals(((WishListElement) obj).getNextNavigationID()) &&
						(
								(action == null && ((WishListElement) obj).getAction() == null) ||
								(action != null && ((WishListElement) obj).getAction() != null && action.equals(((WishListElement) obj).getAction()))
						) &&
						step == ((WishListElement) obj).getStep() && substep == ((WishListElement) obj).getSubstep() &&
						stepsTotal == ((WishListElement) obj).getStepsTotal() && substepTotal == ((WishListElement) obj).getSubstepTotal();
			}
		}

		return super.equals(obj);
	}

	public WishListElement() {}

	public WishListElement(String nextItem) {
		this.nextItem = nextItem;
	}

	public WishListElement(JSONObject json) {
		deserializeToClass(json);
	}


	public WishListElement deserializeToClass(JSONObject json) {
		return (WishListElement) deserialize(json, WishListElement.class);
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

	public boolean needsToReplaceLastStackElement(WishListElement lastElement) {
		return !equals(lastElement) && (step == lastElement.getStep()) && (substep == lastElement.getSubstep());
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

	public int getIdWish() {
		return idWish;
	}
	public void setIdWish(int idWish) {
		this.idWish = idWish;
	}

	public String getIdStep() {
		return idStep;
	}
	public void setIdStep(String idStep) {
		this.idStep = idStep;
	}

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

	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}

	public int getStep() {
		return step;
	}
	public void setStep(int step) {
		this.step = step;
	}

	public int getStepsTotal() {
		return stepsTotal;
	}
	public void setStepsTotal(int stepsTotal) {
		this.stepsTotal = stepsTotal;
	}

	public int getSubstep() {
		return substep;
	}
	public void setSubstep(int substep) {
		this.substep = substep;
	}

	public int getSubstepTotal() {
		return substepTotal;
	}
	public void setSubstepTotal(int substepTotal) {
		this.substepTotal = substepTotal;
	}

	public String getAvatarURL() {
		return avatarURL;
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public String getFriendID() {
		return friendID;
	}
	public void setFriendID(String friendID) {
		this.friendID = friendID;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public boolean isSelected() {
		return isSelected;
	}
	public void setSelected(boolean selected) {
		this.isSelected = selected;
	}

	//endregion

}
