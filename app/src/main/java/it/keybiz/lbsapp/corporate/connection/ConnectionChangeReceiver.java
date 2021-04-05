package it.keybiz.lbsapp.corporate.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import it.keybiz.lbsapp.corporate.utilities.LogUtils;

/**
 * @author mbaldrighi on 9/1/2017.
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {

	public static final String LOG_TAG = ConnectionChangeReceiver.class.getCanonicalName();

	private HLSocketConnection connection;

	public ConnectionChangeReceiver() {
		super();
	}

	public ConnectionChangeReceiver(HLSocketConnection connection) {
		this.connection = connection;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		LogUtils.d(LOG_TAG, "Connectivity change received!");
		if (debugIntent(intent, LOG_TAG)) {
			if (!connection.isConnected())
				connection.openConnection(false);

			// INFO: 3/12/19    blocks even connection to chat socket: CHAT disabled
//			if (!connection.isConnectedChat())
//				connection.openConnection(true);
		}
	}

	private boolean debugIntent(Intent intent, String tag) {
		LogUtils.d(tag, "action: " + intent.getAction());
		LogUtils.d(tag, "component: " + intent.getComponent());
		Bundle extras = intent.getExtras();
		if (extras != null) {
			boolean noConn = extras.getBoolean("noConnectivity", false);
			return !noConn;

//			for (String key : extras.keySet()) {
//				Object extra = extras.get(key);
//				LogUtils.d(tag, "key [" + key + "]: " + extra);
//
//				if (extra != null && extra instanceof NetworkInfo.State) {
//					if (extra == NetworkInfo.State.CONNECTED)
//						return true;
//				}
//			}
		} else {
			LogUtils.d(tag, "no extras");
		}

		return false;
	}
}
