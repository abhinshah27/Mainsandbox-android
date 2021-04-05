/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 12/13/2017.
 */
public enum SearchTypeEnum implements Serializable {

	INNER_CIRCLE("innercircle"),
	INTERESTS("interests"),

	REMOTE("remote"),
	LOCAL("local");

	private String value;

	SearchTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static SearchTypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return null;

        SearchTypeEnum[] statuses = SearchTypeEnum.values();
		for (SearchTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return null;
	}

}
