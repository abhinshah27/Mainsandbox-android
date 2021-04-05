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
import org.json.JSONObject;

import java.util.Iterator;

import io.realm.Realm;
import io.realm.RealmList;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;

/**
 * {@link Service} subclass whose duty is to call for the various properties regarding the acting user.
 * @author mbaldrighi on 6/26/2018.
 */
public class FetchingOperationsService extends Service implements OnServerMessageReceivedListener {

	public static final String LOG_TAG = FetchingOperationsService.class.getCanonicalName();

	private static final int NUM_OPS = 2;       // INTEREST postponed
	private int opsCount = 0;

	private ServerMessageReceiver receiver;


	public static void startService(Context context) {
		try {
			context.startService(new Intent(context, FetchingOperationsService.class));
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
			callServer(Operations.CIRCLES, realm);
			callServer(Operations.IDENTITIES, realm);
//			callServer(Operations.INTERESTS, realm);
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


	private enum Operations { IDENTITIES, CIRCLES, INTERESTS }
	private int resultCount = 0;
	private void callServer(Operations op, Realm realm) {
		Object[] results = null;
		String id = new HLUser().readUser(realm).getUserId();
		try {
			if (op == Operations.IDENTITIES)
				results = HLServerCalls.getIdentities(id);
			else if (op == Operations.CIRCLES)
				results = HLServerCalls.getCirclesForProfile(id, -1);
			else if (op == Operations.INTERESTS)
				results = HLServerCalls.getMyInterests(id);
		} catch (JSONException e) {
			e.printStackTrace();
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
			case Constants.SERVER_OP_GET_IDENTITIES_V2:
				++opsCount;

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
				break;

			case Constants.SERVER_OP_GET_MY_INTERESTS:
				++opsCount;

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
				break;

			case Constants.SERVER_OP_GET_CIRCLES_PROFILE:
				++opsCount;

				try {
					realm = RealmUtils.getCheckedRealm();
					final HLUser user = new HLUser().readUser(realm);
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {

							HLCircle circle;
							JSONObject circles = responseObject.optJSONObject(0);
							if (circles != null) {
								if (user.getCircleObjects() == null)
									user.setCircleObjects(new RealmList<HLCircle>());
								else
									user.getCircleObjects().clear();

								Iterator<String> iter = circles.keys();
								while (iter.hasNext()) {
									String key = iter.next();
									circle = new HLCircle().deserializeToClass(circles.optJSONObject(key));
//									circle.setNameToDisplay(key);
									user.getCircleObjects().add(circle);
								}

								user.updateFilters();
							}

							LocalBroadcastManager.getInstance(FetchingOperationsService.this)
									.sendBroadcast(new Intent(Constants.BROADCAST_CIRCLES_RETRIEVED));
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
			case Constants.SERVER_OP_GET_CIRCLES_PROFILE:
			case Constants.SERVER_OP_GET_MY_INTERESTS:
				if (++opsCount == NUM_OPS) {
					try {
//					unregisterReceiver(receiver);
						LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
					} catch (IllegalArgumentException e) {
						LogUtils.d(LOG_TAG, e.getMessage());
					}

					LogUtils.e(LOG_TAG, "SERVER ERROR with error: " + errorCode);
					stopSelf();
				}
				break;
		}

	}

	//endregion

}
