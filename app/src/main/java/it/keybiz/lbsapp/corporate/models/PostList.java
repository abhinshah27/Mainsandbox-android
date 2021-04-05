/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 12/29/2017.
 */
public class PostList implements Serializable, JsonHelper.JsonDeSerializer {

	@SerializedName("_id")
	private String id;

	@SerializedName(value = "name", alternate = "listName")
	@Expose private String name;
	@SerializedName("data")
	private List<Post> posts = new ArrayList<>();

	private int sortOrder;
	private boolean moreData;
	private String nameToDisplay;

	public PostList() {}

	public PostList(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}


	public static Comparator<PostList> ListSortOrderComparator = new Comparator<PostList>() {
		@Override
		public int compare(PostList o1, PostList o2) {
			return Integer.compare(o1.getSortOrder(), o2.getSortOrder());
		}
	};

	public static Comparator<PostList> ListNameComparator = new Comparator<PostList>() {
		@Override
		public int compare(PostList o1, PostList o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};


	public PostList deserializeToClass(String jsonString) {
		return (PostList) deserialize(jsonString, PostList.class);
	}

	public PostList deserializeToClass(JSONObject json) {
		return (PostList) deserialize(json, PostList.class);
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
		if (!Utils.isStringValid(((PostList) des).getId()))
			((PostList) des).setId(UUID.randomUUID().toString());
		return des;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		return null;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
		JsonHelper.JsonDeSerializer des =  JsonHelper.deserialize(jsonString, myClass);
		if (des != null && !Utils.isStringValid(((PostList) des).getId()))
			((PostList) des).setId(UUID.randomUUID().toString());
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

	public List<Post> getPosts() {
		return posts;
	}
	public void setPosts(List<Post> posts) {
		this.posts = posts;
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
