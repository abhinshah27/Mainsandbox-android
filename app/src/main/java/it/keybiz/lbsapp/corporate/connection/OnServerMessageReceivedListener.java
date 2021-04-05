/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.connection;

import org.json.JSONArray;

/**
 * @author mbaldrighi on 10/29/2017.
 */
public interface OnServerMessageReceivedListener {

	void handleSuccessResponse(int operationId, JSONArray responseObject);
	void handleErrorResponse(int operationId, int errorCode);

}
