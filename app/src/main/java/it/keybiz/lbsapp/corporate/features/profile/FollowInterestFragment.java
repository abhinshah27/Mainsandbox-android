/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.realm.RealmObject;
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
import it.keybiz.lbsapp.corporate.models.InterestCategory;
import it.keybiz.lbsapp.corporate.models.enums.SearchTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FollowInterestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FollowInterestFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		SearchHelper.OnQuerySubmitted {

	public static final String LOG_TAG = FollowInterestFragment.class.getCanonicalName();

//	public enum ViewType implements Serializable { HOME_PAGE, SIMILAR }
//	private ViewType mType;

	private String objectId, userName, userAvatar;

	private View tileLayoutMain, tileLayout1, tileLayout2, tileLayout3, tileLayout4, tileLayout5, tileLayout6;
	private ImageView tileMain, tile1, tile2, tile3, tile4, tile5, tile6;
	private TextView textMain, text1, text2, text3, text4, text5, text6;
	private ViewGroup categoriesContainer;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private EditText searchBox;
	private TextView noResult;

	private boolean isUser;

	private boolean automaticTransition = true;

	private AsyncTask<JSONArray, Void, Void> interestTask;
	private boolean wantsProgress;

	private SearchHelper mSearchHelper = null;


	public FollowInterestFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment InnerCircleFragment.
	 */
	public static FollowInterestFragment newInstance(/*ViewType type, */String userId, String userName, String userAvatar) {
		FollowInterestFragment fragment = new FollowInterestFragment();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, userId);
		args.putString(Constants.EXTRA_PARAM_2, userName);
		args.putString(Constants.EXTRA_PARAM_3, userAvatar);
//		args.putSerializable(Constants.EXTRA_PARAM_4, type);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		mSearchHelper = new SearchHelper(this);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_profile_follow_new_interest, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		callServer();

		if (searchBox.getText().length() < 3)
			automaticTransition = true;
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_FOLLOW_NEW);

		configureResponseReceiver();
		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (interestTask != null && !interestTask.isCancelled())
			interestTask.cancel(true);

		Utils.closeKeyboard(searchBox);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.main_interest_layout:
			case R.id.interest_1:
			case R.id.interest_2:
			case R.id.interest_3:
			case R.id.interest_4:
			case R.id.interest_5:
			case R.id.interest_6:
				if (view.getTag(view.getId()) instanceof Interest)
					goToInterestDetail(((Interest) view.getTag(view.getId())));
				break;

			case R.id.back_arrow:
				if (getActivity() != null)
					getActivity().onBackPressed();
		}
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

		outState.putString(Constants.EXTRA_PARAM_1, objectId);
		outState.putString(Constants.EXTRA_PARAM_2, userName);
		outState.putString(Constants.EXTRA_PARAM_3, userAvatar);
