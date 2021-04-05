/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import it.keybiz.lbsapp.corporate.R;
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
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.ProfileContactToSend;
import it.keybiz.lbsapp.corporate.models.UserGenericLocation;
import it.keybiz.lbsapp.corporate.models.enums.SearchTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.ComputeAndPopulateHandlerThread;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InnerCircleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InnerCircleFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, Handler.Callback,
		SearchHelper.OnQuerySubmitted, InviteHelper.OnInviteActionListener,
		CirclesAdapter.OnInnerCircleActionListener, LoadMoreResponseHandlerTask.OnDataLoadedListener,
		UserWithLocationAdapter.NearMeItemInteractionListener {

	public static final String LOG_TAG = InnerCircleFragment.class.getCanonicalName();

	private boolean showCircles = true, showContacts = false, showNearMe = false;

	private String userId, userName, userAvatar;
	private String query;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private View progress, progressNearMe;
	private TextView progrMessage, progrMessageNearMe;

	private RecyclerView circlesRv, contactsRv, nearMeRv;
	private TextView noResult, noResultContacts, noResultNearMe;
	private View circlesTab, contactsTab, nearMeTab, circlesLayout, contactsLayout, nearMeLayout;
	private View geoLocationLayout;
	private TextView geoLocationMessage;

	private EditText searchBox;

	private boolean isUser;

	// CIRCLES
	private List<HLCircle> circlesListToShow = new ArrayList<>();
	private Map<String, HLCircle> circlesListLoaded = new ConcurrentHashMap<>();
	private CirclesAdapter circlesAdapter;
	private LinearLayoutManager circlesLlm;

	private SwipeRefreshLayout srl, srlContacts, srlNearMe;

	private boolean fromLoadMore;
	private int newItemsCount;


	// CONTACTS
	private List<ProfileContactToSend> contactsToShow = new ArrayList<>();
	private List<ProfileContactToSend> contactsRetrieved = new ArrayList<>();
	private LongSparseArray<ProfileContactToSend> contactsTask = new LongSparseArray<>();
	private ContactsAdapter contactsAdapter;
	private LinearLayoutManager contactsLlm;
	private boolean contactsLoaded;
	private boolean contactsInterrupted;

	private Integer scrollPositionContacts, scrollPositionCircles, scrollPositionNearMe;

	private InviteHelper inviteHelper;

	private AsyncTask<JSONArray, Void, List<ProfileContactToSend>> handleContactsTask;
	private AsyncTask<Void, Void, Void> getContactTask;
	private AsyncTask<List<ProfileContactToSend>, Void, Void> getPicturesTask;

	private SearchHelper mSearchHelper;


	// NEAR ME
	private List<UserGenericLocation> nearMeListToShow = new ArrayList<>();
	private Map<String, UserGenericLocation> nearMeLoaded = new ConcurrentHashMap<>();
	private UserWithLocationAdapter nearMeAdapter;
	private LinearLayoutManager nearMeLlm;


	private OnLocationUpdateActionsListener mListener;


	public InnerCircleFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment InnerCircleFragment.
	 */
	public static InnerCircleFragment newInstance(String userId, String userName, String userAvatar,
												  boolean switchToContacts) {
		InnerCircleFragment fragment = new InnerCircleFragment();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, userId);
		args.putString(Constants.EXTRA_PARAM_2, userName);
		args.putString(Constants.EXTRA_PARAM_3, userAvatar);
		args.putBoolean(Constants.EXTRA_PARAM_4, switchToContacts);
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
		View view = inflater.inflate(R.layout.fragment_profile_inner_circle, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);


		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		configureResponseReceiver();

		if (inviteHelper != null)
			inviteHelper.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_INNER_CIRCLE);

		triggerBackgroundOperations();

		setLayout();
	}

	@Override
	public void onPause() {

		onSaveInstanceState(new Bundle());

		scrollPositionContacts = contactsLlm.findFirstCompletelyVisibleItemPosition();
		scrollPositionCircles = circlesLlm.findFirstCompletelyVisibleItemPosition();

		if (handleContactsTask != null && !handleContactsTask.isCancelled())
			handleContactsTask.cancel(true);
		if (getContactTask != null && !getContactTask.isCancelled())
			getContactTask.cancel(true);
		if (getPicturesTask != null && !getPicturesTask.isCancelled())
			getPicturesTask.cancel(true);

		contactsInterrupted = true;
		progress.setVisibility(View.GONE);

		if (inviteHelper != null)
			inviteHelper.onPause();

		Utils.closeKeyboard(searchBox);

		super.onPause();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		circlesLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		contactsLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		nearMeLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);

		if (inviteHelper == null)
			inviteHelper = new InviteHelper(getContext(), this);

		contactsAdapter = new ContactsAdapter(contactsToShow, inviteHelper);
		contactsAdapter.setHasStableIds(true);

		circlesAdapter = new CirclesAdapter(circlesListToShow, this);
		circlesAdapter.setHasStableIds(true);

		nearMeAdapter = new UserWithLocationAdapter(new UserGenericLocationDiffCallback(), this);
		nearMeAdapter.setHasStableIds(true);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.profile_tab_inner_circle:

				scrollPositionContacts = contactsLlm.findFirstCompletelyVisibleItemPosition();
				scrollPositionCircles = circlesLlm.findFirstCompletelyVisibleItemPosition();

				showCircles = true;
				showNearMe = false;
				showContacts = false;
				setLayout();
				break;

			case R.id.profile_tab_contacts:
				showCircles = false;
				showContacts = true;
				showNearMe = false;

				if (!callForContacts()) {
					Utils.askRequiredPermissionForFragment(
							this,
							Manifest.permission.READ_CONTACTS,
							Constants.PERMISSIONS_REQUEST_CONTACTS
					);
				}

				setLayout();
				break;

			case R.id.profile_tab_near_me:
				showCircles = false;
				showContacts = false;
				showNearMe = true;

				if (mListener != null) {
					if (mListener.getService() != null && !mListener.isFragmentWaitingForServer()) {
						mListener.setFragmentWaitingForServer(true);
						mListener.getService().askForSingleUpdate();
					}
				}
				else callServer(CallType.NEAR_ME);

				setLayout();
				break;

			case R.id.back_arrow:
				if (getActivity() != null)
					getActivity().onBackPressed();
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnLocationUpdateActionsListener) {
			mListener = (OnLocationUpdateActionsListener) context;
			mListener.startLocationUpdateService();
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);
		if (context instanceof OnLocationUpdateActionsListener) {
			mListener = (OnLocationUpdateActionsListener) context;
			mListener.startLocationUpdateService();
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(Constants.EXTRA_PARAM_1, userId);
		outState.putString(Constants.EXTRA_PARAM_2, userName);
		outState.putString(Constants.EXTRA_PARAM_3, userAvatar);
		outState.putBoolean(Constants.EXTRA_PARAM_4, showContacts);
		outState.putString(Constants.EXTRA_PARAM_5, query);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				userId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				userName = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				userAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				showContacts = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_4, false);

			if (Utils.isStringValid(userId) && RealmObject.isValid(mUser))
				isUser = userId.equals(mUser.getId());
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

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLES_PROFILE:
				Utils.setRefreshingForSwipeLayout(srl, false);
				if (responseObject == null || responseObject.length() == 0) {
					newItemsCount = 0;
					return;
				}

				newItemsCount = responseObject.length();

				if (fromLoadMore) {
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.CIRCLES,
							null, null).execute(responseObject);
				}
				else handleCircleResponse(responseObject);

				break;

			case Constants.SERVER_OP_MATCH_USER_HL:
				handleContactsTask = new HandleContactsTask(contactsRetrieved, contactsAdapter).execute(responseObject);
				break;

			case Constants.SERVER_OP_INVITE_EMAIL:
				Toast.makeText(getContext(), R.string.invite_success_email, Toast.LENGTH_SHORT).show();
				break;

			case Constants.SERVER_OP_GET_NEAR_ME:
