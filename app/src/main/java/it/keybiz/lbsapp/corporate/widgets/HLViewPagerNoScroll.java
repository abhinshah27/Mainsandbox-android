/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * This class is a custom and non-swipeable ViewPager.
 *
 * With the method {@code setScrollable()} it is possible to enable
 *
 * @author mbaldrighi on 9/20/2017.
 */
public class HLViewPagerNoScroll extends ViewPager {

	private boolean enabled;

	/*
	 * Default constructors
	 */
	public HLViewPagerNoScroll(Context context) {
		super(context);
	}

	public HLViewPagerNoScroll(Context context, AttributeSet attrs) {
		super(context, attrs);
	}


	/**
	 * This method enables ViewPager's scrolling when
	 * @param scroll
	 */
	public void setScrollable(boolean scroll) {
		this.enabled = scroll;
	}


	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return enabled && super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return enabled && super.onInterceptTouchEvent(ev);
	}

}
