/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.PhoneNumberHelper;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class InterestClaimFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListenerWithErrorDescription, OnMissingConnectionListener,
		MediaHelper.MediaUploadListenerWithCallback.OnUploadCompleteListener, OnBackPressedListener {

	public static final String LOG_TAG = InterestClaimFragment.class.getCanonicalName();

	private SlidingUpPanelLayout slidingPanel;

	private String interestId;
	private String interestName;
	private String interestAvatar;

	private TextView toolbarTitle;
	private ImageView profilePicture;
	private View backArrow;
	private TextView title, body1;
	private TextView titlePhone;

	private String mediaFileUri;


	private View mPhoneCallView;
	private View mDocumentView;

	private boolean phoneViewActive = false;

	// PHONE SECTION
	private PhoneNumberHelper mPhoneHelper;

	private String mLanguage;
	private List<String> languages = new ArrayList<>();
	private ArrayAdapter<String> languagesAdapter;
	private Spinner languagesSpinner;


	public InterestClaimFragment() {
		// Required empty public constructor
	}

	public static InterestClaimFragment newInstance(String interestId, String name, String avatarUrl) {

		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, interestId);
		args.putString(Constants.EXTRA_PARAM_2, name);
		args.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		InterestClaimFragment fragment = new InterestClaimFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_interest_claim, container, false);
		configureLayout(view);
		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof ProfileActivity)
			((ProfileActivity) getActivity()).setBackListener(this);

		languages.add("English");
		if (getActivity() != null) {
			languagesAdapter = new ArrayAdapter<>(getActivity(), R.layout.claim_lang_simple_spinner_item, languages);
			// Specify the layout to use when the list of choices appears
			languagesAdapter.setDropDownViewResource(R.layout.claim_lang_dropdown_item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_INTEREST_CLAIM);

		getLanguages();

		setLayout();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		if (context instanceof ProfileActivity)
			((ProfileActivity) context).setBackListener(this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof ProfileActivity)
			((ProfileActivity) activity).setBackListener(this);
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (getActivity() instanceof ProfileActivity)
			((ProfileActivity) getActivity()).setBackListener(null);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(Constants.EXTRA_PARAM_1, interestId);
		outState.putString(Constants.EXTRA_PARAM_2, interestName);
		outState.putString(Constants.EXTRA_PARAM_3, interestAvatar);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				interestId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				interestName = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				interestAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.back_arrow:
				if (getActivity() != null)
					getActivity().onBackPressed();
				break;

			case R.id.claim_page_btn:
				if (slidingPanel != null)
					slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
				break;
			case R.id.claim_page_btn_phone:
				claimType = ClaimType.PHONE;
				String numComplete = mPhoneHelper != null ? mPhoneHelper.getCompleteNumber() : null;
				if (Utils.isStringValid(numComplete))
					claimInterest(null, numComplete, mLanguage);
				else activityListener.showAlert(R.string.error_claim_missing_info);
				break;

			case R.id.scan_file_button:
				if (getActivity() instanceof HLActivity)
					mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(((HLActivity) getActivity()), HLMediaType.PHOTO, this);
				break;
			case R.id.choose_file_button:
				if (getActivity() instanceof HLActivity)
					MediaHelper.checkPermissionForDocuments(((HLActivity) getActivity()), this, true);
				break;
			case R.id.dialog_btn_cancel:
				if (slidingPanel != null)
					slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
				break;

			case R.id.claim_phone_call_btn:
				showPhoneView(true);
				break;
			case R.id.claim_documents_btn:
				showPhoneView(false);
				break;
		}
	}


	@Override
	public void onBackPressed() {
		if (slidingPanel != null && slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
			slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
		else if (getActivity() instanceof ProfileActivity) {
			((ProfileActivity) getActivity()).setBackListener(null);
			getActivity().onBackPressed();
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (getActivity() == null || !(getActivity() instanceof HLActivity)) return;

		File f = null;
		switch (requestCode) {
			case Constants.RESULT_DOCUMENTS:
				if (data != null && data.getData() != null) {
					Uri selectedFile = data.getData();

					try {
						InputStream input = getActivity().getContentResolver().openInputStream(selectedFile);
						String mime = getActivity().getContentResolver().getType(selectedFile);
						f = MediaHelper.getFileFromContentUri(input, mime, (HLActivity) getActivity(), true);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
				break;

			case Constants.RESULT_PHOTO:
				Uri u = Uri.parse(mediaFileUri);
				f = new File(u.getPath());
				if (f.exists()) {
					switch (requestCode) {
						case Constants.RESULT_PHOTO:
							LogUtils.d(LOG_TAG, "Media captured with type=PHOTO and path: " + f.getAbsolutePath()
									+ " and file exists=" + f.exists());
							break;
					}

//					MediaHelper.scanFile(getActivity(), f.getAbsolutePath());
				}
				break;
		}

		attemptMediaUpload((HLActivity) getActivity(), f);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Utils.isContextValid(getActivity()) && getActivity() instanceof HLActivity) {
			HLActivity activity = ((HLActivity) getActivity());

			switch (requestCode) {
				case Constants.PERMISSIONS_REQUEST_DOCUMENTS:
					if (grantResults.length > 0) {
						if (grantResults.length == 1 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED) {
							MediaHelper.checkPermissionForDocuments(activity, this, true);
						}
					}
					break;

				case Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA:
					if (grantResults.length > 0) {
						if (grantResults.length == 3 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED &&
								grantResults[1] == PackageManager.PERMISSION_GRANTED &&
								grantResults[2] == PackageManager.PERMISSION_GRANTED) {
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.PHOTO, this);
						}
						else if (grantResults.length == 2 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED &&
								grantResults[1] == PackageManager.PERMISSION_GRANTED) {
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.PHOTO, this);
						}
						else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.PHOTO, this);
					}
					break;
			}
		}
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_CLAIM_INTEREST:
				LogUtils.d(LOG_TAG, "Interest claimed successfully");
				if (Utils.isContextValid(getActivity())) {
					final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
							.customView(R.layout.custom_dialog_claim_thanks, false)
							.cancelable(false)
							.build();
					View v = dialog.getCustomView();
					if (v != null) {
						v.findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
								getActivity().onBackPressed();
							}
						});

						// TODO: 5/15/2018    CURRENTLY THERE'S NO DIFFERENCE
