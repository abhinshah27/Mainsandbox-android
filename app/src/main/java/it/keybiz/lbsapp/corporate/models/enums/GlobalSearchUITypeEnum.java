/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 4/10/2018.
 */
public enum GlobalSearchUITypeEnum implements Serializable {

	CARDS("cards"),
	SQUARED("squared");

	private String value;

	GlobalSearchUITypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static GlobalSearchUITypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return CARDS;

        GlobalSearchUITypeEnum[] statuses = GlobalSearchUITypeEnum.values();
		for (GlobalSearchUITypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return CARDS;
	}

}
