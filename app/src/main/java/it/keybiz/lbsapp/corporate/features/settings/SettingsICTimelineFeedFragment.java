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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
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
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/22/2018.
 */
public class SettingsICTimelineFeedFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener {

	private static String[] itemsFirstLevel;

	private TextView title;
	private ViewGroup itemsContainer;

	private int sortOrderSelected = Constants.SERVER_SORT_TL_ENUM_RECENT;
	private int autoPlaySelected = Constants.SERVER_AUTOPLAY_TL_ENUM_WIFI;


	public SettingsICTimelineFeedFragment() {
		// Required empty public constructor
	}

	public static SettingsICTimelineFeedFragment newInstance() {
		Bundle args = new Bundle();

		SettingsICTimelineFeedFragment fragment = new SettingsICTimelineFeedFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.layout_simple_title_container, container, false);

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

		if (Utils.isContextValid(getActivity()))
			itemsFirstLevel = getResources().getStringArray(R.array.settings_inner_circle_feed);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_FEED);

		getInfo();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

	}

	@Override
	public void onBackPressed() {
		saveInfo();
	}

	private void saveInfo() {
		Bundle dataBundle = new Bundle();
		dataBundle.putString("type", "set");
		dataBundle.putInt("sortOrder", sortOrderSelected);
		dataBundle.putInt("autoPlayVideos", autoPlaySelected);
		callServer(mType = CallType.SET, dataBundle);
	}

	private void getInfo() {
		Bundle dataBundle = new Bundle();
		dataBundle.putString("type", "get");
		callServer(mType = CallType.GET, dataBundle);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

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
			case Constants.SERVER_OP_SETTINGS_IC_GET_SET_FEED:

				if (mType == CallType.GET) {
					JSONObject feed = responseObject.optJSONObject(0).optJSONObject("timeLineFeedPrefs");
					if (feed != null && feed.length() > 0) {
						sortOrderSelected = feed.optInt("sortOrder", Constants.SERVER_SORT_TL_ENUM_DEFAULT);
						if (sortOrderSelected == Constants.SERVER_SORT_TL_ENUM_DEFAULT)
							sortOrderSelected = Constants.SERVER_SORT_TL_ENUM_RECENT;
						autoPlaySelected = feed.optInt("autoPlayVideos", Constants.SERVER_AUTOPLAY_TL_ENUM_WIFI);
					}
				}
				else if (mType != CallType.SET)
					handleErrorResponse(operationId, 0);


				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {

						mUser.getSettings().setRawSortOrder(sortOrderSelected);
						mUser.getSettings().setRawAutoplayVideo(autoPlaySelected);

						if (mType == CallType.GET)
							setLayout();
						else if (mType == CallType.SET)
							settingsActivityListener.closeScreen();
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
					settingsActivityListener.closeScreen();
				}
				return;
		}

		if (mType == CallType.SET) {
			activityListener.showGenericError();
			settingsActivityListener.closeScreen();
		}
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
		title = view.findViewById(R.id.title);
		itemsContainer = view.findViewById(R.id.items_container);
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_inner_circle);

		title.setText(R.string.settings_ic_feed);

		sortOrderSelected = mUser.getSettingSortOrder(true);
		if (sortOrderSelected == Constants.SERVER_SORT_TL_ENUM_DEFAULT)
			sortOrderSelected = Constants.SERVER_SORT_TL_ENUM_RECENT;
		autoPlaySelected = mUser.getSettingAutoPlay();

		if (itemsFirstLevel != null && itemsFirstLevel.length > 0 && Utils.isContextValid(getContext())) {
			itemsContainer.removeAllViews();

			for (int i = 0; i < itemsFirstLevel.length; i++) {
				final int position = i;
				final View container = LayoutInflater.from(getContext()).inflate(R.layout.item_settings_feed, itemsContainer, false);
				if (container != null) {
					((TextView) container.findViewById(R.id.action_text)).setText(itemsFirstLevel[i]);
					final ViewGroup secondLevelContainer = container.findViewById(R.id.child_layout);

					container.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (container.isActivated()) {
								container.setActivated(false);
								secondLevelContainer.setVisibility(View.GONE);
							}
							else {
								container.setActivated(true);
								secondLevelContainer.setVisibility(View.VISIBLE);
							}
						}
					});

					int[] values = null;
					String[] strings = null;
					if (secondLevelContainer != null) {
						secondLevelContainer.removeAllViews();

						switch (i) {
							// client knows that pos 0 is sortOrder setting
							case 0:
								values = getResources().getIntArray(R.array.settings_ic_feed_sort_values);
								strings = getResources().getStringArray(R.array.settings_ic_feed_sort);
								break;

							// client knows that pos 1 is autoPlay setting
							case 1:
								values = getResources().getIntArray(R.array.settings_ic_feed_ap_values);
								strings = getResources().getStringArray(R.array.settings_ic_feed_autoplay);
								break;
						}

						if (values != null && values.length == strings.length) {
							for (int j = 0; j < strings.length; j++) {
								final int secLevelposition = j;
								final View checkbox = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_checkbox, secondLevelContainer, false);
								if (checkbox != null) {
									((TextView) checkbox.findViewById(R.id.text)).setText(strings[j]);

									checkbox.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											checkbox.setSelected(true);
											switch (position) {
												case 0:
													sortOrderSelected = (int) checkbox.getTag();
													break;
												case 1:
													autoPlaySelected = (int) checkbox.getTag();
													break;
											}

											for (int k = 0; k < secondLevelContainer.getChildCount(); k++) {
												if (k != secLevelposition) {
													secondLevelContainer.getChildAt(k).setSelected(false);
												}
											}
										}
									});

									checkbox.setTag(values[j]);
									switch (i) {
										case 0:
											checkbox.setSelected(values[j] == sortOrderSelected);
											break;
										case 1:
											checkbox.setSelected(values[j] == autoPlaySelected);
											break;
									}

									secondLevelContainer.addView(checkbox);
								}
							}
						}
					}

					itemsContainer.addView(container);
				}
			}
		}
	}


	public enum CallType { GET, SET }
	private CallType mType;
	private void callServer(CallType type, @NonNull Bundle dataBundle) {
		Object[] result = null;

		try {
			result = HLServerCalls.settingsOperationsOnFeedSettings(mUser.getUserId(), dataBundle, type);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}

}
