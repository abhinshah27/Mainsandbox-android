/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.createPost;

import android.content.Context;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.tags.TagAdapter;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnTagFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TagFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TagFragment extends HLFragment implements SearchHelper.OnQuerySubmitted,
		OnServerMessageReceivedListener, OnMissingConnectionListener {

	private EditText searchBox;
	private View layoutInterests;
	private TextView textPeople, noResultPeople, noResultInterests;
	private RecyclerView recViewPeople, recViewInterests;
	private LinearLayoutManager llmPeople, llmInterests;
	private TagAdapter adapterPeople, adapterInterests;

	private List<HLUserGeneric> users = new ArrayList<>();
	private List<Interest> interests = new ArrayList<>();
	private List<Object> usersToShow = new ArrayList<>(),
			interestsToShow = new ArrayList<>();

	private String query;

	private SearchHelper mSearchHelper;

	private OnTagFragmentInteractionListener mListener;


	public TagFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 * @return A new instance of fragment ProfileFragment.
	 */
	public static TagFragment newInstance() {
		TagFragment fragment = new TagFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.cpost_fragment_tags, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llmPeople = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		llmInterests = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

		adapterPeople = new TagAdapter(usersToShow, mListener.getCreatePostHelper(), false);
		adapterPeople.setHasStableIds(true);
		adapterInterests = new TagAdapter(interestsToShow, mListener.getCreatePostHelper(), false);
		adapterInterests.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		getData();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchBox);
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnTagFragmentInteractionListener) {
			mListener = (OnTagFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnTagFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnTagFragmentInteractionListener {
		Object[] isObjectForTagSelected(String id, boolean fromInitiatives);
		BasicAdapterInteractionsListener getCreatePostHelper();
		void addTagToSearchList(Tag tag);
		void updateSearchData(String query);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		setData(responseObject, operationId);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
		searchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					adapterPeople.notifyDataSetChanged();
					adapterInterests.notifyDataSetChanged();
				}
			}
		});

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

//		searchBox.addTextChangedListener(new SearchTextWatcher(this,
//				SearchTextWatcher.SearchType.SINGLE_CHAR));

		layoutInterests = view.findViewById(R.id.layout_list_interests);

		textPeople = view.findViewById(R.id.tag_people_text);
		noResultPeople = view.findViewById(R.id.no_result_tag_people);
		noResultInterests = view.findViewById(R.id.no_result_tag_interests);

		recViewPeople = view.findViewById(R.id.tag_rv_people);
		recViewInterests = view.findViewById(R.id.tag_rv_interests);
	}

	@Override
	protected void setLayout() {
		recViewPeople.setLayoutManager(llmPeople);
		recViewPeople.setAdapter(adapterPeople);
		recViewInterests.setLayoutManager(llmInterests);
		recViewInterests.setAdapter(adapterInterests);

		layoutInterests.setVisibility(mUser.isActingAsInterest() ? View.GONE : View.VISIBLE);

		if (Utils.isStringValid(query))
			searchBox.setText(query);

		if (mUser.isActingAsInterest()) {
			layoutInterests.setVisibility(View.GONE);
			noResultPeople.setText(R.string.no_followers_for_interest_short);
			textPeople.setText(R.string.tag_section_followers);
		}
		else {
			layoutInterests.setVisibility(View.VISIBLE);
			noResultPeople.setText(R.string.no_people_in_ic_short);
			textPeople.setText(R.string.tag_section_people);
		}
	}

	@Override
	public void onQueryReceived(String query) {
		this.query = query;

		mListener.updateSearchData(query);
	}


	private void getData() {
		if (mUser.isActingAsInterest())
			callServer(CallType.FOLLOWERS);
		else {
			callServer(CallType.USERS);
			callServer(CallType.INTERESTS);
		}

	}

	private enum CallType { USERS, INTERESTS, FOLLOWERS }
	private void callServer(CallType type) {
		Object[] result = null;

		try {
			if (type == CallType.USERS)
				result = HLServerCalls.getInnerCircle(mUser.getUserId(), 1);
			else if (type == CallType.INTERESTS)
				result = HLServerCalls.getMyInterests(mUser.getUserId());
			else if (type == CallType.FOLLOWERS)
				result = HLServerCalls.getFollowers(mUser.getId(), mUser.getId(), 1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
	}


	private void setData(JSONArray response, int operationId) {
		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
			case Constants.SERVER_OP_GET_FOLLOWERS:
				JSONArray jUsers = new JSONArray();
				JSONObject object0 = response.optJSONObject(0);
				if (operationId == Constants.SERVER_OP_GET_FOLLOWERS) {
					if (object0 != null && object0.length() > 0) {
						jUsers = object0.optJSONArray("items");
					}
				}
				else {
					if (object0 != null) {
						JSONArray lists = object0.optJSONArray("lists");
						if (lists != null && lists.length() > 0) {
							JSONObject list = lists.optJSONObject(0);
							if (list != null && list.length() > 0) {
								jUsers = list.optJSONArray("users");
							}
						}
					}
				}

				if (jUsers == null || jUsers.length() == 0) {
					recViewPeople.setVisibility(View.GONE);
					noResultPeople.setVisibility(View.VISIBLE);
					return;
				}

				recViewPeople.setVisibility(View.VISIBLE);
				noResultPeople.setVisibility(View.GONE);

				if (usersToShow == null)
					usersToShow = new ArrayList<>();
				else
					usersToShow.clear();
				if (users == null)
					users = new ArrayList<>();
				else
					users.clear();

				if (jUsers.length() > 0) {
					for (int i = 0; i < jUsers.length(); i++) {
						JSONObject jUser = jUsers.optJSONObject(i);
						if (jUser != null) {
							HLUserGeneric user = new HLUserGeneric().deserializeToClass(jUser);
							if (user != null) {
								users.add(user);

								mListener.addTagToSearchList(Tag.convertFromGenericUser(user));
							}
						}
					}
				}

				Collections.sort(users);
				usersToShow.addAll(users);
				adapterPeople.notifyDataSetChanged();
				break;


			case Constants.SERVER_OP_GET_MY_INTERESTS:
				if (response == null || response.length() == 0) {
					recViewInterests.setVisibility(View.GONE);
					noResultInterests.setVisibility(View.VISIBLE);
					return;
				}

				recViewInterests.setVisibility(View.VISIBLE);
				noResultInterests.setVisibility(View.GONE);

				if (interestsToShow == null)
					interestsToShow = new ArrayList<>();
				else
					interestsToShow.clear();
				if (this.interests == null)
					this.interests = new ArrayList<>();
				else
					this.interests.clear();

				for (int i = 0; i < response.length(); i++) {
					JSONObject jInterest = response.optJSONObject(i);
					if (jInterest != null) {
						Interest interest = new Interest().deserializeToClass(jInterest);
						if (interest != null) {
							interests.add(interest);

							mListener.addTagToSearchList(Tag.convertFromInterest(interest));
						}
					}
				}

				Collections.sort(interests);
				interestsToShow.addAll(interests);
				adapterInterests.notifyDataSetChanged();
				break;
		}
	}


	//region == Getters and setters ==

	public TagAdapter getAdapterPeople() {
		return adapterPeople;
	}

	public TagAdapter getAdapterInterests() {
		return adapterInterests;
	}


	//endregion
}
