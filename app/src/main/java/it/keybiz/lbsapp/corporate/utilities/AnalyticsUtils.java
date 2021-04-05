/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities;

import android.app.Activity;
import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Basic class dedicated to the action of sending analytics to Google through Firebase Analytics SDK.
 *
 * @author mbaldrighi on 5/15/2018.
 */
public class AnalyticsUtils {

	/* LOGIN */
	public static final String LOGIN_SIGNUP = "LoginSignup";
	public static final String LOGIN_SIGNUP_2 = "SignupLastStep";
	public static final String LOGIN_RESET_PWD = "ResetPassword";

	/* HOME */
	public static final String HOME_WEB_VIEW = "HomeWebView";
	public static final String HOME_TIMELINE_FEED = "TimelineFeed";
	public static final String HOME_ME_PAGE = "MePage";
	public static final String HOME_CHAT_ROOMS = "ChatRoomsView";
	public static final String HOME_MEMORY_INTERACTIONS_ADD = "AddMemoryInteractions";
	public static final String HOME_MEMORY_INTERACTIONS_VIEW = "ViewMemoryInteractions";

	/* GLOBAL SEARCH */
    public static final String GLOBAL_SEARCH = "GlobalSearch";
    public static final String GLOB_VIEW_MORE = "ViewMoreDetail";

	/* TIMELINE */
	public static final String FEED_VIDEO_VIEWER = "PlayerVideo";
	public static final String FEED_IMAGE_VIEWER = "ImageViewer";
	public static final String FEED_WEBLINK = "LinkBrowser";
	public static final String FEED_VIEW_TAGS = "ViewAllTags";
	public static final String FEED_TEXT_VIEWER = "TextViewer";

	/* ME */
	public static final String ME_DETAIL = "MeDetail";
	public static final String ME_LANGUAGES = "LanguagesSelection";
	public static final String ME_FAMILY_USER = "FamilyRelationshipUserSelector";
	public static final String ME_FAMILY_RELATION = "FamilyRelationshipSelector";
	public static final String ME_DIARY = "Diary";
	public static final String ME_INNER_CIRCLE = "InnerCircle";
	public static final String ME_INTERESTS = "MyInterests";
	public static final String ME_FOLLOW_NEW = "FollowNewInterest";
	public static final String ME_CATEGORY_BROWSER = "InterestCategoryBrowser";
	public static final String ME_INTEREST_FOLLOWERS = "InterestFollower";
	public static final String ME_INTEREST_SIMILAR = "InterestSimilar";
	public static final String ME_INTEREST_CLAIM = "ClaimInterest";
	public static final String ME_IDENTITIES_SELECTION = "IdentitiesSelection";
	public static final String ME_VIEW_MORE_CIRCLE = "ViewMoreCircle";
	public static final String ME_SEARCH_PROFILE = "SearchProfile";
	public static final String ME_NOTIFICATION = "Notification";
	public static final String ME_DOCS_LIST = "UserDocumentationList";
	public static final String ME_DOCS_VIEWER = "UserDocumentationViewer";


	/* CREATE POST */
	public static final String CREATE_POST = "CreatePost";
	public static final String CPOST_VISIBILITY = "SelectPostVisibility";
	public static final String CREATE_POST_PREVIEW = "CreatePostPreview";

	/* SETTINGS */
	public static final String SETTINGS_MAIN = "SettingsMain";
	public static final String SETTINGS_ACCOUNT = "AccountSettings";
	public static final String SETTINGS_INNER_CIRCLE = "InnerCircleSettings";
	public static final String SETTINGS_LEGACY_CONTACT = "LegacyContactSelect";
	public static final String SETTINGS_CIRCLES = "MyCirclesSettings";
	public static final String SETTINGS_FRIEND = "FriendsBrowser";
	public static final String SETTINGS_FEED = "TimelineFeedPreferences";
	public static final String SETTINGS_FOLDERS = "FolderSettings";
	public static final String SETTINGS_PRIVACY_BLOCKED = "BlockedUsers";
	public static final String SETTINGS_PRIVACY_LEGACY_TRIGGER = "LegacyContactTrigger";
	public static final String SETTINGS_PRIVACY_DELETE_ACCOUNT = "DeleteAccount";
	public static final String SETTINGS_2_STEP_CONTACTS_SELECTION = "TwoStepVerificationContactSelection";
	public static final String SETTINGS_2_STEP_CONTACTS_VIEW = "TwoStepVerificationContactView";
	public static final String SETTINGS_HELP_LIST = "SettingsHelpStandardList";
	public static final String SETTINGS_PRIVACY_FAQ_FINAL = "FAQFinalStep";
	public static final String SETTINGS_PRIVACY_SELECTION = "SettingsPrivacySelection";
	public static final String SETTINGS_PRIVACY_SELECTED_U_C = "SettingsPrivacySelectedUsersCircles";
	public static final String SETTINGS_PRIVACY_USER_GUIDE = "UserGuide";
	public static final String SETTINGS_PRIVACY_CONTACT_US = "ContactUsSetting";
	public static final String SETTINGS_PRIVACY_SECURITY_LIST = "SettingsPrivacySecurityList";
	public static final String SETTINGS_REDEEM_HEARTS_SELECTION = "SettingsRedeemHeartsSelection";
	public static final String SETTINGS_REDEEM_HEARTS_CONFIRMATION = "SettingsRedeemHeartsConfirmation";

