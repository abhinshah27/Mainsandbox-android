package it.keybiz.lbsapp.corporate.utilities;

/**
 * Contains all the constants used in Highlanders project.
 *
 * @author mbaldrighi on 9/14/2017.
 */
public class Constants {

	/* ====== */
	/* SERVER */
	/* ====== */

	public static final String SERVER_END_POINT_DEBUG = "ws://accessocasa.ddns.net:3030/rtcom";
	public static final String SERVER_END_POINT_DEBUG_CHAT = "ws://accessocasa.ddns.net:3030/chat";
	public static final String SERVER_END_POINT_3031 = "ws://ec2-34-201-111-17.compute-1.amazonaws.com:3031/rtcom";
	public static final String SERVER_END_POINT_80 = "ws://ec2-34-201-111-17.compute-1.amazonaws.com:80/rtcom";

	public static final String SERVER_END_POINT_DEV = "ws://34.201.111.17:3031/rtcomluiss";
	public static final String SERVER_END_POINT_PROD = "wss://nativesocket.highlanders.app:3031/rtcomluiss";
	public static final String SERVER_END_POINT_DEV_CHAT = "ws://34.201.111.17:3031/chat";
	public static final String SERVER_END_POINT_PROD_CHAT = "wss://nativesocket.highlanders.app:3030/chat";

	public static final String MEDIA_UPLOAD_URL_DEV = "http://ec2-34-201-111-17.compute-1.amazonaws.com/luiss/UploadMedia.aspx";
	public static final String MEDIA_UPLOAD_URL_DEV_TEST = "http://ec2-34-201-111-17.compute-1.amazonaws.com/luiss/UploadTest.aspx";
	public static final String MEDIA_UPLOAD_URL_DEBUG_8080 = "http://accessocasa.ddns.net:8080/UploadMedia.aspx";
	public static final String MEDIA_UPLOAD_URL_PROD = "https://upload.lsm.icrossing.tech/luiss/UploadMedia.aspx";

	public static final String SERVER_CODE_DB_NAME = "luiss";

	public static final String SERVER_CODE_NAME = "database";
	public static final String SERVER_CODE_NAME_SUBSCR = "socketsubscription";
	public static final String SERVER_CODE_NAME_TIMELINE = "timeline";

	public static final String SERVER_CODE_COLL_USERS = "users";
	public static final String SERVER_CODE_COLL_POSTS = "posts";
	public static final String SERVER_CODE_COLL_COMMENTS = "comments";
	public static final String SERVER_CODE_COLL_SHARES = "shares";
	public static final String SERVER_CODE_COLL_HEARTS = "hearts";
	public static final String SERVER_CODE_COLL_TAG = "tags";
	public static final String SERVER_CODE_COLL_INNER_CIRCLE = "innercircle";
	public static final String SERVER_CODE_COLL_NOTIFICATIONS = "notifications";
	public static final String SERVER_CODE_COLL_INTERESTS = "interests";
	public static final String SERVER_CODE_COLL_INITIATIVES = "initiatives";
	public static final String SERVER_CODE_COLL_WISHES = "wishes";
	public static final String SERVER_CODE_COLL_SETTINGS = "settings";
	public static final String SERVER_CODE_COLL_SEARCH = "search";
	public static final String SERVER_CODE_COLL_REDEEM = "redeem";
	public static final String SERVER_CODE_COLL_CHAT = "chatmessages";

	// LOGIN/SIGNUP
	public static final int SERVER_OP_SIGNUP = 1000;
	public static final int SERVER_OP_SIGNIN = 1001;
	public static final int SERVER_OP_HCODE_VERIFY = 1006;
	public static final int SERVER_OP_PWD_RECOVERY = 1007;
	public static final int SERVER_OP_REPORT_USER = 1009;
	public static final int SERVER_OP_SIGNIN_V2 = 1010;

	// GENERIC
	public static final int SERVER_OP_SOCKET_SUBSCR = 1008;
	public static final int SERVER_OP_FCM_TOKEN = 1005;

