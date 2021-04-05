/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.connection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import it.keybiz.lbsapp.corporate.BuildConfig;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.features.login.LoginActivity;
import it.keybiz.lbsapp.corporate.features.profile.UserDetailFragment;
import it.keybiz.lbsapp.corporate.features.settings.SettingsAccountFragment;
import it.keybiz.lbsapp.corporate.features.settings.SettingsICSingleCircleFragment;
import it.keybiz.lbsapp.corporate.features.settings.SettingsICTimelineFeedFragment;
import it.keybiz.lbsapp.corporate.features.settings.SettingsPrivacySelectionFragment;
import it.keybiz.lbsapp.corporate.features.settings.SettingsSecurityLegacy2StepFragment;
import it.keybiz.lbsapp.corporate.features.settings.SettingsSecurityLegacyContactTriggerFragment;
import it.keybiz.lbsapp.corporate.models.FamilyRelationship;
import it.keybiz.lbsapp.corporate.models.GenericUserFamilyRels;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLInterestAbout;
import it.keybiz.lbsapp.corporate.models.HLNotifications;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserAboutMe;
import it.keybiz.lbsapp.corporate.models.HLUserAboutMeMore;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.InteractionComment;
import it.keybiz.lbsapp.corporate.models.InteractionHeart;
import it.keybiz.lbsapp.corporate.models.InteractionPost;
import it.keybiz.lbsapp.corporate.models.InteractionShare;
import it.keybiz.lbsapp.corporate.models.LifeEvent;
import it.keybiz.lbsapp.corporate.models.MarketPlace;
import it.keybiz.lbsapp.corporate.models.MemoryMediaObject;
import it.keybiz.lbsapp.corporate.models.MoreInfoObject;
import it.keybiz.lbsapp.corporate.models.Notification;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.ProfileContactToSend;
import it.keybiz.lbsapp.corporate.models.SettingsHelpElement;
import it.keybiz.lbsapp.corporate.models.WishEmail;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.models.enums.ActionTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.NotificationTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.models.enums.SearchTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.UnBlockUserEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * Class containing all the basic and specific operations needed for the client-server communication.
 *
 * @author mbaldrighi on 10/1/2017.
 */
public class HLServerCalls {


	//region == BASE METHODS ==

	/**
	 * Builds the default bridge communication structure between server and client.
	 *
	 * @param name the server name.
	 * @param opCode the code of the action performed.
	 * @param dbName the database name.
	 * @param collection the reference to the HL database collection.
	 * @param pairs the {@link List} of objects to be sent to the server. These objects are again lists
	 *              of {@link Pair} accepting a String as key and Object as value.
	 *
	 * @return A String array containing the actual JSON message and the unique identifier for the call.
	 *
	 * @throws JSONException if something goes wrong during the operation.
	 */
	protected static String[] buildMessageJson(@NonNull String name, int opCode, @NonNull String dbName,
	                                         @NonNull String collection, List<List<Pair<String, Object>>> pairs,
	                                         String logTag)
			throws JSONException {

		return buildMessageJson(name, opCode, dbName, collection, pairs, logTag, false);
	}

	/**
	 * Builds the default bridge communication structure between server and client.
	 *
	 * @param name the server name.
	 * @param opCode the code of the action performed.
	 * @param dbName the database name.
	 * @param collection the reference to the HL database collection.
	 * @param pairs the {@link List} of objects to be sent to the server. These objects are again lists
	 *              of {@link Pair} accepting a String as key and Object as value.
	 * @param saveUUIDForNotification true if UUID is to be saved for notification record.
	 *
	 * @return A String array containing the actual JSON message and the unique identifier for the call.
	 *
	 * @throws JSONException if something goes wrong during the operation.
	 */
	protected static String[] buildMessageJson(@NonNull String name, int opCode, @NonNull String dbName,
	                                         @NonNull String collection, List<List<Pair<String, Object>>> pairs,
	                                         String logTag, boolean saveUUIDForNotification)
			throws JSONException {

		JSONObject json = new JSONObject();
		JSONObject event = new JSONObject();
		JSONArray objects = new JSONArray();

		String idOp;
		event.put("idOperation", idOp = getNewIdOperation())
				.put("action", opCode);
		if (Utils.isStringValid(name))
			event.put("name", name);
		if (Utils.isStringValid(dbName))
			event.put("dbName", dbName);
		if (Utils.isStringValid(collection))
			event.put("collection", collection);

		event.put("v", BuildConfig.BASE_VERSION_NAME);

		json.put("event", event);

		JSONObject obj;
		if (pairs != null && !pairs.isEmpty()) {
			for (List<Pair<String, Object>> lp : pairs) {
				if (lp != null && !lp.isEmpty()) {
					obj = new JSONObject();
					for (Pair<String, Object> p : lp){
						obj.put(p.first, p.second);
					}
					objects.put(obj);
				}
			}
		}
		json.put("objects", objects);

		LogUtils.d(logTag, "with body: " + json.toString());

		if (saveUUIDForNotification)
			HLNotifications.getInstance().addRequestUUID(idOp);

		return new String[] { json.toString(), idOp};
	}


	/**
	 * Builds the default bridge communication structure between server and client.
	 *
	 * @param name the server name.
	 * @param opCode the code of the action performed.
	 * @param dbName the database name.
	 * @param collection the reference to the HL database collection.
	 * @param jsonArray the {@link JSONArray} to be sent to the server.
	 *
	 * @return A String array containing the actual JSON message and the unique identifier for the call.
	 *
	 * @throws JSONException if something goes wrong during the operation.
	 */
	protected static String[] buildMessageJson(@NonNull String name, int opCode, @NonNull String dbName,
	                                         @NonNull String collection, @NonNull JSONArray jsonArray,
	                                         String logTag)
			throws JSONException {

		JSONObject json = new JSONObject();
		JSONObject event = new JSONObject();

		String idOp;
		event.put("idOperation", idOp = getNewIdOperation())
				.put("action", opCode);
		if (Utils.isStringValid(name))
			event.put("name", name);
		if (Utils.isStringValid(dbName))
			event.put("dbName", dbName);
		if (Utils.isStringValid(collection))
			event.put("collection", collection);
		json.put("event", event);

		json.put("objects", jsonArray);

		LogUtils.d(logTag, "with body: " + json.toString());

		return new String[] { json.toString(), idOp};
	}

	/**
	 * Builds the default bridge communication structure between server and client.
	 *
	 * @param opCode the code of the action performed.
	 * @param collection the reference to the HL database collection.
	 * @param pairs the {@link List} of objects to be sent to the server. These objects are again lists
	 *              of {@link Pair} accepting a String as key and Object as value.
	 *
	 * @return A String array containing the actual JSON message and the unique identifier for the call.
	 *
	 * @throws JSONException if something goes wrong during the operation.
	 */
	protected static String[] buildMessageJson(int opCode, @NonNull String collection,
	                                         List<List<Pair<String, Object>>> pairs, String logTag)
			throws JSONException {

		return buildMessageJson(Constants.SERVER_CODE_NAME, opCode, Constants.SERVER_CODE_DB_NAME,
				collection, pairs, logTag);
	}

	/**
	 * Builds the default bridge communication structure between server and client.
	 *
	 * @param opCode the code of the action performed.
	 * @param collection the reference to the HL database collection.
	 * @param pairs the {@link List} of objects to be sent to the server. These objects are again lists
	 *              of {@link Pair} accepting a String as key and Object as value.
	 *
	 * @return A String array containing the actual JSON message and the unique identifier for the call.
	 *
	 * @throws JSONException if something goes wrong during the operation.
	 */
	protected static String[] buildMessageJson(@NonNull String name, int opCode,
	                                         @NonNull String collection,
	                                         List<List<Pair<String, Object>>> pairs, String logTag)
			throws JSONException {

		return buildMessageJson(name, opCode, Constants.SERVER_CODE_DB_NAME,
				collection, pairs, logTag);
	}

	/**
	 * Parses string result sent from server for client.
	 *
	 * @param message the {@link String} object received from server.
	 *
	 * @return A 4-element {@link Object}[] containing: [0] the unique operation id originally sent
	 * by client, [1] the original action code, [2] the actual {@link JSONObject} result, [3] the
	 * status code for the operation (0 for SUCCESS, i>0 for ERROR).

	 * @throws JSONException if something goes wrong during the operation.
	 */
	public static Object[] parseJsonResponse(String message) throws JSONException {
		if (Utils.isStringValid(message)) {
			JSONObject jsonMessage = new JSONObject(message);

			JSONObject event = jsonMessage.optJSONObject("event");
			JSONArray results = jsonMessage.optJSONArray("results");

			int action = event.optInt("action");
			String idOp = event.optString("idOperation");

			JSONObject object;
			int statusCode = 0;
			String description = "";
			if (results != null && results.length() > 0) {
				object = results.optJSONObject(0);
				statusCode = object.optInt("responseStatus");
				description = object.optString("description");
			}

			return new Object[] { idOp, action, results, statusCode, description };
		}

		return null;
	}

