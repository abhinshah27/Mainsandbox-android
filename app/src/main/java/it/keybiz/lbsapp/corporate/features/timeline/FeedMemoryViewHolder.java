/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.globalSearch.GlobalSearchActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.profile.SinglePostActivity;
import it.keybiz.lbsapp.corporate.features.settings.SettingsActivity;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.FullScreenHelper;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.InteractionsViewerActivity;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.TextViewActivity;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.InteractionPost;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.models.enums.InteractionTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;
import it.keybiz.lbsapp.corporate.widgets.AutoEllipsizingTextView;
import kotlin.Triple;

/**
 * @author mbaldrighi on 9/28/2017.
 */
public class FeedMemoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
		GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
		TimelineFragment.OnConfigChangedAction,
		FullScreenHelper.RestoreFullScreenStateListener {

	final View mMainView;

	private final View maskContainer;
	private final View maskUpper, maskUpperHide;
	final View maskLower;
	private final View maskLowerHide;
	private final CircleImageView profilePic;
	private final TextView userName;
	private final TextView cntHeartsUser;
	private final TextView timeStamp;
	private final View sharedVia;
	private final TextView sharedViaName;
	private final AutoEllipsizingTextView postCaption;
	private final TextView viewMoreText;
	private final View bottomSectionLower;
	private final TextView cntHeartsPost;
	private final TextView cntCommentsPost;
	private final TextView cntSharesPost;
	private final View btnHearts;
	private final View btnComments;
	private final View btnShares;

	private final ImageView pinIcon;

	private final View initiativesLayout, initiativesIcon;
	private final TextView initiativesText;

	private final View tagsLayout, moreTags;
	private final TextView viewTags;
	private final View tag2L;
	private final View tag3L;
	private final View tag4L;
	private final View tag5L;
	private final ImageView tag1, tag2, tag3, tag4, tag5;

	private final ImageView privacyIcon;

	private final ValueAnimator maskUpAlphaOn, maskUpAlphaOnTotal;
	private final ValueAnimator maskUpAlphaOff, maskUpAlphaOffTotal;
	private final ValueAnimator maskLowAlphaOn, maskLowAlphaOnTotal;
	private final ValueAnimator maskLowAlphaOff, maskLowAlphaOffTotal;
	private final ValueAnimator maskUpShift;
	private final ValueAnimator maskLowShift;

	protected final OnTimelineFragmentInteractionListener mListener;
	protected TimelineFragment fragment;
	private Resources resources;
	private FullScreenHelper fullScreenHelper;

	Post mItem;
	Integer itemPosition;
	private GestureDetector mDetector;

	View playBtn, pauseBtn;

	private View rootView;

	private boolean moreTextVisible = false;

	boolean canProcessEvents = false;


	FeedMemoryViewHolder(View view, TimelineFragment fragment,
	                     OnTimelineFragmentInteractionListener mListener) {
		super(view);

		this.rootView = view;

		this.mListener = mListener;
		this.fragment = fragment;
		if (mListener.getActivity() != null) {
			this.fullScreenHelper = mListener.getFullScreenListener();
			this.resources = mListener.getResources();
		}

		mDetector = new GestureDetector(getActivity(), this);
		mDetector.setOnDoubleTapListener(this);

		mMainView = view.findViewById(R.id.post_main_view);

		maskContainer = view.findViewById(R.id.mask_container);
		maskUpper = view.findViewById(R.id.timeline_post_mask_upper);
		maskUpperHide = maskUpper.findViewById(R.id.hideable_layout);
		maskUpAlphaOn = getAlphaAnimations(true, true, false);
		maskUpAlphaOnTotal = getAlphaAnimations(true, true, true);
		maskUpAlphaOff = getAlphaAnimations(true, false, false);
		maskUpAlphaOffTotal = getAlphaAnimations(true, false, true);
		maskUpShift = getMaskTranslation(maskUpper);

		View picLayout = maskUpper.findViewById(R.id.profile_picture);
		profilePic = picLayout.findViewById(R.id.pic);
		userName = maskUpper.findViewById(R.id.user_name);
		cntHeartsUser = maskUpper.findViewById(R.id.count_hearts_user);
		timeStamp = maskUpper.findViewById(R.id.time_stamp);
		sharedVia = maskUpper.findViewById(R.id.shared_via);
		sharedViaName = maskUpper.findViewById(R.id.shared_name);

		View moreOptions = maskUpper.findViewById(R.id.more_options);

		maskLower = view.findViewById(R.id.timeline_post_mask_lower);
		maskLowerHide = maskLower.findViewById(R.id.hideable_layout);
		maskLowAlphaOn = getAlphaAnimations(false, true, false);
		maskLowAlphaOnTotal = getAlphaAnimations(false, true, true);
		maskLowAlphaOff = getAlphaAnimations(false, false, false);
		maskLowAlphaOffTotal = getAlphaAnimations(false, false, true);
		maskLowShift = getMaskTranslation(maskLower);

		postCaption = maskLower.findViewById(R.id.post_caption);
		postCaption.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {

				int visibleLines = Utils.calculateVisibleTextLines(postCaption);
				postCaption.setMaxLines(visibleLines);
				postCaption.setEllipsize(TextUtils.TruncateAt.END);

				int ellipsis = postCaption.getEllipsisCount();
				viewMoreText.setVisibility(ellipsis > 0 ? View.VISIBLE : View.GONE);

				moreTextVisible = ellipsis > 0;
			}
		});
		viewMoreText = maskLower.findViewById(R.id.view_more_text);

		bottomSectionLower = maskLower.findViewById(R.id.bottom_section);
		cntHeartsPost = maskLower.findViewById(R.id.count_hearts_post);
		cntCommentsPost = maskLower.findViewById(R.id.count_comments);
		cntSharesPost = maskLower.findViewById(R.id.count_shares);

		btnHearts = maskLower.findViewById(R.id.button_hearts);
		btnComments = maskLower.findViewById(R.id.button_comments);
		btnShares = maskLower.findViewById(R.id.button_shares);
		View btnPin = maskLower.findViewById(R.id.button_pin);

		initiativesLayout = maskLower.findViewById(R.id.initiative_label_layout);
		initiativesText = maskLower.findViewById(R.id.initiative_label_txt);
		initiativesIcon = maskLower.findViewById(R.id.balloons_icon);

		tagsLayout = maskLower.findViewById(R.id.tags_layout);
		View tag1L = maskLower.findViewById(R.id.tag_1);
		this.tag1 = tag1L.findViewById(R.id.pic);
		tag2L = maskLower.findViewById(R.id.tag_2);
		this.tag2 = tag2L.findViewById(R.id.pic);
		tag3L = maskLower.findViewById(R.id.tag_3);
		this.tag3 = tag3L.findViewById(R.id.pic);
		tag4L = maskLower.findViewById(R.id.tag_4);
		this.tag4 = tag4L.findViewById(R.id.pic);
		tag5L = maskLower.findViewById(R.id.tag_5);
		this.tag5 = tag5L.findViewById(R.id.pic);
		viewTags = maskLower.findViewById(R.id.more_tags);
		moreTags = maskLower.findViewById(R.id.tags_more);

		privacyIcon = maskUpper.findViewById(R.id.icon_privacy);

		pinIcon = maskLower.findViewById(R.id.pin_icon);

		mMainView.setOnClickListener(this);
		mMainView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				boolean performed = mDetector.onTouchEvent(event);