	// SEARCH and GLOBAL SEARCH
	public static final int SERVER_OP_SEARCH = 4000;
	public static final int SERVER_OP_SEARCH_GLOBAL = 4001;
	public static final int SERVER_OP_SEARCH_GLOBAL_MOST_POP = 4002;
	public static final int SERVER_OP_SEARCH_GLOBAL_DETAIL = 4003;
	public static final int SERVER_OP_SEARCH_GLOBAL_TIMELINE = 4004;

	// TIMELINE
	public static final int SERVER_OP_GET_TIMELINE = 1100;
	public static final int SERVER_OP_GET_COMMENTS = 1101;
	public static final int SERVER_OP_GET_SHARES = 1102;
	public static final int SERVER_OP_GET_HEARTS = 1103;
	public static final int SERVER_OP_CREATE_POST = 1104;
	public static final int SERVER_OP_SEND_HEARTS = 1105;
	public static final int SERVER_OP_SEND_COMMENT = 1106;
	public static final int SERVER_OP_SHARE_POST = 1107;
	public static final int SERVER_OP_ADD_TAGS = 1108;
	public static final int SERVER_OP_DELETE_POST = 1109;
	public static final int SERVER_OP_MANAGE_COMMENT = 1111;
	public static final int SERVER_OP_MODERATION = 1112;
	public static final int SERVER_OP_LIKE_COMMENT = 1113;
	public static final int SERVER_OP_EDIT_POST = 1114;
	public static final int SERVER_OP_GET_PARSED_WEB_LINK = 1115;
	public static final int SERVER_OP_SET_RT_DATA_READ = 1117;
	public static final int SERVER_OP_CREATE_POST_V2 = 1118;

	// FOLDERS
	public static final int SERVER_OP_FOLDERS_CREATE = 1300;
	public static final int SERVER_OP_FOLDERS_ADD_POST = 1301;
	public static final int SERVER_OP_FOLDERS_REMOVE_POST = 1302;
	public static final int SERVER_OP_FOLDERS_DELETE = 1303;
	public static final int SERVER_OP_FOLDERS_GET = 1304;
	public static final int SERVER_OP_FOLDERS_RENAME = 1305;

	// CIRCLES
	public static final int SERVER_OP_ADD_TO_CIRCLE = 1003;
	public static final int SERVER_OP_AUTHORIZE_TO_CIRCLE = 1004;
	public static final int SERVER_OP_GET_CIRCLE = 1200;

	// PROFILE
	public static final int SERVER_OP_GET_USER_PROFILE = 1400;
	public static final int SERVER_OP_GET_PERSON = 1401;
	public static final int SERVER_OP_GET_MY_DIARY = 1402;
	public static final int SERVER_OP_GET_NOTIFICATIONS = 1403;
	public static final int SERVER_OP_GET_NOTIFICATIONS_REQUESTS = 1499;
	public static final int SERVER_OP_SET_NOTIFICATION_READ = 1404;
	public static final int SERVER_OP_GET_SINGLE_POST = 1405;
	public static final int SERVER_OP_GET_INTERESTS_HP = 1406;
	public static final int SERVER_OP_GET_MY_INTERESTS = 1408;
	public static final int SERVER_OP_DO_ACTION = 1409;
	public static final int SERVER_OP_GET_CIRCLES_PROFILE = 1410;
	public static final int SERVER_OP_MATCH_USER_HL = 1411;
	public static final int SERVER_OP_UPDATE_PROFILE = 1412;
	public static final int SERVER_OP_INVITE_EMAIL = 1413;
	public static final int SERVER_OP_GET_IDENTITIES = 1414;
	public static final int SERVER_OP_GET_NOTIFICATION_COUNT = 1416;
	public static final int SERVER_OP_GET_INNERCIRCLE_FOR_FAMILY = 1417;
	public static final int SERVER_OP_REMOVE_FAMILY = 1418;
	public static final int SERVER_OP_CAN_APPLY_FAMILY_FILTER = 1419;
	public static final int SERVER_OP_GET_IDENTITIES_V2 = 1420;
	public static final int SERVER_OP_GET_USER_HEARTS = 1421;
	public static final int SERVER_OP_GET_PERSON_V2= 1422;
	public static final int SERVER_OP_LOGOUT = 1424;
	public static final int SERVER_OP_GET_MY_DIARY_V2 = 1425;
	public static final int SERVER_OP_SEND_LOCATION = 1426;
	public static final int SERVER_OP_GET_NEAR_ME = 1427;
	public static final int SERVER_OP_SEND_INVITED_CONTACT = 1428;
	public static final int SERVER_OP_SEND_HACK_DOC_DOWNLOADED = 1432;

