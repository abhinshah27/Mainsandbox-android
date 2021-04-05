/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;

/**
 * @author mbaldrighi on 5/1/2018.
 */
public class BrowseInterestsByCategoryFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener,
		BrowseInterestsAdapter.OnItemClickListener, LoadMoreResponseHandlerTask.OnDataLoadedListener {

	public static final String LOG_TAG = BrowseInterestsByCategoryFragment.class.getCanonicalName();

	private String categoryId, categoryName;

	private TextView toolbarTitle;
	private View profilePicture;

	private SwipeRefreshLayout srl;

	private RecyclerView baseRecView;
	private LinearLayoutManager llm;
	private BrowseInterestsAdapter baseAdapter;
	private List<Interest> mItems = new ArrayList<>();
	private TextView noResult;

	private int pageIdToCall = 0;
	private boolean fromLoadMore;
	private int newItemsCount;

	private Parcelable currentState;


	public BrowseInterestsByCategoryFragment() {}

	public static BrowseInterestsByCategoryFragment newInstance(String categoryId, String categoryName) {
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, categoryId);
		args.putString(Constants.EXTRA_PARAM_2, categoryName);
		BrowseInterestsByCategoryFragment fragment = new BrowseInterestsByCategoryFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_global_search_lists, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llm = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
		baseAdapter = new BrowseInterestsAdapter(mItems, this);
		baseAdapter.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_CATEGORY_BROWSER);

		if (pageIdToCall == 0) callServer(CallType.GET);
		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		pageIdToCall = getLastPageId();
		currentState = llm.onSaveInstanceState();
	}


	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.back_arrow && Utils.isContextValid(getActivity()))
			getActivity().onBackPressed();
	}

	@Override
	public void onItemClick(Object object) {
		String id = null;
		ProfileHelper.ProfileType type = null;
		if (object instanceof HLUserGeneric) {
			id = ((HLUserGeneric) object).getId();
			type = ProfileHelper.ProfileType.NOT_FRIEND;
		}
		if (object instanceof Interest) {
			id = ((Interest) object).getId();
			type = ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED;
		}

		if (Utils.isStringValid(id) && type != null) {
			profileActivityListener.showProfileCardFragment(type, id);
		}
	}

	@Override
	public void onBackPressed() {}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);
		Utils.setRefreshingForSwipeLayout(srl, false);

		newItemsCount = responseObject != null ? responseObject.length() : 0;

		if (operationId == Constants.SERVER_OP_GET_INTERESTS_BY_CATEGORY) {
			if (fromLoadMore) {
				new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.INTERESTS,
						null, null).execute(responseObject);

//				fromLoadMore = false;
			}
			else
				try {
					setData(responseObject, false);
				} catch (JSONException e) {
					e.printStackTrace();
				}
		}

	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
		Utils.setRefreshingForSwipeLayout(srl, false);

		baseRecView.setVisibility(View.GONE);
		noResult.setText(R.string.error_generic_list);
		noResult.setVisibility(View.VISIBLE);
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);

		handleErrorResponse(operationId, 0);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				categoryId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				categoryName = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
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
		view.setPadding(0, 0, 0, 0);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		if (toolbar != null) {
			toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
			toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
			profilePicture = toolbar.findViewById(R.id.profile_picture);
		}

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				pageIdToCall = 0;
				Utils.setRefreshingForSwipeLayout(srl, true);
				callServer(CallType.GET);
			}
		});

		baseRecView = view.findViewById(R.id.base_list);
		baseRecView.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				fromLoadMore = true;
				callServer(CallType.GET);
			}
		});
		noResult = view.findViewById(R.id.no_result);
	}

	@Override
	protected void setLayout() {
		toolbarTitle.setText(categoryName);
		profilePicture.setVisibility(View.INVISIBLE);

		baseRecView.setLayoutManager(llm);
		baseRecView.setAdapter(baseAdapter);

		llm.onRestoreInstanceState(currentState);
	}

	private enum CallType { GET, SEARCH }
	private void callServer(CallType type) {
		Object[] result = null;

		try {
			if (type == CallType.GET && Utils.isStringValid(categoryId))
				result = HLServerCalls.getInterestsByCategory(mUser.getId(), categoryId, ++pageIdToCall);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
	}


	private void setData(JSONArray response, boolean background) throws JSONException {
		if (pageIdToCall == 1 && (response == null || response.length() == 0) && !background) {
			baseRecView.setVisibility(View.GONE);
			noResult.setText(R.string.no_interests_in_category);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		if (!background) {
			baseRecView.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);
		}

		if (pageIdToCall == 1) {
			if (mItems == null)
				mItems = new ArrayList<>();
			else
				mItems.clear();
		}

		if (response != null) {
			for (int i = 0; i < response.length(); i++) {
				JSONObject jObj = response.getJSONObject(i);
				Interest obj = new Interest().deserializeToClass(jObj);
				mItems.add(obj);
			}

			if (!background)
				baseAdapter.notifyDataSetChanged();
		}
	}


	//region == LOAD MORE INTERFACE METHODS ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return activityListener;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return baseAdapter;
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
