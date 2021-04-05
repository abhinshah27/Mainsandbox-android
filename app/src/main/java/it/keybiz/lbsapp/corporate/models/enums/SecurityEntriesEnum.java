/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

/**
 * @author mbaldrighi on 3/27/2018.
 */
public enum SecurityEntriesEnum {

	LEGACY_CONTACT_TRIGGER(0),
	DELETE_ACCOUNT(1);

	private int value;

	SecurityEntriesEnum(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static SecurityEntriesEnum toEnum(int value) {
		if (value < 0)
			return null;

        SecurityEntriesEnum[] statuses = SecurityEntriesEnum.values();
		for (SecurityEntriesEnum status : statuses)
			if (status.getValue() == value)
				return status;
		return null;
	}

}
