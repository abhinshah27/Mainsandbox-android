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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

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
public class WishBaseListFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnSpecificDateSetListener,
		WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishBaseListFragment.class.getCanonicalName();

	private ListView baseListView;
	private List<WishListElement> baseItems = new ArrayList<>();
	private BaseListAdapter baseAdapter;

	private SwipeRefreshLayout srl;

	private View noResult;

	private int stepToHighlight;
	private String friendId;
	private boolean root, withIcons;

	private boolean dismissCallResult;

	private WishListElement selectedElement;


	public WishBaseListFragment() {
		// Required empty public constructor
	}

	public static WishBaseListFragment newInstance(boolean root, boolean withIcons) {
		Bundle args = new Bundle();
		args.putBoolean(Constants.EXTRA_PARAM_1, root);
		args.putBoolean(Constants.EXTRA_PARAM_2, withIcons);
		WishBaseListFragment fragment = new WishBaseListFragment();
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

		View view = inflater.inflate(R.layout.fragment_wishes_base_list, container, false);
		configureLayout(view);


		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() != null)
			baseAdapter = new BaseListAdapter(getActivity(), baseItems, withIcons);

		if (getActivity() instanceof WishesActivity) {
			((WishesActivity) getActivity()).setSpecificDateSetListener(this);
		}

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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_LIST);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, root);
		wishesActivityListener.handleSteps();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();
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

		outState.putBoolean(Constants.EXTRA_PARAM_1, root);
		outState.putBoolean(Constants.EXTRA_PARAM_2, withIcons);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				root = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_1, false);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				withIcons = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_2, false);
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
		wishesActivityListener.resumeNextNavigation();
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

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				parseResponse(responseObject);

				if (wishesActivityListener.getOnNextClickListener() == null)
					wishesActivityListener.setOnNextClickListener(this);

//				if (wishesActivityListener != null)
//					wishesActivityListener.restoreReceiver();
				break;
		}
	}

	private void parseResponse(JSONArray responseObject) {

		if (dismissCallResult) {
			dismissCallResult = false;
			return;
		}

		if (responseObject == null || responseObject.length() == 0)
			return;

		JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
		if (json != null && json.length() > 0) {
			noResult.setVisibility(View.GONE);
			baseListView.setVisibility(View.VISIBLE);

			if (baseItems == null)
				baseItems = new ArrayList<>();
			else
				baseItems.clear();

			for (int i = 0; i < json.length(); i++) {
				WishListElement wli = new WishListElement().deserializeToClass(json.optJSONObject(i));
				baseItems.add(wli);
			}

			String specificDate = wishesActivityListener.getSpecificDateString();
			if (Utils.isStringValid(specificDate))
				forwardDate(specificDate);
			else
				baseAdapter.notifyDataSetChanged();
		}
		else {
			noResult.setVisibility(View.VISIBLE);
			baseListView.setVisibility(View.GONE);

			// needs to re-enable srl if for example user is returning from background
			srl.setEnabled(true);
		}

		wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
		wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				activityListener.showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		activityListener.closeProgress();

		Utils.setRefreshingForSwipeLayout(srl, false);
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

				if (wishesActivityListener.getOnNextClickListener() == null)
					wishesActivityListener.setOnNextClickListener(WishBaseListFragment.this);

				// needs to set srl to null because after selection the new nextItem would be called.
				srl.setEnabled(false);

				selectedElement = baseItems.get(position);
				selectedElement.setSelected(!selectedElement.isSelected());

				for (int i = 0; i < baseItems.size() ; i++) {
					if (i == position) continue;

					WishListElement wli = baseItems.get(i);
					wli.setSelected(false);
				}

				baseAdapter.notifyDataSetChanged();

				wishesActivityListener.enableDisableNextButton(selectedElement.isSelected());
			}
		});

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);

				wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
			}
		});

		noResult = view.findViewById(R.id.no_result);
	}

	@Override
	protected void setLayout() {
		baseListView.setAdapter(baseAdapter);
	}


	@Override
	public void forwardDate(@NonNull String date) {
		baseAdapter.setDate(date);
		baseAdapter.notifyDataSetChanged();
	}

	@Override
	public void dismissCallResult() {
		dismissCallResult = true;
	}

}
