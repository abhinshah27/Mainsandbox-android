/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
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
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.GenericUserFamilyRels;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/22/2018.
 */
public class ProfileFamilyRelationsStep1Fragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		BasicAdapterInteractionsListener, SearchHelper.OnQuerySubmitted,
		LoadMoreResponseHandlerTask.OnDataLoadedListener {

	public static final String LOG_TAG = ProfileFamilyRelationsStep1Fragment.class.getCanonicalName();

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private View layoutSearchBox;
	private EditText searchBox;
	private String query;

	private RecyclerView membersView;
	private List<GenericUserFamilyRels> membersList = new ArrayList<>();
	private List<GenericUserFamilyRels> membersListToShow = new ArrayList<>();
	private LinearLayoutManager membersLlm;
	private ProfileFamilyRelationshipsAdapter membersAdapter;
	private TextView noResult;

	private SwipeRefreshLayout srl;

	// TODO: 3/22/2018    some preparation has been made BUT COMPLETE!!!
	private int pageIdToCall = 1;
	private boolean fromLoadMore = false;
	private int newItemCount;

	private SearchHelper mSearchHelper;


	public ProfileFamilyRelationsStep1Fragment() {
		// Required empty public constructor
	}

	public static ProfileFamilyRelationsStep1Fragment newInstance() {
		Bundle args = new Bundle();
		ProfileFamilyRelationsStep1Fragment fragment = new ProfileFamilyRelationsStep1Fragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_profile_family_step_1, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof ProfileActivity)
			((ProfileActivity) getActivity()).setBackListener(null);

		if (Utils.isContextValid(getActivity())) {
			membersAdapter = new ProfileFamilyRelationshipsAdapter(membersListToShow, this);
			membersAdapter.setHasStableIds(true);

			membersLlm = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_FAMILY_USER);

		callServer();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchBox);
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
			case R.id.back_arrow:
				if (Utils.isContextValid(getActivity()))
					getActivity().onBackPressed();
				break;
		}
	}

	// Adapter's custom interface callback
	@Override
	public void onItemClick(Object object) {
		if (object instanceof GenericUserFamilyRels) {
			profileActivityListener.showFamilyRelationsStep2Fragment((GenericUserFamilyRels) object);
		}
	}

	@Override
	public void onItemClick(Object object, View view) {}

	@Override
	public HLUser getUser() {
		return mUser;
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_INNERCIRCLE_FOR_FAMILY:
				setData(responseObject, false);
				break;

		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		@StringRes int msg = R.string.error_generic_list;
		switch (operationId) {
			case Constants.SERVER_OP_GET_INNERCIRCLE_FOR_FAMILY:
				break;
		}

		activityListener.showAlert(msg);
	}

	@Override
	public void onMissingConnection(int operationId) {
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

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		profilePicture = toolbar.findViewById(R.id.profile_picture);
		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);

		membersView = view.findViewById(R.id.generic_rv);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);

				callServer();
			}
		});

		layoutSearchBox = view.findViewById(R.id.search_box);
		searchBox = layoutSearchBox.findViewById(R.id.search_field);
		searchBox.setHint(R.string.profile_search_box_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(layoutSearchBox, searchBox);

		searchBox.setText(query);

		noResult = view.findViewById(R.id.no_result);
	}

	@Override
	protected void setLayout() {
		toolbarTitle.setText(R.string.title_family_relations);
		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), mUser.getUserAvatarURL(), profilePicture);

		layoutSearchBox.setVisibility(View.VISIBLE);

		membersView.setAdapter(membersAdapter);
		membersView.setLayoutManager(membersLlm);

		searchBox.setText(query);
		searchBox.setHint(R.string.profile_search_box_hint);
	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		this.query = query;

		if (membersList != null && !membersList.isEmpty()) {
			membersListToShow.clear();
			membersListToShow.addAll(Stream.of(membersList).filter(new Predicate<GenericUserFamilyRels>() {
				@Override
				public boolean test(GenericUserFamilyRels member) {
					return member.getCompleteName().toLowerCase().contains(query.toLowerCase());
				}
			}).collect(Collectors.toList()));

			membersView.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);

			if (membersListToShow.isEmpty()) {
				membersView.setVisibility(View.GONE);
				noResult.setText(R.string.no_search_result);
				noResult.setVisibility(View.VISIBLE);
			}

			membersAdapter.notifyDataSetChanged();
		}
	}

	private void setData(final JSONArray response, boolean background) {
		if (response == null || response.length() == 0) {
			membersView.setVisibility(View.GONE);
			noResult.setText(R.string.no_member_in_circle);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		if (pageIdToCall == 1) {

			if (membersList == null)
				membersList = new ArrayList<>();
			else
				membersList.clear();
			if (membersListToShow == null)
				membersListToShow = new ArrayList<>();
			else
				membersListToShow.clear();
		}

		membersView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		try {
			for (int i = 0; i < response.length(); i++) {
				JSONObject json = response.getJSONObject(i);
				GenericUserFamilyRels obj = new GenericUserFamilyRels().deserializeComplete(json);
				if (obj !=null)
					membersList.add(obj);
			}

			membersListToShow.addAll(membersList);
			if (!background)
				membersAdapter.notifyDataSetChanged();
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}

	}


	private void callServer() {
		Object[] result = null;

		try {
			// TODO: 3/22/2018    restore PAGINATION
			pageIdToCall = 1;

			result = HLServerCalls.getInnerCircleForFamilyRelationships(mUser.getUserId());
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}


	private void setDataFromSearch(JSONArray response, boolean background) {
		if (pageIdToCall == 1) {
			if (response == null || response.length() == 0) {
				membersView.setVisibility(View.GONE);
				noResult.setVisibility(View.VISIBLE);
				return;
			}

			if (membersList == null)
				membersList = new ArrayList<>();
			else
				membersList.clear();
		}

		membersView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		try {
			for (int i = 0; i < response.length(); i++) {
				JSONObject json = response.getJSONObject(i);
				GenericUserFamilyRels obj = new GenericUserFamilyRels().deserializeComplete(json);
				if (obj !=null)
					membersList.add(obj);
			}

			membersListToShow.addAll(membersList);
			if (!background)
				membersAdapter.notifyDataSetChanged();
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}


	//region == Load More interface ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return null;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return membersAdapter;
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
