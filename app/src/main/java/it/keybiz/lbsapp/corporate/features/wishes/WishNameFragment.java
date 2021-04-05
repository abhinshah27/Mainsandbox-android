/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class WishNameFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishNameFragment.class.getCanonicalName();

	private EditText nameEt;

	public WishNameFragment() {
		// Required empty public constructor
	}

	public static WishNameFragment newInstance() {

		Bundle args = new Bundle();
		WishNameFragment fragment = new WishNameFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_wishes_name, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		WishListElement wli = new WishListElement("root");
		wli.setStepsTotal(6);
		wli.setStep(1);
		wishesActivityListener.setSelectedWishListElement(wli);
		wishesActivityListener.handleSteps(true);
		wishesActivityListener.setOnNextClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_NAME);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(nameEt);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (getActivity() instanceof WishesActivity)
			((WishesActivity) getActivity()).setBackListener(null);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {

		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		}
	}

	@Override
	public void onNextClick() {
		wishesActivityListener.setWishName(nameEt.getText().toString());
		wishesActivityListener.resumeNextNavigation();
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (getActivity() == null || !(getActivity() instanceof HLActivity)) return;


	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Utils.isContextValid(getActivity()) && getActivity() instanceof HLActivity) {
			HLActivity activity = ((HLActivity) getActivity());

			switch (requestCode) {

			}
		}
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {

		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {

		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		activityListener.closeProgress();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	protected void configureLayout(@NonNull View view) {
		nameEt = view.findViewById(R.id.wish_name);
		nameEt.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				wishesActivityListener.enableDisableNextButton(s.length() > 0);
			}
		});

		String wishName = null;
		if (wishesActivityListener.isEditMode()) {
			wishName = wishesActivityListener.getWishToEdit().getName();
		}

		if (Utils.isStringValid(wishName))
			nameEt.setText(wishName);
	}

	@Override
	protected void setLayout() {
		wishesActivityListener.getStepTitle().setVisibility(View.GONE);
		wishesActivityListener.getStepSubTitle().setVisibility(View.GONE);

		wishesActivityListener.showStepsBar();
		wishesActivityListener.setToolbarTitle(wishesActivityListener.isEditMode() ?
				R.string.wish_preview_edit : R.string.title_create_wish);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				wishesActivityListener.enableDisableNextButton(nameEt.getText().length() > 0);
			}
		},300);

		nameEt.requestFocus();
	}

}
