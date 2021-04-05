/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Predicate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.models.enums.SearchTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class WishesSearchFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		SearchHelper.OnQuerySubmitted, LoadMoreResponseHandlerTask.OnDataLoadedListener,
		WishInnerCircleInterestAdapter.OnElementClickedListener, WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishesSearchFragment.class.getCanonicalName();

	private String query;
	private SearchTypeEnum type;

	private EditText searchBox;

	private SwipeRefreshLayout srl;

	private TextView noResult;
	private RecyclerView searchRecView;
	private List<WishListElement> searchResults = new ArrayList<>();
	private List<WishListElement> searchResultsToShow = new ArrayList<>();
	private LinearLayoutManager searchLlm;

	// TODO: 2/27/2018     SEARCH TEMPORARILY LOCAL
	private Map<String, WishListElement> resultsLoaded = new ConcurrentHashMap<>();

	private WishInnerCircleInterestAdapter searchAdapter;

	private int pageIdToCall = 1;     // pageIdToCall automatically set to 1 instead of 0 because pagination is temporarily disabled
	private boolean fromLoadMore;
	private int newItemsCount;

	private WishListElement selectedElement;

	private SearchHelper mSearchHelper;


	public WishesSearchFragment() {
		// Required empty public constructor
	}

	public static WishesSearchFragment newInstance(SearchTypeEnum type) {

		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, type);

		WishesSearchFragment fragment = new WishesSearchFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.layout_search_plus_rv, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		searchLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		searchAdapter = new WishInnerCircleInterestAdapter(searchResultsToShow, this,
				WishInnerCircleInterestAdapter.ItemType.TRIGGER);
		searchAdapter.setHasStableIds(true);

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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_SEARCH_USERS_INTERESTS);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchBox);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.back_arrow:
				if (getActivity() != null)
					getActivity().onBackPressed();
				break;
		}
	}

	@Override
	public void onNextClick() {
		wishesActivityListener.setSelectedWishListElement(selectedElement);
		wishesActivityListener.resumeNextNavigation();
	}

	@Override
	public void onElementClick(@NonNull WishListElement listElement) {

		// needs to set srl to null because after selection the new nextItem would be called.
		srl.setEnabled(false);

		selectedElement = listElement;

		selectedElement.setSelected(!selectedElement.isSelected());
		Stream.of(searchResultsToShow).forEach(new Consumer<WishListElement>() {
			@Override
			public void accept(WishListElement listElement) {
				if (!selectedElement.equals(listElement)) {
					listElement.setSelected(false);
				}
			}
		});

		searchAdapter.notifyDataSetChanged();

		wishesActivityListener.enableDisableNextButton(selectedElement.isSelected());
		wishesActivityListener.setTriggerFriendId(selectedElement.isSelected() ? selectedElement.getFriendID() : null);
		LogUtils.d(LOG_TAG, "InnerCircle selection with ID: " + selectedElement.getFriendID());
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		if (responseObject == null || responseObject.length() == 0)
			return;

		newItemsCount = responseObject.length();

		switch (operationId) {

			// TODO: 2/27/2018     SEARCH TEMPORARILY LOCAL
			case Constants.SERVER_OP_SEARCH:
				if (fromLoadMore)
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.SEARCH,
							null, null).execute(responseObject);
				else
					try {
						setData(responseObject, false);
					} catch (JSONException e) {
						LogUtils.e(LOG_TAG, e.getMessage(), e);
						break;
					}
				break;

			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
				try {
					setData(json, false);
				} catch (JSONException e) {
					LogUtils.e(LOG_TAG, e.getMessage(), e);
					break;
				}

				wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
				wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_SEARCH:
				activityListener.showAlert(R.string.error_generic_list);
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				type = (SearchTypeEnum) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				query = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(Constants.EXTRA_PARAM_1, type);
		outState.putString(Constants.EXTRA_PARAM_2, query);
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		searchBox.setText(query);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);


				wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
			}
		});

		noResult = view.findViewById(R.id.no_result);
		searchRecView = view.findViewById(R.id.generic_rv);

		// TODO: 3/1/2018    SEARCH TEMPORARILY LOCAL -> NO NEED FOR SCROLL LISTENER
//		searchRecView.addOnScrollListener(new LoadMoreScrollListener() {
//			@Override
//			public void onLoadMore() {
//				callSearchFromServer(query);
//				fromLoadMore = true;
//			}
//		});

	}

	@Override
	protected void setLayout() {
		searchRecView.setLayoutManager(searchLlm);
		searchRecView.setAdapter(searchAdapter);

		searchBox.setText(query);
		searchBox.setHint(type == SearchTypeEnum.INTERESTS ?
				R.string.interest_search_box_hint_3 : R.string.profile_search_box_hint_2);
	}


	@Override
	public void onQueryReceived(final String query) {
		this.query = query;

		if (searchResults != null && !searchResults.isEmpty()) {
			searchResultsToShow.clear();
			searchResultsToShow.addAll(Stream.of(searchResults).filter(new Predicate<WishListElement>() {
				@Override
				public boolean test(WishListElement contact) {
					return contact.getName().toLowerCase().contains(query.toLowerCase());
				}
			}).collect(Collectors.toList()));

			searchAdapter.notifyDataSetChanged();
		}
	}

	private void callSearchFromServer(String queryString) {
		HLServerCalls.search(((HLActivity) getActivity()), queryString, type,
				++pageIdToCall, this);
	}

	private void setData(JSONArray response, boolean background) throws JSONException {
		if (pageIdToCall == 1 && (response == null || response.length() == 0 ||
				(response.getJSONObject(0) != null && response.getJSONObject(0).length() == 0))) {
			searchRecView.setVisibility(View.GONE);
			noResult.setText(Utils.isStringValid(query) ? R.string.no_search_result : R.string.no_result_wish_base_list_2);
			noResult.setVisibility(View.VISIBLE);

			// needs to re-enable srl if for example user is returning from background
			srl.setEnabled(true);

			return;
		}

		searchRecView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		if (pageIdToCall == 1) {
			if (searchResults == null)
				searchResults = new ArrayList<>();
			else
				searchResults.clear();

			if (searchResultsToShow == null)
				searchResultsToShow = new ArrayList<>();
			else
				searchResultsToShow.clear();
		}
		try {
			for (int i = 0; i < response.length(); i++) {
				JSONObject json = response.getJSONObject(i);
				WishListElement wle = new WishListElement().deserializeToClass(json);
				searchResults.add(wle);
				searchResultsToShow.add(wle);
			}

			if (!background)
				searchAdapter.notifyDataSetChanged();
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}


	//region == LOAD MORE INTERFACE METHODS ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return activityListener;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return searchAdapter;
	}

	@Override
	public void setData(Realm realm) {}

	@Override
	public void setData(JSONArray array) {
		try {
			setData(array, true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isFromLoadMore() {
		return fromLoadMore;
	}

	@Override
	public void resetFromLoadMore() {
		fromLoadMore = false;
	}

	@Override
	public int getLastPageId() {
		return pageIdToCall - 1;
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}

	//endregion

}
