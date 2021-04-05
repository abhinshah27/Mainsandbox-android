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

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
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
import it.keybiz.lbsapp.corporate.models.HLInterests;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class InterestsFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		SearchHelper.OnQuerySubmitted, InterestsAdapter.OnItemClickListener {

	public static final String LOG_TAG = InterestsFragment.class.getCanonicalName();

	private String userId, userName, userAvatar;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private View followNew;
	private EditText searchBox;

	private TextView allInterestsText;
	private RecyclerView interestsRecView;
	private TextView noResult;
	private List<Interest> interestsToShow = new ArrayList<>();
	private List<Interest> interestsRetrieved = new ArrayList<>();
	private LinearLayoutManager interestsLlm;

	private InterestsAdapter interestsAdapter;

	private boolean automaticTransition = true;
	private boolean isUser;

	private SwipeRefreshLayout srl;

	private SearchHelper mSearchHelper;


	public InterestsFragment() {
		// Required empty public constructor
	}

	public static InterestsFragment newInstance(String userId, String userName, String userAvatar) {

		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_2, userId);
		args.putString(Constants.EXTRA_PARAM_3, userName);
		args.putString(Constants.EXTRA_PARAM_4, userAvatar);

		InterestsFragment fragment = new InterestsFragment();
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

		View view = inflater.inflate(R.layout.fragment_profile_my_interests, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		interestsLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		interestsAdapter = new InterestsAdapter(interestsToShow, this);
		interestsAdapter.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();

		if (searchBox.getText().length() < 3)
			automaticTransition = true;
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_INTERESTS);

		setLayout();
		callForInterests();
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

			case R.id.follow_unfollow_layout:
				profileActivityListener.showFollowInterestFragment(userId, userName, userAvatar);
				break;
		}
	}

	@Override
	public void onItemClick(Object object) {
		if (object instanceof Interest) {
			// TODO: 1/23/2018    check if the interest is one of the identities
			profileActivityListener.showProfileCardFragment(((Interest) object).isClaimed() ?
							ProfileHelper.ProfileType.INTEREST_CLAIMED : ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED,
					((Interest) object).getId());
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_MY_INTERESTS:
				setData(responseObject);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_MY_INTERESTS:
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
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				userId = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				userName = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				userAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_4);

			if (Utils.isStringValid(userId))
				isUser = userId.equals(mUser.getId());
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
				Utils.setRefreshingForSwipeLayout(srl, true);

				callForInterests();
			}
		});

		followNew = view.findViewById(R.id.follow_unfollow_layout);
		followNew.setOnClickListener(this);
		followNew.requestFocus();

		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
		searchBox.setHint(R.string.interest_search_box_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		allInterestsText = view.findViewById(R.id.all_interests_section);
		interestsRecView = view.findViewById(R.id.all_interests_rv);
		noResult = view.findViewById(R.id.no_result);
	}

	@Override
	protected void setLayout() {
		if (Utils.isStringValid(userAvatar))
			MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);

		if (isUser) {
			toolbarTitle.setText(R.string.title_my_interests);
			followNew.setVisibility(View.VISIBLE);
		}
		else {
			toolbarTitle.setText(getString(R.string.title_other_s_interests, Utils.getFirstNameForUI(userName)));
			followNew.setVisibility(View.GONE);
		}

		interestsRecView.setLayoutManager(interestsLlm);
		interestsRecView.setAdapter(interestsAdapter);

		resetInterests(true);

		if (interestsToShow == null)
			interestsToShow = new ArrayList<>();
		else
			interestsToShow.clear();
		interestsToShow.addAll(HLInterests.getInstance().getSortedInterests());

		if (interestsToShow == null || interestsToShow.isEmpty()) {
			interestsRecView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
		}
		else {
			interestsRecView.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);
		}
		interestsAdapter.notifyDataSetChanged();

		if (searchBox.getText().length() > 0) {
			onQueryReceived(searchBox.getText().toString());
			return;
		}

		allInterestsText.setText(getString(R.string.my_interests_title_rv, interestsToShow.size()));
		searchBox.setEnabled(!interestsToShow.isEmpty());
	}


	@Override
	public void onQueryReceived(@NonNull final String query) {
		if (interestsRetrieved != null && !interestsRetrieved.isEmpty()) {
			interestsToShow.clear();
			Collections.sort(interestsRetrieved);
			if (!query.isEmpty()) {
				interestsToShow.addAll(Stream.of(interestsRetrieved).filter(new Predicate<Interest>() {
					@Override
					public boolean test(Interest interest) {
						return interest.getName().toLowerCase().contains(query.toLowerCase());
					}
				}).collect(Collectors.toList()));
				allInterestsText.setText(getString(R.string.search_results_txt, interestsToShow.size()));
			}
			else {
				interestsToShow.addAll(interestsRetrieved);
				allInterestsText.setText(getString(R.string.my_interests_title_rv, interestsToShow.size()));
			}
			interestsAdapter.notifyDataSetChanged();

			if (interestsToShow.isEmpty()) {
				noResult.setText(R.string.no_search_result);
				interestsRecView.setVisibility(View.GONE);
				noResult.setVisibility(View.VISIBLE);
			}
			else {
				interestsRecView.setVisibility(View.generateViewId());
				noResult.setVisibility(View.GONE);
			}
		}
	}

	private void callForInterests() {
		Object[] result = null;

		try {
			result = HLServerCalls.getMyInterests(userId);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
	}

	private void setData(JSONArray response) {
		if (response == null || response.length() == 0) {
			interestsRecView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		interestsRecView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		HLInterests interests = HLInterests.getInstance();

		try {
			interests.setInterests(response, mUser.getUserId());
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}

		if (interestsToShow == null)
			interestsToShow = new ArrayList<>();
		else
			interestsToShow.clear();

		List<Interest> list = new ArrayList<>(interests.getInterests());
		Collections.sort(list);
		interestsToShow.addAll(list);
		interestsAdapter.notifyDataSetChanged();

		resetInterests(false);
		interestsRetrieved.addAll(list);

		allInterestsText.setText(getString(R.string.my_interests_title_rv, interestsToShow.size()));
		searchBox.setEnabled(!interestsToShow.isEmpty());
	}

	private void resetInterests(boolean restoreFromRealm) {
		if (interestsRetrieved == null)
			interestsRetrieved = new ArrayList<>();
		else
			interestsRetrieved.clear();

		if (restoreFromRealm)
			interestsRetrieved.addAll(HLInterests.getInstance().getSortedInterests());
	}

}
