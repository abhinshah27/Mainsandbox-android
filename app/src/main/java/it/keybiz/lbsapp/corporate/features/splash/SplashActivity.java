/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.chat.HandleChatsUpdateService;
import it.keybiz.lbsapp.corporate.features.userGuide.UserGuideActivity;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.services.FetchingOperationsService;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A splash screen introducing Highlanders app.
 */
public class SplashActivity extends HLActivity {

	public static final String LOG_TAG = SplashActivity.class.getCanonicalName();

	private static final long SPLASH_DURATION = 2000;
	private static final long SPLASH_DURATION_SHORT = 750;

	private boolean hasNotification = false/*, hasChatRelatedNotification = false*/;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		setRootContent(R.id.root_content);

		manageIntent();

		new Handler().postDelayed(() -> {
			Intent intent = new Intent();

			if (SharedPrefsUtils.isFirstAccess(SplashActivity.this)) {
				intent.setClass(SplashActivity.this, UserGuideActivity.class);
				intent.putExtra(Constants.EXTRA_PARAM_1, UserGuideActivity.ViewType.FIRST_OPEN);
			}
			else {
				intent.setClass(SplashActivity.this, HomeActivity.class);
				if (hasNotification) {
//					intent.setClass(SplashActivity.this, ProfileActivity.class);
					// extras from 1 to 4 are already in use
//					intent.putExtra(Constants.FRAGMENT_KEY_CODE, Constants.FRAGMENT_NOTIFICATIONS);
//					intent.putExtra(Constants.EXTRA_PARAM_5, true);

					intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_NOTIFICATIONS);
				}
//				else if (hasChatRelatedNotification)
//					intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_NOTIFICATIONS);

				if (LBSLinkApp.hasValidUserSession(realm) && mUser != null && mUser.isValid()) {
					String id = new HLUser().readUser(realm).getId();
					if (Utils.isStringValid(id) && !id.equals(Constants.GUEST_USER_ID))
						LogUtils.d(LOG_TAG, "USER_ID: " + id);

					if (!mUser.isActingAsInterest()) {
						FetchingOperationsService.startService(getApplicationContext());
						HandleChatsUpdateService.startService(getApplicationContext());
					}
//						GetTimelineService.startService(getApplicationContext());
				} else new HLUser().write(realm);
			}

			startActivity(intent);
			finish();
			overridePendingTransition(0, R.anim.alpha_out);
		}, (hasNotification/* || hasChatRelatedNotification*/) ? SPLASH_DURATION_SHORT : SPLASH_DURATION);
	}


	// NO NEED TO OVERRIDE THIS
	@Override
	protected void configureResponseReceiver() {}

	@Override
	protected void manageIntent() {
		Intent in = getIntent();
		if (in != null) {
			if (in.hasExtra(Constants.KEY_NOTIFICATION_RECEIVED)) {
				String code = in.getStringExtra(Constants.KEY_NOTIFICATION_RECEIVED);
				if (Utils.isStringValid(code)) {
					switch (code) {
						case Constants.CODE_NOTIFICATION_GENERIC:
							hasNotification = true;
							break;
						case Constants.CODE_NOTIFICATION_CHAT_UNSENT_MESSAGES:
						case Constants.CODE_NOTIFICATION_CHAT:
//							hasChatRelatedNotification = true;
					}
				}
			}
		}
	}

}

