/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.connection;

/**
 * Interface used to add some custom code other than a simple "No connection" message.
 *
 * @author mbaldrighi on 11/20/2017.
 */
public interface OnMissingConnectionListener {

	/**
	 * @param operationId the code of the operation related to the performed call.
	 */
	void onMissingConnection(int operationId);
}
