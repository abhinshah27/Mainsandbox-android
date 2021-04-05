/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.FamilyRelationship;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserAboutMe;
import it.keybiz.lbsapp.corporate.models.HLUserAboutMeMore;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.LifeEvent;
import it.keybiz.lbsapp.corporate.models.enums.RequestsStatusEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.EditPictureMenuItemClickListenerKt;
import it.keybiz.lbsapp.corporate.utilities.listeners.OnTargetMediaUriSelectedListener;
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserDetailFragment extends HLFragment implements View.OnClickListener,
		DatePickerDialog.OnDateSetListener, OnMissingConnectionListener,
		MediaHelper.MediaUploadListenerWithCallback.OnUploadCompleteListener,
		OnTargetMediaUriSelectedListener {

	public static final String LOG_TAG = UserDetailFragment.class.getCanonicalName();

	private View mainView;

	private View backgroundContainer;
	private View heartsAvailLayout;
	private ImageView profilePicture;
	private TextView name;
	private TextView heartsTot, heartsAvailable;

	private View aboutMeEditIcon, aboutMeEditDone;
	private TextView birthday;
	private Spinner status;
	private View spinnerStatusIndicator;
	private TextView city;
	private EditText cityEdit;
	private TextView description;
	private EditText descriptionEdit;
	private DetailSpinnerAdapter aboutStatusAdapter;

	private View eventsEditIcon, eventsEditDone;
	private ViewGroup eventsContainer;
	private TextView noLifetimeEvents;
	private boolean restoreNoEventsView;

	private RealmList<LifeEvent> updatedLifeEvents;
	private HLUserAboutMe updatedAboutMe;
	private HLUserAboutMeMore updatedMoreAboutMe;
	private String updateType;


	private View moreAboutEditIcon, moreAboutEditDone;
	private Spinner genderSpinner, interestedInSpinner;
	private View genderSpinnerIndicator, interestedSpinnerIndicator;
	private TextView moreBirthplace, moreAddress, moreOtherNames;
	private EditText moreBirthplaceEdit, moreAddressEdit, moreOtherNamesEdit;
	private TextView moreLanguages;
	private ArrayAdapter<CharSequence> moreGenderAdapter;
	private DetailSpinnerAdapter moreInterestedInAdapter;
	private RealmList<String> moreLanguagesList;


	private View familyRelsView;
	private View familyEditIcon, familyEditDone;
	private ViewGroup familyContainer;
	private TextView noFamilyRels;
	private boolean restoreNoFamilyView;


	private String mediaFileUri;
	private HLMediaType mediaCaptureType;

	private boolean friendMode = false;
	private String friendId;
	private HLUserGeneric friendUser;

	private boolean editAbout = false;
	private boolean editEvents = false;
	private boolean editMoreAbout = false;
	private boolean editFamily = false;

	private FamilyRelationship relationToRemove;
	private View relationToRemoveView;

	public UserDetailFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param friendId Parameter 1.
	 * @return A new instance of fragment UserDetailFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static UserDetailFragment newInstance(String friendId) {
		UserDetailFragment fragment = new UserDetailFragment();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, friendId);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		mainView = inflater.inflate(R.layout.fragment_profile_landing, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(mainView);

		return mainView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() != null) {
			String[] statuses = getResources().getStringArray(R.array.relationship_statuses);
			aboutStatusAdapter = new DetailSpinnerAdapter(getActivity(), R.layout.about_me_simple_spinner_item,
					new ArrayList<>(Arrays.asList(statuses)), R.layout.about_me_dropdown_item);

			moreGenderAdapter = ArrayAdapter.createFromResource(getActivity(),
					R.array.profile_more_gender, R.layout.more_about_me_simple_spinner_item);
			// Specify the layout to use when the list of choices appears
			moreGenderAdapter.setDropDownViewResource(R.layout.more_about_me_dropdown_item);

			String[] interestedIn = getResources().getStringArray(R.array.profile_more_interestedin);
			moreInterestedInAdapter = new DetailSpinnerAdapter(getActivity(), R.layout.more_about_me_simple_spinner_item,
					new ArrayList<>(Arrays.asList(interestedIn)), R.layout.more_about_me_dropdown_item);

			configureResponseReceiver();
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_DETAIL);

		setLayout();

		if (friendMode)
			callForFriendDetails(CallType.PROFILE);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(Constants.EXTRA_PARAM_1, friendId);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				friendId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
		}

		friendMode = Utils.isStringValid(friendId);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.close_btn:
				if (Utils.isContextValid(getActivity())) {
					getActivity().onBackPressed();
				}