//						String s = getString(R.string.dialog_claim_message_document);
//						if (claimType == ClaimType.PHONE)
//							s = getString(R.string.dialog_claim_message_phone);
//						((TextView) v.findViewById(R.id.dialog_content)).setText(s);
					}

					dialog.show();
				}

				GetIdentitiesService.startService(getActivity());
				break;

			case Constants.SERVER_OP_CLAIM_INTEREST_GET_LANG:
				if (responseObject == null || responseObject.optJSONObject(0) == null) {
					handleErrorResponse(operationId, 0, null);
					return;
				}

				JSONArray languages = responseObject.optJSONObject(0).optJSONArray("languages");
				if (languages != null && languages.length() > 0) {
					this.languages.clear();
					for (int i = 0; i < languages.length(); i++) {
						this.languages.add(languages.optString(i));
					}

					languagesAdapter.notifyDataSetChanged();
				}

				break;
		}
	}


	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode, String description) {
		switch (operationId) {
			case Constants.SERVER_OP_CLAIM_INTEREST:
				switch (errorCode) {
					case Constants.SERVER_ERROR_INTEREST_ALREADY_CLAIMED:
						activityListener.showAlert(description);
						break;
				}
				break;

			case Constants.SERVER_OP_CLAIM_INTEREST_GET_LANG:
				languages.clear();
				languages.add("English");
				languagesAdapter.notifyDataSetChanged();

				mLanguage = "English";
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		activityListener.closeProgress();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	protected void configureLayout(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		profilePicture = toolbar.findViewById(R.id.profile_picture);
		backArrow = toolbar.findViewById(R.id.back_arrow);
		backArrow.setOnClickListener(this);

		slidingPanel = view.findViewById(R.id.sliding_file_upload);
		slidingPanel.setTouchEnabled(true);
		slidingPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
			@Override
			public void onPanelSlide(View panel, float slideOffset) {}

			@Override
			public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
				if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED)
					backArrow.setOnClickListener(InterestClaimFragment.this);
				else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED)
					backArrow.setOnClickListener(null);
			}
		});

		View bottomSheet = view.findViewById(R.id.claim_bottom_sheet);
		bottomSheet.findViewById(R.id.scan_file_button).setOnClickListener(this);
		bottomSheet.findViewById(R.id.choose_file_button).setOnClickListener(this);

		title = view.findViewById(R.id.claim_title);
		body1 = view.findViewById(R.id.claim_body_1);
		titlePhone = view.findViewById(R.id.claim_title_phone);

		mDocumentView = view.findViewById(R.id.layout_documents);

		view.findViewById(R.id.claim_page_btn).setOnClickListener(this);
		view.findViewById(R.id.claim_phone_call_btn).setOnClickListener(this);


		mPhoneCallView = view.findViewById(R.id.layout_phone_call);

		if (mPhoneHelper == null)
			mPhoneHelper = new PhoneNumberHelper(PhoneNumberHelper.UsageType.INTEREST_CLAIM);
		mPhoneHelper.configurePicker(view);

		languagesSpinner = view.findViewById(R.id.languages_spinner);
		languagesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mLanguage = languages.get(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		view.findViewById(R.id.claim_page_btn_phone).setOnClickListener(this);
		view.findViewById(R.id.claim_documents_btn).setOnClickListener(this);

	}

	@Override
	protected void setLayout() {
		toolbarTitle.setText(getString(R.string.interest_claim_toolbar, interestName));
		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), interestAvatar, profilePicture);

		title.setText(getString(R.string.interest_claim_title_body, interestName));
		body1.setText(getString(R.string.interest_claim_body_1, interestName));
		titlePhone.setText(getString(R.string.interest_claim_title_body_phone, interestName));

		languagesSpinner.setAdapter(languagesAdapter);

		if (Utils.isStringValid(mLanguage) && languages != null && languages.contains(mLanguage))
			languagesSpinner.setSelection(languages.indexOf(mLanguage));
	}


	private enum ClaimType { PHONE, DOCUMENT }
	private ClaimType claimType;
	private void claimInterest(@Nullable String mediaLink, @Nullable String phoneNumber, @Nullable String language) {
		if ((Utils.isStringValid(mediaLink) && claimType == ClaimType.DOCUMENT) ||
				(Utils.isStringValid(phoneNumber) && claimType == ClaimType.PHONE)) {
			Object[] result = null;
			try {
				result = HLServerCalls.claimInterest(mUser.getUserId(), interestId, mediaLink,
						phoneNumber, language);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (getActivity() instanceof HLActivity)
				HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
						.handleCallResult(this, (HLActivity) getActivity(), result);
		}
		else {
			activityListener.showAlert(R.string.error_claim_missing_info);
		}
	}

	private void getLanguages() {
		Object[] result = null;
		try {
			result = HLServerCalls.claimInterestGetLanguages();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}


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
			try {
				MediaHelper.MediaUploadListener listener = new MediaHelper.MediaUploadListenerWithCallback(activity,
						file.getAbsolutePath(), this);

				service.uploadMedia(activity, file, MediaHelper.MEDIA_UPLOAD_CLAIM, mUser.getId(), mUser.getCompleteName(), listener);
			}
			catch (IllegalArgumentException e) {
				LogUtils.e(LOG_TAG, e.getMessage(), e);
				activity.showAlertWithRetry(R.string.error_upload_media, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						activity.openProgress();
						try {
							MediaHelper.MediaUploadListener listener =
									new MediaHelper.MediaUploadListenerWithCallback(activity,
											file.getAbsolutePath(), InterestClaimFragment.this);
							service.uploadMedia(activity, file, MediaHelper.MEDIA_UPLOAD_CLAIM, mUser.getId(), mUser.getCompleteName(), listener);
						} catch (IllegalArgumentException e1) {
							LogUtils.e(LOG_TAG, e.getMessage() + "\n\n2nd time in a row!", e);
							activity.showAlert(R.string.error_upload_media);
						}
					}
				});
				activity.closeProgress();
			}
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void onUploadComplete(String path, final String mediaLink) {
		if (Utils.isContextValid(getActivity())) {
			if (slidingPanel != null)
				slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

			claimType = ClaimType.DOCUMENT;

			if (Utils.isContextValid(getActivity())) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						claimInterest(mediaLink, null, null);
					}
				});
			}

		}

		// deletes temporary file used for upload
		File f = new File(path);
		if (f.exists())
			f.delete();
	}


	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showPhoneView(final boolean show) {
		int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

		mDocumentView.setVisibility(show ? View.GONE : View.VISIBLE);
		mDocumentView.animate().setDuration(shortAnimTime).alpha(
				show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mDocumentView.setVisibility(show ? View.GONE : View.VISIBLE);
			}
		});

		mPhoneCallView.setVisibility(show ? View.VISIBLE : View.GONE);
		mPhoneCallView.animate().setDuration(shortAnimTime).alpha(
				show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mPhoneCallView.setVisibility(show ? View.VISIBLE: View.GONE);
			}
		});

		phoneViewActive = show;
	}

}
