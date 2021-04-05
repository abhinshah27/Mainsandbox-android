/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;


import android.animation.ObjectAnimator;
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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListenerWithErrorDescription;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLInterestAbout;
import it.keybiz.lbsapp.corporate.models.HLInterests;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.MoreInfoObject;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.EditPictureMenuItemClickListenerKt;
import it.keybiz.lbsapp.corporate.utilities.listeners.OnTargetMediaUriSelectedListener;
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InterestDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InterestDetailFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListenerWithErrorDescription,
		OnMissingConnectionListener,
		MediaHelper.MediaUploadListenerWithCallback.OnUploadCompleteListener,
		OnTargetMediaUriSelectedListener {

	public static final String LOG_TAG = InterestDetailFragment.class.getCanonicalName();

	private static String fallbackString;

	private enum UpdateType { ABOUT, MORE_INFO }
	private UpdateType updateType;

	private View mainView;

	private View backgroundContainer;
	private View preferredLabel;
	private View interestHeartsFollowersLayout;
	private TextView interestHeartsTotal;
	private TextView interestFollowers;
	private ImageView interestPicture;
	private TextView interestName;
	private EditText interestNameEdit;
	private TextView interestHeadline;
	private EditText interestHeadlineEdit;

	private TextView aboutTitle;
	private View aboutEditIcon, aboutEditDone;
	private TextView interestDescription;
	private EditText interestDescriptionEdit;
	private View publicInfoLayout;
	private TextView publicInfo;

	private View moreEditIcon, moreEditDone, moreInfoSection;
	private ViewGroup moreInfoContainer;

	private String updatedName, updatedHeadline;
	private HLInterestAbout updatedAbout;
	private List<MoreInfoObject> updatedMoreInfo;

	private String mediaFileUri;
	private HLMediaType mediaCaptureType;

	private String interestId;
	private Interest mInterest;

	private boolean editAbout = false;
	private boolean editMoreInfo = false;

	private TextView readMoreBtn;
	private boolean readMoreCollapsed = true;
	private int collapsedHeight = 0, expandedHeight = 0;

	private boolean meEditMode;


	public InterestDetailFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param interestId Parameter 1.
	 * @return A new instance of fragment UserDetailFragment.
	 */
	public static InterestDetailFragment newInstance(String interestId) {
		InterestDetailFragment fragment = new InterestDetailFragment();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, interestId);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		mainView = inflater.inflate(R.layout.fragment_interest_details, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(mainView);

		return mainView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() != null)
			configureResponseReceiver();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		fallbackString = getString(R.string.generic_fallback);
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);

		fallbackString = getString(R.string.generic_fallback);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_DETAIL);

		if (meEditMode)
			mInterest = (Interest) mUser.getSelectedObject();
		else if (Utils.isStringValid(interestId))
			mInterest = HLInterests.getInstance().getInterest(interestId);

		setLayout();

		callForInterestDetails();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(Constants.EXTRA_PARAM_1, interestId);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				interestId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);

			meEditMode = mUser != null && RealmObject.isValid(mUser) && mUser.isActingAsInterest() &&
					(!Utils.isStringValid(interestId) ||
							interestId.equals(mUser.getSelectedIdentity().getIdDBObject()));
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.close_btn:
				if (Utils.isContextValid(getActivity()))
					getActivity().onBackPressed();
				break;

			case R.id.about_edit:
				manageAbout(true, false);
				break;
			case R.id.about_done:
				manageAbout(false, true);
				break;
			case R.id.more_info_edit:
				manageMoreInfo(true, false);
				break;
			case R.id.more_info_done:
				manageMoreInfo(false, true);
				break;

			case R.id.profile_picture:
				mediaCaptureType = HLMediaType.PHOTO_PROFILE;
				if (getActivity() instanceof HLActivity) {
					MediaHelper.openPopupMenu(getContext(), R.menu.edit_users_picture, interestPicture,
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

			case R.id.read_more:
				animateReadMore();
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (!Utils.isContextValid(getActivity()) || !(getActivity() instanceof HLActivity)) return;

		if (resultCode == Activity.RESULT_OK) {
			File f = null;
			if (requestCode == Constants.RESULT_PHOTO) {

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
			}
			else if (requestCode == Constants.RESULT_GALLERY) {
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

						f = new File(Uri.parse(mediaFileUri).getPath());
					}
				}
			}

			attemptMediaUpload(((HLActivity) getActivity()), f);
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
					uploadType = MediaHelper.MEDIA_UPLOAD_NEW_PIC_PROFILE_INTEREST;
				else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
					uploadType = MediaHelper.MEDIA_UPLOAD_NEW_PIC_WALL_INTEREST;

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

//		Realm realm = null;
//		try {
//			realm = RealmUtils.getCheckedRealm();
//			if (RealmUtils.isValid(realm) && mInterest != null && RealmObject.isManaged(mInterest))
//			realm.executeTransaction(new Realm.Transaction() {
//				@Override
//				public void execute(@NonNull Realm realm) {
//					HLUser mUser = new HLUser().readUser(realm);
//					String id = mUser.getSelectedIdentity().getIdDBObject();
//					Interest mInterest = (Interest) RealmUtils.readFirstFromRealmWithId(realm, Interest.class, "_id", id);
//
//					if (mInterest != null) {
//						if (mediaCaptureType == PostTypeEnum.PHOTO_PROFILE)
//							mInterest.setAvatarURL(mediaLink);
//						else if (mediaCaptureType == PostTypeEnum.PHOTO_WALL)
//							mInterest.setWallPictureURL(mediaLink);
//					}
//				}
//			});
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if (realm != null)
//				realm.close();
//		}

		if (Utils.isContextValid(getActivity())) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							HLUser mUser = new HLUser().readUser(realm);
							String id = mUser.getSelectedIdentity().getIdDBObject();
							Interest mInterest = (Interest) RealmUtils.readFirstFromRealmWithId(realm, Interest.class, "_id", id);

							if (mInterest != null) {
								if (mediaCaptureType == HLMediaType.PHOTO_PROFILE)
									mInterest.setAvatarURL(mediaLink);
								else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
									mInterest.setWallPictureURL(mediaLink);
							}

							handleMediaResult(path);
						}
					});
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
							interestPicture.setImageResource(R.drawable.ic_profile_placeholder);
					}

					@Override
					public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
						if (mediaCaptureType == HLMediaType.PHOTO_PROFILE)
							interestPicture.setImageDrawable(resource);
						else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
							mainView.setBackground(resource);
					}
				});
			}
		}
	}

	@Override
	public void onTargetMediaUriSelect(String mediaFileUri) {
		this.mediaFileUri = mediaFileUri;
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
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_UPDATE_INTEREST:

				if (updateType == UpdateType.ABOUT) {
					aboutEditIcon.setVisibility(View.VISIBLE);
					aboutEditDone.setVisibility(View.GONE);

					manageAboutTextViews(interestName, interestNameEdit);
					manageAboutTextViews(interestHeadline, interestHeadlineEdit);

					manageTextViews(interestDescription, interestDescriptionEdit.getText().toString(), false);
					interestDescriptionEdit.setVisibility(View.GONE);

					readMoreBtn.setVisibility(View.VISIBLE);
					animateReadMore();

					if (RealmUtils.isValid(realm) && mInterest != null) {
						realm.beginTransaction();
						mInterest.setName(updatedName);
						mInterest.setHeadline(updatedHeadline);
						mInterest.setAbout(realm.copyToRealm(updatedAbout));
						realm.commitTransaction();
					}
				}
				else {
					moreEditIcon.setVisibility(View.VISIBLE);
					moreEditDone.setVisibility(View.GONE);

					handleMoreInfoForRealm();
					manageMoreInfo(false, false);
				}
				break;

			case Constants.SERVER_OP_GET_INTEREST_PROFILE:
				if (responseObject != null && responseObject.length() > 0) {
					try {
						final JSONObject json = responseObject.getJSONObject(0);
						if (meEditMode) {
							if (RealmUtils.isValid(realm)) {
								realm.executeTransaction(new Realm.Transaction() {
									@Override
									public void execute(@NonNull Realm realm) {
										mInterest = realm.createOrUpdateObjectFromJson(Interest.class, json);
										setLayout();
									}
								});
							}
						}
						else  {
							mInterest = new Interest().deserializeToClass(json);

							if (mInterest != null && HLInterests.getInstance().hasInterest(mInterest.getId()))
								HLInterests.getInstance().setInterest(mInterest);

							setLayout();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				break;
		}
	}

	private void handleMoreInfoForRealm() {
		if (updatedMoreInfo != null && !updatedMoreInfo.isEmpty() &&
				RealmUtils.isValid(realm) && mInterest != null) {
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(@NonNull Realm realm) {
					RealmList<MoreInfoObject> temp = new RealmList<>();
					for (MoreInfoObject mio : updatedMoreInfo) {
						temp.add(realm.copyToRealm(mio));
					}
					mInterest.setMoreInfo(temp);
				}
			});
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode, String description) {
		String log = "ERROR with code: ";
		String error = getString(R.string.error_generic_operation);

		switch (operationId) {
			case Constants.SERVER_OP_UPDATE_INTEREST:
				if (errorCode == Constants.SERVER_ERROR_USER_NOT_OWNER)
					error = description;
				log = "ERROR UPDATE INTEREST with code: ";
				break;

			case Constants.SERVER_OP_GET_INTEREST_PROFILE:
				log = "ERROR GET INTEREST DETAILS with code: ";
				break;
		}

		LogUtils.e(LOG_TAG, log + errorCode);
		activityListener.showAlert(error);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {
		switch (operationId) {
			case Constants.SERVER_OP_GET_INTEREST_PROFILE:
				setLayout();
				break;
		}
	}


	@Override
	protected void configureLayout(@NonNull View view) {
		View close = view.findViewById(R.id.close_btn);
		close.setOnClickListener(this);

		backgroundContainer = view.findViewById(R.id.background_container);
		interestHeartsFollowersLayout = view.findViewById(R.id.hearts_followers_section);
		interestPicture = view.findViewById(R.id.profile_picture);
		interestName = view.findViewById(R.id.interest_name);
		interestNameEdit = view.findViewById(R.id.interest_name_edit);

		interestHeadline = view.findViewById(R.id.interest_headline);
		interestHeadlineEdit = view.findViewById(R.id.interest_headline_edit);

		interestHeartsTotal = view.findViewById(R.id.count_heart_total);
		interestFollowers = view.findViewById(R.id.count_followers);

		preferredLabel = view.findViewById(R.id.preferred_icon);


		/* ABOUT */

		aboutTitle = view.findViewById(R.id.about_title);
		aboutTitle.setOnClickListener(this);

		aboutEditIcon = view.findViewById(R.id.about_edit);
		aboutEditDone = view.findViewById(R.id.about_done);

		interestDescription = view.findViewById(R.id.description);
		interestDescriptionEdit = view.findViewById(R.id.description_edit);
		interestDescriptionEdit.setVisibility(View.GONE);

		readMoreBtn = view.findViewById(R.id.read_more);
		readMoreBtn.setOnClickListener(this);

		moreEditIcon = view.findViewById(R.id.more_info_edit);
		moreEditDone = view.findViewById(R.id.more_info_done);
		moreInfoSection = view.findViewById(R.id.more_info_section);
		moreInfoContainer = view.findViewById(R.id.more_info_container);

		publicInfoLayout = view.findViewById(R.id.public_info_section);
		publicInfo = view.findViewById(R.id.public_info_text);


		if (meEditMode) {
			backgroundContainer.setOnClickListener(this);
			interestPicture.setOnClickListener(this);

			interestPicture.setOnLongClickListener(v -> {
				// INFO: 2/14/19    LUISS - NO IDENTITIES
//				if (Utils.isContextValid(getActivity())) {
//					Utils.openIdentitySelection(getActivity(), null);
//					return true;
//				}
				return false;
			});

			aboutEditIcon.setOnClickListener(this);
			aboutEditDone.setOnClickListener(this);
			moreEditIcon.setOnClickListener(this);
			moreEditDone.setOnClickListener(this);
		}
	}

	@Override
	protected void setLayout() {
		String coverURL = "", avatarURL = "", sName = "", sHeadline = "";
		String sDescription = "", sNote = "";
		String totHearts = "", totFollowers = "";
		boolean isPreferred = false, isClaimed = false;
		if (!meEditMode) {
			aboutEditIcon.setVisibility(View.GONE);
			aboutEditDone.setVisibility(View.GONE);
			moreEditIcon.setVisibility(View.GONE);
			moreEditDone.setVisibility(View.GONE);
		}
		else {
			aboutEditIcon.setVisibility(editAbout ? View.GONE : View.VISIBLE);
			aboutEditDone.setVisibility(editAbout ? View.VISIBLE : View.GONE);
			moreEditIcon.setVisibility(editMoreInfo ? View.GONE : View.VISIBLE);
			moreEditDone.setVisibility(editMoreInfo ? View.VISIBLE : View.GONE);
		}

		String id = null;
		if (mInterest != null) {
			coverURL = mInterest.getWallPictureURL();
			avatarURL = mInterest.getAvatarURL();
			sName = mInterest.getName();
			sHeadline = mInterest.getHeadline();

			sDescription = mInterest.getAboutDescription();
			sNote = mInterest.getAboutDescriptionNote();

			totHearts = mInterest.getHeartsWithNumber(getResources());
			totFollowers = mInterest.getFollowersWithNumber(getResources());

			isPreferred = mInterest.isPreferred();
			isClaimed = mInterest.isClaimed();

			id = mInterest.getId();
		}

		GlideApp.with(mainView).asDrawable().load(coverURL).into(new SimpleTarget<Drawable>() {
			@Override
			public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
				mainView.setBackground(resource);
			}
		});

		preferredLabel.setVisibility((!meEditMode && isPreferred) ? View.VISIBLE : View.INVISIBLE);

		MediaHelper.loadPictureWithGlide(getContext(), avatarURL, interestPicture);

		boolean isHighlanders = Utils.isStringValid(id) && id.equals(Constants.ID_INTEREST_HIGHLANDERS);
		interestHeartsFollowersLayout.setVisibility(isHighlanders ? View.GONE : View.VISIBLE);

		interestHeartsTotal.setText(totHearts);
		interestFollowers.setText(totFollowers);

		aboutTitle.setText(getString(R.string.about_title, sName));

		readMoreBtn.setText(Utils.getFormattedHtml(getString(R.string.read_more)));

		interestNameEdit.setText(sName);
		manageAboutTextViews(interestName, interestNameEdit);
		interestHeadlineEdit.setText(sHeadline);
		manageAboutTextViews(interestHeadline, interestHeadlineEdit);

		interestDescriptionEdit.setText(sDescription);
		manageTextViews(interestDescription, sDescription, false);

		aboutEditIcon.setVisibility(!meEditMode ? View.GONE : View.VISIBLE);
		moreEditIcon.setVisibility(!meEditMode ? View.GONE : View.VISIBLE);

		// INFO: 2/15/19    LUISS - NO public banner
		publicInfoLayout.setVisibility(/*isClaimed ? View.GONE : View.VISIBLE*/View.GONE);
		publicInfo.setText(sNote);

		manageAbout(editAbout, false);

		setMoreInfo();
	}

	private void manageAboutTextViews(TextView tv, EditText et) {
		String text = et.getText().toString();
		switch (et.getId()) {
			case R.id.interest_name_edit:
				tv.setText(Utils.isStringValid(text) ? text : getString(R.string.interest_detail_hint_name));
				break;
			case R.id.interest_headline_edit:
				tv.setText(Utils.isStringValid(text) ? text : getString(R.string.interest_detail_hint_headline));
				break;
		}

		tv.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(text) ? R.color.black_87 : R.color.black_38));
		tv.setVisibility(View.VISIBLE);
		et.setVisibility(View.GONE);
	}

	private void manageTextViews(TextView tv, String text, boolean wantsUnderline) {
		tv.setText(Utils.isStringValid(text) ? text : fallbackString);

		tv.setTextColor(Utils.getColor(getContext(),
				Utils.isStringValid(text) ? R.color.black_54 : R.color.black_38));
		tv.setVisibility(View.VISIBLE);

		if (wantsUnderline && Utils.isStringValid(text) && !text.equals(fallbackString))
			addUnderline(tv, text);
	}


	private void manageAbout(boolean editable, boolean call) {
		editAbout = editable;

		if (editable) {
			aboutEditIcon.setVisibility(View.GONE);
			aboutEditDone.setVisibility(View.VISIBLE);

			if (!interestName.getText().toString().equals(getString(R.string.interest_detail_hint_name)))
				interestNameEdit.setText(interestName.getText());
			interestNameEdit.setVisibility(View.VISIBLE);
			interestName.setVisibility(View.GONE);
			if (!interestHeadline.getText().toString().equals(getString(R.string.interest_detail_hint_headline)))
				interestHeadlineEdit.setText(interestHeadline.getText());
			interestHeadlineEdit.setVisibility(View.VISIBLE);
			interestHeadline.setVisibility(View.GONE);
			if (!interestDescription.getText().toString().equals(getString(R.string.user_detail_hint_description)))
				interestDescriptionEdit.setText(interestDescription.getText());
			interestDescriptionEdit.setVisibility(View.VISIBLE);
			interestDescription.setVisibility(View.GONE);

			readMoreBtn.setVisibility(View.GONE);
			animateReadMore();
		}
		else {
			updatedName = interestNameEdit.getText().toString();
			updatedHeadline = interestHeadlineEdit.getText().toString();

			updatedAbout = new HLInterestAbout(
					interestDescriptionEdit.getText().toString(),
					publicInfo.getText().toString()
			);
		}

		if (call) {
			updateType = UpdateType.ABOUT;

			if (Utils.isStringValid(updatedName))
				callUpdate();
			else
				activityListener.showAlert(R.string.interest_name_needed);
		}
	}

	private void setMoreInfo() {
		final List<MoreInfoObject> moreInfo =  mInterest != null ?
				mInterest.getMoreInfo() : new ArrayList<MoreInfoObject>();

		moreInfoContainer.removeAllViews();

		if (updatedMoreInfo == null)
			updatedMoreInfo = new ArrayList<>();
		else
			updatedMoreInfo.clear();

		if (!moreInfo.isEmpty()) {
			moreInfoSection.setVisibility(View.VISIBLE);
			moreEditIcon.setVisibility(meEditMode ? View.VISIBLE : View.GONE);
			moreEditDone.setVisibility(View.GONE);

			for (MoreInfoObject mio : moreInfo) {
				updatedMoreInfo.add(new MoreInfoObject(mio.getIconImageLink(), mio.getText()));

				final View v = LayoutInflater.from(getContext()).inflate(R.layout.item_interest_more_info, moreInfoContainer, false);
				if (v != null) {
					ImageView icon = v.findViewById(R.id.more_info_icon);
					MediaHelper.loadPictureWithGlide(icon.getContext(), mio.getIconImageLink(),
							new RequestOptions().fitCenter(), 0, 0, icon);
					String s = mio.getText();
					TextView tv = v.findViewById(R.id.more_info_tv);
					manageTextViews(tv, s, true);

					// initializes EditText with same string as the static TextView
					if (Utils.isStringValid(s))
						((TextView) v.findViewById(R.id.more_info_et)).setText(s);

					v.setTag(mio);

					moreInfoContainer.addView(v);
				}
			}
		}
		else moreInfoSection.setVisibility(View.GONE);

//		if (meEditMode)
//			manageMoreInfo(editMoreInfo, false);
	}

	private void manageMoreInfo(boolean editable, boolean call) {
		if (moreInfoContainer == null) return;

		editMoreInfo = editable;

		if (updatedMoreInfo == null)
			updatedMoreInfo = new ArrayList<>();
		else
			updatedMoreInfo.clear();
		for (int i = 0; i < moreInfoContainer.getChildCount(); i++) {
			View child = moreInfoContainer.getChildAt(i);
			if (child != null) {

				final TextView textTv = child.findViewById(R.id.more_info_tv);
				final EditText textEt = child.findViewById(R.id.more_info_et);

				if (editable) {
					moreEditIcon.setVisibility(View.GONE);
					moreEditDone.setVisibility(View.VISIBLE);

					String s = textTv.getText().toString().equals(fallbackString) ? "" : textTv.getText().toString();
					textEt.setText(s);
					textTv.setVisibility(View.GONE);
					textEt.setVisibility(View.VISIBLE);
				}
				else {
					Object tag = child.getTag();
					if (tag instanceof MoreInfoObject) {
						MoreInfoObject moreInfoObject = new MoreInfoObject(
								((MoreInfoObject) tag).getIconImageLink(),
								textEt.getText().toString()
						);

						updatedMoreInfo.add(moreInfoObject);
					}

					if (!call) {
						manageTextViews(textTv, textEt.getText().toString(), true);
//						textTv.setText(textEt.getText());
						textEt.setVisibility(View.GONE);
						textTv.setVisibility(View.VISIBLE);
					}
				}
			}
		}

		if (!editable) {
			if (call) {
				updateType = UpdateType.MORE_INFO;
				callUpdate();
			}
			else {
				moreEditIcon.setVisibility(View.VISIBLE);
				moreEditDone.setVisibility(View.GONE);
			}
		}
	}


	private void callUpdate() {
		Object[] result = null;

		if (mUser == null || mInterest == null || !Utils.isStringValid(mInterest.getId())) {
			activityListener.showGenericError();
			return;
		}

		try {
			result = HLServerCalls.updateInterestProfile(mUser.getUserId(), mInterest.getId(), updatedName,
					updatedHeadline, updatedAbout, updatedMoreInfo);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication())).handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private void callForInterestDetails() {
		Object[] result = null;

		try {
			result = HLServerCalls.getInterestProfile(mUser.getId(), Utils.isStringValid(interestId) ? interestId : mUser.getId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication())).handleCallResult(this, (HLActivity) getActivity(), result);
	}



	private void animateReadMore() {
		if (readMoreCollapsed) {
			readMoreBtn.setText(Utils.getFormattedHtml(getString(R.string.read_less)));

			if (collapsedHeight == 0) collapsedHeight = interestDescription.getMeasuredHeight();
			interestDescription.setHeight(collapsedHeight);
			interestDescriptionEdit.setHeight(collapsedHeight);
			interestDescription.setMaxLines(Integer.MAX_VALUE); //expand fully
			interestDescriptionEdit.setMaxLines(Integer.MAX_VALUE); //expand fully
			interestDescription.measure(View.MeasureSpec.makeMeasureSpec(interestDescription.getMeasuredWidth(), View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.UNSPECIFIED));
			if (expandedHeight == 0) expandedHeight = interestDescription.getMeasuredHeight();
			ObjectAnimator animation = ObjectAnimator.ofInt(interestDescription, "height", collapsedHeight, expandedHeight);
			animation.setDuration(250).start();

			readMoreCollapsed = false;
		}
		else {
			readMoreBtn.setText(Utils.getFormattedHtml(getString(R.string.read_more)));

			interestDescription.setMaxLines(getResources().getInteger(R.integer.interest_detail_description_lines)); //collapse fully
			interestDescriptionEdit.setMaxLines(getResources().getInteger(R.integer.interest_detail_description_lines)); //collapse fully
			ObjectAnimator animation = ObjectAnimator.ofInt(interestDescription, "height", expandedHeight, collapsedHeight);
			animation.setDuration(250).start();

			readMoreCollapsed = true;
		}
	}


	private void addUnderline(TextView textView, String string) {
		if (Utils.isStringValid(string)) {
			SpannableString spannableString = new SpannableString(textView.getText());
			int startIndexOfLink = textView.getText().toString().indexOf(string);
			spannableString.setSpan(new UnderlineSpan(), startIndexOfLink, startIndexOfLink + string.length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setText(spannableString, TextView.BufferType.SPANNABLE);
		}
	}

}
