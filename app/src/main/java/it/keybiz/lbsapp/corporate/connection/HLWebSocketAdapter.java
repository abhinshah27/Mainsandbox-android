package it.keybiz.lbsapp.corporate.connection;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import java.util.List;
import java.util.Map;

import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnApplicationContextNeeded;
import it.keybiz.lbsapp.corporate.fcm.SendFCMTokenService;
import it.keybiz.lbsapp.corporate.features.chat.HandleChatsUpdateService;
import it.keybiz.lbsapp.corporate.services.GetConfigurationDataService;
import it.keybiz.lbsapp.corporate.services.SubscribeToSocketService;
import it.keybiz.lbsapp.corporate.services.SubscribeToSocketServiceChat;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;

/**
 * @author mbaldrighi on 9/1/2017.
 */
public class HLWebSocketAdapter extends WebSocketAdapter {

	private static final String LOG_TAG = HLWebSocketAdapter.class.getCanonicalName();

	private OnApplicationContextNeeded application;

	private OnConnectionChangedListener mListener;

	private boolean isChat;

	private OnSocketConnectionDetectedListener mConnectionListener;
	public interface OnSocketConnectionDetectedListener {
		void onSocketConnected();
	}


	HLWebSocketAdapter(OnApplicationContextNeeded application, boolean isChat) {
		this.application = application;
		this.isChat = isChat;
	}


	@Override
	public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
		super.onStateChanged(websocket, newState);
		LogUtils.d(LOG_TAG, "WS STATE CHANGED: " + newState + " for WebSocket: " + websocket.toString());

		if (newState != WebSocketState.OPEN)
			mListener.onConnectionChange();
	}

	@Override
	public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
		super.onConnected(websocket, headers);
		LogUtils.d(LOG_TAG, "WS CONNECTION SUCCESS for WebSocket: " + websocket.toString());

		if (mConnectionListener != null) {
			mConnectionListener.onSocketConnected();
			return;
		}

		HLRequestTracker.getInstance(application).checkPendingAndRetry(isChat);

		if (isChat) {
			// automatically re-subscribe client to real-time communication
			SubscribeToSocketServiceChat.startService(application.getHLContext());
			HandleChatsUpdateService.startService(application.getHLContext());
		}
		else {
			// automatically re-subscribe client to real-time communication
			SubscribeToSocketService.startService(application.getHLContext());
			// automatically re-sends client's FCM notifications token
			SendFCMTokenService.startService(application.getHLContext(), null);
			// automatically re-fetches user's general configuration data
			GetConfigurationDataService.startService(application.getHLContext());
		}
	}

	@Override
	public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
	                           WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
		super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
		LogUtils.d(LOG_TAG, "WS DISCONNECTION SUCCESS" + " for WebSocket: " + websocket.toString());

		if (closedByServer)
			LBSLinkApp.getSocketConnection().openConnection(isChat);
	}

	@Override
	public void onTextMessage(WebSocket websocket, String text) throws Exception {
		super.onTextMessage(websocket, text);
		LogUtils.d(LOG_TAG, "WS MESSAGE RECEIVED: " + text);

		HLRequestTracker.getInstance(application).onDataReceivedAsync(text);
	}

	@Override
	public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
		super.onBinaryMessage(websocket, binary);
		LogUtils.d(LOG_TAG, "WS ENCRYPTED BYTE[] MESSAGE RECEIVED");

		HLRequestTracker.getInstance(application).onDataReceivedAsync(binary);
	}

	/*
	 * ERRORS
	 */
	@Override
	public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
		super.onError(websocket, cause);
		LogUtils.e(LOG_TAG, "WS GENERIC ERROR: " + cause.getMessage() + " for WebSocket: " + websocket.toString(), cause);
	}

	@Override
	public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
		super.onConnectError(websocket, exception);
		LogUtils.e(LOG_TAG, "WS CONNECTION ERROR: " + exception.getMessage() + " for WebSocket: " + websocket.toString(), exception);
	}

	@Override
	public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
		super.onTextMessageError(websocket, cause, data);
		LogUtils.e(LOG_TAG, "WS TEXT MESSAGE ERROR: " + cause.getMessage(), cause);
	}

	@Override
	public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
		super.onUnexpectedError(websocket, cause);
		LogUtils.e(LOG_TAG, "WS UNEXPECTED ERROR: " + cause.getMessage(), cause);
	}

	@Override
	public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
		super.onSendError(websocket, cause, frame);
		LogUtils.e(LOG_TAG, "WS SEND ERROR: " + cause.getMessage() + " for WebSocket: " + websocket.toString(), cause);
	}

	/*
		* PINGs&PONGs
		*/
	@Override
	public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
		super.onPingFrame(websocket, frame);
		LogUtils.v(LOG_TAG, "WS PING RECEIVED" + " for WebSocket: " + websocket.toString());
		websocket.sendPong();
	}

	@Override
	public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
		super.onPongFrame(websocket, frame);
		LogUtils.v(LOG_TAG, "WS PONG RECEIVED" + " for WebSocket: " + websocket.toString());
	}


	//region == Getters and setters ==

	public void setListener(OnConnectionChangedListener listener) {
		this.mListener = listener;
	}

	public void setConnectionListener(OnSocketConnectionDetectedListener mConnectionListener) {
		this.mConnectionListener = mConnectionListener;
	}

	//endregion

}
