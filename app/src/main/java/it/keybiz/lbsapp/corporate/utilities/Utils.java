package it.keybiz.lbsapp.corporate.utilities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.FontRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.realTime.RealTimeCommunicationHelperKotlin;
import it.keybiz.lbsapp.corporate.fcm.FCMNotNeededException;
import it.keybiz.lbsapp.corporate.fcm.SendFCMTokenService;
import it.keybiz.lbsapp.corporate.features.login.LoginActivity;
import it.keybiz.lbsapp.corporate.features.profile.SelectIdentityActivity;
import it.keybiz.lbsapp.corporate.features.viewers.WebViewActivity;
import it.keybiz.lbsapp.corporate.features.viewers.WebViewActivityDocuments;
import it.keybiz.lbsapp.corporate.models.HLInterests;
import it.keybiz.lbsapp.corporate.models.HLNotifications;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.widgets.HLCustomTypefaceSpan;


/**
 * @author mbaldrighi on 9/18/2017.
 */
public class Utils {

	/**
	 * More easily accessible method to get whether the provided String is null of 0-length.
	 * @param s the provided String.
	 * @return Whether the provided String is null or 0-length.
	 */
	public static boolean isStringValid(String s) {
		return areStringsValid(s);
	}

	/**
	 * More easily accessible method to get whether AT LEAST ONE String within a provided set is null
	 * or 0-length.
	 * @param arr the provided String set.
	 * @return Whether the provided String set is null or 0-length.
	 */
	public static boolean areStringsValid(String ... arr) {
		for (String s : arr) {
			if (TextUtils.isEmpty(s))
				return false;
		}

		return true;
	}

	/**
	 * This method converts dp units to pixels.
	 *
	 * @param unit      the wanted unit.
	 * @param value        the wanted value units.
	 * @param resources the application's {@code Resources}
	 * @return The corresponding pixel value.
	 */
	public static int convertToPx(int unit, float value, Resources resources) {
		float px = TypedValue.applyDimension(unit, value, resources.getDisplayMetrics());
		return (int) px;
	}

	/**
	 * This method converts dp units to pixels.
	 *
	 * @param dp        the wanted dp units.
	 * @param resources the application's {@code Resources}
	 * @return The corresponding pixel value.
	 */
	public static int dpToPx(float dp, Resources resources){
		return convertToPx(TypedValue.COMPLEX_UNIT_DIP, dp, resources);
	}

	/**
	 * This method converts dp units to pixels.
	 *
	 * @param sp        the wanted dp units.
	 * @param resources the application's {@code Resources}
	 * @return The corresponding pixel value.
	 */
	public static int spToPx(float sp, Resources resources){
		return convertToPx(TypedValue.COMPLEX_UNIT_SP, sp, resources);
	}

	/**
	 * This method converts dp units to pixels.
	 * {@link Resources#getDimension} already returns the value including {@link android.util.DisplayMetrics#density}
	 *
	 * @param dimen     the    the wanted dp units.
	 * @param resources the application's {@code Resources}
	 * @return The corresponding pixel value.
	 */
	public static int dpToPx(@DimenRes int dimen, Resources resources) {
		return ((int) resources.getDimension(dimen));
	}

	public static int pxToDp(float px) {
		DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		float dp = px / (metrics.densityDpi / 160f);
		return Math.round(dp);
	}

	/**
	 * Adds pre-existent HTML formatting to text.
	 * @param seq the String to be HTML formatted.
	 * @return The {@link Spanned} object trasformed from the original String.
	 */
	public static Spanned getFormattedHtml(String seq) {
		if (isStringValid(seq)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				return Html.fromHtml(seq, Html.FROM_HTML_MODE_LEGACY);
			else
				return Html.fromHtml(seq);
		}
		return null;
	}

