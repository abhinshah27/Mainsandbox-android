/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 3/14/2018.
 */
public enum LegacyContactStatusEnum implements Serializable {

	NONE("none"),
	PENDING("pending"),
	AUTHORIZED("authorized");

	private String value;

	LegacyContactStatusEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static LegacyContactStatusEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return NONE;

        LegacyContactStatusEnum[] statuses = LegacyContactStatusEnum.values();
		for (LegacyContactStatusEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return NONE;
	}

}
