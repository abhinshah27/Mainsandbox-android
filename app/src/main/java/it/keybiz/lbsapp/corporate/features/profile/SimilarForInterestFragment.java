/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.io.Serializable;
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
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SimilarForInterestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SimilarForInterestFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		SearchHelper.OnQuerySubmitted/*, LoadMoreResponseHandlerTask.OnDataLoadedListener*/,
		SimilarForInterestAdapter.OnItemClickListener {

	public static final String LOG_TAG = SimilarForInterestFragment.class.getCanonicalName();

	public enum ViewType implements Serializable { SIMILAR, SIMILAR_EMPTY_DIARY }
	private ViewType viewType;

	private String interestId;
	private String interestName;
	private String interestAvatar;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private SwipeRefreshLayout srl;

	private RecyclerView recyclerView;
	private LinearLayoutManager llm;

	private View searchBoxLayout;
	private EditText searchBox;

	private TextView noResult;
	private TextView infoNoPost;
	private List<Interest> similarLoaded = new ArrayList<>();
	private List<Interest> similarToDisplay = new ArrayList<>();
	private SimilarForInterestAdapter similarAdapter;

	private SearchHelper mSearchHelper;


	public SimilarForInterestFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment InnerCircleFragment.
	 */
	public static SimilarForInterestFragment newInstance(ViewType type, String interestId,
	                                                     String interestName, String interestAvatar) {
		SimilarForInterestFragment fragment = new SimilarForInterestFragment();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, interestId);
		args.putString(Constants.EXTRA_PARAM_2, interestName);
		args.putString(Constants.EXTRA_PARAM_3, interestAvatar);
		args.putSerializable(Constants.EXTRA_PARAM_4, type);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_interest_similar, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
		callServer();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), viewType == ViewType.SIMILAR ?
				AnalyticsUtils.ME_INTEREST_SIMILAR : null);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchBox);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);

		similarAdapter = new SimilarForInterestAdapter(similarToDisplay, this);
		similarAdapter.setHasStableIds(true);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.back_arrow:
				if (getActivity() != null)
					getActivity().onBackPressed();
		}
	}

	@Override
	public void onItemClick(Object object) {
		Interest interest = ((Interest) object);
		String id = interest.getId();

		if (Utils.isStringValid(id))
			profileActivityListener.showProfileCardFragment(ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED, id);
		else
			activityListener.showGenericError();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
//		if (context instanceof OnProfileFragmentInteractionListener) {
//			mListener = (OnProfileFragmentInteractionListener) context;
//		} else {
//			throw new RuntimeException(context.toString()
//					+ " must implement OnFragmentInteractionListener");
//		}
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);
//		if (context instanceof OnProfileFragmentInteractionListener) {
//			mListener = (OnProfileFragmentInteractionListener) context;
//		} else {
//			throw new RuntimeException(context.toString()
//					+ " must implement OnFragmentInteractionListener");
//		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
//		mListener = null;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(Constants.EXTRA_PARAM_1, interestId);
		outState.putString(Constants.EXTRA_PARAM_2, interestName);
		outState.putString(Constants.EXTRA_PARAM_3, interestAvatar);
		outState.putSerializable(Constants.EXTRA_PARAM_4, viewType);
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
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				viewType = (ViewType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_4);

//			if (Utils.isStringValid(interestId) && RealmObject.isValid(mUser))
//				isUser = userId.equals(mUser.getId());
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		try {
			switch (operationId) {
				case Constants.SERVER_OP_GET_SIMILAR_INTERESTS:
					handleSimilarResponse(responseObject);
					break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_SIMILAR_INTERESTS:
				activityListener.showAlert(R.string.error_generic_list);
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}


	/*
	//region == LOAD MORE INTERFACE METHODS ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return activityListener;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return circlesAdapter;
	}

	@Override
	public void setData(Realm realm) {}

	@Override
	public void setData(JSONArray array) {
		setData(true);
	}

	@Override
	public boolean isFromLoadMore() {
		return fromLoadMore;
	}

	@Override
	public int getLastPageId() {
		if (circlesListLoaded == null)
			circlesListLoaded = new HashMap<>();

		return (circlesListLoaded.size() / 20);
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}

	//endregion
	*/



	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		configureToolbar(toolbar);

		view.findViewById(R.id.focus_catcher).requestFocus();

		searchBoxLayout = view.findViewById(R.id.search_box);
		searchBox = searchBoxLayout.findViewById(R.id.search_field);
		searchBox.setHint(R.string.hint_search_similar);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchBoxLayout, searchBox);

		infoNoPost = view.findViewById(R.id.info_no_post);

		recyclerView = view.findViewById(R.id.initiatives_page);
