/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

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

/**
 * {@link Service} subclass whose duty is to call for the available identities for the acting user.
 * @author mbaldrighi on 1/26/2018.
 */
public class GetIdentitiesService extends Service implements OnServerMessageReceivedListener {

	public static final String LOG_TAG = GetIdentitiesService.class.getCanonicalName();

	private ServerMessageReceiver receiver;


	public static void startService(Context context) {
		try {
			context.startService(new Intent(context, GetIdentitiesService.class));
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

		Realm realm = null;
		try {
			realm = RealmUtils.getCheckedRealm();
			callIdentities(realm);
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


	private void callIdentities(Realm realm) {
		Object[] results = null;
		try {
			results = HLServerCalls.getIdentities(new HLUser().readUser(realm).getUserId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (results == null || !((boolean) results[0])) {
			try {
//				unregisterReceiver(receiver);
				LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
			} catch (IllegalArgumentException e) {
				LogUtils.d(LOG_TAG, e.getMessage());
			}
		}
	}


	//region == Receiver Callback ==

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {

		switch (operationId) {
			case Constants.SERVER_OP_GET_IDENTITIES_V2:
				try {
//					unregisterReceiver(receiver);
					LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
				} catch (IllegalArgumentException e) {
					LogUtils.d(LOG_TAG, e.getMessage());
				}

				Realm realm = null;
				try {
					realm = RealmUtils.getCheckedRealm();
					final HLUser user = new HLUser().readUser(realm);
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							user.setIdentities(responseObject);
//							realm.copyToRealmOrUpdate(user);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					RealmUtils.closeRealm(realm);
				}

				stopSelf();
				break;
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
