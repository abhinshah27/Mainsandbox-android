/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.annimon.stream.function.Predicate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
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
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListenerWithErrorDescription;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
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
public class SettingsSecurityLegacy2StepFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListenerWithErrorDescription, OnMissingConnectionListener, OnBackPressedListener,
		BasicAdapterInteractionsListener, SearchHelper.OnQuerySubmitted, LoadMoreResponseHandlerTask.OnDataLoadedListener {

	public static final String LOG_TAG = SettingsSecurityLegacy2StepFragment.class.getCanonicalName();

	private boolean isSelection;
	private ArrayList<String> filters = new ArrayList<>();

	private TextView titleCircle;
	private View layoutSearchBox;
	private EditText searchBox;
	private String query;

	private TextView addMemberBtn;

	private RecyclerView membersView;
	private List<HLUserGeneric> membersList = new RealmList<>();
	private List<HLUserGeneric> membersListToShow = new ArrayList<>();
	private LinearLayoutManager membersLlm;
	private Settings2StepContactsAdapter membersAdapter;
	private TextView noResult;

	private SwipeRefreshLayout srl;

	private MaterialDialog dialogAddRemoveContact;

	private HLUserGeneric selectedContact;

	// TODO: 3/22/2018    some preparation has been made BUT COMPLETE!!!
	private int pageIdToCall = 1;
	private boolean fromLoadMore = false;
	private int newItemCount;

	private SearchHelper mSearchHelper;


	public SettingsSecurityLegacy2StepFragment() {
		// Required empty public constructor
	}

	public static SettingsSecurityLegacy2StepFragment newInstance(boolean isSelection,
	                                                              @Nullable ArrayList<String> filters) {
		Bundle args = new Bundle();
		args.putBoolean(Constants.EXTRA_PARAM_1, isSelection);
		args.putStringArrayList(Constants.EXTRA_PARAM_2, filters);
		SettingsSecurityLegacy2StepFragment fragment = new SettingsSecurityLegacy2StepFragment();
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
			membersAdapter = new Settings2StepContactsAdapter(membersListToShow, isSelection, filters, this);
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

		AnalyticsUtils.trackScreen(getContext(), isSelection ? AnalyticsUtils.SETTINGS_2_STEP_CONTACTS_SELECTION : AnalyticsUtils.SETTINGS_2_STEP_CONTACTS_VIEW);

		callServer(isSelection ? CallType.GET_IC : CallType.GET_SELECTED, null);

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

		outState.putBoolean(Constants.EXTRA_PARAM_1, isSelection);
		outState.putStringArrayList(Constants.EXTRA_PARAM_2, filters);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				isSelection = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_1, false);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2)) {
				List<String> filters = savedInstanceState.getStringArrayList(Constants.EXTRA_PARAM_2);

				if (filters != null && !filters.isEmpty()) {
					if (this.filters == null)
						this.filters = new ArrayList<>();
					else
						this.filters.clear();

					this.filters.addAll(filters);
				}
			}
		}
	}

	@Override
	public void onBackPressed() {}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_add_new:
				settingsActivityListener.showSecurityLegacy2StepFragment(true, filters);
				break;
		}
	}

	// Adapter's custom interface callback
	@Override
	public void onItemClick(Object object) {
		if (object instanceof HLUserGeneric) {
			selectedContact = (HLUserGeneric) object;

			showAddDeleteDialog(((HLUserGeneric) object).getCompleteName());
		}
	}

	@Override
	public void onItemClick(Object object, View view) {}

	@Override
	public HLUser getUser() {
		return mUser;
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
			case Constants.SERVER_OP_SETTINGS_SECURITY_GET_2_STEP_CONTACTS:
				setData(responseObject, false);
				break;

			case Constants.SERVER_OP_SETTINGS_ADD_REMOVE_2_STEP_CONTACT:
				DialogUtils.closeDialog(dialogAddRemoveContact);

				if (!isSelection)
					callServer(CallType.GET_SELECTED, null);
				else {
					filters.add(selectedContact.getId());
					membersAdapter.applyFilter();
				}
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode, String description) {
		Utils.setRefreshingForSwipeLayout(srl, false);

		@StringRes int msg = R.string.error_generic_list;
		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
			case Constants.SERVER_OP_SETTINGS_SECURITY_GET_2_STEP_CONTACTS:
				break;
			case Constants.SERVER_OP_SETTINGS_ADD_REMOVE_2_STEP_CONTACT:
				if (errorCode == Constants.SERVER_ERROR_SETTINGS_5PL_2_STEP_CONTACTS) {
					activityListener.showAlert(description);
					return;
				}
		}

		activityListener.showAlert(msg);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
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

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);

				callServer(isSelection ? CallType.GET_IC : CallType.GET_SELECTED, null);
			}
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
		settingsActivityListener.setToolbarTitle(R.string.settings_main_security);

		titleCircle.setText(R.string.settings_security_lct_2step_title);

		layoutSearchBox.setVisibility(isSelection ? View.VISIBLE : View.GONE);

		addMemberBtn.setText(R.string.settings_security_2_step_add);
		addMemberBtn.setVisibility(isSelection ? View.GONE : View.VISIBLE);


		membersView.setAdapter(membersAdapter);
		membersView.setLayoutManager(membersLlm);

		searchBox.setText(query);
		searchBox.setHint(R.string.search_by_name);

		if (!isSelection) {
			membersList.addAll(mUser.getTwoStepVerificationContacts());

			if (!membersList.isEmpty()) {
				membersView.setVisibility(View.VISIBLE);
				noResult.setVisibility(View.GONE);

				membersListToShow.addAll(membersList);
				membersAdapter.notifyDataSetChanged();
			}
			else {
				membersView.setVisibility(View.GONE);
				noResult.setText(R.string.no_2_step_contact);
				noResult.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		this.query = query;

		if (membersList != null && !membersList.isEmpty()) {
			membersListToShow.clear();
			membersListToShow.addAll(Stream.of(membersList).filter(new Predicate<HLUserGeneric>() {
				@Override
				public boolean test(HLUserGeneric member) {
					return member.getName().toLowerCase().contains(query.toLowerCase());
				}
			}).collect(Collectors.toList()));

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

	/**
	 * Used only when the fragment is in getInnerCircleMode.
	 *
	 * @param response the received JSON response from server.
	 * @param background if the method is entered from a background thread after a load more (used later for pagination).
	 */
	private void setData(final JSONArray response, boolean background) {
		if (response == null || response.length() == 0) {
			membersView.setVisibility(View.GONE);
			noResult.setText(
					isSelection ? R.string.no_member_in_circle : R.string.no_2_step_contact
			);
			noResult.setVisibility(View.VISIBLE);
			if (isSelection)
				activityListener.showAlert(R.string.error_generic_update);
			else {
				if (filters != null)
					filters.clear();

				if (mUser.hasLegacyContact())
					filters.add(mUser.getLegacyContact().getId());

				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						mUser.getSettings().setTwoStepVerificationContacts(new RealmList<HLUserGeneric>());
					}
				});
			}
			return;
		}

		JSONArray users = response;
		if (isSelection) {
			JSONObject result = response.optJSONObject(0);
			if (result != null) {
				JSONArray lists = result.optJSONArray("lists");
				if (lists != null && lists.length() > 0) {
					JSONObject list = lists.optJSONObject(0);
					if (list != null && list.length() > 0)
						users = list.optJSONArray("users");
				}
			}
		}

		if (pageIdToCall == 1) {
			if (users == null || users.length() == 0) {
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

			if (filters == null)
				filters = new ArrayList<>();
			else if (!isSelection) {
				filters.clear();

				if (mUser.hasLegacyContact())
					filters.add(mUser.getLegacyContact().getId());
			}
		}

		membersView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		try {

			if (!isSelection) {
				final JSONArray jUsers = users;
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						if (jUsers != null && jUsers.length() > 0) {
							mUser.getSettings().setTwoStepVerificationContacts(new RealmList<HLUserGeneric>());

							try {
								for (int i = 0; i < jUsers.length(); i++) {
									JSONObject json = jUsers.getJSONObject(i);
									HLUserGeneric obj = new HLUserGeneric().deserializeToClass(json);
									if (obj != null) {
										if (filters != null)
											filters.add(obj.getId());

										membersList.add(obj);

										mUser.getSettings().getTwoStepVerificationContacts().add(obj);
									}
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
			else {
				for (int i = 0; i < users.length(); i++) {
					JSONObject json = users.getJSONObject(i);
					HLUserGeneric obj = new HLUserGeneric().deserializeToClass(json);
					if (obj != null) {
						if (filters != null && filters.contains(obj.getId()))
							continue;

						membersList.add(obj);
					}
				}
			}

			membersListToShow.addAll(membersList);

			if (!background) {
				membersAdapter.notifyDataSetChanged();
				addMemberBtn.setVisibility((isSelection || (membersListToShow != null && membersListToShow.size() >= 5)) ? View.GONE : View.VISIBLE);
			}
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
	}



	public enum CallType { GET_SELECTED, GET_IC, ADD_2_STEP, REMOVE_2_STEP }
	private void callServer(CallType type, @Nullable String contactId) {
		Object[] result = null;

		try {
			if (type == CallType.GET_IC) {
				result = HLServerCalls.getInnerCircle(
						mUser.getUserId(),
						pageIdToCall
				);
			}
			else if (type == CallType.GET_SELECTED)
				result = HLServerCalls.get2StepVerificationContacts(mUser.getUserId());
			else if (type != null && Utils.isStringValid(contactId)) {
				result = HLServerCalls.settingsOperationsOn2StepContacts(mUser.getUserId(), contactId, type);
			}
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


	private void showAddDeleteDialog(String contactName) {
		if (Utils.isContextValid(getContext()) && Utils.isStringValid(contactName)) {
			dialogAddRemoveContact = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_add_remove_2_step_user);
			if (dialogAddRemoveContact != null) {
				View view = dialogAddRemoveContact.getCustomView();
				if (view != null) {
					((TextView) view.findViewById(R.id.dialog_title)).setText(
							isSelection ? R.string.dialog_add_remove_2_step_title_a : R.string.dialog_add_remove_2_step_title_r
					);
					((TextView) view.findViewById(R.id.dialog_message)).setText(
							getString(isSelection ? R.string.dialog_add_remove_2_step_message_a : R.string.dialog_add_remove_2_step_message_r, contactName)
					);

					DialogUtils.setPositiveButton(
							view.findViewById(R.id.button_positive),
							isSelection ? R.string.add : R.string.action_remove,
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									callServer((isSelection ? CallType.ADD_2_STEP : CallType.REMOVE_2_STEP), selectedContact.getId());
								}
							}
					);

					view.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dialogAddRemoveContact.dismiss();
						}
					});
				}

				dialogAddRemoveContact.show();
			}
		}
	}

	//endregion

}
