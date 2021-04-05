/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline;

import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.realm.Realm;
import io.realm.RealmList;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnApplicationContextNeeded;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.connection.realTime.RealTimeCommunicationHelperKotlin;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.globalSearch.GlobalSearchActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.settings.SettingsActivity;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.FullScreenHelper;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.OnActionsResultFromBottom;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.PostBottomSheetHelper;
import it.keybiz.lbsapp.corporate.models.FeedFilter;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.enums.FeedFilterTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnTimelineFragmentInteractionListener}
 * interface.
 */
public class TimelineFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, RealTimeCommunicationListener,
		LoadMoreResponseHandlerTask.OnDataLoadedListener, OnBackPressedListener,
		OnMissingConnectionListener, OnActionsResultFromBottom, Handler.Callback {

	public enum FragmentUsageType { TIMELINE, SINGLE, DIARY, GLOBAL_SEARCH, GUEST }
	private FragmentUsageType usageType;

	private View mainView;

	private Toolbar toolbar;
	private View titleView;
	private View filterToggle;
	private View sortingSection, sortRecent, sortLoved, sortShuffle;
	private ImageView sortBtn;

	private Toolbar toolbarDiarySingle;

	private String userId;
	private String userName;
	private String userAvatar;
	private String postId;
	private String postListName;
	private String globalSearchQuery;

	public static final String LOG_TAG = TimelineFragment.class.getCanonicalName();

	private RecyclerView postListView;
	private InnerCircleFeedAdapter postAdapter;
	private LinearLayoutManager postLlm;
	private List<Post> posts = new ArrayList<>();
	private PagerSnapHelper psh;

	private OnTimelineFragmentInteractionListener mListener;
	private OnConfigChangedAction mConfigListener;

	private PostBottomSheetHelper bottomSheetHelper;


	private SwipeRefreshLayout srl;

	private boolean dataFromLoadMore = false;
	private int newItemsCount = 0;

	// field's purpose has changed: that's why the JSON key to be parsed is "canApplyFamilyFilter" [l. 625]
	private Boolean hasFamilyRelations = null;

	private View newPostToast;

	private LoadMoreScrollListener mScrollListener;

	private View feedFilter;
	private ViewGroup filterContainer;
	private boolean filterVisible = false;
	private List<FeedFilter> filters = new CopyOnWriteArrayList<>();
	private ArrayList<String> filtersString;
	private boolean isFilterAll;
	private BroadcastReceiver filterRcvr;
	private boolean filterRequested = false;

	private int tempSkip = 0;


	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TimelineFragment() {
	}

	@SuppressWarnings("unused")
	public static TimelineFragment newInstance(FragmentUsageType type, String userId, String name,
											   String avatar,
											   @Nullable String postId,
											   @Nullable String postListName,
											   @Nullable String globalSearchQuery) {
		TimelineFragment fragment = new TimelineFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, type);
		args.putString(Constants.EXTRA_PARAM_2, postId);
		args.putString(Constants.EXTRA_PARAM_3, postListName);
		args.putSerializable(Constants.EXTRA_PARAM_4, userId);
		args.putString(Constants.EXTRA_PARAM_5, name);
		args.putString(Constants.EXTRA_PARAM_6, avatar);
		args.putString(Constants.EXTRA_PARAM_7, globalSearchQuery);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		postAdapter = new InnerCircleFeedAdapter(this, posts, mListener);
		postAdapter.setHasStableIds(true);

		psh = new PagerSnapHelper();

		setRetainInstance(true);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		configurePostOptionsSheets();

		if (getActivity() instanceof HomeActivity) {
			((HomeActivity) getActivity()).setBackListener(this);

			if (((HomeActivity) getActivity()).getBottomSheetHelper() != null)
				((HomeActivity) getActivity()).getBottomSheetHelper().setPostActionsListener(this);

			// TODO: 9/13/2018    REMOVE if OKAY
//			((HomeActivity) getActivity()).setPostDeletedListener(this);

		}
		if (getActivity() instanceof GlobalSearchActivity) {
			((GlobalSearchActivity) getActivity()).setBackListener(this);

			if (((GlobalSearchActivity) getActivity()).getBottomSheetHelper() != null)
				((GlobalSearchActivity) getActivity()).getBottomSheetHelper().setPostActionsListener(this);
		}
		else if (getActivity() instanceof ProfileActivity) {
			((ProfileActivity) getActivity()).getFullScreenListener().setDiaryMode(usageType == FragmentUsageType.DIARY);
		}
		else if (getActivity() instanceof SettingsActivity) {
			((SettingsActivity) getActivity()).getFullScreenListener().setDiaryMode(usageType == FragmentUsageType.DIARY);
		}

		RealTimeCommunicationHelperKotlin.Companion.getInstance(getActivity()).setListener(this);

		configureResponseReceiver();

		postLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		postListView.setLayoutManager(postLlm);

		postListView.setAdapter(postAdapter);

		/*
		SensorManager sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(new HLOrientationSensorListener(),
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		*/


		filterRcvr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (Utils.isContextValid(context)) {
					populateFilters(realm, mUser);
					setFilterView(realm, mUser);
				}
			}
		};


	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		mainView = inflater.inflate(R.layout.home_fragment_timeline, container, false);

		onRestoreInstanceState(getArguments(), savedInstanceState);

		toolbar = mainView.findViewById(R.id.toolbar);
		toolbarDiarySingle = mainView.findViewById(R.id.toolbar_diary_single);
		configureToolbar(toolbar, toolbarDiarySingle);

		configureLayout(mainView);

		return mainView;
	}

	private void configureToolbar(Toolbar toolbar, Toolbar toolbar2) {

		sortBtn = toolbar.findViewById(R.id.sort);
		sortLoved = toolbar.findViewById(R.id.sort_loved);
		sortLoved.setOnClickListener(this);
		sortRecent = toolbar.findViewById(R.id.sort_date);
		sortRecent.setOnClickListener(this);
		sortShuffle = toolbar.findViewById(R.id.sort_shuffle);
		sortShuffle.setOnClickListener(this);

		titleView = toolbar.findViewById(R.id.logo_title);
		titleView.setOnClickListener(this);

		filterToggle = toolbar.findViewById(R.id.filter_toggle);
		sortingSection = toolbar.findViewById(R.id.buttons_container);

		toolbar.findViewById(R.id.sort_click_consumer).setOnClickListener(this);
		toolbar.findViewById(R.id.filter_click_consumer).setOnClickListener(this);

		toolbar2.findViewById(R.id.back_arrow).setOnClickListener(this);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		postListView.setItemViewCacheSize(0);
		postListView.setDrawingCacheEnabled(true);
		postListView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
		psh.attachToRecyclerView(postListView);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(getContext())
				.registerReceiver(filterRcvr, new IntentFilter(Constants.BROADCAST_CIRCLES_RETRIEVED));

		if (bottomSheetHelper != null)
			bottomSheetHelper.onResume();

		setLayout();

		if (usageType == FragmentUsageType.TIMELINE) {
			if (/*LBSLinkApp.refreshAllowed*/false) {
				Utils.setRefreshingForSwipeLayout(srl, true);

				callTimeline(false);
			} else
				setData(realm, false);
		}
		else if (usageType == FragmentUsageType.SINGLE) {
			setDataSingle();
			callSinglePost();
		}
		else if (usageType == FragmentUsageType.DIARY) {
			setDataPostList();
			callPostList(tempSkip);
		}
		else if (usageType == FragmentUsageType.GLOBAL_SEARCH) {
			setDataPostList();
			callPostListGlobal(tempSkip);
		}

		callFamilyCheck();
		callCircles();
	}

	@Override
	public void onPause() {
		super.onPause();

		saveLastPostSeen(false);

		if (bottomSheetHelper != null)
			bottomSheetHelper.onPause();

		try {
			if (Utils.isContextValid(getContext()))
//				getContext().unregisterReceiver(filterRcvr);
				LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(filterRcvr);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		postAdapter.cleanAdapterMediaControllers(true);
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroyView() {

		saveLastPostSeen(false);

		RealTimeCommunicationHelperKotlin.Companion.getInstance(getActivity()).setListener(null);

		super.onDestroyView();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnTimelineFragmentInteractionListener) {
			mListener = (OnTimelineFragmentInteractionListener) context;

			mListener.setLastPageID(1);
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnTimelineFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;

		if (Utils.isContextValid(getActivity())) {
			if (getActivity() instanceof HomeActivity)
				((HomeActivity) getActivity()).setBackListener(null);
			else if (getActivity() instanceof GlobalSearchActivity)
				((GlobalSearchActivity) getActivity()).setBackListener(null);
		}
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();

		if (feedFilter.getVisibility() == View.VISIBLE) {
			handleFilterToggle();
		}
		else {
			switch (id) {
				case R.id.sort_click_consumer:
					toggleSortSectionVisibility();
					break;
				case R.id.sort_loved:
				case R.id.sort_date:
				case R.id.sort_shuffle:
					handleSorting(view);
					break;

				case R.id.filter_click_consumer:
					handleFilterToggle();
					break;

				case R.id.back_arrow:
					onBackPressed();
					break;

				case R.id.toast_rt_new_post:
					if (mListener.getLastAdapterPosition() != null && mListener.getLastAdapterPosition() >= 10)
						postLlm.scrollToPosition(0);
					else
						postLlm.smoothScrollToPosition(postListView, null, 0);
					hideNewPostToast(true);
					break;

				case R.id.logo_title:
					if (mListener.getLastAdapterPosition() != null && mListener.getLastAdapterPosition() >= 10)
						postLlm.scrollToPosition(0);
					else
						postLlm.smoothScrollToPosition(postListView, null, 0);
					break;

			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			// caught when returning from InteractionsViewerActivity
			case Constants.RESULT_TIMELINE_INTERACTIONS_VIEW:
				LogUtils.d(LOG_TAG, "Back to timeline main screen");
				break;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		switch (newConfig.orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				if (mConfigListener != null) {

					if (mConfigListener instanceof FeedMemoryViewHolder) {
						int pos = ((FeedMemoryViewHolder) mConfigListener).getAdapterPosition();
						if (pos > -1) {
							switch (postAdapter.getItemViewType(pos)) {
								case InnerCircleFeedAdapter.TYPE_PICTURE:
								case InnerCircleFeedAdapter.TYPE_VIDEO:
									mConfigListener.onConfigChanged();
							}
						}
					}
				}
				break;
		}

		super.onConfigurationChanged(newConfig);
	}


	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putStringArrayList(Constants.EXTRA_PARAM_8, filtersString);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				usageType = (FragmentUsageType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				postId = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				postListName = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				userId = savedInstanceState.getString(Constants.EXTRA_PARAM_4);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_5))
				userName = savedInstanceState.getString(Constants.EXTRA_PARAM_5);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_6))
				userAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_6);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_7))
				globalSearchQuery = savedInstanceState.getString(Constants.EXTRA_PARAM_7);
		}
	}

	private void onRestoreInstanceState(Bundle arguments, Bundle savedInstanceState) {
		onRestoreInstanceState(arguments);

		if (savedInstanceState != null && savedInstanceState.containsKey(Constants.EXTRA_PARAM_8))
			filtersString = savedInstanceState.getStringArrayList(Constants.EXTRA_PARAM_8);

		if (filtersString != null && !filtersString.isEmpty()) {
			LBSLinkApp.refreshAllowed = !filtersString.containsAll(mUser.getSelectedFeedFilters()) ||
					!mUser.getSelectedFeedFilters().containsAll(filtersString);

			if (LBSLinkApp.refreshAllowed)
				filtersString = new ArrayList<>(mUser.getSelectedFeedFilters());
		}
	}

	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
	}


	@Override
	public void onBackPressed() {

		if (bottomSheetHelper == null || !bottomSheetHelper.closePostSheet()) {
			if (Utils.isContextValid(getActivity())) {
				if (getActivity() instanceof HomeActivity)
					((HomeActivity) getActivity()).setBackListener(null);
				else if (getActivity() instanceof GlobalSearchActivity)
					((GlobalSearchActivity) getActivity()).setBackListener(null);
				getActivity().onBackPressed();
			}
		}
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		Context context = getContext();
		HLPosts posts = HLPosts.getInstance();
		if (Utils.isContextValid(context)) {
			switch (operationId) {
				case Constants.SERVER_OP_GET_TIMELINE:
				case Constants.SERVER_OP_SEARCH_GLOBAL_TIMELINE:

					newItemsCount = responseObject != null ? responseObject.length() : 0;
					mScrollListener.setCanFetch(newItemsCount == Constants.PAGINATION_AMOUNT);

					if (usageType == FragmentUsageType.TIMELINE) {

						// if sorting order changed these operations need to be executed before
						if (selectedSortOrder != null) {

							realm.executeTransaction(realm -> mUser.getSettings().setOverriddenSortOrder(selectedSortOrder));

							mListener.setLastPageID(1);
							mListener.setLastAdapterPosition(0);
							saveLastPostSeen(true);

							toggleSortSectionVisibility();
						}


						if (dataFromLoadMore) {
							mListener.setLastPageID(mListener.getPageIdToCall());

							// TODO: 11/9/2017    restore if necessary
//							new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.POSTS, null, null)
//									.execute(responseObject);
						}

						LBSLinkApp.refreshAllowed = false;

						// handles filter visibility
						if (filterRequested) {
							handleFilterToggle();
							filterRequested = false;
						}

						// now Realm database can grow considerably.
						// newSession value depends on if MAIN feed has been refreshed
						boolean condition = operationId == Constants.SERVER_OP_GET_TIMELINE &&
								!dataFromLoadMore && mListener.getLastPageID() == 1;
						if (condition) {
							posts.cleanRealmPostsNewSession(realm);
							mListener.setLastPageID(1);
						}
						else posts.cleanRealmPosts(realm, getContext());

						dataFromLoadMore = false;
						selectedSortOrder = null;

						try {
							posts.setPosts(responseObject, realm, true);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						setData(realm, false);
					}
					else if (usageType == FragmentUsageType.DIARY ||
							usageType == FragmentUsageType.GUEST ||
							usageType == FragmentUsageType.GLOBAL_SEARCH
					) {

						if (dataFromLoadMore)
							mListener.setLastPageID(mListener.getPageIdToCall());

						int skip = mListener.getFeedSkip(realm, usageType, false);
						if ((responseObject == null || responseObject.length() == 0) && skip == 0) {
							final MaterialDialog dialog = DialogUtils.showGenericAlertCustomView(getContext(), R.layout.custom_dialog_folder_no_posts);
							if (dialog != null) {
								new Handler().postDelayed(() -> {
									dialog.dismiss();
									if (getActivity() != null)
										getActivity().onBackPressed();
								}, 2000);
							}

							return;
						}

						LBSLinkApp.refreshAllowed = false;

						posts.populatePostsForDiaryOrGlobalSearch(responseObject, realm, getLastPageId() == 1);
						setDataPostList();
					}
					break;

				case Constants.SERVER_OP_GET_SINGLE_POST:
					JSONObject json = responseObject.optJSONObject(0);
					if (json != null) {
						posts.setPost(json, realm, posts.isPostToBePersisted(postId));

						if (this.posts == null)
							this.posts = new ArrayList<>();
						this.posts.clear();

						this.posts.add(posts.getPost(postId));
						postAdapter.notifyDataSetChanged();
					}
					else if (Utils.isContextValid(getContext())) {
						final MaterialDialog dialog = DialogUtils.showGenericAlertCustomView(getContext(), R.layout.custom_dialog_single_post_deleted);
						if (dialog != null) {
							new Handler().postDelayed(() -> {
								dialog.dismiss();
								if (getActivity() != null)
									getActivity().finish();
							}, 2000);
						}
					}
					break;

				case Constants.SERVER_OP_SETTINGS_IC_GET_SET_FEED:
					realm.executeTransaction(realm -> {
						if (selectedSortOrder > 0)
							mUser.getSettings().setRawSortOrder(selectedSortOrder);

						mListener.setLastPageID(1);
						LBSLinkApp.refreshAllowed = true;
						mListener.setLastAdapterPosition(0);

						callTimeline(true);
						toggleSortSectionVisibility();
					});
					break;


				case Constants.SERVER_OP_CAN_APPLY_FAMILY_FILTER:
					if (responseObject == null || responseObject.isNull(0))
						return;

					hasFamilyRelations = responseObject.optJSONObject(0).optBoolean("canApplyFamilyFilter", false);

					realm.executeTransaction(realm -> mUser.updateFiltersForSingleCircle(new HLCircle(Constants.CIRCLE_FAMILY_NAME), hasFamilyRelations));
					break;

				case Constants.SERVER_OP_GET_CIRCLES_PROFILE:
					if (responseObject == null || responseObject.length() == 0)
						return;

					realm.executeTransactionAsync(
							realm -> {
								HLCircle circle;
								JSONObject circles = responseObject.optJSONObject(0);
								HLUser user = new HLUser().readUser(realm);
								if (circles != null) {
									if (user.getCircleObjects() == null)
										user.setCircleObjects(new RealmList<>());
									else
										user.getCircleObjects().clear();

									Iterator<String> iter = circles.keys();
									while (iter.hasNext()) {
										String key = iter.next();
										circle = new HLCircle().deserializeToClass(circles.optJSONObject(key));
										user.getCircleObjects().add(circle);
									}
								}
							},
							() -> {
								realm = RealmUtils.checkAndFetchRealm(realm);
								mUser = RealmUtils.checkAndFetchUser(realm, mUser);

								populateFilters(realm, mUser);
								setFilterView(realm, mUser);
							}
					);
					break;
			}
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (errorCode) {
			case Constants.SERVER_ERROR_GENERIC:
				activityListener.showAlert(R.string.error_generic_list);
				break;

			case Constants.SERVER_OP_SETTINGS_IC_GET_SET_FEED:
				activityListener.showGenericError();
				break;
		}

		LogUtils.e(LOG_TAG, "SERVER ERROR with error: " + errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);

		dataFromLoadMore = false;

		switch (usageType) {
			case TIMELINE:
				setData(realm, false);
				break;
			case SINGLE:
				setDataSingle();
				break;
			case DIARY:
			case GUEST:
			case GLOBAL_SEARCH:
				setDataPostList();
				break;
		}
	}


	@Override
	public BasicInteractionListener getActivityListener() {
		return activityListener;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return postAdapter;
	}

	@Override
	public void setData(Realm realm) {
		setData(realm, true);
	}

	@Override
	public void setData(JSONArray array) {}

	@Override
	public boolean isFromLoadMore() {
		return dataFromLoadMore;
	}

	@Override
	public void resetFromLoadMore() {
		dataFromLoadMore = false;
	}

	@Override
	public int getLastPageId() {
		return mListener.getLastPageID();
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}


	/*
	 * Post actions from bottom sheet interface
	 */
	@Override
	public void onPostDeleted(@NonNull String postId) {
		if (Utils.isStringValid(postId)) {
			Post post = HLPosts.getInstance().getPost(postId);
			if (post != null && posts.contains(post)) {
				int index = posts.indexOf(post);
				if (index != -1) {
					posts.remove(index);
					postAdapter.notifyItemRemoved(index);
				}

				HLPosts.getInstance().deletePost(postId, realm, true);
			}
		}
	}

	private Integer indexBeforeDeletion = null;
	@Override
	public void onInterestUnFollowed(@NonNull String postId, boolean followed) {
		HLPosts posts = HLPosts.getInstance();
		Post post = posts.getPost(postId);
		if (!followed && post != null) {
//			indexBeforeDeletion = posts.getPostIndex(postId, false);

			posts.deletePostsByAuthorId(post.getAuthorId(), new Handler(this));
		}
	}

	@Override
	public void onUserBlocked(@NonNull String postId) {
		HLPosts posts = HLPosts.getInstance();
		Post post = posts.getPost(postId);

//		indexBeforeDeletion = posts.getPostIndex(postId, false);

		if (post != null)
			posts.deletePostsByAuthorId(post.getAuthorId(), new Handler(this));
	}


	@Override
	public boolean handleMessage(Message msg) {
		saveLastPostSeen(false);
		setData(realm, false);
		return true;
	}


	//region == REAL-TIME COMMUNICATION ==

	@Override
	public void onPostAdded(@Nullable Post post, int position) {
		if (usageType == FragmentUsageType.TIMELINE) {
			if (post != null) {
				saveLastPostSeen(false);

				posts.add(position, post);
				postAdapter.notifyItemInserted(position);

				int lastPos = HLPosts.getInstance().getPostIndex(getContext());
				if (position < lastPos) animateNewPostToast();
			}
		}
	}

	private void animateNewPostToast() {
		if (newPostToast != null) {
			newPostToast.animate().alpha(1).setDuration(300).setListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {
					newPostToast.setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					hideNewPostToast(false);
				}

				@Override
				public void onAnimationCancel(Animator animation) {}

				@Override
				public void onAnimationRepeat(Animator animation) {}
			}).start();
		}
	}

	private void hideNewPostToast(boolean fromTap) {
		new Handler().postDelayed(() -> newPostToast.animate().alpha(0).setDuration(300).setListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {}

			@Override
			public void onAnimationEnd(Animator animation) {
				newPostToast.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationCancel(Animator animation) {}

			@Override
			public void onAnimationRepeat(Animator animation) {}
		}).start(), fromTap ? 0 : 5000);
	}

	@Override
	public void onPostUpdated(@NonNull String postId, int position) {
		if (usageType == FragmentUsageType.TIMELINE) {
			int pos = HLPosts.getInstance().getPostIndex(postId, false);
			postAdapter.notifyItemChanged(pos);
		}
	}

	@Override
	public void onPostDeleted(int position) {
		if (usageType == FragmentUsageType.TIMELINE) {
			posts.remove(position);
			postAdapter.notifyItemRemoved(position);
		}
	}

	@Override
	public void onHeartsUpdated(int position) {
		if (usageType == FragmentUsageType.TIMELINE)
			postAdapter.notifyItemChanged(position);
	}

	@Override
	public void onSharesUpdated(int position) {
		if (usageType == FragmentUsageType.TIMELINE)
			postAdapter.notifyItemChanged(position);
	}

	@Override
	public void onTagsUpdated(int position) {
		if (usageType == FragmentUsageType.TIMELINE)
			postAdapter.notifyItemChanged(position);
	}

	@Override
	public void onCommentsUpdated(int position) {
		if (usageType == FragmentUsageType.TIMELINE)
			postAdapter.notifyItemChanged(position);
	}

	@Override
	public void onNewDataPushed(boolean hasInsert) {
		if (usageType == FragmentUsageType.TIMELINE) {
			setData(realm, false);

			// only if user's position != 0 it makes sense to show the toast
			if (hasInsert && posts.size() > 1 && HLPosts.getInstance().getPostIndex(getContext()) > 0)
				animateNewPostToast();
		}
	}

	// NOT CALLED
	@Override
	public void registerRealTimeReceiver() {
//		if (Utils.isContextValid(getContext()))
//			getContext().registerReceiver(realTimeHelper.getServerMessageReceiver(), new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));
//			LocalBroadcastManager.getInstance(getContext()).registerReceiver(realTimeHelper.getServerMessageReceiver(), new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));
	}

	// NOT CALLED
	@Override
	public void unregisterRealTimeReceiver() {
		try {
//			if (Utils.isContextValid(getContext()))
//				getContext().unregisterReceiver(realTimeHelper.getServerMessageReceiver());
//				LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(realTimeHelper.getServerMessageReceiver());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Realm getRealm() {
		return realm;
	}

	//endregion


	//region == CLASS CUSTOM METHODS ==

	@Override
	protected void configureLayout(@NonNull View view) {

		newPostToast = view.findViewById(R.id.toast_rt_new_post);
		newPostToast.setOnClickListener(this);

		postListView = view.findViewById(R.id.list);
		postListView.addOnScrollListener(
				mScrollListener = new LoadMoreScrollListener(LoadMoreScrollListener.Type.MAIN_FEED) {
					@Override
					public void onLoadMore() {
						saveLastPostSeen(false);
						callTimelineFromLoadMore();
					}
				}
		);

		srl = Utils.getGenericSwipeLayout(view, () -> {

			mScrollListener.setCanFetch(null);

			mListener.setLastAdapterPosition(0);
			mListener.setLastPageID(1);

			// resets stored post in SharedPreferences
			saveLastPostSeen(true);

			Utils.setRefreshingForSwipeLayout(srl, true);

			if (usageType == FragmentUsageType.TIMELINE)
				callTimeline(true);
			else if (usageType == FragmentUsageType.SINGLE)
				callSinglePost();
			else if (usageType == FragmentUsageType.DIARY)
				callPostList(0);
			else if (usageType == FragmentUsageType.GLOBAL_SEARCH)
				callPostListGlobal(0);
		});

		configureFilter(view);
	}

	@Override
	protected void setLayout() {
		boolean canShowToolbarForFullScreen =
				mListener.getFullScreenListener().getFullScreenType() == FullScreenHelper.FullScreenType.NONE;
		toolbar.setVisibility(canShowToolbarForFullScreen && conditionForLayout() ? View.VISIBLE : View.GONE);
		toolbarDiarySingle.setVisibility(!canShowToolbarForFullScreen || conditionForLayout() ? View.GONE : View.VISIBLE);

		int px = getResources().getDimensionPixelSize(R.dimen.toolbar_height);
		srl.setProgressViewOffset(true, px, px + (px/2));

		@DrawableRes int sortResId;
		switch (mUser.getSettingSortOrder()) {
			case Constants.SERVER_SORT_TL_ENUM_LOVED:
				sortResId = R.drawable.layer_sort_mostloved_active;
				break;
			case Constants.SERVER_SORT_TL_ENUM_SHUFFLE:
				sortResId = R.drawable.layer_sort_shuffle_active;
				break;
			default:
				sortResId = R.drawable.layer_sort_recent_active;
		}
		sortBtn.setImageResource(sortResId);

		sortRecent.setSelected(
				mUser.getSettingSortOrder() == Constants.SERVER_SORT_TL_ENUM_RECENT ||
						mUser.getSettingSortOrder() == Constants.SERVER_SORT_TL_ENUM_DEFAULT
		);
		sortLoved.setSelected(mUser.getSettingSortOrder() == Constants.SERVER_SORT_TL_ENUM_LOVED);
		sortShuffle.setSelected(mUser.getSettingSortOrder() == Constants.SERVER_SORT_TL_ENUM_SHUFFLE);

		setFilterView(realm, mUser);
		filterToggle.setSelected(!isFilterAll);
		handleFilterToggleVisibility();
	}

	private void configurePostOptionsSheets() {
		if (getActivity() instanceof HomeActivity || getActivity() instanceof GlobalSearchActivity)
			return;

		if (bottomSheetHelper == null && getActivity() instanceof HLActivity) {
			bottomSheetHelper = new PostBottomSheetHelper((HLActivity) getActivity());
		}
		bottomSheetHelper.configurePostOptionsSheets(mainView);
		bottomSheetHelper.setPostActionsListener(this);
	}

	private boolean conditionForLayout() {
		return usageType == FragmentUsageType.TIMELINE;
	}

	private void setData(Realm realm, boolean background) {
		if (posts == null)
			posts = new ArrayList<>();
		posts.clear();
		posts.addAll(HLPosts.getInstance().getVisiblePosts());

		if (!background) {
			if (dataFromLoadMore) {
				getAdapter().notifyItemRangeInserted(
						/*Constants.PAGINATION_AMOUNT * getLastPageId()*/
						mListener.getFeedSkip(realm, FragmentUsageType.TIMELINE, false),
						getNewItemsCount()
				);
				dataFromLoadMore = false;
			} else {
				postAdapter.notifyDataSetChanged();
				restoreLastPosition();
			}
		}
	}

	private void callCircles() {
		Object[] result = null;

		try {
			if (mUser.isValid())
				result = HLServerCalls.getCirclesForProfile(mUser.getUserId(), 1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
				.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	// checkFamily is currently unused
	private boolean checkFamily = false;
	private void configureFilter(View view) {
		feedFilter = view.findViewById(R.id.tl_filter);

		filterContainer = view.findViewById(R.id.filters_container);

		populateFilters(realm, mUser);

		view.findViewById(R.id.filter_apply_btn).setOnClickListener(v -> {

			if (filtersString == null)
				filtersString = new ArrayList<>();
			else
				filtersString.clear();

			if (isFilterAll) {
				filtersString.add(mUser.isActingAsInterest() ?
						FeedFilterTypeEnum.getCallValue(FeedFilterTypeEnum.ALL_INT) : FeedFilterTypeEnum.getCallValue(FeedFilterTypeEnum.ALL));

				checkFamily = !mUser.isActingAsInterest();
			}
			else if (filters != null && !filters.isEmpty()) {
				Stream.of(filters).forEach(feedFilter -> {
					if (feedFilter != null && feedFilter.isSelected()) {
						filtersString.add(feedFilter.getNameForServer());

						if (feedFilter.isCircle() && feedFilter.isFamilyCircle())
							checkFamily = true;
					}
				});

				if (filtersString.isEmpty()) {
					activityListener.showAlert(R.string.error_filter_no_sel);
					return;
				}
			}
			else  {
				activityListener.showGenericError();
				return;
			}

			// Dialog currently unused.
//			if (/*checkFamily && !hasFamilyRelations*/false) {
//				final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_generic_title_text_btns);
//				if (dialog != null) {
//					View customView = dialog.getCustomView();
//					if (customView != null) {
//
//						((TextView) customView.findViewById(R.id.dialog_title)).setText(R.string.dialog_familY_filter_title);
//						((TextView) customView.findViewById(R.id.dialog_message)).setText(R.string.dialog_family_filter_message);
//
//						Button pos = customView.findViewById(R.id.button_positive);
//						pos.setText(R.string.yes);
//						pos.setOnClickListener(new View.OnClickListener() {
//							@Override
//							public void onClick(View v) {
//								ProfileActivity.openUserDetailFragment(getContext());
//								dialog.dismiss();
//							}
//						});
//
//						Button neg = customView.findViewById(R.id.button_negative);
//						neg.setText(R.string.action_later);
//						neg.setOnClickListener(new View.OnClickListener() {
//							@Override
//							public void onClick(View v) {
//								filterRequested = true;
//								callTimeline(true);
//
//								dialog.dismiss();
//							}
//						});
//					}
//
//					dialog.show();
//				}
//
//				checkFamily = false;
//			}
//			else {
				filterRequested = true;
				callTimeline(true);
//			}

			realm.executeTransaction(realm -> {
				if (mUser.getSelectedFeedFilters() == null)
					mUser.setSelectedFeedFilters(new RealmList<>());
				else
					mUser.getSelectedFeedFilters().clear();
				mUser.getSelectedFeedFilters().addAll(filtersString);
			});
		});
	}

	private void populateFilters(Realm realm, HLUser user) {
		if (filters == null)
			filters = new ArrayList<>();
		else
			filters.clear();

		realm = RealmUtils.checkAndFetchRealm(realm);
		user = RealmUtils.checkAndFetchUser(realm, user);

		if (Utils.isContextValid(getContext())) {
			if (!user.isActingAsInterest()) {
				filters.add(new FeedFilter(getContext(), FeedFilterTypeEnum.ALL));
				List<HLCircle> circles = new ArrayList<>(user.getCircleObjects());
				if (circles.size() > 0) {
					Collections.sort(circles, HLCircle.CircleNameComparator);
					for (HLCircle circle : circles) {
						filters.add(new FeedFilter(circle.getName(), circle.getNameToDisplay()));
					}
				}
				filters.add(new FeedFilter(getContext(), FeedFilterTypeEnum.INTERESTS));
				filters.add(new FeedFilter(getContext(), FeedFilterTypeEnum.NEWS));
			} else {
				filters.add(new FeedFilter(getContext(), FeedFilterTypeEnum.ALL_INT));
				filters.add(new FeedFilter(getContext(), FeedFilterTypeEnum.MY_STORIES));
				filters.add(new FeedFilter(getContext(), FeedFilterTypeEnum.NEWS_INT));
			}
		}
	}

	private void setFilterView(Realm realm, HLUser user) {
		if (filterContainer != null) {
			filterContainer.removeAllViews();

			if (filters != null && !filters.isEmpty()) {

				realm = RealmUtils.checkAndFetchRealm(realm);
				user = RealmUtils.checkAndFetchUser(realm, user);

				final boolean isInterest = user.isActingAsInterest();

				filtersString = new ArrayList<>(user.getSelectedFeedFilters());
				isFilterAll = filtersString.isEmpty() || filtersString.contains("any") || filtersString.contains("anyInterest");

				filterToggle.setSelected(!isFilterAll);

				for (final FeedFilter filter : filters) {
					if (filter != null) {
						final View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_checkbox_dark_filter, filterContainer, false);

						if (view != null) {
							((TextView) view.findViewById(R.id.text)).setText(filter.getNameToDisplay());
							boolean selected = filtersString != null && !filtersString.isEmpty() && filtersString.contains(filter.getNameForServer());
							if (isFilterAll || selected)
								filter.setSelected(true);
							view.setSelected(filter.isSelected());
							view.setOnClickListener(v -> {
								boolean selected1 = !view.isSelected();
								switch (filter.getType()) {
									case ALL:
									case ALL_INT:
										isFilterAll = selected1;
										for (int i = 0; i < filterContainer.getChildCount(); i++) {
											View view1 = filterContainer.getChildAt(i);
											FeedFilter f = filters.get(i);
											view1.setSelected(selected1);
											f.setSelected(selected1);
										}
										break;

									case INTERESTS:
									case NEWS:
									case CIRCLE:
									case MY_STORIES:
									case NEWS_INT:
										view.setSelected(selected1);
										filter.setSelected(selected1);
										boolean isIC = filter.isInnerCircle();
										if (!selected1) {
											for (int i = 0; i < filterContainer.getChildCount(); i++) {
												View view1 = filterContainer.getChildAt(i);
												FeedFilter f = filters.get(i);
												if (f.isAll() && f.isSelected()) {
													view1.setSelected(false);
													f.setSelected(false);
													isFilterAll = false;
												}
												if (!isInterest && f.isInnerCircle() && filter.isCircle()) {
													view1.setSelected(false);
													f.setSelected(false);
												}
											}
										}
										else {
											boolean allSelected = true;
											View allView = null;
											FeedFilter allFilter = null;
											for (int i = 0; i < filterContainer.getChildCount(); i++) {
												View view1 = filterContainer.getChildAt(i);
												FeedFilter f = filters.get(i);
												if (f.isAll()) {
													allFilter = f;
													allView = view1;
												}
												else {
													if (!f.isSelected()) {
														if (!isInterest && isIC && f.isCircle()) {
															f.setSelected(true);
															view1.setSelected(true);
															continue;
														}

														allSelected = false;
														break;
													}
												}
											}

											if (allSelected && allView != null) {
												allFilter.setSelected(true);
												allView.setSelected(true);
												isFilterAll = true;
											}
										}
										break;
								}
							});

							filterContainer.addView(view);
						}
					}
				}
			}
		}
	}

	private void setDataSingle() {
		if (posts == null)
			posts = new ArrayList<>();
		posts.clear();
		posts.add(HLPosts.getInstance().getPost(postId));

		postAdapter.notifyDataSetChanged();
	}

	private void setDataPostList() {
		if (posts == null)
			posts = new ArrayList<>();
		posts.clear();
		posts.addAll(HLPosts.getInstance().getSortedPostsForDiary());

		if (dataFromLoadMore) {
			getAdapter().notifyItemRangeInserted(
					/*Constants.PAGINATION_AMOUNT * getLastPageId()*/
					mListener.getFeedSkip(realm, FragmentUsageType.DIARY, false),      // used #DIARY but any other than #TIMELINE should work
					getNewItemsCount()
			);
			dataFromLoadMore = false;
		}
		else {
			postAdapter.notifyDataSetChanged();

			if (!posts.isEmpty())
				restoreLastPosition();
		}
	}

	private void restoreLastPosition() {
		if (postListView != null && Utils.isContextValid(getActivity())) {
			int lastPos;
			if (usageType == FragmentUsageType.TIMELINE) {
				lastPos = HLPosts.getInstance().getPostIndex(getActivity());

				RealTimeCommunicationHelperKotlin RTHelper = RealTimeCommunicationHelperKotlin.Companion.getInstance(getContext());
				if (RTHelper.getHasInsert()) {
					animateNewPostToast();
					RTHelper.setHasInsert(false);
				}
			}
			else {
				lastPos = HLPosts.getInstance()
						.getPostIndex(
								Utils.isStringValid(postId) ?
										postId : HLPosts.lastPostSeenIdGlobalDiary,
								true
						);
				postId = null;
			}
			new Handler().post(() -> postListView.scrollToPosition(lastPos));
		}
	}

	public void saveLastPostSeen(boolean reset) {
		if (usageType == FragmentUsageType.TIMELINE) {
			if (reset) HLPosts.lastPostSeenId = "";

			SharedPrefsUtils.storeLastPostSeen(getContext(), HLPosts.lastPostSeenId);
			LogUtils.d("LAST POST STORED in FEED", HLPosts.lastPostSeenId);
		}
	}

	private void callTimeline(Integer explicitSortOrder, boolean wantsTop) {
		Object[] results = null;
		try {
			results = HLServerCalls.getTimeline(mUser.getId(), mUser.getCompleteName(), mUser.getAvatarURL(),
					explicitSortOrder == null ? mUser.getSettingSortOrder() : explicitSortOrder,
					mListener.getFeedSkip(realm, FragmentUsageType.TIMELINE, wantsTop),
					filtersString, mUser.isActingAsInterest());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (results != null && getActivity() != null) {
			HLRequestTracker.getInstance((OnApplicationContextNeeded) getActivity().getApplication())
					.handleCallResult(this, ((HLActivity) getActivity()), results);
		}
	}

	private void callTimeline(boolean wantsTop) {
		callTimeline(null, wantsTop);
	}

	private void callTimelineFromLoadMore() {

		tempSkip = mListener.getFeedSkip(realm, usageType, false);

		if (usageType == FragmentUsageType.TIMELINE)
			callTimeline(false);
		else if (usageType == FragmentUsageType.DIARY)
			callPostList(tempSkip);
		else if (usageType == FragmentUsageType.GLOBAL_SEARCH)
			callPostListGlobal(tempSkip);

		dataFromLoadMore = true;
	}

	private void callSinglePost() {
		Object[] results = null;
		try {
			results = HLServerCalls.getSinglePost(mUser.getId(), postId);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (results != null && getActivity() != null) {
			HLRequestTracker.getInstance((OnApplicationContextNeeded) getActivity().getApplication())
					.handleCallResult(this, ((HLActivity) getActivity()), results);
		}
	}

	private void callPostList(int skip) {
		Object[] results = null;

		List<String> lists = new ArrayList<>();
		lists.add(postListName);

		try {
			results = HLServerCalls.getTimeline(userId, userName, userAvatar, -1,
					skip, lists, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (results != null && getActivity() != null) {
			HLRequestTracker.getInstance((OnApplicationContextNeeded) getActivity().getApplication())
					.handleCallResult(this, ((HLActivity) getActivity()), results);
		}
	}


	private void callPostListGlobal(int skip) {
		Object[] results = null;
		try {
			results = HLServerCalls.actionsOnGlobalSearch(userId, globalSearchQuery, HLServerCalls.GlobalSearchAction.GET_TIMELINE,
					postListName, skip);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (results != null && getActivity() != null) {
			HLRequestTracker.getInstance((OnApplicationContextNeeded) getActivity().getApplication())
					.handleCallResult(this, ((HLActivity) getActivity()), results);
		}
	}


	private Integer selectedSortOrder = null;
	private void handleSorting(View view) {
		view.setSelected(!view.isSelected());
		switch (view.getId()) {
			case R.id.sort_date:
				selectedSortOrder = Constants.SERVER_SORT_TL_ENUM_RECENT;
				sortLoved.setSelected(false);
				sortShuffle.setSelected(false);
				break;
			case R.id.sort_loved:
				selectedSortOrder = Constants.SERVER_SORT_TL_ENUM_LOVED;
				sortRecent.setSelected(false);
				sortShuffle.setSelected(false);
				break;
			case R.id.sort_shuffle:
				selectedSortOrder = Constants.SERVER_SORT_TL_ENUM_SHUFFLE;
				sortRecent.setSelected(false);
				sortLoved.setSelected(false);
				break;
		}

		callTimeline(selectedSortOrder, true);


		// TODO: 9/7/2018    should just be an override of the main setting in Settings section
		/*
		// TODO: 4/24/2018    CHECK IF CORRECT and eventually move code in OnSuccessResponse HERE
		if (selectedSortOrder != null) {
			Bundle dataBundle = new Bundle();
			dataBundle.putString("type", "set");
			dataBundle.putInt("sortOrder", selectedSortOrder);
			dataBundle.putInt("autoPlayVideos", mUser.getSettingAutoPlay());

			Object[] result = null;

			try {
				result = HLServerCalls.settingsOperationsOnFeedSettings(mUser.getUserId(), dataBundle,
						SettingsICTimelineFeedFragment.CallType.SET);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (getActivity() instanceof HLActivity) {
				HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
						.handleCallResult(this, (HLActivity) getActivity(), result);
			}
		}
		*/
	}


	private void callFamilyCheck() {
		Object[] result = null;

		try {
			result = HLServerCalls.canApplyFamilyFilter(mUser.getUserId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}

	public void handleFilterToggle() {

		filterVisible = !filterVisible;

		if (filterVisible && filterContainer != null && filters != null && !mUser.isActingAsInterest()) {
			int index = -1;
			for (int i = 0; i < filters.size(); i++) {
				FeedFilter filter = filters.get(i);
				if (filter.isFamilyCircle())
					index = i;
			}

			if (hasFamilyRelations == null)
				hasFamilyRelations = mUser.hasAuthorizedFamilyRelationships();

			if (index == -1 && hasFamilyRelations) {
				final FeedFilter family = new FeedFilter(Constants.CIRCLE_FAMILY_NAME, Constants.CIRCLE_FAMILY_NAME_IT);
				filters.add(1, family);
				final View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_checkbox_dark_filter, filterContainer, false);

				if (view != null) {
					((TextView) view.findViewById(R.id.text)).setText(family.getNameToDisplay());
					boolean icSelected = filtersString != null &&
							!filtersString.isEmpty() &&
							filtersString.contains(FeedFilterTypeEnum.getCallValue(Constants.INNER_CIRCLE_NAME));
					family.setSelected(isFilterAll || icSelected);
					view.setSelected(family.isSelected());
					view.setOnClickListener(v -> {
						boolean selected = !view.isSelected();
						view.setSelected(selected);
						family.setSelected(selected);
						if (!selected) {
							for (int i = 0; i < filterContainer.getChildCount(); i++) {
								View view1 = filterContainer.getChildAt(i);
								FeedFilter f = filters.get(i);
								if (f.isAll() && f.isSelected()) {
									view1.setSelected(false);
									f.setSelected(false);
									isFilterAll = false;
								}
								if (f.isInnerCircle()) {
									view1.setSelected(false);
									f.setSelected(false);
								}
							}
						}
						else {
							boolean allSelected = true;
							View allView = null;
							FeedFilter allFilter = null;
							for (int i = 0; i < filterContainer.getChildCount(); i++) {
								View view1 = filterContainer.getChildAt(i);
								FeedFilter f = filters.get(i);
								if (f.isAll()) {
									allFilter = f;
									allView = view1;
								}
								else {
									if (!f.isSelected()) {
										allSelected = false;
										break;
									}
								}
							}

							if (allSelected && allView != null) {
								allFilter.setSelected(true);
								allView.setSelected(true);
								isFilterAll = true;
							}
						}
					});

					filterContainer.addView(view, 1);
				}
			}
			else if (index > -1 && !hasFamilyRelations) {
				filters.remove(index);
				filterContainer.removeViewAt(index);
			}
		}

		feedFilter.setVisibility(filterVisible ? View.VISIBLE : View.GONE);

		if (feedFilter.getVisibility() == View.GONE) {
			filterToggle.setSelected(!isFilterAll);
		}
	}

	private void handleFilterToggleVisibility() {
		filterToggle.setVisibility(conditionForLayout() ? View.VISIBLE : View.GONE);
	}

	public boolean hasFilterVisible() {
		return feedFilter != null && feedFilter.getVisibility() == View.VISIBLE;
	}

	private void toggleSortSectionVisibility() {
		if (sortingSection.getVisibility() == View.VISIBLE) {
			sortingSection.setVisibility(View.GONE);
			titleView.setVisibility(View.VISIBLE);
			handleFilterToggleVisibility();

			@DrawableRes int sortResId;
			switch (mUser.getSettingSortOrder()) {
				case Constants.SERVER_SORT_TL_ENUM_LOVED:
					sortResId = R.drawable.layer_sort_mostloved_active;
					break;
				case Constants.SERVER_SORT_TL_ENUM_SHUFFLE:
					sortResId = R.drawable.layer_sort_shuffle_active;
					break;
				default:
					sortResId = R.drawable.layer_sort_recent_active;
			}
			sortBtn.setImageResource(sortResId);
		}
		else {
			titleView.setVisibility(View.GONE);
			filterToggle.setVisibility(View.GONE);
			sortingSection.setVisibility(View.VISIBLE);
		}
	}

	//endregion


	//region == Getters and setters ==

	public void setConfigListener(OnConfigChangedAction mConfigListener) {
		this.mConfigListener = mConfigListener;
	}

	public InnerCircleFeedAdapter getPostAdapter() {
		return postAdapter;
	}

	public Toolbar getToolbar() {
		return conditionForLayout() ? toolbar : toolbarDiarySingle;
	}

	public PostBottomSheetHelper getBottomSheetHelper() {
		return bottomSheetHelper;
	}

	public FragmentUsageType getUsageType() {
		return usageType;
	}
	public void setUsageType(FragmentUsageType usageType) {
		this.usageType = usageType;
	}

	public void setPostListName(String postListName) {
		this.postListName = postListName;
	}

	//endregion


	public interface OnConfigChangedAction {
		void onConfigChanged();
	}


	private class HLOrientationSensorListener implements SensorEventListener {
		int orientation=-1;

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.values[1] < 6.5 && event.values[1] > -6.5) {
				if (orientation != 1) {
					LogUtils.d(LOG_TAG, "Sensor Event: Landscape");
					mConfigListener.onConfigChanged();
				}
				orientation = 1;
			}
			else {
				if (orientation != 0)
					LogUtils.d(LOG_TAG, "Sensor Event: Portrait");
				orientation = 0;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int i) {}
	}


	/**
	 * Extension of {@link AsyncTask} handling the data update for new posts.
	 * It accepts as param the {@link JSONArray} server response.
	 */
	private class PostResponseHandler extends AsyncTask<JSONArray, Void, Void> {

		boolean exception = false;
		int size = 20;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(JSONArray... params) {
			Realm realm = null;
			try {
				realm = RealmUtils.getCheckedRealm();

				HLPosts instance = HLPosts.getInstance();
				try {
//					instance.cleanRealmPostsNewSession(realm, true);
					if (params[0] != null) {
						size = params[0].length();
						instance.setPosts(params[0], realm, true);
					}
				}
				catch (JSONException e) {
					LogUtils.e(LOG_TAG, e.getMessage(), e);
					setData(realm, true);
					exception = true;
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RealmUtils.closeRealm(realm);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			if (exception)
				activityListener.showAlert(R.string.error_generic_list);

			if (size == 1)
				postAdapter.notifyItemInserted(0);
			else if (size > 1) {
				postAdapter.notifyItemRangeInserted(0, size);
			}
		}
	}

	private Boolean isVisible = null;
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		isVisible = isVisibleToUser;
		if (postAdapter != null) {
			if (!isVisibleToUser)
				postAdapter.cleanAdapterMediaControllers(false);
			else
				postAdapter.resumeVideo();

			super.setUserVisibleHint(isVisibleToUser);
		}
	}

	public boolean isVisibleToUser() {
		boolean currentBottomItemTL = (HomeActivity.getCurrentPagerItem() == HomeActivity.PAGER_ITEM_TIMELINE);
		return isVisible == null || isVisible || currentBottomItemTL;
	}
	public void setVisible(boolean visible) {
		isVisible = visible;
	}

}
