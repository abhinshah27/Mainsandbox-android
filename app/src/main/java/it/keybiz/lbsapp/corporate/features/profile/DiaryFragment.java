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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import io.realm.RealmObject;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.PostList;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.EditPictureMenuItemClickListenerKt;
import it.keybiz.lbsapp.corporate.utilities.listeners.OnTargetMediaUriSelectedListener;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DiaryFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * @author mbaldrighi on 12/29/2017
 */
public class DiaryFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		PostListsAdapter.OnDiaryActionListener,
		MediaHelper.MediaUploadListenerWithCallback.OnUploadCompleteListener,
		OnTargetMediaUriSelectedListener {

	public static final String LOG_TAG = DiaryFragment.class.getCanonicalName();

	private String userId, userName, userAvatar;

	private SwipeRefreshLayout srl;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private RecyclerView postListsRv;
	private Integer scrollPosition;
	private View noResult;

	private boolean isUser;

	// CIRCLES
	private List<PostList> postListsToShow = new ArrayList<>();
	private Map<String, PostList> postListsLoaded = new ConcurrentHashMap<>();
	private PostListsAdapter listsAdapter;
	private LinearLayoutManager listsLlm;

	private SparseArray<WeakReference<HorizontalScrollView>> scrollViews = new SparseArray<>();
	private SparseArray<Integer[]> scrollViewsPositions = new SparseArray<>();

	private String mediaFileUri;


	public DiaryFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment InnerCircleFragment.
	 */
	public static DiaryFragment newInstance(String userId, String userName, String userAvatar) {
		DiaryFragment fragment = new DiaryFragment();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, userId);
		args.putString(Constants.EXTRA_PARAM_2, userName);
		args.putString(Constants.EXTRA_PARAM_3, userAvatar);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_profile_my_diary, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);


		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		callForPosts();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_DIARY);

		configureResponseReceiver();
		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		scrollPosition = listsLlm.findFirstCompletelyVisibleItemPosition();
		if (scrollViews != null && scrollViews.size() > 0) {
			for (int i = 0; i < scrollViews.size(); i++) {
				int key = scrollViews.keyAt(i);
				WeakReference<HorizontalScrollView> hsv = scrollViews.get(key);
				HorizontalScrollView scrollView = hsv.get();
				if (scrollView != null && scrollView.getTag() instanceof Integer) {
					scrollViewsPositions.put(
							((Integer) scrollView.getTag()),
							new Integer[] {
									scrollView.getScrollX(),
									scrollView.getScrollY()
							}
					);
				}
			}
		}
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		listsLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);

		listsAdapter = new PostListsAdapter(postListsToShow, this, isUser);
		listsAdapter.setHasStableIds(true);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.back_arrow:
				if (getActivity() != null)
					getActivity().onBackPressed();
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(Constants.EXTRA_PARAM_1, userId);
		outState.putString(Constants.EXTRA_PARAM_2, userName);
		outState.putString(Constants.EXTRA_PARAM_3, userAvatar);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				userId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				userName = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				userAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_3);

			if (Utils.isStringValid(userId) && RealmObject.isValid(mUser))
				isUser = userId.equals(mUser.getId());
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		if (responseObject == null || responseObject.length() == 0) return;

		switch (operationId) {
			case Constants.SERVER_OP_GET_MY_DIARY_V2:
				try {
					handleListResponse(responseObject);
					resetPositions(false);
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);
		resetPositions(false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_MY_DIARY_V2:
				activityListener.showAlert(R.string.error_generic_update);
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
		resetPositions(false);
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

						Uri u = Uri.parse(mediaFileUri);
						f = new File(u.getPath());
						if (f.exists()) {
							LogUtils.d(LOG_TAG, "Media captured with type=PHOTO and path: " + f.getAbsolutePath()
									+ " and file exists=" + f.exists());
						}
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

								f = new File(Uri.parse(mediaFileUri).getPath());
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


	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		configureToolbar(toolbar);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {

				resetPositions(true);

				Utils.setRefreshingForSwipeLayout(srl, true);

				callForPosts();
			}
		});

		postListsRv = view.findViewById(R.id.diary_rv);
		noResult = view.findViewById(R.id.no_result);
	}

	@Override
	protected void setLayout() {
		if (isUser)
			toolbarTitle.setText(R.string.profile_diary_me);
		else
			toolbarTitle.setText(getString(R.string.title_other_s_diary, Utils.getFirstNameForUI(userName)));

		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);

		postListsRv.setLayoutManager(listsLlm);
		postListsRv.setAdapter(listsAdapter);

		if (postListsToShow == null)
			postListsToShow = new ArrayList<>();
		else
			postListsToShow.clear();

		List<PostList> list = new ArrayList<>(postListsLoaded.values());
		Collections.sort(list, PostList.ListSortOrderComparator);
		postListsToShow.addAll(list);
		listsAdapter.notifyDataSetChanged();

		restorePositions();

		noResult.setVisibility(!postListsToShow.isEmpty() ? View.GONE : View.VISIBLE);
	}

	private void restorePositions() {
		if (scrollPosition != null) {
			listsLlm.scrollToPosition(scrollPosition);
		}
	}

	private void resetPositions(boolean resetScrollViews) {
		scrollPosition = null;

		if (resetScrollViews) {
			if (scrollViewsPositions == null)
				scrollViewsPositions = new SparseArray<>();
			else
				scrollViewsPositions.clear();
		}
	}


	private void configureToolbar(Toolbar toolbar) {
		if (toolbar != null) {
			toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
			toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
			profilePicture = toolbar.findViewById(R.id.profile_picture);
		}
	}

	private void callForPosts() {
		Object[] result = null;
		try {
			if (isUser)
				result = HLServerCalls.getMyDiary(userId, null);
			else
				result = HLServerCalls.getMyDiary(mUser.getUserId(), userId);

		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}


	private void handleListResponse(JSONArray response) throws JSONException {
		PostList list;
		JSONObject lists = response.getJSONObject(0);
		if (lists != null) {

			if (scrollViews == null)
				scrollViews = new SparseArray<>();
			else
				scrollViews.clear();

			Iterator<String> iter = lists.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				list = new PostList().deserializeToClass(lists.getJSONObject(key));
				list.setNameToDisplay(key);

				postListsLoaded.put(list.getName(), list);
			}

			setLayout();
		}
	}

	//endregion



	@Override
	public void goToTimeline(@NonNull String listName, @Nullable String postId) {
		profileActivityListener.showDiaryTimelineFragment(listName, postId, userId, userName, userAvatar);
	}

	@Override
	public void handlePictures(String id, View anchor) {
		if (getActivity() instanceof HLActivity) {
			MediaHelper.openPopupMenu(
					getContext(),
					R.menu.popup_menu_camera,
					anchor,
					new EditPictureMenuItemClickListenerKt(
							((HLActivity) getActivity()),
							mediaCaptureType = id.equals(Constants.DIARY_LIST_ID_PROFILE_PIC) ? HLMediaType.PHOTO_PROFILE : HLMediaType.PHOTO_WALL,
							this,
							this
					)
			);
		}
	}

	@Override
	public HLUser getUser() {
		return mUser;
	}

	@Override
	public void saveScrollView(int position, HorizontalScrollView scrollView) {
		if (scrollViews == null)
			scrollViews = new SparseArray<>();

		scrollViews.put(position, new WeakReference<>(scrollView));
	}

	@Override
	public void restoreScrollView(int position) {
		if (scrollViews != null && scrollViews.size() > 0 &&
				scrollViewsPositions != null && scrollViewsPositions.size() > 0) {
			if (scrollViews.size() >= position) {
				final HorizontalScrollView hsv = scrollViews.get(position).get();
				if (hsv != null) {
					final Integer[] coords = scrollViewsPositions.get(position);
					if (coords != null) {
						new Handler().post(new Runnable() {
							@Override
							public void run() {
								hsv.scrollTo(coords[0], coords[1]);
							}
						});
					}
				}
			}
		}
	}

	@Override
	public void onTargetMediaUriSelect(String mediaFileUri) {
		this.mediaFileUri = mediaFileUri;
	}

	@Override
	public void onUploadComplete(String path, final String mediaLink) {
		if (Utils.isContextValid(getActivity())) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							HLUser mUser = new HLUser().readUser(realm);
							if (!mUser.isActingAsInterest()) {
								if (mediaCaptureType == HLMediaType.PHOTO_PROFILE) {
									mUser.setAvatarURL(userAvatar = mediaLink);
									MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);
								}
								else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
									mUser.setCoverPhotoURL(mediaLink);
							}
							else {
								String id = mUser.getSelectedIdentity().getIdDBObject();
								Interest mInterest = (Interest) RealmUtils.readFirstFromRealmWithId(realm, Interest.class, "_id", id);
								if (mInterest != null) {
									if (mediaCaptureType == HLMediaType.PHOTO_PROFILE) {
										mInterest.setAvatarURL(userAvatar = mediaLink);
										MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);
									}
									else if (mediaCaptureType == HLMediaType.PHOTO_WALL)
										mInterest.setWallPictureURL(mediaLink);
								}
							}
						}
					});

					callForPosts();
				}
			});
		}
	}

	private String uploadType;
	private HLMediaType mediaCaptureType;
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

}