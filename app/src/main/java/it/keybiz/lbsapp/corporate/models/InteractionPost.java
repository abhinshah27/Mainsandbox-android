/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;


import android.content.res.Resources;

import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.Date;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.utilities.Constants;

/**
 * @author mbaldrighi on 9/25/2017.
 */
public class InteractionPost implements Serializable {

	/**
	 * {@link Enum} representing the different kinds of possible interactions with a post.
	 * <p>
	 * The {@link #PIN} constant is only used to recognize the kind of tap in order to handle the actions
	 * in the single types {@link RecyclerView.ViewHolder}s.
	 */
	public enum Type {
		HEARTS, COMMENT, SHARE, PIN
	}

	private enum TimeUnit {
		SECONDS, MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS
	}


	//region == Class custom methods ==

	public static String getTimeStamp(Resources res, Date creationDate) {
		if (creationDate != null) {
			long l = System.currentTimeMillis() - creationDate.getTime();

			TimeUnit unit = TimeUnit.YEARS;
			if (l < Constants.TIME_UNIT_MINUTE)
				unit = TimeUnit.SECONDS;
			else if (l < Constants.TIME_UNIT_HOUR)
				unit = TimeUnit.MINUTES;
			else if (l < Constants.TIME_UNIT_DAY)
				unit = TimeUnit.HOURS;
			else if (l < Constants.TIME_UNIT_WEEK)
				unit = TimeUnit.DAYS;
			else if (l < Constants.TIME_UNIT_MONTH)
				unit = TimeUnit.WEEKS;
			else if (l < Constants.TIME_UNIT_YEAR)
				unit = TimeUnit.MONTHS;

			switch (unit) {
				case SECONDS:
					return res.getString(R.string.now).toLowerCase();
				case MINUTES:
					return (int) (l / Constants.TIME_UNIT_MINUTE) + "m";
				case HOURS:
					return (int) (l / Constants.TIME_UNIT_HOUR) + "h";
				case DAYS:
					return (int) (l / Constants.TIME_UNIT_DAY) + "d";
				case WEEKS:
					return (int) (l / Constants.TIME_UNIT_WEEK) + "w";
				case MONTHS:
					return (int) (l / Constants.TIME_UNIT_MONTH) + "mo";
				case YEARS:
					return (int) (l / Constants.TIME_UNIT_YEAR) + "y";

			}
 		}

		return null;
	}

	//endregion

}
