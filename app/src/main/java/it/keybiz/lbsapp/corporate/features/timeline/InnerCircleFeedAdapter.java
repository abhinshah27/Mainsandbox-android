/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.globalSearch.GlobalSearchActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.profile.SinglePostActivity;
import it.keybiz.lbsapp.corporate.features.settings.SettingsActivity;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp;
import it.keybiz.lbsapp.corporate.widgets.PlayerViewNoController;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Post} and makes a call to the
 * specified {@link OnTimelineFragmentInteractionListener}.
 */
public class InnerCircleFeedAdapter extends RecyclerView.Adapter<FeedMemoryViewHolder> {

	public static final int TYPE_PICTURE = 0;
	public static final int TYPE_VIDEO = 1;
	private static final int TYPE_AUDIO = 2;
	private static final int TYPE_TEXT = 3;
	private static final int TYPE_WEBLINK = 4;

	private final List<Post> mValues;
	private final OnTimelineFragmentInteractionListener mListener;
	private final HLActivity activity;

	private TimelineFragment fragment;

	private AsyncTask audioPlayTask, videoPlayTask;
	private MediaPlayer mediaPlayer;
	private PlayerViewNoController videoView;
	private View playBtn;


	public InnerCircleFeedAdapter(TimelineFragment fragment, List<Post> items, OnTimelineFragmentInteractionListener listener) {
		this.fragment = fragment;
		activity = setActivity();
		mValues = items;
		mListener = listener;
	}

