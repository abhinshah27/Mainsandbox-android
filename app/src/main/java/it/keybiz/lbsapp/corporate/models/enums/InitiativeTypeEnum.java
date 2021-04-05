/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 7/11/2018.
 */
public enum InitiativeTypeEnum implements Serializable {

	TRANSFER_HEARTS("TH"),
	COLLECT_HEARTS("CH"),
	GIVE_SUPPORT("GS");

	private String value;

	InitiativeTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static InitiativeTypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return null;

        InitiativeTypeEnum[] statuses = InitiativeTypeEnum.values();
		for (InitiativeTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return null;
	}

}
