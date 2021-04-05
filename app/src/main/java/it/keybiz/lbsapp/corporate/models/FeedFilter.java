/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.Context;

import it.keybiz.lbsapp.corporate.models.enums.FeedFilterTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 6/25/2018.
 */
public class FeedFilter {

	private final FeedFilterTypeEnum type;
	private final String name;
	private final String nameForServer;
	private final String nameToDisplay;
	private boolean selected;


	public FeedFilter(String name, String nameToDisplay) {
		this.type = FeedFilterTypeEnum.CIRCLE;
		this.name = name;
		this.nameForServer = FeedFilterTypeEnum.getCallValue(name);
		this.nameToDisplay = nameToDisplay;
	}

	public FeedFilter(Context context, FeedFilterTypeEnum type) {
		this.type = type;
		this.name = FeedFilterTypeEnum.getValue(context, type);
		this.nameForServer = FeedFilterTypeEnum.getCallValue(type);
		this.nameToDisplay = FeedFilterTypeEnum.getValue(context, type);
	}

	public boolean isFamilyCircle() {
		return Utils.isStringValid(name) && name.equals(Constants.CIRCLE_FAMILY_NAME);
	}

	public boolean isInnerCircle() {
		return Utils.isStringValid(name) && name.equals(Constants.INNER_CIRCLE_NAME);
	}

	public boolean isAll() {
		return type == FeedFilterTypeEnum.ALL || type == FeedFilterTypeEnum.ALL_INT;
	}

	public boolean isCircle() {
		return type == FeedFilterTypeEnum.CIRCLE;
	}


	//region == Getters and setters ==

	public FeedFilterTypeEnum getType() {
		return type;
	}

	public String getNameForServer() {
		return nameForServer;
	}

	public String getNameToDisplay() {
		return nameToDisplay;
	}

	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	//endregion

}
