/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.vincent.videocompressor;

import android.util.Log;

import androidx.annotation.NonNull;

/**
 * @author mbaldrighi on 9/14/2017.
 */
public class LogUtils {

	private static final boolean LOGGABLE = BuildConfig.DEBUG;

	/*
	 * DEBUG
	 */
	public static void d(@NonNull String tag, @NonNull Object obj) {
		d(tag, obj.toString());
	}

	public static void d(String tag, String obj) {
		if (LOGGABLE)
			Log.d(tag, obj);
	}

	/*
	 * INFO
	 */
	public static void i(@NonNull String tag, @NonNull Object obj) {
		i(tag, obj.toString());
	}

	public static void i(String tag, @NonNull String obj) {
		if (LOGGABLE)
			Log.i(tag, obj);
	}

	/*
	 * VERBOSE
	 */
	public static void v(@NonNull String tag, @NonNull Object obj) {
		v(tag, obj.toString());
	}

	public static void v(String tag, String obj) {
		if (LOGGABLE)
			Log.v(tag, obj);
	}

	/*
	 * ERROR
	 */
	public static void e(@NonNull String tag, @NonNull Object obj, Throwable th) {
		e(tag, obj.toString(), th);
	}

	public static void e(String tag, @NonNull Object obj) {
		e(tag, obj.toString(), null);
	}

	public static void e(String tag, String obj, Throwable th) {
		if (LOGGABLE) {
			Log.e(tag, obj, th);

			if (th != null) th.printStackTrace();
		}
	}

}
