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
import android.os.Handler;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import io.realm.Realm;
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
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.PostList;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;

/**
 * Although the feeling must be the same as the My Diary screen in the user's Profile section, a new
 * class is needed because of the intrinsic difference in logic between the two screens.
 */
public class WishPostSelectionFoldersFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener,
		WishPostSelectionAdapterFolders.OnPostTileActionListener, PostListsAdapterWishes.WishPostListAdapterListener,
		LoadMoreResponseHandlerTask.OnDataLoadedListener, OnHandlingScrollPositionListener {

	public static final String LOG_TAG = WishPostSelectionFoldersFragment.class.getCanonicalName();

	private RecyclerView baseRecView;
	private LinearLayoutManager llm;
	private Map<String, PostList> postListsLoaded = new HashMap<>();
	private List<PostList> postListToShow = new ArrayList<>();
	private PostListsAdapterWishes listAdapter;

	private View shufflePosts;
	private TextView noResult;

//	private int stepToHighlight;

	private boolean fromLoadMore = false;
	private int pageIdToCall = 0;
	private int newItemsCount;
	private RecyclerView.Adapter loadMoreAdapter;
	private String scrollingListName;
	private List<Post> scrollingPostList;

	private ArrayList<String> selectedPosts = new ArrayList<>();

	private WishListElement selectedElement;

	private Integer scrollPosition = null;
	private SparseArray<WeakReference<RecyclerView>> scrollViews = new SparseArray<>();
	private SparseIntArray scrollViewsPositions = new SparseIntArray();


	public WishPostSelectionFoldersFragment() {
		// Required empty public constructor
	}

	public static WishPostSelectionFoldersFragment newInstance() {
		Bundle args = new Bundle();
		WishPostSelectionFoldersFragment fragment = new WishPostSelectionFoldersFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_wishes_post_selection_folders, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() != null) {
			listAdapter = new PostListsAdapterWishes(postListToShow, this, this, this);
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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_POST_SELECTION_FOLDERS);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		callForPosts();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		pageIdToCall = 0;
		fromLoadMore = false;
		scrollingListName = null;

		scrollPosition = llm.findFirstCompletelyVisibleItemPosition();
		if (scrollViews != null && scrollViews.size() > 0) {
			for (int i = 0; i < scrollViews.size(); i++) {
				int key = scrollViews.keyAt(i);
				WeakReference<RecyclerView> hsv = scrollViews.get(key);
				RecyclerView scrollView = hsv.get();
				if (scrollView != null && scrollView.getTag() instanceof Integer) {

					int pos = 0;
					if (scrollView.getLayoutManager() instanceof LinearLayoutManager)
						pos = ((LinearLayoutManager) scrollView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
					scrollViewsPositions.put(((Integer) scrollView.getTag()), pos);
				}
			}
		}

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
		switch (v.getId()) {
			case R.id.shuffle_post_layout:
				shufflePosts.setSelected(!shufflePosts.isSelected());
				break;
		}
	}

	@Override
	public void onNextClick() {
		Bundle bundle = new Bundle();
		bundle.putBoolean("wantsShuffle", shufflePosts.isSelected());
		bundle.putStringArrayList("selectedPosts", selectedPosts);
		wishesActivityListener.setDataBundle(bundle);

		wishesActivityListener.setSelectedWishListElement(selectedElement);
		wishesActivityListener.resumeNextNavigation();
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

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_POST_FOLDERS:
				if (fromLoadMore) {
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.WISH_POSTS,
							null, null).execute(responseObject);
				}
				else
					reload = true;

					try {
						handleListResponse(responseObject);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				break;

			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				if (responseObject == null || responseObject.length() == 0)
					return;

				JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
				if (json != null && json.length() == 1) {
					selectedElement = new WishListElement().deserializeToClass(json.optJSONObject(0));
				}

				wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
				wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_POSTS:
				activityListener.showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		activityListener.closeProgress();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	protected void configureLayout(@NonNull View view) {
		baseRecView = view.findViewById(R.id.base_list);

		shufflePosts = view.findViewById(R.id.shuffle_post_layout);
		shufflePosts.setOnClickListener(this);

		noResult = view.findViewById(R.id.no_result);
	}

	@Override
	protected void setLayout() {
		baseRecView.setLayoutManager(llm);
		baseRecView.setAdapter(listAdapter);

		if (reload) {
			List<PostList> list = new ArrayList<>(postListsLoaded.values());
			Collections.sort(list, PostList.ListSortOrderComparator);
			postListToShow.addAll(list);
			listAdapter.notifyDataSetChanged();

			reload = false;
		}

		restorePositions();

		noResult.setVisibility(!postListToShow.isEmpty() ? View.GONE : View.VISIBLE);
	}

	private void handleListResponse(JSONArray response) throws JSONException {
		PostList list;
		JSONObject lists = response.getJSONObject(0);
		if (lists != null) {
			Iterator<String> iter = lists.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				list = new PostList().deserializeToClass(lists.getJSONObject(key));
				list.setNameToDisplay(key);

				postListsLoaded.put(list.getName(), list);
			}

			setLayout();
		}
	}

	private void restorePositions() {
		if (scrollPosition != null) {
			llm.scrollToPosition(scrollPosition);
		}
	}

	boolean reload = false;
	private void callForPosts() {

		if (postListsLoaded != null && !postListsLoaded.isEmpty() && !fromLoadMore) return;

		Object[] result = null;

		if (postListsLoaded == null)
			postListsLoaded = new HashMap<>();

		if (!fromLoadMore) {
			if (postListToShow == null)
				postListToShow = new ArrayList<>();
			else
				postListToShow.clear();

			pageIdToCall = 0;
		}

		try {
			result = HLServerCalls.getPostFoldersForWish(mUser.getUserId(), ++pageIdToCall, scrollingListName);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}


	//region == Adapter interface ==

	@Override
	public void openPostPreview(View view, Post post) {
		if (Utils.isContextValid(getActivity())) {

			HLPosts.getInstance().setSelectedPostForWish(post);

			Intent intent = new Intent(getActivity(), PostPreviewActivity.class);
			startActivity(intent);

//			if (Utils.hasLollipop()) {
//				String name = Constants.TRANSITION_WISH_POST_ENLARGE + "_" + post.getId();
//				intent.putExtra(Constants.EXTRA_PARAM_1, name);
//				ViewCompat.setTransitionName(view, name);
//				ActivityOptions options = ActivityOptions
//						.makeSceneTransitionAnimation(getActivity(), view, name);
//				startActivity(intent, options.toBundle());
//			} else {
//				startActivity(intent);
//				getActivity().overridePendingTransition(0, 0);
//			}
		}
	}

	@Override
	public void selectDeselect(String listName, String postId, int position, RecyclerView.Adapter adapter) {
		if (selectedPosts == null)
			selectedPosts = new ArrayList<>();

		if (!selectedPosts.contains(postId))
			selectedPosts.add(postId);
		else
			selectedPosts.remove(postId);

		wishesActivityListener.enableDisableNextButton(!selectedPosts.isEmpty());

		adapter.notifyItemChanged(position);
	}

	@Override
	public boolean isPostSelected(String listName, String postId) {
		return selectedPosts.contains(postId);
	}


	@Override
	public void onLoadMore(String listName, int lastPageId, RecyclerView.Adapter adapter, List<Post> postList) {
		scrollingListName = listName;
		pageIdToCall = lastPageId;
		loadMoreAdapter = adapter;
		scrollingPostList = postList;
		fromLoadMore = true;

		callForPosts();
	}

	//endregion


	//region == Position interface ==

	@Override
	public void saveScrollView(int position, RecyclerView scrollView) {
		if (scrollViews == null)
			scrollViews = new SparseArray<>();

		scrollViews.put(position, new WeakReference<>(scrollView));
	}

	@Override
	public void restoreScrollView(int position) {
		if (scrollViews != null && scrollViews.size() > 0 &&
				scrollViewsPositions != null && scrollViewsPositions.size() > 0) {
			if (scrollViews.size() >= position) {
				final RecyclerView hsv = scrollViews.get(position).get();
				if (hsv != null) {
					final Integer pos = scrollViewsPositions.get(position);
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							hsv.scrollToPosition(pos);
						}
					});
				}
			}
		}
	}

	//endregion


	//region == Load more methods ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return null;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return loadMoreAdapter;
	}

	@Override
	public void setData(Realm realm) {

	}

	@Override
	public void setData(JSONArray array) {
		if (postListsLoaded == null || postListsLoaded.isEmpty() ||
				!Utils.isStringValid(scrollingListName) ||
				array == null || array.length() != 1)
			return;

		JSONObject json = array.optJSONObject(0);

		if (json != null) {
			Iterator<String> iter = json.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				PostList jList = new PostList().deserializeToClass(json.optJSONObject(key));
				if (jList != null && Utils.areStringsValid(jList.getName(), scrollingListName) &&
						jList.getName().equals(scrollingListName)) {
					if (scrollingPostList != null) {
						List<Post> jPosts = jList.getPosts();
						if (jPosts != null && !jPosts.isEmpty()) {
							newItemsCount = jPosts.size();
							scrollingPostList.addAll(jPosts);
						}
					}
				}
			}
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
