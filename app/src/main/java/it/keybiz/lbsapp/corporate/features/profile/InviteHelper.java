/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BaseHelper;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.ProfileContactToSend;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 12/21/2017.
 */
public class InviteHelper extends BaseHelper implements OnServerMessageReceivedListener, OnMissingConnectionListener {

	public static String LOG_TAG = InviteHelper.class.getCanonicalName();

	private static String MESSAGE;
	private String phoneNumber;

	private WeakReference<Context> context;

	private boolean deviceHasWhatsApp;

	private OnInviteActionListener mListener;

	private ServerMessageReceiver mReceiver;


	InviteHelper(Context context, OnInviteActionListener listener) {
		super(context);

		this.context = new WeakReference<>(context);
		this.mListener = listener;

		MESSAGE = context.getString(R.string.invite_message, mListener.getUserName());

		if (Utils.isContextValid(context)) {
			PackageManager pm = context.getPackageManager();
			try {
				pm.getPackageInfo("com.whatsapp", PackageManager.GET_META_DATA);
				//Check if package exists or not. If not then code
				//in catch block will be called
				deviceHasWhatsApp = true;
			} catch (PackageManager.NameNotFoundException e) {
				LogUtils.d(LOG_TAG, context.getString(R.string.error_invite_no_wa));
				deviceHasWhatsApp = false;
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		if (mReceiver == null)
			mReceiver = new ServerMessageReceiver();
		mReceiver.setListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (context != null && Utils.isContextValid(context.get())) {
			LocalBroadcastManager.getInstance(context.get())
					.registerReceiver(mReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
		}
	}

	@Override
	public void onPause() {

		try {
			if (context != null && Utils.isContextValid(context.get())) {
				LocalBroadcastManager.getInstance(context.get())
						.unregisterReceiver(mReceiver);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		super.onPause();
	}

	@Override
	public void onMissingConnection(int operationId) {}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		if (operationId == Constants.SERVER_OP_SEND_INVITED_CONTACT) {
			LogUtils.d(LOG_TAG, "Contact sending COMPLETE");
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		if (operationId == Constants.SERVER_OP_SEND_INVITED_CONTACT) {
			LogUtils.d(LOG_TAG, "Contact sending FAILED");
		}
	}


	private void callServer(ProfileContactToSend contact) {
		Object[] result = null;

		String userID = null;
		if (context != null && context.get() instanceof HLActivity)
			userID = ((HLActivity) context.get()).getUser().getUserId();
		try {
			if (Utils.isStringValid(userID))
				result = HLServerCalls.sendInvitedContact(userID, contact);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (context != null && context.get() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) ((HLActivity) context.get()).getApplication()))
					.handleCallResult(this, ((HLActivity) context.get()), result);
		}
	}



	public void openSelector(final View view, final ProfileContactToSend contact) {
		boolean hasPhones = contact.hasPhones();
		boolean hasEmails = contact.hasEmails();

		final PopupMenu menu = new PopupMenu(view.getContext(), view);
		if (!hasPhones && !hasEmails && !deviceHasWhatsApp) {
			menu.getMenu().add(Menu.NONE, 1, 1, R.string.no_entries_found);
			menu.getMenu().findItem(1).setEnabled(false);
			menu.show();
		}
		else {
			menu.inflate(R.menu.popup_menu_invite);
			menu.setOnMenuItemClickListener(item -> {
				switch (item.getItemId()) {
					case R.id.invite_email:
					case R.id.invite_sms:
					case R.id.invite_wa:
						openEntrySelection(view, menu, item.getItemId(), contact);
						return true;

					default:
						return false;
				}
			});

			MenuItem itemSMS = menu.getMenu().findItem(R.id.invite_sms),
					itemEmail = menu.getMenu().findItem(R.id.invite_email),
					itemWA = menu.getMenu().findItem(R.id.invite_wa);

			itemSMS.setVisible(hasPhones);
			itemEmail.setVisible(hasEmails);
			itemWA.setVisible(deviceHasWhatsApp);

			menu.show();
		}
	}


	private void openEntrySelection(View view, PopupMenu open, @IdRes final int menuItemId,
									final ProfileContactToSend contact) {

		PopupMenu menu = new PopupMenu(view.getContext(), view);

		List<String> elements = null;
		switch (menuItemId) {
			case R.id.invite_sms:
			case R.id.invite_wa:
				elements = contact.getPhones();
				break;
			case R.id.invite_email:
				elements = contact.getEmails();
				break;
		}

		if (elements != null && !elements.isEmpty()) {
			for (int i = 0; i < elements.size(); i++) {
				final String element = elements.get(i);
				if (Utils.isStringValid(element)) {
					menu.getMenu()
							.add(Menu.NONE, i + 1, i, element)
							.setOnMenuItemClickListener(item -> {
								switch (menuItemId) {
									case R.id.invite_email:
										if (Utils.isStringValid(element))
											mListener.sendEmail(element);
										else
											Toast.makeText(context.get(), R.string.error_invite_no_email, Toast.LENGTH_SHORT).show();
										return true;
									case R.id.invite_sms:
										if (Utils.isStringValid(element)) {
											phoneNumber = element;

											callServer(contact);

											sendSMS();
										}
										else
											Toast.makeText(context.get(), R.string.error_invite_no_phone, Toast.LENGTH_SHORT).show();
										return true;
									case R.id.invite_wa:

										callServer(contact);

										sendWAMessage(element);
										return true;

									default:
										return false;
								}
							});
				}
			}
		}

		open.dismiss();
		menu.show();
	}



	private void sendSMS() {
		sendSMS(phoneNumber);
	}

	private void sendSMS(String number) {
		Uri sms_uri = Uri.parse("smsto:+" + getFormattedPhoneNumber(number));
		Intent intent = new Intent(Intent.ACTION_SENDTO, sms_uri);
		intent.putExtra("sms_body", MESSAGE);
		if (intent.resolveActivity(context.get().getPackageManager()) != null) {
			context.get().startActivity(intent);
		}
	}

	private void sendWAMessage(String number) {
		try {
			Intent waIntent = new Intent(Intent.ACTION_VIEW);
			waIntent.setPackage("com.whatsapp");

			String url = "https://api.whatsapp.com/send?phone=" + getFormattedPhoneNumber(number) +
					"&text=" + URLEncoder.encode(MESSAGE, "UTF-8");
			waIntent.setData(Uri.parse(url));
			context.get().startActivity(waIntent);
		} catch (UnsupportedEncodingException e) {
			Toast.makeText(context.get(), R.string.error_invite_no_wa, Toast.LENGTH_SHORT) .show();
		}
	}

	private String getFormattedPhoneNumber(String phoneNumber) {
		// matches any non-digit character and replaces it with "".
		return phoneNumber.replaceAll("[^\\d]", "").trim();
	}

	public interface OnInviteActionListener {
		void sendEmail(String email);
		String getUserName();
		Fragment getFragment();
	}

}
