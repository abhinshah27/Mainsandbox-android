/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

/**
 * @author maxbaldrighi on 3/20/2018.
 */
public class HLCustomTypefaceSpan extends TypefaceSpan {

	private Typeface newType;

	//region == Class constructors ==

	public HLCustomTypefaceSpan(String family, Typeface type) {
		super(family);
		newType = type;
	}

	// TODO: 3/20/2018   if ever used, needs to be written correctly
	public HLCustomTypefaceSpan(Parcel in) {
		super(in);

		// typeface needs to be read from parcel
	}

	//endregion


	@Override
	public void updateDrawState(TextPaint ds) {
		applyCustomTypeface(ds, newType);
	}

	@Override
	public void updateMeasureState(TextPaint paint) {
		applyCustomTypeface(paint, newType);
	}

	private static void applyCustomTypeface(Paint paint, Typeface tf) {
		int oldStyle;
		Typeface old = paint.getTypeface();
		if (old == null)
			oldStyle = 0;
		else
			oldStyle = old.getStyle();

		int fake = oldStyle & ~tf.getStyle();
		if ((fake & Typeface.BOLD) != 0) {
			paint.setFakeBoldText(true);
		}

		if ((fake & Typeface.ITALIC) != 0) {
			paint.setTextSkewX(-0.25f);
		}

		paint.setTypeface(tf);
	}


	// region == Parcelable CREATOR ==

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public HLCustomTypefaceSpan createFromParcel(Parcel in) {
			return new HLCustomTypefaceSpan(in);
		}

		public HLCustomTypefaceSpan[] newArray(int size) {
			return new HLCustomTypefaceSpan[size];
		}
	};

	//endregion
}