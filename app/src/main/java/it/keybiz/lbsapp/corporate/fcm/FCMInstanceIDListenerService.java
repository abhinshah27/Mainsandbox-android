/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.fcm;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * The functions of this class have been transferred to {@link com.google.firebase.messaging.FirebaseMessagingService#onNewToken(String)}
 * method.
 * @author mbaldrighi on 11/28/2017.
 */
@Deprecated
public class FCMInstanceIDListenerService extends FirebaseInstanceIdService {

	private static final String LOG_TAG = FCMInstanceIDListenerService.class.getCanonicalName();

	/**
	 * Called if InstanceID token is updated. This may occur if the security of
	 * the previous token had been compromised. Note that this is also called
	 * when the InstanceID token is initially generated, so this is where
	 * you retrieve the token.
	 */
	@Override
	public void onTokenRefresh() {
		// Get updated InstanceID token.
		String refreshedToken = FirebaseInstanceId.getInstance().getToken();
		LogUtils.d(LOG_TAG, "Refreshed FCM token: " + refreshedToken);

		if (Utils.isStringValid(refreshedToken))
			SendFCMTokenService.startService(this);
	}

}
