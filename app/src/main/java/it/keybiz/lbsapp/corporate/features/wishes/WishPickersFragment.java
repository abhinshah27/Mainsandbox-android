/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.shawnlin.numberpicker.NumberPicker;

import org.json.JSONArray;

import java.util.Calendar;

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
public class WishPickersFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishPickersFragment.class.getCanonicalName();

	private WishesActivity.TimePickerClockType clockType;
	private AsyncTask<Void, Void, String[]> pickerTask;

	private enum PickerType {YEAR, HOUR_OF_DAY, MINUTE, AM_PM }

	private NumberPicker yearPicker;
	private NumberPicker hourOfDay, minute, amPm;

	private int selectedYear, selectedHour, selectedTime, selectedHalf;

	private WishListElement selectedElement;


	public WishPickersFragment() {
		// Required empty public constructor
	}

	public static WishPickersFragment newInstance(WishesActivity.TimePickerClockType type) {

		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, type);
		WishPickersFragment fragment = new WishPickersFragment();
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

		View view = inflater.inflate(R.layout.fragment_wishes_pickers, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_PICKER);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		wishesActivityListener.setNextAlwaysOn();
		wishesActivityListener.enableDisableNextButton(true);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (pickerTask != null && !pickerTask.isCancelled())
			pickerTask.cancel(true);
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

		outState.putSerializable(Constants.EXTRA_PARAM_1, clockType);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				clockType = (WishesActivity.TimePickerClockType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		}
	}

	@Override
	public void onNextClick() {

		Bundle bundle = new Bundle();
		bundle.putString("year", String.valueOf(selectedYear));
//		bundle.putString("year", yearPicker.getDisplayedValues()[selectedYear]);

		String hour = selectedHour < 10 ? ("0" + selectedHour) : String.valueOf(selectedHour);
		String time = selectedTime < 10 ? ("0" + selectedTime) : String.valueOf(selectedTime);

		bundle.putString("time", hour + ":" + time);
		wishesActivityListener.setDataBundle(bundle);

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
		yearPicker = view.findViewById(R.id.years);
		yearPicker.setOnValueChangedListener(new WishPickerValueChangedListener(PickerType.YEAR));
		selectedYear = Calendar.getInstance().get(Calendar.YEAR);
		yearPicker.setMinValue(selectedYear);
		yearPicker.setMaxValue(selectedYear + 201);
		yearPicker.setValue(selectedYear);

		hourOfDay = view.findViewById(R.id.hour_of_day);
		hourOfDay.setOnValueChangedListener(new WishPickerValueChangedListener(PickerType.HOUR_OF_DAY));
		hourOfDay.setValue(0);

		minute = view.findViewById(R.id.minute);
		minute.setOnValueChangedListener(new WishPickerValueChangedListener(PickerType.MINUTE));
		minute.setDisplayedValues(getResources().getStringArray(R.array.picker_time_minute));
		minute.setValue(0);

		amPm = view.findViewById(R.id.ante_post_meridiem);
		amPm.setOnValueChangedListener(new WishPickerValueChangedListener(PickerType.AM_PM));
		amPm.setDisplayedValues(getResources().getStringArray(R.array.picker_time_am_pm));
		amPm.setValue(0);
	}

	@Override
	protected void setLayout() {
		if (clockType == WishesActivity.TimePickerClockType.HOURS_24) {
			amPm.setVisibility(View.GONE);

			LinearLayout.LayoutParams hlp = (LinearLayout.LayoutParams) hourOfDay.getLayoutParams();
			hlp.weight = .5f;
			hourOfDay.setLayoutParams(hlp);

			LinearLayout.LayoutParams mlp = (LinearLayout.LayoutParams) minute.getLayoutParams();
			mlp.weight = .5f;
			minute.setLayoutParams(mlp);

			hourOfDay.setMaxValue(23);
			hourOfDay.setDisplayedValues(getResources().getStringArray(R.array.picker_time_hour_24));
		}
		else if (clockType == WishesActivity.TimePickerClockType.HOURS_12) {
			amPm.setVisibility(View.VISIBLE);

			float weight = 1f / 3;
			LinearLayout.LayoutParams hlp = (LinearLayout.LayoutParams) hourOfDay.getLayoutParams();
			hlp.weight = weight;
			hourOfDay.setLayoutParams(hlp);

			LinearLayout.LayoutParams mlp = (LinearLayout.LayoutParams) minute.getLayoutParams();
			mlp.weight = weight;
			minute.setLayoutParams(mlp);

			LinearLayout.LayoutParams alp = (LinearLayout.LayoutParams) amPm.getLayoutParams();
			alp.weight = weight;
			amPm.setLayoutParams(alp);

			hourOfDay.setMaxValue(11);
			hourOfDay.setDisplayedValues(getResources().getStringArray(R.array.picker_time_hour_12));
		}
	}

	private String[] setValues() {
		String[] array = new String[201];
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		for (int i = 0; i < 201; i++) {
			array[i] = String.valueOf(currentYear + i);
		}

		return array;
	}

	private class WishPickerValueChangedListener implements NumberPicker.OnValueChangeListener {

		private PickerType type;

		WishPickerValueChangedListener(@NonNull PickerType type) {
			this.type = type;
		}

		public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
			if (type != null) {
				switch (type) {
					case YEAR:
						selectedYear = newVal;
						break;

					case HOUR_OF_DAY:
						selectedHour = newVal;
						break;

					case MINUTE:
						selectedTime = newVal;
						break;

					case AM_PM:
						selectedHalf = newVal;
						break;
				}
			}

		}
	}

}
