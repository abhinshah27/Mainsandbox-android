/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.connection.realTime.RealTimeCommunicationHelperKotlin;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.FullScreenHelper;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.InteractionPost;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;


public class LandscapeMediaActivity extends HLActivity implements View.OnClickListener,
		GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
		RealTimeCommunicationListener {

	public static final String LOG_TAG = LandscapeMediaActivity.class.getCanonicalName();

	private ImageView mainView;

	private Post item;

	private FullScreenHelper fullScreenListener;

	private GestureDetector mDetector;
	private View maskUpper;
	private ValueAnimator maskUpAlphaOn;
	private ImageView profilePic;
	private TextView userName;
	private TextView cntHeartsUser;
	private TextView timeStamp;

	private View maskLower;
	private ValueAnimator maskLowAlphaOn;
	private TextView postCaption;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landscape_media);

		manageIntent(getIntent());

		// TODO: 12/29/2017     return HERE for fullscreen helper usageMode
		fullScreenListener = new FullScreenHelper(this, false);

		configureLayout();

		mDetector = new GestureDetector(this, this);
		mDetector.setOnDoubleTapListener(this);

		RealTimeCommunicationHelperKotlin.Companion.getInstance(this).setListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerRealTimeReceiver();

		if (mDetector != null)
			mDetector.setOnDoubleTapListener(this);

		setLayout();

		if (item != null) {
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterRealTimeReceiver();
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();

		switch (id) {
			case R.id.main_view:
				break;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		switch (newConfig.orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				returnHome();
				break;
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		if (mDetector != null)
			mDetector.setOnDoubleTapListener(this);
	}

	@Override
	public void onBackPressed() {
		returnHome();
		super.onBackPressed();
	}

	private void manageIntent(Intent intent) {
		if (intent != null) {
			if (intent.hasExtra(Constants.EXTRA_PARAM_1)) {
				String id = intent.getStringExtra(Constants.EXTRA_PARAM_1);
				item = HLPosts.getInstance().getPost(id);
			}
		}
	}

	private void configureLayout() {
		mainView = findViewById(R.id.main_view);
		mainView.setOnClickListener(this);
		mainView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				return mDetector.onTouchEvent(motionEvent);
			}
		});

		maskUpper = findViewById(R.id.timeline_post_mask_upper);
		maskUpper.setAlpha(0f);
		maskUpAlphaOn = fullScreenListener.getMaskAlphaAnimationOn(maskUpper);

		profilePic = findViewById(R.id.profile_picture);
		userName = findViewById(R.id.user_name);
		cntHeartsUser = findViewById(R.id.count_hearts_user);
		timeStamp = findViewById(R.id.time_stamp);

		maskLower = findViewById(R.id.timeline_post_mask_lower);
		maskLower.setAlpha(0f);
		maskLowAlphaOn = fullScreenListener.getMaskAlphaAnimationOn(maskLower);

		View lowerBottom = findViewById(R.id.bottom_section);
		lowerBottom.setVisibility(View.GONE);
		postCaption = findViewById(R.id.post_caption);
	}

	private void setLayout() {
		if (item != null && Utils.isContextValid(mainView.getContext())) {
			MediaHelper.loadPictureWithGlide(this, item.getContent(), mainView);
			userName.setText(item.getAuthor());
			cntHeartsUser.setText(String.valueOf(item.getCountHeartsUser()));
			timeStamp.setText(InteractionPost.getTimeStamp(getResources(), item.getCreationDate()));

			postCaption.setText(item.getCaption());
		}
	}

	private void returnHome() {
		if (Utils.hasLollipop())
			supportFinishAfterTransition();
		else {
			finish();
			overridePendingTransition(0, 0);
		}
	}


	/*
	 * Gestures Listeners
	 */
	@Override
	public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
		returnHome();
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent motionEvent) {
		LogUtils.d(LOG_TAG, "onDoubleTap() CAUGHT");
		// TODO: 10/12/2017    RETURN HERE!!!
//		if (fullScreenListener.getFullScreenType() != FullScreenHelper.FullScreenType.POST_MASK)
//			fullScreenListener.applyPostMaskV2(maskUpAlphaOn, maskLowAlphaOn, maskUpper, null, null);
//		else {
//			fullScreenListener.removePostMask(maskUpper, maskLower);
//		}
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent motionEvent) {
		LogUtils.d(LOG_TAG, "onDoubleTapEvent() CAUGHT");
		return true;
	}

	@Override
	public boolean onDown(MotionEvent motionEvent) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent motionEvent) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent motionEvent) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent motionEvent) {

	}

	@Override
	public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
		return false;
	}

	private class GlideRequestListener implements RequestListener<Drawable> {

		GlideRequestListener() {}

		@Override
		public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
			supportStartPostponedEnterTransition();
			return false;
		}

		@Override
		public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
			supportStartPostponedEnterTransition();
			mainView.getViewTreeObserver().addOnPreDrawListener(
					new ViewTreeObserver.OnPreDrawListener() {
						@Override
						public boolean onPreDraw() {
							mainView.getViewTreeObserver().removeOnPreDrawListener(this);
							if (Utils.hasLollipop()) {
								startPostponedEnterTransition();
							}
							return true;
						}
					});

			return false;
		}
	}


	@Override
	protected void configureResponseReceiver() {

	}

	@Override
	protected void manageIntent() {

	}


	//region == Real-time communication interface methods ==

	@Override
	public void onPostAdded(@NonNull Post post, int position) {}

	@Override
	public void onPostUpdated(@NonNull String postId, int position) {
		// TODO: 11/1/2017   RETURN HERE
	}

	@Override
	public void onPostDeleted(int position) {}

	@Override
	public void onHeartsUpdated(int position) {}

	@Override
	public void onSharesUpdated(int position) {}

	@Override
	public void onTagsUpdated(int position) {}

	@Override
	public void onCommentsUpdated(int position) {}

	@Override
	public void onNewDataPushed(boolean hasInsert) {}

	@Override
	public void registerRealTimeReceiver() {}

	@Override
	public void unregisterRealTimeReceiver() {}

	@Override
	public Realm getRealm() {
		return realm;
	}

	//endregion

}