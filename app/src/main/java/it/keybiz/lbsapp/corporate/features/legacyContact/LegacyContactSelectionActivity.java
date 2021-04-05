/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.legacyContact;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

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
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.enums.SearchTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class LegacyContactSelectionActivity extends HLActivity implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, LoadMoreResponseHandlerTask.OnDataLoadedListener,
		SearchHelper.OnQuerySubmitted, BasicAdapterInteractionsListener {

	public static final String LOG_TAG = LegacyContactSelectionActivity.class.getCanonicalName();

	private String query;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	// TODO: 4/13/2018    SEARCH IS NOW HIDDEN
	private View searchLayout;
	private EditText searchBox;

	private TextView noResult;

	private RecyclerView viewMoreRecView;
	private List<HLUserGeneric> viewMoreResults = new ArrayList<>();
	private List<HLUserGeneric> viewMoreResultsToShow = new ArrayList<>();
	private LinearLayoutManager viewMoreLlm;

	private LegacyContactAdapter viewMoreAdapter;

	private int pageIdToCall = 0;
	private boolean fromLoadMore = false;
	private int newItemCount;

	private View selectedItem;
	private MaterialDialog legacyConfirmDialog;

	private SearchHelper mSearchHelper;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_legacy_contact);
		setRootContent(R.id.root_content);

		mSearchHelper = new SearchHelper(this);

		configureLayout();

		viewMoreLlm = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
		viewMoreAdapter = new LegacyContactAdapter(viewMoreResultsToShow, this);
		viewMoreAdapter.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.SETTINGS_LEGACY_CONTACT);

		callServer(CallType.CIRCLE, null);
		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchBox);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.back_arrow:
				setResult(RESULT_CANCELED);
				finish();
				break;
		}
	}

	@Override
	public void onItemClick(Object object) {}

	@Override
	public void onItemClick(Object object, View view) {
		if (object instanceof HLUserGeneric) {
			selectedItem = view;

			final HLUserGeneric user = ((HLUserGeneric) object);
			legacyConfirmDialog =
					DialogUtils.createGenericAlertCustomView(this, R.layout.custom_dialog_legacy_contact);
			if (legacyConfirmDialog != null) {
				View v = legacyConfirmDialog.getCustomView();
				if (v != null) {
					((TextView) v.findViewById(R.id.dialog_legacy_message))
							.setText(getString(R.string.dialog_legacy_message, user.getCompleteName()));

					Button positive = v.findViewById(R.id.button_positive);
					positive.setText(R.string.ok);
					positive.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							callServer(CallType.REQUEST, user.getId());
						}
					});

					v.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							legacyConfirmDialog.dismiss();
						}
					});
				}

				legacyConfirmDialog.show();
			}
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
				if (fromLoadMore) {
					newItemCount = responseObject.length();
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.INNER_CIRCLE,
							null, null).execute(responseObject);

//					fromLoadMore = false;
				}
				else
					setData(responseObject, false);
				break;

			case Constants.SERVER_OP_SEARCH:
				if (fromLoadMore) {
					newItemCount = responseObject.length();
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.SEARCH,
							null, null).execute(responseObject);

