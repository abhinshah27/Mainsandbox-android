package it.keybiz.lbsapp.corporate.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.AnimRes;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * @author mbaldrighi on 8/30/2017.
 */
public class FragmentsUtils {

	public static void addFragmentNull(FragmentTransaction fragmentTransaction, @IdRes int containerResId,
	                                   Fragment fragment, String logTag) {
		addFragmentNull(fragmentTransaction, containerResId, fragment, logTag, null, 0);
	}

	public static  void addFragmentNull(FragmentTransaction fragmentTransaction, @IdRes int containerResId,
	                                    Fragment fragment, String logTag, Fragment target, int requestCode) {
		addFragmentNull(fragmentTransaction, containerResId, fragment, logTag, target, requestCode, null);
	}

	public static  void addFragmentNull(FragmentTransaction fragmentTransaction, @IdRes int containerResId,
	                                    Fragment fragment, String logTag, Fragment target, int requestCode,
	                                    @Nullable String tagName) {
		if(target != null)
			fragment.setTargetFragment(target, requestCode);
		fragment.setRetainInstance(true);
		fragmentTransaction.replace(containerResId, fragment, logTag);
		if (Utils.isStringValid(tagName))
			fragmentTransaction.addToBackStack(tagName);
		else
			fragmentTransaction.addToBackStack(null);
	}


	public static void addFragmentNotNull(FragmentTransaction fragmentTransaction, Fragment fragment) {
		addFragmentNotNull(fragmentTransaction, fragment, null, 0);
	}

	public static void addFragmentNotNull(FragmentTransaction fragmentTransaction, Fragment fragment, Fragment target, int requestCode) {
		addFragmentNotNull(fragmentTransaction, fragment, target, requestCode, null);
	}

	public static void addFragmentNotNull(FragmentTransaction fragmentTransaction, Fragment fragment,
	                                      Fragment target, int requestCode, @Nullable String tagName) {
		if(target != null)
			fragment.setTargetFragment(target, requestCode);
		if (Utils.isStringValid(tagName))
			fragmentTransaction.addToBackStack(tagName);
		else
			fragmentTransaction.addToBackStack(null);
		fragmentTransaction.detach(fragment);
		fragmentTransaction.attach(fragment);
	}

	public static void openFragment(Context context, Bundle bundle, int fragmentCode, int requestCode, Class mClass) {
		Intent intent = new Intent(context, mClass);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.putExtra(Constants.FRAGMENT_KEY_CODE, fragmentCode);
		if(bundle != null)
			intent.putExtras(bundle);
		if(requestCode > 0)
			((Activity) context).startActivityForResult(intent, requestCode);
		else
			context.startActivity(intent);
	}

	public static void openFragment(Context context, Bundle bundle, int fragmentCode, int requestCode,
	                                Class mClass, @AnimRes int enterTransition, @AnimRes int exitTransition) {
		Intent intent = new Intent(context, mClass);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.putExtra(Constants.FRAGMENT_KEY_CODE, fragmentCode);
		if(bundle != null)
			intent.putExtras(bundle);
		if(requestCode > 0)
			((Activity) context).startActivityForResult(intent, requestCode);
		else
			context.startActivity(intent);

		((Activity) context).overridePendingTransition(enterTransition, exitTransition);
	}
}
