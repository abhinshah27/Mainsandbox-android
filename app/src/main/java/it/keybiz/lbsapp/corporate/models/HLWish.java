/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 3/14/2018.
 */
/*@RealmClass*/
public class HLWish implements /*RealmModel, RealmModelListener,*/ Serializable,
		JsonHelper.JsonDeSerializer, Comparable<HLWish> {

	@SerializedName("_id")
	@Expose private String id;
	@Expose private String wishName;
	@Expose private String coverURL;


	public HLWish() {}


	@Override
	public int hashCode() {
		if (Utils.isStringValid(getId()))
			return getId().hashCode();
		else
			return super.hashCode();
	}

	@Override
	public int compareTo(@NonNull HLWish o) {
		if (Utils.areStringsValid(this.getName(), o.getName()))
			return this.getName().compareTo(o.getName());

		return 0;
	}



	public HLWish deserializeToClass(JSONObject json) {
		return (HLWish) deserialize(json, HLWish.class);
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

	/*
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
	public void deserializeStringListFromRealm() throws JSONException {
		if (Utils.isStringValid(getCategoriesRealm())) {
			JSONArray arr = new JSONArray(getCategoriesRealm());

			if (getCategories() == null)
				setCategories(new ArrayList<Integer>());
			getCategories().clear();
			for (int i = 0; i < arr.length(); i++) {
				getCategories().add(arr.getInt(i));
			}
		}
	}

	@Override
	public void serializeStringListForRealm() {
		if (getCategories() != null && !getCategories().isEmpty())
			setCategoriesRealm(new Gson().toJson(getCategories()));
		else
			setCategoriesRealm(null);
	}

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
	*/

	//region == Getters and setters

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return wishName;
	}
	public void setName(String name) {
		this.wishName = name;
	}

	public String getCoverURL() {
		return coverURL;
	}
	public void setCoverURL(String coverURL) {
		this.coverURL = coverURL;
	}

	//endregion

}