//		outState.putSerializable(Constants.EXTRA_PARAM_4, mType);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				objectId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				userName = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				userAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
//			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
//				mType = (ViewType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_4);

			if (Utils.isStringValid(objectId) && RealmObject.isValid(mUser))
				isUser = objectId.equals(mUser.getId());
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

		if (responseObject == null || responseObject.length() == 0) return;

		switch (operationId) {
			case Constants.SERVER_OP_GET_INTERESTS_HP:
			case Constants.SERVER_OP_GET_SIMILAR_INTERESTS:
				handleResponse(responseObject);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_GET_INTERESTS_HP:
			case Constants.SERVER_OP_GET_SIMILAR_INTERESTS:
				activityListener.showAlert(R.string.error_generic_update);
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}


	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		configureToolbar(toolbar);

		view.findViewById(R.id.focus_catcher).requestFocus();

		View searchBoxLayout = view.findViewById(R.id.search_box);
		searchBox = searchBoxLayout.findViewById(R.id.search_field);
		searchBox.setHint(R.string.global_search_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchBoxLayout, searchBox);

		noResult = view.findViewById(R.id.no_result);

		tileLayoutMain = view.findViewById(R.id.main_interest_layout);
		tileMain = tileLayoutMain.findViewById(R.id.main_interest);
		textMain = tileLayoutMain.findViewById(R.id.main_interest_name);
		tileLayout1 = view.findViewById(R.id.interest_1);
		tile1 = tileLayout1.findViewById(R.id.interest_image);
		text1 = tileLayout1.findViewById(R.id.interest_name);
		tileLayout2 = view.findViewById(R.id.interest_2);
		tile2 = tileLayout2.findViewById(R.id.interest_image);
		text2 = tileLayout2.findViewById(R.id.interest_name);
		tileLayout3 = view.findViewById(R.id.interest_3);
		tile3 = tileLayout3.findViewById(R.id.interest_image);
		text3 = tileLayout3.findViewById(R.id.interest_name);
		tileLayout4 = view.findViewById(R.id.interest_4);
		tile4 = tileLayout4.findViewById(R.id.interest_image);
		text4 = tileLayout4.findViewById(R.id.interest_name);
		tileLayout5 = view.findViewById(R.id.interest_5);
		tile5 = tileLayout5.findViewById(R.id.interest_image);
		text5 = tileLayout5.findViewById(R.id.interest_name);
		tileLayout6 = view.findViewById(R.id.interest_6);
		tile6 = tileLayout6.findViewById(R.id.interest_image);
		text6 = tileLayout6.findViewById(R.id.interest_name);

		tileLayoutMain.setOnClickListener(this);
		tileLayout1.setOnClickListener(this);
		tileLayout2.setOnClickListener(this);
		tileLayout3.setOnClickListener(this);
		tileLayout4.setOnClickListener(this);
		tileLayout5.setOnClickListener(this);
		tileLayout6.setOnClickListener(this);

		categoriesContainer = view.findViewById(R.id.items_container);
	}

	@Override
	protected void setLayout() {
//		if (mType == ViewType.HOME_PAGE) {
			if (isUser)
				toolbarTitle.setText(R.string.title_my_interests);
			else
				toolbarTitle.setText(getString(R.string.title_other_s_interests, Utils.getFirstNameForUI(userName)));
//		}
//		else
//			toolbarTitle.setText(getString(R.string.title_interest_diary, userName));

		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);

		if (searchBox.getText().length() > 0) {
			onQueryReceived(searchBox.getText().toString());
			return;
		}


		categoriesContainer.setVisibility(/*mType == ViewType.HOME_PAGE ? */View.VISIBLE/* : View.GONE*/);
//		if (mType == ViewType.HOME_PAGE) {
			List<Interest> featured = HLInterests.getInstance().getFeaturedInterests();
			if (featured != null && !featured.isEmpty()) {
				wantsProgress = false;
				for (int i = 0; i < featured.size(); i++) {
					handleSingleInterest(featured.get(i), i);
				}
			} else {
				wantsProgress = true;
				handleTilesVisibility(0);
			}

			List<InterestCategory> categories = HLInterests.getInstance().getCategories();
			categoriesContainer.removeAllViews();
			if (categories != null && !categories.isEmpty()) {
				for (InterestCategory ic : categories) {
					handleSingleCategory(ic);
				}
			}