//					fromLoadMore = false;
				}
				else
					setDataFromSearch(responseObject, false);
				break;

			case Constants.SERVER_OP_SETTINGS_LEGACY_REQUEST:
				if (selectedItem != null) {
					View reqSent = selectedItem.findViewById(R.id.request_sent);
					if (reqSent != null)
						reqSent.setVisibility(View.VISIBLE);

					DialogUtils.closeDialog (legacyConfirmDialog);

					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							setResult(Activity.RESULT_OK);
							finish();
						}
					}, 1000);
				}
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
				showAlert(R.string.error_generic_list);
				break;

			case Constants.SERVER_OP_SETTINGS_LEGACY_REQUEST:
				showGenericError();
		}
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void manageIntent() {

	}

	protected void configureLayout() {
		View toolbar = findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
		profilePicture = toolbar.findViewById(R.id.profile_picture);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);

		searchLayout = findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
		searchBox.setHint(R.string.profile_search_box_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		searchBox.setText(query);

		noResult = findViewById(R.id.no_result);
		viewMoreRecView = findViewById(R.id.generic_rv);
		viewMoreRecView.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				fromLoadMore = true;
				if (Utils.isStringValid(query))
					callServer(CallType.SEARCH, null);
				else
					callServer(CallType.CIRCLE, null);
			}
		});
	}

	private void setLayout() {
		if (Utils.isStringValid(mUser.getAvatarURL()))
			MediaHelper.loadProfilePictureWithPlaceholder(this, mUser.getAvatarURL(), profilePicture);

		toolbarTitle.setText(R.string.toolbar_legacy_title);
		noResult.setText(R.string.no_people_in_ic);

		viewMoreRecView.setLayoutManager(viewMoreLlm);
		viewMoreRecView.setAdapter(viewMoreAdapter);

		searchBox.setText(query);
		// TODO: 4/13/2018    SEARCH IS NOW HIDDEN
		if (searchLayout != null)
			searchLayout.setVisibility(View.GONE);
	}


	@Override
	public void onQueryReceived(@NonNull String query) {
		this.query = query;

		pageIdToCall = 0;

		searchBox.setText("");
	}

	private enum CallType { CIRCLE, SEARCH, REQUEST }
	private void callServer(CallType type, String legacyId) {
		Object[] result = null;

		try {
			if (type == CallType.CIRCLE)
				result = HLServerCalls.getInnerCircle(mUser.getId(), ++pageIdToCall);
			else if (type == CallType.REQUEST)
				result = HLServerCalls.sendLegacyRequest(mUser.getId(), legacyId);
			else if (type == CallType.SEARCH && Utils.isStringValid(query))
				result = HLServerCalls.search(mUser.getId(), SearchTypeEnum.INNER_CIRCLE, query, ++pageIdToCall);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance((LBSLinkApp) getApplication())
				.handleCallResult(this, this, result);
	}

	private void setData(JSONArray response, boolean background) {
		if (response == null || response.length() == 0) {
			viewMoreRecView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
			showAlert(R.string.error_generic_update);
			return;
		}

		JSONObject result = response.optJSONObject(0);
		if (result != null) {
			JSONArray lists = result.optJSONArray("lists");
			if (lists != null && lists.length() > 0) {
				JSONObject list = lists.optJSONObject(0);
				if (list != null && list.length() > 0) {
					JSONArray users = list.optJSONArray("users");

					boolean isUsersValid = users != null && users.length() > 0;
					newItemCount = isUsersValid ? users.length() : 0;

					if (pageIdToCall == 1) {
						if (!isUsersValid) {
							viewMoreRecView.setVisibility(View.GONE);
							noResult.setVisibility(View.VISIBLE);
							return;
						}

						if (viewMoreResults == null)
							viewMoreResults = new ArrayList<>();
						else
							viewMoreResults.clear();
					}
					else if (!isUsersValid) return;

					viewMoreRecView.setVisibility(View.VISIBLE);
					noResult.setVisibility(View.GONE);

					try {
						for (int i = 0; i < users.length(); i++) {
							JSONObject json = users.getJSONObject(i);
							HLUserGeneric obj = new HLUserGeneric().deserializeToClass(json);

							if (obj != null) {
								if (!mUser.isTwoStepVerificationContact(obj.getId()))
									viewMoreResults.add(obj);
							}
						}

						viewMoreResultsToShow.addAll(viewMoreResults);
						if (!background)
							viewMoreAdapter.notifyDataSetChanged();
					}
					catch (JSONException e) {
						LogUtils.e(LOG_TAG, e.getMessage(), e);
					}
				}
			}
		}
	}

	private void setDataFromSearch(JSONArray response, boolean background) {
		if (pageIdToCall == 1) {
			if (response == null || response.length() == 0) {
				viewMoreRecView.setVisibility(View.GONE);
				noResult.setVisibility(View.VISIBLE);
				return;
			}

			if (viewMoreResults == null)
				viewMoreResults = new ArrayList<>();
			else
				viewMoreResults.clear();
		}

		viewMoreRecView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		try {
			for (int i = 0; i < response.length(); i++) {
				JSONObject json = response.getJSONObject(i);
				HLUserGeneric obj = new HLUserGeneric().deserializeToClass(json);
				if (obj !=null)
					viewMoreResults.add(obj);
			}

			viewMoreResultsToShow.addAll(viewMoreResults);
			if (!background)
				viewMoreAdapter.notifyDataSetChanged();
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
		return viewMoreAdapter;
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
