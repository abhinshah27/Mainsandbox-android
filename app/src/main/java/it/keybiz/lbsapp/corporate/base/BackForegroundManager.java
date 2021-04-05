/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.base;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import it.keybiz.lbsapp.corporate.utilities.LogUtils;

/**
 * @author mbaldrighi on 9/1/2017.
 */
public class BackForegroundManager implements Application.ActivityLifecycleCallbacks {

	public static final String LOG_TAG = BackForegroundManager.class.getCanonicalName();
	public static final long BACKGROUND_DELAY = 500;

	private static BackForegroundManager instance;

	public interface BackForegroundListener {
		void onBecameForeground();
		void onBecameBackground();
	}

	private boolean mInBackground = true;
	private final List<BackForegroundListener> listeners = new ArrayList<>();
	private final Handler mBackgroundDelayHandler = new Handler();
	private Runnable mBackgroundTransition;

	/**
	 * Class empty constructor.
	 */
	private BackForegroundManager() {
	}

	/**
	 * Retrieves the class singleton instance.
	 * @return the BackForeground instance.
	 */
	public static BackForegroundManager getInstance() {
		if (instance == null)
			return new BackForegroundManager();

		return instance;
	}

	/**
	 * Retrieves the class singleton instance, registering ActivityLifecycle callbacks.
	 * Valid only in the Application's onCreate() method.
	 * @param application the present Application instance.
	 */
	public static void init(Application application) {
		if (instance == null) {
			instance = new BackForegroundManager();
			application.registerActivityLifecycleCallbacks(instance);
		}
	}

	public void registerListener(BackForegroundListener listener) {
		listeners.add(listener);
	}

	public void unregisterListener(BackForegroundListener listener) {
		listeners.remove(listener);
	}

	public boolean isInBackground() {
		return mInBackground;
	}

	@Override
	public void onActivityResumed(Activity activity) {
		if (mBackgroundTransition != null) {
			mBackgroundDelayHandler.removeCallbacks(mBackgroundTransition);
			mBackgroundTransition = null;
		}

		if (mInBackground) {
			mInBackground = false;
			notifyOnBecameForeground();
			LogUtils.i(LOG_TAG, "Application went to foreground");
		}
	}

	private void notifyOnBecameForeground() {
		for (BackForegroundListener listener : listeners) {
			try {
				listener.onBecameForeground();
			} catch (Exception e) {
				LogUtils.e(LOG_TAG, "Listener threw exception!" + e, e);
			}
		}
	}

	@Override
	public void onActivityPaused(Activity activity) {
		if (!mInBackground && mBackgroundTransition == null) {
			mBackgroundTransition = new Runnable() {
				@Override
				public void run() {
					mInBackground = true;
					mBackgroundTransition = null;
					notifyOnBecameBackground();
					LogUtils.i(LOG_TAG, "Application went to background");
				}
			};
			mBackgroundDelayHandler.postDelayed(mBackgroundTransition, BACKGROUND_DELAY);
		}
	}

	private void notifyOnBecameBackground() {
		for (BackForegroundListener listener : listeners) {
			try {
				listener.onBecameBackground();
			} catch (Exception e) {
				LogUtils.e(LOG_TAG, "Listener threw exception!" + e);
			}
		}
	}


	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

	@Override
	public void onActivityStarted(Activity activity) {}

	@Override
	public void onActivityStopped(Activity activity) {}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle oustState) {}

	@Override
	public void onActivityDestroyed(Activity activity) {}


//	public class HLFragmentLifecycleListener extends android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks {
//
//
//
//		@Override
//		public void onFragmentResumed(android.support.v4.app.FragmentManager fm, Fragment f) {
//			super.onFragmentResumed(fm, f);
//		}
//
//		@Override
//		public void onFragmentPaused(android.support.v4.app.FragmentManager fm, Fragment f) {
//			super.onFragmentPaused(fm, f);
//		}
//	}
}
