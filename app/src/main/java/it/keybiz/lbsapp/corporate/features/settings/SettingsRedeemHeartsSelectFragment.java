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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLIdentity;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.MarketPlace;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsRedeemHeartsSelectFragment extends HLFragment implements OnServerMessageReceivedListener,
		OnMissingConnectionListener, BasicAdapterInteractionsListener {

	public static final String LOG_TAG = SettingsRedeemHeartsSelectFragment.class.getCanonicalName();

	public static double hlHeartsValue;

	private HLIdentity mIdentity;

	private TextView cntHeartsAvailable;
	private long numHeartsAvailable;
	private long numHeartsEntered;

	private EditText enterAmount;
	private View redeemBtn;

	private RecyclerView marketPlacesView;
	private List<MarketPlace> marketPlaces = new ArrayList<>();
	private LinearLayoutManager llm;
	private MarketPlacesAdapter mAdapter;
	private View noResult;

	private SwipeRefreshLayout srl;

	private MarketPlace selectedMarketPlace;


	public SettingsRedeemHeartsSelectFragment() {
		// Required empty public constructor
	}

	public static SettingsRedeemHeartsSelectFragment newInstance(HLIdentity identity) {
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, identity);
		SettingsRedeemHeartsSelectFragment fragment = new SettingsRedeemHeartsSelectFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_settings_redeem_select, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llm = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
		mAdapter = new MarketPlacesAdapter(marketPlaces, this);
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

		callForMarketPlaces();

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
		}
	}

	@Override
	public void onItemClick(Object object) {}

	@Override
	public void onItemClick(Object object, View view) {

		if (object instanceof MarketPlace) {
			((MarketPlace) object).setSelected(true);
			selectedMarketPlace = (MarketPlace) object;

			for (MarketPlace mkt : marketPlaces) {
				if (mkt != selectedMarketPlace)
					mkt.setSelected(false);
			}
			mAdapter.notifyDataSetChanged();

			Utils.closeKeyboard(enterAmount);

			checkButtonAndEnable();
		}
	}

	@Override
	public HLUser getUser() {
		return mUser;
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

		Utils.setRefreshingForSwipeLayout(srl, false);

		if (responseObject == null || responseObject.length() == 0) {
			handleErrorResponse(operationId, 0);
			return;
		}

		JSONObject json = responseObject.optJSONObject(0);
		if (json != null) {
			numHeartsAvailable = json.optLong("availableHearts");
			hlHeartsValue = json.optDouble("heartsValue");

			JSONArray mkts = json.optJSONArray("mktPlaces");
			if (mkts != null && mkts.length() > 0) {
				setData(mkts);
			}
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		marketPlacesView.setVisibility(View.GONE);
		noResult.setVisibility(View.VISIBLE);
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		cntHeartsAvailable = view.findViewById(R.id.count_heart_available);
		enterAmount = view.findViewById(R.id.edit_amount);
		enterAmount.addTextChangedListener(new TextWatcher() {

			boolean block = true;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String string = s.toString().trim().replaceAll("[,.]", "");
				block = Utils.isStringValid(string) && Long.parseLong(string) > numHeartsAvailable;
			}

			@Override
			public void afterTextChanged(Editable s) {
				enterAmount.removeTextChangedListener(this);
				String string;
				if (!block) {
					string = s.toString().trim().replaceAll("[,.]", "");
					if (s.length() > 3) {
						string = Utils.formatNumberWithCommas(numHeartsEntered = Long.parseLong(string));
					} else if (s.length() > 0)
						numHeartsEntered = Long.parseLong(string);

				}
				else {
					string = s.toString().substring(0, s.length() - 1);
				}

				enterAmount.setText(string);
				enterAmount.setSelection(enterAmount.getText().length());
				enterAmount.addTextChangedListener(this);

				updateViews(numHeartsEntered);

			}
		});

		redeemBtn = view.findViewById(R.id.redeem_btn);
		redeemBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				settingsActivityListener.showSettingsRedeemHeartsConfirmFragment(mIdentity, selectedMarketPlace, hlHeartsValue);
			}
		});
		redeemBtn.setEnabled(false);

		marketPlacesView = view.findViewById(R.id.rec_view);
		noResult = view.findViewById(R.id.no_result);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);
				callForMarketPlaces();
			}
		});
	}

	@Override
	protected void setLayout() {

		settingsActivityListener.setToolbarTitle(R.string.section_redeem_title);

		if (mIdentity != null)
			settingsActivityListener.refreshProfilePicture(mIdentity.getAvatarURL());

		cntHeartsAvailable.setText(
				getString(
						R.string.settings_redeem_available_hearts,
						Utils.formatNumberWithCommas(numHeartsAvailable)
				)
		);

		marketPlacesView.setLayoutManager(llm);
		marketPlacesView.setAdapter(mAdapter);

		if (numHeartsEntered > 0)
			enterAmount.setText(String.valueOf(numHeartsEntered));
	}


	private void updateViews(final long value) {

		if (marketPlaces != null && !marketPlaces.isEmpty()) {
			for (int i = 0; i < marketPlaces.size(); i++) {
				MarketPlace marketPlace = marketPlaces.get(i);

				if (marketPlace != null) {
					marketPlace.setCurrentConversion(value, hlHeartsValue);

					if (marketPlace.isSelected() && !marketPlace.isConversionValid())
						marketPlace.setSelected(false);
				}
				mAdapter.notifyItemChanged(i);
			}
		}
		checkButtonAndEnable();
	}

	private void checkButtonAndEnable() {
		redeemBtn.setEnabled(
				numHeartsAvailable >= numHeartsEntered &&
						selectedMarketPlace != null &&
						selectedMarketPlace.isConversionValid()
		);
	}

	private void callForMarketPlaces() {
		Object[] result = null;

		try {
			result = HLServerCalls.getMarketPlaces(mIdentity.getId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (Utils.isContextValid(getActivity())) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
		}
	}

	private void setData(JSONArray responseObject) {
		if (responseObject != null && responseObject.length() > 0) {

			noResult.setVisibility(View.GONE);
			marketPlacesView.setVisibility(View.VISIBLE);

			if (marketPlaces != null)
				marketPlaces.clear();

			for (int i = 0; i < responseObject.length(); i++) {
				JSONObject mkt = responseObject.optJSONObject(i);
				if (mkt != null) {
					MarketPlace place = new MarketPlace().deserializeToClass(mkt);
					place.setSelected(selectedMarketPlace != null && selectedMarketPlace.equals(place));
					marketPlaces.add(place);
				}
			}

			mAdapter.notifyDataSetChanged();

			cntHeartsAvailable.setText(
					getString(
							R.string.settings_redeem_available_hearts,
							Utils.formatNumberWithCommas(numHeartsAvailable)
					)
			);

			if (!canRedeem()) {
				final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(getActivity(), R.layout.custom_dialog_generic_title_text_1btn);
				if (dialog != null) {
					View view = dialog.getCustomView();
					if (view != null) {
						((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.redeem_hearts_title);
						((TextView) view.findViewById(R.id.dialog_content)).setText(R.string.redeem_hearts_message);
						Button close = view.findViewById(R.id.close_btn);
						close.setText(R.string.action_close);
						close.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (Utils.isContextValid(getActivity()))
									getActivity().onBackPressed();
								dialog.dismiss();
							}
						});
					}
					dialog.show();
				}
			}

			if (numHeartsEntered > 0)
				enterAmount.setText(String.valueOf(numHeartsEntered));
		}
		else {
			marketPlacesView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
		}
	}


	private boolean canRedeem() {
		if (numHeartsAvailable == 0 || hlHeartsValue == 0)
			return false;

		boolean can = false;
		if (marketPlaces != null && !marketPlaces.isEmpty()) {
			for (MarketPlace mkt :
					marketPlaces) {
				if (mkt != null) {
					mkt.setCurrentConversion(numHeartsAvailable, hlHeartsValue);

					if (mkt.isConversionValid())
						can = true;
					mkt.setCurrentConversion(0);

					if (can)
						return true;
				}
			}
		}

		return false;
	}

}
