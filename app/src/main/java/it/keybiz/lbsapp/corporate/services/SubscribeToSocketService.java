/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
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

import java.util.Objects;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListenerWithIdOperation;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;

/**
 * {@link Service} subclass whose duty is to subscribe client to Real-Time communication with socket.
 *
 * @author mbaldrighi on 11/01/2017.
 */
public class SubscribeToSocketService extends Service implements OnServerMessageReceivedListenerWithIdOperation {

    public static final String LOG_TAG = SubscribeToSocketService.class.getCanonicalName();

    private ServerMessageReceiver receiver;

    private String idOperation = null;


    public static void startService(Context context) {
        LogUtils.d(LOG_TAG, "SUBSCRIPTION to socket: startService()");
        try {
            context.startService(new Intent(context, SubscribeToSocketService.class));
        } catch (IllegalStateException e) {
            LogUtils.e(LOG_TAG, "Cannot start background service: " + e.getMessage(), e);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (receiver == null)
            receiver = new ServerMessageReceiver();
        receiver.setListener(this);
//		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (receiver == null)
            receiver = new ServerMessageReceiver();
        receiver.setListener(this);
//		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));

        Realm realm = null;
        try {
            realm = RealmUtils.getCheckedRealm();
            HLUser user = new HLUser().readUser(realm);
            if (user != null && user.isValid() && !user.isActingAsInterest()) {
                String userId = user.getId();
                callSubscription(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
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

    private void callSubscription(@NonNull String id) {
        Object[] results = HLServerCalls.subscribeToSocket(this, id, false);

        if (results.length == 3)
            idOperation = (String) results[2];

        LogUtils.d(LOG_TAG, "SUBSCRIPTION to socket - idOperation: " + idOperation);

        if (!((boolean) results[0])) {
            LBSLinkApp.subscribedToSocket = false;
            stopSelf();
        }
    }

    //region == Receiver Callback ==


    @Override
    public void handleSuccessResponse(String operationUUID, int operationId, JSONArray responseObject) {
        switch (operationId) {
            case Constants.SERVER_OP_SOCKET_SUBSCR:
                if (Objects.equals(idOperation, operationUUID)) {
                    LBSLinkApp.subscribedToSocket = true;

                    LogUtils.d(LOG_TAG, "SUBSCRIPTION to socket SUCCESS");
                    stopSelf();
                }
                break;
        }
    }

    @Override
    public void handleSuccessResponse(int operationId, JSONArray responseObject) {}

    @Override
    public void handleErrorResponse(int operationId, int errorCode) {

        LBSLinkApp.subscribedToSocket = false;

        switch (operationId) {
            case Constants.SERVER_OP_SOCKET_SUBSCR:
                LogUtils.e(LOG_TAG, "SUBSCRIPTION to socket FAILED");
                stopSelf();
                break;
        }
    }

    //endregion

}
