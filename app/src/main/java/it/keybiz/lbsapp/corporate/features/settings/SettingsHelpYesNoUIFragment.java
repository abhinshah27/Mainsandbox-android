/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.SettingsHelpElement;
import it.keybiz.lbsapp.corporate.models.enums.ActionTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;


public class SettingsHelpYesNoUIFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnBackPressedListener, OnMissingConnectionListener {

	public static final String LOG_TAG = SettingsHelpYesNoUIFragment.class.getCanonicalName();

	public enum UsageType { STATIC, SERVER }
	private UsageType usageType = UsageType.SERVER;

	private SettingsHelpElement currentElement;

	private String elementId;

	private TextView titleView, mainText;
	private View checkboxYes, checkboxNo;
	private TextView checkboxYesText, checkboxNoText;
	private View hideableLayout, hideNegative, hidePositive;
	private EditText message;

	private ActionTypeEnum action = null;

	private View mainScrollView;


	public SettingsHelpYesNoUIFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment ProfileFragment.
	 */
	public static SettingsHelpYesNoUIFragment newInstance(SettingsHelpElement element,
	                                                      @Nullable String itemId, UsageType type) {
		SettingsHelpYesNoUIFragment fragment = new SettingsHelpYesNoUIFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, element);
		args.putString(Constants.EXTRA_PARAM_2, itemId);
		args.putSerializable(Constants.EXTRA_PARAM_3, type);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		// Inflate the layout for this fragment
		mainScrollView = inflater.inflate(R.layout.fragment_settings_help_yes_no, container, false);
		configureLayout(mainScrollView);

		return mainScrollView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_PRIVACY_FAQ_FINAL);

		if (usageType == UsageType.SERVER)
			callServer(mType = CallType.ELEMENTS, null, null, null);

		setLayout();
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

	}

	@Override
	public void onDetach() {
		super.onDetach();
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.checkbox_yes:
				checkboxYes.setSelected(!checkboxYes.isSelected());
				checkboxNo.setSelected(false);
				if (checkboxYes.isSelected()) {
					hidePositive.setVisibility(View.VISIBLE);
					hideNegative.setVisibility(View.GONE);
					hideableLayout.setVisibility(View.VISIBLE);
					action = ActionTypeEnum.convertBooleanToEnum(checkboxYes.isSelected(), ActionTypeEnum.YES);

					hidePositive.requestFocus(View.FOCUS_DOWN);
					if (mainScrollView instanceof ScrollView)
						((ScrollView) mainScrollView).fullScroll(View.FOCUS_DOWN);
				}
				else {
					hideableLayout.setVisibility(View.GONE);
					action = null;
				}
				break;

			case R.id.checkbox_no:
				checkboxNo.setSelected(!checkboxNo.isSelected());
				checkboxYes.setSelected(false);
				if (checkboxNo.isSelected()) {
					hidePositive.setVisibility(View.GONE);
					hideNegative.setVisibility(View.VISIBLE);
					hideableLayout.setVisibility(View.VISIBLE);
					action = ActionTypeEnum.convertBooleanToEnum(!checkboxNo.isSelected(), ActionTypeEnum.YES);

					hideNegative.requestFocus(View.FOCUS_DOWN);
					if (mainScrollView instanceof ScrollView)
						((ScrollView) mainScrollView).fullScroll(View.FOCUS_DOWN);
				}
				else {
					hideableLayout.setVisibility(View.GONE);
					action = null;
				}
				break;
		}
	}

	@Override
	public void onBackPressed() {
		saveInfo();
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (responseObject == null || responseObject.length() == 0) {
			handleErrorResponse(operationId, 0);
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_HELP_GET_ELEMENTS:
				JSONObject obj = responseObject.optJSONObject(0);
				if (obj != null && obj.length() > 0) {
					elementId = obj.optString("_id");

					setData(obj.optJSONArray("items"));

					setLayout();
				}
				break;

			case Constants.SERVER_OP_SETTINGS_HELP_GET_SET_UI_LEVEL:
				settingsActivityListener.closeScreen();
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		if (mType == CallType.SET)
			settingsActivityListener.closeScreen();
		activityListener.showGenericError();
	}

	@Override
	public void onMissingConnection(int operationId) {
		if (mType == CallType.SET)
			settingsActivityListener.closeScreen();
	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				currentElement = (SettingsHelpElement) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				elementId = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				usageType = (UsageType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_3);
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		titleView = view.findViewById(R.id.title);

		mainText = view.findViewById(R.id.custom_text);

		checkboxYes = view.findViewById(R.id.checkbox_yes);
		checkboxYesText = checkboxYes.findViewById(R.id.text);
		checkboxNo = view.findViewById(R.id.checkbox_no);
		checkboxNoText = checkboxNo.findViewById(R.id.text);

		hideableLayout = view.findViewById(R.id.hideable_layout);
		hideNegative = view.findViewById(R.id.layout_negative_feedback);
		hidePositive = view.findViewById(R.id.layout_positive_feedback);

		checkboxYes.setOnClickListener(this);
		checkboxNo.setOnClickListener(this);

		message = view.findViewById(R.id.message_et);
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_help);

		titleView.setText(currentElement.getTitle());

		checkboxYesText.setText(R.string.yes);
		checkboxNoText.setText(R.string.no);

		if (currentElement != null) {
			mainText.setText(currentElement.getTextToShow());
			mainText.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	private void saveInfo() {
		String sMessage = message.getText().toString();

		if (checkboxNo.isSelected() && !Utils.isStringValid(sMessage)) {
			activityListener.showAlert(R.string.error_fields_required_feedback_negative);
			return;
		}

		callServer(mType = CallType.SET, action, "", sMessage);
	}


	private enum CallType { ELEMENTS, GET, SET }
	private CallType mType;
	private void callServer(CallType type, @Nullable ActionTypeEnum action, String email, String message) {
		if (currentElement != null) {
			Object[] result = null;

			try {
				if (type == CallType.ELEMENTS)
					result = HLServerCalls.getSettingsHelpElements(currentElement);
				else
					result = HLServerCalls.settingsOperationsOnHelpElements(mUser.getUserId(),
							elementId, action, email, message);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (getActivity() instanceof HLActivity)
				HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
						.handleCallResult(this, (HLActivity) getActivity(), result);

			return;
		}

		activityListener.showGenericError();
	}


	private void setData(JSONArray response) {
		if (response != null && response.length() == 1)
			currentElement = new SettingsHelpElement().deserializeToClass(response.optJSONObject(0));
	}

}
