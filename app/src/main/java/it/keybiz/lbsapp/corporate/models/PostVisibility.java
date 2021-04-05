/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * The nested object containing the user's info about birthday, relationship status, city, and description.
 *
 * @author mbaldrighi on 4/6/2018.
 */
@RealmClass
public class PostVisibility implements RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer,
		Serializable {

	@Expose private int rawVisibilityType = 0;
	@Expose private RealmList<String> values = new RealmList<>();


	public PostVisibility() {}

	public PostVisibility(int rawVisibilityType) {
		this.rawVisibilityType = rawVisibilityType;
	}

	public PostVisibility(int rawVisibilityType, RealmList<String> values) {
		this.rawVisibilityType = rawVisibilityType;
		this.values = values;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PostVisibility) {
			if (rawVisibilityType == ((PostVisibility) obj).rawVisibilityType &&
					rawVisibilityType != PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES.getValue()) {
				return true;
			}
			else {
				if (rawVisibilityType == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES.getValue()) {
					if (values != null && !values.isEmpty() &&
							((PostVisibility) obj).values != null && !((PostVisibility) obj).values.isEmpty()) {

						if (values.size() != ((PostVisibility) obj).values.size())
							return false;
						else {
							for (String s : ((PostVisibility) obj).values) {
								if (!values.contains(s))
									return false;
							}

							return true;
						}
					}
				}

				return false;
			}
		}

		return super.equals(obj);
	}

	public boolean hasChanged(PostVisibility obj) {
		// this
		if (rawVisibilityType == PrivacyPostVisibilityEnum.ONLY_SELECTED_USERS.getValue())
			rawVisibilityType = PrivacyPostVisibilityEnum.INNER_CIRCLE.getValue();

		return !equals(obj);
	}

	public void addValue(String name) {
		if (Utils.isStringValid(name) && values != null) {
			if (!values.contains(name))
				values.add(name);
		}
	}

	public boolean removeValue(String name) {
		if (Utils.isStringValid(name) && values != null) {
			if (values.contains(name)) {
				values.remove(name);
				return true;
			}
		}
		return false;
	}

	public void resetValues() {
		if (values == null)
			values = new RealmList<>();
		else
			values.clear();
	}

	public boolean hasValues() {
		return values != null && !values.isEmpty();
	}

	public ArrayList<String> getValuesArrayList() {
		if (hasValues()) {
			ArrayList<String> result = new ArrayList<>();
			result.addAll(values);
			return result;
		}

		return null;
	}

	public void setValuesArrayList(ArrayList<String> values) {
		if (this.values == null)
			this.values = new RealmList<>();
		else
			this.values.clear();

		if (values != null)
			this.values.addAll(values);
	}


	//region == Realm model methods ==

	@Override
	public void reset() {}

	@Override
	public Object read(Realm realm) {
		return read(realm, PostVisibility.class);
	}

	@Override
	public RealmModel read(Realm realm, Class<? extends RealmModel> model) {
		return RealmUtils.readFirstFromRealm(realm, this.getClass());
	}

	@Override
	public void deserializeStringListFromRealm() {}

	@Override
	public void serializeStringListForRealm() {}

	@Override
	public void write(Realm realm) {
		RealmUtils.writeToRealm(realm,this);
	}

	@Override
	public void write(JSONObject json) {}

	@Override
	public void write(Object object) {}

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

	public int getRawVisibilityType() {
		return rawVisibilityType;
	}
	public void setRawVisibilityType(int rawVisibilityType) {
		this.rawVisibilityType = rawVisibilityType;
	}

	public RealmList<String> getValues() {
		return values;
	}
	public void setValues(RealmList<String> values) {
		this.values = values;
	}


	//endregion

}
