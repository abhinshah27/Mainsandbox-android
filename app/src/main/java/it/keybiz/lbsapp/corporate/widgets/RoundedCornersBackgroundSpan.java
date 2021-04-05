/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.LineBackgroundSpan;

/**
 * @author mbaldrighi on 7/4/2018.
 */
public class RoundedCornersBackgroundSpan implements LineBackgroundSpan {

	private int mBackgroundColor;
	private int mPadding;
	private float mRadius;
	private RectF mBgRect;

	public RoundedCornersBackgroundSpan(int backgroundColor, int padding, float radius) {
		super();
		mBackgroundColor = backgroundColor;
		mPadding = padding;
		mRadius = radius;
		// Pre-create rect for performance
		mBgRect = new RectF();
	}

	@Override
	public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
//		char ch = text.charAt(text.length() - 1);
//		if (lnum > 0 &&	(Character.isWhitespace(ch) || Character.isSpaceChar(ch))) {
//			text = text.subSequence(0, text.length() - 2);
//		}
		final int textWidth = Math.round(p.measureText(text, start, end));
		final int paintColor = p.getColor();
		// Draw the background
		mBgRect.set(left - mPadding,
				top + (mPadding * 2 / 3),
				left + textWidth + mPadding,
				baseline + p.descent() + (mPadding / 3)
		);
		p.setColor(mBackgroundColor);
		c.drawRoundRect(mBgRect, mRadius, mRadius, p);
		p.setColor(paintColor);
	}
}
