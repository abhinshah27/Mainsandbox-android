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
public enum ActionTypeEnum implements Serializable {

	ALLOW("allow"),
	DENY("deny"),

	ON("on"),
	OFF("off"),

	YES("yes"),
	NO("no");

	private String value;

	ActionTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static ActionTypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return null;

        ActionTypeEnum[] statuses = ActionTypeEnum.values();
		for (ActionTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return null;
	}

	public static boolean convertEnumToBoolean(ActionTypeEnum type) {
		switch (type) {
			case ALLOW:
			case ON:
			case YES:
				return true;

			case DENY:
			case OFF:
			case NO:
				return false;

			default:
				return false;
		}
	}

	public static ActionTypeEnum convertBooleanToEnum(boolean bool, ActionTypeEnum positiveRef) {
		switch (positiveRef) {
			case ALLOW:
				return bool ? ALLOW : DENY;
			case ON:
				return bool ? ON : OFF;
			case YES:
				return bool ? YES : NO;
		}

		return null;
	}

}
