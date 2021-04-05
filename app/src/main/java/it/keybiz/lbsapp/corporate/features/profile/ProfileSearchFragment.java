/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.InterestCategory;
import it.keybiz.lbsapp.corporate.models.enums.SearchTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileSearchFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		SearchHelper.OnQuerySubmitted, LoadMoreResponseHandlerTask.OnDataLoadedListener,
		ProfileSearchAdapter.OnItemClickListener {

	public static final String LOG_TAG = ProfileSearchFragment.class.getCanonicalName();

	private String query;
	private SearchTypeEnum type;
	private String userId, userName, userAvatar;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private EditText searchBox;

	private TextView noResult;
	private RecyclerView searchRecView;
	private List<Object> searchResults = new ArrayList<>();
	private LinearLayoutManager searchLlm;

	private Map<String, InterestCategory> categoriesLoaded = new ConcurrentHashMap<>();

	private ProfileSearchAdapter searchAdapter;

	private int pageIdToCall = 0;
	private boolean fromLoadMore;
	private int newItemsCount;

	private SwipeRefreshLayout srl;

	private SearchHelper mSearchHelper;
	private boolean firstSearchDone = false;


	public ProfileSearchFragment() {
		// Required empty public constructor
	}

	public static ProfileSearchFragment newInstance(String query, SearchTypeEnum type, String userId,
	                                                String userName, String userAvatar) {

		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, query);
		args.putSerializable(Constants.EXTRA_PARAM_2, type);
		args.putString(Constants.EXTRA_PARAM_3, userId);
		args.putString(Constants.EXTRA_PARAM_4, userName);
		args.putString(Constants.EXTRA_PARAM_5, userAvatar);

		ProfileSearchFragment fragment = new ProfileSearchFragment();
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

		View view = inflater.inflate(R.layout.fragment_generic_list, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());
		setArguments(null);

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		searchLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		searchAdapter = new ProfileSearchAdapter(searchResults, this);
		searchAdapter.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_SEARCH_PROFILE);

		setLayout();
	}

	@Override
	public void onPause() {
		firstSearchDone = false;
		onSaveInstanceState(new Bundle());
		Utils.closeKeyboard(searchBox);

		super.onPause();
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
	public void onItemClick(Object object) {
		if (type == SearchTypeEnum.INNER_CIRCLE && object instanceof HLUserGeneric) {
			profileActivityListener.showProfileCardFragment(ProfileHelper.ProfileType.NOT_FRIEND, ((HLUserGeneric) object).getId());
		}
		else if (type == SearchTypeEnum.INTERESTS && object instanceof Interest) {

			// FIXME: 2/6/2018   for inconsistencies in the server responses and limitations due the use of Gson library here the ID to be used is Interest#identityId.

			Interest interest = ((Interest) object);
			String id = interest.getId()/*null*/;
//			if (Utils.isStringValid(interest.getId()) && interest.getId().startsWith("int"))
//				id = interest.getId();
//			else if (Utils.isStringValid(interest.getId()) && interest.getId().startsWith("int"))
//				id = interest.getId();

			if (Utils.isStringValid(id))
				profileActivityListener.showProfileCardFragment(ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED, id);
			else
				activityListener.showGenericError();
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		newItemsCount = responseObject.length();

		switch (operationId) {
			case Constants.SERVER_OP_SEARCH:
				if (fromLoadMore) {
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.SEARCH,
							null, null).execute(responseObject);
				}
				else
					setData(responseObject, false);
		}
	}

	/*
	// FIXME: 5/19/2018     FIX AND RETHINK the whole pagination thing

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_SEARCH:

				if (type == SearchTypeEnum.INTERESTS) {
					JSONObject json = responseObject.optJSONObject(0);
					JSONArray data = json.optJSONArray("data");
					if (data != null)
						newItemsCount = data.length() + 1;
				}
				else {
					newItemsCount = responseObject.length();
				}

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
		}
	}
	*/


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
				query = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				type = (SearchTypeEnum) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				userId = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				userName = savedInstanceState.getString(Constants.EXTRA_PARAM_4);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_5))
				userAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_5);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(Constants.EXTRA_PARAM_1, query);
		outState.putSerializable(Constants.EXTRA_PARAM_2, type);
		outState.putString(Constants.EXTRA_PARAM_3, userId);
		outState.putString(Constants.EXTRA_PARAM_4, userName);
		outState.putString(Constants.EXTRA_PARAM_5, userAvatar);
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		View toolbar = view.findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
		profilePicture = toolbar.findViewById(R.id.profile_picture);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);

		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
		searchBox.setHint(R.string.profile_search_box_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		searchBox.setText(query);

		noResult = view.findViewById(R.id.no_result);
		searchRecView = view.findViewById(R.id.generic_rv);
		searchRecView.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				callSearchFromServer(query);
				fromLoadMore = true;
			}
		});

		srl = Utils.getGenericSwipeLayout(view, () -> {
			Utils.setRefreshingForSwipeLayout(srl, true);
			callSearchFromServer(query);
		});
	}

	@Override
	protected void setLayout() {
		if (Utils.isStringValid(userAvatar))
			MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);

		toolbarTitle.setText(type == SearchTypeEnum.INNER_CIRCLE ?
				R.string.title_search_profile : R.string.title_search_interests);

		searchRecView.setLayoutManager(searchLlm);
		searchRecView.setAdapter(searchAdapter);

		if (!firstSearchDone) {
			searchBox.setText(query);
			onQueryReceived(query);
			firstSearchDone = true;
		}
	}


	@Override
	public void onQueryReceived(@NonNull String query) {
		this.query = query;

		pageIdToCall = 0;
		if (categoriesLoaded != null)
			categoriesLoaded.clear();
		callSearchFromServer(query);
	}

	private void callSearchFromServer(String queryString) {
		HLServerCalls.search(((HLActivity) getActivity()), queryString, type,
				++pageIdToCall, this);
	}

	private void setData(JSONArray response, boolean background) {
		if (pageIdToCall == 1 && (response == null || response.length() == 0)) {
			searchRecView.setVisibility(View.GONE);
			noResult.setText(R.string.no_search_result);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		searchRecView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		if (pageIdToCall == 1) {
			if (searchResults == null)
				searchResults = new ArrayList<>();
			else
				searchResults.clear();
		}
		try {
			if (type == SearchTypeEnum.INNER_CIRCLE) {
				for (int i = 0; i < response.length(); i++) {
					JSONObject json = response.getJSONObject(i);
					Object obj = new HLUserGeneric().deserializeToClass(json);
					searchResults.add(obj);
				}
				if (!background)
					searchAdapter.notifyDataSetChanged();
			}
			else handleCategoriesResponse(response, background);
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}


	private void handleCategoriesResponse(JSONArray response, boolean background) throws JSONException {
		InterestCategory category;
		JSONObject categories = response.getJSONObject(0);
		if (categories != null) {
			Iterator<String> iter = categories.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				category = new InterestCategory().deserializeToClass(categories.getJSONObject(key));
				category.setNameToDisplay(key);

				categoriesLoaded.put(category.getName(), category);
			}

			if (searchResults == null)
				searchResults = new ArrayList<>();
			else
				searchResults.clear();

			List<InterestCategory> categories1 = new ArrayList<>(categoriesLoaded.values());
			Collections.sort(categories1, InterestCategory.CategorySortOrderComparator);
			for (InterestCategory ic : categories1) {
				if (ic != null) {
					searchResults.add(ic.getNameToDisplay());
					searchResults.addAll(ic.getInterests());
				}
			}
			if (!background)
				searchAdapter.notifyDataSetChanged();
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
		setData(array, true);
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
