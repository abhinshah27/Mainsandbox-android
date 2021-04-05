/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.view.View;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;

import java.lang.ref.WeakReference;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.caches.AudioVideoCache;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 9/28/2017.
 */
public class FeedMemoryAudioVH extends FeedMemoryViewHolder {

	private MediaPlayer mediaPlayer;

	private View playBtn;
	private View pauseBtn;
	private View progressView;
	private View pauseLayout;
	private int pauseFrame = 0;

	private boolean playing = false;

	private int lastPlayerPosition;
	private LottieAnimationView animationView;
	private LottieComposition siriComposition;

	private AsyncTask playTask;
	private Object media;


	FeedMemoryAudioVH(View view, TimelineFragment fragment, OnTimelineFragmentInteractionListener mListener) {
		super(view, fragment, mListener);

		mediaPlayer = new MediaPlayer();

		animationView = view.findViewById(R.id.wave3);
		if (getActivity() != null) {
			siriComposition = LBSLinkApp.siriComposition;
		}
		if (animationView != null && siriComposition != null)
			animationView.setComposition(siriComposition);

		playBtn = view.findViewById(R.id.play_btn);
		pauseBtn = view.findViewById(R.id.pause_btn);
		progressView = view.findViewById(R.id.progress_layout);
		pauseLayout = view.findViewById(R.id.pause_layout);
		playBtn.setOnClickListener(this);
		pauseLayout.setOnClickListener(this);

		super.playBtn = playBtn;
		super.pauseBtn = pauseLayout;
	}


	public void onBindViewHolder(@NonNull Post object) {
		super.onBindViewHolder(object);

		// TODO: 5/6/2018    DISABLES AUTOPLAY AUDIO
//		if (getActivity().getUser().feedAutoplayAllowed(getActivity()) &&
//				super.fragment != null && super.fragment.isVisibleToUser()) {
//
//			if (mediaPlayer == null)
//				mediaPlayer = new MediaPlayer();
//
//			if (!mediaPlayer.isPlaying()) {
//				playBtn.setVisibility(View.GONE);
//				pauseBtn.setVisibility(View.GONE);
//				progressView.setVisibility(View.GONE);
//
//				playTask = new PlayAudioTask(mediaPlayer, playBtn, pauseBtn, progressView, playing,
//						lastPlayerPosition, animationView, pauseFrame)
//						.execute(mItem.getContent());
//			}
//		}
//		else {
			playBtn.setVisibility(View.VISIBLE);
			pauseBtn.setVisibility(View.GONE);
			progressView.setVisibility(View.GONE);
//		}


		//TO MAKE SURE WAVE IS SET TO 0 WHEN VIEW IS ATTACHED.
		animationView.setFrame(0);
	}


	@Override
	public void onClick(View view) {
		super.onClick(view);

		if (canProcessEvents) {
			if (view.getId() == R.id.play_btn) {
				if (Utils.isStringValid(mItem.getContent())) preparePlayerAndStart();
			} else if (view.getId() == R.id.pause_layout) {
				if (Utils.isStringValid(mItem.getContent())) {
					if (mediaPlayer != null && mediaPlayer.isPlaying()) {
						mediaPlayer.pause();
						pauseLayout.setVisibility(View.GONE);
						pauseBtn.setVisibility(View.GONE);
						playing = false;
						playBtn.setVisibility(View.VISIBLE);
						animationView.pauseAnimation();
						pauseFrame = animationView.getFrame();

					}
//					else {
//						return;
//					}
				}
			}
		}
	}


	void setLastPlayerPosition() {
		if (mediaPlayer != null)
			lastPlayerPosition = mediaPlayer.getCurrentPosition();
	}

