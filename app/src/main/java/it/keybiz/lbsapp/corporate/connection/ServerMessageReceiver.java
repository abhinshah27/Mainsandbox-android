/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 10/6/2017.
 */
public class ServerMessageReceiver extends BroadcastReceiver {

	private OnServerMessageReceivedListener mListener;

	private Set<String> operationIds = new HashSet<>();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && mListener != null) {

			boolean condition = true;
			if (mListener instanceof Context)
				condition = Utils.isContextValid(((Context) mListener));
			else if (mListener instanceof Fragment)
				condition = ((Fragment) mListener).isVisible() && Utils.isContextValid(((Fragment) mListener).getContext());

			if (Utils.isContextValid(context) && condition) {
				int action = -1;
				String operationId = null;
				String intentFilter = intent.getAction();
				if (intent.hasExtra(Constants.EXTRA_PARAM_1))
					action = intent.getIntExtra(Constants.EXTRA_PARAM_1, -1);
				if (intent.hasExtra(Constants.EXTRA_PARAM_5))
					operationId = intent.getStringExtra(Constants.EXTRA_PARAM_5);

				if (checkOperationId(operationId, intentFilter)) {
					if (isError(intent)) {
						int statusCode = intent.getIntExtra(Constants.EXTRA_PARAM_3, -1);
						String description = intent.getStringExtra(Constants.EXTRA_PARAM_4);

						if (mListener instanceof OnServerMessageReceivedListenerWithErrorDescription) {
							((OnServerMessageReceivedListenerWithErrorDescription) mListener)
									.handleErrorResponse(action, statusCode, description);
						} else mListener.handleErrorResponse(action, statusCode);
					} else {
						JSONArray objects = null;
						if (intent.hasExtra(Constants.EXTRA_PARAM_2)) {
							String s = intent.getStringExtra(Constants.EXTRA_PARAM_2);
							if (Utils.isStringValid(s)) {
								try {
									objects = new JSONArray(s);
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							if (
									(
											action == Constants.SERVER_OP_GET_NOTIFICATIONS ||
													action == Constants.SERVER_OP_SOCKET_SUBSCR ||
													action == Constants.SERVER_OP_GET_PARSED_WEB_LINK
									) &&
									Utils.isStringValid(operationId) &&
									mListener instanceof OnServerMessageReceivedListenerWithIdOperation
								) {
								((OnServerMessageReceivedListenerWithIdOperation) mListener)
										.handleSuccessResponse(operationId, action, objects);
							} else mListener.handleSuccessResponse(action, objects);
						}
					}
				}
			}
		}
	}


	private boolean isError(Intent intent) {
		return intent.hasExtra(Constants.EXTRA_PARAM_3);
	}

	private boolean checkOperationId(String operation, String intentFilter) {
		if (!Utils.isStringValid(operation) &&
				(Utils.isStringValid(intentFilter) && !intentFilter.equals(Constants.BROADCAST_REALTIME_COMMUNICATION))) {
			return false;
		}

		if (operationIds == null)
			operationIds = new HashSet<>();

		if (!operationIds.contains(operation)) {
			operationIds.add(operation);
			return true;
		}

		return false;
	}


	public void setListener(OnServerMessageReceivedListener listener) {
		this.mListener = listener;
	}

}
