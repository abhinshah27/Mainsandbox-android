/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import org.json.JSONObject;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Wrapper utility class providing methods to serialize and deserialize project classes using {@link Gson}.
 *
 * @author mbaldrighi on 10/3/2017.
 */
public class JsonHelper {

	public interface JsonDeSerializer {

		JsonElement serializeWithExpose();
		String serializeToStringWithExpose();

		JsonElement serialize();
		String serializeToString();

		JsonDeSerializer deserialize(JSONObject json, Class myClass);

		JsonDeSerializer deserialize(JsonElement json, Class myClass);

		JsonDeSerializer deserialize(String jsonString, Class myClass);

		Object getSelfObject();
	}


	public static JsonElement serializeWithExpose(JsonDeSerializer object) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJsonTree(object);
	}

	public static String serializeToStringWithExpose(JsonDeSerializer object) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJson(object);
	}

	public static JsonElement serialize(JsonDeSerializer object) {
		return new Gson().toJsonTree(object);
	}

	public static String serializeToString(JsonDeSerializer object) {
		return new Gson().toJson(object);
	}

	public static JsonDeSerializer deserialize(String jsonString, Class myClass) {
		if (Utils.isStringValid(jsonString))
			return (JsonDeSerializer) new Gson().fromJson(jsonString, myClass);

		return null;
	}

	public static JsonDeSerializer deserialize(JSONObject json, Class myClass) {
		if (json != null)
			return deserialize(json.toString(), myClass);

		return null;
	}

	public static JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		if (json != null)
			return (JsonDeSerializer) new Gson().fromJson(json, myClass);

		return null;
	}

}
