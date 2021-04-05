/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 11/8/2017.
 */
public class BackgroundHelper {

	private ViewGroup mContainer;
	private ImageView newImageView;
	// TODO: 11/8/2017   other view types here

	private Post mPost;

	public BackgroundHelper(ViewGroup mContainer, @NonNull Post post) {
		this.mContainer = mContainer;
		this.mPost = post;
	}

	public BackgroundHelper(Activity context, @IdRes int viewResId, @NonNull Post post) {
		if (Utils.isContextValid(context)) {
			mContainer = context.findViewById(viewResId);
		}
		this.mPost = post;
	}

	public void configureBackground() {
		if (mPost != null && mContainer != null) {
			switch (mPost.getTypeEnum()) {
				case PHOTO:
				case PHOTO_PROFILE:
				case PHOTO_WALL:
					configureBackgroundPicture();
					break;

				// TODO: 11/8/2017   other cases here
			}
		}
	}

	public void setBackground() {
		if (mPost != null && mContainer != null) {
			// get root_view and set black background
			((Activity) mContainer.getContext()).findViewById(R.id.root_content).setBackgroundColor(Color.BLACK);
			switch (mPost.getTypeEnum()) {
				case PHOTO:
				case PHOTO_PROFILE:
				case PHOTO_WALL:
					setBackgroundPicture();
					break;

				// TODO: 11/8/2017   other cases here
			}
		}
	}

	private void configureBackgroundPicture() {
		newImageView = new ImageView(mContainer.getContext());
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		newImageView.setLayoutParams(lp);
		mContainer.addView(newImageView);
	}

	private void setBackgroundPicture() {
		MediaHelper.loadPictureWithGlide(newImageView.getContext(), mPost.getContent(),newImageView);
	}

	public void setBackgroundVideo() {

	}

	public void setBackgroundText() {

	}

	public void setBackgroundAudio() {

	}


	public View getBackgroundView() {
		if (mPost != null) {
			switch (mPost.getTypeEnum()) {
				case PHOTO:
				case PHOTO_PROFILE:
				case PHOTO_WALL:
					return newImageView;

				// TODO: 11/8/2017   other cases here
			}
		}

		return null;
	}



	//region == Getters and setters ==

	public ViewGroup getContainer() {
		return mContainer;
	}
	public void setContainer(ViewGroup mContainer) {
		this.mContainer = mContainer;
	}

	public Post getPost() {
		return mPost;
	}
	public void setPost(Post post) {
		this.mPost = post;
	}

	//endregion
}