//		recyclerView.addOnScrollListener(new LoadMoreScrollListener() {
//			@Override
//			public void onLoadMore() {
//				callServer();
//				fromLoadMore = true;
//			}
//		});
		noResult = view.findViewById(R.id.no_result);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);

				callServer();
			}
		});
	}

	@Override
	protected void setLayout() {
		toolbarTitle.setText(viewType == ViewType.SIMILAR ? getString(R.string.similar_title, interestName) : getString(R.string.profile_diary_friend));

		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), interestAvatar, profilePicture);

		recyclerView.setLayoutManager(llm);

		recyclerView.setAdapter(similarAdapter);
		setDataForSimilar();

		searchBoxLayout.setVisibility(viewType == ViewType.SIMILAR ? View.VISIBLE : View.GONE);
		infoNoPost.setVisibility(viewType == ViewType.SIMILAR ? View.GONE : View.VISIBLE);
		infoNoPost.setText(Utils.getFormattedHtml(getString(R.string.interest_no_posts, interestName)));
	}

	private void configureToolbar(Toolbar toolbar) {
		if (toolbar != null) {
			toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
			toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
			profilePicture = toolbar.findViewById(R.id.profile_picture);
		}
	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		RecyclerView.Adapter adapter;

		adapter = similarAdapter;

		if (similarLoaded != null && !similarLoaded.isEmpty()) {
			similarToDisplay.clear();
			similarToDisplay.addAll(Stream.of(similarLoaded).filter(new Predicate<Interest>() {
				@Override
				public boolean test(Interest interest) {
					return interest.getName().toLowerCase().contains(query.toLowerCase());
				}
			}).collect(Collectors.toList()));
		}

		int finalSize = similarToDisplay != null ? similarToDisplay.size() : 0;
		if (finalSize == 0) {
			recyclerView.setVisibility(View.GONE);

			noResult.setText(R.string.no_search_result);
			noResult.setVisibility(View.VISIBLE);
		} else {
			recyclerView.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);

			adapter.notifyDataSetChanged();
		}
	}

	private void callServer() {
		Utils.setRefreshingForSwipeLayout(srl, true);

		Object[] result = null;
		try {
			result = HLServerCalls.getSimilarInterests(interestId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private void handleSimilarResponse(JSONArray response) throws JSONException {
		JSONObject json = response.getJSONObject(0);
		JSONArray images = json.getJSONArray("imgs");

		if (similarToDisplay == null)
			similarToDisplay = new ArrayList<>();
		else
			similarToDisplay.clear();

		if (similarLoaded == null)
			similarLoaded = new ArrayList<>();
		else
			similarLoaded.clear();

		if (images != null && images.length() > 0) {
			for (int i = 0; i < images.length(); i++) {
				Interest interest = new Interest().deserializeToClass(images.optJSONObject(i));
				if (interest != null) {
					similarLoaded.add(interest);
					similarToDisplay.add(interest);
				}
			}
		}
		setLayout();
	}

	private void setDataForSimilar() {
		if (similarLoaded == null || similarLoaded.size() == 0) {
			// INFO: 2/26/19    noResult HIDDEN
			noResult.setVisibility(View.GONE);
			noResult.setText(R.string.no_similar_for_interest);
			recyclerView.setVisibility(View.GONE);
			return;
		}

		if (similarToDisplay == null)
			similarToDisplay = new ArrayList<>();
		else
			similarToDisplay.clear();

		noResult.setVisibility(View.GONE);
		recyclerView.setVisibility(View.VISIBLE);
		Collections.sort(similarLoaded);
		similarToDisplay.addAll(similarLoaded);
		similarAdapter.notifyDataSetChanged();
	}

	//endregion

}