//				if (friendMode && getActivity() != null) {
//					getActivity().onBackPressed();
//				}
				break;

			case R.id.about_me_edit:
				manageAboutMe(true, false);
				break;
			case R.id.about_me_done:
				manageAboutMe(false, true);
				break;

			case R.id.birthday_value:
				openDatePicker(PickerType.BIRTHDAY, birthday.getText().toString(), null);
				break;

			case R.id.lifetime_edit:
				manageLifetimeEvents(true, false);
				break;
			case R.id.lifetime_done:
				manageLifetimeEvents(false, true);
				break;

			case R.id.more_about_me_edit:
				manageMoreAboutMe(true, false);
				break;
			case R.id.more_about_me_done:
				manageMoreAboutMe(false, true);
				break;

			case R.id.family_edit:
				manageFamilyRelations(true);
				break;
			case R.id.family_done:
				manageFamilyRelations(false);
				break;

			case R.id.profile_picture:
				mediaCaptureType = HLMediaType.PHOTO_PROFILE;
				if (getActivity() instanceof HLActivity) {
					MediaHelper.openPopupMenu(getContext(), R.menu.edit_users_picture, profilePicture,
							new EditPictureMenuItemClickListenerKt((HLActivity) getActivity(), mediaCaptureType, this, this));
				}
				break;

			case R.id.background_container:
				mediaCaptureType = HLMediaType.PHOTO_WALL;
				if (getActivity() instanceof HLActivity) {
					MediaHelper.openPopupMenu(getContext(), R.menu.edit_users_picture, backgroundContainer,
							new EditPictureMenuItemClickListenerKt((HLActivity) getActivity(), mediaCaptureType, this, this));
				}
				break;

			case R.id.more_languages_text:
				Intent intent = new Intent(getContext(), SelectLanguageActivity.class);
				ArrayList<String> langs = new ArrayList<>();
				langs.addAll(mUser.getMoreAboutLanguages());
				intent.putExtra(Constants.EXTRA_PARAM_1, langs);
				startActivityForResult(intent, Constants.RESULT_PROFILE_ADD_LANGUAGES);
				if (Utils.isContextValid(getActivity()))
					getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.no_animation);
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
						else
							activityListener.showAlert(R.string.error_upload_media);
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
								else
									activityListener.showAlert(R.string.error_upload_media);
							}
						}
					}
					attemptMediaUpload(((HLActivity) getActivity()), f);
				}
				break;

			case Constants.RESULT_PROFILE_ADD_LANGUAGES:

				if (resultCode == Activity.RESULT_OK) {
					if (data != null && data.hasExtra(Constants.EXTRA_PARAM_1)) {
						moreLanguagesList = new RealmList<>();
						moreLanguagesList.addAll(data.getStringArrayListExtra(Constants.EXTRA_PARAM_1));
						String langs = Utils.formatStringFromLanguages(moreLanguagesList);
						moreLanguages.setText(langs);
					}
				}
				else moreLanguages.setText(R.string.user_detail_hint_languages);

				break;
		}
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

							RealmUtils.closeRealm(realm);
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
						if (mediaCaptureType == HLMediaType.PHOTO_PROFILE)
							profilePicture.setImageDrawable(resource);
						else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
							mainView.setBackground(resource);
					}
				});
			}
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
	public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {

	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_UPDATE_PROFILE:
				switch (updateType) {
					case LIFE_EVENTS:
						handleEventsForRealm();
						manageLifetimeEvents(false, false);
						break;

					case ABOUT_ME:
						aboutMeEditIcon.setVisibility(View.VISIBLE);
						aboutMeEditDone.setVisibility(View.GONE);

						birthday.setOnClickListener(null);

						status.setEnabled(false);
						spinnerStatusIndicator.setVisibility(View.GONE);

						boolean city0 = (cityEdit.getText().length() == 0);
						city.setText(city0 ? getString(R.string.user_detail_hint_city) : cityEdit.getText());
						city.setTextColor(Utils.getColor(getContext(),
								city0 ? R.color.black_38 : R.color.black_87));
						city.setVisibility(View.VISIBLE);
						boolean descr0 = (descriptionEdit.getText().length() == 0);
						description.setText(descr0 ? getString(R.string.user_detail_hint_description) : descriptionEdit.getText());
						description.setTextColor(Utils.getColor(getContext(),
								descr0 ? R.color.black_38 : R.color.black_54));
						description.setVisibility(View.VISIBLE);

						cityEdit.setVisibility(View.GONE);
						descriptionEdit.setVisibility(View.GONE);

						if (RealmUtils.isValid(realm)) {
							realm.beginTransaction();
							mUser.setAboutMe(realm.copyToRealm(updatedAboutMe));
							realm.commitTransaction();
						}
						break;

					case MORE_ABOUT_ME:
						moreAboutEditIcon.setVisibility(View.VISIBLE);
						moreAboutEditDone.setVisibility(View.GONE);

						moreLanguages.setOnClickListener(null);

						genderSpinner.setEnabled(false);
						genderSpinnerIndicator.setVisibility(View.GONE);
						interestedInSpinner.setEnabled(false);
						interestedSpinnerIndicator.setVisibility(View.GONE);

						moreLanguages.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
						moreLanguages.setCompoundDrawablePadding(0);
						moreLanguagesList = null;

						boolean birthplace0 = (moreBirthplaceEdit.getText().length() == 0);
						moreBirthplace.setText(birthplace0 ? getString(R.string.user_detail_hint_birthplace) : moreBirthplaceEdit.getText());
						moreBirthplace.setTextColor(Utils.getColor(getContext(),
								birthplace0 ? R.color.black_38 : R.color.black_87));
						moreBirthplace.setVisibility(View.VISIBLE);
						moreBirthplaceEdit.setVisibility(View.GONE);

						boolean address0 = (moreAddressEdit.getText().length() == 0);
						moreAddress.setText(address0 ? getString(R.string.user_detail_hint_address) : moreAddressEdit.getText());
						moreAddress.setTextColor(Utils.getColor(getContext(),
								address0 ? R.color.black_38 : R.color.black_87));
						moreAddress.setVisibility(View.VISIBLE);
						moreAddressEdit.setVisibility(View.GONE);

						boolean otherNames0 = (moreOtherNamesEdit.getText().length() == 0);
						moreOtherNames.setText(otherNames0 ? getString(R.string.user_detail_hint_other_names) : moreOtherNamesEdit.getText());
						moreOtherNames.setTextColor(Utils.getColor(getContext(),
								otherNames0 ? R.color.black_38 : R.color.black_87));
						moreOtherNames.setVisibility(View.VISIBLE);
						moreOtherNamesEdit.setVisibility(View.GONE);

						if (RealmUtils.isValid(realm)) {
							realm.beginTransaction();
							mUser.setMoreAboutMe(realm.copyToRealm(updatedMoreAboutMe));
							realm.commitTransaction();
						}
						break;
				}
				break;

			case Constants.SERVER_OP_REMOVE_FAMILY:

				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						if (mUser.hasFamilyRelationships())
							mUser.getFamilyRelationships().remove(relationToRemove);

						if (!mUser.hasAuthorizedFamilyRelationships()) {
							mUser.setWantsFamilyFilter(false);
							mUser.updateFiltersForSingleCircle(new HLCircle(Constants.CIRCLE_FAMILY_NAME), false);
						}
					}
				});

				if (familyContainer != null)
					familyContainer.removeView(relationToRemoveView);

				break;

			case Constants.SERVER_OP_GET_USER_PROFILE:
				if (responseObject != null && responseObject.length() > 0) {
					try {
						JSONObject user = responseObject.getJSONObject(0);
						friendUser = new HLUserGeneric().deserializeToClass(user);
						callForFriendDetails(CallType.PERSON);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				break;

			case Constants.SERVER_OP_GET_PERSON_V2:
				if (responseObject != null && responseObject.length() > 0) {
					try {
						JSONObject user = responseObject.getJSONObject(0);
						if (friendUser != null)
							friendUser.deserializeComplete(user, true);
						setLayout();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		String log = "ERROR with code: ";
		switch (operationId) {
			case Constants.SERVER_OP_UPDATE_PROFILE:
				log = "ERROR UPDATE PROFILE with code: ";
				break;

			case Constants.SERVER_OP_GET_USER_PROFILE:
			case Constants.SERVER_OP_GET_PERSON_V2:
				log = "ERROR GET USER DETAILS with code: ";
				break;
		}

		LogUtils.e(LOG_TAG, log + errorCode);
		activityListener.showGenericError();
	}

	@Override
	public void onMissingConnection(int operationId) {
		switch (operationId) {
			case Constants.SERVER_OP_GET_USER_PROFILE:
			case Constants.SERVER_OP_GET_PERSON_V2:
				setLayout();
				break;
		}
	}


	@Override
	protected void configureLayout(@NonNull View view) {
		view.findViewById(R.id.close_btn).setOnClickListener(this);

		backgroundContainer = view.findViewById(R.id.background_container);
		heartsAvailLayout = view.findViewById(R.id.hearts_avail_layout);

		profilePicture = view.findViewById(R.id.profile_picture);
		name = view.findViewById(R.id.name);
		heartsTot = view.findViewById(R.id.count_heart_total);
		heartsAvailable = view.findViewById(R.id.count_heart_available);


		/* ABOUT ME */

		aboutMeEditIcon = view.findViewById(R.id.about_me_edit);
		aboutMeEditDone = view.findViewById(R.id.about_me_done);

		birthday = view.findViewById(R.id.birthday_value);
		city = view.findViewById(R.id.hometown_value);
		cityEdit = view.findViewById(R.id.hometown_edit);
		cityEdit.setVisibility(View.GONE);
		description = view.findViewById(R.id.description);
		descriptionEdit = view.findViewById(R.id.description_edit);
		descriptionEdit.setVisibility(View.GONE);

		spinnerStatusIndicator = view.findViewById(R.id.spinner_indicator_marital_status);
		status = view.findViewById(R.id.relationship_value);
		status.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {}
		});
		status.setSelection(0);
		status.setEnabled(false);
		spinnerStatusIndicator.setVisibility(View.GONE);


		/* LIFETIME EVENTS */

		eventsEditIcon = view.findViewById(R.id.lifetime_edit);
		eventsEditDone = view.findViewById(R.id.lifetime_done);

		eventsContainer = view.findViewById(R.id.lt_events_container);
		noLifetimeEvents = view.findViewById(R.id.no_lt_events);


		/* MORE ABOUT ME */

		moreAboutEditIcon = view.findViewById(R.id.more_about_me_edit);
		moreAboutEditDone = view.findViewById(R.id.more_about_me_done);

		moreLanguages = view.findViewById(R.id.more_languages_text);
		moreLanguages.setSelected(true);
		moreBirthplace = view.findViewById(R.id.more_birth_text);
		moreBirthplaceEdit = view.findViewById(R.id.more_birth_edit);
		moreBirthplaceEdit.setVisibility(View.GONE);
		moreBirthplaceEdit.setHint(R.string.friend_detail_hint_birthplace);
		moreAddress = view.findViewById(R.id.more_address_text);
		moreAddressEdit = view.findViewById(R.id.more_address_edit);
		moreAddressEdit.setVisibility(View.GONE);
		moreAddressEdit.setHint(R.string.friend_detail_hint_address);
		moreOtherNames = view.findViewById(R.id.more_other_text);
		moreOtherNamesEdit = view.findViewById(R.id.more_other_edit);
		moreOtherNamesEdit.setVisibility(View.GONE);
		moreOtherNamesEdit.setHint(R.string.friend_detail_hint_other_names);

		genderSpinnerIndicator = view.findViewById(R.id.spinner_indicator_gender);
		genderSpinner = view.findViewById(R.id.spinner_gender);
		genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {}
		});
		genderSpinner.setSelection(0);
		genderSpinner.setEnabled(false);
		genderSpinnerIndicator.setVisibility(View.GONE);

		interestedSpinnerIndicator = view.findViewById(R.id.spinner_indicator_interestedin);
		interestedInSpinner = view.findViewById(R.id.spinner_interestedin);
		interestedInSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {}
		});
		interestedInSpinner.setSelection(0);
		interestedInSpinner.setEnabled(false);
		interestedSpinnerIndicator.setVisibility(View.GONE);


		/* FAMILY RELATIONSHIPS */

		familyRelsView = view.findViewById(R.id.family_relationships_view);
		familyContainer = view.findViewById(R.id.family_container);
		familyEditIcon = view.findViewById(R.id.family_edit);
		familyEditDone = view.findViewById(R.id.family_done);
		noFamilyRels = view.findViewById(R.id.no_family);


		if (!friendMode) {
			backgroundContainer.setOnClickListener(this);
			profilePicture.setOnClickListener(this);

			profilePicture.setOnLongClickListener(v -> {
				// INFO: 2/14/19    LUISS - NO IDENTITIES
//				if (Utils.isContextValid(getActivity())) {
//					Utils.openIdentitySelection(getActivity(), null);
//					return true;
//				}
				return false;
			});

			aboutMeEditIcon.setOnClickListener(this);
			aboutMeEditDone.setOnClickListener(this);
			birthday.setOnClickListener(null);

			eventsEditIcon.setOnClickListener(this);
			eventsEditDone.setOnClickListener(this);

			moreAboutEditIcon.setOnClickListener(this);
			moreAboutEditDone.setOnClickListener(this);
			moreLanguages.setOnClickListener(null);

			familyEditIcon.setOnClickListener(this);
			familyEditDone.setOnClickListener(this);
		}
	}

	@Override
	protected void setLayout() {
		String coverURL = "", avatarURL = "", sName = "";
		String sBirthday = "", sBirthdayFallBack = "";
		String sCity = "", sCityFallBack = "";
		String sDescription = "", sDescriptionFallBack = "";
		int totHearts = 0;
		String sStatus = ""; int iStatus = -1;
		String sBirthplace = "", sBirthplaceFallBack = "";
		String sAddress = "", sAddressFallBack = "";
		String sOtherNames = "", sOtherNamesFallBack = "";
		String sLanguages = "", sLanguagesFallBack = "";
		int sGender = 0;
		String sInterestedIn = ""; int iInterestedIn = -1;
		if (friendMode) {
			if (friendUser != null) {
				coverURL = friendUser.getWallImageLink();
				avatarURL = friendUser.getAvatarURL();
				sName = friendUser.getCompleteName();

				sBirthday = friendUser.getAboutBirthday();
				sStatus = friendUser.getAboutStatus();
				iStatus = friendUser.getAboutStatusSelection(getResources());
				sCity = friendUser.getAboutCity();
				sDescription = friendUser.getAboutDescription();

				sBirthplace = friendUser.getMoreAboutBirthplace();
				sGender = friendUser.getMoreAboutGenderSelection(getResources());
				sInterestedIn = friendUser.getMoreAboutInterestedIn();
				iInterestedIn = friendUser.getMoreAboutInterestedInSelection(getResources());
				sAddress = friendUser.getMoreAboutAddress();
				sOtherNames = friendUser.getMoreAboutOtherNames();
				sLanguages = friendUser.getMoreAboutLanguages();

				sBirthdayFallBack = getString(R.string.friend_detail_hint_birthdate);
				sCityFallBack = getString(R.string.friend_detail_hint_city);
				sDescriptionFallBack = getString(R.string.friend_detail_hint_description);

				sBirthplaceFallBack = getString(R.string.friend_detail_hint_birthplace);
				sAddressFallBack = getString(R.string.friend_detail_hint_address);
				sOtherNamesFallBack = getString(R.string.friend_detail_hint_other_names);
				sLanguagesFallBack = getString(R.string.friend_detail_hint_languages);

				totHearts = friendUser.getTotHearts();
			}
		}
		else {
			coverURL = mUser.getCoverPhotoURL();
			avatarURL = mUser.getAvatarURL();
			sName = mUser.getCompleteName();

			sBirthday = mUser.getAboutBirthday();
			sStatus = mUser.getAboutStatus();
			iStatus = mUser.getAboutStatusSelection(getResources());
			sCity = mUser.getAboutCity();
			sDescription = mUser.getAboutDescription();

			sBirthplace = mUser.getMoreAboutBirthplace();
			sGender = mUser.getMoreAboutGenderSelection(getResources());
			sInterestedIn = mUser.getMoreAboutInterestedIn();
			iInterestedIn = mUser.getMoreAboutInterestedInSelection(getResources());
			sAddress = mUser.getMoreAboutAddress();
			sOtherNames = mUser.getMoreAboutOtherNames();
			sLanguages = mUser.getMoreAboutLanguagesToString();

			sBirthdayFallBack = getString(R.string.user_detail_hint_birthdate);
			sCityFallBack = getString(R.string.user_detail_hint_city);
			sDescriptionFallBack = getString(R.string.user_detail_hint_description);

			sBirthplaceFallBack = getString(R.string.user_detail_hint_birthplace);
			sAddressFallBack = getString(R.string.user_detail_hint_address);
			sOtherNamesFallBack = getString(R.string.user_detail_hint_other_names);
			sLanguagesFallBack = getString(R.string.user_detail_hint_languages);

			totHearts = mUser.getTotHearts();
		}

		familyRelsView.setVisibility(View.VISIBLE);

		if (Utils.isContextValid(getContext())) {
			// TODO: 12/29/2017    disables bitmaps usage
			GlideApp.with(getContext()).asDrawable().load(coverURL).into(new SimpleTarget<Drawable>() {
				@Override
				public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
					mainView.setBackground(resource);
				}
			});
//		mainView.setBackground(new BitmapDrawable(getResources(), mUser.getWall()));

			// TODO: 12/29/2017    disables bitmaps usage
			MediaHelper.loadPictureWithGlide(getContext(), avatarURL, profilePicture);
//		profilePicture.setImageBitmap(mUser.getAvatar());
		}

		name.setText(sName);
		heartsTot.setText(Utils.getReadableCount(heartsTot.getResources(), totHearts));
		// INFO: 2/14/19    LUISS - available info
		heartsAvailLayout.setVisibility(/*friendMode ? View.GONE : View.VISIBLE*/View.GONE);
		heartsAvailable.setText(Utils.getReadableCount(heartsAvailable.getResources(), mUser.getTotHeartsAvailable()));

		String date = sBirthday;
		birthday.setText(Utils.isStringValid(date) ? date : sBirthdayFallBack);
		birthday.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(date) ? R.color.black_87 : R.color.black_38));

		// Apply the adapter to the spinner
		if (iStatus == -1) {
			SharedPrefsUtils.storeStringProperty(getContext(), SharedPrefsUtils.PROPERTY_STATUS_FALLBACK, sStatus);
			iStatus = 0;
		}
		aboutStatusAdapter.getItems().add(0, SharedPrefsUtils.getStringProperty(getContext(), SharedPrefsUtils.PROPERTY_STATUS_FALLBACK));
		status.setAdapter(aboutStatusAdapter);
		if (status.isEnabled())
			status.setSelection(iStatus);
		else {
			status.setEnabled(true);
			status.setSelection(iStatus);
			status.setEnabled(false);
		}

		String c = sCity;
		city.setText(Utils.isStringValid(c) ? c : sCityFallBack);
		city.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(c) ? R.color.black_87 : R.color.black_38));
		String descr = sDescription;
		description.setText(Utils.isStringValid(descr) ? descr : sDescriptionFallBack);
		description.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(descr) ? R.color.black_54 : R.color.black_38));

		aboutMeEditIcon.setVisibility(friendMode ? View.GONE : View.VISIBLE);

		manageAboutMe(editAbout, false);

		setEvents();


		// Apply the adapter to the spinner
		genderSpinner.setAdapter(moreGenderAdapter);
		if (genderSpinner.isEnabled())
			genderSpinner.setSelection(sGender);
		else {
			genderSpinner.setEnabled(true);
			genderSpinner.setSelection(sGender);
			genderSpinner.setEnabled(false);
		}

		if (iInterestedIn == -1) {
			SharedPrefsUtils.storeStringProperty(getContext(), SharedPrefsUtils.PROPERTY_INTERESTED_IN_FALLBACK, sInterestedIn);
			iInterestedIn = 0;
		}
		moreInterestedInAdapter.getItems().add(0, SharedPrefsUtils.getStringProperty(getContext(),
				SharedPrefsUtils.PROPERTY_INTERESTED_IN_FALLBACK));
		interestedInSpinner.setAdapter(moreInterestedInAdapter);
		if (interestedInSpinner.isEnabled())
			interestedInSpinner.setSelection(iInterestedIn);
		else {
			interestedInSpinner.setEnabled(true);
			interestedInSpinner.setSelection(iInterestedIn);
			interestedInSpinner.setEnabled(false);
		}

		String b = sBirthplace;
		moreBirthplace.setText(Utils.isStringValid(b) ? b : sBirthplaceFallBack);
		moreBirthplace.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(b) ? R.color.black_87 : R.color.black_38));

		String addr = sAddress;
		moreAddress.setText(Utils.isStringValid(addr) ? addr : sAddressFallBack);
		moreAddress.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(addr) ? R.color.black_87 : R.color.black_38));

		String langs = (Utils.isStringValid(moreLanguages.getText().toString()) && !sLanguages.equals(moreLanguages.getText().toString())) ?
				moreLanguages.getText().toString() : sLanguages;
		moreLanguages.setText(Utils.isStringValid(langs) ? langs : sLanguagesFallBack);
		moreLanguages.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(langs) ? R.color.black_87 : R.color.black_38));
		moreLanguages.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
		moreLanguages.setCompoundDrawablePadding(0);

		String other = sOtherNames;
		moreOtherNames.setText(Utils.isStringValid(other) ? other : sOtherNamesFallBack);
		moreOtherNames.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(other) ? R.color.black_87 : R.color.black_38));

		moreAboutEditIcon.setVisibility(friendMode ? View.GONE : View.VISIBLE);
		manageMoreAboutMe(editMoreAbout, false);

		setFamilyRelationships();
	}


	private void manageLifetimeEvents(boolean editable, boolean call) {
		if (eventsContainer == null ||
				(!friendMode && eventsContainer.getChildCount() < 1))
			return;

		editEvents = editable;

		if (updatedLifeEvents == null)
			updatedLifeEvents = new RealmList<>();
		else
			updatedLifeEvents.clear();
		for (int i = 0; i < eventsContainer.getChildCount() - 1; i++) {
			View child = eventsContainer.getChildAt(i);
			if (child != null) {

				if (child.getId() == R.id.no_lt_events) {
					if (editable) {
						eventsEditIcon.setVisibility(View.GONE);
						eventsEditDone.setVisibility(View.VISIBLE);

						// if events.size > 0, remove NoEvent string
						if ((eventsContainer.getChildCount() - 1) > 1)
							eventsContainer.removeView(child);
					}
					continue;
				}

				boolean isAdd = child.getId() == R.id.lifetime_event_add;
				final TextView dateTv = child.findViewById(isAdd ? R.id.lt_event_date_add : R.id.lt_event_date),
						descriptionTv = child.findViewById(isAdd ? R.id.lt_event_description_add : R.id.lt_event_description);
				EditText descriptionEt = child.findViewById(isAdd ? R.id.lt_event_description_edit_add : R.id.lt_event_description_edit);

				View delete = child.findViewById(isAdd ? R.id.btn_delete_add : R.id.btn_delete);

				if (editable) {
					eventsEditIcon.setVisibility(View.GONE);
					eventsEditDone.setVisibility(View.VISIBLE);

					dateTv.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							openDatePicker(PickerType.LT_EVENT, dateTv.getText().toString(), dateTv);
						}
					});

					descriptionEt.setText(descriptionTv.getText());
					descriptionTv.setVisibility(View.GONE);
					descriptionEt.setVisibility(View.VISIBLE);

					delete.setVisibility(View.VISIBLE);
				}
				else {
					if (call) {
						LifeEvent event = new LifeEvent(dateTv.getText().toString(), descriptionEt.getText().toString());

						if (event.isValid()) {
							updatedLifeEvents.add(event);
						}
						else if (!event.isVoid()) {
							if (Utils.isContextValid(getContext())) {
								final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_error_invalid_event);
								if (dialog != null) {
									View v = dialog.getCustomView();
									if (v != null) {
										Button bp = v.findViewById(R.id.button_positive);
										bp.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View view) {
												dialog.dismiss();
											}
										});
										bp.setText(R.string.ok);

										v.findViewById(R.id.button_negative).setVisibility(View.GONE);
									}
									dialog.show();
								}
								return;
							}
						} else {
							eventsContainer.removeView(child);

							if (eventsContainer.getChildCount() == 1)
								restoreNoEventsView = true;
						}
					}
					else {
						dateTv.setOnClickListener(null);

						descriptionTv.setText(descriptionEt.getText());
						descriptionEt.setVisibility(View.GONE);
						descriptionTv.setVisibility(View.VISIBLE);

						delete.setVisibility(View.GONE);
					}
				}
			}
		}

		if (!editable) {
			if (call) {
				callUpdate(updateType = LIFE_EVENTS);
			}
			else {
				if (restoreNoEventsView) {
					String s = getResources().getString(R.string.no_lifetime_events);
					if (friendMode) {
						s = getResources().getString(
								R.string.no_lifetime_events_friend,
								friendUser != null ?
										friendUser.getFirstName() :
										getResources().getString(R.string.user_detail_your_friend)
						);
					}
					noLifetimeEvents.setText(s);
					eventsContainer.addView(noLifetimeEvents, 0);
					eventsEditIcon.setVisibility(View.GONE);
					restoreNoEventsView = false;
				}
				else
					eventsEditIcon.setVisibility(friendMode ? View.GONE : View.VISIBLE);

				eventsEditDone.setVisibility(View.GONE);
			}
		}
	}

	private void handleEventsForRealm() {
		if (updatedLifeEvents != null && !updatedLifeEvents.isEmpty() &&
				RealmUtils.isValid(realm)) {
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(@NonNull Realm realm) {
					RealmList<LifeEvent> temp = new RealmList<>();
					for (LifeEvent le : updatedLifeEvents) {
						temp.add(realm.copyToRealm(le));
					}
					mUser.setLifeEvents(temp);
				}
			});
		}
	}

	private void manageAboutMe(boolean editable, boolean call) {
		editAbout = editable;

		if (editable) {
			aboutMeEditIcon.setVisibility(View.GONE);
			aboutMeEditDone.setVisibility(View.VISIBLE);

			birthday.setOnClickListener(this);

			status.setEnabled(true);
			spinnerStatusIndicator.setVisibility(View.VISIBLE);

			city.setVisibility(View.GONE);
			description.setVisibility(View.GONE);

			if (!city.getText().toString().equals(getString(R.string.user_detail_hint_city)))
				cityEdit.setText(city.getText());
			cityEdit.setVisibility(View.VISIBLE);
			if (!description.getText().toString().equals(getString(R.string.user_detail_hint_description)))
				descriptionEdit.setText(description.getText());
			descriptionEdit.setVisibility(View.VISIBLE);
		}
		else {
			updatedAboutMe = new HLUserAboutMe(
					birthday.getText().toString(),
					status.getSelectedItem().toString(),
					cityEdit.getText().toString(),
					descriptionEdit.getText().toString()
			);
		}

		if (call)
			callUpdate(updateType = ABOUT_ME);
	}

	private void manageMoreAboutMe(boolean editable, boolean call) {
		editMoreAbout = editable;

		if (editable) {
			moreAboutEditIcon.setVisibility(View.GONE);
			moreAboutEditDone.setVisibility(View.VISIBLE);

			moreLanguages.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_signup_right_indicator, 0);
			moreLanguages.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.activity_margin));

			moreLanguages.setOnClickListener(this);

			genderSpinner.setEnabled(true);
			genderSpinnerIndicator.setVisibility(View.VISIBLE);
			interestedInSpinner.setEnabled(true);
			interestedSpinnerIndicator.setVisibility(View.VISIBLE);

			moreBirthplace.setVisibility(View.GONE);
			moreAddress.setVisibility(View.GONE);
			moreOtherNames.setVisibility(View.GONE);

			if (!moreBirthplace.getText().toString().equals(getString(R.string.user_detail_hint_birthplace)))
				moreBirthplaceEdit.setText(moreBirthplace.getText());
			moreBirthplaceEdit.setVisibility(View.VISIBLE);
			if (!moreAddress.getText().toString().equals(getString(R.string.user_detail_hint_address)))
				moreAddressEdit.setText(moreAddress.getText());
			moreAddressEdit.setVisibility(View.VISIBLE);
			if (!moreOtherNames.getText().toString().equals(getString(R.string.user_detail_hint_other_names)))
				moreOtherNamesEdit.setText(moreOtherNames.getText());
			moreOtherNamesEdit.setVisibility(View.VISIBLE);
		}
		else {
			if (moreLanguagesList != null) {
				updatedMoreAboutMe = new HLUserAboutMeMore(
						moreBirthplaceEdit.getText().toString(),
						genderSpinner.getSelectedItem().toString(),
						interestedInSpinner.getSelectedItem().toString(),
						moreLanguagesList,
						moreOtherNamesEdit.getText().toString(),
						moreAddressEdit.getText().toString()
				);
			}
			else {
				updatedMoreAboutMe = new HLUserAboutMeMore(
						getContext(),
						moreBirthplaceEdit.getText().toString(),
						genderSpinner.getSelectedItem().toString(),
						interestedInSpinner.getSelectedItem().toString(),
						moreLanguages.getText().toString(),
						moreOtherNamesEdit.getText().toString(),
						moreAddressEdit.getText().toString()
				);
			}
		}

		if (call)
			callUpdate(updateType = MORE_ABOUT_ME);
	}

	public static final String LIFE_EVENTS = "lifeevents";
	public static final String ABOUT_ME = "aboutme";
	public static final String MORE_ABOUT_ME = "moreaboutme";
	private void callUpdate(String context) {
		Object[] result = null;

		try {
			result = HLServerCalls.updateProfile(mUser.getId(), context, updatedAboutMe, updatedLifeEvents,
					updatedMoreAboutMe, getResources());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication())).handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private enum CallType { PROFILE, PERSON }
	private void callForFriendDetails(CallType type) {
		Object[] result = null;

		try {
			if (type == CallType.PROFILE)
				result = HLServerCalls.getUserProfile(friendId);
			else if (type == CallType.PERSON)
				result = HLServerCalls.getPerson(mUser.getId(), friendId);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private void callForFamilyRemove(@NonNull FamilyRelationship familyRel) {
		Object[] result = null;

		try {
			result = HLServerCalls.removeFamilyRelation(mUser.getId(), familyRel);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private void setEvents() {
		final List<LifeEvent> events = friendMode ?
				(friendUser != null ? friendUser.getLifeEvents() : new ArrayList<LifeEvent>())
				: mUser.getLifeEvents();

		eventsContainer.removeAllViews();

		if (events != null && !events.isEmpty()) {
			eventsEditIcon.setVisibility(friendMode ? View.GONE : View.VISIBLE);
			eventsEditDone.setVisibility(View.GONE);

			if (friendMode)
				Collections.sort(events);
			else if (RealmUtils.isValid(realm)) {
				realm.beginTransaction();
				Collections.sort(events);
				realm.commitTransaction();
			}
			for (LifeEvent le : events) {
				final View v = LayoutInflater.from(getContext()).inflate(R.layout.item_profile_lifetime_event, eventsContainer, false);
				if (v != null) {
					((TextView) v.findViewById(R.id.lt_event_date)).setText(le.getFormattedDate());
					v.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							v.setVisibility(View.GONE);
							eventsContainer.removeView(v);

							if (eventsContainer.getChildCount() == 1) {
								restoreNoEventsView = true;
								manageLifetimeEvents(false, true);
							}
						}
					});

					String s = le.getDescription();
					((TextView) v.findViewById(R.id.lt_event_description)).setText(Utils.getFormattedHtml(s));

					// initializes EditText with same string as the static TextView
					if (Utils.isStringValid(s))
						((TextView) v.findViewById(R.id.lt_event_description_edit)).setText(s);

					v.setTag(le);

					eventsContainer.addView(v);
				}
			}
		}
		else {
			restoreNoEventsView = true;
//			eventsContainer.addView(noLifetimeEvents, 0);
//			eventsEditIcon.setVisibility(View.GONE);
//			eventsEditDone.setVisibility(View.GONE);
		}

		if (!friendMode) {
			View newEvent = LayoutInflater.from(getContext()).inflate(R.layout.item_profile_lifetime_event_new, eventsContainer, false);
			newEvent.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					final View addEvent = LayoutInflater.from(getContext()).inflate(R.layout.item_profile_lifetime_event_add, eventsContainer, false);
					if (addEvent != null) {
						final EditText addEventDescrEdit = addEvent.findViewById(R.id.lt_event_description_edit_add);
						final TextView addEventDescr = addEvent.findViewById(R.id.lt_event_description_add);

						final TextView addEventDate = addEvent.findViewById(R.id.lt_event_date_add);
						addEventDate.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								openDatePicker(PickerType.LT_EVENT,
										new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date()),
										addEventDate);
							}
						});

						final View addEventDelete = addEvent.findViewById(R.id.btn_delete_add);
						addEventDelete.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								view.setVisibility(View.GONE);
								eventsContainer.removeView(addEvent);

								if (eventsContainer.getChildCount() == 1) {
									restoreNoEventsView = true;
									manageLifetimeEvents(false, true);
								}
							}
						});

						addEventDescr.setVisibility(View.GONE);
						addEventDescrEdit.setVisibility(View.VISIBLE);

						addEventDelete.setVisibility(View.VISIBLE);

						eventsContainer.addView(addEvent, eventsContainer.getChildCount() - 1);
					}

					if (eventsEditDone.getVisibility() != View.VISIBLE) {
						// manage previous events making them editable
						manageLifetimeEvents(true, false);
					}
				}
			});
			eventsContainer.addView(newEvent);
		}

		manageLifetimeEvents(editEvents, false);
	}

	private void setFamilyRelationships() {

		List<FamilyRelationship> famRels = null;
		if (friendMode) {
			if (friendUser != null)
				famRels = friendUser.getFamilyRelationships();
		}
		else famRels = mUser.getFamilyRelationships();

		if (famRels != null && !famRels.isEmpty()) {
			familyContainer.removeAllViews();
			familyEditDone.setVisibility(View.GONE);

			if (RealmUtils.isValid(realm)) {
				realm.beginTransaction();
				Collections.sort(famRels);
				realm.commitTransaction();
			}
			for (final FamilyRelationship fr : famRels) {
				final View family = LayoutInflater.from(getContext()).inflate(R.layout.item_family_relations_detail, familyContainer, false);
				if (family != null && fr != null) {

					((TextView) family.findViewById(R.id.relation_name)).setText(fr.getFamilyRelationshipName());
					TextView name = family.findViewById(R.id.name);
					name.setText(fr.getName());
					TextView status = family.findViewById(R.id.status);
					status.setText(RequestsStatusEnum.getReadableForm(getContext(), fr.getStatus()));

					ImageView iv = family.findViewById(R.id.profile_picture);
					MediaHelper.loadProfilePictureWithPlaceholder(family.getContext(), fr.getAvatarURL(), iv);

					status.setVisibility(fr.getStatusEnum() != RequestsStatusEnum.AUTHORIZED ? View.VISIBLE : View.GONE);
					name.setEnabled(fr.getStatusEnum() == RequestsStatusEnum.AUTHORIZED);

					family.findViewById(R.id.picture_overlay).setVisibility(
							fr.getStatusEnum() != RequestsStatusEnum.AUTHORIZED ? View.VISIBLE : View.GONE
					);

					View close = family.findViewById(R.id.remove_btn);
					close.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							relationToRemoveView = family;
							callForFamilyRemove(relationToRemove = fr);
						}
					});
					close.setVisibility(View.GONE);

					family.setTag(fr);

					familyContainer.addView(family);
				}
			}
		}
		else {
			if (friendMode) {
				String s = getResources().getString(
						R.string.no_family_relationships_friend,
						friendUser != null ?
								friendUser.getFirstName() :
								getResources().getString(R.string.user_detail_your_friend)
				);
				noFamilyRels.setText(s);
			}
			else {
				familyContainer.removeAllViews();
				noFamilyRels.setVisibility(View.GONE);
			}
		}

		if (!friendMode) {
			View newFamily = LayoutInflater.from(getContext()).inflate(R.layout.item_profile_family_new, familyContainer, false);
			newFamily.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {

					profileActivityListener.showFamiyRelationsStep1Fragment();

				}
			});
			familyContainer.addView(newFamily);
		}

		manageFamilyRelations(editFamily);
	}

	private void manageFamilyRelations(boolean editable) {
		if (familyContainer == null || familyContainer.getChildCount() < 1)
			return;

		editFamily = editable;

		for (int i = 0; i < familyContainer.getChildCount() - 1; i++) {
			View child = familyContainer.getChildAt(i);
			if (child != null && child.getTag() instanceof FamilyRelationship) {

				// show close btn only if member is authorized and item is editable
				child.findViewById(R.id.remove_btn).setVisibility(
						editable ? View.VISIBLE : View.GONE
				);

				View status = child.findViewById(R.id.status);
				if (editable)
					status.setVisibility(View.GONE);
				else if (((FamilyRelationship) child.getTag()).getStatusEnum() != RequestsStatusEnum.AUTHORIZED)
					status.setVisibility(View.VISIBLE);
			}
		}

		familyEditIcon.setVisibility((editable || friendMode) ? View.GONE : View.VISIBLE);
		familyEditDone.setVisibility((!editable || friendMode) ? View.GONE : View.VISIBLE);
	}



	private enum PickerType { BIRTHDAY, LT_EVENT }
	private void openDatePicker(final PickerType type, String defaultDate, @Nullable final TextView eventDate) {
		Calendar date = Calendar.getInstance();

		try {
			Date d;
			if (type == PickerType.BIRTHDAY) {
				if (Utils.isStringValid(defaultDate)) {
					d = HLUserAboutMe.formatNewDate(defaultDate);
				}
				else {
					d = new Date();
				}
			}
			else {
				d = LifeEvent.formatNewDate(defaultDate);
			}
			date.setTime(d);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		DatePickerDialog dpd = DatePickerDialog.newInstance(
				this,
				date.get(Calendar.YEAR),
				date.get(Calendar.MONTH),
				date.get(Calendar.DAY_OF_MONTH)
		);
		dpd.setVersion(DatePickerDialog.Version.VERSION_2);
		dpd.setAccentColor(Utils.getColor(getContext(), R.color.colorAccent));
		dpd.setTitle(type == PickerType.BIRTHDAY ? getString(R.string.set_birthday) : getString(R.string.lt_event_date));

		// no max date set
//		if (type == PickerType.BIRTHDAY) {
//			Calendar max = Calendar.getInstance();
//			max.set(Calendar.YEAR, max.get(Calendar.YEAR) - 18);
//			dpd.setMaxDate(max);
//		}

		dpd.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
				if (type == PickerType.BIRTHDAY) {
					Calendar cal = Calendar.getInstance();
					cal.set(year, monthOfYear, dayOfMonth);
					SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
					birthday.setText(sdf.format(cal.getTime()));
					birthday.setTextColor(Utils.getColor(getContext(), R.color.black_87));
				}
				else if (type == PickerType.LT_EVENT && eventDate != null) {
					Calendar cal = Calendar.getInstance();
					cal.set(year, monthOfYear, dayOfMonth);
					SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
					eventDate.setText(sdf.format(cal.getTime()));
				}
			}
		});

		if (getActivity() != null)
			dpd.show(getActivity().getSupportFragmentManager(), "DatePickerDialog");
	}

	@Override
	public void onTargetMediaUriSelect(String mediaFileUri) {
		this.mediaFileUri = mediaFileUri;
	}


	//region == Class inner classes ==

	private class DetailSpinnerAdapter extends ArrayAdapter<CharSequence> {

		private List<CharSequence> mItems;
		@LayoutRes private int mDropDownResId;

		DetailSpinnerAdapter(@NonNull Context context, @LayoutRes int textResId, @NonNull List<CharSequence> objects,
		                     @LayoutRes int dropDownResId) {
			super(context, textResId, objects);

			mItems = objects;
			mDropDownResId = dropDownResId;
		}

		@NonNull
		public List<CharSequence> getItems() {
			return mItems == null ? new ArrayList<>() : mItems;
		}

		@Override
		public void setDropDownViewResource(int resource) {
			super.setDropDownViewResource(mDropDownResId);
		}
	}

	//endregion

}