	// INTERESTS
	public static final int SERVER_OP_GET_INTEREST_PROFILE = 1500;
	public static final int SERVER_OP_UPDATE_INTEREST = 1501;
	public static final int SERVER_OP_GET_INITIATIVES = 1502;
	public static final int SERVER_OP_GET_SIMILAR_INTERESTS = 1503;
	public static final int SERVER_OP_GET_FOLLOWERS = 1504;
	public static final int SERVER_OP_SET_AS_PREFERRED = 1505;
	public static final int SERVER_OP_INTEREST_FOLLOW_UNFOLLOW = 1506;
	public static final int SERVER_OP_CLAIM_INTEREST = 1507;
	public static final int SERVER_OP_GET_INTERESTS_BY_CATEGORY = 1508;
	public static final int SERVER_OP_CLAIM_INTEREST_GET_LANG = 1510;

	// WISHES
	public static final int SERVER_OP_WISH_GET_LIST_ELEMENTS = 1600;
	public static final int SERVER_OP_WISH_SAVE_NEW_LIFE_EVENT = 1601;
	public static final int SERVER_OP_WISH_CREATE_POST = 1602;
	public static final int SERVER_OP_WISH_EDIT_POST = 1603;
	public static final int SERVER_OP_WISH_GET_POSTS = 1605;
	public static final int SERVER_OP_WISH_SAVE_EMAIL = 1606;
	public static final int SERVER_OP_WISH_VALIDATE_PHONE = 1607;
	public static final int SERVER_OP_WISH_CHECK_PHONE_VALIDATION = 1608;
	public static final int SERVER_OP_WISH_BACK = 1609;
	public static final int SERVER_OP_WISH_GET_POST_FOLDERS = 1610;
	public static final int SERVER_OP_WISH_SAVE = 1611;
	public static final int SERVER_OP_GET_SAVED_WISHES = 1612;
	public static final int SERVER_OP_WISH_DELETE = 1614;
	public static final int SERVER_OP_WISH_GET_SUMMARY = 1615;

	// SETTINGS
	public static final int SERVER_OP_SETTINGS_LEGACY_REQUEST = 1700;
	public static final int SERVER_OP_AUTHORIZE_LEGACY_CONTACT = 1701;
	public static final int SERVER_OP_SETTINGS_ADD_REMOVE_2_STEP_CONTACT = 1702;

	public static final int SERVER_OP_SETTINGS_GET_SET_USER_INFO = 1800;
	public static final int SERVER_OP_SETTINGS_CHANGE_PWD = 1801;
	public static final int SERVER_OP_SETTINGS_IC_GET = 1802;
	public static final int SERVER_OP_SETTINGS_IC_CIRCLES_GET = 1803;
	public static final int SERVER_OP_SETTINGS_IC_CIRCLES_RENAME = 1804;
	public static final int SERVER_OP_SETTINGS_IC_CIRCLES_ADD_DELETE = 1805;
	public static final int SERVER_OP_SETTINGS_IC_UNFRIEND = 1806;
	public static final int SERVER_OP_SETTINGS_IC_GET_SET_FEED = 1807;
	public static final int SERVER_OP_SETTINGS_IC_MEMBER_ADD_DELETE = 1808;
	public static final int SERVER_OP_SETTINGS_PRIVACY_GET_SET = 1809;
	public static final int SERVER_OP_SETTINGS_PRIVACY_POST_GET_SEL_CIRCLES = 1810;
	public static final int SERVER_OP_SETTINGS_PRIVACY_POST_GET_SEL_USERS = 1811;
	public static final int SERVER_OP_SETTING_BLOCK_UNBLOCK_USER = 1812;
	public static final int SERVER_OP_SETTINGS_PRIVACY_GET_BLOCKED = 1813;
	public static final int SERVER_OP_SETTINGS_SECURITY_GET_2_STEP_CONTACTS = 1814;
	public static final int SERVER_OP_SETTINGS_SECURITY_GET_SET_2_STEP = 1815;
	public static final int SERVER_OP_SETTINGS_SECURITY_GET_SET_INACTIVITY = 1816;
	public static final int SERVER_OP_SETTINGS_SECURITY_GET_SET_INACTIVITY_VALUES = 1817;
	public static final int SERVER_OP_SETTINGS_SECURITY_GET_SET_PAPER = 1818;
	public static final int SERVER_OP_SETTINGS_SECURITY_DELETE_ACCOUNT = 1819;
	public static final int SERVER_OP_SETTINGS_HELP_GET_ELEMENTS = 1820;
	public static final int SERVER_OP_SETTINGS_HELP_GET_SET_UI_LEVEL = 1821;

