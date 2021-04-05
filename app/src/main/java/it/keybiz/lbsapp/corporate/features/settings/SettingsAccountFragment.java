/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListenerWithErrorDescription;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.PhoneNumberHelper;
import it.keybiz.lbsapp.corporate.utilities.listeners.EditPictureMenuItemClickListenerKt;
import it.keybiz.lbsapp.corporate.utilities.listeners.FieldValidityWatcher;
import it.keybiz.lbsapp.corporate.utilities.listeners.OnTargetMediaUriSelectedListener;
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/20/2018.
 */
public class SettingsAccountFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListenerWithErrorDescription, OnMissingConnectionListener, OnBackPressedListener,
		OnTargetMediaUriSelectedListener,
		MediaHelper.MediaUploadListenerWithCallback.OnUploadCompleteListener {

	private ImageView profilePicture, coverPicture;
	private TextView changeProfile, changeCover;

	private EditText firstName, lastName, email, mobile;

	private MaterialDialog dialogChangePwd;

	private String mediaFileUri;
	private HLMediaType mediaCaptureType;

	private static final UnderlineSpan UNDERLINE_SPAN = new UnderlineSpan();
	private static ForegroundColorSpan cssp;

	private PhoneNumberHelper mPhoneHelper;


	public SettingsAccountFragment() {
		// Required empty public constructor
	}

	public static SettingsAccountFragment newInstance() {
		Bundle args = new Bundle();

		SettingsAccountFragment fragment = new SettingsAccountFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_settings_account, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(this);

		else if (Utils.isContextValid(getActivity()))
			cssp = new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.black_38));
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_ACCOUNT);

		callServer(mType = CallType.GET, null);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (Utils.isContextValid(getActivity()))
			Utils.closeKeyboard(getActivity());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

	}

	@Override
	public void onBackPressed() {
		if (dialogChangePwd != null && dialogChangePwd.isShowing())
			dialogChangePwd.dismiss();
		else {
			saveInfo();
		}
	}

	private void saveInfo() {
		String sFirst = firstName.getText().toString();
		String sLast = lastName.getText().toString();
		String sEmail = email.getText().toString();
		String sMobile = mobile.getText().toString().replaceAll("\\s", "").trim();

		if (Utils.areStringsValid(sFirst, sLast, sEmail) && Utils.isEmailValid(sEmail)) {
			Bundle dataBundle = new Bundle();
			dataBundle.putString("firstName", sFirst.trim());
			dataBundle.putString("lastName", sLast.trim());
			dataBundle.putString("email", sEmail);
			dataBundle.putString("phoneNumber", Utils.isStringValid(sMobile) ? sMobile : "");
			callServer(mType = CallType.SAVE, dataBundle);
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.layout_photo_profile:
				mediaCaptureType = HLMediaType.PHOTO_PROFILE;
				if (getActivity() instanceof HLActivity) {
					MediaHelper.openPopupMenu(
							v.getContext(),
							R.menu.edit_users_picture,
							v,
							new EditPictureMenuItemClickListenerKt(
									(HLActivity) getActivity(),
									mediaCaptureType,
									this,
									this
							)
					);
				}
				break;

			case R.id.layout_photo_cover:
				mediaCaptureType = HLMediaType.PHOTO_WALL;
				if (getActivity() instanceof HLActivity) {
					MediaHelper.openPopupMenu(
							v.getContext(),
							R.menu.edit_users_picture,
							v,
							new EditPictureMenuItemClickListenerKt(
									(HLActivity) getActivity(),
									mediaCaptureType,
									this,
									this
							)
					);
				}
				break;

			case R.id.btn_change_pwd:
				dialogChangePwd = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_change_password);
				if (dialogChangePwd != null) {
					View view = dialogChangePwd.getCustomView();
					final EditText current;
					if (view != null) {
						View currentView = view.findViewById(R.id.settings_pwd_current);
						current = currentView.findViewById(R.id.edit_text);
						current.addTextChangedListener(new FieldValidityWatcher(R.id.settings_pwd_current, current));
						current.setHint(R.string.prompt_pwd_current);
						Utils.applyFontToTextView(current, R.string.fontRegular);
						current.setTransformationMethod(new PasswordTransformationMethod());

						View newPwdView = view.findViewById(R.id.settings_pwd_new);
						final EditText newPwd = newPwdView.findViewById(R.id.edit_text);
						newPwd.addTextChangedListener(new FieldValidityWatcher(R.id.settings_pwd_new, newPwd));
						newPwd.setHint(R.string.prompt_pwd_new);
						Utils.applyFontToTextView(newPwd, R.string.fontRegular);
						newPwd.setTransformationMethod(new PasswordTransformationMethod());

						View newPwdConfirmView = view.findViewById(R.id.settings_pwd_new_confirm);
						final EditText newPwdConfirm = newPwdConfirmView.findViewById(R.id.edit_text);
						newPwdConfirm.addTextChangedListener(new FieldValidityWatcher(R.id.settings_pwd_new_confirm, newPwdConfirm));
						newPwdConfirm.setHint(R.string.prompt_pwd_new_confirm);
						Utils.applyFontToTextView(newPwdConfirm, R.string.fontRegular);
						newPwdConfirm.setTransformationMethod(new PasswordTransformationMethod());

						DialogUtils.setPositiveButton(
								view.findViewById(R.id.save_pwd),
								R.string.action_save,
								new View.OnClickListener() {
									@Override
									public void onClick(View v) {

										String sCurrent = current.getText().toString(),
												sNewPwd = newPwd.getText().toString(),
												sNewPwdConfirm = newPwdConfirm.getText().toString();

										if (Utils.areStringsValid(sCurrent, sNewPwd, sNewPwdConfirm) &&

												// check on current pwd must be disabled, at least temporarily
//												Utils.isPasswordValid(sCurrent) &&

												Utils.isPasswordValid(sNewPwd) &&
												Utils.isPasswordValid(sNewPwdConfirm) &&
												sNewPwd.equals(sNewPwdConfirm)) {

											Bundle bundle = new Bundle();
											bundle.putString("currentPassword", sCurrent);
											bundle.putString("newPassword1", sNewPwd);
											bundle.putString("newPassword2", sNewPwdConfirm);

											callServer(CallType.CHANGE_PWD, bundle);
										}
									}
								}
						);
					}

					dialogChangePwd.show();

					DialogUtils.openKeyboardForDialog(dialogChangePwd);
				}
				break;
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (!Utils.isContextValid(getActivity()) || !(getActivity() instanceof HLActivity)) return;

		switch (requestCode) {

			case Constants.RESULT_PHOTO:
			case Constants.RESULT_GALLERY:
				File f = null;
				if (resultCode == Activity.RESULT_OK) {
					if (requestCode == Constants.RESULT_PHOTO) {

						if (Utils.isStringValid(mediaFileUri)) {
							Uri u = Uri.parse(mediaFileUri);
							f = new File(u.getPath());
							if (f.exists()) {
								LogUtils.d(LOG_TAG, "Media captured with type=PHOTO and path: " + f.getAbsolutePath()
										+ " and file exists=" + f.exists());
							}
						}
						else {
							activityListener.showAlert(R.string.error_upload_media);
							return;
						}

//						MediaHelper.scanFile(getActivity(), f.getAbsolutePath());
					}
					else {
						if (data != null && data.getData() != null) {
							Uri selectedFile = data.getData();
							String[] filePathColumn = new String[1];
							if (mediaCaptureType == HLMediaType.PHOTO_PROFILE ||
									mediaCaptureType == HLMediaType.PHOTO_WALL)
								filePathColumn[0] = MediaStore.Images.Media.DATA;
							else
								return;

							Cursor cursor = getActivity().getContentResolver().query(selectedFile,
									filePathColumn, null, null, null);
							if (cursor != null) {
								cursor.moveToFirst();

								int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
								mediaFileUri = cursor.getString(columnIndex);
								cursor.close();

								if (Utils.isStringValid(mediaFileUri))
									f = new File(Uri.parse(mediaFileUri).getPath());
								else {
									activityListener.showAlert(R.string.error_upload_media);
									return;
								}
							}
						}
					}
					attemptMediaUpload(((HLActivity) getActivity()), f);
				}
				break;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Utils.isContextValid(getActivity()) && getActivity() instanceof HLActivity) {
			HLActivity activity = ((HLActivity) getActivity());

			switch (requestCode) {
				case Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA:
					if (grantResults.length > 0) {
						if (grantResults.length == 3 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED &&
								grantResults[1] == PackageManager.PERMISSION_GRANTED &&
								grantResults[2] == PackageManager.PERMISSION_GRANTED) {
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, mediaCaptureType, this);
						} else if (grantResults.length == 2 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED &&
								grantResults[1] == PackageManager.PERMISSION_GRANTED) {
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, mediaCaptureType, this);
						} else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, mediaCaptureType, this);
					}
					break;

				case Constants.PERMISSIONS_REQUEST_GALLERY:
					if (grantResults.length > 0) {
						if (grantResults.length == 2 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED &&
								grantResults[1] == PackageManager.PERMISSION_GRANTED) {
							MediaHelper.checkPermissionForGallery(activity, mediaCaptureType, this);
						} else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
							MediaHelper.checkPermissionForGallery(activity, mediaCaptureType, this);
					}
					break;
			}
		}
	}





	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (responseObject == null || responseObject.length() == 0) {
			handleErrorResponse(operationId, 0, null);
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_GET_SET_USER_INFO:

				JSONObject json = responseObject.optJSONObject(0);
				final String jFirst = json.optString("firstName");
				final String jLast = json.optString("lastName");
				final String jEmail = json.optString("email");
				final String jMobile = json.optString("phoneNumber");
				final String jAvatar = json.optString("avatarURL");
				final String jCover = json.optString("wallImageLink");

				if (RealmUtils.isValid(realm)) {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							String sFirst = mUser.getFirstName();
							String sLast = mUser.getLastName();
							String sEmail = mUser.getEmail();
							String sMobile = mUser.getPhoneNumber();
							String sAvatar = mUser.getUserAvatarURL();
							String sCover = mUser.getUserCoverPhotoURL();

							if (mType == CallType.SAVE) {
								sFirst = firstName.getText().toString();
								sLast = lastName.getText().toString();
								sEmail = email.getText().toString();
								sMobile = mobile.getText().toString();
							}
							else if (mType == CallType.GET) {
								sFirst = jFirst;
								sLast = jLast;
								sEmail = jEmail;
								sMobile = jMobile;
								sAvatar = jAvatar;
								sCover = jCover;
							}

							mUser.setFirstName(sFirst);
							mUser.setLastName(sLast);
							mUser.setEmail(sEmail);
							mUser.setPhoneNumber(sMobile);
							mUser.setAvatarURL(sAvatar);
							mUser.setCoverPhotoURL(sCover);

							if (mType == CallType.GET)
								setLayout();
							else if (mType == CallType.SAVE)
								settingsActivityListener.closeScreen();
						}
					});
				}
				break;
			case Constants.SERVER_OP_SETTINGS_CHANGE_PWD:
				DialogUtils.closeKeyboardForDialog(dialogChangePwd);
				DialogUtils.closeDialog(dialogChangePwd);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode, String description) {
		@StringRes int msg = R.string.error_generic_operation;
		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_CHANGE_PWD:
				if (errorCode == Constants.SERVER_ERROR_SETTINGS_CHANGE_PWD_NO_MATCH_NEW)
					msg = R.string.error_change_pwd_new;
				else if (errorCode == Constants.SERVER_ERROR_SETTINGS_CHANGE_PWD_NO_MATCH_OLD)
					msg = R.string.error_change_pwd_old;
				activityListener.showAlert(description);
				break;

			case Constants.SERVER_OP_SETTINGS_GET_SET_USER_INFO:

				if (mType == CallType.SAVE)
					settingsActivityListener.closeScreen();
				activityListener.showAlert(msg);
				break;
		}

	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {
		if (mType == CallType.SAVE)
			settingsActivityListener.closeScreen();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		view.findViewById(R.id.btn_change_pwd).setOnClickListener(this);

		view.findViewById(R.id.layout_photo_profile).setOnClickListener(this);
		view.findViewById(R.id.layout_photo_cover).setOnClickListener(this);

		profilePicture = view.findViewById(R.id.profile_photo);
		coverPicture = view.findViewById(R.id.cover_photo);

		changeProfile = view.findViewById(R.id.txt_change_profile);
		changeCover = view.findViewById(R.id.txt_change_cover);

		View firstNameView = view.findViewById(R.id.first_name);
		firstName = firstNameView.findViewById(R.id.edit_text);
		firstName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		firstName.setHint(R.string.prompt_first_name);
		firstName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		firstName.addTextChangedListener(new FieldValidityWatcher(R.id.first_name, firstName));

		View lastNameView = view.findViewById(R.id.last_name);
		lastName = lastNameView.findViewById(R.id.edit_text);
		lastName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		lastName.setHint(R.string.prompt_last_name);
		lastName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		lastName.addTextChangedListener(new FieldValidityWatcher(R.id.last_name, lastName));

		View emailView = view.findViewById(R.id.settings_email);
		email = emailView.findViewById(R.id.edit_text);
		email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		email.setHint(R.string.prompt_email);
		email.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		email.addTextChangedListener(new FieldValidityWatcher(R.id.settings_email, email));

		View mobileView = view.findViewById(R.id.mobile);
		mobile = mobileView.findViewById(R.id.edit_text);
		mobile.setInputType(InputType.TYPE_CLASS_PHONE);
		mobile.setHint(R.string.prompt_mobile_opt_2);
		mobile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);

		/*
		if (mPhoneHelper == null)
			mPhoneHelper = new PhoneNumberHelper(PhoneNumberHelper.UsageType.SETTINGS_ACCOUNT);
		mPhoneHelper.configurePicker(view);
		*/
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_account);

		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), mUser.getUserAvatarURL(), profilePicture);
		MediaHelper.loadPictureWithGlide(getContext(), mUser.getCoverPhotoURL(), coverPicture);

		Utils.applySpansToTextView(changeProfile, R.string.action_change, UNDERLINE_SPAN, cssp);
		Utils.applySpansToTextView(changeCover, R.string.action_change, UNDERLINE_SPAN, cssp);

		firstName.setText(mUser.getFirstName());
		lastName.setText(mUser.getLastName());
		email.setText(mUser.getEmail());
		mobile.setText(mUser.getPhoneNumber());

	}


	public enum CallType { GET, SAVE, CHANGE_PWD }
	private CallType mType;
	private void callServer(CallType type, @Nullable Bundle dataBundle) {
		Object[] result = null;

		try {
			if (type == CallType.GET)
				result = HLServerCalls.getSettings(mUser.getUserId(), HLServerCalls.SettingType.ACCOUNT);
			else if (dataBundle != null && !dataBundle.isEmpty())
				result = HLServerCalls.saveAccountSettings(mUser.getUserId(), dataBundle, type);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}


	//region == Upload media section ==

	@Override
	public void onTargetMediaUriSelect(String mediaFileUri) {
		this.mediaFileUri = mediaFileUri;
	}

	String uploadType;
	private void attemptMediaUpload(final HLActivity activity, final File file) {
		if (file != null && file.exists()) {
			activity.openProgress();
			activity.setShowProgressAnyway(true);

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					activity.setProgressMessage(R.string.uploading_media);
				}
			}, 500);

			final MediaHelper.UploadService service = new MediaHelper.UploadService(activity);
			final MediaHelper.MediaUploadListenerWithCallback listener = new MediaHelper.MediaUploadListenerWithCallback(activity,
					file.getAbsolutePath(), this);
			try {
				if (mediaCaptureType == HLMediaType.PHOTO_PROFILE)
					uploadType = MediaHelper.MEDIA_UPLOAD_NEW_PIC_PROFILE;
				else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
					uploadType = MediaHelper.MEDIA_UPLOAD_NEW_PIC_WALL;

				if (Utils.isStringValid(uploadType))
					service.uploadMedia(activity, file, uploadType, mUser.getId(), mUser.getCompleteName(), listener);
			}
			catch (IllegalArgumentException e) {
				LogUtils.e(LOG_TAG, e.getMessage(), e);
				activity.showAlertWithRetry(R.string.error_upload_media, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						activity.openProgress();
						try {
							service.uploadMedia(activity, file, uploadType, mUser.getId(), mUser.getCompleteName(), listener);
						} catch (IllegalArgumentException e1) {
							LogUtils.e(LOG_TAG, e.getMessage() + "\n\n2nd time in a row!", e);
							activity.showAlert(R.string.error_upload_media);
						}
					}
				});
				activity.closeProgress();
			}
		}
		else activityListener.showGenericError();
	}

	@Override
	public void onUploadComplete(final String path, final String mediaLink) {
		if (Utils.isContextValid(getActivity())) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							HLUser mUser = new HLUser().readUser(realm);
							if (mediaCaptureType == HLMediaType.PHOTO_PROFILE)
								mUser.setAvatarURL(mediaLink);
							else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
								mUser.setCoverPhotoURL(mediaLink);
						}
					});

					handleMediaResult(path);
				}
			});
		}
	}

	private void handleMediaResult(String path) {
		if (Utils.isStringValid(path)) {
			File f = new File(Uri.parse("file:" + path).getPath());

			if (f.exists() && Utils.isContextValid(getActivity())) {
				GlideApp.with(getActivity()).asDrawable().load(f).into(new SimpleTarget<Drawable>() {
					@Override
					public void onLoadFailed(@Nullable Drawable errorDrawable) {
						if (mediaCaptureType == HLMediaType.PHOTO_PROFILE)
							profilePicture.setImageResource(R.drawable.ic_profile_placeholder);
					}

					@Override
					public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
						if (mediaCaptureType == HLMediaType.PHOTO_PROFILE) {
							profilePicture.setImageDrawable(resource);
							settingsActivityListener.refreshProfilePicture(resource);
						}
						else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
							coverPicture.setImageDrawable(resource);
					}
				});
			}
		}
	}

	//endregion
}
