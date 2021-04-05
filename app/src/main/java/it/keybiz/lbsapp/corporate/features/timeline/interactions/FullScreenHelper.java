/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.profile.SinglePostActivity;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 9/21/2017.
 */
public class FullScreenHelper {

	public static final String LOG_TAG = FullScreenHelper.class.getCanonicalName();

	public enum FullScreenType {
		POST_MASK,
		FULL_SCREEN,
		FULL_SCREEN_ELEMENTS,
		NONE
	}

	private final int COLLAPSED_HEIGHT = 0;

	private FullScreenType type = FullScreenType.NONE;

	private int expHeightT;
	private int expHeightB;

	/* COLLAPSE-EXPAND ANIMATIONS */
	private ValueAnimator expandBottom;
	private ValueAnimator collapseBottom;
	private ValueAnimator expandToolbar;
	private ValueAnimator collapseToolbar;

	private boolean singleMode = false;
	private boolean diaryMode = false;


	public FullScreenHelper(Context context, boolean diaryMode) {
		if (Utils.isContextValid(context)) {
			expHeightB = Utils.dpToPx(R.dimen.bottom_bar_height, context.getResources());
			expHeightT = Utils.dpToPx(R.dimen.toolbar_height, context.getResources());
		}

		this.diaryMode = diaryMode;

		if (context instanceof SinglePostActivity) /*{*/
			singleMode = true;
//			type = FullScreenType.POST_MASK;
//		}
	}


	public FullScreenType getFullScreenType() {
		return type;
	}

	/**
	 * This method switches Activity to full screen mode.
	 *
	 * @param toolbar     {@link HomeActivity}'s {@link Toolbar}.
	 * @param bottomBar   {@link HomeActivity}'s bottom bar {@link View}.
	 * @param maskUpAnim  the {@link ValueAnimator} animating alpha value (1 -> 0) of post mask upper section.
	 * @param maskLowAnim the {@link ValueAnimator} animating alpha value (1 -> 0) of post mask lower section.
	 * @param maskUp      the {@link View} inflating the post mask upper section.
	 * @param maskLow     the {@link View} inflating the post mask lower section.
	 * @param adapter     the {@link RecyclerView.Adapter} instance.
	 */
	public void goFullscreenV2(@Nullable Toolbar toolbar, @Nullable View bottomBar,
	                           @NonNull ValueAnimator maskUpAnim, @NonNull ValueAnimator maskLowAnim,
	                           @NonNull final View maskUp, @NonNull final View maskLow,
	                           @NonNull final RecyclerView.Adapter adapter, final boolean total) {

		AnimatorSet set = new AnimatorSet();
		if ((singleMode || diaryMode) && toolbar != null) {
			set.playTogether(
					getCollapseAnimation(toolbar, expHeightT),
					maskUpAnim, maskLowAnim
			);
		}
		else if (toolbar != null && bottomBar != null) {
			set.playTogether(
					collapseBottom != null ? collapseBottom : (collapseBottom = getCollapseAnimation(bottomBar, expHeightB)),
					getCollapseAnimation(toolbar, expHeightT),
					maskUpAnim,
					maskLowAnim
			);
		}
		else set.playTogether(maskUpAnim, maskLowAnim);
		set.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animator) {}

			@Override
			public void onAnimationEnd(Animator animator) {
				type = total ? FullScreenType.FULL_SCREEN : FullScreenType.FULL_SCREEN_ELEMENTS;

				if (maskUp.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
					int marginStart = (maskUp.getId() == R.id.hideable_layout) ? Utils.dpToPx(10f, maskUp.getResources()) : 0;
					((RelativeLayout.LayoutParams) maskUp.getLayoutParams()).setMargins(marginStart, 0, 0, 0);
				}
				else if (maskUp.getLayoutParams() instanceof LinearLayout.LayoutParams)
					((LinearLayout.LayoutParams) maskUp.getLayoutParams()).setMargins(0, 0, 0, 0);

				if (maskLow.getLayoutParams() instanceof RelativeLayout.LayoutParams)
					((RelativeLayout.LayoutParams) maskLow.getLayoutParams()).setMargins(0, 0, 0, 0);
				else if (maskLow.getLayoutParams() instanceof LinearLayout.LayoutParams)
					((LinearLayout.LayoutParams) maskLow.getLayoutParams()).setMargins(0, 0, 0, 0);
				maskUp.requestLayout();
				maskLow.requestLayout();

				adapter.notifyDataSetChanged();
			}

			@Override
			public void onAnimationCancel(Animator animator) {}