	public static final int SERVER_OP_SETTINGS_CONFIGURATION_DATA = 1822;

	public static final int SERVER_OP_CHAT_INITIALIZE_ROOM = 1900;
	public static final int SERVER_OP_CHAT_SEND_MESSAGE = 1901;
	public static final int SERVER_OP_CHAT_UPDATE_LIST = 1902;
	public static final int SERVER_OP_CHAT_DELETE_ROOM = 1903;
	public static final int SERVER_OP_CHAT_FETCH_MESSAGES = 1904;
	public static final int SERVER_OP_CHAT_SEND_USER_ACTIVITY = 1907;
	public static final int SERVER_OP_CHAT_HACK = 1909;
	public static final int SERVER_OP_CHAT_SET_MESSAGES_READ = 1910;
	public static final int SERVER_OP_CHAT_SET_USER_ONLINE = 1913;
	public static final int SERVER_OP_CHAT_GET_NEW_CHATS = 1914;


	public static final int SERVER_OP_REDEEM_GET_MARKETPLACES = 5000;
	public static final int SERVER_OP_REDEEM_HEARTS = 5001;


	public static final int SERVER_SORT_TL_ENUM_DEFAULT = 2000;
	public static final int SERVER_SORT_TL_ENUM_RECENT = 2001;
	public static final int SERVER_SORT_TL_ENUM_LOVED = 2002;
	public static final int SERVER_SORT_TL_ENUM_SHUFFLE = 2003;

	public static final int SERVER_AUTOPLAY_TL_ENUM_BOTH = 3000;
	public static final int SERVER_AUTOPLAY_TL_ENUM_WIFI = 3001;
	public static final int SERVER_AUTOPLAY_TL_ENUM_NEVER = 3002;


	// REAL-TIME COMMUNICATION
	public static final int SERVER_OP_RT_NEW_POST = 1200;
	public static final int SERVER_OP_RT_UPDATE_POST = 1201;
	public static final int SERVER_OP_RT_DELETE_POST = 1202;
	public static final int SERVER_OP_RT_UPDATE_HEARTS = 1203;
	public static final int SERVER_OP_RT_UPDATE_SHARES = 1204;
	public static final int SERVER_OP_RT_UPDATE_TAGS = 1205;
	public static final int SERVER_OP_RT_UPDATE_COMMENTS = 1206;
	public static final int SERVER_OP_RT_NEW_SHARE = 1207;
	public static final int SERVER_OP_RT_NEW_COMMENT = 1208;
	public static final int SERVER_OP_RT_EDIT_POST = 1209;
	public static final int SERVER_OP_RT_PUSH_DATA = 1222;

	public static final int SERVER_OP_RT_CHAT_DELIVERY = 1905;
	public static final int SERVER_OP_RT_CHAT_UPDATE_STATUS = 1906;
	public static final int SERVER_OP_RT_CHAT_UPDATE_ACTIVITY = 1908;
	public static final int SERVER_OP_RT_CHAT_MESSAGE_READ = 1911;
	public static final int SERVER_OP_RT_CHAT_MESSAGE_DELIVERED = 1912;
	public static final int SERVER_OP_RT_CHAT_DOCUMENT_OPENED = 1915;


