/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 3/26/2018.
 */
public enum UnBlockUserEnum {

	BLOCK("b"),
	UNBLOCK("u");

	private String value;

	UnBlockUserEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static UnBlockUserEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return null;

        UnBlockUserEnum[] statuses = UnBlockUserEnum.values();
		for (UnBlockUserEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return null;
	}

}
