/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Locale;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 5/21/2018.
 */
public class MarketPlace implements Serializable, JsonHelper.JsonDeSerializer, Comparable<MarketPlace> {

	@SerializedName("mktID")
	private String id;

	@SerializedName("vendorName")
	private String name;
	private String avatarURL;
	private String description;
	private double conversionRate;

	private transient long currentConversion;

	private transient boolean selected;


	public MarketPlace() {}

	public MarketPlace(String name, double conversionRate) {
		this.name = name;
		this.conversionRate = conversionRate;
	}


	@Override
	public int hashCode() {
		if (Utils.isStringValid(getId()))
			return getId().hashCode();
		else
			return super.hashCode();
	}

	@Override
	public int compareTo(@NonNull MarketPlace o) {
		if (Utils.areStringsValid(this.getName(), o.getName()))
			return this.getName().compareTo(o.getName());

		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		return
				obj instanceof MarketPlace &&
						Utils.areStringsValid(id, ((MarketPlace) obj).getId()) &&
						id.equals(((MarketPlace) obj).getId());
	}

	public MarketPlace deserializeToClass(JSONObject json) {
		return (MarketPlace) deserialize(json, MarketPlace.class);
	}

	public void setCurrentConversion(long currentInsertedHearts, double heartsValue) {
		double res = currentInsertedHearts * heartsValue * conversionRate;
		double toFloor = ((long) res) / 10;
		double floored = Math.floor(toFloor);

		currentConversion = ((long) floored) * 10;
	}

	public long revertCurrentConversion(double heartsValue) {
		double res = currentConversion / heartsValue / conversionRate;
		return ((long) res);
	}

	public boolean isConversionValid() {
		return currentConversion >= Constants.HL_MINIMUM_REDEMPTION;
	}

	public String getReadableMoneyConverted() {
		return String.format(Locale.US, "$%s", Utils.formatNumberWithCommas(currentConversion));
	}

	public String getReadableHeartsReconverted(Context context, long reconvertedHearts) {
		return context.getString(R.string.redeem_amount_declared, Utils.formatNumberWithCommas(reconvertedHearts));
	}

	public boolean isCash(Context context) {
		return Utils.isStringValid(name) && name.equals(context.getString(R.string.redeem_method_cash));
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


	//region == Getters and setters

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

	public String getAvatarURL() {
		return avatarURL;
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public double getConversionRate() {
		return conversionRate;
	}
	public void setConversionRate(double conversionRate) {
		this.conversionRate = conversionRate;
	}

	public long getCurrentConversion() {
		return currentConversion;
	}
	public void setCurrentConversion(long currentConversion) {
		this.currentConversion = currentConversion;
	}

	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	//endregion

}
