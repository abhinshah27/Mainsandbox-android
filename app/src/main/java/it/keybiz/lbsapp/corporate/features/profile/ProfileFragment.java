/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.settings.SettingsICSingleCircleFragment;
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.SDKTokenUtils;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLInterests;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.enums.UnBlockUserEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ProfileFragment.OnProfileFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends HLFragment implements ProfileHelper.OnProfileInteractionsListener,
		OnMissingConnectionListener, View.OnClickListener {

	private ProfileHelper profileHelper;

	private boolean viewCreated = false;

	private ProfileHelper.ProfileType profileType;
	private String objectId;
	private HLUserGeneric userGeneric;
	private Interest interest;
	private int bottomBarSelItem  = -1;

	private OnProfileFragmentInteractionListener mListener;

	private MaterialDialog preferDialog;
	private MaterialDialog moderationDialog;

	private boolean isRequestingAuthorizationToIC = false;


	public ProfileFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment ProfileFragment.
	 */
	public static ProfileFragment newInstance(ProfileHelper.ProfileType profileType, String personId,
	                                          int bottomBarSetItem) {
		ProfileFragment fragment = new ProfileFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, profileType);
		args.putString(Constants.EXTRA_PARAM_2, personId);
		args.putInt(Constants.EXTRA_PARAM_4, bottomBarSetItem);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		profileHelper = new ProfileHelper(this);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.home_fragment_profile, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		callServer(CallType.PROFILE, null);
		setLayout();
	}

	@Override
	public void onPause() {
		onSaveInstanceState(new Bundle());

		LBSLinkApp.notificationsFragmentVisible = false;

		super.onPause();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {}
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		viewCreated = true;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(Constants.EXTRA_PARAM_1, profileType);
		outState.putString(Constants.EXTRA_PARAM_2, objectId);
		outState.putBoolean(Constants.EXTRA_PARAM_3, viewCreated);
		outState.putInt(Constants.EXTRA_PARAM_4, bottomBarSelItem);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				profileType = (ProfileHelper.ProfileType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				objectId = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				viewCreated = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_3);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				bottomBarSelItem = savedInstanceState.getInt(Constants.EXTRA_PARAM_4);
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == Constants.RESULT_SELECT_IDENTITY) {
			if (resultCode == Activity.RESULT_OK) {
				profileType = mUser.getProfileType();

				callServer(CallType.PROFILE, null);
			}
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (responseObject == null || responseObject.length() == 0) return;

		final JSONObject jsonObject = responseObject.optJSONObject(0);
		switch (operationId) {
			case Constants.SERVER_OP_GET_USER_PROFILE:
				if (RealmUtils.isValid(realm)) {
					realm.executeTransactionAsync(
							realm -> {

								if (jsonObject != null && jsonObject.has("_id")) {

									realm.createOrUpdateObjectFromJson(HLUser.class, jsonObject);

//								mUser = RealmUtils.checkAndFetchUser(realm, mUser);

									HLUser user = new HLUser().readUser(realm);
									user.updateFiltersForSingleCircle(
											new HLCircle(Constants.CIRCLE_FAMILY_NAME),
											user.hasAuthorizedFamilyRelationships()
									);
								}
							},
							() -> {             // onSuccess
								if (viewCreated) {
									setLayout();
									viewCreated = false;
								}
							}
					);

					realm = RealmUtils.checkAndFetchRealm(realm);
					mUser = RealmUtils.checkAndFetchUser(realm, mUser);
				}
				break;

			case Constants.SERVER_OP_GET_PERSON_V2:
				userGeneric = new HLUserGeneric().deserializeComplete(jsonObject, false);
				userGeneric.setId(objectId);
				profileType = userGeneric.isFriend() ? ProfileHelper.ProfileType.FRIEND : ProfileHelper.ProfileType.NOT_FRIEND;

				// if is Friend call for Twilio token
				if (profileType == ProfileHelper.ProfileType.FRIEND) {
					SDKTokenUtils.init(getContext(), mUser.getUserId(), token -> profileHelper.setCallsToken(token));
				}

				if (viewCreated) {
					setLayout();
					viewCreated = false;
				}
				break;

			case Constants.SERVER_OP_ADD_TO_CIRCLE:
				isRequestingAuthorizationToIC = false;
				profileHelper.showAuthorizationRequestResult(true);
				break;

			case Constants.SERVER_OP_GET_INTEREST_PROFILE:
				if (profileType == ProfileHelper.ProfileType.INTEREST_ME) {
					if (RealmUtils.isValid(realm)) {
						realm.executeTransaction(new Realm.Transaction() {
							@Override
							public void execute(@NonNull Realm realm) {
								interest = realm.createOrUpdateObjectFromJson(Interest.class, jsonObject);

								if (viewCreated) {
									setLayout();
									viewCreated = false;
								}
							}
						});
					}
				}
				else {
					interest = new Interest().deserializeToClass(jsonObject);
					if (interest != null) {
						profileType = interest.isClaimed() ?
								ProfileHelper.ProfileType.INTEREST_CLAIMED : ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED;

						// TODO: 1/25/2018     UPDATING MAY CAUSE HARM
						if (HLInterests.getInstance().hasInterest(interest.getId()))
							HLInterests.getInstance().setInterest(interest);
					}
				}
				if (viewCreated) {
					setLayout();
					viewCreated = false;
				}
				break;

			case Constants.SERVER_OP_INTEREST_FOLLOW_UNFOLLOW:
				if (interest != null) {
					interest.setFollowed(!interest.isFollowed());
					interest.setTotFollowers(interest.getTotFollowers() + (interest.isFollowed() ? +1 : -1));

					// TODO: 1/25/2018     UPDATING MAY CAUSE HARM
					if (HLInterests.getInstance().hasInterest(interest.getId())) {
						if (interest.isFollowed())
							HLInterests.getInstance().setInterest(interest);
						else {
							HLInterests.getInstance().removeInterest(interest.getId());

							// deletes preferred interest after un-following if it was preferred
							if (interest.isPreferred()) {
								interest.setPreferred(false);
								realm.executeTransaction(new Realm.Transaction() {
									@Override
									public void execute(@NonNull Realm realm) {
										if (mUser != null) {
											mUser.setHasAPreferredInterest(false);

											if (mUser.getSettings() != null)
												mUser.getSettings().setPreferredInterest(null);
										}
									}
								});
							}

							if (jsonObject.has("userID"))
								HLPosts.getInstance().deletePostsByAuthorId(jsonObject.optString("userID"), null);
						}
					}
					setLayout();
				}
				break;

			case Constants.SERVER_OP_SET_AS_PREFERRED:
				DialogUtils.closeDialog(preferDialog);

				if (interest != null) {
					interest.setPreferred(!interest.isPreferred());
					setLayout();

					// TODO: 1/25/2018     UPDATING MAY CAUSE HARM
					if (HLInterests.getInstance().hasInterest(interest.getId()))
						HLInterests.getInstance().setInterest(interest);

					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							if (mUser != null) {
								mUser.setHasAPreferredInterest(interest.isPreferred());

								if (mUser.getSettings() != null) {
									mUser.getSettings().setPreferredInterest(
											interest.isPreferred() ? realm.copyToRealmOrUpdate(interest) : null
									);
								}
							}
						}
					});

				}
				break;

			case Constants.SERVER_OP_SETTING_BLOCK_UNBLOCK_USER:
			case Constants.SERVER_OP_SETTINGS_IC_UNFRIEND:
			case Constants.SERVER_OP_REPORT_USER:

				if (operationId == Constants.SERVER_OP_SETTING_BLOCK_UNBLOCK_USER &&
						Utils.isContextValid(getActivity())) {
					getActivity().finish();
				}
				else if (operationId == Constants.SERVER_OP_SETTINGS_IC_UNFRIEND) {
					userGeneric.setFriend(false);

					if (profileHelper != null) {
						profileHelper.setLayout(profileType = ProfileHelper.ProfileType.NOT_FRIEND, userGeneric, null, bottomBarSelItem);
					}
				}

				DialogUtils.closeDialog(moderationDialog);
				break;

			case Constants.SERVER_OP_CHAT_INITIALIZE_ROOM:
				if (profileHelper != null && profileHelper.isRoomInitializationCalled()) {
					if (responseObject.length() > 0)
						profileHelper.handleRoomInitialization(responseObject);
					else
						handleErrorResponse(operationId, 0);

					if (profileHelper != null)
						profileHelper.setRoomInitializationCalled(false);
				}
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_GET_USER_PROFILE:
			case Constants.SERVER_OP_GET_PERSON_V2:
			case Constants.SERVER_OP_GET_INTEREST_PROFILE:
				activityListener.showAlert(R.string.error_generic_update);
				break;

			case Constants.SERVER_OP_ADD_TO_CIRCLE:
				isRequestingAuthorizationToIC = false;
				activityListener.showAlert(R.string.error_ic_authorization_request);
				profileHelper.showAuthorizationRequestResult(false);
				break;

			case Constants.SERVER_OP_INTEREST_FOLLOW_UNFOLLOW:
			case Constants.SERVER_OP_SET_AS_PREFERRED:

			case Constants.SERVER_OP_SETTING_BLOCK_UNBLOCK_USER:
			case Constants.SERVER_OP_SETTINGS_IC_UNFRIEND:
			case Constants.SERVER_OP_REPORT_USER:

			case Constants.SERVER_OP_CHAT_INITIALIZE_ROOM:
				activityListener.showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {

		if (isRequestingAuthorizationToIC && operationId == Constants.SERVER_OP_ADD_TO_CIRCLE) {
			isRequestingAuthorizationToIC = false;
			profileHelper.handleMissingConnectionForInviteButton();
		}

	}


	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnProfileFragmentInteractionListener {
		void onProfileFragmentInteraction(Uri uri);
	}


	//region == PROFILE HELPER INTERFACE ==

	@Override @NonNull
	public HLUser getUser() {
		if (mUser != null)
			return mUser;
		else {
			if (!RealmUtils.isValid(realm))
				realm = RealmUtils.getCheckedRealm();

			return new HLUser().readUser(realm);
		}
	}

	@Override
	public void goToMyUserDetails() {
		if (profileType == ProfileHelper.ProfileType.ME)
			ProfileActivity.openUserDetailFragment(getContext());
		else if (profileType == ProfileHelper.ProfileType.FRIEND && userGeneric != null &&
				profileActivityListener != null)
			profileActivityListener.showUserDetailFragment(userGeneric.getId());
	}

	@Override
	public void goToDiary() {
		String id = profileType == ProfileHelper.ProfileType.ME ? mUser.getId() : objectId;

		if (getActivity() instanceof HomeActivity)
			ProfileActivity.openDiaryFragment(getContext(), id, mUser.getCompleteName(), mUser.getAvatarURL());
		else if (getActivity() instanceof ProfileActivity && userGeneric != null)
			profileActivityListener.showDiaryFragment(id, userGeneric.getCompleteName(), userGeneric.getAvatarURL());
	}

	@Override
	public void goToDiaryForInterest() {
		String id = null;
		if (profileType == ProfileHelper.ProfileType.INTEREST_ME)
			id = mUser.getId();
		else if (interest != null)
			id = interest.getId();

		if (Utils.isStringValid(id)) {
			if (getActivity() instanceof HomeActivity)
				ProfileActivity.openDiaryFragment(getContext(), id, mUser.getCompleteName(), mUser.getAvatarURL());
			else if (getActivity() instanceof ProfileActivity && interest != null) {
				if (interest.isEmptyDiary())
					profileActivityListener.showSimilarEmptyDiaryFragment(id, interest.getName(), interest.getAvatarURL());
				else
					profileActivityListener.showDiaryFragment(id, interest.getName(), interest.getAvatarURL());
			}
		}
	}

	@Override
	public void goToInnerCircle() {
		String id = profileType == ProfileHelper.ProfileType.ME ? mUser.getId() : objectId;
		if (getActivity() instanceof HomeActivity)
			ProfileActivity.openInnerCircleFragment(getContext(), id, mUser.getCompleteName(), mUser.getAvatarURL());
		else if (getActivity() instanceof ProfileActivity && userGeneric != null)
			profileActivityListener.showInnerCircleFragment(id, userGeneric.getCompleteName(), userGeneric.getAvatarURL());
	}

	@Override
	public void goToInnerInterests() {
		String id = null, name = null, avatar = null;
		if (profileType == ProfileHelper.ProfileType.ME) {
			id = mUser.getId();
			name = mUser.getCompleteName();
			avatar = mUser.getAvatarURL();
		}
		else if (userGeneric != null){
			id = userGeneric.getId();
			name = userGeneric.getCompleteName();
			avatar = userGeneric.getAvatarURL();
		}

		if (Utils.isStringValid(id)) {
			if (getActivity() instanceof HomeActivity)
				ProfileActivity.openMyInterestsFragment(getContext(), id, name, avatar);
			else if (getActivity() instanceof ProfileActivity)
				profileActivityListener.showInterestsFragment(id, name, avatar);
		}
	}

	@Override
	public void inviteToInnerCircle() {
		callServer(CallType.AUTHORIZATION, null);
	}

	@Override
	public void onNotificationClick() {
		ProfileActivity.openNotificationsFragment(getContext());
	}

	@Override
	public ProfileFragment getProfileFragment() {
		return this;
	}

	@Override
	public void onBackClick() {
		if (getActivity() != null)
			getActivity().onBackPressed();
	}

	@Override
	public void onDotsClick(View dots, final View preferredLabel) {
		if (profileType == ProfileHelper.ProfileType.INTEREST_CLAIMED ||
				profileType == ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED) {
			PopupMenu menu = new PopupMenu(getContext(), dots);
			menu.inflate(R.menu.popup_menu_preferred_interest);
			menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if (Utils.isContextValid(getContext())) {
						if (item.getItemId() == R.id.interest_claim) {
							goToClaimPage();
						}
						else {
							if (interest != null && !interest.isPreferred()) {
								if (interest.isFollowed()) {
									preferDialog = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_prefer_interest);
									View v = (preferDialog != null) ? preferDialog.getCustomView() : null;
									if (v != null) {
										Button pos = v.findViewById(R.id.button_positive);
										pos.setText(R.string.action_set);
										pos.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												callServer(CallType.PREFER, null);
											}
										});
										v.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												preferDialog.dismiss();
											}
										});
									}
									DialogUtils.showDialog(preferDialog);
								} else {
									final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_prefer_interest_not_followed);
									View v = (dialog != null) ? dialog.getCustomView() : null;
									if (v != null) {
										Button pos = v.findViewById(R.id.button_positive);
										pos.setText(R.string.ok);
										pos.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												dialog.dismiss();
											}
										});
										v.findViewById(R.id.button_negative).setVisibility(View.GONE);
									}
									DialogUtils.showDialog(dialog);
								}
							} else callServer(CallType.UNPREFER, null);
						}

						return true;
					}

					return false;
				}
			});

			MenuItem prefer = menu.getMenu().findItem(R.id.interest_prefer);
			MenuItem unprefer = menu.getMenu().findItem(R.id.interest_unprefer);
			MenuItem claim = menu.getMenu().findItem(R.id.interest_claim);

			Utils.applyRegularFontToMenuItem(getContext(), prefer);
			Utils.applyRegularFontToMenuItem(getContext(), unprefer);
			Utils.applyRegularFontToMenuItem(getContext(), claim);

			if (interest != null) {
				prefer.setVisible(!interest.isPreferred());
				unprefer.setVisible(interest.isPreferred());
				// INFO: 2/14/19    LUISS disables CLAIM
				claim.setVisible(/*!interest.isClaimed()*/false);
				menu.show();
			}
		}
	}

	@Override
	public void onDotsClickUser(View dots) {
		if (profileType == ProfileHelper.ProfileType.FRIEND ||
				profileType == ProfileHelper.ProfileType.NOT_FRIEND) {
			PopupMenu menu = new PopupMenu(getContext(), dots);
			menu.inflate(R.menu.popup_menu_moderation_user);
			menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if (Utils.isContextValid(getContext())) {
						if (item.getItemId() == R.id.user_remove)
							flagUser(FlagType.REMOVE);
						else if (item.getItemId() == R.id.user_block)
							flagUser(FlagType.BLOCK);
						else if (item.getItemId() == R.id.user_report)
							flagUser(FlagType.REPORT);

						return true;
					}

					return false;
				}
			});

			MenuItem remove = menu.getMenu().findItem(R.id.user_remove);
			MenuItem block = menu.getMenu().findItem(R.id.user_block);
			MenuItem report = menu.getMenu().findItem(R.id.user_report);

			Utils.applyRegularFontToMenuItem(getContext(), remove);
			Utils.applyRegularFontToMenuItem(getContext(), block);
			Utils.applyRegularFontToMenuItem(getContext(), report);

			if (userGeneric != null)
				remove.setVisible(userGeneric.isFriend());
			menu.show();
		}
	}

	private enum FlagType { REMOVE, BLOCK, REPORT }
	private void flagUser(final FlagType type) {
		moderationDialog = DialogUtils.createGenericAlertCustomView(getContext(),
				R.layout.custom_dialog_flag_user);

		@StringRes int positive = -1;
		@StringRes int title = -1;
		@StringRes int message = -1;
		if (type != null) {
			switch (type) {
				case BLOCK:
					positive = R.string.action_block;
					title = R.string.dialog_moderate_user_title_block;
					message = R.string.dialog_moderate_user_message_block;
					break;
				case REMOVE:
					positive = R.string.action_remove;
					title = R.string.dialog_moderate_user_title_remove;
					message = R.string.dialog_moderate_user_message_remove;
					break;
				case REPORT:
					positive = R.string.action_report;
					title = R.string.dialog_moderate_user_title_report;
					message = R.string.dialog_moderate_user_message_report;
					break;
			}

			if (moderationDialog != null) {
				View v = moderationDialog.getCustomView();
				if (v != null) {
					((TextView) v.findViewById(R.id.dialog_flag_title)).setText(title);
					((TextView) v.findViewById(R.id.dialog_flag_message)).setText(getString(message, userGeneric.getCompleteName()));

					final View errorMessage = v.findViewById(R.id.error_empty_message);

					final EditText editText = v.findViewById(R.id.report_post_edittext);
					editText.setVisibility(type == FlagType.REPORT ? View.VISIBLE : View.GONE);
					editText.addTextChangedListener(new TextWatcher() {
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {}

						@Override
						public void afterTextChanged(Editable s) {
							if (errorMessage.getAlpha() == 1 && s.length() > 0)
								errorMessage.animate().alpha(0).setDuration(200).start();
						}
					});

					Button positiveBtn = v.findViewById(R.id.button_positive);
					positiveBtn.setText(positive);
					positiveBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							switch (type) {
								case BLOCK:
									blockUser();
									break;
								case REMOVE:
									removeUser();
									break;
								case REPORT:
									String msg = editText.getText().toString();
									if (Utils.isStringValid(msg))
										reportUser(msg);
									else
										errorMessage.animate().alpha(1).setDuration(200).start();

									break;
							}
						}
					});

					Button negativeBtn = v.findViewById(R.id.button_negative);
					negativeBtn.setText(R.string.cancel);
					negativeBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							moderationDialog.dismiss();
						}
					});
				}
				moderationDialog.show();
			}
		}
	}

	private void blockUser() {
		callServer(CallType.USER_BLOCK, null);
	}

	private void removeUser() {
		callServer(CallType.USER_REMOVE, null);
	}

	private void reportUser(String reasons) {
		callServer(CallType.USER_REPORT, reasons);
	}


	@Override
	public void goToInitiatives() {

		// now only SIMILAR exist

//		String id = null, name = null, avatar = null;
//		if (interest != null){
//			id = interest.getId();
//			name = interest.getName();
//			avatar = interest.getAvatarURL();
//		}
//
//		if (Utils.isStringValid(id)) {
//			if (getActivity() instanceof HomeActivity)
//				ProfileActivity.openInitiativesForInterestFragment(getContext(), id, name, avatar);
//			else if (getActivity() instanceof ProfileActivity)
//				profileActivityListener.showInitiativesForInterestFragment(id, name, avatar);
//		}
	}

	@Override
	public void goToSimilar() {
		String id = null, name = null, avatar = null;
		if (interest != null){
			id = interest.getId();
			name = interest.getName();
			avatar = interest.getAvatarURL();
		}

		if (Utils.isStringValid(id)) {
			if (getActivity() instanceof HomeActivity)
				ProfileActivity.openSimilarForInterestFragment(getContext(), id, name, avatar);
			else if (getActivity() instanceof ProfileActivity)
				profileActivityListener.showSimilarForInterestFragment(id, name, avatar);
		}
	}

	@Override
	public void goToClaimPage() {
		if (profileActivityListener != null && interest != null)
			profileActivityListener.showClaimInterestFragment(interest.getId(), interest.getName(), interest.getAvatarURL());
	}

	@Override
	public void followInterest() {
		callServer(CallType.FOLLOW, null);
	}

	@Override
	public void unfollowInterest() {
		callServer(CallType.UNFOLLOW, null);
	}

	@Override
	public void goToInterestDetails() {
		if (profileType == ProfileHelper.ProfileType.INTEREST_ME)
			ProfileActivity.openInterestDetailFragment(getContext());
		else if (interest != null)
			profileActivityListener.showInterestDetailFragment(interest.getId());
	}

	@Override
	public void goToFollowers() {
		String id = null, name = null, avatar = null;
		if (interest != null){
			id = interest.getId();
			name = interest.getName();
			avatar = interest.getAvatarURL();
		}

		if (Utils.isStringValid(id)) {
			if (getActivity() instanceof HomeActivity)
				ProfileActivity.openFollowersFragment(getContext(), id, name, avatar);
			else if (getActivity() instanceof ProfileActivity)
				profileActivityListener.showFollowersFragment(id, name, avatar);
		}
	}

	//endregion


	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {
		if (profileHelper != null) {
			profileHelper.configureLayout(view);
		}
	}

	@Override
	protected void setLayout() {

		if (Utils.isStringValid(objectId) && HLInterests.getInstance().hasInterest(objectId))
			interest = HLInterests.getInstance().getInterest(objectId);

		if (profileType == ProfileHelper.ProfileType.INTEREST_ME && mUser.getSelectedObject() instanceof Interest)
			interest = (Interest) mUser.getSelectedObject();

		profileHelper.setLayout(profileType, userGeneric, interest, bottomBarSelItem);
	}

	public enum CallType { PROFILE, AUTHORIZATION, FOLLOW, UNFOLLOW, PREFER, UNPREFER,
		USER_REMOVE, USER_BLOCK, USER_REPORT }
	public void callServer(CallType type, @Nullable String reasons) {
		Object[] result = null;

		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);

		if (type == CallType.PROFILE) {
			try {
				if (mUser.isValid() && (profileType == ProfileHelper.ProfileType.ME ||
						profileType == ProfileHelper.ProfileType.INTEREST_ME)) {

					// to solve issue with identity switch
					profileType = mUser.getProfileType();

					if (profileType == ProfileHelper.ProfileType.ME)
						result = HLServerCalls.getUserProfile(mUser.getId());
					else if (profileType == ProfileHelper.ProfileType.INTEREST_ME)
						result = HLServerCalls.getInterestProfile(mUser.getId(), mUser.getId());
				} else if (Utils.isStringValid(objectId)) {
					if (profileType == ProfileHelper.ProfileType.INTEREST_CLAIMED ||
							profileType == ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED) {
						result = HLServerCalls.getInterestProfile(mUser.getId(), objectId);
					} else
						result = HLServerCalls.getPerson(mUser.getId(), objectId);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		else {
			if (mUser.isValid()) {
				try {
					switch (type) {
						case AUTHORIZATION:
							isRequestingAuthorizationToIC = true;
							result = HLServerCalls.requestAuthorizationForInnerCircle(mUser, userGeneric);
							break;
						case FOLLOW:
							result = HLServerCalls.doPositiveActionOnInterest(HLServerCalls.InterestActionType.FOLLOWING, mUser.getId(), objectId);
							break;
						case UNFOLLOW:
							result = HLServerCalls.doNegativeActionOnInterest(HLServerCalls.InterestActionType.FOLLOWING, mUser.getId(), objectId);
							break;
						case PREFER:
							result = HLServerCalls.doPositiveActionOnInterest(HLServerCalls.InterestActionType.PREFERRING, mUser.getId(), objectId);
							break;
						case UNPREFER:
							result = HLServerCalls.doNegativeActionOnInterest(HLServerCalls.InterestActionType.PREFERRING, mUser.getId(), objectId);
							break;
						case USER_REMOVE:
							if (Utils.isStringValid(objectId)) {
								Bundle bundle = new Bundle();
								bundle.putString("friendID", objectId);
								result = HLServerCalls.settingsOperationsOnSingleCircle(mUser.getId(), bundle, SettingsICSingleCircleFragment.CallType.UNFRIEND);
							}
							break;
						case USER_BLOCK:
							if (Utils.isStringValid(objectId)) {
								result = HLServerCalls.blockUnblockUsers(
										mUser.getId(),
										objectId,
										UnBlockUserEnum.BLOCK);
							}
							break;
						case USER_REPORT:
							if (Utils.areStringsValid(objectId, reasons)) {
								result = HLServerCalls.report(HLServerCalls.CallType.USER, mUser.getId(), objectId, reasons);
							}
							break;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	//endregion


	//region == Getters and setters ==

	public View getBottomBarNotificationDot() {
		if (profileHelper != null)
			return profileHelper.getBottomBarNotificationDot();

		return null;
	}

	public void setProfileType(ProfileHelper.ProfileType profileType) {
		this.profileType = profileType;
	}

	//endregion
}
