/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.userGuide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp;

/**
 * @author mbaldrighi on 5/4/2018.
 */
public class PhotoGuideFragment extends HLFragment {

	@DrawableRes int source;
	private ImageView main;


	public PhotoGuideFragment() {}

	public static PhotoGuideFragment newInstance(@DrawableRes int source) {
		Bundle args = new Bundle();
		args.putInt(Constants.EXTRA_PARAM_1, source);
		PhotoGuideFragment fragment = new PhotoGuideFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		main = new ImageView(inflater.getContext());
		main.setScaleType(ImageView.ScaleType.FIT_XY);
		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		main.setBackgroundColor(Utils.getColor(inflater.getContext(), R.color.colorAccent));
		main.setLayoutParams(lp);

		return main;
	}

	@Override
	public void onResume() {
		super.onResume();

		setLayout();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(Constants.EXTRA_PARAM_1, source);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				source = savedInstanceState.getInt(Constants.EXTRA_PARAM_1, 0);
		}
	}

	@Override
	protected void configureResponseReceiver() {}

	@Override
	protected void configureLayout(@NonNull View view) {}

	@Override
	protected void setLayout() {

		if (source != 0)
			GlideApp.with(main).load(source).centerInside().into(main);
	}
}
