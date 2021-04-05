/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.base;


import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

/**
 * Provides the methods for a basic interaction between a {@link Fragment}
 * and its {@link android.app.Activity}.
 *
 * @author mbaldrighi on 10/9/2017.
 */
public interface BasicInteractionListener {

	void showAlert(String msg);
	void showAlert(int msgResId);
	void showGenericError();

	void openProgress();
	void closeProgress();
	void setProgressMessage(@StringRes int stringResId);
	void setProgressMessage(@NonNull String string);

}