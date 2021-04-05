package it.keybiz.lbsapp.corporate.base;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.connection.HLSocketConnection;
import it.keybiz.lbsapp.corporate.connection.HLWebSocketAdapter;
import it.keybiz.lbsapp.corporate.connection.HLWebSocketClient;
import it.keybiz.lbsapp.corporate.connection.OnConnectionChangedListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.profile.LocationUpdateService;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.CallsActivityNoService;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 9/14/2017.
 */
public abstract class HLActivity extends AppCompatActivity implements OnServerMessageReceivedListener,
		OnConnectionChangedListener, Handler.Callback {

	public static final String LOG_TAG = HLActivity.class.getCanonicalName();

	protected View rootContent;

	/* Handles the generic and general progress indicator for the activity */
	private View genericProgressIndicator;
	private TextView genericProgressMessage;
	private boolean showProgressAnyway;

	private BroadcastReceiver noConnectionReceiver;

	protected HLUser mUser;

	protected Realm realm;


	protected ServerMessageReceiver serverMessageReceiver = new ServerMessageReceiver();


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

		mDecorView = getWindow().getDecorView();
		mDecorView.setOnSystemUiVisibilityChangeListener(
				flags -> {
					boolean visible = (flags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
//					controlsView.animate()
//							.alpha(visible ? 1 : 0)
//							.translationY(visible ? 0 : controlsView.getHeight());
				}
		);

		noConnectionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				closeProgress();
				LogUtils.d(LOG_TAG, "NO CONNECTION AVAILABLE");
				// TODO: 1/23/2018    CONNECTION POPUP - uncomment if needed
				// showAlert(R.string.error_data_connection);
			}
		};

		realm = RealmUtils.getCheckedRealm();
		mUser = new HLUser().readUser(realm);
		Utils.logUserForCrashlytics(mUser);

//		if (Utils.hasLollipop() && !Utils.hasMarshmallow()) {
//			Window window = getWindow();
//			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//			window.setStatusBarColor(Color.TRANSPARENT);
//		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		LogUtils.d(LOG_TAG, "onStart() HIT");

		if (!Utils.isStringValid(CallsActivityNoService.Companion.getCurrentCallID())) {
			NotificationManager manager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
			if (manager != null)
				manager.cancel(Constants.NOTIFICATION_CALLS_ID);
		}

		// sets itself as listener for the websocket adapter for connection changes.
		HLSocketConnection connection = LBSLinkApp.getSocketConnection();
		if (connection != null) {
			HLWebSocketClient client = connection.getClient();
			HLWebSocketClient clientChat = connection.getClientChat();
			if (client != null) {
				HLWebSocketAdapter adapter = client.getAdapter();
				if (adapter != null)
					adapter.setListener(this);
			}
			if (clientChat != null) {
				HLWebSocketAdapter adapter = clientChat.getAdapter();
				if (adapter != null)
					adapter.setListener(this);
			}
		}

		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);