	protected static Object[] resultsNotConnected() {
		return new Object[] { false, null, null };
	}

	@SuppressLint("HardwareIds")
	protected static String getSecureID(Context context) {
		return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

	protected static String getNewIdOperation() {
		return UUID.randomUUID().toString();
	}

	//endregion


	//region == API CALLS ==

	public static Object[] subscribeToSocket(final Context context, final HLUser user, boolean isChat) {
		return subscribeToSocket(context, user.getId(), isChat);
	}

	public static Object[] subscribeToSocket(final Context context, final String userId, boolean isChat) {
		if (isChat) {
			if (!LBSLinkApp.getSocketConnection().isConnectedChat())
				return resultsNotConnected();
		}
		else {
			if (!LBSLinkApp.getSocketConnection().isConnected())
				return resultsNotConnected();
		}

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;
		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("deviceID", getSecureID(context)));

			// adds static tag for temporary versioning
			pairs.add(new Pair<>("v", BuildConfig.BASE_VERSION_NAME));

			list.add(pairs);

			try {
				jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME_SUBSCR, Constants.SERVER_OP_SOCKET_SUBSCR, "",
						"", list, isChat ? "SOCKET SUBSCRIPTION CHAT call" : "SOCKET SUBSCRIPTION call");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (isChat) {
				if (!LBSLinkApp.subscribedToSocketChat) {
					LBSLinkApp.getSocketConnection().sendMessageChat(jsonResult[0]);
					LBSLinkApp.subscribedToSocketChat = true;
				}
			}
			else {
				if (!LBSLinkApp.subscribedToSocket) {
					LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
					LBSLinkApp.subscribedToSocket = true;
				}
			}
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] signUp(Context context, String fName, String sName, String email,
	                              String password, Date dob, LoginActivity.Gender gender,
	                              @Nullable String hCode) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;
		if (Utils.areStringsValid(email, password, fName, sName)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("isConfirmed", false));
			pairs.add(new Pair<>("firstName", fName));
			pairs.add(new Pair<>("lastName", sName));
			pairs.add(new Pair<>("email", email));
			pairs.add(new Pair<>("password", password));
			pairs.add(new Pair<>("birthDate", dob != null ? Utils.formatDateForDB(dob) : ""));
			pairs.add(new Pair<>("g", gender == LoginActivity.Gender.NONE ? "" : (gender == LoginActivity.Gender.MALE ? "Male" : "Female")));
			pairs.add(new Pair<>("hcode", Utils.isStringValid(hCode) ? hCode : ""));
			pairs.add(new Pair<>("version", 0));
			pairs.add(new Pair<>("deviceID", getSecureID(context)));
			String sLoc = Locale.getDefault().toString();
			if (sLoc.length() >= 5)
				pairs.add(new Pair<>("mylanguage", sLoc.substring(0, 5)));
			else
				pairs.add(new Pair<>("mylanguage", Locale.getDefault().getLanguage()));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_OP_SIGNUP,
					Constants.SERVER_CODE_COLL_USERS, list, "SIGN UP call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] HCodeVerification(String hCode) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;
		if (Utils.isStringValid(hCode)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("hcode", hCode));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_OP_HCODE_VERIFY,
					Constants.SERVER_CODE_COLL_USERS, list, "HCODE VERIFICATION call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}


	public static Object[] signIn(Context context, String email, String password) throws JSONException {
		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;
		if (Utils.areStringsValid(email, password)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("isConfirmed", false));
			pairs.add(new Pair<>("email", email));
			pairs.add(new Pair<>("password", password));
			pairs.add(new Pair<>("version", 0));
			pairs.add(new Pair<>("deviceID", getSecureID(context)));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_OP_SIGNIN_V2,
					Constants.SERVER_CODE_COLL_USERS, list, "SIGN IN V2 call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] sendNotificationToken(String userId, String token) throws JSONException {
		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;
		if (Utils.areStringsValid(userId, token)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", !userId.equals(Constants.GUEST_USER_ID) ? userId : ""));
			pairs.add(new Pair<>("token", token));
			pairs.add(new Pair<>("mod", BuildConfig.DEBUG ? "d" : "p"));
			pairs.add(new Pair<>("so", "Android"));
			list.add(pairs);

			if (!LBSLinkApp.fcmTokenSent) {
				jsonResult = buildMessageJson(Constants.SERVER_OP_FCM_TOKEN,
						Constants.SERVER_CODE_COLL_USERS, list, "SEND FCM TOKEN call");

				LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
				LBSLinkApp.fcmTokenSent = true;
			}
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] recoverPassword(String email) throws JSONException {
		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;
		if (Utils.isStringValid(email)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("email", email));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_OP_PWD_RECOVERY,
					Constants.SERVER_CODE_COLL_USERS, list, "RECOVER PASSWORD call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}


	//region == TIMELINE CALLS ==

	public static Object[] getTimeline(@NonNull String userId, @NonNull String name, @Nullable String avatar,
	                                   int orderType, int skip, @NonNull List<String> listIds, boolean isInterest)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("name", name));
			String avatarUrl = "";
			if (Utils.isStringValid(avatar))
				avatarUrl = avatar;
			pairs.add(new Pair<>("avatarURL", avatarUrl));
			if (orderType == -1)
				orderType = Constants.SERVER_SORT_TL_ENUM_RECENT;
			pairs.add(new Pair<>("ordertype", orderType));

			// new value
			pairs.add(new Pair<>("skip", skip));
			// maintained for compatibility
			pairs.add(new Pair<>("pageID", 1));

			JSONArray ids = new JSONArray(listIds);
			if (ids.length() == 0)
				ids.put(isInterest ? "anyInterest" : "any");
			pairs.add(new Pair<>("listID", ids));

			// adds static tag for temporary versioning
			pairs.add(new Pair<>("v", BuildConfig.BASE_VERSION_NAME));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_OP_GET_TIMELINE,
					Constants.SERVER_CODE_COLL_POSTS, list, "GET TIMELINE call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

//	public static Object[] getTimeline(@NonNull String userId, @NonNull String name, @Nullable String avatar,
//	                                   @Nullable String listName, int orderType, int pageId)
//			throws JSONException {
//
//		return getTimeline(userId, name, avatar, orderType, pageId, listName);
//	}
//
//	public static Object[] getTimeline(@NonNull HLUser user, int orderType, int pageId)
//			throws JSONException {
//
//		return getTimeline(user.getId(), user.getCompleteName(), user.getAvatarURL(), orderType, pageId, null);
//	}

	public static Object[] getInteractionsForPost(@NonNull HLUser user, @NonNull String postId,
	                                              @NonNull InteractionPost.Type type, int pageId)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		String uid = user.getId();
		if (Utils.areStringsValid(uid, postId)) {
			list = new ArrayList<>();

			int opCode; String collection;
			switch (type) {
				case COMMENT:
					opCode = Constants.SERVER_OP_GET_COMMENTS;
					collection = Constants.SERVER_CODE_COLL_COMMENTS;
					break;
				case HEARTS:
					opCode = Constants.SERVER_OP_GET_HEARTS;
					collection = Constants.SERVER_CODE_COLL_HEARTS;
					break;
				case SHARE:
					opCode = Constants.SERVER_OP_GET_SHARES;
					collection = Constants.SERVER_CODE_COLL_SHARES;
					break;
				default:
					opCode = 0;
					collection = "";
			}

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", uid));
			pairs.add(new Pair<>("postID", postId));
			if (pageId == -1)
				pageId = 1;
			pairs.add(new Pair<>("pageID", pageId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opCode, collection, list,
					"GET INTERACTIONS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	/**
	 * For the API call to UPLOAD a MEDIA file
	 * {@link MediaHelper.UploadService} class.
	 */

	public static Object[] createEditPost(@NonNull Post post)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		JSONArray jsonArray = new JSONArray();
		String uid = post.getAuthorId();
		if (Utils.isStringValid(uid)) {
			String obj;
			int opCode;
			if (Utils.isStringValid(post.getId())) {
				obj = post.serializeWithTags(false);
				opCode = Constants.SERVER_OP_EDIT_POST;
			}
			else {
				obj = post.serializeWithTags(true);
				opCode = Constants.SERVER_OP_CREATE_POST_V2;
			}

			jsonArray.put(new JSONObject(obj));

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opCode,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, jsonArray,
					"CREATE POST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] sendHeartsOrComments(@Nullable InteractionHeart interHeart,
	                                            @Nullable InteractionComment interComment,
	                                            @NonNull HLUser user, @NonNull Post post)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		String uid = user.getId();
		String pid = post.getId();
		if (Utils.areStringsValid(uid, pid)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("postID",pid));
			pairs.add(new Pair<>("userID", uid));
			pairs.add(new Pair<>("name", user.getCompleteName()));

			String avatarUrl = user.getAvatarURL();
			pairs.add(new Pair<>("avatarURL", Utils.isStringValid(avatarUrl) ? avatarUrl : ""));

			int opCode = 0;
			if (interHeart != null || interComment != null) {
				if (interHeart != null) {
					pairs.add(new Pair<>("date", Utils.formatDateForDB(interHeart.getCreationDate())));
					int originalHeart = (post.getHeartsLeft() != null) ? post.getHeartsLeft() : 0;
					pairs.add(new Pair<>("count", interHeart.getCount() - originalHeart));
					pairs.add(new Pair<>("heartID", Utils.isStringValid(post.getHeartsLeftID()) ?
							post.getHeartsLeftID() : ""));
					opCode = Constants.SERVER_OP_SEND_HEARTS;
				} else {
					pairs.add(new Pair<>("date", Utils.formatDateForDB(interComment.getCreationDate())));
					pairs.add(new Pair<>("message", interComment.getMessage()));
					pairs.add(new Pair<>("isVisible", interComment.isVisible()));
					pairs.add(new Pair<>("level", interComment.getLevel()));
					pairs.add(new Pair<>("parentCommentID", interComment.getParentCommentID()));
					pairs.add(new Pair<>("operation", "a"));
					opCode = Constants.SERVER_OP_SEND_COMMENT;
				}
			}

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opCode,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list,
					"SEND INTERACTION call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] sharePost(@NonNull InteractionShare interShare, Map<String, HLCircle> circles,
	                                 Map<String, HLUserGeneric> users, @NonNull String postId)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		String uid = interShare.getAuthorId();
		if (Utils.areStringsValid(uid, postId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("postID", postId));
			pairs.add(new Pair<>("date", Utils.formatDateForDB(interShare.getCreationDate())));
			pairs.add(new Pair<>("userID", uid));
			pairs.add(new Pair<>("name", interShare.getAuthor()));
			String avatarUrl = interShare.getAuthorUrl();
			pairs.add(new Pair<>("avatarURL", Utils.isStringValid(avatarUrl) ? avatarUrl : ""));

			JSONArray c = new JSONArray();
			if (circles != null && !circles.isEmpty()) {
				for (HLCircle cir : circles.values()) {
					if (cir != null) {
						String s = cir.serializeToStringWithExpose();
						c.put(new JSONObject(s));
					}
				}
			}
			JSONArray u = new JSONArray();
			if (users != null && !users.isEmpty()) {
				for (HLUserGeneric us : users.values()) {
					if (us != null) {
						String s = us.serializeToStringWithExpose();
						u.put(new JSONObject(s));
					}
				}
			}
			pairs.add(new Pair<>("circles", c));
			pairs.add(new Pair<>("users", u));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SHARE_POST,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SHARES, list,
					"SHARE POST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] likeComment(@NonNull InteractionComment interComment,
	                                   @NonNull HLUser user, @NonNull String postId, boolean delete)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		String cid = interComment.getId();
		String userId = user.getId();
		if (Utils.areStringsValid(userId, postId, cid)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("postID",postId));
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("name", user.getCompleteName()));
			pairs.add(new Pair<>("commentID", cid));
			pairs.add(new Pair<>("date", Utils.formatDateForDB(System.currentTimeMillis())));
			pairs.add(new Pair<>("like", !delete));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_LIKE_COMMENT,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list,
					"LIKE COMMENT call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] manageComment(@NonNull InteractionComment interComment,
	                                     @NonNull HLUser user, @NonNull String postId)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		String userId = user.getId();
		String cid = interComment.getId();
		if (Utils.areStringsValid(userId, postId, cid)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("postID",postId));
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("commentID", cid));
			pairs.add(new Pair<>("name", user.getCompleteName()));
			String avatar = user.getAvatarURL();
			pairs.add(new Pair<>("avatarURL", Utils.isStringValid(avatar) ? avatar : ""));
			pairs.add(new Pair<>("date", Utils.formatDateForDB(interComment.getCreationDate())));
			pairs.add(new Pair<>("message", interComment.getMessage()));
			pairs.add(new Pair<>("isVisible", interComment.isVisible()));
			pairs.add(new Pair<>("level", interComment.getLevel()));
			pairs.add(new Pair<>("parentCommentID", interComment.getParentCommentID()));
			String operation = interComment.isVisible() ? "m" : "d";
			pairs.add(new Pair<>("operation", operation));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_MANAGE_COMMENT,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list, "MANAGE COMMENT call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] deletePost(@NonNull String userId, @NonNull String postId)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, postId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("postID",postId));
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_DELETE_POST,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list, "DELETE POST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public enum ListsCallType { GET, CREATE, ADD_POST, REMOVE_POST, DELETE, RENAME }
	public static Object[] manageLists(@NonNull ListsCallType type, @NonNull String userId,
	                                   @Nullable Bundle bundle)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;


