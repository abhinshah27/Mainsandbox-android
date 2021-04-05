/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities;

import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * @author mbaldrighi on 10/5/2017.
 */
public class DialogUtils {

	/**
	 * It opens the provided {@link MaterialDialog}.
	 * @param dialog the provided {@link MaterialDialog}.
	 */
	public static void showDialog(MaterialDialog dialog) {
		if (dialog != null && !dialog.isShowing())
			dialog.show();
	}

	/**
	 * It dismisses the provided {@link MaterialDialog}.
	 * @param dialog the provided {@link MaterialDialog}.
	 */
	public static void closeDialog(MaterialDialog dialog) {
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
	}

	/**
	 * It instantiates a generic dialog consisting of a custom view.
	 * @param context the {@link android.app.Application}'s/{@link android.app.Activity}'s {@link Context}.
	 * @param customLayout the provided {@link  LayoutRes} int pointing to the wanted View.
	 * @return The generic {@link MaterialDialog}.
	 */
	public static MaterialDialog createGenericAlertCustomView(Context context, @LayoutRes int customLayout) {
		if (Utils.isContextValid(context)) {
			return new MaterialDialog.Builder(context)
					.customView(customLayout, false)
					.cancelable(true)
					.autoDismiss(false)
					.build();
		}

		return null;
	}

	/**
	 * It instantiates AND shows a generic dialog consisting of a custom view.
	 * @param context the {@link android.app.Application}'s/{@link android.app.Activity}'s {@link Context}.
	 * @param customLayout the provided {@link  LayoutRes} int pointing to the wanted View.
	 * @return The generic {@link MaterialDialog}.
	 */
	public static MaterialDialog showGenericAlertCustomView(Context context, @LayoutRes int customLayout) {
		if (Utils.isContextValid(context)) {
			return new MaterialDialog.Builder(context)
					.customView(customLayout, false)
					.cancelable(true)
					.autoDismiss(false)
					.show();
		}

		return null;
	}

	public static TextView setPositiveButton(TextView view, @StringRes int textResId, View.OnClickListener listener) {
		if (view != null) {
			view.setText(textResId);
			view.setOnClickListener(listener);

			return view;
		}

		return null;
	}

	public static void openKeyboardForDialog(MaterialDialog dialog) {
		if (dialog != null) {
			Window dialogWindow = dialog.getWindow();
			if (dialogWindow != null) {
				dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
						WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
				dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			}
		}
	}

	public static void closeKeyboardForDialog(MaterialDialog dialog) {
		if (dialog != null) {
			Window dialogWindow = dialog.getWindow();
			if (dialogWindow != null) {
				dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
						WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
				dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
			}
		}
	}

}
