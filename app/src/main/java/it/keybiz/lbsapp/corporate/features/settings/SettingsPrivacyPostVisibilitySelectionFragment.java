/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
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
import it.keybiz.lbsapp.corporate.models.enums.PrivacyEntriesEnum;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;

/**
 * A simple {@link Fragment} subclass.
 * @author mbaldrighi on 3/8/2018.
 */
public class SettingsPrivacyPostVisibilitySelectionFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, SearchHelper.OnQuerySubmitted,
		BasicAdapterInteractionsListener, OnBackPressedListener {

	public static final String LOG_TAG = SettingsPrivacyPostVisibilitySelectionFragment.class.getCanonicalName();

	private SettingsPrivacySelectionFragment.PrivacySubItem item;
	private PrivacyPostVisibilityEnum mViewType;

	private static UnderlineSpan underlineSpan = new UnderlineSpan();

	private String query;

	private TextView title;
	private TextView selectAll, clearSelections;

	private EditText searchBox;

	private RecyclerView baseRecView;
	private LinearLayoutManager llm;
	private List<ObjectWithSelection> searchResults = new ArrayList<>();
	private List<ObjectWithSelection> searchResultsToShow = new ArrayList<>();
	private SettingsPostVisibilitySelectionAdapter selectionAdapter;

	private TextView noResult;

	private SwipeRefreshLayout srl;

	private boolean fromLoadMore = false;
	private int pageIdToCall = 1;           // forced to 1 because pagination has been disabled
	private int newItemsCount;

	private ArrayList<String> values = new ArrayList<>();

	private SearchHelper mSearchHelper;


	public SettingsPrivacyPostVisibilitySelectionFragment() {
		// Required empty public constructor
	}

	public static SettingsPrivacyPostVisibilitySelectionFragment newInstance(SettingsPrivacySelectionFragment.PrivacySubItem item,
	                                                                         PrivacyPostVisibilityEnum viewType) {
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, item);
		args.putSerializable(Constants.EXTRA_PARAM_2, viewType);
		SettingsPrivacyPostVisibilitySelectionFragment fragment = new SettingsPrivacyPostVisibilitySelectionFragment();
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

		View view = inflater.inflate(R.layout.fragment_settings_post_visib_selected, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() != null) {
			selectionAdapter = new SettingsPostVisibilitySelectionAdapter(searchResultsToShow, this,
					mViewType);
			selectionAdapter.setHasStableIds(true);

			llm = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
		}

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_PRIVACY_SELECTED_U_C);

		callServer(mType = CallType.GET);
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
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(null);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				item = (SettingsPrivacySelectionFragment.PrivacySubItem) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				mViewType = (PrivacyPostVisibilityEnum) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_2);
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_select_all:
				selectDeselectAll(true);
				break;

			case R.id.btn_clear_selections:
				selectDeselectAll(false);
				break;
		}
	}

	@Override
	public void onBackPressed() {
		if (item != null) {
			if (values != null && !values.isEmpty()) {
				item.setValues(values);
				callServer(mType = CallType.SET);
			}
			else if (Utils.isContextValid(getActivity())) {
				if (getActivity() instanceof SettingsActivity)
					((SettingsActivity) getActivity()).setBackListener(null);

				getActivity().onBackPressed();
			}
		}
		else {
			activityListener.showAlert(R.string.error_unknown);
			settingsActivityListener.closeScreen();
		}
	}

	@Override
	public void onItemClick(Object object) {
		if (object instanceof ObjectWithSelection) {
			ObjectWithSelection ows = (ObjectWithSelection) object;
			ows.setSelected(!ows.isSelected());
			selectionAdapter.notifyDataSetChanged();

			String id = null;
			if (mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_USERS)
				id = ows.getUserID();
			else if (mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES)
				id = ows.getName();

			if (Utils.isStringValid(id)) {
				if (ows.isSelected() && !values.contains(id))
					values.add(id);
				else if (!ows.isSelected())
					values.remove(id);
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
//				if (fromLoadMore)
//					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.SEARCH,
//							null, null).execute(responseObject);
//				else
				setData(responseObject, false);
				break;

			case Constants.SERVER_OP_SETTINGS_PRIVACY_POST_GET_SEL_CIRCLES:
			case Constants.SERVER_OP_SETTINGS_PRIVACY_POST_GET_SEL_USERS:
				setData(responseObject, false);
				break;

			case Constants.SERVER_OP_SETTINGS_PRIVACY_GET_SET:
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						if (item != null && PrivacyEntriesEnum.toEnum(item.getParentIndex()) == PrivacyEntriesEnum.POST_VISIBILITY) {
							if (mUser != null && mUser.getSettings() != null) {
								mUser.getSettings().setRawPostVisibility(item.getIndex());

								if (mUser.getSettings().getValues() == null)
									mUser.getSettings().setValues(new RealmList<String>());
								else
									mUser.getSettings().getValues().clear();
								mUser.getSettings().getValues().addAll(item.getValues());
							}
						}

						if (Utils.isContextValid(getActivity())) {
							FragmentManager manager = getActivity().getSupportFragmentManager();
							if (manager != null) {
								manager.popBackStack(SettingsInnerEntriesFragment.LOG_TAG, 0);
							}
						}
					}
				});
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
				return;

			case Constants.SERVER_OP_SETTINGS_IC_GET_SET_FEED:
				activityListener.showGenericError();
				break;

		}

		if (mType == CallType.SET)
			settingsActivityListener.closeScreen();
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
		activityListener.closeProgress();

		if (mType == CallType.SET)
			settingsActivityListener.closeScreen();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		this.query = query;

		if (searchResults != null && !searchResults.isEmpty()) {
			searchResultsToShow.clear();
			searchResultsToShow.addAll(Stream.of(searchResults).filter(new Predicate<ObjectWithSelection>() {
				@Override
				public boolean test(ObjectWithSelection obj) {
					return obj.getName().toLowerCase().contains(query.toLowerCase());
				}
			}).collect(Collectors.toList()));

			selectionAdapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		title = view.findViewById(R.id.title);

		selectAll = view.findViewById(R.id.btn_select_all);
		selectAll.setOnClickListener(this);
		clearSelections = view.findViewById(R.id.btn_clear_selections);
		clearSelections.setOnClickListener(this);

		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		searchBox.setText(query);

		noResult = view.findViewById(R.id.no_result);
		baseRecView = view.findViewById(R.id.selections_list);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);
				callServer(CallType.GET);
			}
		});
	}

	@Override
	protected void setLayout() {
		title.setText(mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES ?
				R.string.settings_privacy_pv_sel_circles : R.string.settings_privacy_pv_sel_users );

		Utils.applySpansToTextView(selectAll, R.string.action_select_all, underlineSpan);
		Utils.applySpansToTextView(clearSelections, R.string.action_clear_selection, underlineSpan);

		baseRecView.setLayoutManager(llm);
		baseRecView.setAdapter(selectionAdapter);

		searchBox.setText(query);
		searchBox.setHint(R.string.search_by_name);
	}

	private void setData(JSONArray response, boolean background) {
		if (pageIdToCall == 1 && (response == null || (response.length() == 0))) {
			baseRecView.setVisibility(View.GONE);
			noResult.setText(Utils.isStringValid(query) ?
					R.string.no_search_result : R.string.no_people_in_ic);
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
				ObjectWithSelection obj = new ObjectWithSelection().deserializeToClass(json);
				if (obj != null) {
					if (values != null && !values.isEmpty()) {
						String id = null;
						if (mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_USERS)
							id = obj.getUserID();
						else if (mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES)
							id = obj.getName();

						obj.setSelected(Utils.isStringValid(id) && values.contains(id));
					}
					searchResults.add(obj);
				}
			}

			searchResultsToShow.addAll(searchResults);
			if (!background)
				selectionAdapter.notifyDataSetChanged();
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}

	private enum CallType { GET, SET }
	private CallType mType;
	private void callServer(CallType type) {
		Object[] result = null;

		try {
			if (type == CallType.GET)
				result = HLServerCalls.getSettingsPostVisibilitySelection(mUser.getUserId(), mViewType);
			else if (type == CallType.SET && item != null)
				result = HLServerCalls.settingsOperationsOnPrivacy(mUser.getUserId(),
						item.getParentIndex(), item, SettingsPrivacySelectionFragment.CallType.SET);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}

	private void selectDeselectAll(boolean select) {
		if (values == null)
			values = new ArrayList<>();
		else
			values.clear();

		if (searchResults != null) {
			for (ObjectWithSelection ows : searchResults) {
				ows.setSelected(select);

				if (select && mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_USERS)
					values.add(ows.getUserID());
				else if (select && mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES)
					values.add(ows.getName());
			}

			selectionAdapter.notifyDataSetChanged();
		}
	}


	//region == Load more methods ==

	/*
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
	public int getLastPageId() {
		return pageIdToCall - 1;
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}
	*/

	//endregion


	//region == Class custom inner classes ==

	public class ObjectWithSelection {

		String userID;              // when object is HLUserGeneric
		String avatarURL;           // when object is HLUserGeneric

		String name;                // common to HLUserGeneric AND to Circle String
		@SerializedName("UIName")
		String nameToDisplay;       // ONLY Circle String
		boolean isSelected;         // common to HLUserGeneric AND to Circle String

		@Override
		public int hashCode() {
			if (Utils.isStringValid(userID))
				return userID.hashCode();
			else if (Utils.isStringValid(name))
				return name.hashCode();

			return super.hashCode();
		}

		ObjectWithSelection() {}

		ObjectWithSelection deserializeToClass(JSONObject json) {
			return new Gson().fromJson(json.toString(), ObjectWithSelection.class);
		}


		public boolean isSelected() {
			return isSelected;
		}
		public void setSelected(boolean selected) {
			isSelected = selected;
		}

		public String getUserID() {
			return userID;
		}

		public String getName() {
			return name;
		}

		public String getNameToDisplay() {
			return nameToDisplay;
		}

		public String getAvatarURL() {
			return avatarURL;
		}
	}

	//endregion

}
