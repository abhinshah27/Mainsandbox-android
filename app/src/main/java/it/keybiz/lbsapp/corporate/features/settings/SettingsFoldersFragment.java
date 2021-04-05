/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;


import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

import static it.keybiz.lbsapp.corporate.connection.HLServerCalls.FoldersCirclesCallType.ADD;
import static it.keybiz.lbsapp.corporate.connection.HLServerCalls.FoldersCirclesCallType.GET;
import static it.keybiz.lbsapp.corporate.connection.HLServerCalls.FoldersCirclesCallType.REMOVE;
import static it.keybiz.lbsapp.corporate.connection.HLServerCalls.FoldersCirclesCallType.RENAME;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/21/2018.
 */
public class SettingsFoldersFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener,
		BasicAdapterInteractionsListener, ListView.OnItemClickListener {

	public static final String LOG_TAG = SettingsFoldersFragment.class.getCanonicalName();

	private View title;
	private TextView addBtn;

	private ListView simpleListView;
	private List<Object> simpleList = new ArrayList<>();
	private SettingsCirclesFoldersAdapter simpleListAdapter;

	private SwipeRefreshLayout srl;
	private View noResult;

	private MaterialDialog dialogRemove;
	private MaterialDialog dialogAddRename;

	private boolean goingToTimeline = false;

	private String selectedObject;
	private String newObjectName;


	public SettingsFoldersFragment() {
		// Required empty public constructor
	}

	public static SettingsFoldersFragment newInstance() {
		Bundle args = new Bundle();
		SettingsFoldersFragment fragment = new SettingsFoldersFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_settings_circles_folders, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(this);

		if (Utils.isContextValid(getActivity()))
			simpleListAdapter = new SettingsCirclesFoldersAdapter(getActivity(), R.layout.item_settings_circle_folder,
					simpleList, SettingsCirclesFoldersAdapter.ViewType.FOLDERS, this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_FOLDERS);

		callServer(GET, null);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (Utils.isContextValid(getActivity()))
			Utils.closeKeyboard(getActivity());

		if (goingToTimeline) {
			settingsActivityListener.setToolbarVisibility(false);
			settingsActivityListener.setBottomBarVisibility(false);
			settingsActivityListener.addRemoveTopPaddingFromFragmentContainer(false);
		}

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) { }

	@Override
	public void onBackPressed() {
		if ((dialogAddRename != null && dialogAddRename.isShowing()) ||
				(dialogRemove != null && dialogRemove.isShowing())) {

			if (dialogAddRename != null && dialogAddRename.isShowing())
				dialogAddRename.dismiss();
			if (dialogRemove != null && dialogRemove.isShowing())
				dialogRemove.dismiss();
		}
		else if (Utils.isContextValid(getActivity())) {
			if (getActivity() instanceof SettingsActivity)
				((SettingsActivity) getActivity()).setBackListener(null);

			getActivity().onBackPressed();
		}
	}



	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_add_new:
				showAddRenameDialog(null);
				break;
		}
	}

	// ListView's interface callback
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		CharSequence name = (CharSequence) parent.getAdapter().getItem(position);
		settingsActivityListener.showFoldersPostFragment((String) name);
		goingToTimeline = true;
	}

	// Adapter's custom interface callback
	@Override
	public void onItemClick(Object object) {}

	@Override
	public void onItemClick(Object object, View view) {
		if (view.getId() == R.id.btn_edit)
			showAddRenameDialog(object.toString());
		else if (view.getId() == R.id.btn_remove)
			showDeleteDialog(object.toString());
	}

	@Override
	public HLUser getUser() {
		return mUser;
	}


	@Override
	public void handleSuccessResponse(final int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		if (responseObject == null || responseObject.length() == 0) {
			handleErrorResponse(operationId, 0);
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_FOLDERS_GET:
				JSONObject json = responseObject.optJSONObject(0);
				if (json != null && json.length() > 0) {
					setData(json.optJSONArray("lists"));
				}
				break;

			case Constants.SERVER_OP_FOLDERS_CREATE:
			case Constants.SERVER_OP_FOLDERS_DELETE:
				if (simpleList != null && simpleListAdapter != null) {
					if (mType == ADD) {
						DialogUtils.closeDialog(dialogAddRename);
						if (Utils.isStringValid(newObjectName)) {
							simpleList.add(newObjectName);
							simpleListAdapter.notifyDataSetChanged();
						}
					} else if (mType == REMOVE) {

						HLPosts.getInstance().updateDeletedList(selectedObject);

						DialogUtils.closeDialog(dialogRemove);
						if (Utils.isStringValid(selectedObject)) {
							simpleList.remove(selectedObject);
							simpleListAdapter.notifyDataSetChanged();
						}
					}
				}

				Utils.setRefreshingForSwipeLayout(srl, true);
				new Handler().postDelayed(
						() -> callServer(GET, null),
						1000
				);
				break;

			case Constants.SERVER_OP_FOLDERS_RENAME:
				if (Utils.areStringsValid(selectedObject, newObjectName)) {

					HLPosts.getInstance().updateRenamedList(selectedObject, newObjectName);

					if (simpleList != null &&
							simpleListAdapter != null) {
						int index = simpleList.indexOf(selectedObject);
						simpleList.remove(index);
						simpleList.add(index, newObjectName);
						simpleListAdapter.notifyDataSetChanged();
					}
				}

				Utils.setRefreshingForSwipeLayout(srl, true);
				new Handler().postDelayed(
						() -> callServer(GET, null),
						1000
				);

				DialogUtils.closeDialog(dialogAddRename);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		@StringRes int msg = R.string.error_generic_list;
		switch (operationId) {
			case Constants.SERVER_OP_FOLDERS_GET:
				break;
			case Constants.SERVER_OP_FOLDERS_CREATE:
			case Constants.SERVER_OP_FOLDERS_DELETE:
			case Constants.SERVER_OP_FOLDERS_RENAME:
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
		title = view.findViewById(R.id.title);
		addBtn = view.findViewById(R.id.btn_add_new);
		addBtn.setOnClickListener(this);

		simpleListView = view.findViewById(R.id.circles_list);
		simpleListView.setOnItemClickListener(this);

		noResult = view.findViewById(R.id.no_result);
		srl = Utils.getGenericSwipeLayout(view, () -> {
			Utils.setRefreshingForSwipeLayout(srl, true);

			callServer(GET, null);
		});
	}

	@Override
	protected void setLayout() {

		settingsActivityListener.setToolbarVisibility(true);
		settingsActivityListener.setBottomBarVisibility(true);
		settingsActivityListener.addRemoveTopPaddingFromFragmentContainer(true);
		goingToTimeline = false;

		title.setVisibility(View.GONE);

		settingsActivityListener.setToolbarTitle(R.string.settings_main_lists);

		addBtn.setText(R.string.settings_folders_add);

		onResumeForListData();
	}


	private void setData(final JSONArray data) {
		realm.executeTransaction(realm -> {
			if (data != null && data.length() > 0) {
				RealmList<String> list = mUser.getFolders();

				if (list == null)
					list = new RealmList<>();
				else
					list.clear();

				for (int i = 0; i < data.length(); i++) {
					list.add(data.optString(i));
				}
			}
			else {
				mUser.setFolders(new RealmList<>());
			}

			onResumeForListData();
		});
	}

	private void onResumeForListData() {
		simpleListView.setAdapter(simpleListAdapter);

		if (simpleList == null)
			simpleList = new ArrayList<>();
		else
			simpleList.clear();

		simpleList.addAll(mUser.getFolders());
		simpleListAdapter.notifyDataSetChanged();

		noResult.setVisibility(simpleList.isEmpty() ? View.VISIBLE : View.GONE);
	}


	private HLServerCalls.FoldersCirclesCallType mType;
	private void callServer(HLServerCalls.FoldersCirclesCallType type, @Nullable Bundle bundle) {
		Object[] result = null;

		try {
			if (type == GET) {
				result = HLServerCalls.manageLists(HLServerCalls.ListsCallType.GET, mUser.getUserId(), null);
			}
			else if (type == RENAME && bundle != null) {
				result = HLServerCalls.manageLists(HLServerCalls.ListsCallType.RENAME, mUser.getUserId(), bundle);
			}
			else if ((type == ADD || type == REMOVE) && bundle != null) {

				if (type == ADD)
					result = HLServerCalls.manageLists(HLServerCalls.ListsCallType.CREATE, mUser.getUserId(), bundle);
				else
					result = HLServerCalls.manageLists(HLServerCalls.ListsCallType.DELETE, mUser.getUserId(), bundle);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}


	private void showAddRenameDialog(@Nullable final String objectName) {
		if (Utils.isContextValid(getContext())) {
			dialogAddRename = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_circle_folder_rename);
			if (dialogAddRename != null) {
				View view = dialogAddRename.getCustomView();
				if (view != null) {
					final EditText newName = view.findViewById(R.id.rename_circle_edittext);

					final TextView positive = DialogUtils.setPositiveButton(
							view.findViewById(R.id.button_positive),
							Utils.isStringValid(selectedObject = objectName) ?
									R.string.action_rename : R.string.action_save,
							v -> {
								String sName = newName.getText().toString();
								if (Utils.isStringValid(sName)) {
									Bundle bundle = new Bundle();
									newObjectName = sName;

									if (Utils.isStringValid(objectName)) {
										bundle.putString("oldListName", objectName);
										bundle.putString("newListName", sName);
										callServer(mType = RENAME, bundle);
									} else {
										bundle.putString("listID", sName);
										callServer(mType = ADD, bundle);
									}

								}
							});
					positive.setEnabled(false);

					newName.addTextChangedListener(new TextWatcher() {
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {}

						@Override
						public void afterTextChanged(Editable s) {
							boolean condition = s != null && s.length() > 0 &&
									!s.toString().trim().equals(objectName);

							positive.setEnabled(condition);
						}
					});

					view.findViewById(R.id.button_negative).setOnClickListener(v -> dialogAddRename.dismiss());

					if (Utils.isStringValid(objectName)) {
						newName.setText(objectName);
						newName.setHint(R.string.settings_circle_rename_hint);
						((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.dialog_folder_title_rename);
					}
					else {
						newName.setHint(R.string.dialog_create_list_hint);
						((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.dialog_folder_title_add);
					}
				}

				dialogAddRename.show();

				DialogUtils.openKeyboardForDialog(dialogAddRename);
			}
		}
	}


	private void showDeleteDialog(final String objectName) {
		if (Utils.isContextValid(getContext()) && Utils.isStringValid(objectName)) {
			selectedObject = objectName;
			dialogRemove = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_delete_circle_folder);
			if (dialogRemove != null) {
				View view = dialogRemove.getCustomView();
				if (view != null) {
					final TextView message = view.findViewById(R.id.dialog_message);

					String sMessage = getString(
							R.string.dialog_delete_folder_message,
							objectName);
					int start = sMessage.indexOf(objectName);
					int end = sMessage.lastIndexOf(objectName);

					SpannableStringBuilder spannableString = new SpannableStringBuilder(sMessage);
					spannableString.setSpan(
							new ForegroundColorSpan(
									Utils.getColor(getContext(),
											R.color.colorAccent)
							),
							start,
							end,
							Spannable.SPAN_INCLUSIVE_EXCLUSIVE
					);
					message.setText(spannableString);

					DialogUtils.setPositiveButton(view.findViewById(R.id.button_positive), R.string.action_delete, v -> {
						Bundle bundle = new Bundle();
						bundle.putString("listID", objectName);

						callServer(mType = REMOVE, bundle);
					});

					view.findViewById(R.id.button_negative).setOnClickListener(v -> dialogRemove.dismiss());
				}

				dialogRemove.show();
			}
		}
	}

	//endregion

}
