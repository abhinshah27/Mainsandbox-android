/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsSecurityDeleteAccountFragment extends HLFragment implements OnServerMessageReceivedListener,
		OnMissingConnectionListener, View.OnClickListener {

	public static final String LOG_TAG = SettingsSecurityDeleteAccountFragment.class.getCanonicalName();

	private EditText messageEt;
	private View sureBox, heartsBox;
	private View deleteBtn;


	public SettingsSecurityDeleteAccountFragment() {
		// Required empty public constructor
	}

	public static SettingsSecurityDeleteAccountFragment newInstance() {
		Bundle args = new Bundle();
		SettingsSecurityDeleteAccountFragment fragment = new SettingsSecurityDeleteAccountFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_settings_delete_account, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(null);

		if (Utils.isContextValid(getActivity()))
			getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_PRIVACY_DELETE_ACCOUNT);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(messageEt);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_delete_account:
				callDeleteAccount(messageEt.getText().toString());
				break;

			case R.id.checkbox_sure:
			case R.id.checkbox_hearts:
				v.setSelected(!v.isSelected());
				checkButtonEnabled(messageEt.getText());
				break;
		}
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);


		if (responseObject == null || responseObject.length() == 0)
			handleErrorResponse(operationId, 0);

		if (operationId == Constants.SERVER_OP_SETTINGS_SECURITY_DELETE_ACCOUNT) {
			activityListener.closeProgress();

			Utils.logOut(getActivity(), null);
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
		activityListener.closeProgress();
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
		deleteBtn = view.findViewById(R.id.btn_delete_account);
		deleteBtn.setOnClickListener(this);
		deleteBtn.setEnabled(false);

		messageEt = view.findViewById(R.id.reason_message);
		messageEt.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				checkButtonEnabled(s);
			}
		});

		sureBox = view.findViewById(R.id.checkbox_sure);
		((TextView) sureBox.findViewById(R.id.text)).setText(R.string.delete_account_chck_sure);
		sureBox.setOnClickListener(this);
		heartsBox = view.findViewById(R.id.checkbox_hearts);
		((TextView) heartsBox.findViewById(R.id.text)).setText(R.string.delete_account_chck_hearts);
		heartsBox.setOnClickListener(this);
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_security);
	}


	private void callDeleteAccount(String reason) {
		activityListener.openProgress();
		activityListener.setProgressMessage(R.string.deleting_account);

		Object[] result = null;

		try {
			result = HLServerCalls.deleteAccount(mUser.getUserId(), reason);
		} catch (JSONException e) {
				e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}

	private void checkButtonEnabled(Editable s) {
		deleteBtn.setEnabled(
				(s != null && s.length() > 0) &&
						sureBox.isSelected() &&
						heartsBox.isSelected()
		);
	}
}