//		}

	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		if (!query.isEmpty()) {
			if (automaticTransition) {
				profileActivityListener.showSearchFragment(query, SearchTypeEnum.INTERESTS, objectId,
						userName, userAvatar);

				automaticTransition = false;
			}
			searchBox.setText("");
		}
	}

	private void configureToolbar(Toolbar toolbar) {
		if (toolbar != null) {
			toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
			toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
			profilePicture = toolbar.findViewById(R.id.profile_picture);
		}
	}

	private void callServer() {
		Object[] result = null;
		try {
//			if (mType == ViewType.HOME_PAGE)
				result = HLServerCalls.getAllInterestsHomePage();
//			else if (mType == ViewType.SIMILAR)
//				result = HLServerCalls.getSimilarInterests(objectId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}


	private void handleResponse(JSONArray response) {

//		if (mType == ViewType.HOME_PAGE) {
			interestTask = new InterestsHomePageTask().execute(response);
//		}
//		else {
//			if (response.length() == 1) {
//				JSONObject json = response.getJSONObject(0);
//
//				HLInterests interests = HLInterests.getInstance();
//
//				JSONArray images = json.getJSONArray("imgs");
//				if (images != null) {
//					handleTilesVisibility(images.length());
//
//					if (images.length() > 0) {
//						int size = images.length();
//						if (mType == ViewType.SIMILAR)
//							size = (images.length() > 7) ? 7 : images.length();
//
//						for (int i = 0; i < size; i++) {
//							Interest interest = new Interest().deserializeToClass(images.getJSONObject(i));
//
//							if (mType == ViewType.HOME_PAGE)
//								interests.getFeaturedInterests().add(interest);
//
//							handleSingleInterest(interest, i);
//						}
//					}
//				}
//				else handleTilesVisibility(0);
//
//				String description = json.getString("description");
//				if (mType == ViewType.SIMILAR && Utils.isStringValid(description)) {
//					infoNoPost.setText(description);
//					infoNoPost.setVisibility(View.VISIBLE);
//				}
//				else
//					infoNoPost.setVisibility(View.GONE);
//			}
//		}
	}

	private void handleSingleInterest(Interest interest, int position) {
		if (interest != null && position >= 0) {
			ImageView iv;
			TextView tv;
			View v;
			switch (position) {
				case 0:
					iv = tileMain;
					v = tileLayoutMain;
					tv = textMain;
					break;
				case 1:
					iv = tile1;
					v = tileLayout1;
					tv = text1;
					break;
				case 2:
					iv = tile2;
					v = tileLayout2;
					tv = text2;
					break;
				case 3:
					iv = tile3;
					v = tileLayout3;
					tv = text3;
					break;
				case 4:
					iv = tile4;
					v = tileLayout4;
					tv = text4;
					break;
				case 5:
					iv = tile5;
					v = tileLayout5;
					tv = text5;
					break;
				case 6:
					iv = tile6;
					v = tileLayout6;
					tv = text6;
					break;

				default:
					iv = tileMain;
					v = tileLayoutMain;
					tv = textMain;
			}

			v.setTag(v.getId(), interest);
			if (Utils.isContextValid(getContext())) {
				if (Utils.isStringValid(interest.getAvatarURL())) {
					if (iv != null) {
						MediaHelper.roundPictureCorners(iv, interest.getAvatarURL());
					}
				}
			}

			if (Utils.isStringValid(interest.getName())) {
				tv.setText(interest.getName());
				tv.setVisibility(View.VISIBLE);
			} else
				tv.setVisibility(View.GONE);
		}
	}


	private void handleTilesVisibility(int length) {
		switch (length) {
			case 0:
				tileLayoutMain.setVisibility(View.GONE);
				tileLayout1.setVisibility(View.GONE);
				tileLayout2.setVisibility(View.GONE);
				tileLayout3.setVisibility(View.GONE);
				tileLayout4.setVisibility(View.GONE);
				tileLayout5.setVisibility(View.GONE);
				tileLayout6.setVisibility(View.GONE);

//				if (mType == ViewType.SIMILAR)
//					noResult.setVisibility(View.VISIBLE);
				return;
			case 1:
				tileLayoutMain.setVisibility(View.VISIBLE);
				tileLayout1.setVisibility(View.GONE);
				tileLayout2.setVisibility(View.GONE);
				tileLayout3.setVisibility(View.GONE);
				tileLayout4.setVisibility(View.GONE);
				tileLayout5.setVisibility(View.GONE);
				tileLayout6.setVisibility(View.GONE);
				break;
			case 2:
				tileLayoutMain.setVisibility(View.VISIBLE);
				tileLayout1.setVisibility(View.VISIBLE);
				tileLayout2.setVisibility(View.GONE);
				tileLayout3.setVisibility(View.GONE);
				tileLayout4.setVisibility(View.GONE);
				tileLayout5.setVisibility(View.GONE);
				tileLayout6.setVisibility(View.GONE);
				break;
			case 3:
				tileLayoutMain.setVisibility(View.VISIBLE);
				tileLayout1.setVisibility(View.VISIBLE);
				tileLayout2.setVisibility(View.VISIBLE);
				tileLayout3.setVisibility(View.GONE);
				tileLayout4.setVisibility(View.GONE);
				tileLayout5.setVisibility(View.GONE);
				tileLayout6.setVisibility(View.GONE);
				break;
			case 4:
				tileLayoutMain.setVisibility(View.VISIBLE);
				tileLayout1.setVisibility(View.VISIBLE);
				tileLayout2.setVisibility(View.VISIBLE);
				tileLayout3.setVisibility(View.VISIBLE);
				tileLayout4.setVisibility(View.GONE);
				tileLayout5.setVisibility(View.GONE);
				tileLayout6.setVisibility(View.GONE);
				break;
			case 5:
				tileLayoutMain.setVisibility(View.VISIBLE);
				tileLayout1.setVisibility(View.VISIBLE);
				tileLayout2.setVisibility(View.VISIBLE);
				tileLayout3.setVisibility(View.VISIBLE);
				tileLayout4.setVisibility(View.VISIBLE);
				tileLayout5.setVisibility(View.GONE);
				tileLayout6.setVisibility(View.GONE);
				break;
			case 6:
				tileLayoutMain.setVisibility(View.VISIBLE);
				tileLayout1.setVisibility(View.VISIBLE);
				tileLayout2.setVisibility(View.VISIBLE);
				tileLayout3.setVisibility(View.VISIBLE);
				tileLayout4.setVisibility(View.VISIBLE);
				tileLayout5.setVisibility(View.VISIBLE);
				tileLayout6.setVisibility(View.GONE);
				break;
			case 7:
				tileLayoutMain.setVisibility(View.VISIBLE);
				tileLayout1.setVisibility(View.VISIBLE);
				tileLayout2.setVisibility(View.VISIBLE);
				tileLayout3.setVisibility(View.VISIBLE);
				tileLayout4.setVisibility(View.VISIBLE);
				tileLayout5.setVisibility(View.VISIBLE);
				tileLayout6.setVisibility(View.VISIBLE);
				break;
		}

		noResult.setVisibility(View.GONE);
	}


	private void handleSingleCategory(InterestCategory category) {
		if (category != null && Utils.isContextValid(getContext())) {
			final View catView = LayoutInflater.from(getContext())
					.inflate(R.layout.item_profile_interest_cat, categoriesContainer, false);

			if (catView != null) {
				catView.setTag(category);

				catView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (catView.getTag() instanceof InterestCategory) {
							profileActivityListener.showBrowseInterestByCategoryFragment(
									((InterestCategory) catView.getTag()).getCategoryID(),
									((InterestCategory) catView.getTag()).getName()
							);
						}
					}
				});

				((TextView) catView.findViewById(R.id.category_name)).setText(category.getName());

				categoriesContainer.addView(catView);
			}
		}
	}


	// FIXME: 2/5/2018   for inconsistencies in the server responses and limitations due the use of Gson library here the ID to be used is Interest#identityId.
	private void goToInterestDetail(Interest interest) {
		String id = interest != null ? interest.getId() : null;
//		if (Utils.isStringValid(interest.getId()) && interest.getId().startsWith("int"))
//			id = interest.getId();
//		else if (Utils.isStringValid(interest.getId()) && interest.getId().startsWith("int"))
//			id = interest.getId();

		if (Utils.isStringValid(id))
			profileActivityListener.showProfileCardFragment(ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED, id);
		else
			activityListener.showGenericError();
	}



	class InterestsHomePageTask extends AsyncTask<JSONArray, Void, Void> {

		private int size = 0;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			if (getActivity() instanceof HLActivity && wantsProgress) {
				((HLActivity) getActivity()).setProgressMessage(R.string.loading_data);
				((HLActivity) getActivity()).showProgressIndicator(true);
			}
		}

		@Override
		protected Void doInBackground(JSONArray... jsonArrays) {
			try {

				JSONObject json = jsonArrays[0].getJSONObject(0);

				JSONArray images = json.getJSONArray("imgs");
				if (images != null)
					size = images.length();
				HLInterests.getInstance().setFeaturedInterests(images);

				JSONArray categories = json.getJSONArray("categories");
				HLInterests.getInstance().setGeneralCategories(categories);
//				if (categories != null && categories.length() > 0) {
//
//					for (int i = 0; i < categories.length(); i++) {
//						InterestCategory category = new InterestCategory()
//								.deserializeToClass(categories.getJSONObject(i));
//						categoriesList.add(category);
//					}
//				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			List<Interest> list = HLInterests.getInstance().getFeaturedInterests();
			handleTilesVisibility(list != null ? list.size() : 0);
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					handleSingleInterest(list.get(i), i);
				}
			}

			if (categoriesContainer != null) {
				categoriesContainer.removeAllViews();

				ConcurrentLinkedQueue<InterestCategory> categories = new ConcurrentLinkedQueue<>(HLInterests.getInstance().getCategories());
				if (/*categories != null && */!categories.isEmpty()) {
					for (InterestCategory cat : categories) {
						handleSingleCategory(cat);
					}
				}
			}

			if (getActivity() instanceof HLActivity)
				((HLActivity) getActivity()).showProgressIndicator(false);
		}
	}

}