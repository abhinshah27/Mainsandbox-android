/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author mbaldrighi on 10/6/2017.
 */
public class SharedPrefsUtils {

	/**
	 * The TEMPORARY SharedPreferences file.<p>
	 * Logout procedure deletes it.
	 */
	public static final String HL_PREFS_FILE_TMP = "hl_prefs_tmp";

	/**
	 * The STABLE SharedPreferences file.<p>
	 * Logout procedure DOESN'T delete it, only uninstallation DOES.
	 */
	public static final String HL_PREFS_FILE = "hl_prefs";

	public static final String PROPERTY_USER_TOKEN_ID = "token_user_id";
	public static final String PROPERTY_PERSISTED_POSTS = "persisted_posts";
	public static final String PROPERTY_FCM_TOKEN = "fcm_token";
	public static final String PROPERTY_GUIDE_SEEN = "guide_seen";

	public static final String PROPERTY_STATUS_FALLBACK = "status_fallback";
	public static final String PROPERTY_INTERESTED_IN_FALLBACK = "interested_in_fallback";

	public static final String PROPERTY_LAST_POST_SEEN = "last_post_seen";

	public static final String PROPERTY_TWILIO_SDK_TOKEN = "twilio_sdk_token";

	public static final String PROPERTY_LOCATION_BACKGROUND_GRANTED = "location_background_granted";


