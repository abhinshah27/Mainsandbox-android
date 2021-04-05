/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;

import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 12/5/2017.
 */
@RealmClass
public class LifeEvent implements RealmModel, Serializable, JsonHelper.JsonDeSerializer, Comparable<LifeEvent> {

	@Expose private String date;
	private Date dateObj;
	@Expose private String description;


	public LifeEvent() {}

	public LifeEvent(String date, String description) {
		setDate(date);
		if (Utils.isStringValid(getDate()))
			setDateObj(Utils.getDateFromDB(getDate()));
		setDescription(description);
	}

	@Override
	public int compareTo(final @NonNull LifeEvent lifeEvent) {
		if (getDateObj() == null && Utils.isStringValid(getDate()))
			setDateObj(Utils.getDateFromDB(getDate()));
		if (lifeEvent.getDateObj() == null && Utils.isStringValid(lifeEvent.getDate()))
			lifeEvent.setDateObj(Utils.getDateFromDB(lifeEvent.getDate()));

		if (getDateObj() != null && lifeEvent.getDateObj() != null) {
			return lifeEvent.getDateObj().compareTo(getDateObj());
		}

		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof LifeEvent) {
			if (Utils.areStringsValid(getDate(), getDescription()) &&
					Utils.areStringsValid(((LifeEvent) obj).getDate(), ((LifeEvent) obj).getDescription())) {
				return getDate().equals(((LifeEvent) obj).getDate()) &&
						getDescription().equals(((LifeEvent) obj).getDescription());
			}
		}

		return super.equals(obj);
	}


	public String getFormattedDate() {
		if (getDateObj() == null && Utils.isStringValid(getDate())) {
			Realm realm = null;
			try {
				realm = RealmUtils.getCheckedRealm();
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						setDateObj(Utils.getDateFromDB(getDate()));
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RealmUtils.closeRealm(realm);
			}
		}

		if (getDateObj() != null)
			return new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(getDateObj());

		return "";
	}

	public static Date formatNewDate(String dateString) throws ParseException {
		if (Utils.isStringValid(dateString))
			return new SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(dateString);

		return null;
	}

	public boolean isValid() {
		return dateObj != null && Utils.areStringsValid(date, description);
	}

	public boolean isVoid() {
		return dateObj == null && !Utils.isStringValid(date) && !Utils.isStringValid(description);
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

	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		if (Utils.isStringValid(date)) {
			try {
				Date d = formatNewDate(date);
				if (d != null)
					date = Utils.formatDateForDB(d);
			} catch (ParseException e) {
				e.printStackTrace();
				date = null;
			}
		}
		this.date = date;
	}

	public Date getDateObj() {
		return dateObj;
	}
	public void setDateObj(Date dateObj) {
		this.dateObj = dateObj;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	//endregion
}
