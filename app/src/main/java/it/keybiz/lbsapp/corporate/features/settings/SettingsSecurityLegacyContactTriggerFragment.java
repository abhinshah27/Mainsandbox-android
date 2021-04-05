/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.shawnlin.numberpicker.NumberPicker;

import org.json.JSONArray;
import org.json.JSONException;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.enums.ActionTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/27/2018.
 */
public class SettingsSecurityLegacyContactTriggerFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener {

	public static final String LOG_TAG = SettingsSecurityLegacyContactTriggerFragment.class.getCanonicalName();

	private final int SCREEN_ELEMENTS = 5;
	private int callCount = 0;
	private boolean ignoreCallsCount, goToContactsSelection;

	private final String TYPE_GET = "get", TYPE_SET = "set";
	private String mType;

	private static String[] itemsTitles, itemsDescriptions, itemsInactivityEras;

	private TextView title;
	private ViewGroup itemsContainer;

	private boolean checked2Step, checkedInactivity, checkedPaper;
	private int verificationContacts = 0;

	private int selectedInactivityEra = 0, selectedInactivityValue = 1;
	private String selectedInactivityEraString = "Years";


	public SettingsSecurityLegacyContactTriggerFragment() {
		// Required empty public constructor
	}

	public static SettingsSecurityLegacyContactTriggerFragment newInstance() {
		Bundle args = new Bundle();

		SettingsSecurityLegacyContactTriggerFragment fragment = new SettingsSecurityLegacyContactTriggerFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.layout_simple_title_container, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(this);

		if (Utils.isContextValid(getActivity())) {
			itemsTitles = getResources().getStringArray(R.array.settings_security_legacy_c_trigger_titles);
			itemsDescriptions = getResources().getStringArray(R.array.settings_security_legacy_c_trigger_descr);
			itemsInactivityEras = getResources().getStringArray(R.array.settings_security_lct_inactivity_eras);
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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_PRIVACY_LEGACY_TRIGGER);

		getInfo();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

	}

	@Override
	public void onBackPressed() {
		saveInfo();
	}

	private void saveInfo() {
		mType = TYPE_SET;
		callServer(CallType.SET_2_STEP, ActionTypeEnum.convertBooleanToEnum(checked2Step, ActionTypeEnum.ON));
		callServer(CallType.SET_INACT, ActionTypeEnum.convertBooleanToEnum(checkedInactivity, ActionTypeEnum.ON));
		callServer(CallType.SET_PAPER, ActionTypeEnum.convertBooleanToEnum(checkedPaper, ActionTypeEnum.ON));
		callServer(CallType.SET_INACT_VALUES, null);
	}

	private void getInfo() {
		mType = TYPE_GET;
		callServer(CallType.GET_2_STEP_CONTACTS, null);
		callServer(CallType.GET_2_STEP, null);
		callServer(CallType.GET_INACT, null);
		callServer(CallType.GET_PAPER, null);
		callServer(CallType.GET_INACT_VALUES, null);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (!Utils.isContextValid(getActivity()) || !(getActivity() instanceof HLActivity)) return;

		switch (requestCode) {


		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Utils.isContextValid(getActivity()) && getActivity() instanceof HLActivity) {
			HLActivity activity = ((HLActivity) getActivity());

			switch (requestCode) {

			}
		}
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (responseObject == null) {
			handleErrorResponse(operationId, 0);
			return;
		}

		if (mType.equals(TYPE_GET)) {
			if (operationId == Constants.SERVER_OP_SETTINGS_SECURITY_GET_2_STEP_CONTACTS)
				verificationContacts = responseObject.length();
			else {
				if (responseObject.length() == 0) {
					handleErrorResponse(operationId, 0);
					return;
				}

				if (operationId == Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_INACTIVITY_VALUES) {
					selectedInactivityEraString = responseObject.optJSONObject(0).optString("format", "Years");
					if (Utils.isStringValid(selectedInactivityEraString) &&
							itemsInactivityEras != null && itemsInactivityEras.length > 0) {
						for (int i = 0; i < itemsInactivityEras.length; i++) {
							String era = itemsInactivityEras[i];
							if (era.equals(selectedInactivityEraString))
								selectedInactivityEra = i;
						}
					}

					selectedInactivityValue = responseObject.optJSONObject(0).optInt("qty", 1);
				} else {
					String status = responseObject.optJSONObject(0).optString("status");

					if (operationId == Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_2_STEP)
						checked2Step = ActionTypeEnum.convertEnumToBoolean(ActionTypeEnum.toEnum(status));
					else if (operationId == Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_INACTIVITY)
						checkedInactivity = ActionTypeEnum.convertEnumToBoolean(ActionTypeEnum.toEnum(status));
					else if (operationId == Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_PAPER)
						checkedPaper = ActionTypeEnum.convertEnumToBoolean(ActionTypeEnum.toEnum(status));
				}
			}
		} else if (!mType.equals(TYPE_SET))
			handleErrorResponse(operationId, 0);

		// intercepts call and changes screen
		if (operationId == Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_2_STEP && goToContactsSelection) {
			settingsActivityListener.showSecurityLegacy2StepFragment(false, null);
			goToContactsSelection = false;
			return;
		}

		if (!ignoreCallsCount && ++callCount != (mType.equals(TYPE_SET) ? (SCREEN_ELEMENTS - 1) : SCREEN_ELEMENTS))
			return;

		callCount = 0;

		realm.executeTransaction(new Realm.Transaction() {
			@Override
			public void execute(@NonNull Realm realm) {

				mUser.getSettings().setTwoStepVerificationEnabled(checked2Step);
				mUser.getSettings().setInactivityPeriodEnabled(checkedInactivity);
				mUser.getSettings().setPaperCertificateRequired(checkedPaper);

				mUser.getSettings().setSelectedEra(selectedInactivityEra);
				mUser.getSettings().setSelectedTime(selectedInactivityValue);

				if (mType.equals(TYPE_GET))
					setLayout();
				else if (mType.equals(TYPE_SET)) {
					if (ignoreCallsCount) {
						setLayout();
						ignoreCallsCount = false;
					} else settingsActivityListener.closeScreen();
				}
			}
		});
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_2_STEP:
			case Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_INACTIVITY:
			case Constants.SERVER_OP_SETTINGS_SECURITY_GET_SET_PAPER:
				if (mType.equals(TYPE_GET)) {
					activityListener.showAlert(R.string.error_generic_update);
					return;
				}
				else if (mType.equals(TYPE_SET)) {
					activityListener.showGenericError();
				}
				break;
		}

