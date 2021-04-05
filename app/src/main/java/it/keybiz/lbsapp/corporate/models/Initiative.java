/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.models.enums.InitiativeTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 1/22/2018.
 */
@RealmClass
public class Initiative implements RealmModel, RealmModelListener, Serializable,
		JsonHelper.JsonDeSerializer {

	@Expose private String type;
	@Ignore private InitiativeTypeEnum typeEnum;

	@Expose private String dateUpUntil;
	@Expose private long heartsToTransfer;
	@Expose private String recipient;
	@Expose private String text;
	@Expose private String dateCreation;
	@Ignore private transient Date dateCreationObject;
	@Ignore private transient Date dateUpUntilObject;


	public Initiative() {}


	public Initiative deserializeToClass(JSONObject json) {
		return (Initiative) deserialize(json, Initiative.class);
	}

	public boolean isTransferHearts() {
		return getTypeEnum() == InitiativeTypeEnum.TRANSFER_HEARTS;
	}

	public boolean isCollectHearts() {
		return getTypeEnum() == InitiativeTypeEnum.COLLECT_HEARTS;
	}

	public boolean isGiveSupport() {
		return getTypeEnum() == InitiativeTypeEnum.GIVE_SUPPORT;
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


	//region == Getters and setters

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	public InitiativeTypeEnum getTypeEnum() {
		return typeEnum != null ? typeEnum : (typeEnum = InitiativeTypeEnum.toEnum(getType()));
	}
	public void setTypeEnum(InitiativeTypeEnum typeEnum) {
		this.typeEnum = typeEnum;
	}

	public String getDateUpUntil() {
		return dateUpUntil;
	}
	public void setDateUpUntil(String dateUpUntil) {
		this.dateUpUntil = dateUpUntil;
	}

	public long getHeartsToTransfer() {
		return heartsToTransfer;
	}
	public void setHeartsToTransfer(long heartsToTransfer) {
		this.heartsToTransfer = heartsToTransfer;
	}

	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

	public String getDateCreation() {
		return dateCreation;
	}
	public void setDateCreation(String dateCreation) {
		this.dateCreation = dateCreation;
	}

	public Date getDateCreationObject() {
		return dateCreationObject != null ? dateCreationObject : Utils.getDateFromDB(getDateCreation());
	}
	public void setDateCreationObject(Date dateCreationObject) {
		this.dateCreationObject = dateCreationObject;
	}

	public Date getDateUpUntilObject() {
		return dateUpUntilObject != null ? dateUpUntilObject : Utils.getDateFromDB(getDateUpUntil());
	}
	public void setDateUpUntilObject(Date dateUpUntilObject) {
		this.dateUpUntilObject = dateUpUntilObject;
	}

	//endregion

}
