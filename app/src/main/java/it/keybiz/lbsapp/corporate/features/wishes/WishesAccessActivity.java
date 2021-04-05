/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostActivityMod;
import it.keybiz.lbsapp.corporate.features.legacyContact.LegacyContactSelectionActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

public class WishesAccessActivity extends HLActivity implements View.OnClickListener, OnServerMessageReceivedListener,
		OnMissingConnectionListener {

	private View layoutActive, layoutInactive;
	private View sectionBtnHearts, sectionBtnLegacyPreferred;
	private View layoutBtnLegacy, layoutBtnPreferred, btnSavedWishes, btnSavedWishes1;
	private Button heartsPost, heartsInvite, heartsProfile, layoutBtnLegacyBtn;
	private TextView claimInactive;
	private TextView completeActions;

	private ImageView burgerIcon, burgerIcon1;

	private boolean showSavedWishes;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wish_access);
		setRootContent(R.id.root_content);

		configureLayout();
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.WISHES_WELCOME);

		callForSavedCount();
		callForAvailability();

		setLayout();
	}

	@Override
	protected void manageIntent() {

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.back_btn:
				finish();
				overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
				break;

			case R.id.create_wish_btn:
				WishesActivity.openWishNameFragment(this);
				break;

			case R.id.wishes_action_btn_legacy:
				startActivityForResult(new Intent(this, LegacyContactSelectionActivity.class), Constants.RESULT_SELECT_LEGACY_CONTACT);
				break;

			case R.id.wishes_action_btn_pref_interest:
				ProfileActivity.openMyInterestsFragment(this, mUser.getUserId(), mUser.getCompleteName(), mUser.getAvatarURL());
				break;

			case R.id.wishes_action_btn_post:
				startActivityForResult(new Intent(this, CreatePostActivityMod.class), Constants.RESULT_CREATE_POST);
				break;

			case R.id.wishes_action_btn_invite:
				ProfileActivity.openInnerCircleFragment(this, mUser.getUserId(),
						mUser.getUserCompleteName(), mUser.getUserAvatarURL(), true);
				break;

			case R.id.wishes_action_btn_follow:
				ProfileActivity.openMyInterestsFragment(this, mUser.getUserId(), mUser.getUserCompleteName(), mUser.getUserAvatarURL());
				break;

			case R.id.saved_wishes_btn:
			case R.id.saved_wishes_btn_1:
				if (Utils.isContextValid(this)) {
					startActivity(new Intent(this, SavedWishesActivity.class));
					this.overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
				}
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == Constants.RESULT_CREATE_POST) {

		}
	}

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (responseObject == null) {
			handleErrorResponse(operationId, 0);
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_CONFIGURATION_DATA:
				 if (responseObject.length() == 0) {
					 handleErrorResponse(operationId, 0);
					 return;
				 }

				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						mUser.saveConfigurationData(responseObject.optJSONObject(0));

						if (Utils.isContextValid(WishesAccessActivity.this))
							setLayout();
					}
				});
				break;

		case Constants.SERVER_OP_GET_SAVED_WISHES:
				showSavedWishes = responseObject.length() > 0;
				setLayout();
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		if (Utils.isContextValid(this))
			setLayout();
		showAlert(R.string.error_generic_update);
	}

	@Override
	public void onMissingConnection(int operationId) {
		if (Utils.isContextValid(this))
			setLayout();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	protected void configureLayout() {

		layoutInactive = findViewById(R.id.layout_wishes_inactive);
		layoutActive = findViewById(R.id.layout_wishes_active);

		claimInactive = findViewById(R.id.claim_inactive);

		completeActions = findViewById(R.id.complete_actions);

		sectionBtnLegacyPreferred = findViewById(R.id.buttons_actions_preferred_legacy);
		layoutBtnLegacy = findViewById(R.id.layout_btn_legacy);
		layoutBtnLegacyBtn = findViewById(R.id.wishes_action_btn_legacy);
		layoutBtnPreferred = findViewById(R.id.layout_btn_preferred);

		sectionBtnHearts = findViewById(R.id.buttons_actions_hearts);
		heartsInvite = findViewById(R.id.wishes_action_btn_invite);
		heartsProfile = findViewById(R.id.wishes_action_btn_follow);
		btnSavedWishes1 = findViewById(R.id.saved_wishes_btn_1);
		btnSavedWishes1.setOnClickListener(this);
		burgerIcon1 = btnSavedWishes1.findViewById(R.id.wish_icon_burger);

		btnSavedWishes = findViewById(R.id.saved_wishes_btn);
		btnSavedWishes.setOnClickListener(this);
		burgerIcon = btnSavedWishes.findViewById(R.id.wish_icon_burger);

		findViewById(R.id.wishes_action_btn_post).setOnClickListener(this);
		heartsInvite.setOnClickListener(this);
		heartsProfile.setOnClickListener(this);

		findViewById(R.id.wishes_action_btn_legacy).setOnClickListener(this);
		findViewById(R.id.wishes_action_btn_pref_interest).setOnClickListener(this);

		findViewById(R.id.create_wish_btn).setOnClickListener(this);
		findViewById(R.id.back_btn).setOnClickListener(this);
	}


	protected void setLayout() {

		heartsInvite.setVisibility(View.VISIBLE);
		heartsProfile.setVisibility(View.VISIBLE);

		if (mUser.hasEnoughHeartsForWishes() && mUser.hasLegacyContact() && mUser.hasAPreferredInterest()) {
			layoutInactive.setVisibility(View.GONE);
			layoutActive.setVisibility(View.VISIBLE);

			btnSavedWishes.setVisibility(showSavedWishes ? View.VISIBLE : View.GONE);
		}
		else {
			layoutInactive.setVisibility(View.VISIBLE);
			layoutActive.setVisibility(View.GONE);

			btnSavedWishes1.setVisibility(showSavedWishes ? View.VISIBLE : View.GONE);

			if (!mUser.hasLegacyContact() || !mUser.hasAPreferredInterest()) {
				sectionBtnLegacyPreferred.setVisibility(View.VISIBLE);
				sectionBtnHearts.setVisibility(View.GONE);

				if (!mUser.hasLegacyContact() && !mUser.hasAPreferredInterest()) {
					claimInactive.setText(R.string.wishes_inactive_legacy_pref);
					layoutBtnLegacy.setVisibility(View.VISIBLE);
					layoutBtnLegacyBtn.setText(mUser.canRequestLegacyContact() ?
							R.string.wishes_btn_legacy_contact : R.string.wishes_inactive_legacypending_btn);
					layoutBtnLegacyBtn.setEnabled(mUser.canRequestLegacyContact());
					layoutBtnPreferred.setVisibility(View.VISIBLE);
					completeActions.setText(R.string.wishes_complete_actions);
				}
				else if (!mUser.hasLegacyContact()) {
					claimInactive.setText(mUser.canRequestLegacyContact() ?
							R.string.wishes_inactive_legacy : R.string.wishes_inactive_legacypending);
					layoutBtnLegacy.setVisibility(View.VISIBLE);
					layoutBtnLegacyBtn.setText(mUser.canRequestLegacyContact() ?
							R.string.wishes_btn_legacy_contact : R.string.wishes_inactive_legacypending_btn);
					layoutBtnLegacyBtn.setEnabled(mUser.canRequestLegacyContact());
					completeActions.setVisibility(View.VISIBLE);
					layoutBtnPreferred.setVisibility(View.GONE);
					completeActions.setText(R.string.wishes_complete_action);
				}
				else if (!mUser.hasAPreferredInterest()) {
					claimInactive.setText(R.string.wishes_inactive_pref);
					completeActions.setVisibility(View.VISIBLE);
					layoutBtnLegacy.setVisibility(View.GONE);
					layoutBtnPreferred.setVisibility(View.VISIBLE);
					completeActions.setText(R.string.wishes_complete_action);
				}
			}
			else {
				sectionBtnLegacyPreferred.setVisibility(View.GONE);
				sectionBtnHearts.setVisibility(View.VISIBLE);

				completeActions.setText(R.string.wishes_complete_actions_2);

				// FIXME: 5/17/2018    temporarily hardcoded, but waiting for next update
				claimInactive.setText(getString(R.string.wishes_inactive_hearts, "1,000"));
			}
		}

		if (Utils.isContextValid(this)) {
			if (Utils.hasLollipop()) {
				ColorStateList csl = Utils.hasMarshmallow() ?
						getResources().getColorStateList(R.color.state_list_login, null) : getResources().getColorStateList(R.color.state_list_login);
				burgerIcon.setImageTintList(csl);
				burgerIcon1.setImageTintList(csl);
			}
			else {
				burgerIcon.setImageResource(R.drawable.selector_icon_saved_wishes);
				burgerIcon1.setImageResource(R.drawable.selector_icon_saved_wishes);
			}
		}
	}

	private void callForAvailability() {
		Object[] result = null;

		try {
			result = HLServerCalls.getConfigurationData(mUser.getUserId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((LBSLinkApp) this.getApplication()))
				.handleCallResult(this, this, result);
	}

	private void callForSavedCount() {
		Object[] result = null;

		try {
			result = HLServerCalls.getSavedWishes(mUser.getUserId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((LBSLinkApp) this.getApplication()))
				.handleCallResult(this, this, result);
	}

}
