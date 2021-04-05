/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;


import android.content.Intent;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListenerWithPosition;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;
import it.keybiz.lbsapp.corporate.utilities.listeners.SearchTextWatcher;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class InterestFollowersFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		SearchTextWatcher.OnQuerySubmitted, LoadMoreResponseHandlerTask.OnDataLoadedListener,
		BasicAdapterInteractionsListenerWithPosition {

	public static final String LOG_TAG = InterestFollowersFragment.class.getCanonicalName();

	private final String KEY_CAPTION = "caption";
	private final String KEY_ITEMS = "items";

	private String query;
	private String interestId, interestName, interestAvatar;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private EditText searchBox;
	private SearchTextWatcher searchTextWatcher;

	private SwipeRefreshLayout srl;

	private TextView followersTitle;
	private TextView noResult;
	private RecyclerView followersRecView;
	private List<HLUserGeneric> followersList = new ArrayList<>();
	private LinearLayoutManager llm;

	private Map<String, HLUserGeneric> followersLoaded = new ConcurrentHashMap<>();

	private FollowersAdapter followersAdapter;

	private int pageIdToCall = 0;
	private boolean fromLoadMore;
	private int newItemsCount;

	private Integer lastAdapterPosition;


	public InterestFollowersFragment() {
		// Required empty public constructor
	}

	public static InterestFollowersFragment newInstance(String interestId, String interestName, String interestAvatar) {

		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, interestId);
		args.putString(Constants.EXTRA_PARAM_2, interestName);
		args.putString(Constants.EXTRA_PARAM_3, interestAvatar);

		InterestFollowersFragment fragment = new InterestFollowersFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.fragment_interest_followers, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		followersAdapter = new FollowersAdapter(followersList, this);
		followersAdapter.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_INTEREST_FOLLOWERS);

		setLayout();
		callFollowers();
	}

	@Override
	public void onPause() {
		super.onPause();

		lastAdapterPosition = null;

//		Utils.closeKeyboard(searchBox);
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
		if (object instanceof HLUserGeneric) {
			String id = ((HLUserGeneric) object).getId();
			if (id.equals(mUser.getId()) && Utils.isContextValid(getActivity())) {
				Intent in = new Intent(getActivity(), HomeActivity.class);
				in.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
				startActivity(in);
				getActivity().finish();
			}
			else {
				profileActivityListener.showProfileCardFragment(ProfileHelper.ProfileType.NOT_FRIEND, id);
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
	public void setLastAdapterPosition(int position) {
		lastAdapterPosition = position;
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		newItemsCount = 0;
		if (responseObject == null || responseObject.length() == 0) {
			handleErrorResponse(operationId, 0);
			return;
		}

		JSONObject json = responseObject.optJSONObject(0);
		if (json != null && json.length() > 0) {
			JSONArray items = json.optJSONArray("items");
			newItemsCount = items != null ? items.length() : 0;
		}

		switch (operationId) {
			case Constants.SERVER_OP_GET_FOLLOWERS:
				if (fromLoadMore) {
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.FOLLOWERS,
							null, null).execute(responseObject);

//					fromLoadMore = false;
				}
				else setData(responseObject, false);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_FOLLOWERS:
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
				interestId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				interestName = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				interestAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
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
		View toolbar = view.findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
		profilePicture = toolbar.findViewById(R.id.profile_picture);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);

		/*
		searchBox = view.findViewById(R.id.search_box).findViewById(R.id.search_field);
		if (searchBox != null) {
			searchTextWatcher = new SearchTextWatcher(this);
			searchTextWatcher.setSearchType(SearchTextWatcher.SearchType.SINGLE_CHAR);
			searchBox.addTextChangedListener(searchTextWatcher);
			searchBox.setHint(R.string.profile_search_box_hint);
			searchBox.setText(query);
		}
		*/

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);

				pageIdToCall = 0;
				callFollowers();
			}
		});


		followersTitle = view.findViewById(R.id.title);
		noResult = view.findViewById(R.id.no_result);
		followersRecView = view.findViewById(R.id.followers_rv);
		followersRecView.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				callFollowers();
				fromLoadMore = true;
			}
		});
	}

	@Override
	protected void setLayout() {
		if (Utils.isStringValid(interestAvatar))
			MediaHelper.loadProfilePictureWithPlaceholder(getContext(), interestAvatar, profilePicture);

		toolbarTitle.setText(getString(R.string.followers_title, interestName));

		noResult.setText(R.string.no_followers_for_interest);

		followersRecView.setLayoutManager(llm);
		followersRecView.setAdapter(followersAdapter);

		/*
		searchBox.setText(query);
		*/
	}


	@Override
	public void onQueryReceived(String query) {
		this.query = query;

		pageIdToCall = 0;
		if (followersLoaded != null)
			followersLoaded.clear();
		if (followersList != null)
			followersList.clear();
		callFollowers();
	}

	private void callFollowers() {
		Object[] result = null;
		try {
			result = HLServerCalls.getFollowers(mUser.getId(), interestId, ++pageIdToCall);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private void setData(JSONArray response, boolean background) {
		if (!background) {
			followersRecView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
		}

		if (pageIdToCall == 1) {
			if (followersList == null)
				followersList = new ArrayList<>();
			else
				followersList.clear();

			// TODO: 2/4/2018    CHECK IF BLOCK NEEDED
			/*
			Interest interest = HLInterests.getInstance().getInterest(interestId);
			// TODO: 1/24/2018     SWITCH ATTENTION NEEDED
			if (interest != null && interest.isFollowed() *//*&& this.mUser.getSelectedIdentity() instanceof HLUser*//*) {
				HLUserGeneric mUser = new HLUserGeneric(this.mUser.getId(), this.mUser.getCompleteName(), this.mUser.getAvatarURL());
				followersList.add(mUser);
			}
			*/

		}
		try {
			handleFollowersResponse(response, background);
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}


	private void handleFollowersResponse(JSONArray response, boolean background) throws JSONException {
		JSONObject followers = response.getJSONObject(0);
		if (followers != null) {
			Iterator<String> iter = followers.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				if (Utils.isStringValid(key)) {
					if (key.equals(KEY_CAPTION)) {
						String title = followers.getString(key);
						if (Utils.isStringValid(title) && !background)
							followersTitle.setText(title);
					}
					else if (key.equals(KEY_ITEMS)) {
						JSONArray array = followers.optJSONArray(key);
						if (array != null && array.length() > 0) {
							for (int i = 0; i < array.length(); i++) {
								HLUserGeneric user = new HLUserGeneric().deserializeToClass(array.optJSONObject(i));
								followersLoaded.put(user.getId(), user);
								followersList.add(user);
							}
						}
					}
				}
			}

			if (!background) {
				followersAdapter.notifyDataSetChanged();

				if (followersList != null && !followersList.isEmpty()) {
					followersRecView.setVisibility(View.VISIBLE);
					noResult.setVisibility(View.GONE);

					if (lastAdapterPosition != null && lastAdapterPosition <= followersList.size())
						followersRecView.scrollToPosition(lastAdapterPosition);
				}

			}
		}
	}


	//region == LOAD MORE INTERFACE METHODS ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return activityListener;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return followersAdapter;
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