	@NonNull
	@Override
	public FeedMemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_text, parent, false);

		switch (viewType) {
			case TYPE_AUDIO:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_audio, parent, false);
				return new FeedMemoryAudioVH(view, fragment, mListener);
			case TYPE_PICTURE:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_picture, parent, false);
				return new FeedMemoryPictureVH(view, fragment, mListener);
			case TYPE_TEXT:
				return new FeedMemoryTextVH(view, fragment, mListener);
			case TYPE_VIDEO:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_video_to_use, parent, false);
				return new FeedMemoryVideoVH(view, fragment, mListener);
			case TYPE_WEBLINK:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_weblink, parent, false);
				return new FeedMemoryWebLinkVH(view, fragment, mListener);
		}

		return new FeedMemoryTextVH(view, fragment, mListener);
	}

	@Override
	public void onBindViewHolder(@NonNull final FeedMemoryViewHolder holder, int position) {
		if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
			Post mItem = mValues.get(position);

			if (mItem == null) return;

			holder.setItemPosition(position);
			holder.onBindViewHolder(mItem);

//			if (holder instanceof FeedMemoryAudioVH)
//				holder.onBindViewHolder(mItem);
//			else if (holder instanceof FeedMemoryPictureVH)
//				holder.onBindViewHolder(mItem);
//			else if (holder instanceof FeedMemoryTextVH)
//				holder.onBindViewHolder(mItem);
//			else if (holder instanceof FeedMemoryVideoVH)
//				holder.onBindViewHolder(mItem);
//			else if (holder instanceof FeedMemoryWebLinkVH)
//				holder.onBindViewHolder(mItem);
		}
	}

	@Override
	public int getItemCount() {
		return mValues.size();
	}

	@Override
	public int getItemViewType(int position) {
		Post p = mValues.get(position);
		if (p != null) {
			if (p.isAudioPost())
				return TYPE_AUDIO;
			else if (p.isPicturePost())
				return TYPE_PICTURE;
			else if (p.isVideoPost())
				return TYPE_VIDEO;
			else if (p.isTextPost())
				return TYPE_TEXT;
			else if (p.isWebLinkPost())
				return TYPE_WEBLINK;
		}
		return super.getItemViewType(position);
	}

	@Override
	public long getItemId(int position) {
		if (mValues != null && !mValues.isEmpty()) {
			Post p = mValues.get(position);
			if (p != null)
				return p.hashCode();
		}
		return 0;
	}

	@Override
	public void onViewAttachedToWindow(@NonNull FeedMemoryViewHolder holder) {
		super.onViewAttachedToWindow(holder);

		int pos = holder.getAdapterPosition();
		if (pos > -1 && mValues != null && mValues.size() >= pos) {
			mListener.setLastAdapterPosition(pos);
			holder.setLastAdapterItemId(mValues.get(pos));
			holder.handleTimeStamp(mValues.get(pos));
			mListener.setFsStateListener(holder);
		}

		if (Utils.isContextValid(activity) && fragment.isVisibleToUser()) {

			// TODO: 5/6/2018    DISABLES AUTOPLAY AUDIO
//			if (holder instanceof FeedMemoryAudioVH) {
//				((FeedMemoryAudioVH) holder).preparePlayerAndStart();
//
//				setAudioPlayTask(((FeedMemoryAudioVH) holder).getPlayTask());
//				setMediaPlayer(((FeedMemoryAudioVH) holder).getMediaPlayer());
//				setPlayBtn(((FeedMemoryAudioVH) holder).getPlayBtn());
//			}
//			else

			// resets properties.
			videoPlayTask = null; videoView = null; playBtn = null;

			if (holder instanceof FeedMemoryVideoVH  && fragment.isVisibleToUser()) {
                if (activity.getUser().feedAutoplayAllowed(activity))
                    ((FeedMemoryVideoVH) holder).resumeFromAttach();

				videoPlayTask = ((FeedMemoryVideoVH) holder).getPlayTask();
				videoView = ((FeedMemoryVideoVH) holder).getMainView();
				playBtn = ((FeedMemoryVideoVH) holder).getPlayBtn();
			}
		}
	}

	@Override
	public void onViewDetachedFromWindow(@NonNull FeedMemoryViewHolder holder) {
		super.onViewDetachedFromWindow(holder);

		if (holder instanceof FeedMemoryAudioVH) {
			if (((FeedMemoryAudioVH) holder).isPlaying() && ((FeedMemoryAudioVH) holder).getMediaPlayer() != null) {
				((FeedMemoryAudioVH) holder).getMediaPlayer().stop();
				((FeedMemoryAudioVH) holder).setLastPlayerPosition();
			}

//			setAudioPlayTask(((FeedMemoryAudioVH) holder).getPlayTask());
//			setMediaPlayer(((FeedMemoryAudioVH) holder).getMediaPlayer());
//			setPlayBtn(((FeedMemoryAudioVH) holder).getPlayBtn());
		}
		else if (holder instanceof FeedMemoryVideoVH) {
			if (((FeedMemoryVideoVH) holder).isPlaying() && ((FeedMemoryVideoVH) holder).getMainView() != null) {
				((FeedMemoryVideoVH) holder).getMainView().pause();
				((FeedMemoryVideoVH) holder).setLastPlayerPosition();

				((FeedMemoryVideoVH) holder).setMedia(null);
//				((FeedMemoryVideoVH) holder).getThumbnailView().setImageResource(R.color.transparent);
			}

//			setVideoPlayTask(((FeedMemoryVideoVH) holder).getPlayTask());
//			setVideoView(((FeedMemoryVideoVH) holder).getMainView());
//			setPlayBtn(((FeedMemoryVideoVH) holder).getPlayBtn());
		}
	}

	@Override
	public void onViewRecycled(@NonNull FeedMemoryViewHolder holder) {
		super.onViewRecycled(holder);

		holder.resetMoreTextVisibility( );

		if (holder instanceof FeedMemoryAudioVH) {
			if (((FeedMemoryAudioVH) holder).getPlayTask() != null &&
					!((FeedMemoryAudioVH) holder).getPlayTask().isCancelled())
				((FeedMemoryAudioVH) holder).getPlayTask().cancel(true);

			MediaPlayer mp = ((FeedMemoryAudioVH) holder).getMediaPlayer();
			if (mp != null) {
				if (mp.isPlaying())
					mp.stop();

				mp.release();
				((FeedMemoryAudioVH) holder).setMediaPlayer(null);
			}
		}
		else if (holder instanceof FeedMemoryVideoVH) {
			if (((FeedMemoryVideoVH) holder).getPlayTask() != null &&
					!((FeedMemoryVideoVH) holder).getPlayTask().isCancelled())
				((FeedMemoryVideoVH) holder).getPlayTask().cancel(true);

			((FeedMemoryVideoVH) holder).setMedia(null);
			((FeedMemoryVideoVH) holder).getThumbnailView().setImageResource(R.color.transparent);
			((FeedMemoryVideoVH) holder).getProgressMessage().setText(R.string.buffering_video);
			((FeedMemoryVideoVH) holder).setPlayerInError(false);

			PlayerViewNoController vv = ((FeedMemoryVideoVH) holder).getMainView();
			if (vv != null) {
				if (vv.isPlaying())
					vv.pause();

				vv.stopPlayback();
			}
		}
		else if (holder instanceof FeedMemoryPictureVH) {
			if (holder.mMainView instanceof ImageView)
				GlideApp.with(holder.mMainView).clear(holder.mMainView);
		}
	}

	private HLActivity setActivity() {
		if (fragment != null) {
			Activity activity = fragment.getActivity();
			if (activity instanceof HomeActivity)
				return ((HomeActivity) activity);
			else if (activity instanceof SinglePostActivity)
				return ((SinglePostActivity) activity);
			else if (activity instanceof ProfileActivity)
				return ((ProfileActivity) activity);
			else if (activity instanceof SettingsActivity)
				return ((SettingsActivity) activity);
			else if (activity instanceof GlobalSearchActivity)
				return ((GlobalSearchActivity) activity);
		}
		return null;
	}


	public void cleanAdapterMediaControllers(boolean release) {
			if (audioPlayTask != null && !audioPlayTask.isCancelled())
				audioPlayTask.cancel(true);

			if (mediaPlayer != null) {
				try {
					if (mediaPlayer.isPlaying())
						mediaPlayer.stop();

					if (release) {
						mediaPlayer.release();
						mediaPlayer = null;
					}
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			}


			if (videoPlayTask != null && !videoPlayTask.isCancelled())
				videoPlayTask.cancel(true);

			if (videoView != null) {
				try {
					if (videoView.isPlaying())
						videoView.pause();
					if (release)
					    videoView.stopPlayback();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (playBtn != null) {
				playBtn.post(
						() -> playBtn.setVisibility(View.VISIBLE)
				);
			}
	}

	public void resumeVideo() {
		if (videoView != null && videoView.isValid()) {
			videoView.play(true);
			playBtn.setVisibility(View.GONE);
		}
	}
	


	//region == Getters and setters ==

	//endregion

}
