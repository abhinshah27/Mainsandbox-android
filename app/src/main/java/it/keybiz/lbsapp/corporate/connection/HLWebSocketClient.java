package it.keybiz.lbsapp.corporate.connection;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

import it.keybiz.lbsapp.corporate.base.OnApplicationContextNeeded;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;

/**
 * Instance of the web socket client.
 * <p>
 * It handles the instantiation of {@link WebSocket} and {@link HLWebSocketAdapter} objects.
 *
 * @author mbaldrighi on 9/1/2017.
 */
public class HLWebSocketClient {

	public static final String LOG_TAG = HLWebSocketClient.class.getCanonicalName();
	public static final int TIMEOUT = 3000;

	private OnApplicationContextNeeded application;

	private String host;
	private WebSocket webSocket;

	private HLWebSocketAdapter mAdapter;

	private boolean isChat;

	private HLWebSocketAdapter.OnSocketConnectionDetectedListener socketListener = null;


	public HLWebSocketClient(OnApplicationContextNeeded application, String host, boolean isChat) {
		this.application = application;
		this.host = host;
		this.isChat = isChat;

		LogUtils.d(LOG_TAG, "TEST SOCKET Client ID: " + this.toString());
	}

	public HLWebSocketClient(OnApplicationContextNeeded application, String host, HLWebSocketAdapter.OnSocketConnectionDetectedListener listener) {
		this.application = application;
		this.host = host;
		this.isChat = false;
		this.socketListener = listener;

		LogUtils.d(LOG_TAG, "TEST SOCKET Client ID: " + this.toString());
	}


	public void connect() {
		try {
			if (webSocket == null) {
				webSocket = new WebSocketFactory().createSocket(host, TIMEOUT);
					mAdapter = new HLWebSocketAdapter(application, isChat);
				if (socketListener != null)
					mAdapter.setConnectionListener(socketListener);
				webSocket.addListener(mAdapter);
				webSocket.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
				webSocket.setPingInterval(Constants.TIME_UNIT_SECOND * 60);
				webSocket.connectAsynchronously();

				LogUtils.d(LOG_TAG, "TEST SOCKET WebSocket ID: " + webSocket.toString());
			}
			else reconnect();
		}
		catch (IOException e) {
			e.printStackTrace();
			LogUtils.e(LOG_TAG, e.getMessage());
		}
	}

	private void reconnect() throws IOException {
		webSocket = webSocket.recreate().connectAsynchronously();

		LogUtils.d(LOG_TAG, "WebSocket ID: " + webSocket.toString());
	}

	public void close() {
		if (webSocket != null)
			webSocket.disconnect();
	}

	public boolean hasOpenConnection() {
		return webSocket != null && webSocket.isOpen();
	}


	//region == Getters and setters ==

	public WebSocket getWebSocket() {
		return webSocket;
	}

	public HLWebSocketAdapter getAdapter() {
		return mAdapter;
	}

	//endregion

}
