/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Singleton class containing and handling all the operations on and about {@link Interest} objects.
 * @author mbaldrighi on 12/27/2017.
 */
public class HLInterests {

	public static final String LOG_TAG = HLInterests.class.getCanonicalName();

	private static HLInterests instance = null;
	private static final Object mutex = new Object();

	private Map<String, Interest> interestsMap;
	private List<Interest> featuredInterests;
	private List<InterestCategory> categories;


	public static HLInterests getInstance() {
		if (instance == null) {
			synchronized (mutex){
				if (instance == null)
					instance = new HLInterests();
			}
		}

		return instance;
	}

	private HLInterests() {
		init();
	}

	/**
	 * This method initializes vars like maps and lists. It is called at the singleton instantiation.
	 */
	private void init() {
		interestsMap = new ConcurrentHashMap<>();
		featuredInterests = new ArrayList<>();
		categories = new ArrayList<>();
	}

	public void setInterests(String array, @NonNull String userId) throws JSONException {
		if (Utils.isStringValid(array)) {
			setInterests(new JSONArray(array), userId);
		}
	}

	public void setInterests(JSONArray jsonArray, @NonNull String userId) throws JSONException {
		if (jsonArray != null && jsonArray.length() > 0) {

			if (interestsMap == null)
				interestsMap = new ConcurrentHashMap<>();
			else
				interestsMap.clear();

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject j = jsonArray.getJSONObject(i);
				if (j != null) {
					String id = j.optString("_id");
					if (Utils.isStringValid(id)) {

						Interest interest = new Interest().deserializeToClass(j, userId);
						if (!interestsMap.containsKey(id))
							interestsMap.put(id, interest);
						else
							interestsMap.get(id).update(interest);
					}
				}
			}
		}
	}

	public void setFeaturedInterests(JSONArray jsonArray) throws JSONException {
		if (jsonArray != null && jsonArray.length() > 0) {
			if (featuredInterests == null)
				featuredInterests = new ArrayList<>();
			else
				featuredInterests.clear();

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject j = jsonArray.getJSONObject(i);
				if (j != null) {
					Interest interest = new Interest().deserializeToClass(j);
					featuredInterests.add(interest);
				}
			}
		}
	}

	public void setGeneralCategories(JSONArray jsonArray) throws JSONException {
		if (jsonArray != null && jsonArray.length() > 0) {
			if (categories == null)
				categories = new ArrayList<>();
			else
				categories.clear();

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject j = jsonArray.getJSONObject(i);
				if (j != null) {
					InterestCategory category = new InterestCategory().deserializeToClass(j);
					categories.add(category);
				}
			}
		}
	}

	public void setInterest(Interest interest) {
		if (interest != null) {
			String id = interest.getId();
			if (Utils.isStringValid(id)) {
				if (!interestsMap.containsKey(id)) {
					interestsMap.put(id, interest);
				}
				else {
					interestsMap.get(id).update(interest);
				}
			}
		}
	}

	public boolean hasInterest(@NonNull String id) {
		return  interestsMap != null && !interestsMap.isEmpty() && interestsMap.containsKey(id);

	}

	public Interest getInterest(@NonNull String id) {
		if (hasInterest(id))
			return interestsMap.get(id);

		return null;
	}

	public Collection<Interest> getInterests() {
		if (interestsMap != null)
			return interestsMap.values();

		return new ArrayList<>();
	}

	public Collection<Interest> getSortedInterests() {
		if (interestsMap != null) {
			List<Interest> list = new ArrayList<>(interestsMap.values());
			Collections.sort(list);
			return list;
		}

		return new ArrayList<>();
	}

	public int getInterstsCount() {
		if (interestsMap != null)
			return interestsMap.size();

		return 0;
	}

	public void removeInterest(String id) {
		if (Utils.isStringValid(id)) {
			if (interestsMap != null)
				interestsMap.remove(id);
		}
	}


	public void clearInterests() {
		if (interestsMap != null)
			interestsMap.clear();
	}


	//region == Getters and setters ==

	public List<Interest> getFeaturedInterests() {
		return featuredInterests;
	}
	public void setFeaturedInterests(List<Interest> featuredInterests) {
		this.featuredInterests = featuredInterests;
	}

	public List<InterestCategory> getCategories() {
		return categories;
	}
	public void setCategories(List<InterestCategory> categories) {
		this.categories = categories;
	}

	//endregion

}
