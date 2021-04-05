package it.keybiz.lbsapp.corporate.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import it.keybiz.lbsapp.corporate.utilities.LogUtils;

/**
 * @author mbaldrighi on 9/1/2017.
 */
public class ScreenStateReceiver extends BroadcastReceiver {

	public static final String LOG_TAG = ScreenStateReceiver.class.getCanonicalName();

	private HLSocketConnection connection;

	public ScreenStateReceiver() {
		super();
	}

	public ScreenStateReceiver(HLSocketConnection connection) {
		this.connection = connection;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			LogUtils.i(LOG_TAG, "Screen is ON");
			connection.openConnection(false);

			// INFO: 3/12/19    blocks even connection to chat socket: CHAT disabled
//			connection.openConnection(true);
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			LogUtils.d(LOG_TAG, "Screen is OFF");
			connection.closeConnection();
		}
	}
}
