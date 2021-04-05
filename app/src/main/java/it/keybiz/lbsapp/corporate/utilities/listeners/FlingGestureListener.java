/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * @author mabldrighi on 10/23/2017.
 */
public class FlingGestureListener extends GestureDetector.SimpleOnGestureListener {

	private static final int SWIPE_MIN_DISTANCE = 150;
	private static final int SWIPE_THRESHOLD_VELOCITY = 100;

	private OnFlingCompletedListener mListener;

	public FlingGestureListener(OnFlingCompletedListener mListener) {
		this.mListener = mListener;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (mListener != null ) {
			if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
				mListener.onFlingCompleted();
				return true;
			}
		}

		return false;
	}

	public interface OnFlingCompletedListener {
		void onFlingCompleted();
	}
}
