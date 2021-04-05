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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class WishPreviewFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener {

	public static final String LOG_TAG = WishPreviewFragment.class.getCanonicalName();

	public enum UsageType { IN_WIZARD, VIEW_SAVED_WISH }
	private UsageType mType;

	private String wishId;
	private String wishName;
	private boolean isEditMode;     // edit mode for wish is currently disabled but field needed for behavior purposes

	private CardView wishPreviewCard;

	private TextView toolbarTitle;

	private ImageView wishCover, placeholderIcon;
	private TextView wishTitle;
	private TextView wishTrigger;
	private View repetitionLayout;
	private TextView wishRepetitions;
	private TextView wishAction;
	private TextView wishRecipient;
	private Button mainButton;

	private TextView infoText;


	public WishPreviewFragment() {
		// Required empty public constructor
	}

	public static WishPreviewFragment newInstance(String wishId, String wishName, UsageType type) {
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, wishId);
		args.putString(Constants.EXTRA_PARAM_2, wishName);
		args.putSerializable(Constants.EXTRA_PARAM_3, type);
		WishPreviewFragment fragment = new WishPreviewFragment();
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

		View view = inflater.inflate(R.layout.fragment_wish_preview, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		wishesActivityListener.setProgressMessageForFinalOps(R.string.saving_wish);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_PREVIEW);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		callServer(CallType.SUMMARY);

		setLayout(null);
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
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				wishId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				wishName = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				mType = (UsageType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_3);
		}
		
		isEditMode = Utils.areStringsValid(wishId, wishName);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_action:
				if (Utils.isStringValid(wishId))
					wishesActivityListener.showWishNameFragment();
				else
					callServer(CallType.SAVE);
				break;
		}
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

		activityListener.closeProgress();

		if (responseObject == null || responseObject.length() == 0)
			return;

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_SUMMARY:
				JSONObject json = responseObject.optJSONObject(0).optJSONObject("summary");
				setLayout(json);
				break;

			case Constants.SERVER_OP_WISH_SAVE:
				if (Utils.isContextValid(getActivity())) {
					startActivity(new Intent(getActivity(), SavedWishesActivity.class));
					getActivity().finish();
					getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
				}
				break;

			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				JSONArray array = responseObject.optJSONObject(0).optJSONArray("items");
				if (array != null && array.length() == 1) {
					WishListElement wli = new WishListElement().deserializeToClass(array.optJSONObject(0));
					wishesActivityListener.setSelectedWishListElement(wli);
				}

				/* NOT USED */
				wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
				wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		activityListener.closeProgress();

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
		wishPreviewCard = view.findViewById(R.id.wish_preview_card);

		wishCover = view.findViewById(R.id.image_preview);
		placeholderIcon = view.findViewById(R.id.placeholder_icon);

		wishTitle = view.findViewById(R.id.title);
		wishTrigger = view.findViewById(R.id.trigger);

		repetitionLayout = view.findViewById(R.id.cwish_layout_repetitions);
		wishRepetitions = view.findViewById(R.id.repeat);

		wishAction = view.findViewById(R.id.action);
		wishRecipient = view.findViewById(R.id.to);

		mainButton = view.findViewById(R.id.btn_action);
		mainButton.setOnClickListener(this);

		infoText = view.findViewById(R.id.info_text);
	}

	@Override
	protected void setLayout() {}

	private void setLayout(JSONObject response) {
		wishesActivityListener.getStepTitle().setVisibility(View.GONE);
		wishesActivityListener.getStepSubTitle().setVisibility(View.GONE);

		wishesActivityListener.setToolbarTitle(R.string.wish_toolbar_preview);

//		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) wishPreviewCard.getLayoutParams();
//		int m20 = Utils.dpToPx(20f, getResources());
//		lp.setMargins(m20, /*Utils.dpToPx(-25f, getResources())*/-(m20), m20, m20);

		wishTitle.setText(Utils.isStringValid(wishName) ? wishName : wishesActivityListener.getWishName());

		// EDIT WISH mode is currently disabled
		mainButton.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
		mainButton.setText(isEditMode ? R.string.wish_preview_edit : R.string.wish_preview_save);

		infoText.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
		if (isEditMode)
			wishesActivityListener.hideStepsBar();
		else if (mUser.hasAPreferredInterest()) {
			infoText.setText(getString(R.string.wish_preview_info_txt, mUser.getPreferredInterest().getName()));
			infoText.setVisibility(View.VISIBLE);
		}

		try {
			if (response != null && response.length() > 0) {
				if (response.has("trigger"))
					wishTrigger.setText(response.getString("trigger"));

				if (response.has("repetition")) {
					wishRepetitions.setText(response.getString("repetition"));
					repetitionLayout.setVisibility(View.VISIBLE);
				}
				else
					repetitionLayout.setVisibility(View.GONE);

				if (response.has("action"))
					wishAction.setText(response.getString("action"));

				if (response.has("recipient"))
					wishRecipient.setText(response.getString("recipient"));

				if (response.has("wishImageURL")) {
					String url = response.optString("wishImageURL");
					CardView.LayoutParams lp = (CardView.LayoutParams) placeholderIcon.getLayoutParams();
					if (Utils.isStringValid(url)) {

						if (Utils.hasLollipop())
							MediaHelper.loadPictureWithGlide(getContext(), url, wishCover);
						else
							MediaHelper.roundPictureCorners(wishCover, url);

						placeholderIcon.setImageResource(R.drawable.ic_placeholder_image);
						lp.height = Utils.dpToPx(80f, getResources());
						lp.width = Utils.dpToPx(80f, getResources());
					}
					else {
						placeholderIcon.setImageResource(R.drawable.ic_placeholder_wish);
						lp.height = Utils.dpToPx(100f, getResources());
						lp.width = Utils.dpToPx(100f, getResources());
					}
					placeholderIcon.setLayoutParams(lp);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private enum CallType { SUMMARY, SAVE }
	private void callServer(CallType type) {
		Object[] result = null;

		try {
			if (type == CallType.SUMMARY) {
				// TODO: 9/20/2018    userID now added only if we are looking at the fragment at the end of the creation wizard
				String userId = mType == UsageType.IN_WIZARD ? mUser.getUserId() : null;
				result = HLServerCalls.getSummary(userId, wishId);
			}
			else if (type == CallType.SAVE) {
				activityListener.openProgress();
				result = HLServerCalls.saveWish(mUser.getUserId(), wishesActivityListener.getWishName());
			}
		} catch (JSONException e){
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
	}

}
