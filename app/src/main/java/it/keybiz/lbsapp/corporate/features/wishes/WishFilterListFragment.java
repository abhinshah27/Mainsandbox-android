/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class WishFilterListFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishFilterListFragment.class.getCanonicalName();

	private View mainView;

	private ListView baseListView;
	private List<WishListElement> baseItems = new ArrayList<>();
	private Map<WishListElement, List<String>> filtersMap = new HashMap<>();
	private FilterBaseAdapter baseAdapter;

	private View noResult;

	private WishListElement selectedElement;


	public WishFilterListFragment() {
		// Required empty public constructor
	}

	public static WishFilterListFragment newInstance() {
		Bundle args = new Bundle();
		WishFilterListFragment fragment = new WishFilterListFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		mainView = inflater.inflate(R.layout.fragment_wishes_base_list, container, false);

		configureLayout(mainView);

		return mainView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() != null)
			baseAdapter = new FilterBaseAdapter(getActivity(), baseItems, filtersMap);

		wishesActivityListener.setOnNextClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_FILTER);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(mainView);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (getActivity() instanceof WishesActivity)
			((WishesActivity) getActivity()).setBackListener(null);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {

		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		}
	}

	@Override
	public void onNextClick() {
		wishesActivityListener.setSelectedWishListElement(selectedElement);
		if (wishesActivityListener != null && wishesActivityListener.getSelectedWishListElement() != null) {
			if (filtersMap.containsKey(wishesActivityListener.getSelectedWishListElement())) {
				ArrayList<String> filters = new ArrayList<>(filtersMap.get(wishesActivityListener.getSelectedWishListElement()));

				if (!filters.isEmpty()) {
					Bundle bundle = new Bundle();
					bundle.putStringArrayList("containsWords", filters);
					wishesActivityListener.setDataBundle(bundle);
				}

				wishesActivityListener.resumeNextNavigation();
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (getActivity() == null || !(getActivity() instanceof HLActivity)) return;


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

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				parseResponse(responseObject);

//				if (wishesActivityListener != null)
//					wishesActivityListener.restoreReceiver();
				break;
		}
	}

	private void parseResponse(JSONArray responseObject) {
		if (responseObject == null || responseObject.length() == 0)
			return;

		JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
		if (json != null && json.length() > 0) {

			baseListView.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);

			if (baseItems == null)
				baseItems = new ArrayList<>();
			else
				baseItems.clear();

			if (filtersMap == null)
				filtersMap = new HashMap<>();
			else
				filtersMap.clear();

			for (int i = 0; i < json.length(); i++) {
				WishListElement wli = new WishListElement().deserializeToClass(json.optJSONObject(i));
				baseItems.add(wli);

				filtersMap.put(wli, new ArrayList<String>());
			}

			baseAdapter.notifyDataSetChanged();
		}
		else {
			baseListView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
		}

		wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
		wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));

	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				activityListener.showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		activityListener.closeProgress();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	protected void configureLayout(@NonNull View view) {
		baseListView = view.findViewById(R.id.base_list);
		baseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectedElement = baseItems.get(position);
				selectedElement.setSelected(!selectedElement.isSelected());
				baseAdapter.notifyDataSetChanged();

				wishesActivityListener.enableDisableNextButton(selectedElement.isSelected());
			}
		});

		noResult = view.findViewById(R.id.no_result);

	}

	@Override
	protected void setLayout() {
		baseListView.setAdapter(baseAdapter);
	}

}