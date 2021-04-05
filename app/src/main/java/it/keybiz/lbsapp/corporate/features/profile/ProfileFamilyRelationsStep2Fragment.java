/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.json.JSONArray;
import org.json.JSONException;

import io.realm.Realm;
import io.realm.RealmList;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.FamilyRelationship;
import it.keybiz.lbsapp.corporate.models.GenericUserFamilyRels;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 4/30/2018.
 */
public class ProfileFamilyRelationsStep2Fragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener {

	public static final String LOG_TAG = ProfileFamilyRelationsStep2Fragment.class.getCanonicalName();

	private GenericUserFamilyRels selectedUser;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private TextView selectionName;
	private ImageView selectionPicture;
	private View selectionIcon;

	private ViewGroup optionsContainer;

	private FamilyRelationship selectedRelation;


	public ProfileFamilyRelationsStep2Fragment() {
		// Required empty public constructor
	}

	public static ProfileFamilyRelationsStep2Fragment newInstance(GenericUserFamilyRels selectedUser) {
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, selectedUser);
		ProfileFamilyRelationsStep2Fragment fragment = new ProfileFamilyRelationsStep2Fragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_profile_family_step_2, container, false);

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

		if (getActivity() instanceof ProfileActivity)
			((ProfileActivity) getActivity()).setBackListener(null);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_FAMILY_RELATION);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				selectedUser = (GenericUserFamilyRels) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.back_arrow:
				if (Utils.isContextValid(getActivity()))
					getActivity().onBackPressed();
				break;
		}
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_ADD_TO_CIRCLE:

				if (Utils.isContextValid(getActivity())) {

					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							if (mUser.getFamilyRelationships() == null)
								mUser.setFamilyRelationships(new RealmList<FamilyRelationship>());

							if (selectedRelation != null) {
								selectedRelation.completeRelation(selectedUser);
								mUser.getFamilyRelationships().add(selectedRelation);
							}
						}
					});

					// Pop off everything up to and including the current tab
					FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
					fragmentManager.popBackStack(UserDetailFragment.LOG_TAG, 0);
//					profileActivityListener.showUserDetailFragment(mUser.getUserId());
				}

				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		@StringRes int msg = R.string.error_generic_operation;
		switch (operationId) {

		}

		activityListener.showAlert(msg);
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
	protected void configureLayout(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		profilePicture = toolbar.findViewById(R.id.profile_picture);
		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);

		View selectedUser = view.findViewById(R.id.family_member);
		selectionName = selectedUser.findViewById(R.id.name);
		selectionPicture = selectedUser.findViewById(R.id.profile_picture);
		selectionIcon = selectedUser.findViewById(R.id.check);

		optionsContainer = view.findViewById(R.id.options_container);
	}

	@Override
	protected void setLayout() {
		toolbarTitle.setText(R.string.title_family_relations);
		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), mUser.getUserAvatarURL(), profilePicture);
		selectionIcon.setVisibility(View.GONE);

		if (selectedUser != null) {
			selectionName.setText(selectedUser.getCompleteName());
			MediaHelper.loadProfilePictureWithPlaceholder(getContext(), selectedUser.getAvatarURL(), selectionPicture);

			if (optionsContainer != null && selectedUser.hasAllowedRelationships()) {
				optionsContainer.removeAllViews();

				for (final FamilyRelationship fr : selectedUser.getAllowedFamilyRels()) {

					View cell = LayoutInflater.from(optionsContainer.getContext()).inflate(R.layout.layout_custom_checkbox_dark, optionsContainer, false);
					if (cell != null) {
						((TextView) cell.findViewById(R.id.text)).setText(fr.getFamilyRelationshipName());

						cell.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								v.setSelected(true);

								if (v.getTag() instanceof FamilyRelationship)
									callServer(selectedUser, selectedRelation = ((FamilyRelationship) v.getTag()));
							}
						});

						cell.setTag(fr);
						optionsContainer.addView(cell);
					}

				}
			}
		}
	}

	private void callServer(GenericUserFamilyRels user, FamilyRelationship selected) {
		Object[] result = null;

		try {
			if (user != null && selected != null)
				result = HLServerCalls.requestAuthorizationForFamily(mUser, user, selected.getFamilyRelationshipID(), selected.getFamilyRelationshipName());
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}

}
