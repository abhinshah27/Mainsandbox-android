/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import java.io.File;

import it.keybiz.lbsapp.corporate.utilities.LogUtils;

/**
 * @author mbaldrighi on 5/14/2018.
 */
public class DeleteTempFileService extends IntentService {

	public static final String LOG_TAG = DeleteTempFileService.class.getCanonicalName();


	public DeleteTempFileService() {
		super(LOG_TAG);
	}


	public static void startService(Context context) {
		try {
			context.startService(new Intent(context, DeleteTempFileService.class));
		} catch (IllegalStateException e) {
			LogUtils.e(LOG_TAG, "Cannot start background service: " + e.getMessage(), e);
		}
	}



	@Override
	protected void onHandleIntent(@Nullable Intent intent) {
		File tempDir = getExternalFilesDir("tmp");
		if (tempDir != null && tempDir.exists() && tempDir.isDirectory()) {
			for (File f : tempDir.listFiles()) {
				if (f != null) {
					if (f.delete())
						LogUtils.d(LOG_TAG, "Post media file DELETED correctly");
					else
						LogUtils.e(LOG_TAG, "COULDN'T DELETE post media file " + f.getAbsolutePath());
				}
			}
		}
	}

}
