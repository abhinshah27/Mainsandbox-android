package it.keybiz.lbsapp.corporate.connection;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnApplicationContextNeeded;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 9/14/2017.
 */
public class HLRequestTracker {

	public static final String LOG_TAG = HLRequestTracker.class.getCanonicalName();

	private static HLRequestTracker instance;
	private static final Object mutex = new Object();

	private final ConcurrentMap<String, Request> requests;
	private final ConcurrentMap<String, Request> requestsChat;

	public static HLRequestTracker getInstance(OnApplicationContextNeeded application) {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null)
					instance = new HLRequestTracker();
			}
		}
		instance.application = application;

		return instance;
	}

	private HLRequestTracker() {
		requests = new ConcurrentHashMap<>();
		requestsChat = new ConcurrentHashMap<>();
	}

	private OnApplicationContextNeeded application;


    private void storeId(String body, String idOp, HLActivity caller, boolean isChat) {
        if (!Utils.areStringsValid(idOp, body))
            return;

        Map<String, Request> map = isChat ? requestsChat : requests;
        synchronized (map) {
            map.put(idOp, new Request(idOp, body, caller));
        }
    }


	public synchronized boolean removeAfterCheck(String id) {
        if (requests.isEmpty() && requestsChat.isEmpty())
			return false;

		try {
			if (requests.containsKey(id)) {
				requests.remove(id);
				return true;
			}

            if (requestsChat.containsKey(id)) {
                requestsChat.remove(id);
                return true;
            }
		}
		catch (Exception e) {
			e.printStackTrace();
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}

		return false;
	}

    public synchronized void checkPendingAndRetry(boolean isChat) {

        LogUtils.d(LOG_TAG, "Inside checkPending... method");

        Map<String, Request> map = isChat ? requestsChat : requests;
        try {
            if (!map.isEmpty()) {
                for (Map.Entry<String, Request> entry : map.entrySet()) {
                    Request r = entry.getValue();
                    if (r.isValid())
                        LBSLinkApp.getSocketConnection().sendMessage(r.body);
                    else
                        map.remove(r.id);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(LOG_TAG, e.getMessage(), e);
        }
    }


    void onDataReceivedAsync(final byte[] data) {
    	final HandlerThread ht = new HandlerThread("decryptAndProcessData");
    	ht.start();
    	new Handler(ht.getLooper()).post(() -> {
			try {
				onDataReceived(data);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			ht.quitSafely();
		});
	}

	private void onDataReceived(byte[] data) throws JSONException {
		String newData = Crypto.decryptData(data);
		onDataReceived(newData);
	}

	void onDataReceivedAsync(final String data) {
		final HandlerThread ht = new HandlerThread("decryptAndProcessData");
		ht.start();
		new Handler(ht.getLooper()).post(() -> {
			try {
				onDataReceived(data);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			ht.quitSafely();
		});
	}


	private void onDataReceived(String data) throws JSONException {
		if (!Utils.isStringValid(data))
			return;

		LogUtils.d(LOG_TAG, "Binary data converted to: " + data);

		Object[] result = HLServerCalls.parseJsonResponse(data);

		if (result != null && result.length == 5) {
			if (result[0] != null && result[0] instanceof String && Utils.isStringValid((String) result[0])) {
				removeAfterCheck((String) result[0]);
			}

			if (application != null) {
				int action = (int) result[1];
				JSONArray object = (JSONArray) result[2];
				String intentFilter;
				String operationId = null;
				if (result[0] instanceof String)
					operationId = (String) result[0];

				if (action == Constants.SERVER_OP_GET_CIRCLE && Utils.isStringValid(operationId)) {
					intentFilter = Constants.BROADCAST_SERVER_RESPONSE;
				}
				else {
					switch (action) {
						case Constants.SERVER_OP_SOCKET_SUBSCR:
							intentFilter = Constants.BROADCAST_SOCKET_SUBSCRIPTION;
							break;

						case Constants.SERVER_OP_FCM_TOKEN:
							intentFilter = Constants.BROADCAST_FCM_TOKEN;
							break;

						case Constants.SERVER_OP_RT_NEW_POST:
						case Constants.SERVER_OP_RT_UPDATE_POST:
						case Constants.SERVER_OP_RT_DELETE_POST:
						case Constants.SERVER_OP_RT_UPDATE_HEARTS:
						case Constants.SERVER_OP_RT_UPDATE_SHARES:
						case Constants.SERVER_OP_RT_UPDATE_TAGS:
						case Constants.SERVER_OP_RT_UPDATE_COMMENTS:
						case Constants.SERVER_OP_RT_NEW_SHARE:
						case Constants.SERVER_OP_RT_NEW_COMMENT:
						case Constants.SERVER_OP_RT_EDIT_POST:
						case Constants.SERVER_OP_RT_PUSH_DATA:
						case Constants.SERVER_OP_RT_CHAT_DELIVERY:
						case Constants.SERVER_OP_RT_CHAT_UPDATE_STATUS:
						case Constants.SERVER_OP_RT_CHAT_UPDATE_ACTIVITY:
						case Constants.SERVER_OP_RT_CHAT_MESSAGE_READ:
						case Constants.SERVER_OP_RT_CHAT_MESSAGE_DELIVERED:
						case Constants.SERVER_OP_RT_CHAT_DOCUMENT_OPENED:
							intentFilter = Constants.BROADCAST_REALTIME_COMMUNICATION;
							if (!Utils.isStringValid(operationId))
								operationId = UUID.randomUUID().toString();
							break;

						case Constants.SERVER_OP_GET_NOTIFICATIONS:
							intentFilter = Constants.BROADCAST_SERVER_RESPONSE;
							break;

						default:
							intentFilter = Constants.BROADCAST_SERVER_RESPONSE;
					}
				}
				Intent intent = new Intent(intentFilter);
				intent.putExtra(Constants.EXTRA_PARAM_1, action);
				intent.putExtra(Constants.EXTRA_PARAM_2, object.toString());
				if (isError(result)) {
					int errorCode = (int) result[3];
					String errorDescription = (String) result[4];
					intent.putExtra(Constants.EXTRA_PARAM_3, errorCode);
					intent.putExtra(Constants.EXTRA_PARAM_4, errorDescription);
				}

//				if (wantsIdOperation)
				intent.putExtra(Constants.EXTRA_PARAM_5, operationId);

//				application.getHLContext().sendBroadcast(intent);
				LocalBroadcastManager.getInstance(application.getHLContext()).sendBroadcast(intent);
			}
		}
	}

	private UUID parseBodyForUUID(JSONObject body) {
		if (body == null)
			return null;

		JSONObject event = body.optJSONObject("event");
		if (event != null) {
			String uId = event.optString("idOperation");
			return UUID.fromString(uId);
		}

		return null;
	}

	public void handleCallResult(OnMissingConnectionListener listener, HLActivity activity, Object[] results) {
		handleCallResult(listener, activity, results, false, false);
	}

    public void handleCallResult(OnMissingConnectionListener listener, HLActivity activity, Object[] results, boolean isChat, boolean wantsProgress) {
        if (results != null) {
            storeId((String) results[1], (String) results[2], activity, isChat);
            if (!((boolean) results[0]) && Utils.isContextValid(activity)) {
                activity.closeProgress();

                // Attempts to regulate message visualization after reconnecting
                if (!LBSLinkApp.justComeFromBackground)
                    activity.showAlert(R.string.error_data_connection);

                // for custom actions
                if (listener != null) {
                	int action = -1;
                	if (Utils.isStringValid((String) results[1])) {
						try {
							// even if it is not the "response", what we need is actually "event.action" contained in both sent body and response
							Object[] parsed = HLServerCalls.parseJsonResponse((String) results[1]);
							if (parsed != null && parsed.length >= 2 && parsed[1] instanceof Integer) {
								action = (int) parsed[1];
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

					listener.onMissingConnection(action);
				}
            }

            if (wantsProgress) {
            	activity.setProgressMessage("Loading data");
            	activity.openProgress();
			}
        }
    }


	/**
	 * Checks if the result received from server is in an ERROR status.
	 * @param parseResult the {@link Object[]} result of {@link HLServerCalls#parseJsonResponse(String)}.
	 *
	 * @return {@code true} if the operation reported an ERROR, {@code false} otherwise.
	 */
	public static boolean isError(Object[] parseResult) {
		return !(parseResult != null && parseResult.length == 5) ||
				((int) parseResult[3]) != 0;
	}





	//region == Custom inner classes ==

	public class Request {
		final String id;
		final String body;
		final WeakReference<HLActivity> caller;

		public Request(String id, String body, HLActivity caller) {
			this.id = id;
			this.body = body;
			this.caller = new WeakReference<>(caller);
		}

		public boolean isValid() {
			return Utils.isContextValid(caller.get());
		}
	}

	//endregion

}
