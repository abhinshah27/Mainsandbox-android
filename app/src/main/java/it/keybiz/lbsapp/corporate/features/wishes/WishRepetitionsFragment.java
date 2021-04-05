/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;


import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.shawnlin.numberpicker.NumberPicker;

import org.json.JSONArray;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class WishRepetitionsFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishRepetitionsFragment.class.getCanonicalName();

	private final int INFINITE_TIMES = -1000;

	@ColorInt private static int spanAccent;

	private TextView repeatText, repeatTextTimes;
	private NumberPicker picker;

	private String friendId;

	private int selectedRep = 0;

	private WishListElement selectedElement;


	public WishRepetitionsFragment() {
		// Required empty public constructor
	}

	public static WishRepetitionsFragment newInstance(String friendId) {

		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, friendId);
		WishRepetitionsFragment fragment = new WishRepetitionsFragment();
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

		View view = inflater.inflate(R.layout.fragment_wishes_repetitions, container, false);
		configureLayout(view);


		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (Utils.isContextValid(getActivity()))
			spanAccent = Utils.getColor(getActivity(), R.color.colorAccent);

		wishesActivityListener.setOnNextClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_REPETITION);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();
		wishesActivityListener.setNextAlwaysOn();
		wishesActivityListener.enableDisableNextButton(true);
	}

	@Override
	public void onPause() {
		super.onPause();
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

		outState.putString(Constants.EXTRA_PARAM_1, friendId);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				friendId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		}
	}

	@Override
	public void onNextClick() {
		if (selectedRep == INFINITE_TIMES || selectedRep > 0) {
			Bundle bundle = new Bundle();
			bundle.putInt("repetition", selectedRep);
			wishesActivityListener.setDataBundle(bundle);
		}
		wishesActivityListener.setSelectedWishListElement(selectedElement);
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

		if (responseObject == null || responseObject.length() == 0)
			return;

		JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
		if (json != null && json.length() == 1) {
			selectedElement = new WishListElement().deserializeToClass(json.optJSONObject(0));
		}

		wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
		wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				activityListener.showGenericError();
				break;
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
		SwitchCompat repeat = view.findViewById(R.id.repeat_switch);
		repeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					picker.animate().alpha(1).setDuration(500).setListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {
							picker.setVisibility(View.VISIBLE);
							selectedRep = picker.getValue();
							styleTextView(picker.getValue());
							repeatTextTimes.setVisibility(View.VISIBLE);
						}

						@Override
						public void onAnimationEnd(Animator animation) {}

						@Override
						public void onAnimationCancel(Animator animation) {}

						@Override
						public void onAnimationRepeat(Animator animation) {}
					}).start();
				}
				else {
					picker.animate().alpha(0).setDuration(500).setListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {}

						@Override
						public void onAnimationEnd(Animator animation) {
							picker.setVisibility(View.GONE);
							picker.setValue(1);
							repeatTextTimes.setVisibility(View.GONE);
							repeatText.setText(R.string.wish_no_repeat);
						}

						@Override
						public void onAnimationCancel(Animator animation) {}

						@Override
						public void onAnimationRepeat(Animator animation) {}
					}).start();
				}
			}
		});
		repeatText = view.findViewById(R.id.repetitions_text);
		repeatTextTimes = view.findViewById(R.id.repetitions_text_times);

		picker = view.findViewById(R.id.repeat_picker);
		picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
			@Override
			public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
				styleTextView(newVal);
				selectedRep = (newVal == 100) ? INFINITE_TIMES : newVal;
			}
		});
		picker.setDisplayedValues(getResources().getStringArray(R.array.repetitions_picker_data));
		picker.setValue(1);
	}

	@Override
	protected void setLayout() {}


	private void styleTextView(int newVal) {
		switch (newVal) {
			case 100:
				repeatTextTimes.setText(R.string.wish_repetitions_infinite);
				break;
			default:
				repeatTextTimes.setText(getResources().getQuantityString(R.plurals.plural_wish_repetitions, newVal, newVal));
		}
		addAccentSpan();
		repeatTextTimes.setVisibility(View.VISIBLE);
	}

	private void addAccentSpan() {
		SpannableStringBuilder spannableString = new SpannableStringBuilder(getString(R.string.wish_repetitions_block_1));
		ForegroundColorSpan cssp = new ForegroundColorSpan(spanAccent);
		spannableString.setSpan(cssp, 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		repeatText.setText(spannableString);
	}

}
