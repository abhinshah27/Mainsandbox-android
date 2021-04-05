/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyEntriesEnum;
import it.keybiz.lbsapp.corporate.models.enums.SecurityEntriesEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;


public class SettingsInnerEntriesFragment extends HLFragment implements View.OnClickListener,
		ListView.OnItemClickListener, OnMissingConnectionListener {

	public static final String LOG_TAG = SettingsInnerEntriesFragment.class.getCanonicalName();

	public enum ViewType { PRIVACY, SECURITY }
	private ViewType mViewType;

	private ListView baseList;
	private ArrayAdapter<CharSequence> baseAdapter;


	public SettingsInnerEntriesFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment ProfileFragment.
	 */
	public static SettingsInnerEntriesFragment newInstance(ViewType viewType) {
		SettingsInnerEntriesFragment fragment = new SettingsInnerEntriesFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, viewType);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		// Inflate the layout for this fragment
		baseList = new ListView(container.getContext());
		configureLayout(baseList);

		return baseList;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (Utils.isContextValid(getActivity())) {
			String[] items = getResources().getStringArray(R.array.settings_privacy);
			if (mViewType == ViewType.SECURITY)
				items = getResources().getStringArray(R.array.settings_security);
			baseAdapter = new SettingsListViewAdapter(getActivity(), R.layout.item_settings_entry, items, mViewType);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_PRIVACY_SECURITY_LIST);

		setLayout();
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

	}

	@Override
	public void onDetach() {
		super.onDetach();
	}


	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button_logout) {
			logOut();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (mViewType == ViewType.PRIVACY) {

			// INFO: 2/13/19    LUISS doesn't want apparently the Privacy Policy viewer
//			if (position == 0) {
//				Utils.fireBrowserIntent(getContext(), Constants.URL_PRIVACY, getString(R.string.settings_privacy_policy));
//			}
//			else {
				// changed ...toEnum(position - 1) from ...toEnum(position) because the first element of the list has become the Privacy Policy viewer
				// BUT the title string referred to at line 154 MUST BE always (position)
				PrivacyEntriesEnum entry = PrivacyEntriesEnum.toEnum(position/* - 1*/);
				if (entry != null) {
					switch (entry) {
						case BLOCKED:
							settingsActivityListener.showPrivacyBlockedUsersFragment();
							break;

						default:
							settingsActivityListener.showPrivacySelectionFragment(this, entry.getValue(), (String) parent.getAdapter().getItem(position));
					}
				}
//			}
		}
		else if (mViewType == ViewType.SECURITY) {
			// INFO: 2/8/19    LEGACY CONTACT LOGIC NO LONGER APPLIES >> position 1 assumed
			SecurityEntriesEnum entry = SecurityEntriesEnum.toEnum(1);
			if (entry != null) {
//				if (entry == SecurityEntriesEnum.LEGACY_CONTACT_TRIGGER)
//					settingsActivityListener.showSecurityLegacyContactTriggerFragment();
//				else
					if (entry == SecurityEntriesEnum.DELETE_ACCOUNT)
					settingsActivityListener.showSecurityDeleteAccountFragment();
			}
		}
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {

	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				mViewType = (ViewType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		if (view instanceof ListView) {
			baseList.setOnItemClickListener(this);
			baseList.setDivider(null);
		}
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(mViewType == ViewType.SECURITY ? R.string.settings_main_security : R.string.settings_main_privacy);

		baseList.setAdapter(baseAdapter);
	}


	private void logOut() {

		if (Utils.isContextValid(getActivity())) {
			final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(getActivity(), R.layout.custom_dialog_logout);
			if (dialog != null) {
				View view = dialog.getCustomView();
				if (view != null) {
					DialogUtils.setPositiveButton(
							view.findViewById(R.id.button_positive),
							R.string.action_logout,
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {

									// TODO: 3/19/2018    implement LOGOUT

//									SharedPrefsUtils.clearPreferences(getActivity());
//
//									realm.close();
//									if (getActivity() instanceof HLActivity && RealmUtils.isValid(((HLActivity) getActivity()).getRealm()))
//										((HLActivity) getActivity()).getRealm().close();
//									Realm.deleteRealm(LBSLinkApp.realmConfig);
//
//									Intent intent = new Intent(getActivity(), LoginActivity.class);
//									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//									getActivity().startActivity(intent);
//
//									getActivity().finish();

//									Toast.makeText(getActivity(), "Simulated LOG OUT", Toast.LENGTH_SHORT).show();

									dialog.dismiss();
								}
							}
					);

					view.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dialog.dismiss();
						}
					});
				}

				dialog.show();
			}

		}
	}

}
