/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.createPost;

import android.content.Intent;
import android.os.Bundle;
import android.transition.Explode;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.Initiative;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.CustomGalleryAdapter;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;

/**
 * @author mbaldrighi on 3/16/2018.
 */
public class CreatePostActivityMod extends HLActivity implements GalleryFragment.OnGalleryFragmentInteractionListener,
		CustomGalleryAdapter.OnMediaClickListener, AudioRecordFragment.OnAudioRecordFragmentInteractionListener,
		TagFragment.OnTagFragmentInteractionListener, InitiativesFragment.OnInitiativesFragmentInteractionListener,
		View.OnClickListener, BasicInteractionListener {

	private boolean redirectToHome;
	private String mPostToEditId;

	private CreatePostHelper createPostHelper;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View mainView = getLayoutInflater().inflate(R.layout.activity_create_post_new, null, false);
		setContentView(mainView);
		setProgressIndicator(R.id.generic_progress_indicator);

		/* SETS ENTER EXPLODE TRANSITION */
		if (Utils.hasLollipop()) {
			getWindow().setEnterTransition(new Explode());
			getWindow().setExitTransition(new Explode());
		}

		manageIntent();

		createPostHelper = new CreatePostHelper(this, null, CreatePostHelper.ViewType.NORMAL, mPostToEditId);
		createPostHelper.setRedirectToHome(redirectToHome);
		createPostHelper.onCreate(mainView);
	}

	@Override
	protected void onStart() {
		super.onStart();
		configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.CREATE_POST);

		if (createPostHelper != null)
			createPostHelper.onResume();
	}

	@Override
	protected void onPause() {
		if (createPostHelper != null)
			createPostHelper.onPause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (createPostHelper != null)
			createPostHelper.onStop();

		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (createPostHelper != null)
			createPostHelper.onDestroy();

		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() != R.id.generic_progress_indicator && createPostHelper != null)
			createPostHelper.onClick(v);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(createPostHelper);
	}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent.hasExtra(Constants.EXTRA_PARAM_1))
			mPostToEditId = intent.getStringExtra(Constants.EXTRA_PARAM_1);
		if (intent.hasExtra(Constants.EXTRA_PARAM_2))
			redirectToHome = intent.getBooleanExtra(Constants.EXTRA_PARAM_2, false);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (createPostHelper != null)
			createPostHelper.onTouchEvent(event);
		return super.onTouchEvent(event);
	}


	//region == Fragments' interfaces ==

	// GALLERY
	@Override
	public void setPostType(PostTypeEnum type) {
		if (createPostHelper != null)
			createPostHelper.setPostType(type);
	}

	@Override
	public void setMediaCaptureType(HLMediaType type) {
		if (createPostHelper != null)
			createPostHelper.setMediaCaptureType(type);
	}

	@Override
	public void checkPermissionForGallery(HLMediaType type) {
		if (createPostHelper != null)
			createPostHelper.checkPermissionForGallery(type);
	}

	@Override
	public void onClickImage(String imageUri) {
		if (createPostHelper != null)
			createPostHelper.onClickImage(imageUri);
	}

	@Override
	public void onClickVideo(String videoUri) {
		if (createPostHelper != null)
			createPostHelper.onClickVideo(videoUri);
	}



	// AUDIO RECORDING
	@NonNull
	@Override
	public String getAudioMediaFileUri() {
		if (createPostHelper != null)
			return createPostHelper.getAudioMediaFileUri();

		return "";
	}

	@Override
	public void exitFromRecordingAndSetAudioBackground() {
		if (createPostHelper != null)
			createPostHelper.exitFromRecordingAndSetAudioBackground();
	}

	@Override
	public boolean isEditAudioPost() {
		return createPostHelper != null && createPostHelper.isEditAudioPost();
	}

	@NonNull
	@Override
	public String getAudioUrl() {
		if (createPostHelper != null)
			return createPostHelper.getAudioUrl();

		return "";
	}


	// TAG
	@Override
	public Object[] isObjectForTagSelected(String id, boolean fromInitiative) {
		if (createPostHelper != null)
			return createPostHelper.isObjectForTagSelected(id, fromInitiative);

		return null;
	}

	@Override
	public BasicAdapterInteractionsListener getCreatePostHelper() {
		if (createPostHelper != null)
			return createPostHelper.getCreatePostHelper();

		return null;
	}

	@Override
	public void addTagToSearchList(Tag tag) {
		if (createPostHelper != null)
			createPostHelper.addTagToSearchList(tag);
	}

	@Override
	public void updateSearchData(String query) {
		if (createPostHelper != null)
			createPostHelper.updateSearchData(query);
	}


	// INITIATIVE
	@Override
	public void attachInitiativeToPost(Initiative initiative) {
		if (createPostHelper != null)
			createPostHelper.attachInitiativeToPost(initiative);
	}

	@Override
	public CreatePostHelper getHelperObject() {
		if (createPostHelper != null)
			return createPostHelper.getHelperObject();

		return null;
	}

	@Override
	public void showHideInitiativeLabel(boolean show, @Nullable String text) {
		if (createPostHelper != null)
			createPostHelper.showHideInitiativeLabel(show, text);
	}

	@Override
	public void updateVisibilityForInitiative() {
		if (createPostHelper != null)
			createPostHelper.updateVisibilityForInitiative();
	}

	//endregion

}