	// ERRORS
	public static final int SERVER_ERROR_SIGNUP_USER_ALREADY_EXISTS = 3000;
	public static final int SERVER_ERROR_SIGNIN_WRONG_USERNAME = 3001;
	public static final int SERVER_ERROR_SIGNIN_EMAIL_NOT_CONFIRMED = 3002;
	public static final int SERVER_ERROR_LIST_ALREADY_PRESENT = 3003;
	public static final int SERVER_ERROR_USER_NOT_OWNER = 3004;
	public static final int SERVER_ERROR_INTEREST_ALREADY_CLAIMED = 3005;
	public static final int SERVER_ERROR_SETTINGS_CHANGE_PWD_NO_MATCH_NEW = 3006;
	public static final int SERVER_ERROR_SETTINGS_CHANGE_PWD_NO_MATCH_OLD = 3007;
	public static final int SERVER_ERROR_SETTINGS_5PL_2_STEP_CONTACTS = 3010;
	public static final int SERVER_ERROR_PIC_MODERATION = 3012;
	public static final int SERVER_ERROR_SIGNIN_WRONG_PWD = 3014;
	public static final int SERVER_ERROR_RECOVERY_NO_EMAIL = 3015;
	public static final int SERVER_ERROR_UPLOAD_NO_SIZE = 3017;
	public static final int SERVER_ERROR_REDEEM_NOT_ENOUGH_HEARTS = 3018;
	public static final int SERVER_ERROR_GENERIC = 3999;


	// SHARING
	public static final int SERVER_OP_GET_SHAREABLE_LINK = 5002;


	/* ====== */
	/* CLIENT */
	/* ====== */

	public static final String HTTP_KEY = "jruit657KFIOR7584ROR9585O94jfdRG";

	public static final String GUEST_USER_ID = "73058227abd6ad0a622f1a6b";

	public static final long TIME_UNIT_SECOND = 1000;
	public static final long TIME_UNIT_MINUTE = 60 * TIME_UNIT_SECOND;
	public static final long TIME_UNIT_HOUR = 60 * TIME_UNIT_MINUTE;
	public static final long TIME_UNIT_DAY = 24 * TIME_UNIT_HOUR;
	public static final long TIME_UNIT_WEEK = 7 * TIME_UNIT_DAY;
	public static final long TIME_UNIT_MONTH = 30 * TIME_UNIT_DAY;
	public static final long TIME_UNIT_YEAR = 365 * TIME_UNIT_DAY;

	public static final long ONE_MB_IN_BYTES = ((long) 1e6);

	public static final int VIBE_SHORT = 100;
	public static final int VIBE_LONG = 750;

	public static final String EXTRA_PARAM_1 = "extra_param_1";
	public static final String EXTRA_PARAM_2 = "extra_param_2";
	public static final String EXTRA_PARAM_3 = "extra_param_3";
	public static final String EXTRA_PARAM_4 = "extra_param_4";
	public static final String EXTRA_PARAM_5 = "extra_param_5";
	public static final String EXTRA_PARAM_6 = "extra_param_6";
	public static final String EXTRA_PARAM_7 = "extra_param_7";
	public static final String EXTRA_PARAM_8 = "extra_param_8";
	public static final String EXTRA_PARAM_9 = "extra_param_9";