//		if (!LBSLinkApp.subscribedToSocket)
//			SubscribeToSocketService.startService(getApplicationContext());
//		if (!LBSLinkApp.subscribedToSocketChat)
//			SubscribeToSocketServiceChat.startService(getApplicationContext());
	}

	@Override
	protected void onResume() {
		super.onResume();
		LogUtils.d(LOG_TAG, "onResume() HIT");

		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);

		LocalBroadcastManager.getInstance(this).registerReceiver(noConnectionReceiver, new IntentFilter(Constants.BROADCAST_NO_CONNECTION));
		LocalBroadcastManager.getInstance(this).registerReceiver(serverMessageReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
//		LocalBroadcastManager.getInstance(this).registerReceiver(serverMessageReceiver, new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));

//		registerReceiver(noConnectionReceiver, new IntentFilter(Constants.BROADCAST_NO_CONNECTION));
//		registerReceiver(serverMessageReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
//		registerReceiver(serverMessageReceiver, new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));

//		try {
//			Thread.sleep(380);
//		} catch (InterruptedException e) {
//			LogUtils.e(LOG_TAG, e.getMessage(), e);
//		}
//
//
		// if locationUpdateService isn't running we run it
		if (SharedPrefsUtils.hasLocationBackgroundBeenGranted(this) &&
				Utils.hasApplicationPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
			LocationUpdateService.startService(
					this,
					(this instanceof ProfileActivity) ?
							((ProfileActivity) this).getServiceConnection() : null,
					LocationUpdateService.GroundType.BACKGROUND
			);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

//		unregisterReceiver(noConnectionReceiver);
//		unregisterReceiver(serverMessageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(noConnectionReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(serverMessageReceiver);
	}

	@Override
	protected void onDestroy() {

		try {
//			unregisterReceiver(noConnectionReceiver);
//			unregisterReceiver(serverMessageReceiver);
			LocalBroadcastManager.getInstance(this).unregisterReceiver(noConnectionReceiver);
			LocalBroadcastManager.getInstance(this).unregisterReceiver(serverMessageReceiver);
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		super.onDestroy();

		RealmUtils.closeRealm(realm);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		switch (operationId) {
			case Constants.SERVER_OP_CREATE_POST_V2:
				LogUtils.d(LOG_TAG, "Post creation SUCCESS with object " + responseObject.toString());

				// TODO: 10/15/2017    do something???
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		switch (operationId) {
			case Constants.SERVER_OP_CREATE_POST_V2:
				LogUtils.e(LOG_TAG, "Post creation FAIL with error " + errorCode);
				showAlert(R.string.error_creating_post);
				break;
		}
	}

	@Override
	public void onConnectionChange() {
		closeProgress();

		LogUtils.d(LOG_TAG, "CONNECTION CHANGED - SOCKET CLOSE");
		// TODO: 1/23/2018    CONNECTION POPUP - uncomment if needed
		//		showAlert(R.string.error_connection_changed);
	}


	//region == Class custom methods ==

	protected void configureToolbar(Toolbar toolbar, String title, boolean showBack) {
		if (toolbar != null) {
			setSupportActionBar(toolbar);

			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setDisplayShowHomeEnabled(showBack);

				if (Utils.isStringValid(title)) {
					actionBar.setDisplayShowTitleEnabled(true);
					actionBar.setTitle(title);
				}
				else
					actionBar.setDisplayShowTitleEnabled(false);
			}
		}
	}

	protected void setRootContent(View root) {
		this.rootContent = root;
	}

	protected void setRootContent(@IdRes int resId) {
		setRootContent(findViewById(resId));
	}

	protected void setProgressIndicator(View progressIndicator) {
		this.genericProgressIndicator = progressIndicator;
		this.genericProgressMessage = genericProgressIndicator.findViewById(R.id.progress_message);

		this.genericProgressIndicator.setOnClickListener(v -> { /* do nothing */ });
	}
	protected void setProgressIndicator(@IdRes int resId) {
		setProgressIndicator(findViewById(resId));
	}

	public void setProgressMessage(final @NonNull String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (genericProgressMessage != null) {
					genericProgressMessage.setText(getString(R.string.dots, msg));
					genericProgressMessage.setVisibility(View.VISIBLE);
				}
			}
		});
	}
	public void setProgressMessage(@StringRes int resId) {
		setProgressMessage(getString(resId));
	}

	public void showAlert(String msg, String action, View.OnClickListener listener) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (rootContent != null) {

					// TODO: 9/18/2017   DO WE NEED IT COLORED?
					Snackbar snack = Snackbar.make(rootContent, msg, Snackbar.LENGTH_LONG);
					if (listener != null)
						snack.setAction(action, listener);
					snack.show();

					TextView textView = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
					TextView actionView = snack.getView().findViewById(com.google.android.material.R.id.snackbar_action);

					try {
						actionView.setTextColor(Utils.getColor(getResources(), R.color.luiss_blue_on_blue_active));
						Utils.applyFontToTextView(textView, R.string.fontSemiBold);
						Utils.applyFontToTextView(actionView, R.string.fontBold);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					Toast.makeText(HLActivity.this, msg, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	public void showAlert(@StringRes int msg, @StringRes int action, View.OnClickListener listener) {
		showAlert(getString(msg), getString(action), listener);
	}

	public void showAlert(String msg) {
		showAlert(msg, null, null);
	}

	public void showAlertWithRetry(@StringRes int msg, View.OnClickListener listener) {
		showAlert(getString(msg), getString(R.string.retry), listener);
	}

	public void showAlert(int msgResId) {
		showAlert(getString(msgResId), null, null);
	}

	public void showGenericError() {
		showAlert(R.string.error_unknown);
	}


	public boolean isProgressVisible() {
		return genericProgressIndicator != null && genericProgressIndicator.getVisibility() == View.VISIBLE;
	}

	public void showProgressIndicator(final boolean show) {
		if (genericProgressIndicator != null) {
			genericProgressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
			genericProgressIndicator.animate().setDuration(100).alpha(
					show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					genericProgressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});
		}
	}

	public void handleProgress(final boolean show) {
		if (show) openProgress();
		else closeProgress();
	}

	public void openProgress() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showProgressIndicator(true);
			}
		});
	}

	public void closeProgress() {
		if (!showProgressAnyway) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showProgressIndicator(false);
				}
			});
		}
	}

	//endregion


	//region == Getters and setters ==

	public HLUser getUser() {
		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);
		return mUser;
	}
	public void setUser(HLUser mUser) {
		this.mUser = mUser;
	}

	public boolean isShowProgressAnyway() {
		return showProgressAnyway;
	}
	public void setShowProgressAnyway(boolean showProgressAnyway) {
		this.showProgressAnyway = showProgressAnyway;
	}

	public Realm getRealm() {
		realm = RealmUtils.checkAndFetchRealm(realm);
		return realm;
	}

	//endregion


	//region == Abstract methods ==

	/**
	 * To be always implemented at the latest in {@link Activity#onStart()}.
	 */
	protected abstract void configureResponseReceiver();

	/**
	 * Needed to handle incoming {@link Intent}s  to extract vars and everything that is needed.
	 * To be always implemented in {@link Activity#onCreate(Bundle)}.
	 */
	protected abstract void manageIntent();

	//endregion


	//region == Immersive mode management ==

	private static final int INITIAL_HIDE_DELAY = 300;
	private boolean isImmersive = false;
	protected View mDecorView;
	private final Handler mHideHandler = new Handler(this);

	public void setImmersiveValue(boolean immersive) {
		isImmersive = immersive;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		// When the window loses focus (e.g. the action overflow is shown),
		// cancel any pending hide action. When the window gains focus,
		// hide the system UI.
		if (hasFocus) {
			if (isImmersive) delayedHide(INITIAL_HIDE_DELAY);
		} else {
			mHideHandler.removeMessages(0);
		}
	}
	private void hideSystemUI() {
		mDecorView.setSystemUiVisibility(
//				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//				|
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
		);
	}
	private void showSystemUI() {
		mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}

	private void delayedHide(int delayMillis) {
		mHideHandler.removeMessages(0);
		mHideHandler.sendEmptyMessageDelayed(0, delayMillis);
	}

	@Override
	public boolean handleMessage(Message msg) {
		hideSystemUI();
		return true;
	}

	//endregion

}
