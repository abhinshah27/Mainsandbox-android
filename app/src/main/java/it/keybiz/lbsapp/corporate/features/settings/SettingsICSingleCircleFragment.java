/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

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
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileHelper;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/22/2018.
 */
public class SettingsICSingleCircleFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener,
		BasicAdapterInteractionsListener, SearchHelper.OnQuerySubmitted, LoadMoreResponseHandlerTask.OnDataLoadedListener {

	public static final String LOG_TAG = SettingsICSingleCircleFragment.class.getCanonicalName();

	private HLCircle shownCircle;
	private String filter;
	private boolean isInnerCircle, wantsFilter, isFamily;

	private TextView titleCircle;
	private View layoutSearchBox;
	private EditText searchBox;
	private String query;

	private TextView addMemberBtn;

	private RecyclerView membersView;
	private List<HLUserGeneric> membersList = new ArrayList<>();
	private List<HLUserGeneric> membersListToShow = new ArrayList<>();
	private LinearLayoutManager membersLlm;
	private SettingsSingleCircleAdapter membersAdapter;
	private TextView noResult;

	private SwipeRefreshLayout srl;

	private MaterialDialog dialogActionsOnCircle;


	// TODO: 3/22/2018    some preparation has been made BUT COMPLETE!!!
	private int pageIdToCall = 1;
	private boolean fromLoadMore = false;
	private int newItemCount;


	private HLCircle currentCircle;

	private SearchHelper mSearchHelper;


	public SettingsICSingleCircleFragment() {
		// Required empty public constructor
	}

	public static SettingsICSingleCircleFragment newInstance(@NonNull HLCircle circle, @Nullable String filter) {
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, circle);
		args.putString(Constants.EXTRA_PARAM_2, filter);
		SettingsICSingleCircleFragment fragment = new SettingsICSingleCircleFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_settings_ic_single_circle, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(null);

		if (Utils.isContextValid(getActivity())) {
			membersAdapter = new SettingsSingleCircleAdapter(membersListToShow, isInnerCircle, wantsFilter, isFamily, this);
			membersAdapter.setHasStableIds(true);

			membersLlm = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_FRIEND);

		if (Utils.isStringValid(shownCircle.getName()))
			callServer(CallType.GET, null);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchBox);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(Constants.EXTRA_PARAM_1, shownCircle);
		outState.putString(Constants.EXTRA_PARAM_2, filter);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				shownCircle = (HLCircle) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				filter = savedInstanceState.getString(Constants.EXTRA_PARAM_2);

			if (shownCircle != null && Utils.isStringValid(shownCircle.getName())) {
				isInnerCircle = shownCircle.getName().equals(Constants.INNER_CIRCLE_NAME);
				isFamily = shownCircle.getName().equals(Constants.CIRCLE_FAMILY_NAME);
			}
			wantsFilter = Utils.isStringValid(filter);
		}
	}

	@Override
	public void onBackPressed() {}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_add_new:
				if (shownCircle != null)
					settingsActivityListener.showInnerCircleSingleCircleFragment(new HLCircle(Constants.INNER_CIRCLE_NAME) {{
						setNameToDisplay(Constants.INNER_CIRCLE_NAME_IT);
					}}, shownCircle.getNameToDisplay());
				else
					activityListener.showGenericError();
				break;
		}
	}

	// Adapter's custom interface callback
	@Override
	public void onItemClick(Object object) {}

	@Override
	public void onItemClick(Object object, View view) {
		if (object instanceof HLUserGeneric) {
			if (view.getId() == R.id.check) {
				Bundle bundle = new Bundle();
				bundle.putString("friendID", ((HLUserGeneric) object).getId());
				if (isInnerCircle && wantsFilter) {
					bundle.putString("listID", filter);
					bundle.putString("operation", "a");
					mType = CallType.ADD_MEMBER;
				} else if (isInnerCircle) {
					mType = CallType.UNFRIEND;
				} else {
					bundle.putString("listID", shownCircle.getName());
					bundle.putString("operation", "d");
					mType = CallType.REMOVE_MEMBER;
				}

				createActionDialogAndShow(((HLUserGeneric) object).getCompleteName(), bundle);
			}
			else {
				ProfileActivity.openProfileCardFragment(
						getContext(),
						ProfileHelper.ProfileType.NOT_FRIEND,
						((HLUserGeneric) object).getId(),
						HomeActivity.PAGER_ITEM_PROFILE
				);
			}
		}
	}

	private void createActionDialogAndShow(final String userName, final Bundle bundle) {
		if (Utils.isContextValid(getContext())) {
			dialogActionsOnCircle = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_generic_title_text_btns);
			if (dialogActionsOnCircle != null) {
				View view = dialogActionsOnCircle.getCustomView();
				if (view != null && mType != null) {
					String title = null, message = null, positive = null;
					if (mType == CallType.ADD_MEMBER) {
						title = getString(R.string.settings_circles_add_member_title);
						message = getString(R.string.settings_circles_add_member_msg, userName, filter);
						positive = getString(R.string.add);
					} else if (mType == CallType.REMOVE_MEMBER) {
						title = getString(R.string.settings_circles_remove_member_title);
						message = getString(R.string.settings_circles_remove_member_msg, userName, shownCircle.getNameToDisplay());
						positive = getString(R.string.action_remove);
					} else if (mType == CallType.UNFRIEND) {
						title = getString(R.string.settings_circles_unfriend_title);
						message = getString(R.string.settings_circles_unfriend_msg, userName);
						positive = getString(R.string.action_remove);
					}

					if (Utils.areStringsValid(title, message, positive)) {
						((TextView) view.findViewById(R.id.dialog_title)).setText(title);
						((TextView) view.findViewById(R.id.dialog_message)).setText(message);

						Button posBtn = view.findViewById(R.id.button_positive);
						posBtn.setOnClickListener(v -> callServer(mType, bundle));
						posBtn.setText(positive);

						view.findViewById(R.id.button_negative).setOnClickListener(v -> dialogActionsOnCircle.dismiss());
					}

					dialogActionsOnCircle.show();
				}
			}
		}
	}

	@Override
	public HLUser getUser() {
		return mUser;
	}


	@Override
	public void handleSuccessResponse(final int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
				setData(responseObject, false);
				break;

			case Constants.SERVER_OP_SETTINGS_IC_UNFRIEND:
			case Constants.SERVER_OP_SETTINGS_IC_MEMBER_ADD_DELETE:
				DialogUtils.closeDialog(dialogActionsOnCircle);

				realm.executeTransaction(realm -> {
					if (operationId == Constants.SERVER_OP_SETTINGS_IC_MEMBER_ADD_DELETE) {
						HLCircle circle = new HLCircle(isInnerCircle ? filter : shownCircle.getName());

						if (mType == CallType.ADD_MEMBER) {
							mUser.updateFiltersForSingleCircle(circle, true);
						}
						else if (mType == CallType.REMOVE_MEMBER) {
							if (mUser.getCircleObjects() != null &&
									mUser.getCircleObjects().contains(circle) &&
									membersList.size() == 1) {
								mUser.getCircleObjects().remove(circle);

								if (mUser.getSelectedFeedFilters() != null) {
									mUser.getSelectedFeedFilters().remove(circle.getName());

									// INFO: 3/4/19    It's uncorrect to remove IC from filters if circle is removed or become empty
//										mUser.getSelectedFeedFilters().remove(FeedFilterTypeEnum.getCallValue(Constants.INNER_CIRCLE_NAME));
								}
							}
						}
					}
				});

				Utils.setRefreshingForSwipeLayout(srl, true);
				new Handler().postDelayed(
						() -> callServer(CallType.GET, null),
						1000
				);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		@StringRes int msg = R.string.error_generic_list;
		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_IC_GET:
				break;
			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_ADD_DELETE:
			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_RENAME:
				msg = R.string.error_generic_operation;
				break;
		}

		activityListener.showAlert(msg);
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		addMemberBtn = view.findViewById(R.id.btn_add_new);
		addMemberBtn.setOnClickListener(this);

		membersView = view.findViewById(R.id.circles_list);

		srl = Utils.getGenericSwipeLayout(view, () -> {
			Utils.setRefreshingForSwipeLayout(srl, true);

			callServer(CallType.GET, null);
		});

		layoutSearchBox = view.findViewById(R.id.search_box);
		searchBox = layoutSearchBox.findViewById(R.id.search_field);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(layoutSearchBox, searchBox);

		searchBox.setText(query);

		noResult = view.findViewById(R.id.no_result);

		titleCircle = view.findViewById(R.id.title);
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_inner_circle);

		titleCircle.setText(isInnerCircle ? shownCircle.getNameToDisplay() : getString(R.string.settings_title_circle, shownCircle.getNameToDisplay()));

		layoutSearchBox.setVisibility(isInnerCircle ? View.VISIBLE : View.GONE);

		addMemberBtn.setText(R.string.settings_circles_add_member);

		// TODO: 5/25/2018    TEMPORARILY DISABLES ADD ACTION FROM CIRCLE FAMILY
		addMemberBtn.setVisibility((isInnerCircle || isFamily) ? View.GONE : View.VISIBLE);


		membersView.setAdapter(membersAdapter);
		membersView.setLayoutManager(membersLlm);

		searchBox.setText(query);
		searchBox.setHint(getString(R.string.settings_ic_search_box_hint, shownCircle.getNameToDisplay()));
	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		this.query = query;

		if (membersList != null && !membersList.isEmpty()) {
			membersListToShow.clear();
			membersListToShow.addAll(Stream.of(membersList)
					.filter(member -> member.getName()
							.toLowerCase()
							.contains(query.toLowerCase()))
					.collect(Collectors.toList()));

			membersView.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);

			if (membersListToShow.isEmpty()) {
				membersView.setVisibility(View.GONE);
				noResult.setText(R.string.no_search_result);
				noResult.setVisibility(View.VISIBLE);
			}

			membersAdapter.notifyDataSetChanged();
		}
	}

	private void setData(final JSONArray response, boolean background) {
		if (response == null || response.length() == 0) {
			membersView.setVisibility(View.GONE);
			noResult.setText(R.string.no_member_in_circle);
			noResult.setVisibility(View.VISIBLE);
			activityListener.showAlert(R.string.error_generic_update);
			return;
		}

		JSONObject result = response.optJSONObject(0);
		if (result != null) {
			JSONArray lists = result.optJSONArray("lists");
			if (lists != null && lists.length() > 0) {
				JSONObject list = lists.optJSONObject(0);
				if (list != null && list.length() > 0) {

					currentCircle = new HLCircle().deserializeToClass(list);

//					JSONArray users = list.optJSONArray("users");

					if (currentCircle != null) {
						if (pageIdToCall == 1) {
							if (currentCircle.getUsers() == null || currentCircle.getUsers().size() == 0){
								membersView.setVisibility(View.GONE);
								noResult.setText(R.string.no_member_in_circle);
								noResult.setVisibility(View.VISIBLE);
								return;
							}

							if (membersList == null)
								membersList = new ArrayList<>();
							else
								membersList.clear();
							if (membersListToShow == null)
								membersListToShow = new ArrayList<>();
							else
								membersListToShow.clear();
						}

						membersView.setVisibility(View.VISIBLE);
						noResult.setVisibility(View.GONE);

//						try {
						membersList.addAll(currentCircle.getUsers());
//							for (int i = 0; i < users.length(); i++) {
//								JSONObject json = users.getJSONObject(i);
//								HLUserGeneric obj = new HLUserGeneric().deserializeToClass(json);
//								if (obj != null)
//									membersList.add(obj);
//							}

						membersListToShow.addAll(membersList);
						if (!background)
							membersAdapter.notifyDataSetChanged();
//						} catch (JSONException e) {
//							LogUtils.e(LOG_TAG, e.getMessage(), e);
//						}

						if (!isInnerCircle && !isFamily) {
							realm.executeTransaction(realm -> mUser.updateFiltersForSingleCircle(currentCircle, true));
						}

					} else {
						membersView.setVisibility(View.GONE);
						noResult.setText(R.string.no_member_in_circle);
						noResult.setVisibility(View.VISIBLE);

						activityListener.showGenericError();
					}
				}
			} else {
				membersView.setVisibility(View.GONE);
				noResult.setText(R.string.no_member_in_circle);
				noResult.setVisibility(View.VISIBLE);

				if (!isInnerCircle && !isFamily && currentCircle != null) {
					currentCircle.setUsers(null);
					realm.executeTransaction(realm -> {
						if (mUser.getCircleObjects() != null &&
								mUser.getCircleObjects().contains(currentCircle) &&
								!currentCircle.hasMembers()) {
							mUser.getCircleObjects().remove(currentCircle);
						}

						if (mUser.getSelectedFeedFilters() != null) {
							mUser.getSelectedFeedFilters().remove(currentCircle.getName());

							// INFO: 3/4/19    It's uncorrect to remove IC from filters if circle is removed or become empty
//								mUser.getSelectedFeedFilters().remove(FeedFilterTypeEnum.getCallValue(Constants.INNER_CIRCLE_NAME));
						}

					});
				}
			}
		}
	}



	public enum CallType { GET, ADD_MEMBER, REMOVE_MEMBER, UNFRIEND }
	private CallType mType;
	private void callServer(CallType type, @Nullable Bundle bundle) {
		Object[] result = null;

		try {
			// TODO: 3/22/2018    restore PAGINATION
			pageIdToCall = 0;

			if (type == CallType.GET) {
				if (isInnerCircle) {
					result = HLServerCalls.getInnerCircle(
							mUser.getUserId(),
							wantsFilter ? filter : "",
							++pageIdToCall
					);
				}
				else
					result = HLServerCalls.getCircle(mUser.getUserId(), shownCircle.getName(), ++pageIdToCall);
			}
			else if (type != null && bundle != null)
				result = HLServerCalls.settingsOperationsOnSingleCircle(mUser.getUserId(), bundle, type);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}


	private void setDataFromSearch(JSONArray response, boolean background) {
		if (pageIdToCall == 1) {
			if (response == null || response.length() == 0) {
				membersView.setVisibility(View.GONE);
				noResult.setVisibility(View.VISIBLE);
				return;
			}

			if (membersList == null)
				membersList = new ArrayList<>();
			else
				membersList.clear();
		}

		membersView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		try {
			for (int i = 0; i < response.length(); i++) {
				JSONObject json = response.getJSONObject(i);
				HLUserGeneric obj = new HLUserGeneric().deserializeToClass(json);
				if (obj !=null)
					membersList.add(obj);
			}

			membersListToShow.addAll(membersList);
			if (!background)
				membersAdapter.notifyDataSetChanged();
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
		return membersAdapter;
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


//	private void showAddRenameDialog(@Nullable final String circleName) {
//		if (Utils.isContextValid(getContext())) {
//			dialogCircleAddRename = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_circle_rename);
//			if (dialogCircleAddRename != null) {
//				View view = dialogCircleAddRename.getCustomView();
//				if (view != null) {
//					final EditText newName = view.findViewById(R.id.rename_circle_edittext);
//
//					final TextView positive = DialogUtils.setPositiveButton(
//							(TextView) view.findViewById(R.id.button_positive),
//							Utils.isStringValid(circleName) ?
//									R.string.action_rename : R.string.action_save,
//							new View.OnClickListener() {
//								@Override
//								public void onClick(View v) {
//									String sName = newName.getText().toString();
//									if (Utils.isStringValid(sName)) {
//										Bundle bundle = new Bundle();
//
//										if (Utils.isStringValid(circleName)) {
//											bundle.putString("oldCircleName", circleName);
//											bundle.putString("newCircleName", sName);
//											callServer(mType = CallType.RENAME, bundle);
//										}
//										else {
//											bundle.putString("circleName", sName);
//											bundle.putString("operation", "a");
//											callServer(mType = CallType.ADD, bundle);
//										}
//									}
//								}
//							});
//					positive.setEnabled(false);
//
//					newName.addTextChangedListener(new TextWatcher() {
//						@Override
//						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//						@Override
//						public void onTextChanged(CharSequence s, int start, int before, int count) {}
//
//						@Override
//						public void afterTextChanged(Editable s) {
//							boolean condition = s != null && s.length() > 0 &&
//									!s.toString().trim().equalsIgnoreCase(Constants.INNER_CIRCLE_NAME);
//
//							if (circleName != null)
//								condition = s != null && s.length() > 0 &&
//										!s.toString().trim().equalsIgnoreCase(Constants.INNER_CIRCLE_NAME) &&
//										!s.toString().trim().equals(circleName);
//
//							positive.setEnabled(condition);
//						}
//					});
//
//					view.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							dialogCircleAddRename.dismiss();
//						}
//					});
//
//					if (Utils.isStringValid(circleName)) {
//						newName.setText(circleName);
//						newName.setHint(R.string.settings_circle_rename_hint);
//						((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.dialog_circle_title_rename);
//					}
//					else {
//						newName.setHint(R.string.dialog_create_list_hint);
//						((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.dialog_circle_title_add);
//					}
//				}
//
//				dialogCircleAddRename.show();
//
//				DialogUtils.openKeyboardForDialog(dialogCircleAddRename);
//			}
//		}
//	}
//
//
//	private void showDeleteDialog(final String circleName) {
//		if (Utils.isContextValid(getContext()) && Utils.isStringValid(circleName)) {
//			dialogCircleRemove = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_delete_circle);
//			if (dialogCircleRemove != null) {
//				View view = dialogCircleRemove.getCustomView();
//				if (view != null) {
//					final TextView message = view.findViewById(R.id.dialog_message);
//
//					String sMessage = getString(R.string.dialog_delete_circle_message, circleName);
//					int start = sMessage.indexOf(circleName);
//					int end = sMessage.lastIndexOf(circleName);
//
//					SpannableStringBuilder spannableString = new SpannableStringBuilder(sMessage);
//					spannableString.setSpan(
//							new ForegroundColorSpan(
//									Utils.getColor(getContext(),
//											R.color.colorAccent)
//							),
//							start,
//							end,
//							Spannable.SPAN_INCLUSIVE_EXCLUSIVE
//					);
//					message.setText(spannableString);
//
//					DialogUtils.setPositiveButton((TextView) view.findViewById(R.id.button_positive), R.string.action_delete, new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							Bundle bundle = new Bundle();
//							bundle.putString("circleName", circleName);
//							bundle.putString("operation", "d");
//							callServer(mType = CallType.REMOVE, bundle);
//						}
//					});
//
//					view.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							dialogCircleRemove.dismiss();
//						}
//					});
//				}
//
//				dialogCircleRemove.show();
//			}
//		}
//	}

	//endregion

}