	public static final String REGEX_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	public static final String REGEX_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@!%*?&])[A-Za-z\\d$@!%*?&]{8,}";
	public static final String REGEX_HCODE = "^[A-Z\\d]{5}$";

	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	public static final String TRANSITION_LANDSCAPE = "landscape";
	public static final String TRANSITION_WISH_POST_ENLARGE = "wish_post_enlarge";

	public static final String BROADCAST_NO_CONNECTION = "broadcast_no_connection";
	public static final String BROADCAST_CLOSE_SPLASH = "broadcast_close_splash";
	public static final String BROADCAST_SERVER_RESPONSE = "broadcast_server_response";
	public static final String BROADCAST_SOCKET_SUBSCRIPTION = "broadcast_socket_subscription";
	public static final String BROADCAST_FCM_TOKEN = "broadcast_fcm_token";
	public static final String BROADCAST_REALTIME_COMMUNICATION = "broadcast_realtime_communication";
	public static final String BROADCAST_CIRCLES_RETRIEVED = "broadcast_circles_retrieved";

	public static final String URL_TOS = "http://web.lsm.icrossing.tech/luiss/termini_di_servizio.pdf";
	public static final String URL_PRIVACY = "http://web.lsm.icrossing.tech/luiss/privacy.pdf";
	public static final String URL_COOKIES = "http://web.lsm.icrossing.tech/luiss/cookies.pdf";

	public static final int PAGINATION_AMOUNT = 20;

	public static final String INNER_CIRCLE_NAME = "Inner circle";
	public static final String CIRCLE_FAMILY_NAME = "Family";

	// TODO: 2/28/19    RETURN HERE: FOR NOW HARDCODED
	public static final String INNER_CIRCLE_NAME_IT = "Il mio network";
	public static final String CIRCLE_FAMILY_NAME_IT = "Famiglia";

	public static final String ID_INTEREST_HIGHLANDERS = "int_highlanders";

	public static final String DIARY_LIST_ID_POSTS = "mydiary";
	public static final String DIARY_LIST_ID_PROFILE_PIC = "profilepictures";
	public static final String DIARY_LIST_ID_WALL_PIC = "wallpictures";


	public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";  // Change when in stable

	public static final String WEB_LINK_PLACEHOLDER_URL = "https://s3.eu-west-3.amazonaws.com/luiss-media-storage/0Fixed/Empty.png";


	/* FILES and PATHS */
	public static final String PATH_CUSTOM_HIGHLANDERS = "hl-cache";
	public static final String PATH_EXTERNAL_DIR_MEDIA_PHOTO = "hl-pictures";
//			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
	public static final String PATH_EXTERNAL_DIR_MEDIA_VIDEO = "hl-videos";
