/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.animation.Animator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Locale;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListenerWithErrorDescription;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLIdentity;
import it.keybiz.lbsapp.corporate.models.MarketPlace;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsRedeemHeartsConfirmFragment extends HLFragment implements OnServerMessageReceivedListenerWithErrorDescription,
		OnMissingConnectionListener, View.OnClickListener, OnBackPressedListener {

	public static final String LOG_TAG = SettingsRedeemHeartsConfirmFragment.class.getCanonicalName();

	private static double hlHeartsValue;
	private HLIdentity mIdentity;
	private MarketPlace mMarketPlace;
	private long mConvertedHearts;

	private TextView declaredAmount, convertedAmount, method;
	private ImageView marketPlacePicture;

	private View confirmOverlay;


	public SettingsRedeemHeartsConfirmFragment() {
		// Required empty public constructor
	}

	public static SettingsRedeemHeartsConfirmFragment newInstance(HLIdentity identity, MarketPlace marketPlace,
	                                                              double heartsValue) {
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, identity);
		args.putSerializable(Constants.EXTRA_PARAM_2, marketPlace);
		args.putDouble(Constants.EXTRA_PARAM_3, heartsValue);
		SettingsRedeemHeartsConfirmFragment fragment = new SettingsRedeemHeartsConfirmFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_settings_redeem_confirm, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_REDEEM_HEARTS_SELECTION);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		settingsActivityListener.refreshProfilePicture(mUser.getUserAvatarURL());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				mIdentity = (HLIdentity) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				mMarketPlace = (MarketPlace) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				hlHeartsValue = savedInstanceState.getDouble(Constants.EXTRA_PARAM_3, 0);
		}
	}

	@Override
	public void onClick(View v) {
		if (Utils.isContextValid(getActivity())) {
			FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

			switch (v.getId()) {
				case R.id.edit_btn:
					fragmentManager.popBackStack();
					break;

				case R.id.confirm_btn:
					callRedeem();
					break;

				case R.id.done_btn:
					fragmentManager.popBackStack(SettingsFragment.LOG_TAG, 0);
					break;

			}
		}
	}

	@Override
	public void onBackPressed() {
		if (Utils.isContextValid(getActivity())) {
			FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

			fragmentManager.popBackStack(SettingsFragment.LOG_TAG, 0);

			if (getActivity() instanceof SettingsActivity)
				((SettingsActivity) getActivity()).setBackListener(null);
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (operationId == Constants.SERVER_OP_REDEEM_HEARTS)
			activateOverlay();
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode, String description) {

		String msg = getString(R.string.error_generic_operation);
		if (operationId == Constants.SERVER_OP_REDEEM_HEARTS && errorCode == Constants.SERVER_ERROR_REDEEM_NOT_ENOUGH_HEARTS)
			msg = description;
		activityListener.showAlert(msg);
	}

	@Override
	public void onMissingConnection(int operationId) {}

	@Override
	protected void configureLayout(@NonNull View view) {

		declaredAmount = view.findViewById(R.id.declared_amount);
		convertedAmount = view.findViewById(R.id.converted_amount);
		method = view.findViewById(R.id.redeem_method);

//		View picture = view.findViewById(R.id.mktplace_picture);
		marketPlacePicture = view.findViewById(R.id.mktplace_picture);

		confirmOverlay = view.findViewById(R.id.confirm_overlay);

		view.findViewById(R.id.edit_btn).setOnClickListener(this);
		view.findViewById(R.id.confirm_btn).setOnClickListener(this);
		view.findViewById(R.id.done_btn).setOnClickListener(this);
	}

	@Override
	protected void setLayout() {

		settingsActivityListener.setToolbarTitle(R.string.section_redeem_title);
		if (mIdentity != null)
			settingsActivityListener.refreshProfilePicture(mIdentity.getAvatarURL());

		if (mMarketPlace != null) {
			mConvertedHearts = mMarketPlace.revertCurrentConversion(hlHeartsValue);
			declaredAmount.setText(Utils.formatNumberWithCommas(mConvertedHearts));

			convertedAmount.setText(mMarketPlace.getReadableMoneyConverted());

			MediaHelper.loadPictureWithGlide(getContext(), mMarketPlace.getAvatarURL(), marketPlacePicture);

			method.setText(
					String.format(
							Locale.getDefault(),
							"%s?",
							getString(mMarketPlace.isCash(getContext()) ? R.string.redeem_method_cash : R.string.redeem_method_gift)
					)
			);
		}
	}


	private void callRedeem() {
		Object[] result = null;

		try {
			result = HLServerCalls.redeemHearts(mIdentity.getId(), mMarketPlace, mConvertedHearts);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (Utils.isContextValid(getActivity())) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
		}
	}


	private void activateOverlay() {
		if (confirmOverlay != null) {
			confirmOverlay
					.animate()
					.alpha(1)
					.setDuration(300)
					.setListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {
							confirmOverlay.setVisibility(View.VISIBLE);
						}

						@Override
						public void onAnimationEnd(Animator animation) {}

						@Override
						public void onAnimationCancel(Animator animation) {}

						@Override
						public void onAnimationRepeat(Animator animation) {}
					})
					.start();
		}
	}

}
