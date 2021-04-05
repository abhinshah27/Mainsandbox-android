/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


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

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.enums.SearchTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.UnBlockUserEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsPrivacyBlockedFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		SearchHelper.OnQuerySubmitted, OnBackPressedListener, BasicAdapterInteractionsListener {

	public static final String LOG_TAG = SettingsPrivacyBlockedFragment.class.getCanonicalName();

	private String query;

	// TODO: 3/26/2018    type is TEMPORARILY passed as NULL
	private SearchTypeEnum type;

	private EditText searchBox;

	private SwipeRefreshLayout srl;

	private TextView noResult;
	private RecyclerView searchRecView;
	private List<HLUserGeneric> searchResults = new ArrayList<>();
	private List<HLUserGeneric> searchResultsToShow = new ArrayList<>();
	private LinearLayoutManager searchLlm;

	// TODO: 2/27/2018     SEARCH TEMPORARILY LOCAL
	private Map<String, HLUserGeneric> resultsLoaded = new ConcurrentHashMap<>();

	private SettingsBlockedUsersAdapter searchAdapter;

	private int pageIdToCall = 1;     // pageIdToCall automatically set to 1 instead of 0 because pagination is temporarily disabled
	private boolean fromLoadMore;
	private int newItemsCount;

	private MaterialDialog dialogUnblock;

	private String unblockedUserId;

	private SearchHelper mSearchHelper;


	public SettingsPrivacyBlockedFragment() {
		// Required empty public constructor
	}

	public static SettingsPrivacyBlockedFragment newInstance(SearchTypeEnum type) {

		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, type);

		SettingsPrivacyBlockedFragment fragment = new SettingsPrivacyBlockedFragment();
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

		View view = inflater.inflate(R.layout.fragment_settings_blocked_users, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		searchLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		searchAdapter = new SettingsBlockedUsersAdapter(searchResultsToShow, this);
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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_PRIVACY_BLOCKED);

		callServer(null, null);

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

		}
	}

	@Override
	public void onItemClick(final Object object) {
		if (object instanceof HLUserGeneric) {
			dialogUnblock = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_block_unblock_user);
			if (dialogUnblock != null) {
				View view = dialogUnblock.getCustomView();
				if (view != null) {
					((TextView) view.findViewById(R.id.dialog_message))
							.setText(getString(R.string.dialog_block_unblock_message_u, ((HLUserGeneric) object).getCompleteName()));

					((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.dialog_block_unblock_title_u);

					DialogUtils.setPositiveButton(view.findViewById(R.id.button_positive), R.string.action_unblock, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							callServer(unblockedUserId = ((HLUserGeneric) object).getId(), UnBlockUserEnum.UNBLOCK);
						}
					});

					view.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dialogUnblock.dismiss();
						}
					});
				}

				dialogUnblock.show();
			}
		}
	}

	@Override
	public void onItemClick(Object object, View view) {}

	@Override
	public HLUser getUser() {
		return mUser;
	}

	@Override
	public void onBackPressed() {

	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		if (responseObject == null)
			return;

		newItemsCount = responseObject.length();

		switch (operationId) {

			// TODO: 2/27/2018     SEARCH TEMPORARILY LOCAL
			case Constants.SERVER_OP_SEARCH:
//				if (fromLoadMore)
//					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.SEARCH,
//							null, null).execute(responseObject);
//				else
					setData(responseObject, false);
				break;

			case Constants.SERVER_OP_SETTINGS_PRIVACY_GET_BLOCKED:
				setData(responseObject, false);
				break;

			case Constants.SERVER_OP_SETTING_BLOCK_UNBLOCK_USER:
				LogUtils.d(LOG_TAG, "User " + unblockedUserId + " correctly blocked");
				dialogUnblock.dismiss();
				callServer(null, null);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_SEARCH:
			case Constants.SERVER_OP_SETTINGS_PRIVACY_GET_BLOCKED:
				activityListener.showAlert(R.string.error_generic_list);
				break;

			case Constants.SERVER_OP_SETTING_BLOCK_UNBLOCK_USER:
				activityListener.showGenericError();
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
		View layoutSearchBox = view.findViewById(R.id.search_box);
		searchBox = layoutSearchBox.findViewById(R.id.search_field);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(layoutSearchBox, searchBox);

		searchBox.setText(query);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);
				callServer(null, null);
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
		searchBox.setHint(R.string.search_by_name);
	}


	@Override
	public void onQueryReceived(@NonNull final String query) {
		this.query = query;

		if (searchResults != null && !searchResults.isEmpty()) {
			searchResultsToShow.clear();
			searchResultsToShow.addAll(Stream.of(searchResults).filter(new Predicate<HLUserGeneric>() {
				@Override
				public boolean test(HLUserGeneric contact) {
					return contact.getName().toLowerCase().contains(query.toLowerCase());
				}
			}).collect(Collectors.toList()));

			searchAdapter.notifyDataSetChanged();
		}
	}

	private void callServer(String friendId, @Nullable UnBlockUserEnum type) {
		Object[] result = null;

		try {
			if (type == null)
				result = HLServerCalls.settingsGetBlockedUsers(mUser.getUserId());
			else
				result = HLServerCalls.blockUnblockUsers(mUser.getUserId(), friendId, type);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}

	private void callSearchFromServer(String queryString) {
		HLServerCalls.search(((HLActivity) getActivity()), queryString, type,
				++pageIdToCall, this);
	}

	private void setData(JSONArray response, boolean background) {
		if (pageIdToCall == 1 && (response == null || (response.length() == 0))) {
			searchRecView.setVisibility(View.GONE);
			noResult.setText(Utils.isStringValid(query) ? R.string.no_search_result : R.string.no_blocked_users);
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

			if (searchResultsToShow == null)
				searchResultsToShow = new ArrayList<>();
			else
				searchResultsToShow.clear();
		}
		try {
			for (int i = 0; i < response.length(); i++) {
				JSONObject json = response.getJSONObject(i);
				HLUserGeneric contact = new HLUserGeneric().deserializeToClass(json);
				searchResults.add(contact);
				searchResultsToShow.add(contact);
			}

			if (!background)
				searchAdapter.notifyDataSetChanged();
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}



	//region == LOAD MORE INTERFACE METHODS ==

	/*
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
	public int getLastPageId() {
		return pageIdToCall - 1;
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}
	*/

	//endregion


}