	/* WISHES */
	public static final String WISHES_WELCOME = "WelcomeWish";
	public static final String WISHES_EDIT = "SavedWish";
	public static final String WISHES_NAME = "WishName";
	public static final String WISHES_LIST = "WishStandardList";
	public static final String WISHES_REPETITION = "WishRepetition";
	public static final String WISHES_PICKER = "WishPickerSelector";
	public static final String WISHES_FILTER = "WishFilter";
	public static final String WISHES_POST_SELECTION = "WishSelectPosts";
	public static final String WISHES_POST_SELECTION_FOLDERS = "WishMyDiary";
	public static final String WISHES_SEND_MAIL = "WishSendMail";
	public static final String WISHES_VERIFY_PHONE = "WishVerifyPhonenumber";
	public static final String WISHES_AUDIO_REC = "WishAudioRec";
	public static final String WISHES_CREATE_POST = "WishesCreatePost";
	public static final String WISHES_RECIPIENT = "WishRecipientSelector";
	public static final String WISHES_PREVIEW = "WishPreview";
	public static final String WISHES_COVER_PHOTO = "WishCoverPhoto";
	public static final String WISHES_SEARCH_USERS_INTERESTS = "WishSearchUsersInterests";

	/* CHAT */
	public static final String CHAT_MESSAGES = "MessagesView";
	public static final String CHAT_CREATE_ROOM = "CreateRoomView";
	public static final String CHAT_VIEW_DOCUMENT = "DocumentViewer";

	/* CALLS */
	public static final String CALLS = "AudioVideoCallView";

	/**
	 * Send tracked screen name to FirebaseAnalytics
	 * @param context    the current {@link Context}.
	 * @param screenName the current screen tag that needs to be tracked.
	 */
	public static void trackScreen(Context context, String screenName) {
		if (Utils.isContextValid(context)) {
			FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
			if (context instanceof Activity)
				analytics.setCurrentScreen(((Activity) context), screenName, null);

		}
	}


	//region == UNUSED NAMES ==

	public static final String LOGIN_LOGIN = "Login";
	public static final String LOGIN_SIGNUP_1 = "SignupFirstStep";
	public static final String ME_CONTACTS = "ContactInvite";
	public static final String SETTINGS_PRIVACY_MAIN = "PrivacyMainSettings";
	public static final String SETTINGS_PRIVACY_POST_VISIB = "PostVisibility";
	public static final String SETTINGS_PRIVACY_INCLUDE_IC = "WhoCanIncludeInMyInnerCircle";
	public static final String SETTINGS_PRIVACY_SEE_IC = "WhoCanSeeMyInnerCircle";
	public static final String SETTINGS_PRIVACY_LOOK_UP = "WhoCanLookMeUp";
	public static final String SETTINGS_PRIVACY_COMMENT = "WhoCanCommentOnMyPosts";
	public static final String SETTINGS_PRIVACY_REVIEW_TAG = "TagReviewOption";
	public static final String SETTINGS_PRIVACY_FAQ = "FAQ";
	public static final String SETTINGS_HELP_MAIN = "HelpAndSupportMainSettings";
	public static final String SETTINGS_PRIVACY_SECURITY = "SecurityMainSettings";
	public static final String WISHES_NOT_AVAILABLE = "WishNotAvailable";

	//endregion


}