		if (Utils.isStringValid(userId)) {
			int opCode = -1;
			String tag = "";
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			if (bundle != null) {
				for (String key : bundle.keySet())
					pairs.add(new Pair<>(key, bundle.get(key)));
			}

			switch (type) {
				case GET:
					opCode = Constants.SERVER_OP_FOLDERS_GET;
					tag = "GET POST LISTS";
					break;
				case CREATE:
					opCode = Constants.SERVER_OP_FOLDERS_CREATE;
					tag = "CREATE POST LIST";
					break;
				case ADD_POST:
					opCode = Constants.SERVER_OP_FOLDERS_ADD_POST;
					tag = "ADD POST TO LIST";
					break;
				case REMOVE_POST:
					opCode = Constants.SERVER_OP_FOLDERS_REMOVE_POST;
					tag = "REMOVE POST FROM LIST";
					break;
				case DELETE:
					opCode = Constants.SERVER_OP_FOLDERS_DELETE;
					tag = "DELETE LIST";
					break;
				case RENAME:
					opCode = Constants.SERVER_OP_FOLDERS_RENAME;
					tag = "RENAME LIST";
					break;
			}

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opCode,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list, tag);
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getParsedWebLink(@NonNull String webLink, @Nullable String messageID)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(webLink)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("link", webLink));
			if (Utils.isStringValid(messageID))
				pairs.add(new Pair<>("messageID", messageID));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_PARSED_WEB_LINK,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"GET PARSED WEBLINK call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public enum CallType { POST, USER}
	public static Object[] report(CallType type, @NonNull String userId, @NonNull String objectId, @NonNull String message)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, objectId, message) && type != null) {
			list = new ArrayList<>();

			String objIdKey = "postID",
					userReportingIdKey = "userID",
					reportReasonKey = "message",
					tag = "REPORT POST";
			int idOp = Constants.SERVER_OP_MODERATION;
			if (type == CallType.USER) {
				objIdKey = "userIDReported";
				userReportingIdKey = "userIDReporting";
				reportReasonKey = "reportReason";
				tag = "REPORT USER";
				idOp = Constants.SERVER_OP_REPORT_USER;
			}

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>(userReportingIdKey, userId));
			pairs.add(new Pair<>(objIdKey, objectId));
			pairs.add(new Pair<>(reportReasonKey, message));
			pairs.add(new Pair<>("date", Utils.formatDateForDB(System.currentTimeMillis())));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, idOp,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] setRTDataRead(@Nullable String userId)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SET_RT_DATA_READ,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list,
					"SET RT DATA READ call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	//endregion

	//region == USER PROFILE ==

	public static Object[] getUserProfile(@NonNull String userId)
			throws JSONException {

		return getProfile(userId, null, ProfileTypeCall.USER);
	}

	public static Object[] getInterestProfile(@NonNull String userId, @NonNull String interestIdToFetch)
			throws JSONException {

		return getProfile(userId, interestIdToFetch, ProfileTypeCall.INTEREST);
	}

	public enum ProfileTypeCall { USER, INTEREST }
	/**
	 * Calls for the profile info related to the correct {@link ProfileTypeCall}.
	 *
	 * @param userId must always refer to the main identity ID, unless the call is intended for a
	 * {@link it.keybiz.lbsapp.corporate.features.profile.ProfileHelper.ProfileType#FRIEND}.
	 * @param interestIdToFetch must always refer to the {@link it.keybiz.lbsapp.corporate.models.Interest} to be fetched.
	 * @param type the type of the call
	 * @return the correct result whether the app has or hasn't data connection.
	 * @throws JSONException If something goes wrong while creating the JSON body to be sent to the server.
	 */
	private static Object[] getProfile(@NonNull String userId, @Nullable String interestIdToFetch, ProfileTypeCall type)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			String collection = Constants.SERVER_CODE_COLL_USERS;
			int operation = Constants.SERVER_OP_GET_USER_PROFILE;
			String tag = "GET USER PROFILE call";
			if (type == ProfileTypeCall.USER) {
				List<Pair<String, Object>> pairs = new ArrayList<>();
				pairs.add(new Pair<>("userID", userId));
				list.add(pairs);
			}
			else if (type == ProfileTypeCall.INTEREST) {
				List<Pair<String, Object>> pairs = new ArrayList<>();
				pairs.add(new Pair<>("userID", userId));
				pairs.add(new Pair<>("interestID", interestIdToFetch));
				list.add(pairs);

				collection = Constants.SERVER_CODE_COLL_INTERESTS;
				operation = Constants.SERVER_OP_GET_INTEREST_PROFILE;
				tag = "GET INTEREST PROFILE call";
			}

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, operation,
					Constants.SERVER_CODE_DB_NAME, collection, list, tag);
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getPerson(@NonNull String userId, @NonNull String personId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, personId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("personID", personId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_PERSON_V2,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"GET PERSON call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getMyDiary(@NonNull String userId, @Nullable String personId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("personID", Utils.isStringValid(personId) ? personId : ""));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_MY_DIARY_V2,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"GET MY DIARY call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getNotifications(@NonNull String userId, int pageId, boolean onlyRequests)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			if (pageId == -1)
				pageId = 1;
			pairs.add(new Pair<>("pageID", pageId));
			pairs.add(new Pair<>("onlyRequests", onlyRequests));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_NOTIFICATIONS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"GET NOTIFICATIONS call", onlyRequests);
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] setNotificationAsRead(@NonNull String userId, @NonNull String notificationId)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, notificationId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("notificationID", notificationId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SET_NOTIFICATION_READ,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_NOTIFICATIONS, list,
					"SET NOTIFICATION READ call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getSinglePost(@NonNull String userId, @NonNull String postId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, postId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("postID", postId));
			pairs.add(new Pair<>("userID", userId));

			// adds static tag for temporary versioning
			pairs.add(new Pair<>("v", BuildConfig.BASE_VERSION_NAME));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_SINGLE_POST,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"GET SINGLE POST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] doActionOnNotification(@NonNull String userId, @NonNull String notificationId,
	                                              @NonNull ActionTypeEnum type)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, notificationId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("notificationID", notificationId));
			pairs.add(new Pair<>("action", type.toString()));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_DO_ACTION,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_NOTIFICATIONS, list,
					"DO ACTION NOTIFICATION call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] updateProfile(@NonNull String userId, @NonNull String context,
	                                     @NonNull HLUserAboutMe aboutMe,
	                                     @NonNull List<LifeEvent> lifeEvents,
	                                     @NonNull HLUserAboutMeMore moreAboutMe,
										 @NonNull Resources resources)
			throws JSONException {

		return updateProfile(userId, context, aboutMe, lifeEvents, moreAboutMe, resources, null, null, null,
				null, null);
	}

	public static Object[] updateInterestProfile(@NonNull String userId, @NonNull String interestId,
	                                             @Nullable String interestName,
	                                             @Nullable String interestHeadline,
	                                             @Nullable HLInterestAbout interestAbout,
	                                             @Nullable List<MoreInfoObject> moreInfo)
			throws JSONException {

		return updateProfile(userId, null, null, null, null, null, interestId, interestName,
				interestHeadline, interestAbout, moreInfo);
	}

	public static Object[] updateProfile(@NonNull String userId, @Nullable String context,
	                                     @Nullable HLUserAboutMe aboutMe,
	                                     @Nullable List<LifeEvent> lifeEvents,
	                                     @Nullable HLUserAboutMeMore moreAboutMe, Resources resources,
	                                     @Nullable String interestId,
	                                     @Nullable String interestName, @Nullable String interestHeadline,
	                                     @Nullable HLInterestAbout interestAbout,
	                                     @Nullable List<MoreInfoObject> moreInfo)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			String collection = Constants.SERVER_CODE_COLL_USERS;
			int operation = Constants.SERVER_OP_UPDATE_PROFILE;
			String tag = "UPDATE USER PROFILE call";

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			if (!Utils.isStringValid(context) && interestAbout != null) {
				collection = Constants.SERVER_CODE_COLL_INTERESTS;
				operation = Constants.SERVER_OP_UPDATE_INTEREST;
				tag = "UPDATE INTEREST PROFILE call";
				pairs.add(new Pair<>("interestID", interestId));
				pairs.add(new Pair<>("name", interestName));
				pairs.add(new Pair<>("headline", interestHeadline));
				pairs.add(new Pair<>("about", new JSONObject(interestAbout.serializeToString())));
				JSONArray array = new JSONArray();
				if (moreInfo != null && !moreInfo.isEmpty()) {
					for (MoreInfoObject mio : moreInfo) {
						if (mio != null) {
							String s = mio.serializeToString();
							array.put(new JSONObject(s));
						}
					}
				}
				pairs.add(new Pair<>("more_info", array));
			}
			else {
				if (Utils.isStringValid(context)) {
					pairs.add(new Pair<>("context", context));
					if (context.equals(UserDetailFragment.LIFE_EVENTS) && lifeEvents != null) {
						JSONArray array = new JSONArray();
						if (!lifeEvents.isEmpty()) {
							for (LifeEvent le : lifeEvents) {
								if (le != null) {
									String s = le.serializeToStringWithExpose();
									array.put(new JSONObject(s));
								}
							}
						}
						pairs.add(new Pair<>(context, array));
					}
					else if (context.equals(UserDetailFragment.ABOUT_ME) && aboutMe != null) {
						pairs.add(new Pair<>("birthdate", Utils.formatDateForDB(aboutMe.getBirthdayDate())));
						pairs.add(new Pair<>("status", aboutMe.getStatus()));
						pairs.add(new Pair<>("birthplace", aboutMe.getCity()));
						pairs.add(new Pair<>("description", aboutMe.getDescription()));
					}
					else if (context.equals(UserDetailFragment.MORE_ABOUT_ME) && moreAboutMe != null) {

						String notDefined = resources.getString(R.string.not_defined_yet);

						pairs.add(new Pair<>("sex", moreAboutMe.getGender().equals(notDefined) ? "" : moreAboutMe.getGender()));
						pairs.add(new Pair<>("birthPlace", moreAboutMe.getBirthPlace()));
						pairs.add(new Pair<>("interestedIn", moreAboutMe.getInterestedIn()));
						pairs.add(new Pair<>("otherNames", moreAboutMe.getOtherNames()));
						pairs.add(new Pair<>("address", moreAboutMe.getAddress()));
						JSONArray array = new JSONArray();
						if (moreAboutMe.getLanguages() != null && !moreAboutMe.getLanguages().isEmpty()) {
							for (String lang : moreAboutMe.getLanguages()) {
								if (Utils.isStringValid(lang)) {
									array.put(lang);
								}
							}
						}
						pairs.add(new Pair<>("languages", array));
					}
				}
			}
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, operation,
					Constants.SERVER_CODE_DB_NAME, collection, list, tag);
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] matchContactsWithHLUsers(@NonNull String userId,
	                                                @NonNull List<ProfileContactToSend> contacts)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		JSONArray objects = new JSONArray();

		if (!contacts.isEmpty() /*&& Utils.isStringValid(userId)*/) {
			for (ProfileContactToSend cts :
					contacts) {
				JSONObject json = new JSONObject(cts.serializeToStringWithExpose());
				objects.put(json);
			}

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_MATCH_USER_HL,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, objects,
					"MATCH USERS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] inviteWithEmail(@NonNull String userId, @NonNull String email)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, email)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("email", email));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_INVITE_EMAIL,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"INVITE WITH EMAIL call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] sendInvitedContact(@NonNull String userId, @NonNull ProfileContactToSend contact)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			if (contact.hasPhones()) {
				JSONArray phones = new JSONArray();
				for (String phone : contact.getPhones())
					phones.put(phone);
				pairs.add(new Pair<>("phones", phones));
			}

			if (contact.hasEmails()) {
				JSONArray emails = new JSONArray();
				for (String email : contact.getEmails())
					emails.put(email);
				pairs.add(new Pair<>("emails", emails));
			}

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SEND_INVITED_CONTACT,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"SEND INVITE CONTACT call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getMyInterests(@NonNull String userId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_MY_INTERESTS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"GET MY INTERESTS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getAllInterestsHomePage()
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_INTERESTS_HP,
				Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, new JSONArray(),
				"GET INTERESTS HP call");
		LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getInterestsByCategory(String userId, String categoryId, int pageId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, categoryId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("categoryID", categoryId));
			pairs.add(new Pair<>("pageID", pageId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_INTERESTS_BY_CATEGORY,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"GET INTERESTS BY CAT call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}



	public enum InterestActionType { FOLLOWING, PREFERRING }
	public static Object[] doPositiveActionOnInterest(@NonNull InterestActionType type, @NonNull String userId, @NonNull String interestId)
			throws JSONException {

		return doBooleanActionOnInterest(type, userId, interestId, true);
	}

	public static Object[] doNegativeActionOnInterest(@NonNull InterestActionType type, @NonNull String userId, @NonNull String interestId)
			throws JSONException {

		return doBooleanActionOnInterest(type, userId, interestId, false);
	}

	private static Object[] doBooleanActionOnInterest(@NonNull InterestActionType type, @NonNull String userId, @NonNull String interestId, boolean bool)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, interestId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("interestID", interestId));
			pairs.add(new Pair<>(
					type == InterestActionType.FOLLOWING ? "follow" : "isPreferred", bool)
			);
			list.add(pairs);

			String tag = (type == InterestActionType.FOLLOWING) ?
					(bool ? "FOLLOW" : "UNFOLLOW") : (bool ? "PREFER" : "UNPREFER");
			int operation = (type == InterestActionType.FOLLOWING) ?
					Constants.SERVER_OP_INTEREST_FOLLOW_UNFOLLOW : Constants.SERVER_OP_SET_AS_PREFERRED;

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, operation,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					(tag + " INTEREST call")
			);
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getInitiatives(@NonNull String interestId) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(interestId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("interestID", interestId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_INITIATIVES,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"GET INITIATIVES FOR INTEREST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getFollowers(@NonNull String userId, @NonNull String interestId,
	                                    int pageId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, interestId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("interestID", interestId));
			if (pageId == -1)
				pageId = 1;
			pairs.add(new Pair<>("pageID", pageId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_FOLLOWERS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"GET FOLLOWERS FOR INTEREST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getSimilarInterests(@NonNull String interestId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(interestId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("interestID", interestId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_SIMILAR_INTERESTS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"GET SIMILAR FOR INTEREST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] claimInterest(@NonNull String userId, @NonNull String interestId,
	                                     @Nullable String fileLink, @Nullable String phoneNumber,
	                                     @Nullable String language) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, interestId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("interestID", interestId));
			pairs.add(new Pair<>("date", Utils.formatDateForDB(System.currentTimeMillis())));

			pairs.add(new Pair<>("uploadedFile", Utils.isStringValid(fileLink) ? fileLink : ""));
			pairs.add(new Pair<>("phoneNumber", Utils.isStringValid(phoneNumber) ? phoneNumber : ""));
			pairs.add(new Pair<>("language", Utils.isStringValid(language) ? language : ""));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CLAIM_INTEREST,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"CLAIM INTEREST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] claimInterestGetLanguages() throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CLAIM_INTEREST_GET_LANG,
				Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, new ArrayList<List<Pair<String, Object>>>(),
				"CLAIM GET LANGUAGES call");
		LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getIdentities(@NonNull String userId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_IDENTITIES_V2,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"GET IDENTITIES V2 call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] removeFamilyRelation(@NonNull String userId, @NonNull FamilyRelationship familyRel) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, familyRel.getUserID())) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userIDRequesting", userId));
			pairs.add(new Pair<>("userIDRequested", familyRel.getUserID()));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_REMOVE_FAMILY,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"REMOVE FAMILY RELATION call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] canApplyFamilyFilter(@NonNull String userId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_CAN_APPLY_FAMILY_FILTER,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INTERESTS, list,
					"CAN APPLY FAMILY FILTER call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getUpdatedAvailableHearts(@NonNull String userId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_USER_HEARTS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list,
					"GET USER HEARTS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] logout(@NonNull Context context, @NonNull String userId) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		String token = SharedPrefsUtils.retrieveFCMTokenId(context);
		if (Utils.areStringsValid(userId, token)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("tokens", new JSONArray().put(token)));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_LOGOUT,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"LOGOUT call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] sendLocation(@NonNull String userId, @NonNull String flattenedLocation) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("location", flattenedLocation));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SEND_LOCATION,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"SEND LOCATION call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getNearMeForProfile(@NonNull String userId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_NEAR_ME,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"GET NEAR ME call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] sendDownloadHack(String userId, String url) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, url)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("docUrl", url));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SEND_HACK_DOC_DOWNLOADED,
		 			Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"SEND DOC DOWNLOAD HACK call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	//endregion


	//region == CIRCLES CALLS ==

	public static Object[] getCircle(@NonNull String userId, @NonNull String circleId,
	                                 @Nullable String filter, int pageId)
			throws JSONException {

		List<String> circles = new ArrayList<>();
		if (Utils.isStringValid(circleId))
			circles.add(circleId);

		return getCircles(userId, circles, filter, pageId);
	}

	public static Object[] getCircle(@NonNull String userId, @NonNull String circleId, int pageId)
			throws JSONException {

		return getCircle(userId, circleId, null, pageId);
	}

	public static Object[] getInnerCircle(@NonNull String userId, int pageId)
			throws JSONException {

		return getInnerCircle(userId, "", pageId);
	}

	public static Object[] getInnerCircle(@NonNull String userId, @Nullable String filter, int pageId)
			throws JSONException {

		return getCircle(userId, "", filter, pageId);
	}

	public static Object[] getCircles(@NonNull String userId, @NonNull List<String> circleIds,
	                                  @Nullable String filter, int pageId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			JSONArray arr = new JSONArray();
			if (circleIds.size() > 0) {
				for (String circle : circleIds)
					arr.put(circle);
			}
			else arr.put(Constants.INNER_CIRCLE_NAME);
			pairs.add(new Pair<>("listID", arr));

			if (pageId == -1)
				pageId = 1;
			pairs.add(new Pair<>("pageID", pageId));

			pairs.add(new Pair<>("filter", Utils.isStringValid(filter) ? filter : ""));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_CIRCLE,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
					"GET CIRCLES call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getCirclesForProfile(@NonNull String userId, int pageId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			if (pageId == -1)
				pageId = 1;
			pairs.add(new Pair<>("pageID", pageId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_CIRCLES_PROFILE,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
					"GET CIRCLES FOR PROFILE call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getInnerCircleForFamilyRelationships(String userId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_INNERCIRCLE_FOR_FAMILY,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"GET INNER CIRCLE FOR FAMILY call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] requestAuthorizationForInnerCircle(@NonNull HLUser user, @NonNull HLUserGeneric userGen)
			throws JSONException {

		return addUserToCircle(user, userGen.getId(), userGen.getCompleteName(), userGen.getAvatarURL(),
				"Inner Circle", "innercircle", null, null);
	}

	public static Object[] addUserToCircle(@NonNull HLUser user, @NonNull String userGenID,
	                                       @NonNull String userGenName, @Nullable String userGenAvatar,
	                                       String circleName, String type,
	                                       @Nullable String familyRelationshipID,
	                                       @Nullable String familyRelationshipName)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult;
		List<List<Pair<String, Object>>> list;

		list = new ArrayList<>();

		List<Pair<String, Object>> pairs = new ArrayList<>();
		pairs.add(new Pair<>("type", type));
		pairs.add(new Pair<>("date", Utils.formatDateForDB(System.currentTimeMillis())));
		pairs.add(new Pair<>("userID", user.getId()));
		String avatar = user.getAvatarURL();
		pairs.add(new Pair<>("avatarURL", Utils.isStringValid(avatar) ? avatar : ""));
		pairs.add(new Pair<>("name", user.getCompleteName()));
		pairs.add(new Pair<>("request", new JSONObject()
				.put("name", userGenName)
				.put("circleName", circleName)
				.put("avatarURL", Utils.isStringValid(userGenAvatar) ? userGenAvatar : "")
				.put("userID", userGenID)
				.put("familyRelationshipID", familyRelationshipID)
				.put("familyRelationshipName", familyRelationshipName)));

		list.add(pairs);

		jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_ADD_TO_CIRCLE,
				Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
				"ADD TO CIRCLE call");
		LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] requestAuthorizationForFamily(@NonNull HLUser userID,
	                                                     @NonNull GenericUserFamilyRels selected,
	                                                     @NonNull String relationID,
	                                                     @NonNull String relationName)
			throws JSONException {

		return addUserToCircle(userID, selected.getId(), selected.getCompleteName(), selected.getAvatarURL(),
				"Family", "family", relationID, relationName);
	}


	//endregion


	// region == WISHES CALLS ==

	public static Object[] getListElementForWish(@NonNull String userId, @Nullable String friendId,
	                                             @NonNull WishListElement element, boolean root,
	                                             @Nullable Bundle bundle, @Nullable String wishId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("friendID", Utils.isStringValid(friendId) ? friendId : ""));
			pairs.add(new Pair<>(
					"date",
					Utils.isStringValid(element.getDate()) ? element.getDate() : "")
			);
			pairs.add(new Pair<>(
					"id",
					Utils.isStringValid(element.getId()) ?
							element.getId() : "")
			);
			pairs.add(new Pair<>(
					"name",
					Utils.isStringValid(element.getName()) ?
							element.getName() : "")
			);
			pairs.add(new Pair<>("nextItem", root ? "root" : element.getNextItem()));
			pairs.add(new Pair<>("idWish", element.getIdWish()));
			pairs.add(new Pair<>("idStep", element.getIdStep()));
			pairs.add(new Pair<>("stepsTotal", element.getStepsTotal()));
			pairs.add(new Pair<>("isSelected", true));
			pairs.add(new Pair<>("wishID", Utils.isStringValid(wishId) ? wishId : ""));

			JSONObject data = new JSONObject();
			if (bundle != null && !bundle.isEmpty()) {
				for (String key : bundle.keySet()) {
					Object obj = bundle.get(key);
					if (obj instanceof ArrayList) {
						JSONArray arr = new JSONArray();
						for (int i = 0; i < ((ArrayList) obj).size(); i++) {
							arr.put(((ArrayList) obj).get(i));
						}
						data.put(key, arr);
					}
					else
						data.put(key, obj);
				}
			}
			pairs.add(new Pair<>("data", data));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"WISH GET ELEMENT call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] saveNewLifeEvent(@NonNull String userId, @NonNull LifeEvent event)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("lifeevent", new JSONObject(event.serializeToStringWithExpose())));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_SAVE_NEW_LIFE_EVENT,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_USERS, list,
					"WISH SAVE NEW EVENT call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getPostsForWish(@NonNull String userId, @NonNull PostTypeEnum type, int pageId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("type", type.toString()));
			if (pageId == -1)
				pageId = 1;
			pairs.add(new Pair<>("pageID", pageId));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_GET_POSTS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list,
					"WISH GET POSTS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getPostFoldersForWish(@NonNull String userId, int pageId, String listName)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("listID", Utils.isStringValid(listName) ? listName : ""));
			if (pageId == -1)
				pageId = 1;
			pairs.add(new Pair<>("pageID", pageId));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_GET_POST_FOLDERS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list,
					"WISH GET FOLDERS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] backActionOnWish(@NonNull String userId) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_BACK,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"WISH BACK ACTION call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] createEditPostForWish(@NonNull Post post) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		String uid = post.getAuthorId();
		if (Utils.isStringValid(uid)) {
			list = new ArrayList<>();

			int opCode = Constants.SERVER_OP_WISH_CREATE_POST;

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", uid));
			pairs.add(new Pair<>("date", post.getDate()));
			pairs.add(new Pair<>("type", post.getTypeEnum().toString()));
			if (post.hasMedia()) {
				JSONArray arr = new JSONArray();
				for (MemoryMediaObject media : post.getMediaObjects()) {
					arr.put(new JSONObject(media.serializeToString()));
				}
				pairs.add(new Pair<>("mediaObjects", arr));
			}
			pairs.add(new Pair<>("messageObject", post.getMessageObject().serializeToString()));

			if (post.hasWebLink())
				pairs.add(new Pair<>("webLink", new JSONObject(post.getWebLink().serializeToString())));

			if (post.hasTags())
				pairs.add(new Pair<>("tags", post.getTagsSerialized()));

			if (Utils.isStringValid(post.getId())) {
				pairs.add(new Pair<>("postID", post.getId()));
				opCode = Constants.SERVER_OP_WISH_EDIT_POST;
			}

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opCode,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_POSTS, list,
					"CREATE POST FOR WISH call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] saveEmailForWish(@NonNull String userId, @NonNull WishEmail email)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			JSONObject jEmail =  new JSONObject(email.serializeToString());
			Iterator<String> iter = jEmail.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				pairs.add(new Pair<>(key, jEmail.get(key)));
			}

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_SAVE_EMAIL,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"SAVE EMAIL FOR WISH call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] validatePhoneNumber(@NonNull String userId, @NonNull String name)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, name)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("name", name));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_VALIDATE_PHONE,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"VALIDATE PHONE NUMBER call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] checkPhoneNumberValidation(@NonNull String userId)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_CHECK_PHONE_VALIDATION,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"CHECK PHONE VALIDATION call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] saveWish(@NonNull String userId, @NonNull String wishName)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, wishName)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("wishName", wishName));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_SAVE,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"SAVE WISH call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getSummary(@Nullable String userId, @Nullable String wishId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)|| Utils.isStringValid(wishId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", Utils.isStringValid(userId) ? userId : ""));
			pairs.add(new Pair<>("wishID", Utils.isStringValid(wishId) ? wishId : ""));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_GET_SUMMARY,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"GET WISH SUMMARY call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getConfigurationData(@NonNull String userId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_CONFIGURATION_DATA,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"WISH CREATION ALLOWED call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getSavedWishes(@NonNull String userId) throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_SAVED_WISHES,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"GET SAVED WISHES call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] deleteWish(@NonNull String wishId) throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(wishId)) {
			list = new ArrayList<>();
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("wishID", wishId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_WISH_DELETE,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"DELETE WISH call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	//endregion


	// region == SETTINGS CALLS ==

	public static Object[] sendLegacyRequest(@NonNull String userId, @NonNull String legacyId)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, legacyId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("legacyContactID", legacyId));
			pairs.add(new Pair<>("date", Utils.formatDateForDB(System.currentTimeMillis())));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_LEGACY_REQUEST,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_WISHES, list,
					"SEND LEGACY REQUEST call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public enum SettingType { ACCOUNT, INNER_CIRCLE, CIRCLES }
	/**
	 * Common call for SETTINGS screens to GET info from server.
	 * Common to calls 1800, 1802.
	 *
	 * @param userId the main {@link HLUser} ID.
	 * @param type the provided {@link SettingType} value.
	 * @return The {@link Object[]} containing all the needed info.
	 * @throws JSONException if json serialization doesn't succeed.
	 */
	public static Object[] getSettings(@NonNull String userId, SettingType type)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			int opId = Constants.SERVER_OP_SETTINGS_IC_GET;
			String tag = "GET INNER CIRCLE SETTINGS";
			if (type == SettingType.ACCOUNT) {
				pairs.add(new Pair<>("type", "get"));
				opId = Constants.SERVER_OP_SETTINGS_GET_SET_USER_INFO;
				tag = "GET ACCOUNT SETTINGS";
			}
			else if (type == SettingType.CIRCLES) {
				opId = Constants.SERVER_OP_SETTINGS_IC_CIRCLES_GET;
				tag = "GET CIRCLES FOR SETTINGS";
			}
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opId,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] saveAccountSettings(@NonNull String userId, @NonNull Bundle bundle, SettingsAccountFragment.CallType type)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			for (String key : bundle.keySet())
				pairs.add(new Pair<>(key, bundle.get(key)));

			if (type == SettingsAccountFragment.CallType.SAVE)
				pairs.add(new Pair<>("type", "set"));

			list.add(pairs);

			int opId = Constants.SERVER_OP_SETTINGS_GET_SET_USER_INFO;
			String tag = "SET USER INFO";
			if (type == SettingsAccountFragment.CallType.CHANGE_PWD) {
				opId = Constants.SERVER_OP_SETTINGS_CHANGE_PWD;
				tag = "CHANGE PASSWORD";
			}

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opId,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public enum FoldersCirclesCallType { GET, RENAME, ADD, REMOVE }
	public static Object[] settingsOperationsOnCircles(@NonNull String userId, @NonNull Bundle bundle,
													   FoldersCirclesCallType type)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			for (String key : bundle.keySet())
				pairs.add(new Pair<>(key, bundle.get(key)));

			list.add(pairs);

			int opId = Constants.SERVER_OP_SETTINGS_IC_CIRCLES_ADD_DELETE;
			String tag = "ADD/DELETE CIRCLE";
			if (type == FoldersCirclesCallType.RENAME) {
				opId = Constants.SERVER_OP_SETTINGS_IC_CIRCLES_RENAME;
				tag = "RENAME CIRCLE";
			}
			else if (type == FoldersCirclesCallType.ADD) {
				tag = "ADD CIRCLE";
			}
			else if (type == FoldersCirclesCallType.REMOVE) {
				tag = "DELETE CIRCLE";
			}

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opId,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] settingsOperationsOnSingleCircle(@NonNull String userId, @NonNull Bundle bundle,
	                                                        SettingsICSingleCircleFragment.CallType type)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			for (String key : bundle.keySet())
				pairs.add(new Pair<>(key, bundle.get(key)));

			list.add(pairs);

			int opId = Constants.SERVER_OP_SETTINGS_IC_MEMBER_ADD_DELETE;
			String tag = "ADD/DELETE MEMBER TO/FROM CIRCLE";
			if (type == SettingsICSingleCircleFragment.CallType.UNFRIEND) {
				opId = Constants.SERVER_OP_SETTINGS_IC_UNFRIEND;
				tag = "UNFRIEND CONTACT";
			}
			else if (type == SettingsICSingleCircleFragment.CallType.ADD_MEMBER) {
				tag = "ADD MEMBER TO CIRCLE";
			}
			else if (type == SettingsICSingleCircleFragment.CallType.REMOVE_MEMBER) {
				tag = "DELETE MEMBER FROM CIRCLE";
			}

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opId,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] settingsOperationsOnFeedSettings(@NonNull String userId, @NonNull Bundle bundle,
	                                                        SettingsICTimelineFeedFragment.CallType type)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected() && type == SettingsICTimelineFeedFragment.CallType.SET)
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			for (String key : bundle.keySet())
				pairs.add(new Pair<>(key, bundle.get(key)));

			list.add(pairs);

			int opId = Constants.SERVER_OP_SETTINGS_IC_GET_SET_FEED;
			String tag = "GET FEED INFO";
			if (type == SettingsICTimelineFeedFragment.CallType.SET)
				tag = "SET FEED INFO";

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opId,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] settingsOperationsOnPrivacy(@NonNull String userId, int idItem,
	                                                   @NonNull SettingsPrivacySelectionFragment.PrivacySubItem subItem,
	                                                   SettingsPrivacySelectionFragment.CallType type)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected() && type == SettingsPrivacySelectionFragment.CallType.SET)
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId) && idItem >= 0) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("idxItem", idItem));

			String tag = "GET PRIVACY INFO";
			if (type == SettingsPrivacySelectionFragment.CallType.GET) {
				pairs.add(new Pair<>("type", "get"));
			}
			else if (type == SettingsPrivacySelectionFragment.CallType.SET) {
				JSONObject data = new JSONObject();
				data.put("idxSubItem", subItem.getIndex());
				JSONArray values = new JSONArray();
				if (subItem.hasValues()) {
					for (String val : subItem.getValues())
						values.put(val);

					data.put("values", values);
				}
				pairs.add(new Pair<>("data", data));

				pairs.add(new Pair<>("type", "set"));
				tag = "SET PRIVACY INFO";
			}

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_PRIVACY_GET_SET,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] settingsGetBlockedUsers(@NonNull String userId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_PRIVACY_GET_BLOCKED,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
					"GET BLOCKED USERS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] blockUnblockUsers(@NonNull String userId, @NonNull String friendId,
	                                         @NonNull UnBlockUserEnum type)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, friendId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("friendID", friendId));
			pairs.add(new Pair<>("operation", type.toString()));

			String tag = "BLOCK USER";
			if (type == UnBlockUserEnum.UNBLOCK)
				tag = "UNBLOCK USER";
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTING_BLOCK_UNBLOCK_USER,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getSettingsPostVisibilitySelection(@NonNull String userId,
	                                                          @NonNull PrivacyPostVisibilityEnum viewType)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			int idOp = Constants.SERVER_OP_SETTINGS_PRIVACY_POST_GET_SEL_USERS;
			String tag = "POST-VISIB GET SELECTED USERS";
			if (viewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES) {
				idOp = Constants.SERVER_OP_SETTINGS_PRIVACY_POST_GET_SEL_CIRCLES;
				tag = "POST-VISIB GET SELECTED CIRCLES";
			}

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, idOp,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
					tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getSetLegacyContactTrigger(@NonNull String userId,
	                                                  @NonNull SettingsSecurityLegacyContactTriggerFragment.CallType type,
	                                                  @Nullable ActionTypeEnum action,
	                                                  @Nullable String inactivityEra, int inactivityValue)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			int opId = -1;
			String tag = null, callType = null;
			switch (type) {
				case GET_2_STEP:
				case SET_2_STEP:
					opId = Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_2_STEP;
					tag = "GET 2 STEP";
					callType = "get";
					if (type == SettingsSecurityLegacyContactTriggerFragment.CallType.SET_2_STEP &&
							action != null) {
						tag = "SET 2 STEP";
						callType = "set";
						pairs.add(new Pair<>("status", action.toString()));
					}
					break;
				case GET_INACT:
				case SET_INACT:
					opId = Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_INACTIVITY;
					tag = "GET INACTIVITY";
					callType = "get";
					if (type == SettingsSecurityLegacyContactTriggerFragment.CallType.SET_INACT &&
							action != null) {
						tag = "SET INACTIVITY";
						callType = "set";
						pairs.add(new Pair<>("status", action.toString()));
					}
					break;
				case GET_PAPER:
				case SET_PAPER:
					opId = Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_PAPER;
					tag = "GET PAPER";
					callType = "get";
					if (type == SettingsSecurityLegacyContactTriggerFragment.CallType.SET_PAPER &&
							action != null) {
						tag = "SET PAPER";
						callType = "set";
						pairs.add(new Pair<>("status", action.toString()));
					}
					break;

				case GET_INACT_VALUES:
				case SET_INACT_VALUES:
					opId = Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_INACTIVITY_VALUES;
					tag = "GET INACTIVITY VALUES";
					callType = "get";
					if (type == SettingsSecurityLegacyContactTriggerFragment.CallType.SET_INACT_VALUES &&
							Utils.isStringValid(inactivityEra) && inactivityValue >= 0) {
						tag = "SET INACTIVITY VALUES";
						callType = "set";
						pairs.add(new Pair<>("format", inactivityEra));
						pairs.add(new Pair<>("qty", inactivityValue));
					}
					break;
			}

			if (Utils.areStringsValid(tag, callType)) {
				pairs.add(new Pair<>("type", callType));

				list.add(pairs);

				jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opId,
						Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
						tag + " call");
				LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
			}
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] get2StepVerificationContacts(@NonNull String userId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_SECURITY_GET_2_STEP_CONTACTS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
					"GET 2 STEP CONTACTS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] settingsOperationsOn2StepContacts(@NonNull String userId, @NonNull String contactId,
	                                                         SettingsSecurityLegacy2StepFragment.CallType type)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, contactId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("legacyContactID2step", contactId));
			pairs.add(new Pair<>("date", Utils.formatDateForDB(System.currentTimeMillis())));

			if (type == SettingsSecurityLegacy2StepFragment.CallType.ADD_2_STEP ||
					type == SettingsSecurityLegacy2StepFragment.CallType.REMOVE_2_STEP) {

				String op = "a", tag = "ADD 2 STEP CONTACT";
				if (type == SettingsSecurityLegacy2StepFragment.CallType.REMOVE_2_STEP) {
					op = "r";
					tag = "REMOVE 2 STEP CONTACT";
				}
				pairs.add(new Pair<>("operation", op));

				list.add(pairs);

				jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_ADD_REMOVE_2_STEP_CONTACT,
						Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
						tag + " call");
				LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
			}
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] deleteAccount(@NonNull String userId, @NonNull String reason)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, reason)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("reason", reason));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_SECURITY_DELETE_ACCOUNT,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
					"DELETE ACCOUNT call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static Object[] getSettingsHelpElements(@NonNull SettingsHelpElement element)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (element.hasNextItem()) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("nextItem", element.getNextItem()));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_HELP_GET_ELEMENTS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
					"GET SETTINGS HELP ELEMENTS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	// TODO: 3/30/2018    TEMPORARILY handles ONLY GET call
	public static Object[] settingsOperationsOnHelpElements(@NonNull String userId, @NonNull String itemId,
	                                                        @Nullable ActionTypeEnum action,
	                                                        @Nullable String email, @Nullable String message)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(userId, itemId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("itemID", itemId));
			pairs.add(new Pair<>("type", "set"));

			JSONObject data = new JSONObject();
			data.put("whatSelected", (action != null) ? action.toString() : "")
					.put("email", Utils.isStringValid(email) ? email : "")
					.put("message", Utils.isStringValid(message) ? message : "");
			pairs.add(new Pair<>("data", data));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SETTINGS_HELP_GET_SET_UI_LEVEL,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SETTINGS, list,
					"SET HELP VALUES call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] getMarketPlaces(@NonNull String userId)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId)) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_REDEEM_GET_MARKETPLACES,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_REDEEM, list,
					"REDEEM GET MARKETS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	public static Object[] redeemHearts(@NonNull String userId, @NonNull MarketPlace marketPlace,
	                                    long hearts)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.isStringValid(userId) && hearts > 0) {
			list = new ArrayList<>();

			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("hearts", hearts));
			pairs.add(new Pair<>("cash", ((double) marketPlace.getCurrentConversion())));
			pairs.add(new Pair<>("mktID", marketPlace.getId()));
			pairs.add(new Pair<>("date", Utils.formatDateForDB(System.currentTimeMillis())));
			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_REDEEM_HEARTS,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_REDEEM, list,
					"REDEEM GET MARKETS call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}


	//endregion


	//region == GLOBAL SEARCH ==

	public enum GlobalSearchAction { MOST_POPULAR, SEARCH, GET_DETAIL, GET_TIMELINE }
	public static Object[] actionsOnGlobalSearch(@NonNull String userId, @Nullable String queryString,
	                                             @NonNull GlobalSearchAction action,
	                                             @Nullable String returnType, int pageIdOrSkip)
			throws JSONException {

//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		list = new ArrayList<>();

		if (Utils.isStringValid(userId)) {
			List<Pair<String, Object>> pairs = new ArrayList<>();
			pairs.add(new Pair<>("userID", userId));

			String tag = "GET MOST POPULAR for GLOBAL SEARCH";
			int opId = Constants.SERVER_OP_SEARCH_GLOBAL_MOST_POP;
			if (action == GlobalSearchAction.SEARCH && Utils.isStringValid(queryString)) {
				tag = "DO GLOBAL SEARCH";
				opId = Constants.SERVER_OP_SEARCH_GLOBAL;
				pairs.add(new Pair<>("text", queryString));
			}
			else if (action == GlobalSearchAction.GET_DETAIL && returnType != null) {
				tag = "GET DETAIL for GLOBAL SEARCH";
				opId = Constants.SERVER_OP_SEARCH_GLOBAL_DETAIL;

				if (Utils.isStringValid(queryString))
					pairs.add(new Pair<>("text", queryString));

				pairs.add(new Pair<>("pageID", pageIdOrSkip));
				pairs.add(new Pair<>("searchResultType", returnType));

				// adds static tag for temporary versioning
				pairs.add(new Pair<>("v", BuildConfig.BASE_VERSION_NAME));
			}
			else if (action == GlobalSearchAction.GET_TIMELINE && returnType != null) {
				tag = "GET TIMELINE for GLOBAL SEARCH";
				opId = Constants.SERVER_OP_SEARCH_GLOBAL_TIMELINE;

//				if (Utils.isStringValid(queryString))
				pairs.add(new Pair<>("text", Utils.isStringValid(queryString) ? queryString : ""));

				pairs.add(new Pair<>("searchResultType", returnType));

				// new value
				pairs.add(new Pair<>("skip", pageIdOrSkip));
				// maintained for compatibility
				pairs.add(new Pair<>("pageID", 1));

				// adds static tag for temporary versioning
				pairs.add(new Pair<>("v", BuildConfig.BASE_VERSION_NAME));
			}

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, opId,
					Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_SEARCH,
					list, tag + " call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { LBSLinkApp.getSocketConnection().isConnected(), jsonResult[0], jsonResult[1] };
	}

	//endregion


	//region == CHAT: Reference HLServerCallsChat.kt ==
	//endregion

	//endregion


	//region == COMMON SERVER CALLS ==

	public static void doActionOnNotification(HLActivity activity, String notificationId, ActionTypeEnum type,
	                                          OnMissingConnectionListener connectionListener) {
		Object[] result = null;
		try {
			if (Utils.isContextValid(activity) &&
					Utils.areStringsValid(activity.getUser().getId(), notificationId))
				result = doActionOnNotification(activity.getUser().getId(), notificationId, type);
		} catch (JSONException e) {
			LogUtils.e("COMMON CALLS: ACTION ON NOTIFICATION", e.getMessage(), e);
		}

		HLRequestTracker.getInstance(((LBSLinkApp) activity.getApplication())).handleCallResult(connectionListener, activity, result);
	}

	public static Object[] authorizeFromNotification(@NonNull HLUser user, @NonNull Notification notification,
	                                                 boolean authorized)
			throws JSONException {

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list;

		if (Utils.areStringsValid(user.getId(), notification.getId())) {
			list = new ArrayList<>();

			String tag = null;
			int op = -1;
			if (notification.getType() == NotificationTypeEnum.ADD_TO_CIRCLE ||
					notification.getType() == NotificationTypeEnum.FAMILY_RELATIONSHIP ||
					notification.getType() == NotificationTypeEnum.TAG) {
				tag = "AUTHORIZE TO CIRCLE/FAMILY/TAG call";
				op = Constants.SERVER_OP_AUTHORIZE_TO_CIRCLE;
			} else if (notification.getType() == NotificationTypeEnum.LEGACY_CONTACT) {
				tag = "AUTHORIZE LEGACY CONTACT call";
				op = Constants.SERVER_OP_AUTHORIZE_LEGACY_CONTACT;
			}

			if (Utils.isStringValid(tag) && op != -1) {
				List<Pair<String, Object>> pairs = new ArrayList<>();
				pairs.add(new Pair<>("type", "authorization"));
				pairs.add(new Pair<>("userID", user.getId()));
				String avatar = user.getAvatarURL();
				pairs.add(new Pair<>("avatarURL", Utils.isStringValid(avatar) ? avatar : ""));
				pairs.add(new Pair<>("name", user.getCompleteName()));
				pairs.add(new Pair<>("notificationID", notification.getId()));
				pairs.add(new Pair<>("date", Utils.formatDateForDB(System.currentTimeMillis())));
				pairs.add(new Pair<>("request", new JSONObject()
						.put("name", notification.getName())
						.put("isAuthorized", authorized)
						.put("avatarURL", Utils.isStringValid(notification.getAvatarURL()) ? notification.getAvatarURL() : "")
						.put("userID", notification.getUserId())));

				list.add(pairs);

				jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, op,
						Constants.SERVER_CODE_DB_NAME, Constants.SERVER_CODE_COLL_INNER_CIRCLE, list,
						tag);
				LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
			}
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}

	public static void search(HLActivity activity, String query, SearchTypeEnum searchType, int pageId,
	                          OnMissingConnectionListener connectionListener) {
		Object[] result = null;
		if (Utils.isContextValid(activity) &&
				Utils.areStringsValid(activity.getUser().getId(), query)) {
			try {
				result = search(activity.getUser().getId(), searchType, query, pageId);
			} catch (JSONException e) {
				LogUtils.e("COMMON CALLS: SEARCH", e.getMessage(), e);
			}
		}

		HLRequestTracker.getInstance(((LBSLinkApp) activity.getApplication())).handleCallResult(connectionListener, activity, result);
	}

	public static Object[] search(String userId, SearchTypeEnum type, String queryString, int pageId)
			throws JSONException {
//		if (!LBSLinkApp.getSocketConnection().isConnected())
//			return resultsNotConnected();

		String[] jsonResult;
		List<List<Pair<String, Object>>> list;

		list = new ArrayList<>();

		List<Pair<String, Object>> pairs = new ArrayList<>();
		pairs.add(new Pair<>("type", type.toString()));
		pairs.add(new Pair<>("userID", userId));
		pairs.add(new Pair<>("text", queryString));

		if (pageId == -1)
			pageId = 1;
		pairs.add(new Pair<>("pageID", pageId));

		list.add(pairs);

		jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_SEARCH,
				Constants.SERVER_CODE_DB_NAME,
				type == SearchTypeEnum.INNER_CIRCLE ? Constants.SERVER_CODE_COLL_USERS : Constants.SERVER_CODE_COLL_INTERESTS,
				list, "DO SEARCH call");

		if (!LBSLinkApp.getSocketConnection().isConnected())
			return new Object[]{false, jsonResult[0], jsonResult[1]};
		else {
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
			return new Object[]{true, jsonResult[0], jsonResult[1]};
		}
	}

	public static Object[] performShareCreation(String userId, String id, boolean isChatMessage) throws JSONException {
		if (!LBSLinkApp.getSocketConnection().isConnected())
			return resultsNotConnected();

		String[] jsonResult = new String[2];
		List<List<Pair<String, Object>>> list = new ArrayList<>();
		List<Pair<String, Object>> pairs = new ArrayList<>();

		if (Utils.areStringsValid(userId, id)) {
			pairs.add(new Pair<>("userID", userId));
			pairs.add(new Pair<>("id", id));
			pairs.add(new Pair<>("isChatMessage", isChatMessage));

			list.add(pairs);

			jsonResult = buildMessageJson(Constants.SERVER_CODE_NAME, Constants.SERVER_OP_GET_SHAREABLE_LINK,
					Constants.SERVER_CODE_DB_NAME, list, "GET SHAREABLE LINK call");
			LBSLinkApp.getSocketConnection().sendMessage(jsonResult[0]);
		}

		return new Object[] { true, jsonResult[0], jsonResult[1] };
	}


	//endregion



	private static class BasicBody/* implements JsonHelper.JsonDeSerializer */{

		private final String name = Constants.SERVER_CODE_NAME;
		private final String dbName = Constants.SERVER_CODE_DB_NAME;
		private final String collection;
		private final int action;
		private final String idOperation;
		private JSONObject event;
		private JSONArray objects;

		public BasicBody(String collection, int action, String idOperation) {
			this.collection = collection;
			this.action = action;
			this.idOperation = idOperation;
			this.event = new JSONObject();
			this.objects = new JSONArray();
		}

		public void setObjects(JSONObject target, List<List<Pair<String, Object>>> pairs) throws JSONException {
			JSONObject obj;
			if (target != null) {
				if (pairs != null && !pairs.isEmpty()) {
					for (List<Pair<String, Object>> lp : pairs) {
						if (lp != null && !lp.isEmpty()) {
							obj = new JSONObject();
							for (Pair<String, Object> p : lp) {
								obj.put(p.first, p.second);
							}
							objects.put(obj);
						}
					}
				}
				target.put("objects", objects);
			}
		}

		/*
		@Override
		public JSONObject serializeWithExpose() throws JSONException, IllegalAccessException {
			return JsonHelper.serializeWithExpose(this);
		}

		@Override
		public JsonHelper.JsonDeSerializer deserialize(String jsonString)
				throws IllegalAccessException, NoSuchFieldException, JSONException {
			return JsonHelper.deserialize(jsonString, this);
		}

		@Override
		public JsonHelper.JsonDeSerializer deserialize(JSONObject json)
				throws IllegalAccessException, NoSuchFieldException, JSONException {
			return JsonHelper.deserialize(json, this);
		}

		@Override
		public Map<String, String> replaceNameToServer() {
			return null;
		}

		@Override
		public Map<String, String> replaceNameToClient() {
			return null;
		}

		@Override
		public Set<String> excludeFields() {
			return null;
		}

		@Override
		public Object getSelfObject() {
			return this;
		}
		*/
	}

}