/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 11/15/2017.
 */
public class ResizingTextWatcher implements TextWatcher {

	private static final float TEXT_LARGE = 22f;
	private static final float TEXT_MEDIUM = 16f;
	private static final float TEXT_SMALL = 14f;

	private WeakReference<TextView> mView;

	public ResizingTextWatcher(TextView view) {
		mView = new WeakReference<>(view);
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void afterTextChanged(Editable s) {
		resizeTextInView(mView.get());
	}


	public static void resizeTextInView(final TextView tv) {
		String text;
		if (tv != null && Utils.isStringValid(text = tv.getText().toString())) {
			int length = text.length();

			if (length < 100) tv.setTextSize(TEXT_LARGE);
			else if (length < 160) tv.setTextSize(TEXT_MEDIUM);
			else tv.setTextSize(TEXT_SMALL);
		}
	}
}