		if (mType.equals(TYPE_SET) && ++callCount == (SCREEN_ELEMENTS - 1))
			settingsActivityListener.closeScreen();

	}

	@Override
	public void onMissingConnection(int operationId) {
		if (mType.equals(TYPE_SET) && ++callCount == (SCREEN_ELEMENTS - 1))
			settingsActivityListener.closeScreen();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		title = view.findViewById(R.id.title);
		itemsContainer = view.findViewById(R.id.items_container);
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_security);

		title.setText(R.string.settings_security_legacy_trigger);

		checked2Step = mUser.wantsTwoStepVerification();
		checkedPaper = mUser.wantsPaperCertificate();

		checkedInactivity = mUser.wantsInactivityPeriod();
		int[] values = mUser.getInactivityPeriodValues();
		if (values != null && values.length == 2) {
			selectedInactivityEra = values[0];
			selectedInactivityValue = values[1];
		}

		if (Utils.isContextValid(getContext()) &&
				itemsTitles != null && itemsTitles.length > 0 &&
				itemsDescriptions != null && itemsDescriptions.length > 0) {

			itemsContainer.removeAllViews();

			for (int i = 0; i < itemsTitles.length; i++) {
				final int position = i;
				if (position == 1) {
					final View container = LayoutInflater.from(getContext()).inflate(R.layout.item_settings_lct_switch_inactivity, itemsContainer, false);
					if (container != null) {
						((TextView) container.findViewById(R.id.item_title)).setText(itemsTitles[i]);
						((TextView) container.findViewById(R.id.item_description)).setText(itemsDescriptions[i]);

						final SwitchCompat switchView = container.findViewById(R.id.item_switch);
						final View pickersLayout = container.findViewById(R.id.layout_hideable);
						final NumberPicker pickerEra = container.findViewById(R.id.inactivity_era);
						final NumberPicker pickerValue = container.findViewById(R.id.inactivity_value);
						final TextView inactivityPeriod = container.findViewById(R.id.inactivity_period);

						pickerEra.setDisplayedValues(itemsInactivityEras);
						pickerEra.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
							@Override
							public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
								selectedInactivityEra = newVal;
								selectedInactivityEraString = itemsInactivityEras[selectedInactivityEra];

								int[] minMaxValues = getMinMaxValuesForPicker();
								pickerValue.setMinValue(minMaxValues[0]);
								pickerValue.setMaxValue(minMaxValues[1]);
								if (selectedInactivityValue > pickerValue.getMaxValue())
									pickerValue.setValue(selectedInactivityValue = pickerValue.getMinValue());
								else
									pickerValue.setValue(selectedInactivityValue);

								inactivityPeriod.setText(getString(R.string.inactivity_period, getQuantityStringForPicker()));
							}
						});
						pickerEra.setValue(selectedInactivityEra);

						int[] minMaxValues = getMinMaxValuesForPicker();
						pickerValue.setMinValue(minMaxValues[0]);
						pickerValue.setMaxValue(minMaxValues[1]);
						pickerValue.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
							@Override
							public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
								selectedInactivityValue = newVal;

								inactivityPeriod.setText(getString(R.string.inactivity_period, getQuantityStringForPicker()));
							}
						});
						pickerValue.setValue(selectedInactivityValue);

						inactivityPeriod.setText(getString(R.string.inactivity_period, getQuantityStringForPicker()));

