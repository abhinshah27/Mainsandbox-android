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
import it.keybiz.lbsapp.corporate.models.HLCircle;
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
public class SettingsCirclesFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener,
		BasicAdapterInteractionsListener, ListView.OnItemClickListener {

	public static final String LOG_TAG = SettingsCirclesFragment.class.getCanonicalName();

	private View title;
	private TextView addBtn;

	private ListView simpleListView;
	private List<Object> simpleList = new ArrayList<>();
	private SettingsCirclesFoldersAdapter simpleListAdapter;

	private SwipeRefreshLayout srl;
	private TextView noResult;

	private MaterialDialog dialogRemove;
	private MaterialDialog dialogAddRename;

//	private boolean goingToTimeline = false;

	private HLCircle selectedObject;
	private String newObjectName;


	public SettingsCirclesFragment() {
		// Required empty public constructor
	}

	public static SettingsCirclesFragment newInstance() {
		Bundle args = new Bundle();
		SettingsCirclesFragment fragment = new SettingsCirclesFragment();
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
					simpleList, SettingsCirclesFoldersAdapter.ViewType.CIRCLES, this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_CIRCLES);

		callServer(GET, null);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (Utils.isContextValid(getActivity()))
			Utils.closeKeyboard(getActivity());

//		if (goingToTimeline) {
//			settingsActivityListener.setToolbarVisibility(false);
//			settingsActivityListener.setBottomBarVisibility(false);
//			settingsActivityListener.addRemoveTopPaddingFromFragmentContainer(false);
//		}

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
		HLCircle circle = (HLCircle) parent.getAdapter().getItem(position);
		settingsActivityListener.showInnerCircleSingleCircleFragment(realm.copyFromRealm(circle), null);
	}

	// Adapter's custom interface callback
	@Override
	public void onItemClick(Object object) {}

	@Override
	public void onItemClick(Object object, View view) {

		if (view.getId() == R.id.btn_edit)
			showAddRenameDialog((HLCircle) object);
		else if (view.getId() == R.id.btn_remove)
			showDeleteDialog((HLCircle) object);
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
			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_GET:
				JSONObject json = responseObject.optJSONObject(0);
				if (json != null && json.length() > 0) {
					setData(json.optJSONArray("circles"));
				}
				break;

			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_ADD_DELETE:
				if (simpleList != null && simpleListAdapter != null) {
					if (mType == ADD) {
						DialogUtils.closeDialog(dialogAddRename);
						if (Utils.isStringValid(newObjectName)) {
							simpleList.add(new HLCircle(newObjectName));
							simpleListAdapter.notifyDataSetChanged();
						}
					} else if (mType == REMOVE) {
						DialogUtils.closeDialog(dialogRemove);
						if (selectedObject != null) {
							simpleList.remove(selectedObject);
							simpleListAdapter.notifyDataSetChanged();
						}
					}
				}

				realm.executeTransaction(realm -> {
					if (mType == REMOVE) {
						mUser.updateFiltersForSingleCircle(selectedObject, false);
					}
				});

				Utils.setRefreshingForSwipeLayout(srl, true);
				new Handler().postDelayed(
						() -> callServer(GET, null),
						1000
				);
				break;

			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_RENAME:
				if (selectedObject != null && Utils.isStringValid(newObjectName) && simpleList != null &&
						simpleListAdapter != null) {
					int index = simpleList.indexOf(selectedObject);
					simpleList.remove(index);
					simpleList.add(index, new HLCircle(newObjectName));
					simpleListAdapter.notifyDataSetChanged();
				}

				DialogUtils.closeDialog(dialogAddRename);

				realm.executeTransaction(realm -> {
					HLCircle circle = realm.copyFromRealm(selectedObject);

					if (mUser.getCircleObjects() != null &&
							mUser.getCircleObjects().contains(circle)) {
						int index = mUser.getCircleObjects().indexOf(circle);
						HLCircle c = mUser.getCircleObjects().get(index);
						if (c != null) {
							c.setName(newObjectName);
							c.setNameToDisplay(newObjectName);
						}
					}

					if (mUser.getCircleObjectsWithEmpty() != null &&
							mUser.getCircleObjectsWithEmpty().contains(circle)) {
						int index = mUser.getCircleObjectsWithEmpty().indexOf(circle);
						HLCircle c = mUser.getCircleObjectsWithEmpty().get(index);
						if (c != null) {
							c.setName(newObjectName);
							c.setNameToDisplay(newObjectName);
						}
					}

					if (mUser.getSelectedFeedFilters() != null &&
							mUser.getSelectedFeedFilters().contains(circle.getName())) {
						int index = mUser.getSelectedFeedFilters().indexOf(circle.getName());
						mUser.getSelectedFeedFilters().remove(index);
						mUser.getSelectedFeedFilters().add(index, newObjectName);
					}
				});

				Utils.setRefreshingForSwipeLayout(srl, true);
				new Handler().postDelayed(
						() -> callServer(GET, null),
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
//		goingToTimeline = false;

		title.setVisibility(View.VISIBLE);

		settingsActivityListener.setToolbarTitle(R.string.settings_main_inner_circle);

		addBtn.setText(R.string.settings_circles_add);

		onResumeForListData();
	}


	private void setData(final JSONArray data) {
		realm.executeTransaction(realm -> {
			if (data != null && data.length() > 0) {
				RealmList<String> list = mUser.getCircles();
				RealmList<HLCircle> listObject = mUser.getCircleObjectsWithEmpty();

				if (list == null) list = new RealmList<>();
				else list.clear();

				if (listObject == null) listObject = new RealmList<>();
				else listObject.clear();

				for (int i = 0; i < data.length(); i++) {
					JSONObject jName = data.optJSONObject(i);
					if (jName != null) {
						HLCircle circle = new HLCircle().deserializeToClass(jName);
						if (circle != null) {
							list.add(circle.getName());
							listObject.add(circle);
						}
					}
				}
			}
			else {
				mUser.setCircles(new RealmList<>());
				mUser.setCircleObjectsWithEmpty(new RealmList<>());
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

		simpleList.addAll(mUser.getCircleObjectsWithEmpty());

		simpleListAdapter.notifyDataSetChanged();

		if (simpleList.isEmpty()) {
			noResult.setText(R.string.no_result_circle);
			noResult.setVisibility(View.VISIBLE);
		}
		else noResult.setVisibility(View.GONE);

	}


	private HLServerCalls.FoldersCirclesCallType mType;
	private void callServer(HLServerCalls.FoldersCirclesCallType type, @Nullable Bundle bundle) {
		Object[] result = null;

		try {
			if (type == GET) {
				result = HLServerCalls.getSettings(mUser.getUserId(), HLServerCalls.SettingType.CIRCLES);
			}
			else if (type == RENAME && bundle != null) {
				result = HLServerCalls.settingsOperationsOnCircles(mUser.getUserId(), bundle, type);
			}
			else if ((type == ADD || type == REMOVE) && bundle != null) {
				result = HLServerCalls.settingsOperationsOnCircles(mUser.getUserId(), bundle, type);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}


	private void showAddRenameDialog(@Nullable final HLCircle objectName) {
		if (Utils.isContextValid(getContext())) {
			dialogAddRename = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_circle_folder_rename);
			if (dialogAddRename != null) {
				View view = dialogAddRename.getCustomView();
				if (view != null) {
					final EditText newName = view.findViewById(R.id.rename_circle_edittext);

					final TextView positive = DialogUtils.setPositiveButton(
							view.findViewById(R.id.button_positive),
							(selectedObject = objectName) != null ? R.string.action_rename : R.string.action_save,
							v -> {
								String sName = newName.getText().toString();
								if (Utils.isStringValid(sName)) {
									Bundle bundle = new Bundle();
									newObjectName = sName;

									if (objectName != null) {
										bundle.putString("oldCircleName", objectName.getNameToDisplay());
										bundle.putString("newCircleName", sName);
										callServer(mType = RENAME, bundle);
									} else {
										bundle.putString("circleName", sName);
										bundle.putString("operation", "a");
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
							String ic = Constants.INNER_CIRCLE_NAME_IT.trim().replaceAll("\\s", "");
							String family = Constants.CIRCLE_FAMILY_NAME_IT.trim().replaceAll("\\s", "");
							boolean condition = s != null && s.length() > 0 &&
									!s.toString().trim().replaceAll("\\s", "").equalsIgnoreCase(ic) &&
									!s.toString().trim().replaceAll("\\s", "").equalsIgnoreCase(family);

							if (objectName != null) {
								condition = s != null && s.length() > 0 &&
										!s.toString().trim().replaceAll("\\s", "").equalsIgnoreCase(ic) &&
										!s.toString().trim().replaceAll("\\s", "").equalsIgnoreCase(family) &&
										!s.toString().trim().equals(objectName.getNameToDisplay());
							}

							positive.setEnabled(condition);
						}
					});

					view.findViewById(R.id.button_negative).setOnClickListener(v -> dialogAddRename.dismiss());

					if (objectName != null) {
						newName.setText(objectName.getNameToDisplay());
						newName.setHint(R.string.settings_circle_rename_hint);
						((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.dialog_circle_title_rename);
					}
					else {
						newName.setHint(R.string.dialog_create_list_hint);
						((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.dialog_circle_title_add);
					}
				}

				dialogAddRename.show();

				DialogUtils.openKeyboardForDialog(dialogAddRename);
			}
		}
	}


	private void showDeleteDialog(final HLCircle objectName) {
		if (Utils.isContextValid(getContext()) && objectName != null) {
			selectedObject = objectName;
			dialogRemove = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_delete_circle_folder);
			if (dialogRemove != null) {
				View view = dialogRemove.getCustomView();
				if (view != null) {
					final TextView message = view.findViewById(R.id.dialog_message);

					String sMessage = getString(
							R.string.dialog_delete_circle_message,
							objectName.getNameToDisplay());
					int start = sMessage.indexOf(objectName.getNameToDisplay());
					int end = sMessage.lastIndexOf(objectName.getNameToDisplay());

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
						bundle.putString("circleName", objectName.getName());
						bundle.putString("operation", "d");

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