	/**
	 * Stores a generic String into the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 * @param property the property key.
	 * @param value the string to be written.
	 * @param commit whether the client wants the transaction to be performed sync or async.
	 */
	public static void storeStringProperty(Context context, String property, String value, boolean commit) {
		if (Utils.isContextValid(context)) {
			SharedPreferences sPrefs = context.getSharedPreferences(HL_PREFS_FILE_TMP, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sPrefs.edit();

			editor.putString(property, value);
			if (commit) editor.commit();
			else editor.apply();
		}
	}

	/**
	 * Stores a generic String into the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 * @param property the property key.
	 * @param value the string to be written.
	 */
	public static void storeStringProperty(Context context, String property, String value) {
		storeStringProperty(context, property, value, false);
	}

	/**
	 * Stores a generic Boolean into the provided SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 * @param fileName the SharedPreferences file name.
	 * @param property the property key.
	 * @param value the boolean to be written.
	 */
	public static void storeBooleanProperty(Context context, String fileName, String property, boolean value) {
		if (Utils.isContextValid(context)) {
			SharedPreferences sPrefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sPrefs.edit();

			editor.putBoolean(property, value);
			editor.apply();
		}
	}

	/**
	 * Stores a generic Boolean into the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 * @param property the property key.
	 * @param value the boolean to be written.
	 */
	public static void storeBooleanProperty(Context context, String property, boolean value) {
		storeBooleanProperty(context, HL_PREFS_FILE_TMP, property, value);
	}

	/**
	 * Retrieves a generic String from the TEMPORARY SharedPreferences file.
	 * Default value == "" (empty String).
	 * @param context the Application's/Activity's context.
	 * @param property the property key.
	 */
	public static String getStringProperty(Context context, String property) {
		if (Utils.isContextValid(context)) {
			SharedPreferences sPrefs = context.getSharedPreferences(HL_PREFS_FILE_TMP, Context.MODE_PRIVATE);
			return sPrefs.getString(property, "");
		}

		return "";
	}

	/**
	 * Retrieves a generic String from the provided SharedPreferences file.
	 * Default value == "" (empty String).
	 * @param context the Application's/Activity's context.
	 * @param fileName the SharedPreferences file name.
	 * @param property the property key.
	 */
	public static boolean getBooleanProperty(Context context, String fileName, String property) {
		if (Utils.isContextValid(context)) {
			SharedPreferences sPrefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
			return sPrefs.getBoolean(property, false);
		}

		return false;
	}

	/**
	 * Retrieves a generic String from the TEMPORARY SharedPreferences file.
	 * Default value == "" (empty String).
	 * @param context the Application's/Activity's context.
	 * @param property the property key.
	 */
	public static boolean getBooleanProperty(Context context, String property) {
		return getBooleanProperty(context, HL_PREFS_FILE_TMP, property);
	}


	/**
	 * Stores the {@link SharedPrefsUtils#PROPERTY_USER_TOKEN_ID} into the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 * @param token the user token to be written.
	 */
	public static void storeUserTokenId(Context context, String token) {
		storeStringProperty(context, PROPERTY_USER_TOKEN_ID, token);
	}

	/**
	 * Retrieves the {@link SharedPrefsUtils#PROPERTY_USER_TOKEN_ID} from the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 */
	public static String retrieveUserTokenId(Context context) {
		return getStringProperty(context, PROPERTY_USER_TOKEN_ID);
	}


	/**
	 * Stores the {@link SharedPrefsUtils#PROPERTY_FCM_TOKEN} into the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 * @param token the user token to be written.
	 */
	public static void storeFCMTokenId(Context context, String token) {
		storeStringProperty(context, PROPERTY_FCM_TOKEN, token);
	}

	/**
	 * Retrieves the {@link SharedPrefsUtils#PROPERTY_FCM_TOKEN} from the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 */
	public static String retrieveFCMTokenId(Context context) {
		return getStringProperty(context, PROPERTY_FCM_TOKEN);
	}

	/**
	 * Checks if the SharedPreferences have a valid {@link SharedPrefsUtils#PROPERTY_USER_TOKEN_ID}.
	 * @param context the Application's/Activity's context.
	 * @return True if a valid token is present, false otherwise.
	 */
	public static boolean isSessionValid(Context context) {
		return Utils.isStringValid(retrieveUserTokenId(context));
	}

	public static void setGuideSeen(Context context, boolean guideSeen) {
		storeBooleanProperty(context, HL_PREFS_FILE, PROPERTY_GUIDE_SEEN, guideSeen);
	}

	public static boolean isFirstAccess(Context context) {
		return !getBooleanProperty(context, HL_PREFS_FILE, PROPERTY_GUIDE_SEEN);
	}

	public static void storeLastPostSeen(Context context, String value) {
		storeStringProperty(context, SharedPrefsUtils.PROPERTY_LAST_POST_SEEN, value);
	}

	public static String getLastPostSeen(Context context) {
		return getStringProperty(context, SharedPrefsUtils.PROPERTY_LAST_POST_SEEN);
	}


	// TODO: 11/10/2018    RECONSIDER IF IN THE FUTURE A SharedPreferences MANAGEMENT IS REQUIRED
	/**
	 * Stores the {@link SharedPrefsUtils#PROPERTY_TWILIO_SDK_TOKEN} into the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 * @param token the user token to be written.
	 */
	public static void storeCallsTokenId(Context context, String token) {
		storeStringProperty(context, PROPERTY_TWILIO_SDK_TOKEN, token);
	}

	/**
	 * Retrieves the {@link SharedPrefsUtils#PROPERTY_TWILIO_SDK_TOKEN} from the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 */
	public static String retrieveCallsTokenId(Context context) {
		return getStringProperty(context, PROPERTY_TWILIO_SDK_TOKEN);
	}


	/**
	 * Stores the {@link SharedPrefsUtils#PROPERTY_LOCATION_BACKGROUND_GRANTED} into the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 * @param asked whether the permission has been asked or not.
	 */
	public static void setLocationBackgroundGranted(Context context, boolean asked) {
		storeBooleanProperty(context, PROPERTY_LOCATION_BACKGROUND_GRANTED, asked);
	}

	/**
	 * Retrieves the {@link SharedPrefsUtils#PROPERTY_LOCATION_BACKGROUND_GRANTED} from the TEMPORARY SharedPreferences file.
	 * @param context the Application's/Activity's context.
	 */
	public static boolean hasLocationBackgroundBeenGranted(Context context) {
		return getBooleanProperty(context, PROPERTY_LOCATION_BACKGROUND_GRANTED);
	}


	/**
	 * Clears ONLY the TEMPORARY preferences file.
	 * @param context the Application's/Activity's context.
	 */
	public static void clearPreferences(Context context) {
		if (Utils.isContextValid(context)) {
			SharedPreferences sPrefs = context.getSharedPreferences(HL_PREFS_FILE_TMP, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sPrefs.edit();

			editor.clear();
			editor.apply();
		}
	}

}
