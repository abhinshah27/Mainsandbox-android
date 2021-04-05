/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.IdRes;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.features.login.LoginActivity;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Makes the {@link TextWatcher} that was already used in
 * {@link LoginActivity} public and reusable.
 *
 * @author mbaldrighi on 3/20/2018.
 */
public class FieldValidityWatcher implements TextWatcher {

	private final @IdRes int parentId;
	private final EditText view;

	public FieldValidityWatcher(@IdRes int parentId, EditText view) {
		this.parentId = parentId;
		this.view = view;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void afterTextChanged(Editable s) {
		String string = s.toString();

		if (Utils.isStringValid(string) && view != null) {
			boolean condition = false;
			switch (parentId) {
				// email addresses
				case R.id.email_login:
				case R.id.email_signup:
				case R.id.recovery_email:
				case R.id.settings_email:
				case R.id.reset_pwd_email:
					condition = Utils.isEmailValid(string);
					break;

				// names
				case R.id.first_name:
				case R.id.last_name:
					condition = s.length() > 0;
					break;

				// passwords
				case R.id.password_signup:
				case R.id.password_signup_confirm:
				case R.id.settings_pwd_new:
				case R.id.settings_pwd_new_confirm:
					condition = Utils.isPasswordValid(string);
					break;

				// for these passwords no check! every pwd is valid
				case R.id.password_login:
				case R.id.settings_pwd_current:
					condition = true;
					break;
			}

			if (condition)
				view.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_tick_green_small, 0);
			else
				view.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
		}
	}
}