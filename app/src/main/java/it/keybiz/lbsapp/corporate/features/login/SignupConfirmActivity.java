/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.login;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.OnApplicationContextNeeded;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListenerWithErrorDescription;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.FieldValidityWatcher;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A confirmation screen that offers allows users to enter also their Hcode.
 *
 * @author mbaldrighi on 4/19/2017
 */
public class SignupConfirmActivity extends HLActivity implements View.OnClickListener,
		OnServerMessageReceivedListenerWithErrorDescription, OnMissingConnectionListener {

	public static final String LOG_TAG = SignupConfirmActivity.class.getCanonicalName();

	// UI references.
	private TextView mEmailViewSUp, welcomeTitle;
	private EditText mPasswordViewSUp;
	private View mainButton;

	private View hInterest, progressInterest;
	private ImageView picture;
	private TextView name, errorInterest;

	private Date selectedBDate;
	private LoginActivity.Gender gender;
	private String mFirstName, mLastName, mEmail, mPassword;

	private String[] mHCode = new String[5];

	private boolean serverCalledForSignup = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signup_confirm);
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.generic_progress_indicator);

		manageIntent();

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				serverCalledForSignup = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_1, false);

			if (serverCalledForSignup) {
				finish();
				return;
			}
		}

		// Set up the sign up form.
		mEmailViewSUp = findViewById(R.id.email_confirm);
		welcomeTitle = findViewById(R.id.welcome);

		View pwdSU = findViewById(R.id.password_signup_confirm);
		mPasswordViewSUp = pwdSU.findViewById(R.id.edit_text);
		mPasswordViewSUp.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		mPasswordViewSUp.setImeActionLabel(getString(R.string.action_next), EditorInfo.IME_ACTION_NEXT);
		mPasswordViewSUp.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordViewSUp.setHint(R.string.prompt_password_confirm);
		mPasswordViewSUp.addTextChangedListener(new FieldValidityWatcher(R.id.password_signup_confirm, mPasswordViewSUp));
		Utils.applyFontToTextView(mPasswordViewSUp, R.string.fontRegular);
		mPasswordViewSUp.setTransformationMethod(new PasswordTransformationMethod());

		mainButton = findViewById(R.id.main_button);
		mainButton.setOnClickListener(this);
		findViewById(R.id.back_arrow).setOnClickListener(this);

		EditText mCode1 = findViewById(R.id.hcode_1);
		EditText mCode2 = findViewById(R.id.hcode_2);
		EditText mCode3 = findViewById(R.id.hcode_3);
		EditText mCode4 = findViewById(R.id.hcode_4);
		EditText mCode5 = findViewById(R.id.hcode_5);

		mPasswordViewSUp.setNextFocusDownId(R.id.hcode_1);
		mCode1.setNextFocusRightId(R.id.hcode_2);
		mCode1.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		mCode1.setImeActionLabel(getString(R.string.action_next), EditorInfo.IME_ACTION_NEXT);
		mCode1.addTextChangedListener(new HCodeTextWatcher(mCode1, mCode2));
		mCode1.setOnFocusChangeListener(new HcodeErasingTextFocus());
		mCode1.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
		mCode2.setNextFocusRightId(R.id.hcode_3);
		mCode2.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		mCode2.setImeActionLabel(getString(R.string.action_next), EditorInfo.IME_ACTION_NEXT);
		mCode2.addTextChangedListener(new HCodeTextWatcher(mCode2, mCode3));
		mCode2.setOnFocusChangeListener(new HcodeErasingTextFocus());
		mCode2.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
		mCode3.setNextFocusRightId(R.id.hcode_4);
		mCode3.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		mCode3.setImeActionLabel(getString(R.string.action_next), EditorInfo.IME_ACTION_NEXT);
		mCode3.addTextChangedListener(new HCodeTextWatcher(mCode3, mCode4));
		mCode3.setOnFocusChangeListener(new HcodeErasingTextFocus());
		mCode3.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
		mCode4.setNextFocusRightId(R.id.hcode_5);
		mCode4.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		mCode4.setImeActionLabel(getString(R.string.action_next), EditorInfo.IME_ACTION_NEXT);
		mCode4.addTextChangedListener(new HCodeTextWatcher(mCode4, mCode5));
		mCode4.setOnFocusChangeListener(new HcodeErasingTextFocus());
		mCode4.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
		mCode5.setImeOptions(EditorInfo.IME_ACTION_DONE);
		mCode5.setImeActionLabel(getString(R.string.go), EditorInfo.IME_ACTION_DONE);
		mCode5.addTextChangedListener(new HCodeTextWatcher(mCode5, null));
		mCode5.setOnFocusChangeListener(new HcodeErasingTextFocus());
		mCode5.setFilters(new InputFilter[] {new InputFilter.AllCaps()});

		hInterest = findViewById(R.id.hcode_interest);
		picture = hInterest.findViewById(R.id.profile_picture);
		name = hInterest.findViewById(R.id.name);

		errorInterest = findViewById(R.id.error_interest);
		progressInterest = findViewById(R.id.progress_signup_confirm);
	}


	@Override
	protected void onStart() {
		super.onStart();

		if (!serverCalledForSignup)
			configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.LOGIN_SIGNUP_2);

		if (!serverCalledForSignup) {

			welcomeTitle.requestFocus();

			welcomeTitle.setText(getString(
					gender == LoginActivity.Gender.NONE ?
							R.string.signup_welcome : (
									gender == LoginActivity.Gender.MALE ?
											R.string.signup_welcome_m : R.string.signup_welcome_f
					),
					mFirstName)
			);
			mEmailViewSUp.setText(mEmail);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Utils.closeKeyboard(mPasswordViewSUp);
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();

		switch (id) {
			case R.id.main_button:
				try {
					mainButton.setEnabled(false);
					attemptRegistration();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;

			case R.id.back_arrow:
				finish();
				overridePendingTransition(R.anim.no_animation, R.anim.slide_out_right);
				break;

		}
	}


	@Override
	protected void manageIntent() {

		Intent intent = getIntent();
		if (intent != null) {
			mFirstName = intent.getStringExtra(Constants.EXTRA_PARAM_1);
			mLastName = intent.getStringExtra(Constants.EXTRA_PARAM_2);
			selectedBDate = (Date) intent.getSerializableExtra(Constants.EXTRA_PARAM_3);
			gender = (LoginActivity.Gender) intent.getSerializableExtra(Constants.EXTRA_PARAM_4);
			mEmail = intent.getStringExtra(Constants.EXTRA_PARAM_5);
			mPassword = intent.getStringExtra(Constants.EXTRA_PARAM_6);
		}

	}


	private void callHCodeVerification() {

		Object[] results = null;
		try {
			results = HLServerCalls.HCodeVerification(convertArrayToHcode());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance((OnApplicationContextNeeded) getApplication())
				.handleCallResult(this, this, results);
	}


	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private void attemptRegistration() throws JSONException {
		boolean cancel = false;
		View focusView = null;

		String email, password;
		@StringRes int error = R.string.error_unknown;


		// Reset errors.
		mEmailViewSUp.setError(null);
		mPasswordViewSUp.setError(null);

		// Store values at the time of the login attempt.
		email = mEmailViewSUp.getText().toString();
		password = mPasswordViewSUp.getText().toString();

		if (!Utils.isStringValid(password)) {
			cancel = true;
			error = R.string.error_all_fields_required;
		}
		else if (!Utils.isPasswordValid(password)) {
//				mPasswordViewSUp.setError(getString(R.string.error_invalid_password));
//				focusView = mPasswordViewSUp;
			cancel = true;
			error = R.string.error_invalid_password;
		}

		else if (!password.equals(mPassword)) {
			cancel = true;
			error = R.string.error_incorrect_password;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(email)) {
//				mEmailViewSUp.setError(getString(R.string.error_field_required));
//				focusView = mEmailViewSUp;
			cancel = true;
			error = R.string.error_all_fields_required;
		} else if (!Utils.isEmailValid(email)) {
//				mEmailViewSUp.setError(getString(R.string.error_invalid_email));
//				focusView = mEmailViewSUp;
			cancel = true;
			error = R.string.error_invalid_email;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			if (focusView != null)
				focusView.requestFocus();

			showAlert(error);

			mainButton.setEnabled(true);
		}
		else {
			openProgress();

			Object[] results = HLServerCalls.signUp(
					this,
					mFirstName,
					mLastName,
					email,
					password,
					selectedBDate,
					gender,
					convertArrayToHcode()
			);

			HLRequestTracker.getInstance((OnApplicationContextNeeded) getApplication())
					.handleCallResult(this, this, results);
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		closeProgress();

		@StringRes int message;
		switch (operationId) {
			case Constants.SERVER_OP_SIGNUP:
//				message = R.string.click_confirm_email;
//				showAlert(message);

				final MaterialDialog dialog = new MaterialDialog.Builder(this)
						.content(R.string.signup_email_redirect)
						.cancelable(true)
						.show();


				serverCalledForSignup = true;

				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						Utils.fireOpenEmailIntent(SignupConfirmActivity.this);
						DialogUtils.closeDialog(dialog);
					}
				}, 3000);

				break;

			case Constants.SERVER_OP_HCODE_VERIFY:

				if (responseObject == null || responseObject.length() == 0) {
					handleErrorResponse(operationId, 0, null);
					return;
				}

				JSONObject jInterest = responseObject.optJSONObject(0);
				if (jInterest != null && jInterest.length() > 0) {

					MediaHelper.loadProfilePictureWithPlaceholder(picture.getContext(),
							jInterest.optString("avatarURL"), picture);

					name.setText(jInterest.optString("name"));

					animateView(progressInterest, false);
					animateView(hInterest, true);
					break;
				}
				else handleErrorResponse(operationId, 0, null);
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode, String description) {
		closeProgress();

		mainButton.setEnabled(true);

		String message = getString(R.string.error_unknown);
		if (errorCode == Constants.SERVER_ERROR_GENERIC) {
			showAlert(message);
		}
		else {
			switch (operationId) {
				case Constants.SERVER_OP_SIGNUP:
					switch (errorCode) {
						case Constants.SERVER_ERROR_SIGNIN_EMAIL_NOT_CONFIRMED:
						case Constants.SERVER_ERROR_SIGNIN_WRONG_PWD:
						case Constants.SERVER_ERROR_SIGNIN_WRONG_USERNAME:
						case Constants.SERVER_ERROR_SIGNUP_USER_ALREADY_EXISTS:
							message = description;
							break;
					}
					showAlert(message);

					break;

				case Constants.SERVER_OP_HCODE_VERIFY:
					animateView(progressInterest, false);
					animateView(hInterest, false);
					animateView(errorInterest, true);
					break;
			}
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		closeProgress();
		mainButton.setEnabled(true);
		animateView(progressInterest, false);
	}


	private void animateView(@NonNull final View view, final boolean show) {

		view.animate().alpha(show ? 1 : 0).setListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				if (show)
					view.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (!show)
					view.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationCancel(Animator animation) {}

			@Override
			public void onAnimationRepeat(Animator animation) {}
		}).start();

	}

	private String convertArrayToHcode() {
		if (mHCode != null && mHCode.length > 0) {
			StringBuilder sb = new StringBuilder();
			for (String s : mHCode) {
				sb.append(s);
			}

			String res = sb.toString();
			if (res.trim().replaceAll("\\s", "").length() == 5)
				return res;
		}

		return null;
	}

	private class HCodeTextWatcher implements TextWatcher {

		private View view, nextView;

		HCodeTextWatcher(View view, @Nullable View nextView) {
			this.view = view;
			this.nextView = nextView;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

		@Override
		public void afterTextChanged(Editable s) {

			animateView(errorInterest, false);

			switch (view.getId()) {
				case R.id.hcode_1:
					mHCode[0] = s.toString();
					break;
				case R.id.hcode_2:
					mHCode[1] = s.toString();
					break;
				case R.id.hcode_3:
					mHCode[2] = s.toString();
					break;
				case R.id.hcode_4:
					mHCode[3] = s.toString();
					break;

				case R.id.hcode_5:
					mHCode[4] = s.toString();
					if (s.length() > 0) {
						callHCodeVerification();
						Utils.closeKeyboard(view);
					}
					break;
			}

			if (s.length() > 0) {
				if (nextView != null)
					nextView.requestFocus(View.FOCUS_RIGHT);
			}
		}
	}

	private class HcodeErasingTextFocus implements View.OnFocusChangeListener {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus)
				((TextView) v).setText("");
		}
	}

}