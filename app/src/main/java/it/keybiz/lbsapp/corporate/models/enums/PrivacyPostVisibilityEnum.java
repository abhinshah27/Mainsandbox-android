/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.R;

/**
 * @author mbaldrighi on 3/25/2018.
 */
public enum PrivacyPostVisibilityEnum implements Serializable {

	INNER_CIRCLE(0),
	PUBLIC(1),
	ONLY_ME(2),
	ONLY_SELECTED_CIRCLES(3),
	ONLY_SELECTED_USERS(4);

	private int value;

	PrivacyPostVisibilityEnum(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static PrivacyPostVisibilityEnum toEnum(int value) {
		if (value < 0)
			return null;

        PrivacyPostVisibilityEnum[] statuses = PrivacyPostVisibilityEnum.values();
		for (PrivacyPostVisibilityEnum status : statuses)
			if (status.getValue() == value)
				return status;
		return null;
	}

	public static int convertEnumToSelectionIndex(PrivacyPostVisibilityEnum value) {
		if (value != null) {
			switch (value) {
				case PUBLIC:
					return 0;
				case INNER_CIRCLE:
					return 1;
				case ONLY_SELECTED_CIRCLES:
					return 2;
				case ONLY_ME:
					return 3;
			}
		}

		return -1;
	}

	public static int convertSelectionIndexToEnumValue(int value) {
		if (value >= 0) {
			switch (value) {
				case 0:
					return PUBLIC.getValue();
				case 1:
					return INNER_CIRCLE.getValue();
				case 2:
					return ONLY_SELECTED_CIRCLES.getValue();
				case 3:
					return ONLY_ME.getValue();
			}
		}

		return -1;
	}

	/**
	 * Gets the resources, drawable and string, with which the enum value needs to be translated in the UI.
	 * @param value the current {@link PrivacyPostVisibilityEnum} value.
	 * @param timeline the boolean value indicating if the translation is needed in the timeline feed.
	 *                 If so the drawable must be the white version, otherwise the black version.
	 * @return an int[] containing at the position 0 the drawable res, and the position 1 the string res.
	 */
	public static int[] getResources(PrivacyPostVisibilityEnum value, boolean timeline) {
		if (value != null) {
			@DrawableRes int drawResId;
			@StringRes int stringResId;
			
			switch (value) {
				case INNER_CIRCLE:
					drawResId = timeline ? R.drawable.layer_tl_feed_active : R.drawable.ic_timeline_inactive_black;
					stringResId = R.string.label_privacy_ic;
					break;
				case PUBLIC:
					drawResId = timeline ? R.drawable.layer_privacy_public : R.drawable.ic_privacy_public_black;
					stringResId = R.string.label_privacy_public;
					break;

				case ONLY_ME:
					drawResId = timeline ? R.drawable.layer_privacy_only_me : R.drawable.ic_onlyme_post_black;
					stringResId = R.string.label_privacy_only_me;
					break;

				case ONLY_SELECTED_CIRCLES:
				case ONLY_SELECTED_USERS:
					drawResId = timeline ? R.drawable.layer_privacy_sel_u_c : R.drawable.ic_selected_people_circles_black;
					stringResId = (value == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES) ?
							R.string.label_privacy_sel_circles_short : R.string.label_privacy_sel_users_short;
					break;
					
				default:
					drawResId = timeline ? R.drawable.layer_tl_feed_active : R.drawable.ic_timeline_inactive_black;
					stringResId = R.string.label_privacy_ic;
			}
			
			return new int[] { drawResId, stringResId };
		}
		
		return null;
	}
}
