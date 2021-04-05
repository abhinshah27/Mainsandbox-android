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
import java.util.List;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
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
public class CircleViewMoreFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, LoadMoreResponseHandlerTask.OnDataLoadedListener,
		SearchHelper.OnQuerySubmitted, CircleViewMoreAdapter.OnItemClickListener {

	public static final String LOG_TAG = CircleViewMoreFragment.class.getCanonicalName();

	private String query;
	private String circleName;
	private String userId, userName, userAvatar;

	private SwipeRefreshLayout srl;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private EditText searchBox;

	private TextView noResult;

	private RecyclerView viewMoreRecView;
	private List<HLUserGeneric> viewMoreResults = new ArrayList<>();
	private LinearLayoutManager viewMoreLlm;

	private CircleViewMoreAdapter viewMoreAdapter;

	private int pageIdToCall = 0;
	private boolean fromLoadMore;
	private int newItemCount;

	private boolean automaticTransition;

	private SearchHelper mSearchHelper;


	public CircleViewMoreFragment() {
		// Required empty public constructor
	}

	public static CircleViewMoreFragment newInstance(String circleName, String userId,
	                                                 String userName, String userAvatar) {

		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, circleName);
		args.putString(Constants.EXTRA_PARAM_2, userId);
		args.putString(Constants.EXTRA_PARAM_3, userName);
		args.putString(Constants.EXTRA_PARAM_4, userAvatar);

		CircleViewMoreFragment fragment = new CircleViewMoreFragment();
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

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		viewMoreLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		viewMoreAdapter = new CircleViewMoreAdapter(viewMoreResults, this);
		viewMoreAdapter.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_VIEW_MORE_CIRCLE);

		callForCircle();
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
	public void onItemClick(Object object) {
		if (object instanceof HLUserGeneric)
			profileActivityListener.showProfileCardFragment(ProfileHelper.ProfileType.NOT_FRIEND, ((HLUserGeneric) object).getId());
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
				if (fromLoadMore) {
					newItemCount = responseObject.length();
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.INNER_CIRCLE,
							null, null).execute(responseObject);

//					fromLoadMore = false;
				}
				else
					setData(responseObject, false);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
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
				circleName = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				userId = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				userName = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				userAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_4);
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		View toolbar = view.findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
		profilePicture = toolbar.findViewById(R.id.profile_picture);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				pageIdToCall = 0;
				Utils.setRefreshingForSwipeLayout(srl, true);
				callForCircle();
			}
		});

		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
		searchBox.setHint(R.string.profile_search_box_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		searchBox.setText(query);

		noResult = view.findViewById(R.id.no_result);
		viewMoreRecView = view.findViewById(R.id.generic_rv);
		viewMoreRecView.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				fromLoadMore = true;
				callForCircle();
			}
		});
	}

	@Override
	protected void setLayout() {
		if (Utils.isStringValid(userAvatar))
			MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);

		toolbarTitle.setText(circleName);

		viewMoreRecView.setLayoutManager(viewMoreLlm);
		viewMoreRecView.setAdapter(viewMoreAdapter);

		searchBox.setText(query);
	}


	@Override
	public void onQueryReceived(@NonNull String query) {
		this.query = query;

		if (!this.query.isEmpty()) {
			if (automaticTransition) {
				profileActivityListener.showSearchFragment(query, SearchTypeEnum.INNER_CIRCLE, userId,
						userName, userAvatar);

				automaticTransition = false;
			}
			searchBox.setText("");
		}
	}

	private void callForCircle() {
		Object[] result = null;

		try {
			result = HLServerCalls.getCircle(mUser.getId(), circleName, ++pageIdToCall);
	 	} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
	}

	private void setData(JSONArray response, boolean background) {
		if (response == null || response.length() == 0) {
			viewMoreRecView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
			activityListener.showAlert(R.string.error_generic_update);
			return;
		}

		JSONObject result = response.optJSONObject(0);
		if (result != null) {
			JSONArray lists = result.optJSONArray("lists");
			if (lists != null && lists.length() > 0) {
				JSONObject list = lists.optJSONObject(0);
				if (list != null && list.length() > 0) {
					JSONArray users = list.optJSONArray("users");

					boolean isUsersValid = users != null && users.length() > 0;
					newItemCount = isUsersValid ? users.length() : 0;

					if (pageIdToCall == 1) {
						if (!isUsersValid) {
							viewMoreRecView.setVisibility(View.GONE);
							noResult.setVisibility(View.VISIBLE);
							return;
						}

						if (viewMoreResults == null)
							viewMoreResults = new ArrayList<>();
						else
							viewMoreResults.clear();
					}
					else if (!isUsersValid) return;

					viewMoreRecView.setVisibility(View.VISIBLE);
					noResult.setVisibility(View.GONE);

					try {
						for (int i = 0; i < users.length(); i++) {
							JSONObject json = users.getJSONObject(i);
							HLUserGeneric obj = new HLUserGeneric().deserializeToClass(json);
							if (obj !=null)
								viewMoreResults.add(obj);
						}

						if (!background)
							viewMoreAdapter.notifyDataSetChanged();
					}
					catch (JSONException e) {
						LogUtils.e(LOG_TAG, e.getMessage(), e);
					}
				}
			}
		}
	}


	//region == Load More interface ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return activityListener;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return null;
	}

	@Override
	public void setData(Realm realm) {}

	@Override
	public void setData(JSONArray response) {
		setData(response, true);
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
		return newItemCount;
	}

	//endregion
}
