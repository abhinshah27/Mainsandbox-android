/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.base;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.chat.ChatActivityListener;
import it.keybiz.lbsapp.corporate.features.globalSearch.GlobalSearchActivityListener;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivityListener;
import it.keybiz.lbsapp.corporate.features.settings.SettingsActivityListener;
import it.keybiz.lbsapp.corporate.features.wishes.WishesActivityListener;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public abstract class HLFragment extends Fragment implements OnServerMessageReceivedListener {

	public static final String LOG_TAG = HLFragment.class.getCanonicalName();

	protected BasicInteractionListener activityListener;
	protected ProfileActivityListener profileActivityListener;
	protected WishesActivityListener wishesActivityListener;
	protected SettingsActivityListener settingsActivityListener;
	protected GlobalSearchActivityListener globalSearchActivityListener;
	protected ChatActivityListener chatActivityListener;

	protected ServerMessageReceiver serverMessageReceiver = new ServerMessageReceiver();

	protected Realm realm;

	protected HLUser mUser;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		LogUtils.d(LOG_TAG, "onCreateView() HIT");

		realm = RealmUtils.getCheckedRealm();
		mUser = new HLUser().readUser(realm);
		Utils.logUserForCrashlytics(mUser);

		return null;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		LogUtils.d(LOG_TAG, "onActivityCreated() HIT");

		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);

		if (getActivity() != null) {
//			getActivity().registerReceiver(serverMessageReceiver,
//					new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
//			getActivity().registerReceiver(serverMessageReceiver,
//					new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));
			LocalBroadcastManager.getInstance(getActivity()).registerReceiver(serverMessageReceiver,
					new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
//			LocalBroadcastManager.getInstance(getActivity()).registerReceiver(serverMessageReceiver,
//					new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		LogUtils.d(LOG_TAG, "onStart() HIT");

		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);
	}

	@Override
	public void onResume() {
		super.onResume();
		LogUtils.d(LOG_TAG, "onResume() HIT");

		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);

		if (getActivity() != null) {
//			getActivity().registerReceiver(serverMessageReceiver,
//					new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
//			getActivity().registerReceiver(serverMessageReceiver,
//					new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));
		    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(serverMessageReceiver,
					new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
//			LocalBroadcastManager.getInstance(getActivity()).registerReceiver(serverMessageReceiver,
//					new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));
		}
	}

	@Override
	public void onPause() {

		try {
			if (getActivity() != null)
				LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(serverMessageReceiver);
//				getActivity().unregisterReceiver(serverMessageReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		super.onPause();
	}

	@Override
	public void onDestroyView() {
		RealmUtils.closeRealm(realm);
		super.onDestroyView();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		if (context instanceof BasicInteractionListener)
			activityListener = (BasicInteractionListener) context;

		if (context instanceof ProfileActivityListener)
			profileActivityListener = (ProfileActivityListener) context;
		if (context instanceof WishesActivityListener)
			wishesActivityListener = (WishesActivityListener) context;
		if (context instanceof SettingsActivityListener)
			settingsActivityListener = (SettingsActivityListener) context;
		if (context instanceof GlobalSearchActivityListener)
			globalSearchActivityListener = (GlobalSearchActivityListener) context;
		if (context instanceof ChatActivityListener)
			chatActivityListener = (ChatActivityListener) context;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof BasicInteractionListener)
			activityListener = (BasicInteractionListener) activity;

		if (activity instanceof ProfileActivityListener)
			profileActivityListener = (ProfileActivityListener) activity;
		if (activity instanceof WishesActivityListener)
			wishesActivityListener = (WishesActivityListener) activity;
		if (activity instanceof SettingsActivityListener)
			settingsActivityListener = (SettingsActivityListener) activity;
		if (activity instanceof GlobalSearchActivityListener)
			globalSearchActivityListener = (GlobalSearchActivityListener) activity;
		if (activity instanceof ChatActivityListener)
			chatActivityListener = (ChatActivityListener) activity;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		switch (operationId) {
			case Constants.SERVER_OP_CREATE_POST_V2:
				LogUtils.d(LOG_TAG, "Post creation SUCCESS with object " + responseObject.toString());

				// TODO: 10/15/2017    do something???
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		switch (operationId) {
			case Constants.SERVER_OP_CREATE_POST_V2:
				LogUtils.e(LOG_TAG, "Post creation FAIL with error " + errorCode);
				activityListener.showAlert(R.string.error_creating_post);
				break;
		}
	}

	protected void configureToolbar(Toolbar toolbar, int toolbarTitleID,
	                                int toolbarTitleStringId, boolean showBack){
		if(toolbar != null) {
			if (toolbarTitleID != -1 && toolbarTitleStringId != -1) {
				TextView titleToolbar = toolbar.findViewById(toolbarTitleID);
				titleToolbar.setText(toolbarTitleStringId);
			}
			if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
				((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
				ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
				if (actionbar != null) {
					actionbar.setDisplayShowTitleEnabled(false);
					actionbar.setDisplayShowHomeEnabled(showBack);
					actionbar.setDisplayHomeAsUpEnabled(showBack);
				}
			}
		}
	}

	protected abstract void onRestoreInstanceState(Bundle savedInstanceState);
	protected abstract void configureResponseReceiver();
	protected abstract void configureLayout(@NonNull View view);
	protected abstract void setLayout();

}
