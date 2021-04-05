package it.keybiz.lbsapp.corporate.connection;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import it.keybiz.lbsapp.corporate.BuildConfig;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnApplicationContextNeeded;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Singleton class handling the web socket connection lifecycle.
 *
 * @author mbaldrighi on 9/1/2017.
 */
public class HLSocketConnection {

	public static final String LOG_TAG = HLSocketConnection.class.getCanonicalName();

	private static final String END_POINT = BuildConfig.USE_PROD_CONNECTION ? Constants.SERVER_END_POINT_PROD : Constants.SERVER_END_POINT_DEV;
	private static final String END_POINT_CHAT = BuildConfig.USE_PROD_CONNECTION ? Constants.SERVER_END_POINT_PROD_CHAT : Constants.SERVER_END_POINT_DEV_CHAT;

	private static HLSocketConnection instance;

	private HLWebSocketClient client, clientChat;
	private ScreenStateReceiver screenStateReceiver;
	private ConnectionChangeReceiver connectionChangeReceiver;

	private final Runnable checkConnectionRunnable = new Runnable() {
		@Override
		public void run() {
			LogUtils.v(LOG_TAG, "Checking CONNECTION STATUS");
			if (!isConnected())
				openConnection(false);

			// INFO: 3/12/19    blocks even connection to chat socket: CHAT disabled
//			if (!isConnectedChat())
//				openConnection(true);
		}
	};

	private OnApplicationContextNeeded application;
	private ScheduledExecutorService connectionScheduler;
	private ScheduledFuture scheduledFuture;

	private HLSocketConnection() {
		screenStateReceiver = new ScreenStateReceiver(this);
		connectionChangeReceiver = new ConnectionChangeReceiver(this);
	}

	public static HLSocketConnection getInstance() {
		if (instance == null)
			instance = new HLSocketConnection();

		LogUtils.d(LOG_TAG, "TEST SOCKET Connection singleton ID: " + instance.toString());

		return instance;
	}

	/**
	 * Retrieves the class singleton instance, registering ActivityLifecycle callbacks.
	 * Valid only in the Application's onCreate() method.
	 */
	public static void init() {
		instance = new HLSocketConnection();
	}

	private void startCheckConnection() {
		if(connectionScheduler == null)
			connectionScheduler = Executors.newSingleThreadScheduledExecutor();
		if(scheduledFuture == null)
			scheduledFuture = connectionScheduler.scheduleAtFixedRate(checkConnectionRunnable,
					500, 5000, TimeUnit.MILLISECONDS);
	}

	private void stopCheckConnection() {
		if(scheduledFuture != null) {
			scheduledFuture.cancel(true);
			scheduledFuture = null;
		}
	}


	public void openConnection(boolean isChat) {
		if (isChat)
			if (clientChat != null) clientChat.close();
		else
			if (client != null) client.close();
		try {
			if (isChat) {
				if (clientChat == null)
					clientChat = new HLWebSocketClient(application, END_POINT_CHAT, true);
			}
			else {
				if (client == null)
					client = new HLWebSocketClient(application, END_POINT, false);
			}

			if (Utils.isDeviceConnected(application.getHLContext())) {
				if (isChat) {
					clientChat.connect();
					LogUtils.d(LOG_TAG, "Device connected for CHAT");
				}
				else {
					client.connect();
					LogUtils.d(LOG_TAG, "Device connected");

					// resets pagination vars -> after reconnection always get posts and interactions from scratch
					LBSLinkApp.resetPaginationIds();
				}
			}
			else {
				LogUtils.e(LOG_TAG, "NO CONNECTION AVAILABLE");
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
		initScreenStateListener();
//		initConnectionChangeListener();
		startCheckConnection();
	}


	public void openTempConnectionForLocation(HLWebSocketAdapter.OnSocketConnectionDetectedListener listener) {
		if (client != null) client.close();
		client = null;
		try {
			client = new HLWebSocketClient(application, END_POINT, listener);

			if (Utils.isDeviceConnected(application.getHLContext())) {
				client.connect();
				LogUtils.d(LOG_TAG, "Device connected");
			} else {
				LogUtils.e(LOG_TAG, "NO CONNECTION AVAILABLE");
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}


	public void closeConnection() {
		if (client != null) {
			client.close();
			client = null;
		}
		if (clientChat != null) {
			clientChat.close();
			clientChat = null;
		}
		releaseScreenStateListener();
//		releaseConnectionChangeListener();
		stopCheckConnection();
	}

	void sendMessage(@NonNull String message) {
		LogUtils.d(LOG_TAG, "Original message: " + message);
		sendMessage(Crypto.encryptData(message));
//		sendMessage(message.getBytes(StandardCharsets.UTF_8));
	}

	private void sendMessage(byte[] message) {
		if (message != null && message.length > 0) {
			if (isConnected()) {
				client.getWebSocket().sendBinary(message);
				LogUtils.d(LOG_TAG, "Encrypted Bytes sent: " + Arrays.toString(message));
				return;
			}

			LogUtils.d(LOG_TAG, "Socket not connected. Bytes: " + Arrays.toString(message) + "\tNOT SENT");
			return;
		}

		LogUtils.d(LOG_TAG, "Original bytes null or empty\tNOT SENT");
	}

	void sendMessageChat(@NonNull String message) {
		LogUtils.d(LOG_TAG, "Original message: " + message);
		sendMessageChat(Crypto.encryptData(message));
//		sendMessageChat(message.getBytes(StandardCharsets.UTF_8));
	}

	private void sendMessageChat(byte[] message) {
		if (message != null && message.length > 0) {
			if (isConnectedChat()) {
				clientChat.getWebSocket().sendBinary(message);
				LogUtils.d(LOG_TAG, "Encrypted Bytes sent: " + Arrays.toString(message));
				return;
			}

			LogUtils.d(LOG_TAG, "Socket not connected. Bytes: " + Arrays.toString(message) + "\tNOT SENT");
			return;
		}

		LogUtils.d(LOG_TAG, "Original bytes null or empty\tNOT SENT");
	}

	/**
	 * Screen state listener for socket life cycle
	 */
	private void initScreenStateListener() {
//		application.getHLContext().registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
//		application.getHLContext().registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		LocalBroadcastManager.getInstance(application.getHLContext()).registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
		LocalBroadcastManager.getInstance(application.getHLContext()).registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
	}

	private void releaseScreenStateListener() {
		try {
//			application.getHLContext().unregisterReceiver(screenStateReceiver);
			LocalBroadcastManager.getInstance(application.getHLContext()).unregisterReceiver(screenStateReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Connection changes listener for socket life cycle
	 */
	private void initConnectionChangeListener() {
//		application.getHLContext().registerReceiver(connectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		LocalBroadcastManager.getInstance(application.getHLContext()).registerReceiver(connectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	private void releaseConnectionChangeListener() {
		try {
//			application.getHLContext().unregisterReceiver(connectionChangeReceiver);
			LocalBroadcastManager.getInstance(application.getHLContext()).unregisterReceiver(connectionChangeReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	public boolean isConnected() {
		return Utils.isDeviceConnected(application.getHLContext()) &&
				client != null && client.hasOpenConnection();
	}

	public boolean isConnectedChat() {
		return Utils.isDeviceConnected(application.getHLContext()) &&
				clientChat != null && clientChat.hasOpenConnection();
	}


	public void setContextListener(OnApplicationContextNeeded application) { this.application = application; }

	public HLWebSocketClient getClient() {
		return client;
	}
	public HLWebSocketClient getClientChat() {
		return clientChat;
	}


	@Override
	public String toString() {
		return String.valueOf(this.hashCode());
	}
}
