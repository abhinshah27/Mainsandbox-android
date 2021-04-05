/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.annimon.stream.function.Predicate;

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
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;

/**
 * A simple {@link Fragment} subclass.
 * @author mbaldrighi on 3/8/2018.
 */
public class WishInnerCircleRecipientsFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, SearchHelper.OnQuerySubmitted,
		WishInnerCircleInterestAdapter.OnElementClickedListener, LoadMoreResponseHandlerTask.OnDataLoadedListener,
		WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishInnerCircleRecipientsFragment.class.getCanonicalName();

	private String query;

	private View allMembersCheck, specificMembersCheck;

	private View layoutBoxRecView;
	private EditText searchBox;

	private RecyclerView baseRecView;
	private LinearLayoutManager llm;
	private List<WishListElement> searchResults = new ArrayList<>();
	private List<WishListElement> searchResultsToShow = new ArrayList<>();
	private WishInnerCircleInterestAdapter selectionAdapter;

	private TextView noResult;

	private boolean fromLoadMore = false;
	private int pageIdToCall = 1;
	private int newItemsCount;

	private ArrayList<String> recipients = new ArrayList<>();

	private SearchHelper mSearchHelper;

	private SwipeRefreshLayout srl;


	public WishInnerCircleRecipientsFragment() {
		// Required empty public constructor
	}

	public static WishInnerCircleRecipientsFragment newInstance() {
		Bundle args = new Bundle();
		WishInnerCircleRecipientsFragment fragment = new WishInnerCircleRecipientsFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_wishes_recipients_ic, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() != null) {
			selectionAdapter = new WishInnerCircleInterestAdapter(searchResultsToShow, this,
					WishInnerCircleInterestAdapter.ItemType.RECIPIENT);
			llm = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
		}

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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_RECIPIENT);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

//		pageIdToCall = 1;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (getActivity() instanceof WishesActivity)
			((WishesActivity) getActivity()).setBackListener(null);
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
		if (recipients == null)
			recipients = new ArrayList<>();
		else
			recipients.clear();

		switch (v.getId()) {
			case R.id.all_members:
				if (allMembersCheck.isSelected()) {
					allMembersCheck.setSelected(false);
				}
				else {
					if (searchResults == null || searchResults.isEmpty())
						return;

					allMembersCheck.setSelected(true);
					recipients.add("all");

					if (specificMembersCheck.isSelected()) {
						specificMembersCheck.setSelected(false);
						layoutBoxRecView.setVisibility(View.GONE);
					}
				}
				break;

			case R.id.specific_members:
				specificMembersCheck.setSelected(!specificMembersCheck.isSelected());
				if (allMembersCheck.isSelected())
					allMembersCheck.setSelected(false);

				layoutBoxRecView.setVisibility(specificMembersCheck.isSelected() ? View.VISIBLE : View.GONE);
				break;
		}

		wishesActivityListener.enableDisableNextButton(recipients != null && !recipients.isEmpty());
	}

	@Override
	public void onElementClick(@NonNull WishListElement listElement) {
		listElement.setSelected(!listElement.isSelected());
		selectionAdapter.notifyDataSetChanged();

		String id = listElement.getFriendID();
		if (listElement.isSelected() && !recipients.contains(id))
			recipients.add(id);
		else if (!listElement.isSelected())
			recipients.remove(id);

		wishesActivityListener.enableDisableNextButton(recipients != null && !recipients.isEmpty());
	}

	@Override
	public void onNextClick() {
		if (searchResults != null && !searchResults.isEmpty()) {
			wishesActivityListener.setSelectedWishListElement(searchResults.get(0));

			Bundle bundle = new Bundle();
			bundle.putStringArrayList("members", recipients);
			wishesActivityListener.setDataBundle(bundle);

			wishesActivityListener.resumeNextNavigation();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (getActivity() == null || !(getActivity() instanceof HLActivity)) return;


	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Utils.isContextValid(getActivity()) && getActivity() instanceof HLActivity) {
			HLActivity activity = ((HLActivity) getActivity());

			switch (requestCode) {

			}
		}
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

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
				if (responseObject == null || responseObject.length() == 0)
					return;

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
			case Constants.SERVER_OP_WISH_GET_POSTS:
				activityListener.showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);

		activityListener.closeProgress();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
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

			selectionAdapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		allMembersCheck = view.findViewById(R.id.all_members);
		allMembersCheck.setOnClickListener(this);
		specificMembersCheck = view.findViewById(R.id.specific_members);
		specificMembersCheck.setOnClickListener(this);

		layoutBoxRecView = view.findViewById(R.id.layout_box_recview);
		layoutBoxRecView.setVisibility(View.GONE);
		View searchLayout = layoutBoxRecView.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		searchBox.setText(query);

		noResult = layoutBoxRecView.findViewById(R.id.no_result);
		baseRecView = layoutBoxRecView.findViewById(R.id.generic_rv);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);

				wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
			}
		});
	}

	@Override
	protected void setLayout() {
		baseRecView.setLayoutManager(llm);
		baseRecView.setAdapter(selectionAdapter);

		searchBox.setText(query);
		searchBox.setHint(R.string.action_search);
	}

	private void setData(JSONArray response, boolean background) throws JSONException {
		if (pageIdToCall == 1 && (response == null ||
				(response.getJSONObject(0) != null && response.getJSONObject(0).length() == 0))) {
			baseRecView.setVisibility(View.GONE);
			noResult.setText(Utils.isStringValid(query) ? R.string.no_search_result : R.string.no_result_wish_base_list_2);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		baseRecView.setVisibility(View.VISIBLE);
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
			}

			searchResultsToShow.addAll(searchResults);
			if (!background)
				selectionAdapter.notifyDataSetChanged();
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}


	//region == Load more methods ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return null;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return selectionAdapter;
	}

	@Override
	public void setData(Realm realm) {

	}

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
