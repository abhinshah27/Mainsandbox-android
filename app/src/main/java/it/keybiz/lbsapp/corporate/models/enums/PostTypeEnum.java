/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;

/**
 * Represents the post media type to be inserted into the main View.
 * @author mbaldrighi on 9/27/2017.
 */
public enum PostTypeEnum implements Serializable {
	AUDIO("audio"),
	PHOTO("photo"),
	PHOTO_PROFILE("photoprofile"),
	PHOTO_WALL("photowall"),
	TEXT("text"),               // set as default value
	VIDEO("video"),
	WEB_LINK("webLink"),
	NEWS("news"),
	FOLLOW_INTEREST("followinterest");

	private String value;

	PostTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static PostTypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return TEXT;

        PostTypeEnum[] statuses = PostTypeEnum.values();
		for (PostTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return TEXT;
	}

	public HLMediaType toMediaTypeEnum() {
		if (!Utils.isStringValid(this.toString()))
			return null;

        PostTypeEnum[] statuses = PostTypeEnum.values();
		for (PostTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(this.toString()))
				return HLMediaType.valueOf(status.toString().toUpperCase());
		return null;
	}

}
