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
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 11/10/2017.
 */
@RealmClass
public class HLCircle implements Serializable, JsonHelper.JsonDeSerializer, RealmModel, RealmModelListener {

	@SerializedName("_id")
	private String id;
	@SerializedName(value = "name", alternate = "listName")
	@Expose private String name;

	@SerializedName(value = "data", alternate = "users")
	@Ignore private RealmList<HLUserGeneric> users = new RealmList<>();

	private int sortOrder;
	private boolean moreData;

	@SerializedName("UIName")
	private String nameToDisplay;

	@Ignore private boolean selected;

	public HLCircle() {}

	public HLCircle(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		if (Utils.isStringValid(getName()))
			return getName().hashCode();

		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HLCircle && Utils.areStringsValid(getName(), ((HLCircle) obj).getName())) {
			return getName().equals(((HLCircle) obj).getName());
		}
		return super.equals(obj);
	}

	public static Comparator<HLCircle> CircleNameComparator = new Comparator<HLCircle>() {
		@Override
		public int compare(HLCircle o1, HLCircle o2) {
			if (Utils.areStringsValid(o1.getName(), o2.getName())) {
				if (o1.getName().equals(Constants.INNER_CIRCLE_NAME))
					return -1;
				else if (o2.getName().equals(Constants.INNER_CIRCLE_NAME))
					return 1;
				if (o1.getName().equals(Constants.CIRCLE_FAMILY_NAME))
					return -1;
				else if (o2.getName().equals(Constants.CIRCLE_FAMILY_NAME))
					return 1;
				else
					return o1.getName().compareTo(o2.getName());
			}

			return 0;
		}
	};

	public static Comparator<HLCircle> CircleNameComparatorForFilter = new Comparator<HLCircle>() {
		@Override
		public int compare(HLCircle o1, HLCircle o2) {
			if (o1.getName().equals(Constants.CIRCLE_FAMILY_NAME))
				return -1;
			else if (o2.getName().equals(Constants.CIRCLE_FAMILY_NAME))
				return 1;
			else
				return o1.getName().compareTo(o2.name);
		}
	};

	public static Comparator<HLCircle> CircleSortOrderComparator = new Comparator<HLCircle>() {
		@Override
		public int compare(HLCircle o1, HLCircle o2) {
			return Integer.compare(o1.getSortOrder(), o2.getSortOrder());
		}
	};


	public boolean hasMembers() {
		return getUsers() != null && !getUsers().isEmpty();
	}


	public HLCircle deserializeToClass(String jsonString) {
		return (HLCircle) deserialize(jsonString, HLCircle.class);
	}

	public HLCircle deserializeToClass(JSONObject json) {
		return (HLCircle) deserialize(json, HLCircle.class);
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
		JsonHelper.JsonDeSerializer des = JsonHelper.deserialize(json, myClass);
		if (!Utils.isStringValid(((HLCircle) des).getId()))
			((HLCircle) des).setId(UUID.randomUUID().toString());
		return des;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		return null;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
		JsonHelper.JsonDeSerializer des =  JsonHelper.deserialize(jsonString, myClass);
		if (des != null && !Utils.isStringValid(((HLCircle) des).getId()))
			((HLCircle) des).setId(UUID.randomUUID().toString());
		return des;
	}

	@Override
	public Object getSelfObject() {
		return this;
	}

	/*
	public HLCircle deserializeSpecial(JSONObject json) {
		return deserializeSpecial(json.toString());
	}

	public HLCircle deserializeSpecial(String jsonString) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(HLCircle.class, new HLCircle.CircleDeserializer())
				.create();
		return gson.fromJson(jsonString, HLCircle.class);
	}

	private class CircleDeserializer implements JsonDeserializer<HLCircle> {

		@Override
		public HLCircle deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			HLCircle circle = new HLCircle();
			JsonObject jsonObject = json.getAsJsonObject();
			if (jsonObject.has("data")) {
				JsonObject elem = jsonObject.getAsJsonObject("person");
				if (elem != null && !elem.isJsonNull()) {
					if (elem.has("avatarURL"))
						circle.setAvatarURL(elem.get("avatarURL").getAsString());
					if (elem.has("wallImageLink"))
						circle.setWallImageLink(elem.get("wallImageLink").getAsString());
					if (elem.has("firstName"))
						circle.setFirstName(elem.get("firstName").getAsString());
					if (elem.has("lastName"))
						circle.setLastName(elem.get("lastName").getAsString());
					if (elem.has("totHeartsSinceSubscribed"))
						circle.setTotHearts(elem.get("totHeartsSinceSubscribed").getAsInt());
				}
			}

			return circle;
		}
	}
	*/

	//endregion


	//region == Realm section ==

	@Override
	public void reset() {

	}

	@Override
	public Object read(@Nullable Realm realm) {
		return null;
	}

	@Override
	public RealmModel read(Realm realm, Class<? extends RealmModel> model) {
		return null;
	}

	@Override
	public void deserializeStringListFromRealm() {

	}

	@Override
	public void serializeStringListForRealm() {

	}

	@Override
	public void write(@Nullable Realm realm) {

	}

	@Override
	public void write(Object object) {

	}

	@Override
	public void write(JSONObject json) {

	}

	@Override
	public void write(Realm realm, RealmModel model) {

	}

	@Override
	public void update() {

	}

	@Override
	public void update(Object object) {

	}

	@Override
	public void update(JSONObject json) {

	}

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


	//region == Getters and setters ==

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

	public RealmList<HLUserGeneric> getUsers() {
		return users;
	}
	public void setUsers(RealmList<HLUserGeneric> users) {
		this.users = users;
	}

	public int getSortOrder() {
		return sortOrder;
	}
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public boolean hasMoreData() {
		return moreData;
	}
	public void setMoreData(boolean moreData) {
		this.moreData = moreData;
	}

	public String getNameToDisplay() {
		return nameToDisplay;
	}
	public void setNameToDisplay(String nameToDisplay) {
		this.nameToDisplay = nameToDisplay;
	}

	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	//endregion

}
