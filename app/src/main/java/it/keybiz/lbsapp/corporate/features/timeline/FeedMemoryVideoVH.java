/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.video.VideoListener;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.features.viewers.VideoViewActivity;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.caches.AudioVideoCache;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;
import it.keybiz.lbsapp.corporate.widgets.PlayerViewNoController;

/**
 * @author mbaldrighi on 9/28/2017.
 */
public class FeedMemoryVideoVH extends FeedMemoryViewHolder implements VideoListener {

	private PlayerViewNoController mainView;
	private ImageView thumbnailView;
	private View thumbnailViewLayout;

	private View playBtn;
	private View progressView, progressBar;
	private TextView progressMessage;

	private long lastPlayerPosition;

	private boolean wantsLandscape;

	private AsyncTask playTask;
	private Object media;

	boolean playerInError = false;


	FeedMemoryVideoVH(View view, TimelineFragment fragment, OnTimelineFragmentInteractionListener mListener) {
		super(view, fragment, mListener);

		this.mainView = view.findViewById(R.id.video_view);
		thumbnailView = view.findViewById(R.id.video_view_thumbnail);
		thumbnailViewLayout = view.findViewById(R.id.video_view_thumbnail_layout);

		playBtn = view.findViewById(R.id.play_btn);
		progressView = view.findViewById(R.id.progress_layout);
		progressBar = progressView.findViewById(R.id.progress);
		progressMessage = progressView.findViewById(R.id.progress_message);
		progressMessage.setText(R.string.buffering_video);

		playBtn.setOnClickListener(this);

		super.playBtn = this.playBtn;
	}


	public void onBindViewHolder(@NonNull Post object) {
		super.onBindViewHolder(object);

		// INFO: 2/26/19    adds background color handling here due to following edit
//		mainView.setBackgroundColor(mItem.getBackgroundColor(fragment.getResources()));

		// INFO: 2/26/19    restores MATCH_PARENT for both width and height statically in XML
//		mainView.setLayoutParams(
//				new FrameLayout.LayoutParams(
//						mItem.doesMediaWantFitScale() ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT,
//						mItem.doesMediaWantFitScale() ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT,
//						Gravity.CENTER
//				)
//		);

		if (!isPlaying()) {
			if (canAutoPlayVideo()) {
				playBtn.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				progressView.setVisibility(View.VISIBLE);
			}
			else {
				playBtn.setVisibility(View.VISIBLE);
				progressView.setVisibility(View.GONE);
			}
		}

		if (Utils.hasLollipop())
			mainView.setTransitionName(mItem.getContent());


		handleThumbnail();
	}


	@Override
	public void onClick(View view) {
		super.onClick(view);

		if (canProcessEvents && view.getId() == R.id.play_btn) {
			if (canAutoPlayVideo()) {
				// bypass dispatchMediaKeyEvent when autoplay is on
				mainView.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
			}
			else {
				//TODO   understand whether it's possible to apply the same logic to chat or not
//				goToViewer();
			}

			// even without autoplay, now the payback starts when needed
			preparePlayerAndStart(false);
		}
	}


	private void handleThumbnail() {
		thumbnailView.setLayoutParams(
				new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						mItem.doesMediaWantFitScale() ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT,
						Gravity.CENTER
				)
		);

		thumbnailView.setScaleType(mItem.doesMediaWantFitScale() ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);

		thumbnailViewLayout.setBackgroundColor(
				mItem.doesMediaWantFitScale() ?
						mItem.getBackgroundColor(fragment.getResources()) : Utils.getColor(fragment.getResources(), R.color.divider_on_white)
		);

