/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import org.json.JSONObject;

import io.realm.RealmModel;
import it.keybiz.lbsapp.corporate.base.HLBaseModel;

/**
 * Class conceived to be just another filter on the abstract methods of {@link HLBaseModel} to override.
 *
 * @author mbaldrighi on 10/7/2017.
 */
public class HLModel extends HLBaseModel {

	@Override
	public void reset() {}

	@Override
	public Object read() {
		return null;
	}

	@Override
	public void write() {}

	@Override
	public void write(Object object) {}

	@Override
	public void write(JSONObject json) {}

	@Override
	public void write(RealmModel model) {}

	@Override
	public void update() {}

	@Override
	public void update(Object object) {}

	@Override
	public void update(JSONObject json) {}

	@Override
	public HLModel updateWithReturn() {
		return null;
	}

	@Override
	public HLModel updateWithReturn(Object object) {
		update(object);
		return this;
	}

	@Override
	public HLModel updateWithReturn(JSONObject json) {
		return null;
	}
}