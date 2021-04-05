/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.globalSearch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileHelper;
import it.keybiz.lbsapp.corporate.models.GlobalSearchObject;
import it.keybiz.lbsapp.corporate.models.InterestCategory;
import it.keybiz.lbsapp.corporate.models.PostList;
import it.keybiz.lbsapp.corporate.models.UsersBundle;
import it.keybiz.lbsapp.corporate.models.enums.GlobalSearchTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.ComputeAndPopulateHandlerThread;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;

/**
 * @author Massimo on 4/10/2018.
 */
public class GlobalSearchFragment extends HLFragment implements GlobalSearchListsAdapter.OnGlobalSearchActionListener,
		SearchHelper.OnQuerySubmitted, OnServerMessageReceivedListener, OnMissingConnectionListener, Handler.Callback {

	public static final String LOG_TAG = GlobalSearchFragment.class.getCanonicalName();

	private View focusCatcher;
	private EditText searchBox;
	private String query;

	private SwipeRefreshLayout srl;

	private RecyclerView globalSearchView;
	private LinearLayoutManager llm;
	private GlobalSearchListsAdapter mAdapter;
	private List<GlobalSearchObject> globalSearchList = new ArrayList<>();

	private TextView noResult;

	private Integer scrollPosition;
	private SparseArray<WeakReference<HorizontalScrollView>> scrollViews = new SparseArray<>();
	private SparseArray<int[]> scrollViewsPositions = new SparseArray<>();

	private SearchHelper mSearchHelper = null;


	public GlobalSearchFragment() {}

	public static GlobalSearchFragment newInstance(String initialQuery) {
		Bundle args = new Bundle();
		GlobalSearchFragment fragment = new GlobalSearchFragment();
		args.putString(Constants.EXTRA_PARAM_3, initialQuery);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.home_fragment_global_search, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llm = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
		mAdapter = new GlobalSearchListsAdapter(globalSearchList, this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getActivity(), AnalyticsUtils.GLOBAL_SEARCH);

		callGlobalActions(
				(Utils.isStringValid(query) && query.length() >= 3) ?
						HLServerCalls.GlobalSearchAction.SEARCH : HLServerCalls.GlobalSearchAction.MOST_POPULAR,
				query
		);
		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchBox);

		int position = llm.findFirstCompletelyVisibleItemPosition();
		scrollPosition = position == -1 ? llm.findFirstVisibleItemPosition() : position;
		if (scrollViews != null && scrollViews.size() > 0) {
			for (int i = 0; i < scrollViews.size(); i++) {
				int key = scrollViews.keyAt(i);
				WeakReference<HorizontalScrollView> hsv = scrollViews.get(key);
				HorizontalScrollView scrollView = hsv.get();
				if (scrollView != null && scrollView.getTag() instanceof Integer) {
					scrollViewsPositions.put(
							((Integer) scrollView.getTag()),
							new int[] {
									scrollView.getScrollX(),
									scrollView.getScrollY()
							}
					);
				}
			}
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_SEARCH_GLOBAL:
			case Constants.SERVER_OP_SEARCH_GLOBAL_MOST_POP:

				setData(responseObject);
				resetPositions(false);

				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);
		resetPositions(false);
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
		resetPositions(false);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		// RecView scroll position
		if (scrollPosition != null)
			outState.putInt(Constants.EXTRA_PARAM_1, scrollPosition);

		// HorScrollViews management
		if (scrollViewsPositions != null && scrollViewsPositions.size() > 0) {
			outState.putInt(Constants.EXTRA_PARAM_2, scrollViewsPositions.size());

			for (int i = 0; i < scrollViewsPositions.size(); i++) {
				int[] arr = scrollViewsPositions.valueAt(i);
				if (arr != null && arr.length == 2) {
					outState.putIntArray(String.valueOf(i), arr);
				}
			}
		}

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			scrollPosition = savedInstanceState.getInt(Constants.EXTRA_PARAM_1, 0);

			int scrollViewsSize = savedInstanceState.getInt(Constants.EXTRA_PARAM_2, 0);
			if (scrollViewsSize > 0) {
				scrollViewsPositions = new SparseArray<>();
				for (int i = 0; i < scrollViewsSize; i++) {
					if (savedInstanceState.containsKey(String.valueOf(i))) {
						scrollViewsPositions.put(i, savedInstanceState.getIntArray(String.valueOf(i)));
					}
				}
			}

			query = savedInstanceState.getString(Constants.EXTRA_PARAM_3, "");
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
		view.findViewById(R.id.search_container).setPadding(0, 0, 0, 0);

		focusCatcher = view.findViewById(R.id.focus_catcher);
		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
		searchBox.setHint(R.string.global_search_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				resetPositions(true);

				Utils.setRefreshingForSwipeLayout(srl, true);

				callGlobalActions(
						hasValidQuery() ? HLServerCalls.GlobalSearchAction.SEARCH : HLServerCalls.GlobalSearchAction.MOST_POPULAR,
						searchBox.getText().toString()
				);
			}
		});

		globalSearchView = view.findViewById(R.id.generic_rv);
		noResult = view.findViewById(R.id.no_result);

		view.findViewById(R.id.toolbar).findViewById(R.id.back_arrow)
				.setOnClickListener(
						v -> {
							if (Utils.isContextValid(getActivity())) getActivity().onBackPressed();
						}
				);

	}

	@Override
	protected void setLayout() {
		focusCatcher.requestFocus();

		srl.setPadding(0, getResources().getDimensionPixelSize(R.dimen.activity_margin_lg), 0, 0);

		globalSearchView.setLayoutManager(llm);
		globalSearchView.setAdapter(mAdapter);

		noResult.setText(R.string.no_search_result);

		searchBox.setText(query);
	}


	@Override
	public void onQueryReceived(String query) {
		this.query = query;
		callGlobalActions(
				Utils.isStringValid(query) ?
						HLServerCalls.GlobalSearchAction.SEARCH : HLServerCalls.GlobalSearchAction.MOST_POPULAR,
				query
		);
	}

	private boolean hasValidQuery() {
		String text = searchBox.getText().toString();
		return searchBox != null && Utils.isStringValid(text)/* && text.length() >= 3*/;
	}


	private void restorePositions() {
		if (scrollPosition != null) {
			llm.scrollToPosition(scrollPosition);
		}
	}

	private void resetPositions(boolean resetScrollViews) {
		scrollPosition = null;

		if (resetScrollViews) {
			if (scrollViewsPositions == null)
				scrollViewsPositions = new SparseArray<>();
			else
				scrollViewsPositions.clear();
		}
	}

	@Override
	public void saveScrollView(int position, HorizontalScrollView scrollView) {
		if (scrollViews == null)
			scrollViews = new SparseArray<>();

		scrollViews.put(position, new WeakReference<>(scrollView));
	}

	@Override
	public void restoreScrollView(int position) {
		if (scrollViews != null && scrollViews.size() > 0 &&
				scrollViewsPositions != null && scrollViewsPositions.size() > 0) {
			if (scrollViews.size() >= position) {
				final HorizontalScrollView hsv = scrollViews.get(position).get();
				if (hsv != null) {
					final int[] coords = scrollViewsPositions.get(position);
					if (coords != null) {
						new Handler().post(new Runnable() {
							@Override
							public void run() {
								hsv.scrollTo(coords[0], coords[1]);
							}
						});
					}
				}
			}
		}
	}


	private void callGlobalActions(HLServerCalls.GlobalSearchAction action, String query) {
		Object[] result = null;

		try {
			result = HLServerCalls.actionsOnGlobalSearch(mUser.getId(), query, action, null, -1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
	}


	private void setData(JSONArray response) {

		if (response == null || response.length() == 0 || response.optJSONObject(0).length() == 0) {
			globalSearchView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		globalSearchView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		if (globalSearchList == null)
			globalSearchList = new ArrayList<>();
		else
			globalSearchList.clear();

		new PopulateGlobalThread("populateGlobal", this, response).start();
	}


	//region -- Adapter interface --

	@Override
	public void goToTimeline(@NonNull String listName, @Nullable String postId) {

		globalSearchActivityListener.showGlobalTimelineFragment(listName, postId, mUser.getId(),
				mUser.getCompleteName(), mUser.getAvatarURL(), searchBox.getText().toString());
	}

	@Override
	public void goToInterestsUsersList(GlobalSearchTypeEnum returnType, String title) {

		globalSearchActivityListener.showInterestsUsersListFragment(searchBox.getText().toString(),
				returnType, title);
	}

	@Override
	public void goToInterestUserProfile(String id, boolean isInterest) {
		ProfileActivity.openProfileCardFragment(
				getContext(),
				isInterest ? ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND,
				id,
				HomeActivity.PAGER_ITEM_HOME_WEBVIEW
		);
	}

	//endregion


	private class PopulateGlobalThread extends ComputeAndPopulateHandlerThread {

		private JSONArray response;

		PopulateGlobalThread(String name, GlobalSearchFragment fragment, JSONArray response) {
			super(name, fragment);
			this.response = response;
		}

		@Override
		protected void onLooperPrepared() {
			super.onLooperPrepared();

			if (getHandler() != null) {
				getHandler().post(() -> {
					JSONObject lists = response.optJSONObject(0);
					if (lists != null && lists.length() > 0) {
						Iterator<String> iter = lists.keys();
						while (iter.hasNext()) {
							String key = iter.next();
							if (Utils.isStringValid(key)) {
								JSONObject list = lists.optJSONObject(key);
								if (list != null) {
									GlobalSearchObject obj = new GlobalSearchObject(
											list.optString("searchResultType"),
											list.optString("type"),
											list.optString("UIType")
									);

									Object mainObject = null;
									if (obj.isUsers()) {
										mainObject = new UsersBundle().deserializeToClass(list);
										((UsersBundle) mainObject).setNameToDisplay(key);
									} else if (obj.isInterests()) {
										mainObject = new InterestCategory().deserializeToClass(list);
										((InterestCategory) mainObject).setNameToDisplay(key);
									} else if (obj.isPosts()) {
										mainObject = new PostList().deserializeToClass(list);
										((PostList) mainObject).setNameToDisplay(key);
									}
									obj.setMainObject(mainObject);
									globalSearchList.add(obj);
								}
							}
						}

						Collections.sort(globalSearchList);
					}
				});

				exitOps();
			}
		}
	}


	@Override
	public boolean handleMessage(Message msg) {
		getActivity().runOnUiThread(() -> {
			mAdapter.notifyDataSetChanged();
			restorePositions();
		});

		return true;
	}
}