//			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
	public static final String PATH_EXTERNAL_DIR_MEDIA_AUDIO = "hl-audio";
	public static final String FILENAME_MEDIA_AUDIO = "hl-audio";
	public static final String FILENAME_MEDIA_PHOTO = "hl-image";
	public static final String FILENAME_MEDIA_VIDEO = "hl-video";
	public static final String MIME_VIDEO = "video/mp4";
	public static final String MIME_AUDIO = "audio/mp4";
	public static final String MIME_PIC = "image/jpg";


	/* PERMISSIONS */
	public static final int PERMISSIONS_REQUEST_READ_WRITE_CAMERA = 0;
	public static final int PERMISSIONS_REQUEST_READ_WRITE_AUDIO = 1;
	public static final int PERMISSIONS_REQUEST_READ = 2;
	public static final int PERMISSIONS_REQUEST_GALLERY = 2;
	public static final int PERMISSIONS_REQUEST_CONTACTS = 3;
	public static final int PERMISSIONS_REQUEST_SMS = 4;
	public static final int PERMISSIONS_REQUEST_DOCUMENTS = 5;
	public static final int PERMISSIONS_REQUEST_GALLERY_CUSTOM = 6;
	public static final int PERMISSIONS_REQUEST_LOCATION = 7;
	public static final int PERMISSIONS_REQUEST_CAMERA_MIC_CALLS = 8;
	public static final int PERMISSIONS_REQUEST_PHONE_STATE = 9;

	/* ACTIVITY RESULTS */
	public static final String REQUEST_CODE_KEY = "request_code";
	public static final int NO_RESULT = -1;
	public static final int RESULT_AUDIO = 0;
	public static final int RESULT_PHOTO = 1;
	public static final int RESULT_VIDEO = 2;
	public static final int RESULT_TIMELINE_INTERACTIONS = 3;
	public static final int RESULT_TIMELINE_INTERACTIONS_VIEW = 4;
	public static final int RESULT_EDIT_COMMENT = 5;
	public static final int RESULT_REPLY_TO_COMMENT = 6;
	public static final int RESULT_CREATE_POST = 7;
	public static final int RESULT_GALLERY = 8;
	public static final int RESULT_SINGLE_POST = 9;
	public static final int RESULT_DOCUMENTS = 10;
	public static final int RESULT_SELECT_IDENTITY = 11;
	public static final int RESULT_WISH_CREATE = 12;
	public static final int RESULT_SELECT_LEGACY_CONTACT = 13;
	public static final int RESULT_WISH_EDIT = 14;
	public static final int RESULT_SETTINGS_PRIVACY_SELECTION = 15;
	public static final int RESULT_CREATE_POST_VISIBILITY = 16;
	public static final int RESULT_PROFILE_ADD_LANGUAGES = 17;
	public static final int RESULT_FULL_VIEW_VIDEO = 18;
	public static final int RESULT_ADD_COMMENT = 19;
	public static final int RESULT_CREATE_POST_PREVIEW = 20;
	public static final int RESULT_SETTINGS_NEAR_ME = 21;
	public static final int RESULT_DOC_DOWNLOAD = 22;

	/* FRAGMENT CODES */
	public static final String FRAGMENT_KEY_CODE = "fragment_code";
	public static final int FRAGMENT_INVALID = -1;
	public static final int FRAGMENT_USER_DETAILS = 0;
	public static final int FRAGMENT_DIARY = 1;
	public static final int FRAGMENT_INNER_CIRCLE = 2;
	public static final int FRAGMENT_INTERESTS = 3;
	public static final int FRAGMENT_INTERESTS_NEW = 4;
	public static final int FRAGMENT_PROFILE_CARD = 5;
	public static final int FRAGMENT_INTEREST_DETAILS = 6;
	public static final int FRAGMENT_INTEREST_SIMILAR = 7;
	public static final int FRAGMENT_INTEREST_FOLLOWERS = 8;

	public static final int FRAGMENT_WISH_NAME = 9;
	public static final int FRAGMENT_WISH_PREVIEW = 10;

	public static final int FRAGMENT_SETTINGS_ACCOUNT = 11;
	public static final int FRAGMENT_SETTINGS_INNER_CIRCLE = 12;
	public static final int FRAGMENT_SETTINGS_FOLDERS = 13;
	public static final int FRAGMENT_SETTINGS_PRIVACY = 14;
	public static final int FRAGMENT_SETTINGS_SECURITY = 15;
	public static final int FRAGMENT_SETTINGS_PAYMENT = 16;
	public static final int FRAGMENT_SETTINGS_HELP = 17;

	public static final int FRAGMENT_GLOBAL_SEARCH = 18;
	public static final int FRAGMENT_GLOBAL_SEARCH_TIMELINE = 19;
	public static final int FRAGMENT_GLOBAL_SEARCH_USERS_INTERESTS = 20;

	public static final int FRAGMENT_SETTINGS_MAIN = 21;
	public static final int FRAGMENT_WISHES_MAIN = 22;

	public static final int FRAGMENT_CHAT_MESSAGES = 23;
	public static final int FRAGMENT_CHAT_ROOMS = 24;
	public static final int FRAGMENT_CHAT_CREATION = 25;

	public static final int FRAGMENT_NOTIFICATIONS = 26;


	/* CREATE WISH KNOWN KEYS */
	public static final String KEY_ITEM_ROOT = "root";
	public static final String KEY_ITEM_NEXT = "";
	public static final String KEY_ITEM_PERSONAL_EVENTS = "PERSONAL_LIFE_EVENTS_ID";
	public static final String KEY_ITEM_WISHES_ACTION = "WISHES_ACTIONS";
	public static final String KEY_ITEM_REPETITION = "REPETITION_ID";
	public static final String KEY_ITEM_POST_OPTIONS = "POST_ON_HIGHLANDERS_OPTIONS";
	public static final String KEY_ITEM_SEND_POST_OPTIONS = "SEND_A_POST_OPTIONS";
	public static final String KEY_ITEM_RECIPIENTS = "RECIPIENTS_ID_POST";
	public static final String KEY_ITEM_SPECIFIC_DATE = "SPECIFIC_DATE";
	public static final String KEY_ITEM_SEARCH_IC = "SEARCH_INNERCIRCLE_SOMETHING_NEW";
	public static final String KEY_ITEM_SEARCH_INTERESTS = "SEARCH_INTERESTS_SOMETHING_NEW";

	public static final String KEY_ITEM_POST_TEXT = "POST_SELECTOR0";
	public static final String KEY_ITEM_POSTS_PIC = "POST_SELECTOR1";
	public static final String KEY_ITEM_POSTS_MOVIES = "POST_SELECTOR2";
	public static final String KEY_ITEM_POSTS_AUDIO = "POST_SELECTOR3";
	public static final String KEY_ITEM_POSTS_AUDIO_ONE = "POST_SELECTOR99";

	public static final String KEY_ACTION_ADD_NEW_EVENT = "ADD_NEW_LIFE_EVENT";
	public static final String KEY_ACTION_PHONE_CALL = "VERIFY_NUMBER_POPUP";

	public static final String KEY_NAV_ID_SPECIFIC_DATE = "DATE_SELECTOR_SPECIFIC";
	public static final String KEY_NAV_ID_CALENDAR = "DATE_SELECTOR_CALENDAR";
	public static final String KEY_NAV_ID_REPETITIONS = "UI_SWITCH";
	public static final String KEY_NAV_ID_BASE_LIST = "LIST_ITEMS";
	public static final String KEY_NAV_ID_LIST_AVATAR = "LIST_ITEMS_WITH_AVATAR";
	public static final String KEY_NAV_ID_SEARCH_LIST = "SEARCH_LIST";
	public static final String KEY_NAV_ID_LIST_TILES = "TILES_LIST";
	public static final String KEY_NAV_ID_WISH_MY_DIARY = "WISHES_DIARY_UI";
	public static final String KEY_NAV_ID_EMAIL = "EMAIL_UI";
	public static final String KEY_NAV_ID_FILTER_CONTAINS = "FILTER_CONTAINS";
	public static final String KEY_NAV_ID_COVER = "CHOOSE_COVER_UI";
	public static final String KEY_NAV_ID_RECIPIENT_IC = "SEARCH_INNERCIRCLE_RECIPIENT_UI";
	public static final String KEY_NAV_ID_SAVE_WISH = "SAVE_WISH_UI";
	public static final String KEY_NAV_ID_CREATE_POST = "CREATE_NEW_POST_UI";
	public static final String KEY_NAV_ID_PHONE_VERIFY = "VERIFY_CODE_UI";
	public static final String KEY_NAV_ID_RECORD_AUDIO = "RECORD_AUDIO_UI";


	/* HELP KNOWN KEYS*/
	public static final String KEY_NAV_ID_HELP_YES_NO = "YES_NO_UI";
	public static final String KEY_NAV_ID_BASE_LIST_SETTINGS = "LIST_ITEMS_SETTINGS";
	public static final String KEY_NAV_ID_HELP_USER_GUIDE = "USER_GUIDE_UI";
	public static final String KEY_NAV_ID_HELP_CONTACT = "CONTACT_UI";


	/* NOTIFICATIONS */
	public static final String KEY_NOTIFICATION_RECEIVED = "notification_received";
	public static final String CODE_NOTIFICATION_GENERIC = "notification_generic";
	public static final String CODE_NOTIFICATION_CHAT_UNSENT_MESSAGES = "notification_chat_unsent_messages";
	public static final String CODE_NOTIFICATION_CHAT = "notification_chat";
	public static final String CODE_NOTIFICATION_CALLS = "notification_calls";

	public static final int NOTIFICATION_GENERIC_ID = 100;
	public static final int NOTIFICATION_CALLS_ID = 101;
	public static final int NOTIFICATION_CHAT_UNSENT_ID = 102;
	public static final int NOTIFICATION_CHAT_ID = 103;
	public static final int NOTIFICATION_LOCATION_ID = 104;

	// no bundling for now
	public static final String NOTIFICATION_CHAT_GROUP = "group_chat_messages";


	public static final String NOT_DEFINED_YET = "not defined yet";

	public static final long HL_MINIMUM_REDEMPTION = 50;
}