		MediaHelper.loadPictureWithGlide(getActivity(),
				Utils.isStringValid(mItem.getVideoThumbnail()) ? mItem.getVideoThumbnail() : "",
				mItem.doesMediaWantFitScale() ? RequestOptions.fitCenterTransform() : RequestOptions.centerCropTransform(),
				-1,
				-1,
				thumbnailView
		);
	}


	void setLastPlayerPosition() {
		if (mainView != null)
			lastPlayerPosition = mainView.getCurrentPosition();
	}

	private void preparePlayerAndStart(boolean delay) {
		if (mainView != null && itemPosition == super.getAdapterPosition()) {

			if (lastPlayerPosition <= 0)
				thumbnailView.setVisibility(View.VISIBLE);

			long delayMillis = delay ? 700 : 0;

			if (media == null)
				media = AudioVideoCache.Companion.getInstance(fragment.getContext()).getMedia(mItem.getContent(), HLMediaType.VIDEO);

			if (media == null) {
				media = Utils.isStringValid(mItem.getContent()) ? mItem.getContent() : "";
				delayMillis = 0;
			}

			final long delayMillisFinal = delayMillis;

			mainView.postDelayed(
					() -> {
						if (!delay || delayMillisFinal > 0)
							progressView.setVisibility(View.GONE);
						else {
							progressBar.setVisibility(View.VISIBLE);
							progressView.setVisibility(View.VISIBLE);
						}

						if (!mainView.isValid()) {
							MediaHelper.playVideo(mainView.getContext(), mainView, media, true, mItem.doesMediaWantFitScale(), true,
									lastPlayerPosition, new Player.EventListener() {
										@Override
										public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
											switch (playbackState) {

												case Player.STATE_BUFFERING:
													progressMessage.setText(
															mainView.getContext().getString(
																	R.string.buffering_video_perc,
																	String.valueOf(mainView.getBufferedPercentage())
															)
													);
													progressBar.setVisibility(View.VISIBLE);
													progressView.setVisibility(View.VISIBLE);
													playBtn.setVisibility(View.GONE);
													break;

												case Player.STATE_READY:
													progressView.setVisibility(View.GONE);
													playBtn.setVisibility(playWhenReady ? View.GONE : View.VISIBLE);
													thumbnailViewLayout.setVisibility(playWhenReady ? View.GONE : View.VISIBLE);
													break;
											}
										}

										@Override
										public void onPlayerError(ExoPlaybackException error) {
											playerInError = true;
											progressBar.setVisibility(View.GONE);
											progressMessage.setText(R.string.error_playback_video);
											LogUtils.e("ExoPlayer in FEED", "ERROR: " + error.getMessage());
										}
									});
						}
						else mainView.play(false);
					},
					delayMillisFinal
			);

            playBtn.post(()-> playBtn.setVisibility(View.GONE));
		}
	}

	void resumeFromAttach() {
		if (canAutoPlayVideo() && mainView != null) {
			if (mainView.isValid()) {
				mainView.play(true);
			} else {
				preparePlayerAndStart(true);
			}
		}
	}



	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		if (super.onSingleTapConfirmed(e) && !playerInError) {

			goToViewer();

//		ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
//				getActivity(),
//				mainView,
//				ViewCompat.getTransitionName(mainView));
//
//		mMainView.getContext().startActivity(intent, options.toBundle());
			return true;
		}
		return false;
	}


	private void goToViewer() {
		mListener.saveFullScreenState();

		Intent intent = new Intent(getActivity(), VideoViewActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, mItem.getContent());
		intent.putExtra(Constants.EXTRA_PARAM_2, mItem.getVideoThumbnail());
		intent.putExtra(Constants.EXTRA_PARAM_3, ViewCompat.getTransitionName(mainView));
		intent.putExtra(Constants.EXTRA_PARAM_4, lastPlayerPosition);
		intent.putExtra(Constants.EXTRA_PARAM_5, wantsLandscape);
		intent.putExtra(Constants.EXTRA_PARAM_6, mItem.getId());
		intent.putExtra(Constants.EXTRA_PARAM_7, false);
		mMainView.getContext().startActivity(intent);
	}


	private boolean canAutoPlayVideo() {
		return getActivity().getUser().feedAutoplayAllowed(getActivity()) &&
				super.fragment != null && super.fragment.isVisibleToUser() && itemPosition == super.getAdapterPosition();
	}



	//region == Getters and setters ==

	public PlayerViewNoController getMainView() {
		return mainView;
	}

	public ImageView getThumbnailView() {
		return thumbnailView;
	}

	TextView getProgressMessage() {
		return progressMessage;
	}

	void setPlayerInError(boolean playerInError) {
		this.playerInError = playerInError;
	}

	public void setMedia(Object media) {
		this.media = media;
	}

	AsyncTask getPlayTask() {
		return playTask;
	}

	View getPlayBtn() {
		return playBtn;
	}

	public boolean isPlaying() {
		return mainView.isPlaying();
	}

	//endregion

}
