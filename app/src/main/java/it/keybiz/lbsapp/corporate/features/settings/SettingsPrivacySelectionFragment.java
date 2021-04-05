/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmList;
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
import it.keybiz.lbsapp.corporate.models.enums.PrivacyEntriesEnum;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/22/2018.
 */
public class SettingsPrivacySelectionFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener {

	public static final String LOG_TAG = SettingsPrivacySelectionFragment.class.getCanonicalName();

	private String title;

	private int privacyEntry = -1;
	private PrivacySubItem selectedPref;

	private TextView titleView;
	private ViewGroup itemsContainer;


	public SettingsPrivacySelectionFragment() {
		// Required empty public constructor
	}

	public static SettingsPrivacySelectionFragment newInstance(int privacyEntry, String title) {
		Bundle args = new Bundle();
		args.putInt(Constants.EXTRA_PARAM_1, privacyEntry);
		args.putString(Constants.EXTRA_PARAM_2, title);

		SettingsPrivacySelectionFragment fragment = new SettingsPrivacySelectionFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_settings_privacy_checkboxes, container, false);

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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_PRIVACY_SELECTION);

		getInfo();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				privacyEntry = savedInstanceState.getInt(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				title = savedInstanceState.getString(Constants.EXTRA_PARAM_2);

			if (privacyEntry >= 0)
				selectedPref = new PrivacySubItem(privacyEntry);
		}
	}

	@Override
	public void onBackPressed() {
		saveInfo();
	}

	private void saveInfo() {
		if (selectedPref != null && selectedPref.getIndex() >= 0) {
			Bundle dataBundle = new Bundle();
			dataBundle.putInt("idxSubItem", selectedPref.getIndex());
			if (selectedPref.hasValues())
				dataBundle.putStringArrayList("values", selectedPref.getValues());
			callServer(mType = CallType.SET);
		}
		else {
			activityListener.showAlert(R.string.error_unknown);
			settingsActivityListener.closeScreen();
		}
	}

	private void getInfo() {
		callServer(mType = CallType.GET);
	}

	@Override
	public void onClick(View v) {
		int index = (int) v.getTag();

		v.setSelected(true);
		selectedPref = new PrivacySubItem(privacyEntry, (int) v.getTag(), true);

		for (int k = 0; k < itemsContainer.getChildCount(); k++) {
			if (k != index) {
				itemsContainer.getChildAt(k).setSelected(false);
			}
		}

		if (PrivacyEntriesEnum.toEnum(privacyEntry) == PrivacyEntriesEnum.POST_VISIBILITY) {
			PrivacyPostVisibilityEnum postVisibility = PrivacyPostVisibilityEnum.toEnum(index);
			if (postVisibility == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES ||
					postVisibility == PrivacyPostVisibilityEnum.ONLY_SELECTED_USERS) {

				settingsActivityListener.showPrivacyPostVisibilitySelectionFragment(selectedPref, postVisibility);
			}
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (!Utils.isContextValid(getActivity()) || !(getActivity() instanceof HLActivity)) return;

		switch (requestCode) {


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
			case Constants.SERVER_OP_SETTINGS_PRIVACY_GET_SET:

				if (mType == CallType.GET) {
					JSONObject item = responseObject.optJSONObject(0);
					if (item != null && item.length() > 0) {
						privacyEntry = item.optInt("idxItem");
						JSONObject subItem = item.optJSONObject("item");
						if (subItem != null && subItem.length() > 0) {
							selectedPref = new PrivacySubItem(
									privacyEntry,
									subItem.optInt("idxSubItem", -1),
									subItem.optBoolean("selected", false)
							);
						}
					}
				}
				else if (mType != CallType.SET)
					handleErrorResponse(operationId, 0);

				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {

						if (mType == CallType.GET)
							setLayout();
						else if (mType == CallType.SET) {

							if (selectedPref != null && PrivacyEntriesEnum.toEnum(selectedPref.getParentIndex()) == PrivacyEntriesEnum.POST_VISIBILITY) {
								if (mUser != null && mUser.getSettings() != null) {
									mUser.getSettings().setRawPostVisibility(selectedPref.getIndex());

									if (mUser.getSettings().getValues() == null)
										mUser.getSettings().setValues(new RealmList<String>());
									else
										mUser.getSettings().getValues().clear();
									mUser.getSettings().getValues().addAll(selectedPref.getValues());
								}
							}

							settingsActivityListener.closeScreen();
						}
					}
				});

				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_IC_GET_SET_FEED:
				if (mType == CallType.GET) {
					activityListener.showAlert(R.string.error_generic_update);
					return;
				}
				else if (mType == CallType.SET) {
					activityListener.showGenericError();
				}
				break;
		}

		if (mType == CallType.SET)
			settingsActivityListener.closeScreen();
	}

	@Override
	public void onMissingConnection(int operationId) {
		if (mType == CallType.SET)
			settingsActivityListener.closeScreen();
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

		itemsContainer = view.findViewById(R.id.items_container);
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_privacy);

		titleView.setText(title);

		if (privacyEntry > -1) {
			@ArrayRes int arrayResId = 0;
			PrivacyEntriesEnum entry = PrivacyEntriesEnum.toEnum(privacyEntry);
			if (entry != null) {
				switch (entry) {
					case POST_VISIBILITY:
						arrayResId = R.array.settings_privacy_post_visib;
						break;
					case WHO_CAN_INCLUDE_ME:
						arrayResId = R.array.settings_privacy_include_ic;
						break;
					case WHO_CAN_SEE_MY_IC:
						arrayResId = R.array.settings_privacy_see_ic;
						break;
					case WHO_CAN_LOOK_ME_UP:
						arrayResId = R.array.settings_privacy_look_up;
						break;
					case WHO_CAN_COMMENT:
						arrayResId = R.array.settings_privacy_comment;
						break;
					case REVIEW_TAGS:
						arrayResId = R.array.settings_privacy_review;
						break;
					case WHO_CAN_CHAT:
						arrayResId = R.array.settings_privacy_chat_calls;
						break;
					case WHO_CAN_VIDEO:
						arrayResId = R.array.settings_privacy_chat_calls;
						break;
					case WHO_CAN_VOICE:
						arrayResId = R.array.settings_privacy_chat_calls;
						break;
				}

				if (arrayResId != 0) {
					String[] items = getResources().getStringArray(arrayResId);
					if (items.length > 0 && Utils.isContextValid(getContext())) {
						itemsContainer.removeAllViews();

						for (int j = 0; j < items.length; j++) {
							final View checkbox = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_checkbox, itemsContainer, false);
							if (checkbox != null) {
								((TextView) checkbox.findViewById(R.id.text)).setText(items[j]);
								checkbox.setOnClickListener(this);
								checkbox.setTag(j);
								if (selectedPref != null && selectedPref.getIndex() >= 0)
									checkbox.setSelected(j == selectedPref.getIndex());

								itemsContainer.addView(checkbox);
							}
						}
					}
				}
			}
		}
	}


	public enum CallType { GET, SET }
	private CallType mType;
	private void callServer(CallType type) {
		Object[] result = null;

		try {
			result = HLServerCalls.settingsOperationsOnPrivacy(mUser.getUserId(), privacyEntry,
					selectedPref, type);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}



	public class PrivacySubItem implements Serializable {

		private int parentIndex;

		private int index = -1;
		private boolean selected = true;
		private ArrayList<String> values = new ArrayList<>();


		PrivacySubItem(int privacyEntry) {
			this.parentIndex = privacyEntry;
			setDefaultIndex(privacyEntry);
		}

		PrivacySubItem(int privacyEntry, int index, boolean selected) {
			this.parentIndex = privacyEntry;
			this.index = index;
			this.selected = selected;
		}

		public boolean hasValues() {
			return values != null && !values.isEmpty();
		}

		public boolean isSelected() {
			return selected;
		}

		public int getIndex() {
			return index;
		}

		void setDefaultIndex(int privacyEntry) {
			PrivacyEntriesEnum entry = PrivacyEntriesEnum.toEnum(privacyEntry);
			if (entry != null) {
				switch (entry) {
					case POST_VISIBILITY:
						index = 0;
						break;
					case WHO_CAN_INCLUDE_ME:
						index = 0;
						break;
					case WHO_CAN_SEE_MY_IC:
						index = 1;
						break;
					case WHO_CAN_LOOK_ME_UP:
						index = 0;
						break;
					case WHO_CAN_COMMENT:
						index = 1;
						break;
					case REVIEW_TAGS:
						index = 0;
						break;
					case WHO_CAN_CHAT:
						index = 2;
						break;
					case WHO_CAN_VIDEO:
						index = 2;
						break;
					case WHO_CAN_VOICE:
						index = 2;
						break;
				}
			}
		}

		int getParentIndex() {
			return parentIndex;
		}

		public ArrayList<String> getValues() {
			return values;
		}
		public void setValues(ArrayList<String> values) {
			this.values = values;
		}
	}

}
