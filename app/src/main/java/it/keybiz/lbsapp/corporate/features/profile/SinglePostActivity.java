/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONArray;

import java.util.ArrayList;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.tags.ViewAllTagsActivity;
import it.keybiz.lbsapp.corporate.features.timeline.OnTimelineFragmentInteractionListener;
import it.keybiz.lbsapp.corporate.features.timeline.TimelineFragment;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.FullScreenHelper;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.PostOverlayActionActivity;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.FragmentsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 12/12/2017.
 */
public class SinglePostActivity extends HLActivity implements OnTimelineFragmentInteractionListener,
		BasicInteractionListener, OnMissingConnectionListener, OnBackPressedListener {

	private MediaHelper mediaHelper;
	private String postId;

	private FullScreenHelper fullScreenListener;
	private FullScreenHelper.RestoreFullScreenStateListener fsStateListener;
	private FullScreenHelper.FullScreenType fullScreenSavedState;

//	private SlidingUpPanelLayout bottomSheetPostOwn;
//	private SlidingUpPanelLayout bottomSheetPostOther;

	private OnBackPressedListener backListener;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_single_post);
		setRootContent(R.id.root_content);

		mediaHelper = new MediaHelper();
		fullScreenListener = new FullScreenHelper(this, false);

		manageIntent();
		showTimelineFragment();

//		configurePostOptionsSheets();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(Constants.EXTRA_PARAM_1))
				postId = intent.getStringExtra(Constants.EXTRA_PARAM_1);
		}
	}

	private void showTimelineFragment() {
		if (Utils.isStringValid(postId)) {
			FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
			TimelineFragment fragment = TimelineFragment.newInstance(TimelineFragment.FragmentUsageType.SINGLE,
					null, null, null, postId, null, null);
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.root_content, fragment,
					TimelineFragment.LOG_TAG, null, Constants.NO_RESULT);
			fragmentTransaction.commit();
		}
	}

	@Override
	public void onBackPressed() {
		setResult(RESULT_OK);
		finish();
		overridePendingTransition(R.anim.no_animation, R.anim.slide_out_right);
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);


	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			// caught when returning from PostOverlayActivity
			case Constants.RESULT_TIMELINE_INTERACTIONS:
				restoreFullScreenState();
				break;
		}
	}

	private void restoreFullScreenState() {
		if (fsStateListener != null)
			fsStateListener.restoreFullScreenState(fullScreenSavedState);
	}

	/*
	private void configurePostOptionsSheets() {
		bottomSheetPostOwn = findViewById(R.id.sliding_post_own);
		if (bottomSheetPostOwn != null) {
			bottomSheetPostOwn.setTouchEnabled(true);
			bottomSheetPostOwn.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
				@Override
				public void onPanelSlide(View panel, float slideOffset) {}

				@Override
				public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {}
			});

			bottomSheetPostOwn.findViewById(R.id.item_edit).setOnClickListener(this);
			bottomSheetPostOwn.findViewById(R.id.item_delete).setOnClickListener(this);
		}

		bottomSheetPostOther = findViewById(R.id.sliding_post_other);
		if (bottomSheetPostOther != null) {
			bottomSheetPostOther.setTouchEnabled(true);
			bottomSheetPostOther.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
				@Override
				public void onPanelSlide(View panel, float slideOffset) {}

				@Override
				public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {}
			});

			bottomSheetPostOther.findViewById(R.id.item_hide).setOnClickListener(this);
			bottomSheetPostOther.findViewById(R.id.item_report).setOnClickListener(this);
			bottomSheetPostOther.findViewById(R.id.item_block).setOnClickListener(this);
		}
	}
	*/


	//region == Timeline interface ==

//	@Override
//	public void onManageTapForFullScreen(View clickedView, ValueAnimator maskOnUp, ValueAnimator maskOnLow, ValueAnimator maskOffUp, ValueAnimator maskOffLow) {
//
//	}

	@Override
	public void actionsForLandscape(@NonNull String postId, View view) {

		// TODO: 12/14/2017     needs implementation

	}

	@Override
	public void setLastAdapterPosition(int position) {}

	@Override
	public Integer getLastAdapterPosition() {
		return null;
	}

	@Override
	public FullScreenHelper getFullScreenListener() {
		return fullScreenListener;
	}

	@Override
	public Toolbar getToolbar() {
		return null;
	}

	@Override
	public View getBottomBar() {
		return null;
	}

	@Override
	public MediaHelper getMediaHelper() {
		if (mediaHelper == null)
			mediaHelper = new MediaHelper();

		return mediaHelper;
	}

	@Override
	public void goToInteractionsActivity(@NonNull String postId) {
		Intent intent = new Intent(this, PostOverlayActionActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, postId);
		startActivityForResult(intent, Constants.RESULT_TIMELINE_INTERACTIONS);
	}


	@Override
	public void saveFullScreenState() {
		fullScreenSavedState = fullScreenListener.getFullScreenType();
	}

	@Override
	public int getPageIdToCall() {
		return 0;
	}

	@Override
	public int getLastPageID() {
		return 0;
	}

	@Override
	public void setLastPageID(int lastPageID) {}

	@Override
	public HLActivity getActivity() {
		return this;
	}

	@Override
	public void setFsStateListener(FullScreenHelper.RestoreFullScreenStateListener fsStateListener) {
		this.fsStateListener = fsStateListener;
	}

	@Override
	public void viewAllTags(Post post) {
		if (post != null) {
			if (post.hasTags()) {
				ArrayList<Tag> temp = new ArrayList<>();
				temp.addAll(post.getTags());

				Intent intent = new Intent(this, ViewAllTagsActivity.class);
				intent.putParcelableArrayListExtra(Constants.EXTRA_PARAM_1, temp);
				startActivity(intent);
			}
		}
	}

	//region ++ Post Bottom Sheet section ++

	@Override public void openPostSheet(@NonNull String postId, boolean isOwnPost) {
		TimelineFragment fragment = (TimelineFragment) getSupportFragmentManager().findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				fragment.getBottomSheetHelper().openPostSheet(postId, isOwnPost);
		}
	}

	@Override
	public void closePostSheet() {
		TimelineFragment fragment = (TimelineFragment) getSupportFragmentManager().findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				fragment.getBottomSheetHelper().closePostSheet();
		}
	}

	@Override
	public boolean isPostSheetOpen() {
		TimelineFragment fragment = (TimelineFragment) getSupportFragmentManager().findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				return fragment.getBottomSheetHelper().isPostSheetOpen();
		}

		return false;
	}

	@Override
	public void goToProfile(@NonNull String userId, boolean isInterest) {
		if (mUser.getId().equals(userId)) {
			Intent intent = new Intent(this, HomeActivity.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
			startActivity(intent);
			finish();
		}
		else {
			ProfileHelper.ProfileType type = isInterest ? ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND;
			ProfileActivity.openProfileCardFragment(this, type, userId, HomeActivity.PAGER_ITEM_PROFILE);
		}
	}

	//endregion

	//endregion

}
