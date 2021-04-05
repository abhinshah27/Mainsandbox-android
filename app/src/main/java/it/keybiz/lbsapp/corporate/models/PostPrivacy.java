/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;

/**
 * The nested object containing the post's info about privacy and visibility.
 *
 * @author mbaldrighi on 4/24/2018.
 */
@RealmClass
public class PostPrivacy implements RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer {

	private boolean canComment;
	@SerializedName("canLookMeUp")
	private boolean canLookAuthorUp;

	public PostPrivacy() {}

	public PostPrivacy(boolean canComment, boolean canLookAuthorUp) {
		this.canComment = canComment;
		this.canLookAuthorUp = canLookAuthorUp;
	}

	//region == Realm model methods ==

	@Override
	public void reset() {}

	@Override
	public Object read(Realm realm) {
		return read(realm, PostPrivacy.class);
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

	public boolean canComment() {
		return canComment;
	}
	public void setCanComment(boolean canComment) {
		this.canComment = canComment;
	}

	public boolean canLookAuthorUp() {
		return canLookAuthorUp;
	}
	public void setCanLookAuthorUp(boolean canLookAuthorUp) {
		this.canLookAuthorUp = canLookAuthorUp;
	}

	//endregion

}
