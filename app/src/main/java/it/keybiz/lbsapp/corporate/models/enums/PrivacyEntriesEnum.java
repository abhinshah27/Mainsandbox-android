/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import java.io.Serializable;

/**
 * @author mbaldrighi on 3/25/2018.
 */
public enum PrivacyEntriesEnum implements Serializable {

	POST_VISIBILITY(0),
	WHO_CAN_INCLUDE_ME(1),
	WHO_CAN_SEE_MY_IC(2),
	WHO_CAN_LOOK_ME_UP(3),
	WHO_CAN_COMMENT(4),
	REVIEW_TAGS(5),
	BLOCKED(6),
	WHO_CAN_CHAT(7),
	WHO_CAN_VIDEO(8),
	WHO_CAN_VOICE(9);

	private int value;

	PrivacyEntriesEnum(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static PrivacyEntriesEnum toEnum(int value) {
		if (value < 0)
			return null;

        PrivacyEntriesEnum[] statuses = PrivacyEntriesEnum.values();
		for (PrivacyEntriesEnum status : statuses)
			if (status.getValue() == value)
				return status;
		return null;
	}

}
