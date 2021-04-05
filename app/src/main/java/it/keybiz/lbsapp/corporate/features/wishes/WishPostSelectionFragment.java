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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;
import it.keybiz.lbsapp.corporate.widgets.AutoFittingGridLayoutManager;

/**
 * A simple {@link Fragment} subclass.
 */
public class WishPostSelectionFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener,
		WishPostSelectionAdapter.OnPostTileActionListener, LoadMoreResponseHandlerTask.OnDataLoadedListener {

	public static final String LOG_TAG = WishPostSelectionFragment.class.getCanonicalName();

	private RecyclerView baseRecView;
	private GridLayoutManager glm;
	private List<Post> baseItems = new ArrayList<>();
	private WishPostSelectionAdapter selectionAdapter;

	private View shufflePosts;
	private TextView noResult;

	private boolean fromLoadMore = false;
	private int pageIdToCall = 0;
	private int newItemsCount;

	private PostTypeEnum type;
	private boolean oneSelection;

	private ArrayList<String> selectedPosts = new ArrayList<>();

	private WishListElement selectedElement;

	private Integer scrollPosition = null;


	public WishPostSelectionFragment() {
		// Required empty public constructor
	}

	public static WishPostSelectionFragment newInstance(PostTypeEnum type, boolean oneSelection) {
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, type);
		args.putBoolean(Constants.EXTRA_PARAM_2, oneSelection);
		WishPostSelectionFragment fragment = new WishPostSelectionFragment();
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

		View view = inflater.inflate(R.layout.fragment_wishes_post_selection, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() != null) {
			selectionAdapter = new WishPostSelectionAdapter(baseItems, this);
			int dim = Utils.dpToPx(R.dimen.diary_card_width, getResources());
			glm = new AutoFittingGridLayoutManager(getActivity(), dim);
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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_POST_SELECTION);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		callForPosts();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		scrollPosition = glm.findFirstCompletelyVisibleItemPosition();

		pageIdToCall = 0;
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

		outState.putSerializable(Constants.EXTRA_PARAM_1, type);
		outState.putBoolean(Constants.EXTRA_PARAM_2, oneSelection);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				type = (PostTypeEnum) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				oneSelection = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_2, false);
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
			case Constants.SERVER_OP_WISH_GET_POSTS:

				newItemsCount = responseObject.length();

				if (fromLoadMore) {
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.WISH_POSTS,
							null, null).execute(responseObject);
				}
				else
					try {
						setData(responseObject, false);
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
		baseRecView.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				fromLoadMore = true;
				callForPosts();
			}
		});

		shufflePosts = view.findViewById(R.id.shuffle_post_layout);
		shufflePosts.setOnClickListener(this);

		noResult = view.findViewById(R.id.no_result);
	}

	@Override
	protected void setLayout() {
		baseRecView.setLayoutManager(glm);
		baseRecView.setAdapter(selectionAdapter);

		restorePositions();

		if (oneSelection) {
			shufflePosts.setSelected(false);
			shufflePosts.setVisibility(View.GONE);
		}
	}

	private void setData(JSONArray response, boolean background) throws JSONException {
		if (!background) {
			if (pageIdToCall == 1 && (response == null || response.length() == 0 ||
					(response.getJSONObject(0) != null && response.getJSONObject(0).length() == 0))) {
				baseRecView.setVisibility(View.GONE);
				noResult.setText(R.string.no_posts_in_list);
				noResult.setVisibility(View.VISIBLE);
				return;
			}

			baseRecView.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);
		}

		if (pageIdToCall == 1) {
			if (baseItems == null)
				baseItems = new ArrayList<>();
			else
				baseItems.clear();

			if (response.length() == 1 && !background)
				shufflePosts.setVisibility(View.GONE);
		}
		try {
			for (int i = 0; i < response.length(); i++) {
				JSONObject json = response.getJSONObject(i);
				Post obj = new Post().returnUpdatedPost(json);
				baseItems.add(obj);
			}
			if (!background) {
				selectionAdapter.notifyDataSetChanged();

				restorePositions();
			}
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}

	private void restorePositions() {
		if (scrollPosition != null) {
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					glm.scrollToPosition(scrollPosition);
				}
			});
		}
	}

	private void callForPosts() {

		if (baseItems != null && !baseItems.isEmpty() && !fromLoadMore) return;

		Object[] result = null;

		if (baseItems == null || baseItems.isEmpty())
			pageIdToCall = 0;

		try {
			result = HLServerCalls.getPostsForWish(mUser.getUserId(), type, ++pageIdToCall);
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
	public void selectDeselect(String postId, int position) {
		if (selectedPosts == null)
			selectedPosts = new ArrayList<>();

		if (!selectedPosts.contains(postId)) {
			if (!selectedPosts.isEmpty() && oneSelection)
				activityListener.showAlert(R.string.error_wish_call_one_selection);
			else
				selectedPosts.add(postId);
		}
		else
			selectedPosts.remove(postId);

		wishesActivityListener.enableDisableNextButton(!selectedPosts.isEmpty());

		if (position > -1) selectionAdapter.notifyItemChanged(position);
	}

	@Override
	public boolean isPostSelected(String postId) {
		return selectedPosts.contains(postId);
	}

	//endregion


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
//			fromLoadMore = false;
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
