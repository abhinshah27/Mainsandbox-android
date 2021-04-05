/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostHelper;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 3/15/2018.
 */
public class WishCreatePostFragment extends HLFragment implements OnServerMessageReceivedListener,
		OnMissingConnectionListener, OnBackPressedListener, CreatePostHelper.OnPostSavedListener {

	private String mPostToEditId;
	private String mMediaFileUri;
	private PostTypeEnum mPostType;

	private CreatePostHelper createPostHelper;

	private ServerMessageReceiver temporaryReceiver;

	private WishListElement selectedElement;


	public WishCreatePostFragment() {}

	public static WishCreatePostFragment newInstance(String postToEditId, String mediaFileUri, PostTypeEnum postType) {
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, postToEditId);
		args.putString(Constants.EXTRA_PARAM_2, mediaFileUri);
		args.putSerializable(Constants.EXTRA_PARAM_3, postType);
		WishCreatePostFragment fragment = new WishCreatePostFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View mainView = inflater.inflate(R.layout.activity_create_post_new, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		if (createPostHelper == null)
			createPostHelper = new CreatePostHelper(this, CreatePostHelper.ViewType.WISH, mPostToEditId);
		createPostHelper.onCreateView(mainView);

		return mainView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (createPostHelper != null && getActivity() instanceof HLActivity)
			createPostHelper.onActivityCreated((HLActivity) getActivity());
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_CREATE_POST);

		setBackListener();

		wishesActivityListener.setIgnoreTitlesInResponse(true);
		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		if (createPostHelper != null)
			createPostHelper.onResume();

		setLayout();
	}

	@Override
	public void onPause() {
		if (createPostHelper != null)
			createPostHelper.onPause();

		onSaveInstanceState(new Bundle());

		removeBackListener();

		try {
			if (getActivity() != null) {
//				getActivity().unregisterReceiver(temporaryReceiver);
				LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(temporaryReceiver);
				temporaryReceiver = null;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		super.onPause();
	}

	@Override
	public void onStop() {
		if (createPostHelper != null)
			createPostHelper.onStop();

		super.onStop();
	}

	@Override
	public void onDestroy() {
		if (createPostHelper != null)
			createPostHelper.onDestroy();

		super.onDestroy();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				mPostToEditId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				mMediaFileUri = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				mPostType = (PostTypeEnum) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_3);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(Constants.EXTRA_PARAM_1, mPostToEditId);
		outState.putString(Constants.EXTRA_PARAM_2, mMediaFileUri);
		outState.putSerializable(Constants.EXTRA_PARAM_3, mPostType);
	}

	@Override
	protected void configureLayout(@NonNull View view) {}

	@Override
	protected void setLayout() {
		wishesActivityListener.hideTitles();
		wishesActivityListener.hideStepsBar();
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (createPostHelper != null)
			createPostHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (createPostHelper != null)
			createPostHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public void onBackPressed() {

		if (createPostHelper != null)
			createPostHelper.onBackPressed();
	}

	public void setBackListener() {
		if (getActivity() instanceof WishesActivity)
			((WishesActivity) getActivity()).setBackListener(this);
	}

	public void removeBackListener() {
		if (getActivity() instanceof WishesActivity)
			((WishesActivity) getActivity()).setBackListener(null);
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(createPostHelper);

		temporaryReceiver = new ServerMessageReceiver();
		if (Utils.isContextValid(getContext()))
//			getContext().registerReceiver(temporaryReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
			LocalBroadcastManager.getInstance(getContext()).registerReceiver(temporaryReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
		temporaryReceiver.setListener(this);
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (operationId == Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS) {
			if (responseObject == null || responseObject.length() == 0) {
				handleErrorResponse(operationId, 0);
				return;
			}

			JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
			if (json != null && json.length() == 1) {
				selectedElement = new WishListElement().deserializeToClass(json.optJSONObject(0));
			}

			setLayout();
		}

	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		if (operationId == Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS)
			activityListener.showAlert(R.string.error_creating_post);
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	public void onPostSaved(String postId) {
		if (Utils.isStringValid(postId)) {
			Bundle bundle = new Bundle();
			bundle.putString("postID", postId);
			wishesActivityListener.setDataBundle(bundle);

			wishesActivityListener.setSelectedWishListElement(selectedElement);
			wishesActivityListener.resumeNextNavigation();
		}
	}


	//region == Getters and setters ==

	public CreatePostHelper getCreatePostHelper() {
		return createPostHelper;
	}

	//endregion
}
