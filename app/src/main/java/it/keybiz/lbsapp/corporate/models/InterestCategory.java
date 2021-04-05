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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 11/10/2017.
 */
@RealmClass
public class InterestCategory implements RealmModel, RealmModelListener, Serializable, JsonHelper.JsonDeSerializer {

	@SerializedName("_id")
	private String id;

	// don't know what it does
	private String categoryID;

	@SerializedName(value = "name", alternate = "listName")
	@Expose private String name;
	@SerializedName("data")
	@Ignore private List<Interest> interests = new ArrayList<>();

	private int sortOrder;
	private String nameToDisplay;

	private boolean moreData;


	public InterestCategory() {}

	public InterestCategory(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}


	public static Comparator<InterestCategory> CategorySortOrderComparator = new Comparator<InterestCategory>() {
		@Override
		public int compare(InterestCategory o1, InterestCategory o2) {
			return Integer.compare(o1.getSortOrder(), o2.getSortOrder());
		}
	};

	public static Comparator<InterestCategory> CategoryNameComparator = new Comparator<InterestCategory>() {
		@Override
		public int compare(InterestCategory o1, InterestCategory o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};


	public InterestCategory deserializeToClass(String jsonString) {
		return (InterestCategory) deserialize(jsonString, InterestCategory.class);
	}

	public InterestCategory deserializeToClass(JSONObject json) {
		return (InterestCategory) deserialize(json, InterestCategory.class);
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
		if (!Utils.isStringValid(((InterestCategory) des).getId()))
			((InterestCategory) des).setId(UUID.randomUUID().toString());
		return des;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		return null;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
		JsonHelper.JsonDeSerializer des =  JsonHelper.deserialize(jsonString, myClass);
		if (des != null && !Utils.isStringValid(((InterestCategory) des).getId()))
			((InterestCategory) des).setId(UUID.randomUUID().toString());
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


	//region == Getters and setters ==

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
	
	
	//region == Getters and setters ==

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getCategoryID() {
		return categoryID;
	}
	public void setCategoryID(String categoryID) {
		this.categoryID = categoryID;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public List<Interest> getInterests() {
		return interests;
	}
	public void setInterests(List<Interest> interests) {
		this.interests = interests;
	}

	public int getSortOrder() {
		return sortOrder;
	}
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getNameToDisplay() {
		return nameToDisplay;
	}
	public void setNameToDisplay(String nameToDisplay) {
		this.nameToDisplay = nameToDisplay;
	}

	public boolean hasMoreData() {
		return moreData;
	}
	public void setMoreData(boolean moreData) {
		this.moreData = moreData;
	}

	//endregion

}