//						container.setOnClickListener(new View.OnClickListener() {
//							@Override
//							public void onClick(View v) {
//								if (position == 0) {
//									settingsActivityListener.showSecurityLegacy2StepFragment(false, null);
//								} else if (position == 1) {
//									checkedInactivity = !switchView.isChecked();
//
//									// TODO: 3/27/2018    goTo inactivity selection
//								}
//							}
//						});

						switchView.setChecked(checkedInactivity);
						pickersLayout.setVisibility(switchView.isChecked() ? View.VISIBLE : View.GONE);
						switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								checkedInactivity = isChecked;

								pickersLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
							}
						});

						itemsContainer.addView(container);
					}
				}
				else {
					final View container = LayoutInflater.from(getContext()).inflate(R.layout.item_settings_lct_switch, itemsContainer, false);
					if (container != null) {
						((TextView) container.findViewById(R.id.item_title)).setText(itemsTitles[i]);
						((TextView) container.findViewById(R.id.item_description)).setText(itemsDescriptions[i]);

						final SwitchCompat switchView = container.findViewById(R.id.item_switch);

						View switchContainer = container.findViewById(R.id.switch_container);
						switchContainer.setClickable(verificationContacts == 0);
						switchContainer.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (position == 0) {
									goToContactsSelection = true;
									mType = TYPE_SET;
									callServer(CallType.SET_2_STEP, ActionTypeEnum.convertBooleanToEnum(true, ActionTypeEnum.ON));
								}
							}
						});

						container.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (position == 0)
									settingsActivityListener.showSecurityLegacy2StepFragment(false, null);
							}
						});

						if (position == 0) {
							switchView.setClickable(verificationContacts > 0);
							switchView.setChecked(checked2Step = (checked2Step && (verificationContacts > 0)));
							switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
								@Override
								public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
									checked2Step = isChecked;
									if (checked2Step) {

										// the call has been added to always have consistency between client and server
										// boolean value needed to differentiate cases
										mType = TYPE_SET;
										ignoreCallsCount = true;
										callServer(CallType.SET_2_STEP, ActionTypeEnum.convertBooleanToEnum(true, ActionTypeEnum.ON));
									}
								}
							});
						}
						else if (position == 2) {
							switchView.setChecked(checkedPaper);
							switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
								@Override
								public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
									checkedPaper = isChecked;
								}
							});
						}

						itemsContainer.addView(container);
					}
				}
			}
		}
	}

	@ArrayRes
	private int[] getMinMaxValuesForPicker() {
		switch (selectedInactivityEra) {
			case 1:
				return getResources().getIntArray(R.array.settings_security_lct_inactivity_values_months);
			case 2:
				return getResources().getIntArray(R.array.settings_security_lct_inactivity_values_weeks);
			case 3:
				return getResources().getIntArray(R.array.settings_security_lct_inactivity_values_days);
			case 4:
				return getResources().getIntArray(R.array.settings_security_lct_inactivity_values_hours);

			default:
				return getResources().getIntArray(R.array.settings_security_lct_inactivity_values_years);
		}
	}

	private String getQuantityStringForPicker() {
		switch (selectedInactivityEra) {
			case 1:
				return getResources().getQuantityString(R.plurals.inactivity_months, selectedInactivityValue, selectedInactivityValue);
			case 2:
				return getResources().getQuantityString(R.plurals.inactivity_weeks, selectedInactivityValue, selectedInactivityValue);
			case 3:
				return getResources().getQuantityString(R.plurals.inactivity_days, selectedInactivityValue, selectedInactivityValue);
			case 4:
				return getResources().getQuantityString(R.plurals.inactivity_hours, selectedInactivityValue, selectedInactivityValue);

			default:
				return getResources().getQuantityString(R.plurals.inactivity_years, selectedInactivityValue, selectedInactivityValue);
		}
	}


	public enum CallType { GET_2_STEP_CONTACTS, GET_2_STEP, SET_2_STEP, GET_INACT, SET_INACT,
		GET_PAPER, SET_PAPER, GET_INACT_VALUES, SET_INACT_VALUES }
	private void callServer(CallType type, @Nullable ActionTypeEnum action) {
		Object[] result = null;

		try {
			if (type == CallType.GET_2_STEP_CONTACTS)
				result = HLServerCalls.get2StepVerificationContacts(mUser.getUserId());
			else
				result = HLServerCalls.getSetLegacyContactTrigger(mUser.getUserId(), type, action,
						selectedInactivityEraString, selectedInactivityValue);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}

}
