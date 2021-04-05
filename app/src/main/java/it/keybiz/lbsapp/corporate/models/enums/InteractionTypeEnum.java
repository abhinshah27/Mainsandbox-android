/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Represents the interaction type users can have with a post.
 * @author mbaldrighi on 10/19/2017.
 */
public enum InteractionTypeEnum implements Serializable {
	HEARTS("hearts"),
	COMMENT("comment"),
	SHARE("share");

	private String value;

	InteractionTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static InteractionTypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return HEARTS;

        InteractionTypeEnum[] statuses = InteractionTypeEnum.values();
		for (InteractionTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return HEARTS;
	}

}