	private void preparePlayerAndStart() {
		media = AudioVideoCache.Companion.getInstance(fragment.getContext()).getMedia(mItem.getContent(), HLMediaType.AUDIO);
		if (media == null)
			media = Utils.isStringValid(mItem.getContent()) ? mItem.getContent() : "";

		playTask = MediaHelper.getAudioTask(mediaPlayer != null ? mediaPlayer : new MediaPlayer(), playBtn, pauseBtn,
				progressView, playing, lastPlayerPosition, animationView, pauseFrame)
				.execute(media);

		if (pauseFrame != 0){
			animationView.setFrame(pauseFrame);
			animationView.resumeAnimation();
		}

		pauseBtn.setVisibility(View.VISIBLE);
		pauseLayout.setVisibility(View.VISIBLE);


		/*
		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
			try {
				mediaPlayer.prepare();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
//		if (mediaPlayer != null) {

		mediaPlayer.seekTo(lastPlayerPosition);
		playBtn.setVisibility(View.GONE);
		pauseLayout.setVisibility(View.VISIBLE);
		mediaPlayer.start();

		if (pauseFrame != 0)
			animationView.resumeAnimation();
		else
			animationView.playAnimation();

//		}
		*/

	}


	static class PlayAudioTask extends AsyncTask<String, Void, Boolean> {

		private MediaPlayer mediaPlayer;
		private WeakReference<View> playBtn, pauseBtn;
		private WeakReference<View> progressView;
		private WeakReference<LottieAnimationView> siriAnimation;

		private Boolean playing;
		private Integer lastPlayerPosition;

		int pauseFrame;

		PlayAudioTask(MediaPlayer mediaPlayer, View playBtn, View pauseBtn, View progressView,
		              boolean playing, int lastPlayerPosition, LottieAnimationView siriAnimation, int pauseFrame) {

			this.mediaPlayer = mediaPlayer;
			this.playBtn = new WeakReference<>(playBtn);
			this.pauseBtn = new WeakReference<>(pauseBtn);
			this.progressView = new WeakReference<>(progressView);
			this.playing = playing;
			this.lastPlayerPosition = lastPlayerPosition;
			this.siriAnimation = new WeakReference<>(siriAnimation);
			this.pauseFrame = pauseFrame;
		}

		@Override
		protected Boolean doInBackground(String... strings) {
			Boolean prepared = null;

			if (mediaPlayer != null) {
				try {
					mediaPlayer.setDataSource(strings[0]);
					mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mediaPlayer) {
							playBtn.get().setVisibility(View.VISIBLE);
							pauseBtn.get().setVisibility(View.GONE);

							mediaPlayer.stop();
							mediaPlayer.reset();
							siriAnimation.get().cancelAnimation();
							siriAnimation.get().setFrame(0);

							playing = false;
							lastPlayerPosition = 0;
						}
					});

					mediaPlayer.prepare();
					prepared = true;

				} catch (Exception e) {
					LogUtils.e("MyAudioStreamingApp", Utils.isStringValid(e.getMessage()) ?
							e.getMessage() : "");
					prepared = false;
				}
			}

			return prepared;
		}

		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);

			progressView.get().setVisibility(View.GONE);

			if (mediaPlayer != null) {
				mediaPlayer.start();
				pauseBtn.get().setVisibility(View.VISIBLE);
				if (pauseFrame == 0 ){
					siriAnimation.get().playAnimation();
				}
			}

			playing = true;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			playBtn.get().setVisibility(View.GONE);
			progressView.get().setVisibility(View.VISIBLE);

			siriAnimation.get().setFrame(0);

			if (mediaPlayer.isPlaying()){
				playBtn.get().setVisibility(View.GONE);
			}
			else {
				siriAnimation.get().cancelAnimation();
			}
		}
	}


	//region == Getters and setters ==

	MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}
	public void setMediaPlayer(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	public AsyncTask getPlayTask() {
		return playTask;
	}

	public View getPlayBtn() {
		return playBtn;
	}

	public boolean isPlaying() {
		return mediaPlayer != null && mediaPlayer.isPlaying();
	}

	//endregion

}
