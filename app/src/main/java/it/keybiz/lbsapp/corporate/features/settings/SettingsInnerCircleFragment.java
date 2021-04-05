/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.content.Intent;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/21/2018.
 */
public class SettingsInnerCircleFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener {

	public static final String LOG_TAG = SettingsInnerCircleFragment.class.getCanonicalName();

	private ImageView /*pictureLegacy, */picturePreferred;
	private TextView /*nameLegacy, */namePreferred;
	private TextView /*changeLegacy, */changePreferred;

	private LinearLayout entriesLayout;

//	private HLUserGeneric legacyContact;
//	private String rawLegacyStatus;
	private Interest preferredInterest;


	private static final UnderlineSpan UNDERLINE_SPAN = new UnderlineSpan();
	private static ForegroundColorSpan cssp;

	private static String[] entries;


	public SettingsInnerCircleFragment() {
		// Required empty public constructor
	}

	public static SettingsInnerCircleFragment newInstance() {
		Bundle args = new Bundle();

		SettingsInnerCircleFragment fragment = new SettingsInnerCircleFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_settings_inner_circle, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(null);

		if (Utils.isContextValid(getActivity())) {
			cssp = new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.black_38));
			entries = getResources().getStringArray(R.array.settings_inner_circle);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_INNER_CIRCLE);

		callServer();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (Utils.isContextValid(getActivity()))
			Utils.closeKeyboard(getActivity());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

	}

	@Override
	public void onBackPressed() {

	}



	@Override
	public void onClick(View v) {
		switch (v.getId()) {
//			case R.id.layout_change_legacy:
//				startActivityForResult(new Intent(v.getContext(), LegacyContactSelectionActivity.class), Constants.RESULT_SELECT_LEGACY_CONTACT);
//				break;

			case R.id.layout_change_preferred:
				ProfileActivity.openMyInterestsFragment(v.getContext(), mUser.getUserId(), mUser.getCompleteName(), mUser.getUserAvatarURL());
				break;
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (!Utils.isContextValid(getActivity()) || !(getActivity() instanceof HLActivity)) return;

		switch (requestCode) {
//			case Constants.RESULT_SELECT_LEGACY_CONTACT:
//				if (resultCode == Activity.RESULT_OK)
//					LogUtils.d(LOG_TAG, "Legacy contact updated. UI refresh should happen automatically");
//				else if (resultCode == Activity.RESULT_CANCELED)
//					LogUtils.d(LOG_TAG, "Legacy contact operation canceled");
//				break;
		}
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

		if (responseObject == null || responseObject.length() == 0) {
			handleErrorResponse(operationId, 0);
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_IC_GET:

				JSONObject json = responseObject.optJSONObject(0);
				if (json != null && json.length() > 0) {
//					JSONObject contact = json.optJSONObject("legacyContact");
//					if (contact != null && contact.length() > 0) {
//						legacyContact = new HLUserGeneric().deserializeToClass(contact);
//						rawLegacyStatus = contact.optString("rawLegacyContactStatus");
//					}
					preferredInterest = new Interest().deserializeToClass(json.optJSONObject("preferredInterest"));
				}

				realm.executeTransaction(realm -> {

//						mUser.getSettings().setLegacyContact(realm.copyToRealmOrUpdate(legacyContact));
//						mUser.getSettings().setLegacyContactRawStatus(rawLegacyStatus);
					mUser.getSettings().setPreferredInterest(realm.copyToRealmOrUpdate(preferredInterest));
					mUser.setHasAPreferredInterest(preferredInterest != null);

					setLayout();
				});

				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		@StringRes int msg = R.string.error_generic_update;
		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_IC_GET:
				break;
		}

		activityListener.showAlert(msg);
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		view.findViewById(R.id.layout_change_legacy).setOnClickListener(this);
		view.findViewById(R.id.layout_change_preferred).setOnClickListener(this);

//		pictureLegacy = view.findViewById(R.id.legacy_picture);
//		nameLegacy = view.findViewById(R.id.legacy_name);
		picturePreferred = view.findViewById(R.id.preferred_picture);
		namePreferred = view.findViewById(R.id.preferred_name);

//		changeLegacy = view.findViewById(R.id.change_legacy_btn);
		changePreferred = view.findViewById(R.id.change_preferred_btn);

		entriesLayout = view.findViewById(R.id.entries_layout);
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_inner_circle);

//		legacyContact = mUser.hasLegacyContact() ? mUser.getLegacyContact() : null;
		preferredInterest = mUser.hasAPreferredInterest() ? mUser.getPreferredInterest() : null;

//		if (legacyContact != null) {
//			MediaHelper.loadProfilePictureWithPlaceholder(getContext(), legacyContact.getAvatarURL(), pictureLegacy);
//			nameLegacy.setText(legacyContact.getCompleteName());
//			Utils.applySpansToTextView(changeLegacy, R.string.action_change, UNDERLINE_SPAN, cssp);
//			changeLegacy.setVisibility(View.VISIBLE);
//		}
//		else {
//			if (mUser.canRequestLegacyContact()) {
//				Utils.applySpansToTextView(changeLegacy, R.string.action_add, UNDERLINE_SPAN, cssp);
//				changeLegacy.setVisibility(View.VISIBLE);
//			}
//			else {
//				changeLegacy.setVisibility(View.GONE);
//				nameLegacy.setText(R.string.settings_legacy_pending);
//			}
//		}

		if (preferredInterest != null) {
			MediaHelper.loadProfilePictureWithPlaceholder(getContext(), preferredInterest.getAvatarURL(), picturePreferred);
			namePreferred.setText(preferredInterest.getName());
			Utils.applySpansToTextView(changePreferred, R.string.action_change, UNDERLINE_SPAN, cssp);
		}
		else {
			Utils.applySpansToTextView(changePreferred, R.string.action_add, UNDERLINE_SPAN, cssp);
		}


		if (entries == null)
			entries = getResources().getStringArray(R.array.settings_inner_circle);

		if (entries.length > 0 && entriesLayout != null) {
			entriesLayout.removeAllViews();

			// -1 justified because in string array also the only supposed item by DEVs is present: "You are legacy contact to"
			for (int i = 0; i < entries.length; i++) {
				View entryLayout = LayoutInflater.from(entriesLayout.getContext())
						.inflate(R.layout.item_settings_entry, entriesLayout, false);

				final int position = i;

				if (entryLayout != null) {
					// enables light gray background if row has even position number
					entryLayout.setActivated((position % 2) == 0);

					entryLayout.setOnClickListener(v -> navigateInnerCircleSettings(position));

					((TextView) entryLayout.findViewById(R.id.action_text)).setText(entries[i]);

					entriesLayout.addView(entryLayout);
				}
			}
		}
	}


	private void callServer() {
		Object[] result = null;

		try {
			result = HLServerCalls.getSettings(mUser.getUserId(), HLServerCalls.SettingType.INNER_CIRCLE);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}


	private void navigateInnerCircleSettings(int position) {
		if (position >= 0) {
			switch (position) {
				case 0:
					settingsActivityListener.showInnerCircleCirclesFragment();
					break;
				case 1:
					settingsActivityListener.showInnerCircleTimelineFeedFragment();
					break;
				case 2:
//					Toast.makeText(getContext(), "Let's do some Legacy", Toast.LENGTH_SHORT).show();
					break;
			}
		}
	}

	//endregion

}
