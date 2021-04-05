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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

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
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class WishVerifyPhoneFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishVerifyPhoneFragment.class.getCanonicalName();

	private View layoutVerification, layoutResult;
	private TextView codeView, resultView, retryBtn;

	private String code;

	private WishListElement selectedElement;


	public WishVerifyPhoneFragment() {
		// Required empty public constructor
	}

	public static WishVerifyPhoneFragment newInstance() {
		Bundle args = new Bundle();

		WishVerifyPhoneFragment fragment = new WishVerifyPhoneFragment();
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

		View view = inflater.inflate(R.layout.fragment_wishes_verify_code, container, false);
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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_VERIFY_PHONE);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		if (Utils.isStringValid(mUser.getPhoneNumber()))
			callServer(CallType.VALIDATE);
		else
			showErrorDialog(true);

		setLayout();
	}

	@Override
	public void onPause() {
		onSaveInstanceState(new Bundle());

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
		outState.putString(Constants.EXTRA_PARAM_1, code);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				code = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.retry_btn:
				callServer(CallType.VALIDATE);
				break;
		}
	}

	@Override
	public void onNextClick() {
		callServer(CallType.CHECK);
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

		if (responseObject == null || responseObject.length() != 1) {
			handleErrorResponse(operationId, 0);
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
				if (json != null && json.length() == 1) {
					selectedElement = new WishListElement().deserializeToClass(json.optJSONObject(0));
				}

				wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
				wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
				break;

			case Constants.SERVER_OP_WISH_VALIDATE_PHONE:
				code = responseObject.optJSONObject(0).optString("callerIDVerificationCode");
				setLayout();
				wishesActivityListener.enableDisableNextButton(Utils.isStringValid(code));
				break;

			case Constants.SERVER_OP_WISH_CHECK_PHONE_VALIDATION:
				code = responseObject.optJSONObject(0).optString("callerIDVerificationCode");
				boolean validated = responseObject.optJSONObject(0).optBoolean("isValidated");

				resultView.setText(validated ? R.string.wish_verification_success : R.string.wish_verification_failed);
				retryBtn.setVisibility(validated ? View.GONE : View.VISIBLE);

				if (Utils.isContextValid(getContext()))
					resultView.setTextColor(Utils.getColor(getContext(), validated ? R.color.colorAccent : R.color.black_87));
				layoutVerification.animate().alpha(0).setDuration(350).start();
				layoutResult.animate().alpha(1).setDuration(350).start();

				if (validated) {
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							if (wishesActivityListener != null) {
								wishesActivityListener.setSelectedWishListElement(selectedElement);
								wishesActivityListener.resumeNextNavigation();
							}
						}
					}, 300);
				}

				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		showErrorDialog(false);
		wishesActivityListener.enableDisableNextButton(false);
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
		layoutVerification = view.findViewById(R.id.layout_verification_code);
		codeView = view.findViewById(R.id.phone_code);

		layoutResult = view.findViewById(R.id.result_layout);
		resultView = view.findViewById(R.id.result);
		retryBtn = view.findViewById(R.id.retry_btn);
		retryBtn.setOnClickListener(this);
	}

	@Override
	protected void setLayout() {
		this.codeView.setText(Utils.isStringValid(code) ? code : getString(R.string.big_dash));
	}

	private enum CallType { VALIDATE, CHECK }
	private void callServer(CallType type) {
		Object[] result = null;

		try {
			if (type == CallType.VALIDATE)
				result = HLServerCalls.validatePhoneNumber(mUser.getUserId(), mUser.getCompleteName());
			else if (type == CallType.CHECK)
				result = HLServerCalls.checkPhoneNumberValidation(mUser.getUserId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private void showErrorDialog(final boolean noPhone) {
		final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_generic_title_text_btns);
		if (dialog != null) {
			View view = dialog.getCustomView();
			if (view != null) {
				view.findViewById(R.id.dialog_title).setVisibility(View.GONE);
				((TextView) view.findViewById(R.id.dialog_message)).setText(noPhone ? R.string.error_phone_validation_no_phone : R.string.error_phone_validation_2);
				view.findViewById(R.id.button_negative).setVisibility(View.GONE);
				Button pos = view.findViewById(R.id.button_positive);
				pos.setText(R.string.ok);
				pos.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();

						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								if (Utils.isContextValid(getActivity()))
									getActivity().onBackPressed();
							}
						}, 300);
					}
				});
			}
			dialog.show();
		}
	}

}