//				if (mListener != null)
//					mListener.setFragmentWaitingForServer(false);
				new PopulateNearMeThread("populateNearMe", this, responseObject).start();
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLES_PROFILE:
			case Constants.SERVER_OP_GET_NEAR_ME:

				if (operationId == Constants.SERVER_OP_GET_NEAR_ME) {
					progressNearMe.setVisibility(View.GONE);
					Utils.setRefreshingForSwipeLayout(srlNearMe, false);

//					if (mListener != null)
//						mListener.setFragmentWaitingForServer(false);
				}
				else
					Utils.setRefreshingForSwipeLayout(srl, false);

				activityListener.showAlert(R.string.error_generic_update);
				break;

			case Constants.SERVER_OP_MATCH_USER_HL:
				if (Utils.isContextValid(getActivity())) {
					getActivity().runOnUiThread(
							() -> progress.setVisibility(View.GONE)
					);
				}
				break;

			case Constants.SERVER_OP_INVITE_EMAIL:
				activityListener.showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		if (Utils.isContextValid(getActivity())) {
			getActivity().runOnUiThread(
					() -> {
						Utils.setRefreshingForSwipeLayout(srl, false);
						Utils.setRefreshingForSwipeLayout(srlNearMe, false);
						Utils.setRefreshingForSwipeLayout(srlContacts, false);

						if (operationId == Constants.SERVER_OP_MATCH_USER_HL)
							progress.setVisibility(View.GONE);
						else if (operationId == Constants.SERVER_OP_GET_NEAR_ME)
							progressNearMe.setVisibility(View.GONE);
					}
			);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == Constants.PERMISSIONS_REQUEST_CONTACTS) {
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// starts contacts handling
				if (getContactTask == null) {
					if (contactsRetrieved == null)
						contactsRetrieved = new ArrayList<>();
					getContactTask = new GetContactsTask().execute();
				}
			}
		}
		else if (requestCode == Constants.PERMISSIONS_REQUEST_LOCATION) {
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (mListener != null) {
					setAndShowNearMeProgress();
					mListener.startLocationUpdateService();

					if (mListener.getService() != null && !mListener.isFragmentWaitingForServer()) {
						mListener.setFragmentWaitingForServer(true);
						mListener.getService().askForSingleUpdate();
					}
				}
				else callServer(CallType.NEAR_ME);

				setLayout();
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
	public void resetFromLoadMore() {
		fromLoadMore = false;
	}

	@Override
	public int getLastPageId() {
		if (circlesListLoaded == null)
			circlesListLoaded = new HashMap<>();

		return (circlesListLoaded.size() / Constants.PAGINATION_AMOUNT);
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}

	//endregion



	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		configureToolbar(toolbar);

		view.findViewById(R.id.focus_catcher).requestFocus();

		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
		if (searchBox != null)
			searchBox.setHint(R.string.profile_search_box_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		configureProgress(view);

		circlesRv = view.findViewById(R.id.inner_circle_page_ic);
		circlesRv.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				callServer(CallType.CIRCLES);
				fromLoadMore = true;
			}
		});
		contactsRv = view.findViewById(R.id.inner_circle_page_contacts);
		nearMeRv = view.findViewById(R.id.inner_circle_page_near_me);
		noResult = view.findViewById(R.id.no_result);
		noResultContacts = view.findViewById(R.id.no_result_contacts);
		noResultNearMe = view.findViewById(R.id.no_result_near_me);

		circlesLayout = view.findViewById(R.id.inner_circle_page_circles_layout);
		srl = Utils.getGenericSwipeLayout(view, () -> {
			scrollPositionCircles = null;
			Utils.setRefreshingForSwipeLayout(srl, true);
			callServer(CallType.CIRCLES);
		});

		contactsLayout = view.findViewById(R.id.inner_circle_page_contacts_layout);
		srlContacts = view.findViewById(R.id.swipe_refresh_layout_contacts);
		srlContacts.setDistanceToTriggerSync(200);
		srlContacts.setColorSchemeResources(R.color.colorAccent);
		srlContacts.setOnRefreshListener(() -> {
			if (mayRequestContacts()) {
				scrollPositionContacts = null;
				Utils.setRefreshingForSwipeLayout(srlContacts, true);

				// starts contacts handling
				if (getContactTask == null || getContactTask.getStatus() == AsyncTask.Status.FINISHED) {
					if (contactsRetrieved == null)
						contactsRetrieved = new ArrayList<>();
					else
						contactsRetrieved.clear();

					getContactTask = new GetContactsTask().execute();
				}
			}
		});

		nearMeLayout = view.findViewById(R.id.inner_circle_page_near_me_layout);
		srlNearMe = view.findViewById(R.id.swipe_refresh_layout_near_me);
		srlNearMe.setDistanceToTriggerSync(200);
		srlNearMe.setColorSchemeResources(R.color.colorAccent);
		srlNearMe.setOnRefreshListener(() -> {
			scrollPositionNearMe = null;
			Utils.setRefreshingForSwipeLayout(srlNearMe, true);

			if (mListener != null) {
				if (mListener.getService() != null && !mListener.isFragmentWaitingForServer()) {
					mListener.setFragmentWaitingForServer(true);
					mListener.getService().askForSingleUpdate();
				}
			}
			else callServer(CallType.NEAR_ME);
		});

		circlesTab = view.findViewById(R.id.profile_tab_inner_circle);
		circlesTab.setOnClickListener(this);

		contactsTab = view.findViewById(R.id.profile_tab_contacts);
		contactsTab.setOnClickListener(this);
		contactsTab.setVisibility(isUser ? View.VISIBLE : View.GONE);

		nearMeTab = view.findViewById(R.id.profile_tab_near_me);
		nearMeTab.setOnClickListener(this);
		nearMeTab.setVisibility(isUser ? View.VISIBLE : View.GONE);

		geoLocationLayout = view.findViewById(R.id.enable_location_layout);
		geoLocationLayout.findViewById(R.id.enable_btn).setOnClickListener(
				it -> {
					if (!SharedPrefsUtils.hasLocationBackgroundBeenGranted(getActivity()) && !hasLocationPermission()) {
						SharedPrefsUtils.setLocationBackgroundGranted(it.getContext(), true);

						Utils.askRequiredPermissionForFragment(this, Manifest.permission.ACCESS_FINE_LOCATION, Constants.PERMISSIONS_REQUEST_LOCATION);
					}
					else if (!SharedPrefsUtils.hasLocationBackgroundBeenGranted(getActivity())) {
						SharedPrefsUtils.setLocationBackgroundGranted(it.getContext(), true);

						if (mListener != null) {
							setAndShowNearMeProgress();
							mListener.startLocationUpdateService();

							if (mListener.getService() != null && !mListener.isFragmentWaitingForServer()) {
								mListener.setFragmentWaitingForServer(true);
								mListener.getService().askForSingleUpdate();
							}
						}
						else callServer(CallType.NEAR_ME);

						setLayout();
					}
					else if (!hasLocationPermission())
						Utils.askRequiredPermissionForFragment(this, Manifest.permission.ACCESS_FINE_LOCATION, Constants.PERMISSIONS_REQUEST_LOCATION);
				}
		);
		geoLocationMessage = view.findViewById(R.id.enable_loc_message);

	}

	private void configureProgress(View view) {
		if (view != null) {
			progress = view.findViewById(R.id.progress_contacts);
			progrMessage = progress.findViewById(R.id.progress_message);
			progressNearMe = view.findViewById(R.id.progress_near_me);
			progrMessageNearMe = progressNearMe.findViewById(R.id.progress_message);
		}
	}

	public void setAndShowNearMeProgress() {
		progrMessageNearMe.setText(R.string.progress_message_near_me);
		progressNearMe.setVisibility(View.VISIBLE);
	}

	public void hideNearMeProgress(boolean error) {
		progressNearMe.setVisibility(View.GONE);

		if (error)
			activityListener.showGenericError();
	}

	@Override
	protected void setLayout() {
		toolbarTitle.setText(R.string.profile_card_network);

		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);

		contactsTab.setSelected(showContacts);
		circlesTab.setSelected(showCircles);
		nearMeTab.setSelected(showNearMe);

		circlesLayout.setVisibility(showCircles ? View.VISIBLE : View.GONE);
		contactsLayout.setVisibility(showContacts ? View.VISIBLE : View.GONE);
		nearMeLayout.setVisibility(showNearMe ? View.VISIBLE : View.GONE);

		if (Utils.isStringValid(query))
			searchBox.setText(query);

		if (showContacts) {
			progrMessage.setText(R.string.initializing_query);
			progress.setVisibility(!contactsInterrupted && !contactsLoaded ? View.VISIBLE : View.GONE);

			if (contactsLoaded && contactsRetrieved != null && !contactsRetrieved.isEmpty()) {
				noResultContacts.setVisibility(View.GONE);
				contactsRv.setLayoutManager(contactsLlm);
				contactsRv.setAdapter(contactsAdapter);

				if (searchBox.getText().length() > 0) {
					onQueryReceived(searchBox.getText().toString());
					return;
				}

				if (contactsToShow == null)
					contactsToShow = new ArrayList<>();
				else
					contactsToShow.clear();

				contactsToShow.addAll(contactsRetrieved);
				Collections.sort(contactsToShow);
				contactsAdapter.notifyDataSetChanged();
			}
			else
				noResultContacts.setVisibility(View.VISIBLE);
		}
		else if (showCircles) {
			circlesRv.setLayoutManager(circlesLlm);
			circlesRv.setAdapter(circlesAdapter);
            setData(false);
		}
		else if (showNearMe) {
			nearMeRv.setLayoutManager(nearMeLlm);
			nearMeRv.setAdapter(nearMeAdapter);

			if (hasAllPermissionsNeededForLocation()) {
				nearMeRv.setVisibility(View.VISIBLE);
				geoLocationLayout.setVisibility(View.GONE);
				handleNearMeData();
			}
			else {
				noResultNearMe.setVisibility(View.GONE);
				nearMeRv.setVisibility(View.GONE);
				geoLocationLayout.setVisibility(View.VISIBLE);

				if (!SharedPrefsUtils.hasLocationBackgroundBeenGranted(getActivity()) && !hasLocationPermission())
					geoLocationMessage.setText(R.string.ic_geolocation_prompt_none);
				else if (!SharedPrefsUtils.hasLocationBackgroundBeenGranted(getActivity()))
					geoLocationMessage.setText(R.string.ic_geolocation_prompt_bckgr);
				else if (!hasLocationPermission())
					geoLocationMessage.setText(R.string.ic_geolocation_prompt_perm);
			}
		}

		if (scrollPositionContacts != null && scrollPositionContacts > -1 && contactsLlm != null)
			contactsLlm.scrollToPosition(scrollPositionContacts);
		if (scrollPositionCircles != null && scrollPositionCircles > -1 && circlesLlm != null)
			circlesLlm.scrollToPosition(scrollPositionCircles);
		if (scrollPositionNearMe != null && scrollPositionNearMe > -1 && nearMeLlm != null)
			nearMeLlm.scrollToPosition(scrollPositionNearMe);
	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		this.query = query;

		if (!query.isEmpty()) {
			if (showContacts) {
				if (contactsRetrieved != null && !contactsRetrieved.isEmpty()) {
					contactsToShow.clear();
					Collections.sort(contactsRetrieved);
					contactsToShow.addAll(Stream.of(contactsRetrieved)
							.filter(contact -> contact.getName()
									.toLowerCase()
									.contains(query.toLowerCase()))
							.collect(Collectors.toList()));

					contactsAdapter.notifyDataSetChanged();

					if (!contactsToShow.isEmpty()) {
						contactsRv.setVisibility(View.VISIBLE);
						noResultContacts.setVisibility(View.GONE);
					}
					else {
						contactsRv.setVisibility(View.GONE);
						noResultContacts.setVisibility(View.VISIBLE);
					}
				}
			} else if (showCircles) {
				profileActivityListener.showSearchFragment(query, SearchTypeEnum.INNER_CIRCLE, userId,
						userName, userAvatar);
				searchBox.setText("");
			}
			else if (showNearMe) {
				if (nearMeLoaded != null && !nearMeLoaded.isEmpty()) {
					nearMeListToShow.clear();
					List<UserGenericLocation> tmp = new ArrayList<>(nearMeLoaded.values());
					Collections.sort(tmp);
					nearMeListToShow.addAll(Stream.of(tmp).filter(
							user -> user.getName().toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList())
					);
					nearMeAdapter.submitList(nearMeListToShow);

					if (!nearMeListToShow.isEmpty()) {
						nearMeRv.setVisibility(View.VISIBLE);
						noResultNearMe.setVisibility(View.GONE);
					}
					else {
						nearMeRv.setVisibility(View.GONE);
						noResultNearMe.setVisibility(View.VISIBLE);
					}
				}
			}
		}
		else {
			if (showContacts) {
				if (contactsToShow != null && contactsRetrieved != null) {
					contactsToShow.clear();
					contactsToShow.addAll(contactsRetrieved);
					contactsAdapter.notifyDataSetChanged();

					if (!contactsToShow.isEmpty()) {
						contactsRv.setVisibility(View.VISIBLE);
						noResultContacts.setVisibility(View.GONE);
					}
					else {
						contactsRv.setVisibility(View.GONE);
						noResultContacts.setVisibility(View.VISIBLE);
					}
				}
			} else if (showNearMe) {
				handleNearMeData();
			}
		}
	}

	@Override
	public Fragment getFragment() {
		return this;
	}

	private void configureToolbar(Toolbar toolbar) {
		if (toolbar != null) {
			toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
			toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
			profilePicture = toolbar.findViewById(R.id.profile_picture);
		}
	}


	@Override
	public void sendEmail(String email) {
		Object[] result = null;

		try {
			result = HLServerCalls.inviteWithEmail(mUser.getId(), email);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
		}
	}

	@Override
	public String getUserName() {
		return mUser.getUserCompleteName();
	}

	@Override
	public void goToViewMore(@NonNull String circleName) {
		profileActivityListener.showCircleViewMoreFragment(circleName, userId, userName, userAvatar);
	}

	@Override
	public void goToProfile(@Nullable ProfileHelper.ProfileType type, @NonNull String userId) {
		if (userId.equals(mUser.getId()) && Utils.isContextValid(getActivity())) {
			Intent intent = new Intent(getContext(), HomeActivity.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
			startActivity(intent);
			getActivity().finish();
		}
		else {
			profileActivityListener.showProfileCardFragment(type, userId);
		}
	}

	@Override
	public HLUser getUser() {
		return mUser;
	}

	/**
	 * Triggers two different tasks:
	 * <p>
	 *      - calls server for user's circles to display in the first view.
	 *      <p>
	 *      - calls server for users near me (only if permission already granted)
	 *      <p>
	 *      - starts contacts handling first retrieving Cursor with contacts data and then handling
	 *      server's matching operation (only if permission already granted).
	 */
	private void triggerBackgroundOperations() {
		callServer(CallType.CIRCLES);

		if (mListener != null) {
			if (mListener.getService() != null && !mListener.isFragmentWaitingForServer()) {
				mListener.setFragmentWaitingForServer(true);
				mListener.getService().askForSingleUpdate();
			}
		}
		else callServer(CallType.NEAR_ME);

		callForContacts();
	}

	public enum CallType { CIRCLES, NEAR_ME }
	public void callServer(CallType type) {
		Object[] result = null;

		if (type == CallType.CIRCLES) {
			Utils.setRefreshingForSwipeLayout(srl, true);
			try {
				result = HLServerCalls.getCirclesForProfile(userId, getLastPageId() + 1);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		else if (type == CallType.NEAR_ME) {
			if (hasAllPermissionsNeededForLocation()) {
				// only with all permissions in order makes sense to call for near me: it's the only way the server has stored at least one position
				Utils.setRefreshingForSwipeLayout(srlNearMe, true);

				try {
					result = HLServerCalls.getNearMeForProfile(userId);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private boolean callForContacts() {
		if (hasContactsPermission()) {
			if (isUser && (contactsRetrieved == null || contactsRetrieved.isEmpty())) {
				// starts contacts handling
				if (getContactTask == null) {
					if (contactsRetrieved == null)
						contactsRetrieved = new ArrayList<>();
					getContactTask = new GetContactsTask().execute();
				}
			}
			return true;
		} else return false;
	}


	private void handleCircleResponse(final JSONArray response) {
		realm.executeTransaction(realm -> {
			HLCircle circle;
			JSONObject circles = response.optJSONObject(0);
			if (circles != null) {
				if (mUser.getCircleObjects() == null)
					mUser.setCircleObjects(new RealmList<>());
				else
					mUser.getCircleObjects().clear();

				Iterator<String> iter = circles.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					circle = new HLCircle().deserializeToClass(circles.optJSONObject(key));
//						circle.setNameToDisplay(key);

					circlesListLoaded.put(circle.getName(), circle);
					mUser.getCircleObjects().add(circle);
				}

				mUser.updateFilters();

				setLayout();
			}
		});
	}

	private void setData(boolean background) {
		if (circlesListToShow == null)
			circlesListToShow = new ArrayList<>();
		else
			circlesListToShow.clear();

		if (circlesListLoaded.isEmpty()) {
			noResult.setVisibility(View.VISIBLE);
			circlesRv.setVisibility(View.GONE);
		}
		else {
			noResult.setVisibility(View.GONE);
			circlesRv.setVisibility(View.VISIBLE);

			List<HLCircle> circles = new ArrayList<>(circlesListLoaded.values());
			Collections.sort(circles, HLCircle.CircleSortOrderComparator);
			circlesListToShow.addAll(circles);

			if (!background)
				circlesAdapter.notifyDataSetChanged();
		}
	}

	private void handleNearMeData() {
		if (nearMeListToShow == null)
			nearMeListToShow = new ArrayList<>();
		else
			nearMeListToShow.clear();

		if (nearMeLoaded.isEmpty()) {
			noResultNearMe.setVisibility(hasAllPermissionsNeededForLocation() ? View.VISIBLE : View.GONE);
			nearMeRv.setVisibility(View.GONE);
		}
		else {
			noResultNearMe.setVisibility(View.GONE);
			nearMeRv.setVisibility(hasAllPermissionsNeededForLocation() ? View.VISIBLE : View.GONE);

			List<UserGenericLocation> nearMe = new ArrayList<>(nearMeLoaded.values());
			Collections.sort(nearMe);
			nearMeListToShow.addAll(nearMe);

			nearMeAdapter.submitList(nearMeListToShow);
		}
	}

	private void getContacts2() {
		long start = System.currentTimeMillis();

		String[] projection = {
				ContactsContract.Data.MIMETYPE,
				ContactsContract.Data.CONTACT_ID,
				ContactsContract.Contacts.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Contactables.DATA,
				ContactsContract.CommonDataKinds.Contactables.TYPE,
		};
		String selection = ContactsContract.Data.MIMETYPE + " in (?, ?)";
		String[] selectionArgs = {
				ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
				ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
		};
		String sortOrder = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;

		Uri uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
		// we could also use Uri uri = ContactsContract.Data.CONTENT_URI;

		// ok, let's work...
		final Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);

		int counter = 0;
		if (cursor != null) {

			final int mimeTypeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
			final int idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
			final int nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			final int dataIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.DATA);
			final int typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.TYPE);

			while (cursor.moveToNext()) {
				final long id = cursor.getLong(idIdx);
				ProfileContactToSend addressBookContact = contactsTask.get(id);
				if (addressBookContact == null) {
					addressBookContact = new ProfileContactToSend(getContext(), contactsAdapter,
							id, cursor.getString(nameIdx));
					contactsTask.put(id, addressBookContact);
				}

				int type = cursor.getInt(typeIdx);
				String data = cursor.getString(dataIdx);
				String mimeType = cursor.getString(mimeTypeIdx);
				if (mimeType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
					// mimeType == ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
					addressBookContact.addEmail(data);
				} else {
					// mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
					addressBookContact.addPhone(data);
				}
			}
			long ms = System.currentTimeMillis() - start;
			cursor.close();
		}

		if (contactsTask != null && contactsTask.size() > 0) {
			for (int i = 0; i < contactsTask.size(); i++) {
				long id = contactsTask.keyAt(i);
				ProfileContactToSend c = contactsTask.get(id);
				if (c != null && (c.hasEmails() || c.hasPhones()))
					contactsRetrieved.add(c);
			}
		}
		else {
			getActivity().runOnUiThread(
					() -> progress.setVisibility(View.GONE)
			);
			if (getContactTask != null && !getContactTask.isCancelled())
				getContactTask.cancel(true);
			contactsInterrupted = true;
			return;
		}

		// call to server
		Object[] result = null;
		try {
			result = HLServerCalls.matchContactsWithHLUsers(userId, contactsRetrieved);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}

	}


	private boolean mayRequestContacts() {
		if (hasContactsPermission()) return true;

		Utils.askRequiredPermissionForFragment(this, android.Manifest.permission.READ_CONTACTS, Constants.PERMISSIONS_REQUEST_CONTACTS);
		return false;
	}

	private boolean hasContactsPermission() {
		return Utils.hasApplicationPermission(getActivity(), Manifest.permission.READ_CONTACTS);
	}

	private boolean hasLocationPermission() {
		return Utils.hasApplicationPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
	}

	private boolean hasAllPermissionsNeededForLocation() {
		return hasLocationPermission() && SharedPrefsUtils.hasLocationBackgroundBeenGranted(getActivity());
	}

	//endregion


	//region == CUSTOM INNER CLASSES ==

	//region ++ AsyncTasks ++

	private class GetContactsTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// starts progress
			if (showContacts) {
				if (srlContacts.isRefreshing())
					srlContacts.setRefreshing(false);
				progress.setVisibility(View.VISIBLE);
			}
			progrMessage.setText(R.string.initializing_query);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			getContacts2();
			return null;
		}
	}

	private class HandleContactsTask extends AsyncTask<JSONArray, Void, List<ProfileContactToSend>> {

		private List<ProfileContactToSend> contacts;
		private ContactsAdapter adapter;

		HandleContactsTask(List<ProfileContactToSend> contacts, ContactsAdapter adapter) {
			this.contacts = contacts;
			this.adapter = adapter;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			progrMessage.setText(R.string.contacting_server);
		}

		@Override
		protected List<ProfileContactToSend> doInBackground(JSONArray... arrays) {
			try {
				if (arrays[0] != null && arrays[0].length() > 0) {
					JSONArray indexes = arrays[0].optJSONObject(0).optJSONArray("indexes");

					if (indexes != null && indexes.length() > 0 &&
							contacts != null && !contacts.isEmpty() &&
							contacts.size() >= indexes.length()) {
						for (int i = indexes.length() - 1; i >=0 ; i--) {
							contacts.remove(indexes.getInt(i));
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return contacts;
		}

		@Override
		protected void onPostExecute(List<ProfileContactToSend> contacts) {
			super.onPostExecute(contacts);

			if (contactsToShow == null)
				contactsToShow = new ArrayList<>();
			else
				contactsToShow.clear();

			Collections.sort(contacts);
			contactsToShow.addAll(contacts);

			if (Utils.isContextValid(getActivity()) && showContacts) {
				contactsRv.setLayoutManager(contactsLlm);
				contactsRv.setAdapter(adapter);
				adapter.notifyDataSetChanged();

				noResultContacts.setVisibility(contactsToShow.isEmpty() ? View.VISIBLE : View.GONE);
				contactsRv.setVisibility(contactsToShow.isEmpty() ? View.GONE : View.VISIBLE);
			}

			contactsLoaded = true;
			contactsInterrupted = false;
			progress.setVisibility(View.GONE);

			getPicturesTask = new GetPictureTask(adapter, getContext()).execute(contacts);

			getContactTask = null;
		}
	}

	static class GetPictureTask extends AsyncTask<List<ProfileContactToSend>, Void, Void> {

		private ContactsAdapter mAdapter;
		private WeakReference<Context> context;

		GetPictureTask(ContactsAdapter mAdapter, Context context) {
			this.mAdapter = mAdapter;
			this.context = new WeakReference<>(context);
		}

		@Override
		protected Void doInBackground(List<ProfileContactToSend>[] lists) {
			List<ProfileContactToSend> list = lists[0];
			if (list != null && !list.isEmpty()) {
				Iterator<ProfileContactToSend> iter = list.iterator();
				// solving ConcurrentException
				while (iter.hasNext()) {
					ProfileContactToSend c = iter.next();
					if (c != null) {
						c.setPhoto(context.get());
					}
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			if (mAdapter != null)
				mAdapter.notifyDataSetChanged();
		}
	}

	//endregion


	private class PopulateNearMeThread extends ComputeAndPopulateHandlerThread {

		private JSONArray response;

		PopulateNearMeThread(String name, InnerCircleFragment fragment, JSONArray response) {
			super(name, fragment);
			this.response = response;
		}

		@Override
		protected void onLooperPrepared() {
			super.onLooperPrepared();

			if (getHandler() != null) {
				getHandler().post(() -> {
					if (nearMeListToShow == null) nearMeListToShow = new ArrayList<>();
					else nearMeListToShow.clear();

					if (response != null && response.length() > 0) {
						for (int i = 0; i < response.length(); i++) {
							UserGenericLocation user = null;
							try {
								user = UserGenericLocation.getUserWithLocation(new JSONObject(response.optString(i)));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							if (user != null && Utils.isStringValid(user.getUserID()))
								nearMeLoaded.put(user.getUserID(), user);
						}
					}

					nearMeListToShow.addAll(nearMeLoaded.values());

					Collections.sort(nearMeListToShow);
				});

				exitOps();
			}
		}
	}


	@Override
	public boolean handleMessage(Message msg) {
		getActivity().runOnUiThread(() -> {
			if (nearMeListToShow != null && !nearMeListToShow.isEmpty() && nearMeAdapter != null) {
				nearMeAdapter.submitList(nearMeListToShow);
			}

			Utils.setRefreshingForSwipeLayout(srlNearMe, false);
			progressNearMe.setVisibility(View.GONE);

			setLayout();
		});

		return true;
	}

	//endregion


	//region == Location Updates Listener ==

	interface OnLocationUpdateActionsListener {

		void startLocationUpdateService();
		LocationUpdateService getService();
		boolean hasServerAtLeastOnePosition();
		void startExecutorCheck();
		boolean isFragmentWaitingForServer();
		void setFragmentWaitingForServer(boolean waitingForServer);

	}


	//endregion

}