/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.base;

import org.json.JSONObject;

import io.realm.RealmModel;
import it.keybiz.lbsapp.corporate.models.HLModel;

/**
 * Abstract class providing basic quasi-CRUD methods.
 *
 * @author mbaldrighi on 10/7/2017.
 */
abstract public class HLBaseModel {

	/**
	 * Generic RESET method.
	 */
	public abstract void reset();

	/**
	 * Generic READ method.
	 * @return An {@link Object}.
	 */
	public abstract Object read();

	/**
	 * Generic WRITE method.
	 */
	public abstract void write();

	/**
	 * Generic WRITE method given the {@link Object} to be written.
	 * @param object the {@link Object} to be written.
	 */
	public abstract void write(Object object);

	/**
	 * Generic UPDATE operation from given {@link org.json.JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 */
	public abstract void write(JSONObject json);

	/**
	 * WRITE operation concerning {@link io.realm.Realm} given the {@link RealmModel} to be written.
	 * @param model the {@link RealmModel} to be written.
	 */
	public abstract void write(RealmModel model);

	/**
	 * Generic UPDATE operation.
	 */
	public abstract void update();

	/**
	 * Generic UPDATE operation from given {@link Object}.
	 * @param object the {@link Object} source of the update.
	 */
	public abstract void update(Object object);

	/**
	 * Generic UPDATE operation from given {@link org.json.JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 */
	public abstract void update(JSONObject json);

	/**
	 * Generic UPDATE operation.
	 * @return The same object updated.
	 */
	public abstract HLModel updateWithReturn();

	/**
	 * Generic UPDATE operation from given {@link Object}.
	 * @param object the {@link Object} source of the update.
	 * @return The same object updated.
	 */
	public abstract HLModel updateWithReturn(Object object);

	/**
	 * Generic UPDATE operation from given {@link org.json.JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 * @return The same object updated.
	 */
	public abstract HLModel updateWithReturn(JSONObject json);

}
