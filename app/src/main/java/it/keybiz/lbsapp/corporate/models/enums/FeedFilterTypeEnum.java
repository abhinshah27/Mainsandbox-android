/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import android.content.Context;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 6/25/2018.
 */
public enum FeedFilterTypeEnum implements Serializable {

	// USER
	ALL, CIRCLE, INTERESTS, NEWS,

	// INTEREST
	ALL_INT, MY_STORIES, NEWS_INT;


	public static String getValue(Context context, FeedFilterTypeEnum type) {
		switch (type) {
			case ALL:
			case ALL_INT:
				return context.getString(R.string.tl_filter_all);
			case INTERESTS:
				return context.getString(R.string.tl_filter_interests);
			case NEWS:
			case NEWS_INT:
				return context.getString(R.string.tl_filter_news);

			case MY_STORIES:
				return context.getString(R.string.tl_filter_mystories);

			default:
				return null;
		}
	}

	public static String getCallValue(FeedFilterTypeEnum type) {
		switch (type) {
			case ALL:
				return "any";
			case INTERESTS:
				return "interestFromInternal";
			case NEWS:
				return "newsposts";

			case ALL_INT:
				return "anyInterest";
			case MY_STORIES:
				return "mystoriesInterest";
			case NEWS_INT:
				return "newsInterest";
		}

		return null;
	}

	public static String getCallValue(String name) {
		if (Utils.isStringValid(name)) {
			return name;
		}

		return null;
	}

	public static FeedFilterTypeEnum toEnum(String name) {
		if (Utils.isStringValid(name)) {
			switch (name) {
				case "any":
					return ALL;
				case "newsposts":
					return NEWS;
				case "interestFromInternal":
					return INTERESTS;

				case "anyInterest":
					return ALL_INT;
				case "mystoriesInterest":
					return MY_STORIES;
				case "newsInterest":
					return NEWS_INT;
			}
		}

		return CIRCLE;
	}

}
