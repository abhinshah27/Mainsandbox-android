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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.SettingsHelpElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

// INFO: 2/8/19    the entire class now relies only on the "first" tab: SUPPORT
public class SettingsHelpContactFragment extends HLFragment implements View.OnClickListener,
		ListView.OnItemClickListener, OnMissingConnectionListener {

	public static final String LOG_TAG = SettingsHelpContactFragment.class.getCanonicalName();

	private static String[] elementsTitles, elementsDescriptions, elementsIds;
	private static String descrPress, descrJoinUs;

	private enum TabContent { SUPPORT, PRESS, JOIN_US }
	private TabContent mTabType = TabContent.SUPPORT;

	private View tabSupport, tabPress, tabJoinUs;

	/* SUPPORT */
	private ListView baseList;
	private ArrayAdapter<CharSequence> baseAdapter;

	/* PRESS - JOIN US */
	private View layoutText;
	private TextView customText;


	public SettingsHelpContactFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment SettingsHelpContactFragment.
	 */
	public static SettingsHelpContactFragment newInstance() {
		SettingsHelpContactFragment fragment = new SettingsHelpContactFragment();
		Bundle args = new Bundle();
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
		View view = inflater.inflate(R.layout.fragment_settings_help_contact, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (Utils.isContextValid(getActivity())) {
			elementsTitles = getResources().getStringArray(R.array.help_contact_support_titles);
			elementsDescriptions = getResources().getStringArray(R.array.help_contact_support_descriptions);
			elementsIds = getResources().getStringArray(R.array.help_contact_item_ids);
			descrPress = getString(R.string.help_contact_press_text);
			descrJoinUs = getString(R.string.help_contact_joinus_text);

			baseAdapter = new SettingsListViewAdapter(getActivity(), R.layout.item_settings_entry,
					elementsTitles, null);
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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_PRIVACY_CONTACT_US);

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
		switch (v.getId()) {
			case R.id.help_tab_support:
				mTabType = TabContent.SUPPORT;
				setLayout();
				break;
			case R.id.help_tab_press:
				mTabType = TabContent.PRESS;
				setLayout();
				break;
			case R.id.help_tab_joinus:
				mTabType = TabContent.JOIN_US;
				setLayout();
				break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (elementsTitles != null && elementsTitles.length > 0 &&
				elementsDescriptions != null && elementsDescriptions.length > 0 &&
				elementsIds != null && elementsIds.length > 0 &&
				elementsDescriptions.length == elementsTitles.length && elementsTitles.length == elementsIds.length) {

			SettingsHelpElement element = new SettingsHelpElement();
			element.setTitle(elementsTitles[position]);
			element.setText(elementsDescriptions[position]);

			settingsActivityListener.showSettingsYesNoUIFragmentStatic(element, elementsIds[position],
					SettingsHelpYesNoUIFragment.UsageType.STATIC);
		}
		else
			activityListener.showGenericError();
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
	public void onMissingConnection(int operationId) {}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		tabSupport = view.findViewById(R.id.help_tab_support);
		tabPress = view.findViewById(R.id.help_tab_press);
		tabJoinUs = view.findViewById(R.id.help_tab_joinus);

		tabSupport.setOnClickListener(this);
		tabPress.setOnClickListener(this);
		tabJoinUs.setOnClickListener(this);

		baseList = view.findViewById(R.id.base_list);
		baseList.setOnItemClickListener(this);

		layoutText = view.findViewById(R.id.layout_text);
		customText = view.findViewById(R.id.custom_text);
	}

	@Override
	protected void setLayout() {
		tabSupport.setSelected(mTabType == TabContent.SUPPORT);
		tabPress.setSelected(mTabType == TabContent.PRESS);
		tabJoinUs.setSelected(mTabType == TabContent.JOIN_US);

		switch (mTabType) {
			case SUPPORT:
				baseList.setAdapter(baseAdapter);
				break;

			case PRESS:
				customText.setText(Utils.getFormattedHtml(descrPress));
				break;

			case JOIN_US:
				customText.setText(Utils.getFormattedHtml(descrJoinUs));
				break;
		}

		baseList.setVisibility(mTabType == TabContent.SUPPORT ? View.VISIBLE : View.GONE);
		layoutText.setVisibility(mTabType != TabContent.SUPPORT ? View.VISIBLE : View.GONE);
	}

}
