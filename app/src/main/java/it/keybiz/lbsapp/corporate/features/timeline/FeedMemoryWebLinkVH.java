/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.ResizingTextWatcher;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;
import it.keybiz.lbsapp.corporate.widgets.RoundedCornersBackgroundSpan;

/**
 * @author mbaldrighi on 9/28/2017.
 */
class FeedMemoryWebLinkVH extends FeedMemoryViewHolder {

	public static final String LOG_TAG = FeedMemoryWebLinkVH.class.getCanonicalName();

	private static RoundedCornersBackgroundSpan backgrSpan;

	private ImageView mainView;
	private TextView webLinkTitle, webLinkSource;


	FeedMemoryWebLinkVH(View view, TimelineFragment fragment,
						final OnTimelineFragmentInteractionListener mListener) {
		super(view, fragment, mListener);

		mainView = (ImageView) mMainView;

		webLinkTitle = super.maskLower.findViewById(R.id.weblink_title);
		webLinkTitle.setOnClickListener(this);
		webLinkSource = super.maskLower.findViewById(R.id.weblink_source);
		webLinkSource.setOnClickListener(this);

		backgrSpan = new RoundedCornersBackgroundSpan(
				Utils.getColor(getActivity(), R.color.black_70),
				10,
				0
		);
	}


	public void onBindViewHolder(@NonNull Post object) {
		super.onBindViewHolder(object);

		LogUtils.d(LOG_TAG, "onBindViewHolder called for object: " + object.hashCode());

		if (mainView != null && mainView.getContext() != null && super.mItem != null) {
			String url = Utils.isStringValid(super.mItem.getWebLinkImage()) ? super.mItem.getWebLinkImage() : Constants.WEB_LINK_PLACEHOLDER_URL;
			MediaHelper.loadPictureWithGlide(mainView.getContext(),
					url, null, 0, 0, new CustomViewTarget<ImageView, Drawable>(mainView) {
						@Override
						protected void onResourceCleared(@Nullable Drawable placeholder) {
							MediaHelper.loadPictureWithGlide(mainView.getContext(), Constants.WEB_LINK_PLACEHOLDER_URL, getView());
						}

						@Override
						public void onLoadFailed(@Nullable Drawable errorDrawable) {
							MediaHelper.loadPictureWithGlide(mainView.getContext(), Constants.WEB_LINK_PLACEHOLDER_URL, getView());
						}

						@Override
						public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
							mainView.setImageDrawable(resource);
						}
					});

			if (Utils.isStringValid(mItem.getWebLinkTitle())) {
				webLinkTitle.setShadowLayer(10f, 0, 0, Color.TRANSPARENT);
				webLinkTitle.setPadding(10, 10, 10, 10);
				webLinkTitle.setLineSpacing(2, 1);

				Spannable spanned = new SpannableString(mItem.getWebLinkTitle());
				spanned.setSpan(
						backgrSpan,
						0,
						mItem.getWebLinkTitle().length(),
						Spannable.SPAN_INCLUSIVE_EXCLUSIVE
				);
				webLinkTitle.setText(spanned);
				ResizingTextWatcher.resizeTextInView(webLinkTitle);
			}
			else
				webLinkTitle.setText("");

			webLinkSource.setText(mItem.getWebLinkSource());
		}
	}

	@Override
	public void onClick(View view) {
		super.onClick(view);

		if (canProcessEvents) {
			switch (view.getId()) {
				case R.id.weblink_title:
				case R.id.weblink_source:

					if (!Utils.checkAndOpenLogin(getActivity(), getActivity().getUser(), HomeActivity.PAGER_ITEM_HOME_WEBVIEW)) {
						mListener.saveFullScreenState();
						openWebView();
					}
					break;
			}
		}
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		if (super.onSingleTapConfirmed(e)) {
			mListener.saveFullScreenState();
			openWebView();
			return true;
		}
		return false;
	}


	private void openWebView() {
		mListener.setLastAdapterPosition(itemPosition);
		Utils.fireBrowserIntentWithShare(webLinkSource.getContext(), mItem.getWebLinkUrl(), mItem.getAuthor(), mItem.getId(), false);
	}
}