			@Override
			public void onAnimationRepeat(Animator animator) {}
		});
		set.start();
	}

	/**
	 * This method shows {@link Toolbar} and bottom bar, pushing to their final positions upper and
	 * lower masks, exiting completely the full screen mode.
	 *
	 * @param toolbar     {@link HomeActivity}'s {@link Toolbar}.
	 * @param bottomBar   {@link HomeActivity}'s bottom bar {@link View}.
	 * @param maskUpper  the {@link ValueAnimator} animating alpha value (1 -> 0) of post mask upper section.
	 * @param maskLower  the {@link ValueAnimator} animating alpha value (1 -> 0) of post mask lower section.
	 * @param adapter     the {@link RecyclerView.Adapter} instance.
	 */
	public void exitFullscreenV2(@Nullable final Toolbar toolbar, @Nullable final View bottomBar,
	                             View maskUp, View maskLow,
	                             @NonNull final ValueAnimator maskUpper, @NonNull final ValueAnimator maskLower,
	                             @NonNull final RecyclerView.Adapter adapter, boolean delay) {
		AnimatorSet set = new AnimatorSet();
		if ((singleMode || diaryMode) && toolbar != null) {
			set.playTogether(
					getExpandAnimation(toolbar, expHeightT),
					getMaskUpShift(maskUp)
			);
		}
		else if (toolbar != null && bottomBar != null) {
			set.playTogether(
					getExpandAnimation(bottomBar, expHeightB),
					getExpandAnimation(toolbar, expHeightT),
					getMaskUpShift(maskUp), getMaskLowShift(maskLow)
			);
		}
		else set.playTogether(getMaskUpShift(maskUp), getMaskLowShift(maskLow));
		set.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animator) {
				if (toolbar != null)
					toolbar.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animator animator) {
				type = FullScreenType.NONE;
				adapter.notifyDataSetChanged();
//						maskUpper.setVisibility(View.GONE);
//						maskLower.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationCancel(Animator animator) {}

			@Override
			public void onAnimationRepeat(Animator animator) {}
		});
		if (delay)
			set.setStartDelay(450);
		set.start();
	}

	/**
	 * Applies at the same time upper and lower sections of post mask.
	 *
	 * @param maskUpAnim  the {@link ValueAnimator} animating alpha value (0 -> 1) of post mask upper section.
	 * @param maskLowAnim the {@link ValueAnimator} animating alpha value (0 -> 1) of post mask lower section.
	 * @param maskUp      the {@link View} inflating the post mask upper section.
	 * @param maskLow     the {@link View} inflating the post mask lower section.
	 * @param adapter     the {@link RecyclerView.Adapter} instance.
	 */
	public void applyPostMaskV2(@NonNull ValueAnimator maskUpAnim, @Nullable ValueAnimator maskLowAnim,
	                            @NonNull View maskUp, @Nullable View maskLow,
	                            @Nullable final RecyclerView.Adapter adapter) {
		AnimatorSet set = new AnimatorSet();

		if (maskLow != null)
			set.playTogether(maskUpAnim, maskLowAnim);
		else
			set.play(maskUpAnim);
		set.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animator) {}

			@Override
			public void onAnimationEnd(Animator animator) {
				type = FullScreenType.POST_MASK;
				if (adapter != null)
					adapter.notifyDataSetChanged();
			}

			@Override
			public void onAnimationCancel(Animator animator) {}

			@Override
			public void onAnimationRepeat(Animator animator) {}
		});
		set.start();
	}


	//region == ANIMATION ==

	private ValueAnimator getExpandAnimation(final View expandableSection, int expHeight) {
		if (expandableSection != null && expHeight > 0) {
			ValueAnimator valueAnimator = ValueAnimator.ofInt(COLLAPSED_HEIGHT, expHeight);
			valueAnimator.setDuration(400);
			valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					int val = (int) animation.getAnimatedValue();
					LogUtils.d(LOG_TAG, "Expanding view's height is: " + val);
					expandableSection.getLayoutParams().height = val;
					expandableSection.requestLayout();
				}
			});
			return valueAnimator;
		}

		return null;
	}

	private ValueAnimator getCollapseAnimation(final View expandableSection, int expHeight) {
		if (expandableSection != null && expHeight > 0) {
			ValueAnimator valueAnimator = ValueAnimator.ofInt(expHeight, COLLAPSED_HEIGHT);
			valueAnimator.setDuration(400);
			valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					int val = (int) animation.getAnimatedValue();
					LogUtils.d(LOG_TAG, "Collapsing view's height is: " + val);
					expandableSection.getLayoutParams().height = val;
					expandableSection.requestLayout();
				}
			});
			return valueAnimator;
		}

		return null;
	}

	/**
	 * Retrieves the animation shifting the upper section of the post mask to make room for layout
	 * {@link Toolbar}.
	 *
	 * @param maskUp the View pointing to the upper section of the post mask.
	 * @return The wanted {@link ValueAnimator} object.
	 */
	public ValueAnimator getMaskUpShift(final View maskUp) {
		if (maskUp != null) {
			ValueAnimator valueAnimator = ValueAnimator.ofInt(0, expHeightT);
			valueAnimator.setDuration(400);
			valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					int val = (int) animation.getAnimatedValue();
					LogUtils.d(LOG_TAG, "Mask's marginTop is: " + val);
					((LinearLayout.LayoutParams) maskUp.getLayoutParams()).setMargins(0, val, 0, 0);
					maskUp.requestLayout();
				}
			});
			return valueAnimator;
		}

		return null;
	}

	/**
	 * Retrieves the animation shifting the lower section of the post mask to make room for layout
	 * bottom bar View.
	 *
	 * @param maskLow the View pointing to the lower section of the post mask.
	 * @return The wanted {@link ValueAnimator} object.
	 */
	public ValueAnimator getMaskLowShift(final View maskLow) {
		if (maskLow != null) {
			ValueAnimator valueAnimator = ValueAnimator.ofInt(0, expHeightB);
			valueAnimator.setDuration(400);
			valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					int val = (int) animation.getAnimatedValue();
					LogUtils.d(LOG_TAG, "Mask's marginTop is: " + val);
					((LinearLayout.LayoutParams) maskLow.getLayoutParams()).setMargins(0, 0, 0, val);
					maskLow.requestLayout();
				}
			});
			return valueAnimator;
		}

		return null;
	}

	/**
	 * Retrieves the animation the "turn on" (alpha: 0 -> 1) the provided post mask section.
	 *
	 * @param mask the View pointing to the upper or lower section of the post mask.
	 * @return The wanted {@link ValueAnimator} object.
	 */
	public ValueAnimator getMaskAlphaAnimationOn(final View mask) {
		if (mask != null) {
			ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
			valueAnimator.setDuration(400);
			valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					float val = (float) animation.getAnimatedValue();
					LogUtils.d(LOG_TAG, "Alpha ON value is: " + val);
					mask.setAlpha(val);
				}
			});
			valueAnimator.addListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animator) {
					mask.setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationEnd(Animator animator) {}

				@Override
				public void onAnimationCancel(Animator animator) {}

				@Override
				public void onAnimationRepeat(Animator animator) {}
			});
			return valueAnimator;
		}
		return null;
	}

	/**
	 * Retrieves the animation the "turn off" (alpha: 1 -> 0) the provided post mask section.
	 *
	 * @param mask the View pointing to the upper or lower section of the post mask.
	 * @return The wanted {@link ValueAnimator} object.
	 */
	public ValueAnimator getMaskAlphaAnimationOff(final View mask) {
		if (mask != null) {
			ValueAnimator valueAnimator = ValueAnimator.ofFloat(1f, 0f);
			valueAnimator.setDuration(400);
			valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					float val = (float) animation.getAnimatedValue();
					LogUtils.d(LOG_TAG, "Alpha OFF value is: " + val);
					mask.setAlpha(val);
				}
			});
			valueAnimator.addListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animator) {}

				@Override
				public void onAnimationEnd(Animator animator) {
					mask.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onAnimationCancel(Animator animator) {}

				@Override
				public void onAnimationRepeat(Animator animator) {}
			});
			return valueAnimator;
		}
		return null;
	}

	/**
	 * Tells whether the current {@link FullScreenHelper.FullScreenType}
	 * wants views' alpha "turned on" or not.
	 *
	 * @return True if post mask is "ON", false otherwise.
	 */
	public boolean isAlphaOn() {
		return getFullScreenType() == FullScreenType.POST_MASK || getFullScreenType() == FullScreenType.NONE;
	}

	/**
	 * Tells whether the current {@link FullScreenHelper.FullScreenType}
	 * wants the post mask sections make room for the layout bars or not.
	 *
	 * @return True if post mask has been shifted, false otherwise.
	 */
	public boolean areMaskMargins0() {
		return getFullScreenType() == FullScreenType.FULL_SCREEN_ELEMENTS || getFullScreenType() == FullScreenType.POST_MASK;
	}

	/**
	 * Tells whether the current {@link FullScreenHelper.FullScreenType}
	 * wants the LOWER post mask section make room for the layout bottom bar or not.
	 *
	 * @return True if post mask has been shifted, false otherwise.
	 */
	public boolean areMaskMargins0ForBottom() {
		return singleMode || diaryMode || getFullScreenType() == FullScreenType.FULL_SCREEN_ELEMENTS ||
				getFullScreenType() == FullScreenType.POST_MASK;
	}

	/**
	 * Tells whether the upper post mask is currently visible por not.
	 *
	 * @return True if upper post mask is visible, false otherwise.
	 */
	public boolean isPostMaskUpVisible() {
		return getFullScreenType() == FullScreenType.FULL_SCREEN || getFullScreenType() == FullScreenType.POST_MASK;
	}

	/**
	 * Tells whether the helper has been invoked from {@link SinglePostActivity} or not.
	 *
	 * @return True if the helper is in single mode, false otherwise.
	 */
	public boolean isSingleMode() {
		return singleMode;
	}

	/**
	 * Tells whether the helper has been invoked from {@link it.keybiz.lbsapp.corporate.features.timeline.TimelineFragment} displaying a user's or not.
	 *
	 * @return True if the helper is in single mode, false otherwise.
	 */
	public boolean isDiaryMode() {
		return diaryMode;
	}


	public void setDiaryMode(boolean diaryMode) {
		this.diaryMode = diaryMode;
	}



	public interface RestoreFullScreenStateListener {
		void restoreFullScreenState(FullScreenType type);
	}
}
