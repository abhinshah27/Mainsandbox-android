/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import androidx.annotation.DrawableRes;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 12/5/2017.
 */
public enum NotificationTypeEnum {

	/* NOTIFICATIONS */
	POST("post"),
	HEART("heart"),
	SHARE("sharing"),
	COMMENT("comment"),
	ADD_TO_CIRCLE_AUTHORIZED("AddToCircleAuthorized"),
	DOC_URL("docUrl"),

	/* REQUESTS */
	TAG("HasTaggedYou"),
	ADD_TO_CIRCLE("addToCircle"),
	LEGACY_CONTACT("BeLegacyContact"),
	FAMILY_RELATIONSHIP("AddFamilyRelationship");

	private String value;

	NotificationTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static NotificationTypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return null;

		NotificationTypeEnum[] statuses = NotificationTypeEnum.values();
		for (NotificationTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return null;
	}

	@DrawableRes
	public static int getNotificationIcon(NotificationTypeEnum type) {
		if (type != null) {
			switch (type) {
				case POST:
					return R.drawable.ic_notification_post_black;
				case HEART:
					return R.drawable.ic_notification_hearts_black;
				case SHARE:
					return R.drawable.ic_notification_share_black;
				case TAG:
					return R.drawable.ic_notification_tag_black;
				case COMMENT:
					return R.drawable.ic_notification_comment_black;
				case ADD_TO_CIRCLE:
				case ADD_TO_CIRCLE_AUTHORIZED:
					return R.drawable.ic_timeline_inactive_black;
				case LEGACY_CONTACT:
				case DOC_URL:
					return R.drawable.ic_wishes_active;
				case FAMILY_RELATIONSHIP:
					return R.drawable.ic_family_toggle_notif;
			}
		}

		throw new IllegalArgumentException("Notification type cannot be null");
	}

}
