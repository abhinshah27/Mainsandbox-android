/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmModel;

/**
 * Implements the same abstract methods of {@link it.keybiz.lbsapp.corporate.base.HLBaseModel},
 * since Realm does not allow its classes to extend {@link HLModel}.
 *
 * @author mbaldrighi on 10/7/2017.
 */
public interface RealmModelListener {

	/**
	 * Generic RESET method.
	 */
	void reset();

	/**
	 * Generic READ method.
	 * @param realm the {@link Realm} instance used for the transaction.
	 * @return An {@link Object}.
	 */
	Object read(@Nullable Realm realm);

	/**
	 * READ operation concerning {@link io.realm.Realm} and made on a given {@link RealmModel} class.
	 * @param model the given {@link RealmModel} class.
	 * @return A {@link RealmModel}.
	 */
	RealmModel read(Realm realm, Class<? extends RealmModel> model);

	/**
	 * Since {@link io.realm.Realm} still does not support lists of primitive, nor nested classes
	 */
	void deserializeStringListFromRealm() throws JSONException;

	/**
	 * Since {@link io.realm.Realm} still does not support lists of primitive, nor nested classes
	 */
	void serializeStringListForRealm();
	
	/**
	 * Generic WRITE method.
	 * @param realm the {@link Realm} instance used for the transaction.
	 */
	void write(@Nullable Realm realm);

	/**
	 * Generic WRITE method given the {@link Object} to be written.
	 * @param object the {@link Object} to be written.
	 */
	void write(Object object);

	/**
	 * Generic UPDATE operation from given {@link org.json.JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 */
	void write(JSONObject json);

	/**
	 * WRITE operation concerning {@link io.realm.Realm} given the {@link RealmModel} to be written.
	 * @param realm the {@link Realm} instance used for the transaction.
	 * @param model the {@link RealmModel} to be written.
	 */
	void write(Realm realm, RealmModel model);

	/**
	 * Generic UPDATE operation.
	 */
	void update();

	/**
	 * Generic UPDATE operation from given {@link Object}.
	 * @param object the {@link Object} source of the update.
	 */
	void update(Object object);

	/**
	 * Generic UPDATE operation from given {@link org.json.JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 */
	void update(JSONObject json);

	/**
	 * Generic UPDATE operation.
	 * @return The same object updated.
	 */
	RealmModelListener updateWithReturn();

	/**
	 * Generic UPDATE operation from given {@link Object}.
	 * @param object the {@link Object} source of the update.
	 * @return The same object updated.
	 */
	RealmModelListener updateWithReturn(Object object);

	/**
	 * Generic UPDATE operation from given {@link org.json.JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 * @return The same object updated.
	 */
	RealmModelListener updateWithReturn(JSONObject json);
}
