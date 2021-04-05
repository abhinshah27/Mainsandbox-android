/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * {@link Service} subclass whose duty is to call for the general configuration data.
 * @author mbaldrighi on 7/18/2018.
 */
public class GetConfigurationDataService extends Service implements OnServerMessageReceivedListener {

	public static final String LOG_TAG = GetConfigurationDataService.class.getCanonicalName();

	private static final int NUM_OPS = 2;
	private int opsCount = 0;

	private ServerMessageReceiver receiver;


	public static void startService(Context context) {
		try {
			context.startService(new Intent(context, GetConfigurationDataService.class));
		} catch (IllegalStateException e) {
			LogUtils.e(LOG_TAG, "Cannot start background service: " + e.getMessage(), e);
		}
	}


	@Override
	public void onCreate() {
		super.onCreate();

		receiver = new ServerMessageReceiver();
	}

	@Override
	public void onDestroy() {
		try {
//			unregisterReceiver(receiver);
			LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		} catch (IllegalArgumentException e) {
			LogUtils.d(LOG_TAG, e.getMessage());
		}

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// initializes Receiver if it's null
		if (receiver == null)
			receiver = new ServerMessageReceiver();
		receiver.setListener(this);
//		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));

		resultCount = 0;
		Realm realm = null;
		try {
			realm = RealmUtils.getCheckedRealm();
			callData(Operations.CONFIGURATION, realm);
			callData(Operations.HEARTS, realm);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			RealmUtils.closeRealm(realm);
		}

		return Service.START_STICKY;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	private enum Operations { CONFIGURATION, HEARTS }
	private int resultCount = 0;
	private void callData(Operations op, Realm realm) {
		Object[] results = null;
		HLUser user = new HLUser().readUser(realm);
		if (user != null && user.isValid()) {
			String id = user.getId();
			if (Utils.isStringValid(id)) {
				try {
					if (op == Operations.CONFIGURATION)
						results = HLServerCalls.getConfigurationData(id);
					else
						results = HLServerCalls.getUpdatedAvailableHearts(id);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			try {
//				unregisterReceiver(receiver);
				LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
			} catch (IllegalArgumentException e) {
				LogUtils.d(LOG_TAG, e.getMessage());
			}

			LogUtils.e(LOG_TAG, "Wrong user: USER FAKE. Stopping service");
			stopSelf();
		}

		if (results == null || !((boolean) results[0])) {
			if (++resultCount == NUM_OPS) {
				try {
//				unregisterReceiver(receiver);
					LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
				} catch (IllegalArgumentException e) {
					LogUtils.d(LOG_TAG, e.getMessage());
				}
			}
		}
	}


	//region == Receiver Callback ==

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {

		Realm realm = null;
		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_CONFIGURATION_DATA:
				++opsCount;

				try {
					realm = RealmUtils.getCheckedRealm();
					final HLUser user = new HLUser().readUser(realm);
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							user.saveConfigurationData(responseObject.optJSONObject(0));
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					RealmUtils.closeRealm(realm);
				}
				break;

			case Constants.SERVER_OP_GET_USER_HEARTS:
				++opsCount;

				try {
					realm = RealmUtils.getCheckedRealm();
					final HLUser user = new HLUser().readUser(realm);
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							user.setUpdatedHearts(responseObject.optJSONObject(0));
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					RealmUtils.closeRealm(realm);
				}
				break;
		}

		if (opsCount == NUM_OPS) {
			stopSelf();
			try {
//					unregisterReceiver(receiver);
				LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
			} catch (IllegalArgumentException e) {
				LogUtils.d(LOG_TAG, e.getMessage());
			}
		}

	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {

		switch (operationId) {
			case Constants.SERVER_OP_GET_IDENTITIES_V2:
				try {
//					unregisterReceiver(receiver);
					LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
				} catch (IllegalArgumentException e) {
					LogUtils.d(LOG_TAG, e.getMessage());
				}

				LogUtils.e(LOG_TAG, "SERVER ERROR with error: " + errorCode);
				stopSelf();
				break;
		}

	}

	//endregion

}