//				if (!performed) {
//					v.performAccessibilityAction(event.getAction(), null);
//				}

				return performed;
			}
		});
		picLayout.setOnClickListener(this);
		moreOptions.setOnClickListener(this);
		viewMoreText.setOnClickListener(this);
		btnHearts.setOnClickListener(this);
		btnComments.setOnClickListener(this);
		btnShares.setOnClickListener(this);
		btnPin.setOnClickListener(this);
		viewTags.setOnClickListener(this);
		tag1.setOnClickListener(this);
		tag2.setOnClickListener(this);
		tag3.setOnClickListener(this);
		tag4.setOnClickListener(this);
		tag5.setOnClickListener(this);

		fragment.setConfigListener(this);
	}


	public void onBindViewHolder(@NonNull Post object) {
		mItem = object;

		// background
		rootView.setBackgroundColor(mItem.getBackgroundColor(fragment.getResources()));

		if (Utils.isStringValid(mItem.getAuthorUrl()))
			MediaHelper.loadProfilePictureWithPlaceholder((HLActivity) mListener, mItem.getAuthorUrl(), profilePic);
		else
			profilePic.setImageResource(R.drawable.ic_profile_placeholder);

		userName.setText(mItem.getAuthor());
		cntHeartsUser.setText(Utils.getReadableCount(cntHeartsUser.getResources(), mItem.getCountHeartsUser()));
		handleTimeStamp(mItem);

		String name = object.getNameFromShared();
		if (Utils.isStringValid(name)) {
			sharedVia.setVisibility(View.VISIBLE);
			sharedViaName.setText(Utils.getFormattedHtml(sharedViaName.getContext().getString(R.string.shared_via, name)));
		}
		else sharedVia.setVisibility(View.GONE);

		handleMemoryText();

		bottomSectionLower.setAlpha(1);
		cntHeartsPost.setText(mItem.getReadableInteractionCount(getActivity(), InteractionTypeEnum.HEARTS, true));
		cntCommentsPost.setText(mItem.getReadableInteractionCount(getActivity(), InteractionTypeEnum.COMMENT, true));
		cntSharesPost.setText(mItem.getReadableInteractionCount(getActivity(), InteractionTypeEnum.SHARE, true));

		btnHearts.setSelected(mItem.getHeartsLeft() != null && mItem.getHeartsLeft() > 0);
		btnComments.setSelected(mItem.isYouLeftComments());
		btnShares.setSelected(mItem.isYouDidShares());

		int tCnt = 0;
		if (mItem.getTags() != null)
			tCnt = mItem.getTags().size();
		handleTagsPictures(tCnt);

		if (mItem.getVisibility() != null) {
			int[] resources = PrivacyPostVisibilityEnum.getResources(mItem.getPostVisibilityEnum(), true);
			if (resources != null && resources.length == 2) {
				privacyIcon.setImageResource(resources[0]);
				privacyIcon.setVisibility(View.VISIBLE);
			}
		}
		else privacyIcon.setVisibility(View.GONE);

		pinIcon.setSelected(mItem.hasLists());

		handleInitiativeLabel();

		handleMaskProperties();
	}

	void handleTimeStamp(Post post) {
		if (post != null) {
			Date d = post.getCreationDate();
			if (d == null && Utils.isStringValid(post.getDate()))
				d = Utils.getDateFromDB(post.getDate());
			timeStamp.setText(InteractionPost.getTimeStamp(timeStamp.getResources(), d));
		}
	}

	private void handleInitiativeLabel() {
		if (mItem != null) {
			if (mItem.hasInitiative()) {
				initiativesText.setText(mItem.getInitiative().getText());
				initiativesText.setTextColor(Utils.getColor(getActivity(), R.color.colorAccent));
				initiativesText.setTypeface(initiativesText.getTypeface(), Typeface.NORMAL);
				initiativesLayout.setVisibility(View.VISIBLE);
			}
			else {
				if (mItem.hasGSMessage())
					initiativesText.setText(mItem.getGSMessage());

				initiativesLayout.setVisibility(mItem.hasGSMessage() ? View.VISIBLE : View.GONE);
			}
		}
	}

	private void handleTagsPictures(int tCnt) {
		tagsLayout.setVisibility((tCnt == 0) ? View.GONE : View.VISIBLE);
		viewTags.setText(Utils.getFormattedHtml(viewTags.getResources(), R.string.view_tags));
		viewTags.setVisibility(View.VISIBLE);

		moreTags.setVisibility(tCnt > 5 ? View.VISIBLE : View.GONE);

		tag2L.setVisibility((tCnt >= 2) ? View.VISIBLE : View.GONE);
		tag3L.setVisibility((tCnt >= 3) ? View.VISIBLE : View.GONE);
		tag4L.setVisibility((tCnt >= 4) ? View.VISIBLE : View.GONE);
		tag5L.setVisibility((tCnt >= 5) ? View.VISIBLE : View.GONE);

		if (mItem.getTags() != null && !mItem.getTags().isEmpty())
			for (int i = 0; i < mItem.getTags().size(); i++) {
				Tag t = mItem.getTags().get(i);
				if (t != null) {
					ImageView v = tag1;
					switch (i) {
						case 0: v = tag1; break;
						case 1: v = tag2; break;
						case 2: v = tag3; break;
						case 3: v = tag4; break;
						case 4:
							if (Utils.isStringValid(t.getUserUrl()))
								MediaHelper.loadProfilePictureWithPlaceholder(mListener.getActivity(), t.getUserUrl(), tag5);
							else
								tag5.setImageResource(R.drawable.ic_profile_placeholder);
							return;
					}

					if (Utils.isStringValid(t.getUserUrl()))
						MediaHelper.loadProfilePictureWithPlaceholder(mListener.getActivity(), t.getUserUrl(), v);
					else
						v.setImageResource(R.drawable.ic_profile_placeholder);
				}
			}
	}


	private void handleMemoryText() {
		if (mItem != null) {
			// text
			if (mItem.hasCaption()) {
				ViewCompat.setTransitionName(postCaption, mItem.getId());
				postCaption.setText(mItem.getCaption());

				Triple<Integer, Float, Integer> textStyle = mItem.getTextStyle(fragment.getResources());
				postCaption.setTextColor(textStyle.getFirst());
				viewMoreText.setTextColor(textStyle.getFirst());
				postCaption.setTextSize(TypedValue.COMPLEX_UNIT_SP, textStyle.getSecond());
				postCaption.setGravity(textStyle.getThird());
                viewMoreText.setGravity(textStyle.getThird());

				postCaption.setVisibility(View.VISIBLE);

				viewMoreText.setText(Utils.getFormattedHtml(resources, R.string.view_more));
				viewMoreText.setVisibility(moreTextVisible ? View.VISIBLE : View.GONE);
			}
			else {
				postCaption.setVisibility(View.GONE);
				viewMoreText.setVisibility(View.GONE);
			}
		}
	}


	@Override
	public void onClick(View view) {
		int id = view.getId();

		canProcessEvents = false;

		if (id != R.id.post_main_view && (fragment != null && fragment.hasFilterVisible())) {
			fragment.handleFilterToggle();
		}
		else if (id != R.id.post_main_view && (mListener != null && mListener.isPostSheetOpen())) {
			mListener.closePostSheet();
		}
		else {
			if (mItem != null && mListener != null) {
				if (view.getId() == R.id.profile_picture && (mItem.canLookAuthorUp() || mItem.isInterest())) {
					mListener.goToProfile(mItem.getAuthorId(), mItem.isInterest());
				} else if (!Utils.checkAndOpenLogin(getActivity(), getActivity().getUser(), HomeActivity.PAGER_ITEM_HOME_WEBVIEW)) {
					mListener.setLastAdapterPosition(itemPosition);
					setLastAdapterItemId(mItem);

					switch (id) {
						case R.id.button_hearts:
						case R.id.button_shares:
							if (mItem.isShowHeartsSharesDetails())
								handleInteractions(id);
							break;

						case R.id.button_comments:
						case R.id.button_pin:
							handleInteractions(id);
							break;

						case R.id.more_tags:
							mListener.viewAllTags(mItem);
							break;

						case R.id.more_options:
							mListener.openPostSheet(mItem.getId(), mItem.isActiveUserAuthor(fragment.getRealm()));
							break;

						case R.id.view_more_text:
							goToTextViewer();
							break;
					}

					canProcessEvents = true;
				}
			}
		}
	}

	boolean checkAndCloseFilter() {
		if (mListener != null && mListener.isPostSheetOpen()) {
			mListener.closePostSheet();
			return false;
		}
		if (fragment != null && fragment.hasFilterVisible()) {
			fragment.handleFilterToggle();
			return false;
		}
		return true;
	}

	private void goToTextViewer() {
		Intent intent = new Intent(getActivity(), TextViewActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, mItem.getCaption());
		intent.putExtra(Constants.EXTRA_PARAM_2, mItem.getAuthor());
		getActivity().startActivity(intent);
		getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
	}



	//region == Detector listener ==

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (checkAndCloseFilter()) {
			operate(true, false);
			return true;
		}
		else return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return checkAndCloseFilter() && !Utils.checkAndOpenLogin(getActivity(), getActivity().getUser(), HomeActivity.PAGER_ITEM_HOME_WEBVIEW);
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if (checkAndCloseFilter()) {
			if (!Utils.checkAndOpenLogin(getActivity(), getActivity().getUser(), HomeActivity.PAGER_ITEM_HOME_WEBVIEW)) {
				mListener.setLastAdapterPosition(itemPosition);
				setLastAdapterItemId(mItem);
				mListener.saveFullScreenState();
				applyAnimationsForInteractions();
				mListener.goToInteractionsActivity(mItem.getId());
			}
		}
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	//endregion



	@Override
	public void onConfigChanged() {
		operate(false, true);
	}


	private void operate(boolean complete, boolean considerLandscape) {
		if (mMainView != null && mListener != null) {
			if (considerLandscape) {
				switch (getItemViewType()) {
					case InnerCircleFeedAdapter.TYPE_PICTURE:
						ImageView img = mMainView instanceof ImageView ? ((ImageView) mMainView) : null;
						if (mListener.getMediaHelper().doesMediaWantLandscape(mItem.getTypeEnum(), img, 0, 0)) {
							mListener.setLastAdapterPosition(getAdapterPosition());
							setLastAdapterItemId(mItem);
							mListener.actionsForLandscape(mItem.getId(), mMainView);
						}
						break;
					default:
						if (complete)
							onManageTapForFullScreen();
						return;
				}
			}
			if (complete) {
				onManageTapForFullScreen();
			}
		}
	}

	private void handleMaskProperties() {
		float alpha = fullScreenHelper.isAlphaOn() ? 1f : 0f;
		maskUpperHide.setAlpha(alpha);
		maskLowerHide.setAlpha(alpha);

		int marginTop = fullScreenHelper.areMaskMargins0() ? 0 : Utils.dpToPx(R.dimen.toolbar_height, resources);
		int marginBottom = fullScreenHelper.areMaskMargins0ForBottom() ? 0 : Utils.dpToPx(R.dimen.bottom_bar_height, resources);
		int marginStart = Utils.dpToPx(10f, resources);
		((LinearLayout.LayoutParams) maskUpperHide.getLayoutParams()).setMargins(marginStart, 0, 0, 0);
		((LinearLayout.LayoutParams) maskUpper.getLayoutParams()).setMargins(0, marginTop, 0, 0);
		((LinearLayout.LayoutParams) maskLower.getLayoutParams()).setMargins(0, 0, 0, marginBottom);

		maskUpperHide.requestLayout();
		maskUpper.requestLayout();
		maskLower.requestLayout();
	}

	/**
	 * Communicates taps on Fragment's mainView to manage entering and exiting FULL SCREEN MODE.
	 */
	private void onManageTapForFullScreen() {
		if (mListener != null) {
			FullScreenHelper fullScreenListener = mListener.getFullScreenListener();
			if (fullScreenListener != null) {

				switch (fullScreenListener.getFullScreenType()) {
					case NONE:
						fullScreenListener.goFullscreenV2(fragment.getToolbar(), mListener.getBottomBar(),
								maskUpAlphaOff, maskLowAlphaOff, maskUpperHide, maskLowerHide, fragment.getPostAdapter(), false);

						if (mItem.isPicturePost() || mItem.isVideoPost())
							maskContainer.setBackgroundColor(Utils.getColor(mMainView.getContext(), R.color.transparent));
						break;

					case FULL_SCREEN_ELEMENTS:
						fullScreenListener.applyPostMaskV2(maskUpAlphaOn, maskLowAlphaOn, maskUpperHide, maskLowerHide, fragment.getPostAdapter());

						if (mItem.isPicturePost() || mItem.isVideoPost())
							maskContainer.setBackgroundColor(Utils.getColor(mMainView.getContext(), R.color.black_10));
						break;

					case POST_MASK:
						fullScreenListener.exitFullscreenV2(fragment.getToolbar(), mListener.getBottomBar(), maskUpper, maskLower,
								maskUpShift, maskLowShift, fragment.getPostAdapter(), false);

						if (mItem.isPicturePost() || mItem.isVideoPost())
							maskContainer.setBackgroundColor(Utils.getColor(mMainView.getContext(), R.color.black_10));
						break;
				}
			}
		}
	}

	private boolean doesWantPartialAnimation() {
		FullScreenHelper.FullScreenType type = fullScreenHelper.getFullScreenType();

		return (type == FullScreenHelper.FullScreenType.NONE || type == FullScreenHelper.FullScreenType.POST_MASK) && mItem.hasCaption();
	}

	private void applyAnimationsForInteractions() {
		AnimatorSet set = new AnimatorSet();
		List<Animator> list = new ArrayList<>();
		list.add(maskUpAlphaOffTotal);
		list.add(doesWantPartialAnimation() ? maskLowAlphaOff : maskLowAlphaOffTotal);
		list.add(fullScreenHelper.getMaskAlphaAnimationOff(fragment.getToolbar()));
		if (mListener.getBottomBar() != null)
			list.add(fullScreenHelper.getMaskAlphaAnimationOff(mListener.getBottomBar()));
		if (playBtn != null)
			list.add(fullScreenHelper.getMaskAlphaAnimationOff(playBtn));
		if (pauseBtn != null)
			list.add(fullScreenHelper.getMaskAlphaAnimationOff(pauseBtn));
		set.playTogether(list);
		set.start();
	}

	@Override
	public void restoreFullScreenState(FullScreenHelper.FullScreenType type) {
		AnimatorSet set = new AnimatorSet();
		List<Animator> list = new ArrayList<>();
		list.add(maskUpAlphaOnTotal);
		list.add(doesWantPartialAnimation() ? maskLowAlphaOn : maskLowAlphaOnTotal);
		list.add(fullScreenHelper.getMaskAlphaAnimationOn(fragment.getToolbar()));
		if (mListener.getBottomBar() != null)
			list.add(fullScreenHelper.getMaskAlphaAnimationOn(mListener.getBottomBar()));
		if (playBtn != null)
			list.add(fullScreenHelper.getMaskAlphaAnimationOn(playBtn));
		if (pauseBtn != null)
			list.add(fullScreenHelper.getMaskAlphaAnimationOn(pauseBtn));
		set.playTogether(list);
		set.start();
	}


	private ValueAnimator getAlphaAnimations(boolean up, boolean on, boolean total) {
		FullScreenHelper fullScreenListener = mListener.getFullScreenListener();
		if (fullScreenListener != null) {
			if (up) {
				if (on)
					return fullScreenListener.getMaskAlphaAnimationOn(total ? maskUpper : maskUpperHide);
				else
					return fullScreenListener.getMaskAlphaAnimationOff(total ? maskUpper : maskUpperHide);
			}
			else {
				if (on)
					return fullScreenListener.getMaskAlphaAnimationOn(total ? maskLower: maskLowerHide);
				else
					return fullScreenListener.getMaskAlphaAnimationOff(total ? maskLower : maskLowerHide);
			}
		}

		return null;
	}

	private ValueAnimator getMaskTranslation(View view) {
		int id = view.getId();
		FullScreenHelper fullScreenListener = mListener.getFullScreenListener();
		if (fullScreenListener != null) {
			switch (id) {
				case R.id.timeline_post_mask_upper:
					return fullScreenListener.getMaskUpShift(maskUpper);
				case R.id.timeline_post_mask_lower:
					return fullScreenListener.getMaskLowShift(maskLower);
			}
		}

		return null;
	}

	HLActivity getActivity() {
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


	private void handleInteractions(@IdRes int viewId) {
		InteractionPost.Type type = null;
		switch (viewId) {
			case R.id.button_hearts:
				type = InteractionPost.Type.HEARTS;

				if (mItem.getCountHeartsPost() == 0)
					return;

				break;
			case R.id.button_comments:
				type = InteractionPost.Type.COMMENT;

				if (mItem.getCountComments() == 0)
					return;

				break;
			case R.id.button_shares:
				type = InteractionPost.Type.SHARE;

				if (mItem.getCountShares() == 0)
					return;

				break;

			case R.id.button_pin:
				type = InteractionPost.Type.PIN;
				break;
		}

		int[] padding = new int[2];
		int top = 0, bottom = 0;
		if (fullScreenHelper.getFullScreenType() == FullScreenHelper.FullScreenType.NONE)
			top = fragment.getToolbar().getHeight()/* + maskUpper.getHeight()*/;		// corrects padding: too much with the new Interactions handling
		else if (fullScreenHelper.getFullScreenType() == FullScreenHelper.FullScreenType.POST_MASK)
			top = maskUpper.getHeight();
		padding[0] = top;

		if (fullScreenHelper.getFullScreenType() == FullScreenHelper.FullScreenType.NONE)
			bottom = mListener.getBottomBar() != null ? mListener.getBottomBar().getHeight() : 0;
		padding[1] = bottom;

		Intent intent = new Intent(mListener.getActivity(), InteractionsViewerActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, padding);
		intent.putExtra(Constants.EXTRA_PARAM_2, type);
		intent.putExtra(Constants.EXTRA_PARAM_3, mItem.getId());
		fragment.startActivityForResult(intent, Constants.RESULT_TIMELINE_INTERACTIONS_VIEW);

		LogUtils.d(TimelineFragment.LOG_TAG, "Going to child activity for interaction");

		bottomSectionLower.animate().alpha(0).setDuration(500);
	}


	void setLastAdapterItemId(Post post) {
		if (fragment != null) {
			String lastPost;
			if (fragment.getUsageType() == TimelineFragment.FragmentUsageType.TIMELINE) {
				lastPost = HLPosts.lastPostSeenId = post != null ? post.getId() : "";
				fragment.saveLastPostSeen(false);
			}
			else {
				lastPost = HLPosts.lastPostSeenIdGlobalDiary = post != null ? post.getId() : "";
			}
			LogUtils.d("LAST POST SEEN in FEED", lastPost);
		}
	}

	void resetMoreTextVisibility() {
		this.moreTextVisible = false;
	}

	//region == Getters and setters ==

	void setItemPosition(Integer itemPosition) {
		this.itemPosition = itemPosition;
	}

	//endregion

}