	/**
	 * Adds pre-existent HTML formatting to text.
	 * @param res the application {@link Resources}.
	 * @param resId the {@link StringRes} int pointing to the String to be HTML formatted.
	 * @return The {@link Spanned} object trasformed from the original String.
	 */
	public static Spanned getFormattedHtml(Resources res, @StringRes int resId) {
		String seq = res.getString(resId);
		if (isStringValid(seq)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				return Html.fromHtml(seq, Html.FROM_HTML_MODE_LEGACY);
			else
				return Html.fromHtml(seq);
		}
		return null;
	}

	/**
	 * This method applies the custom font to any provided TextView.
	 *
	 * @param tv the provided TextView object.
	 * @param fontResId the provided font as StringRes.
	 */
	public static void applyFontToTextView(TextView tv, @StringRes int fontResId) {
		if (tv != null && isContextValid(tv.getContext())) {
			String path = "fonts/" + tv.getContext().getString(fontResId);
			Typeface font = Typeface.createFromAsset(tv.getContext().getAssets(), path);
			SpannableString title = new SpannableString(tv.getText());
			title.setSpan(new HLCustomTypefaceSpan("", font), 0, title.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
			tv.setText(title);
		}
	}

	/**
	 * This method applies the wanted @FontRes to any provided TextView.
	 *
	 * @param tv the provided TextView object.
	 * @param fontResId the provided font as @FontRes.
	 */
	public static void applyFontResToTextView(TextView tv, @FontRes int fontResId) {
		if (tv != null && isContextValid(tv.getContext())) {
			tv.setTypeface(ResourcesCompat.getFont(tv.getContext(), fontResId));
		}
	}

	/**
	 * This method applies the custom font to any provided TextView.
	 *
	 * @param context the application's/activity's context.
	 * @param mi      the provided MenuItem object.
	 */
	public static void applyRegularFontToMenuItem(Context context, MenuItem mi) {
		applyFontToMenuItem(context, mi, R.string.fontRegular);
	}

	/**
	 * This method applies the custom font to any provided TextView.
	 *
	 * @param context the application's/activity's context.
	 * @param mi      the provided MenuItem object.
	 */
	public static void applyFontToMenuItem(Context context, MenuItem mi, @StringRes int fontRes) {
		if (isContextValid(context)) {
			String path = "fonts/" + context.getString(fontRes);
			Typeface font = Typeface.createFromAsset(context.getAssets(), path);
			SpannableString title = new SpannableString(mi.getTitle());
			title.setSpan(new HLCustomTypefaceSpan("", font), 0, title.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
			mi.setTitle(title);
		}
	}

	/**
	 * Applies a provided series of styles to a provided {@link TextView}.
	 *
	 * @param textView the provided View to which apply the style.
	 * @param string the {@link String} to be associated with the View.
	 * @param styles the provided array of {@link CharacterStyle} to be applied.
	 */
	public static void applySpansToTextView(TextView textView, String string, CharacterStyle...styles) {
		if (textView != null && isContextValid(textView.getContext())) {
			if (isStringValid(string) && styles != null && styles.length > 0) {
				SpannableStringBuilder spannableString = new SpannableStringBuilder(string);
				for (CharacterStyle style : styles) {
					spannableString.setSpan(style, 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				}

				textView.setText(spannableString);
			}
		}
	}

	/**
	 * Applies a provided series of styles to a provided {@link TextView}.
	 *
	 * @param textView the provided View to which apply the style.
	 * @param stringResId the resource ID of the {@link String} to be associated with the View.
	 * @param styles the provided array of {@link CharacterStyle} to be applied.
	 */
	public static void applySpansToTextView(TextView textView, @StringRes int stringResId, CharacterStyle...styles) {
		if (textView != null && isContextValid(textView.getContext())) {
			applySpansToTextView(textView, textView.getContext().getString(stringResId), styles);
		}
	}


	/**
	 * Checks if the provided {@link String} matches a {@link Pattern} created with a provided regEx string.
	 * @param string the provided string to be checked.
	 * @param regex the provided pattern.
	 * @return True if string matches the pattern, false otherwise.
	 */
	public static boolean doesStringMatchPattern(String string, String regex) {
		if (!areStringsValid(string, regex))
			return false;

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(string);
		return matcher.matches();
	}

	/**
	 * Gets {@link Matcher} of a matching check with {@link Pattern}.
	 * @param string the provided string to be checked.
	 * @param regex the provided pattern.
	 * @return True if string matches the pattern, false otherwise.
	 */
	public static Matcher getMatcherOfMatching(String string, String regex) {
		if (!areStringsValid(string, regex))
			return null;

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(string);
		return matcher;
	}

	/**
	 * Checks if the provided email string matches with the {@link Constants#REGEX_EMAIL} pattern.
	 * @param email the provided email string.
	 * @return True if the string matches the pattern, false otherwise.
	 */
	public static boolean isEmailValid(String email) {
		return doesStringMatchPattern(email, Constants.REGEX_EMAIL);
	}

	public static boolean isPasswordValid(String password) {
		return Utils.doesStringMatchPattern(password, Constants.REGEX_PASSWORD);
	}


	/**
	 * Returns the wanted color int value.
	 * @param context the application's/activity's {@link Context}.
	 * @param colorId the provided {@link ColorRes}.
	 * @return The wanted color int value.
	 */
	public static int getColor(Context context, @ColorRes int colorId) {
		if (context != null)
			return ContextCompat.getColor(context, colorId);
		return android.R.color.transparent;
	}

	/**
	 * Returns the wanted color int value.
	 * @param resources the application's {@link Resources}.
	 * @param colorId the provided {@link ColorRes}.
	 * @return The wanted color int value.
	 */
	public static int getColor(Resources resources, @ColorRes int colorId) {
		if (resources != null)
			return ResourcesCompat.getColor(resources, colorId, null);
		return android.R.color.transparent;
	}

	/**
	 * Closes programmatically the soft input tools.
	 * @param view the {@link View} to which the soft input is anchored.
	 */
	public static void closeKeyboard(View view) {
		if (view != null) {
			InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
		}
	}

	/**
	 * Closes programmatically the soft input tools.
	 */
	public static void closeKeyboard(Activity activity) {
		if (Utils.isContextValid(activity)) {
			activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		}
	}


	/**
	 * Opens programmatically the soft input tools.
	 * @param view the {@link View} to which the soft input is anchored.
	 */
	public static void openKeyboard(@NonNull View view) {
		if(view.requestFocus()){
			InputMethodManager imm =(InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null)
				imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
		}
	}

	/**
	 * Closes programmatically the soft input tools.
	 */
	public static void openKeyboard(Activity activity) {
		if (Utils.isContextValid(activity)) {
			activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
	}

	/**
	 * Shows a {@link Toast} with top gravity, and message from a {@link String}.
	 * @param context the application's/activity's Context instance.
	 * @param msg the provided {@link String}.
	 */
	public static void showTopToast(Context context, @NonNull String msg) {
		if (isContextValid(context)) {
			Toast t = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, Utils.dpToPx(20f, context.getResources()));
			t.show();
		}
	}

	/**
	 * Shows a {@link Toast} with top gravity, and message from a {@link StringRes} resource.
	 * @param context the application's/activity's Context instance.
	 * @param msg the provided {@link StringRes} resource.
	 */
	public static void showTopToast(Context context, @StringRes int msg) {
		showTopToast(context, context.getString(msg));
	}


	/**
	 * Splits a complete name into first and last name, based on the whitespace between them.
	 * @param completeName the provided String to be splitted.
	 * @return The user's first name.
	 */
	public static String getFirstNameForUI(String completeName) {
		String firstName = Utils.isStringValid(completeName) ? completeName : "";
		if (Utils.isStringValid(firstName)) {
			String[] parts = firstName.split("\\s");
			if (parts.length > 0)
				firstName = parts[0];
		}

		return firstName;
	}


	/**
	 * Formats any provided int count to the wanted String "divided" by a provided multiplier. <p>
	 * E.g. 1000 = 1K <p>
	 * @param res the application/activity's Context instance.
	 * @param count the provided int count.
	 * @return The wanted formatted String.
	 */
	public static String getReadableCount(Resources res, int count) {
		try {
			if (count > 0) {
				@StringRes int stringRes = 0;
				double dividingBy = 0;
				if (count >= 1E9) {
					stringRes = R.string.number_multiplier_b_string;
					dividingBy = 1E9;
				}
				else if (count >= 1E6 && count < 1E9) {
					stringRes = R.string.number_multiplier_m_string;
					dividingBy = 1E6;
				}
				else if (count >= 1000 && count < 1000000) {
					stringRes = R.string.number_multiplier_k_string;
					dividingBy = 1E3;
				}

				if (stringRes == 0)
					return String.valueOf(count);
				else {
					double divided = count / dividingBy;

					NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
					DecimalFormat df = ((DecimalFormat) nf);
					df.setRoundingMode(RoundingMode.FLOOR);
					df.setMinimumFractionDigits(0);
					df.setMaximumFractionDigits(1);

					String s = df.format(divided);
					return String.format(
							Locale.getDefault(),
							res.getString(stringRes),
							s
					);
				}
			}
		} catch (ArithmeticException e) {
			LogUtils.e("getReadableCount() FAILED", e.getMessage(), e);
		}

		return String.valueOf(0);
	}


	public static String formatStringFromLanguages(List<String> languages) {
		if (languages != null && !languages.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < languages.size() - 1; i++) {
				sb.append(languages.get(i)).append(", ");
			}
			if (languages.size() > 1)
				sb.append(languages.get(languages.size() - 1));

			return sb.toString();
		}

		return "";
	}


	public static Number parseNumberWithCommas(String numberToParse) {
		NumberFormat format = NumberFormat.getInstance(Locale.US);
		Number number = 0;
		try {
			number = format.parse(numberToParse);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return number;
	}

	public static String formatNumberWithCommas(long numberToFormat) {
		NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
		return format.format(numberToFormat);
	}

	public static int calculateVisibleTextLines(TextView textView) {
		int result = 0;
		if (textView != null) {
			int height = textView.getHeight();
			int lineHeight = textView.getLineHeight();
			if (lineHeight > 0) {
				result = height / lineHeight;

//				LogUtils.d("UTILS: TEXT VISIBLE LINES", "" + height + " / " + lineHeight + " = " + result);
			}
		}
		return result;
	}



	/*
	 * BASE64 methods
	 */

	/**
	 * Method used to encode a Bitmap to a Base64 string.
	 *
	 * @param image the Bitmap image to be encoded.
	 * @return The encoded string.
	 */
	public static String encodeBitmapToBase64(Bitmap image) {
		if (image != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			byte[] b = baos.toByteArray();

			return Base64.encodeToString(b, Base64.DEFAULT);
		}

		return null;
	}

	/**
	 * Method used to decode a Bitmap to Base64 string to a Bitmap object.
	 *
	 * @param encodedString the String image to be decoded.
	 * @return The decoded Bitmap.
	 */
	public static Bitmap decodeBitmapFromBase64(String encodedString){
		Bitmap bitmap;

		if (encodedString != null && !encodedString.isEmpty()) {
			try {
				byte[] decodedByte = Base64.decode(encodedString, Base64.DEFAULT);
				bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
				return bitmap;
			} catch (Exception e) {
				LogUtils.e("BITMAP conversion to BASE64", e.getMessage(), e);
				return null;
			}
		}

		return null;
	}


	/*
	 * NAVIGATION
	 */
	public static void fireBrowserIntent(Context context, String url, String title) {
		fireBrowserIntentWithShare(context, url, title, null, false);
	}

	public static void fireBrowserIntentWithShare(Context context, String url, String title, String postOrMessageId, boolean fromChat) {
		if (isContextValid(context) && context instanceof Activity) {
			Intent intent = new Intent(context, WebViewActivity.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, url);
			intent.putExtra(Constants.EXTRA_PARAM_2, title);
			intent.putExtra(Constants.EXTRA_PARAM_3, postOrMessageId);
			intent.putExtra(Constants.EXTRA_PARAM_4, fromChat);

			context.startActivity(intent);
			((Activity) context).overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
		}
	}

	public static void fireBrowserIntentForDocs(Context context, String url, String title, @Nullable String messageID) {
		if (isContextValid(context) && context instanceof Activity) {
			Intent intent = new Intent(context, WebViewActivityDocuments.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, url);
			intent.putExtra(Constants.EXTRA_PARAM_2, title);
			intent.putExtra(Constants.EXTRA_PARAM_3, messageID);

			context.startActivity(intent);
			((Activity) context).overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
		}
	}

	public static void fireOpenEmailIntent(HLActivity context) {
		try {
//			Intent intent = new Intent(Intent.ACTION_VIEW);
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_APP_EMAIL);
			context.startActivity(Intent.createChooser(intent, context.getString(R.string.click_confirm_email_2)));
		} catch (Exception e) {
			e.printStackTrace();
			context.showAlert(R.string.no_email_apps);
			Intent redirection = new Intent(context, LoginActivity.class);
			redirection.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			redirection.putExtra(Constants.EXTRA_PARAM_1, true);
			context.startActivity(redirection);
		} finally {
			context.finish();
		}
	}

	public static void fireShareIntent(Context context, String content) {
		if (isContextValid(context)) {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, content);
			context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.action_share_title)));
		}
	}

	/*
	 * Connectivity
	 */
	public static boolean isDeviceConnected(Context context) {
		ConnectivityManager cm =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = null;
		if (cm != null) {
			activeNetwork = cm.getActiveNetworkInfo();
		}
		boolean connected = activeNetwork != null && activeNetwork.isConnected();

		if (!connected) {
			context.sendBroadcast(new Intent(Constants.BROADCAST_NO_CONNECTION));
			return false;
		} else return true;
	}

	public static boolean isContextValid(Context context) {
		boolean condition = context != null;
		if (context instanceof Activity) {
			condition = !((Activity) context).isFinishing() && !((Activity) context).isDestroyed();
		}

		return condition;
	}


	/*
	 * Time and Date
	 */
	/**
	 * Method used to convert any string date coming from the server to its millisecond value.
	 *
	 * @param date the string date coming from the server.
	 * @return The date's milliseconds {@code long} value.
	 * @throws ParseException if something goes wrong while parsing the date in question.
	 */
	public static long getDateMillisFromDB(String date) {
		Date newDate = getDateFromDB(date);
		return newDate != null ? newDate.getTime() : 0;
	}

	/**
	 * Method used to convert any string date coming from the server to a Java Date object.
	 *
	 * @param date the string date coming from the server.
	 * @return The wanted Date object.
	 */
	public static Date getDateFromDB(@Nullable String date) {
		if(!isStringValid(date))
			return null;

		if (date.contains("Z")) {   // it means it comes from server -> go on with the split
			String[] split = date.split("\\.");
			if (split.length == 2 && (split[1].length() > 4))
				date = split[0] + "." + split[1].substring(0, 3) + "Z";
		}

		try {
			SimpleDateFormat dateIn = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
			Date newDate = dateIn.parse(date.replaceAll("Z$", "+0000"));
			long millis = newDate.getTime();
			return newDate;
//			return new Date(newDate.getTime() + TimeZone.getDefault().getOffset(millis));
		} catch (ParseException ex) {
			return null;
		}
	}

	/**
	 * Method used to format any {@link Date} to be sent to the server.
	 *
	 * @param date the {@link Date} to be sent to the server.
	 * @return The wanted formatted date String.
	 */
	public static String formatDateForDB(Date date) {
		return formatDate(date, Constants.DATE_FORMAT);
	}

	/**
	 * Method used to format any date expressed in milliseconds to be sent to the server.
	 *
	 * @param millis the date to be sent to the server expressed in milliseconds.
	 * @return The wanted formatted date String.
	 */
	public static String formatDateForDB(long millis) {
		return formatDate(millis, Constants.DATE_FORMAT);
	}

	/**
	 * Method used to format any date expressed in milliseconds with a provided format.
	 *
	 * @param millis the date to be sent to the server expressed in milliseconds.
	 * @return The wanted formatted date String.
	 */
	public static String formatDate(long millis, String format) {
		if (!Utils.isStringValid(format))
			return "";

		SimpleDateFormat dateOut = new SimpleDateFormat(format, Locale.getDefault());
		return dateOut.format(new Date(millis));
	}

	/**
	 * Method used to format any {@link Date} to be sent to the server.
	 *
	 * @param date the {@link Date} to be sent to the server.
	 * @return The wanted formatted date String.
	 */
	public static String formatDate(Date date, String format) {
		if (date == null || !Utils.isStringValid(format))
			return "";

		SimpleDateFormat dateOut = new SimpleDateFormat(format, Locale.getDefault());
		return dateOut.format(date);
	}


	/**
	 * Method used to format any {@link Date} to be sent to the server.
	 *
	 * @param date the {@link Date} to be sent to the server.
	 * @return The wanted formatted date String.
	 */
	public static String formatDateWithTime(Context context, Date date, String dateFormat, boolean separator) {
		if (date == null || !Utils.isStringValid(dateFormat))
			return "";

		return formatDate(date, dateFormat) + (separator ? ", " : "") + formatTime(context, date);
	}

	/**
	 * Method used to format any {@link Date} to be sent to the server.
	 *
	 * @param date the {@link Date} to be sent to the server.
	 * @return The wanted formatted date String.
	 */
	public static String formatTime(Context context, Date date) {
		if (date == null || !isContextValid(context))
			return "";

		return DateFormat.getTimeFormat(context).format(date);
	}


	/*
	 * Permissions
	 */
	/**
	 * This method is used to ask for permissions on SDK >= 23 (Marshmallow).
	 *
	 * @param context     the application's/activity's context.
	 * @param permission  the String referring to the permission requested.
	 * @param msgResId    the resId of the message to be shown in the dialog.
	 * @param permReqCode the int used to catch the user's response.
	 * @return True if the permission has to be requested, false otherwise.
	 */
	public static boolean askForPermissions(final Activity context, final String permission,
	                                        int titleResId, int msgResId, final int permReqCode) {
		if (!hasApplicationPermission(context, permission)) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
				if (titleResId != 0 && msgResId != 0) {
					new AlertDialog.Builder(context)
							.setTitle(titleResId)
							.setMessage(msgResId)
//							.setDismiss(true)
							.setCancelable(true)
							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									askRequiredPermission(context, permission, permReqCode);
								}
							})
							.show();
					return true;
				}
			}
			askRequiredPermission(context, permission, permReqCode);
			return true;
		}
		return false;
	}

	/**
	 * This method is used to check whether the provided permission has already been granted or not.
	 *
	 * @param context    the application's/activity's context.
	 * @param permission the String pointing to the permission to be checked.
	 * @return True if the provided permission has already been granted, false otherwise.
	 */
	public static boolean hasApplicationPermission(Context context, String permission) {
		return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Asks for a set of permissions required by the application
	 * @param context     the application's/activity's context.
	 * @param permissions the String array pointing to the permission set to be asked.
	 * @param code        the int used to catch the user's response.
	 */
	public static void askRequiredPermission(Activity context, String[] permissions, final int code){
		ActivityCompat.requestPermissions(context, permissions, code);
	}

	/**
	 * Asks for a permission required by the application
	 * @param context    the application's/activity's context.
	 * @param permission the String pointing to the permission to be asked.
	 * @param code       the int used to catch the user's response.
	 */
	public static void askRequiredPermission(Activity context, String permission, final int code){
		askRequiredPermission(context, new String[]{permission}, code);
	}

	/**
	 * Asks for a set of permissions required by the application
	 * @param fragment   the {@link Fragment} that's expecting the result.
	 * @param permissions the String array pointing to the permission set to be asked.
	 * @param code        the int used to catch the user's response.
	 */
	public static void askRequiredPermissionForFragment(Fragment fragment, String[] permissions, final int code){
		fragment.requestPermissions(permissions, code);
	}

	/**
	 * Asks for a permission required by the application
	 * @param fragment   the {@link Fragment} that's expecting the result.
	 * @param permission the String pointing to the permission to be asked.
	 * @param code       the int used to catch the user's response.
	 */
	public static void askRequiredPermissionForFragment(Fragment fragment, String permission, final int code){
		askRequiredPermissionForFragment(fragment, new String[]{permission}, code);
	}


	/*
	 * Android API Version
	 */
	public static boolean hasKitKat() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}

	public static boolean hasKitKatWatch() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH;
	}

	public static boolean hasLollipop() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}

	public static boolean hasMarshmallow() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
	}

	public static boolean hasNougat() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
	}

	public static boolean hasOreo() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
	}

	public static boolean hasPie() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
	}


	/*
	 * SYSTEM
	 */
	/**
	 * Checks if the connection type of the device is {@link ConnectivityManager#TYPE_WIFI} or
	 * {@link ConnectivityManager#TYPE_WIMAX}.
	 * @param context the application's/activity's {@link Context} instance.
	 * @return True if the connection type is WIKI or WIMAX, false otherwise.
	 */
	public static boolean isConnectionWiFi(Context context) {
		if (isContextValid(context)) {
			ConnectivityManager cm =
					(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo activeNetwork = null;
			if (cm != null) {
				activeNetwork = cm.getActiveNetworkInfo();
			}
			boolean isConnected = activeNetwork != null &&
					activeNetwork.isConnectedOrConnecting();

			if (isConnected) {
				return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ||
						activeNetwork.getType() == ConnectivityManager.TYPE_WIMAX;
			}
		}

		return false;
	}

	/**
	 * Checks if device has a camera hardware.
	 * @param context the application/activity's Context
	 * @return True if device has camera hardware, false otherwise
	 */
	public static boolean hasDeviceCamera(Context context) {
		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}


	/*
	 * PLAY SERVICES & FIREBASE
	 */
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	/**
	 * Checks the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	public static boolean checkPlayServices(Context context) {
		GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();

		int result = googleAPI.isGooglePlayServicesAvailable(context);
		if (result != ConnectionResult.SUCCESS) {
			if (context instanceof Activity) {
				Activity activity = ((Activity) context);
				if (googleAPI.isUserResolvableError(result))
					googleAPI.getErrorDialog(activity, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
				else {
					GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(activity);
					activity.finish();
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * Retrieves the refreshed FCM token from {@link FirebaseInstanceId}.
	 * @param context the application's/activity's Context instance.
	 * @return The refreshed token upon successful operation, {@code null} otherwise.
	 */
	public static void getFcmToken(Context context, SendFCMTokenService service) {
		if (Utils.checkPlayServices(context)) {
			FirebaseInstanceId
					.getInstance()
					.getInstanceId()
					.addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
						@Override
						public void onComplete(@NonNull Task<InstanceIdResult> task) {
							if (task.isSuccessful()) {
								// Get updated InstanceID token.
								String refreshedToken = task.getResult() != null ? task.getResult().getToken() : null;
//								LogUtils.i(context.getClass().getCanonicalName(), "Refreshed FCM token: " + refreshedToken);
								LogUtils.d(context.getClass().getCanonicalName(), "Refreshed FCM token: " + refreshedToken);

								String fcm = SharedPrefsUtils.retrieveFCMTokenId(context);
								if (!LBSLinkApp.fcmTokenSent || (Utils.isStringValid(refreshedToken) && !fcm.equals(refreshedToken))) {
									SharedPrefsUtils.storeFCMTokenId(context, refreshedToken);

									service.sendToken(refreshedToken);
								}
								else
									LogUtils.d("FCM ERROR", new FCMNotNeededException());
							}
						}
					});
		}
	}


	/*
	 * FABRIC - CRASHLYTICS
	 */
	public static void logUserForCrashlytics(HLUser user) {
		if (user != null) {
			Crashlytics.setUserIdentifier(user.getId());
			Crashlytics.setUserEmail(user.getEmail());
			Crashlytics.setUserName(user.getCompleteName());
		}
	}


	/*
	 * COMMON UI USAGES
	 */
	public static void openIdentitySelection(Activity context, Fragment fragment) {
		if (isContextValid(context)) {
			if (fragment != null)
				fragment.startActivityForResult(new Intent(context, SelectIdentityActivity.class), Constants.RESULT_SELECT_IDENTITY);
			else
				context.startActivityForResult(new Intent(context, SelectIdentityActivity.class), Constants.RESULT_SELECT_IDENTITY);
			context.overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
		}
	}

	public static SwipeRefreshLayout getGenericSwipeLayout(@NonNull View root, @Nullable SwipeRefreshLayout.OnRefreshListener listener) {
		SwipeRefreshLayout srl = root.findViewById(R.id.swipe_refresh_layout);
		srl.setDistanceToTriggerSync(200);
		srl.setColorSchemeResources(R.color.colorAccent);
		if (listener != null)
			srl.setOnRefreshListener(listener);

		return srl;
	}

	public static void setRefreshingForSwipeLayout(SwipeRefreshLayout swipeRefreshLayout, boolean refreshing) {
		if (swipeRefreshLayout != null)
			swipeRefreshLayout.setRefreshing(refreshing);
	}


	/**
	 * Common method used to log out from Highlanders app.
	 *
	 * @param activity the {@link Activity} from which the mthod is called.
	 * @param dialog the dialog used to introduce the log out ops.
	 */
	public static void logOut(Activity activity, @Nullable MaterialDialog dialog) {
		SharedPrefsUtils.clearPreferences(activity);

		// clean singletons' collections
		HLNotifications.getInstance().clearNotifications();
		HLInterests.getInstance().clearInterests();
		HLPosts.getInstance().clearPosts();

		RealTimeCommunicationHelperKotlin.Companion.getInstance(activity).closeRealmInstance();
		RealmUtils.closeAllRealms();
		Realm.deleteRealm(LBSLinkApp.realmConfig);
		RealTimeCommunicationHelperKotlin.Companion.getInstance(activity).restoreRealmInstance();

		Intent intent = new Intent(activity, LoginActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		activity.startActivity(intent);

		activity.finish();
		activity.overridePendingTransition(R.anim.no_animation, R.anim.slide_out_top);
		DialogUtils.closeDialog(dialog);
	}


	public static boolean checkAndOpenLogin(Activity activity, HLUser user, int page) {
		if ((user == null || !user.isValid()) && isContextValid(activity)) {
			Intent intent = new Intent(activity, LoginActivity.class);
			intent.putExtra(Constants.EXTRA_PARAM_2, page);
			activity.startActivity(intent);
			activity.overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
			return true;
		}

		return false;
	}



	public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				LogUtils.i ("CHECK SERVICE", serviceClass.getCanonicalName() + " is running = true");
				return true;
			}
		}
		LogUtils.i ("CHECK SERVICE", serviceClass.getCanonicalName() + " is running = false");
		return false;
	}



}
