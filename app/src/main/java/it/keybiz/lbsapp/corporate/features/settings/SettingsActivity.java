/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONArray;

import java.util.ArrayList;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostActivityMod;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileHelper;
import it.keybiz.lbsapp.corporate.features.profile.UserDetailFragment;
import it.keybiz.lbsapp.corporate.features.tags.ViewAllTagsActivity;
import it.keybiz.lbsapp.corporate.features.timeline.OnTimelineFragmentInteractionListener;
import it.keybiz.lbsapp.corporate.features.timeline.TimelineFragment;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.FullScreenHelper;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.PostOverlayActionActivity;
import it.keybiz.lbsapp.corporate.features.userGuide.UserGuideActivity;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLIdentity;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.MarketPlace;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.SettingsHelpElement;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.FragmentsUtils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 3/19/2018.
 */
public class SettingsActivity extends HLActivity implements View.OnClickListener, OnServerMessageReceivedListener,
		SettingsActivityListener, OnMissingConnectionListener, BasicInteractionListener,
		OnTimelineFragmentInteractionListener {

	private FragmentManager fragmentManager = getSupportFragmentManager();

	private Toolbar toolbar;
	private TextView toolbarTitle;
	private ImageView profilePicture;

	private FrameLayout pagesContainer;

//	private View bottomBar;

	private MediaHelper mediaHelper;
	private FullScreenHelper fullScreenListener;
	private FullScreenHelper.FullScreenType fullScreenSavedState;
	private FullScreenHelper.RestoreFullScreenStateListener fsStateListener;

	/**
	 * Serves for TIMELINE pagination purpose.
	 */
	private int lastPageID = 1;

	private OnBackPressedListener backListener;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.generic_progress_indicator);

		toolbar = findViewById(R.id.toolbar);
		profilePicture = findViewById(R.id.profile_picture);
		toolbarTitle = findViewById(R.id.toolbar_title);

		findViewById(R.id.back_arrow).setOnClickListener(this);

		pagesContainer = findViewById(R.id.pages_container);

//		configureBottomBar(bottomBar = findViewById(R.id.bottom_bar));

		mediaHelper = new MediaHelper();
		fullScreenListener = new FullScreenHelper(this, false);

		manageIntent();
	}

	@Override
	protected void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();

		MediaHelper.loadProfilePictureWithPlaceholder(this, mUser.getAvatarURL(), profilePicture);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		HLPosts.getInstance().resetPropertiesForDiaryOrGlobalSearch();
		super.onDestroy();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.back_arrow:
				onBackPressed();
				break;

			case R.id.bottom_timeline:
			case R.id.bottom_profile:
			case R.id.bottom_wishes:
			case R.id.main_action_btn:
				handleBottomBarAction(view.getId());
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			// caught when returning from PostOverlayActivity
			case Constants.RESULT_TIMELINE_INTERACTIONS:
				if (fsStateListener != null)
					fsStateListener.restoreFullScreenState(fullScreenSavedState);
				break;

		}
	}

	@Override
	public void onBackPressed() {
		if (backListener != null)
			backListener.onBackPressed();
		else if (fragmentManager.getBackStackEntryCount() == 1) {
			finish();
			overridePendingTransition(R.anim.no_animation, R.anim.slide_out_top);
		}
		else {
			toolbar.setVisibility(View.VISIBLE);
//			bottomBar.setVisibility(View.VISIBLE);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
			addRemoveTopPaddingFromFragmentContainer(true);
			super.onBackPressed();
		}
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if(intent == null)
			return;

		int showFragment = intent.getIntExtra(Constants.FRAGMENT_KEY_CODE,
				Constants.FRAGMENT_INVALID);
		int requestCode = intent.getIntExtra(Constants.REQUEST_CODE_KEY, Constants.NO_RESULT);
		Bundle extras = intent.getExtras();
		String userId = null;
		String name = null;
		String avatar = null;

		switch (showFragment) {
			case Constants.FRAGMENT_SETTINGS_ACCOUNT:
				addSettingsAccountFragment(null, requestCode, false);
				break;

			case Constants.FRAGMENT_SETTINGS_INNER_CIRCLE:
				addSettingsInnerCircleFragment(null, requestCode, false);
				break;

			case Constants.FRAGMENT_SETTINGS_FOLDERS:
				addSettingsFoldersFragment(null, requestCode, false);
				break;

			case Constants.FRAGMENT_SETTINGS_PRIVACY:
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1)) { }
				}
				addSettingsPrivacyFragment(null, requestCode, false);
				break;

			case Constants.FRAGMENT_SETTINGS_SECURITY:
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1)) { }
				}
				addSettingsSecurityFragment(null, requestCode, false);
				break;

			case Constants.FRAGMENT_SETTINGS_PAYMENT:
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1)) { }
				}
				addSettingsPaymentFragment(null, requestCode, false);
				break;

			case Constants.FRAGMENT_SETTINGS_HELP:
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1)) { }
				}
				addSettingsHelpFragment(null, new SettingsHelpElement(), false, requestCode, false);
				break;

			case Constants.FRAGMENT_SETTINGS_MAIN:
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1)) { }
				}
				addSettingsMainFragment(null, requestCode, false);
				break;
		}
	}

	private void configureBottomBar(final View bar) {
		if (bar != null) {
			View l1 = bar.findViewById(R.id.bottom_timeline);
			l1.setOnClickListener(this);
			View l2 = bar.findViewById(R.id.bottom_profile);
			l2.setOnClickListener(this);
			View l3 = bar.findViewById(R.id.bottom_wishes);
			l3.setOnClickListener(this);
			View l4 = bar.findViewById(R.id.bottom_settings);
			l4.setOnClickListener(this);
			l4.setSelected(true);

			ImageView ib1 = bar.findViewById(R.id.icon_timeline);
			TransitionDrawable td1 = (TransitionDrawable) ib1.getDrawable();
			td1.setCrossFadeEnabled(true);
			ImageView ib2 = bar.findViewById(R.id.icon_profile);
			TransitionDrawable td2 = (TransitionDrawable) ib2.getDrawable();
			td2.setCrossFadeEnabled(true);
			ImageView ib3 = bar.findViewById(R.id.icon_notifications);
			TransitionDrawable td3 = (TransitionDrawable) ib3.getDrawable();
			td3.setCrossFadeEnabled(true);
			ImageView ib4 = bar.findViewById(R.id.icon_settings);
			TransitionDrawable td4 = (TransitionDrawable) ib4.getDrawable();
			td4.setCrossFadeEnabled(true);
			td4.startTransition(0);

			ImageView main = bar.findViewById(R.id.main_action_btn);
			main.setOnClickListener(this);
		}
	}

	private void handleBottomBarAction(@IdRes int viewId) {
		Intent intent = new Intent();

		if (viewId == R.id.main_action_btn) {
			intent.setClass(this, CreatePostActivityMod.class);
			intent.putExtra(Constants.EXTRA_PARAM_2, true);
		}
		else {
			intent.setClass(this, HomeActivity.class);
			int pageId = -1;
			switch (viewId) {
				case R.id.bottom_profile:
					pageId = HomeActivity.PAGER_ITEM_PROFILE;
					break;
				case R.id.bottom_notifications:
					pageId = HomeActivity.PAGER_ITEM_NOTIFICATIONS;
					break;
				case R.id.bottom_timeline:
					pageId = HomeActivity.PAGER_ITEM_TIMELINE;
					break;
			}

			if (pageId != -1)
				intent.putExtra(Constants.EXTRA_PARAM_1, pageId);
		}

		startActivity(intent);
		finish();
	}


	@Override
	public void addRemoveTopPaddingFromFragmentContainer(boolean add) {
		if (pagesContainer != null) {
			pagesContainer.setPadding(
					0,
					add ? getResources().getDimensionPixelSize(R.dimen.activity_margin_lg) : 0,
					0,
					0
			);
		}
	}

	@Override
	public void setToolbarVisibility(boolean visible) {
		if (toolbar != null)
			toolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	@Override
	public void setBottomBarVisibility(boolean visible) {
//		if (bottomBar != null)
//			bottomBar.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	@Override
	public void setToolbarTitle(int titleResId) {
		setToolbarTitle(getString(titleResId));
	}

	@Override
	public void setToolbarTitle(@NonNull String title) {
		if (toolbarTitle != null)
			toolbarTitle.setText(title);
	}

	@Override
	public void refreshProfilePicture(@NonNull String link) {
		MediaHelper.loadProfilePictureWithPlaceholder(this, link, profilePicture);
	}

	@Override
	public void refreshProfilePicture(@NonNull Drawable drawable) {
		if (profilePicture != null)
			profilePicture.setImageDrawable(drawable);
	}

	@Override
	public void closeScreen() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setBackListener(null);
						onBackPressed();
					}
				});
			}
		}, 300);
	}


	//region == Fragment section ==

	/* SETTINGS ACCOUNT FRAGMENT */
	public static void openSettingsMainFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_SETTINGS_MAIN,
				Constants.NO_RESULT, SettingsActivity.class, R.anim.slide_in_top, R.anim.no_animation);
	}

	private void addSettingsMainFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsFragment fragment = (SettingsFragment) fragmentManager.findFragmentByTag(SettingsFragment.LOG_TAG);
		if (fragment == null) {
			fragment = SettingsFragment.newInstance();
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					SettingsFragment.LOG_TAG, target, requestCode, SettingsFragment.LOG_TAG);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* SETTINGS ACCOUNT FRAGMENT */
	public static void openSettingsAccountFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_SETTINGS_ACCOUNT,
				Constants.NO_RESULT, SettingsActivity.class, R.anim.slide_in_right, R.anim.slide_out_left);
	}

	@Override
	public void showSettingsAccountFragment() {
		addSettingsAccountFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsAccountFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsAccountFragment fragment = (SettingsAccountFragment) fragmentManager.findFragmentByTag(SettingsAccountFragment.LOG_TAG);
		if (fragment == null) {
			fragment = SettingsAccountFragment.newInstance();
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					SettingsAccountFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* SETTINGS INNER CIRCLE FRAGMENT */
	public static void openSettingsInnerCircleFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_SETTINGS_INNER_CIRCLE,
				Constants.NO_RESULT, SettingsActivity.class, R.anim.slide_in_right, R.anim.slide_out_left);
	}

	@Override
	public void showSettingsInnerCircleFragment() {
		addSettingsInnerCircleFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsInnerCircleFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsInnerCircleFragment fragment = (SettingsInnerCircleFragment) fragmentManager.findFragmentByTag(SettingsInnerCircleFragment.LOG_TAG);
		if (fragment == null) {
			fragment = SettingsInnerCircleFragment.newInstance();
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					SettingsInnerCircleFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	@Override
	public void showInnerCircleCirclesFragment() {
		addSettingsCirclesFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsCirclesFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsCirclesFragment fragment = (SettingsCirclesFragment) fragmentManager.findFragmentByTag(SettingsCirclesFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsCirclesFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsCirclesFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	@Override
	public void showInnerCircleSingleCircleFragment(@NonNull HLCircle circle, @Nullable String filter) {
		addSettingsInnerCircleSingleCircleFragment(null, circle, filter, Constants.NO_RESULT, true);
	}

	private void addSettingsInnerCircleSingleCircleFragment(Fragment target, @NonNull HLCircle circle,
	                                                        @Nullable String filter, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsICSingleCircleFragment fragment = (SettingsICSingleCircleFragment) fragmentManager.findFragmentByTag(SettingsICSingleCircleFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsICSingleCircleFragment.newInstance(circle, filter);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsICSingleCircleFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showInnerCircleTimelineFeedFragment() {
		addSettingsInnerCircleTimelineFeedFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsInnerCircleTimelineFeedFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsICTimelineFeedFragment fragment = (SettingsICTimelineFeedFragment) fragmentManager.findFragmentByTag(SettingsICTimelineFeedFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsICTimelineFeedFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsICTimelineFeedFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}



	/* SETTINGS FOLDERS FRAGMENT */
	public static void openSettingsFoldersFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_SETTINGS_FOLDERS,
				Constants.NO_RESULT, SettingsActivity.class, R.anim.slide_in_right, R.anim.slide_out_left);
	}

	@Override
	public void showSettingsFoldersFragment() {
		addSettingsFoldersFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsFoldersFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsFoldersFragment fragment = (SettingsFoldersFragment) fragmentManager.findFragmentByTag(SettingsFoldersFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsFoldersFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsFoldersFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showFoldersPostFragment(String listName) {
		addFoldersPostFragment(null, listName, Constants.NO_RESULT, true);
	}

	private void addFoldersPostFragment(Fragment target, String listName,
	                                    int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		TimelineFragment fragment = (TimelineFragment) fragmentManager.findFragmentByTag(TimelineFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = TimelineFragment.newInstance(TimelineFragment.FragmentUsageType.DIARY,
				mUser.getUserId(), mUser.getUserCompleteName(), mUser.getUserAvatarURL(), null, listName, null);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				TimelineFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();

//		toolbar.setVisibility(View.GONE);
//		bottomBar.setVisibility(View.GONE);
//		addRemoveTopPaddingFromFragmentContainer(false);
	}



	private void addSettingsEntriesFragment(Fragment target, SettingsInnerEntriesFragment.ViewType viewType,
	                                        int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsInnerEntriesFragment fragment = (SettingsInnerEntriesFragment) fragmentManager.findFragmentByTag(SettingsInnerEntriesFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsInnerEntriesFragment.newInstance(viewType);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsInnerEntriesFragment.LOG_TAG, target, requestCode, SettingsInnerEntriesFragment.LOG_TAG);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}



	/* SETTINGS PRIVACY FRAGMENT */
	public static void openSettingsPrivacyFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_SETTINGS_PRIVACY, Constants.NO_RESULT, SettingsActivity.class);
	}

	@Override
	public void showSettingsPrivacyFragment() {
		addSettingsPrivacyFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsPrivacyFragment(Fragment target, int requestCode, boolean animate) {
		addSettingsEntriesFragment(target, SettingsInnerEntriesFragment.ViewType.PRIVACY, requestCode, animate);
	}

	@Override
	public void showPrivacySelectionFragment(Fragment target, int privacyEntry, String title) {
		addSettingsPrivacySelectionFragment(null, privacyEntry, title, Constants.RESULT_SETTINGS_PRIVACY_SELECTION, true);
	}

	private void addSettingsPrivacySelectionFragment(Fragment target, int privacyEntry, String title, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsPrivacySelectionFragment fragment = (SettingsPrivacySelectionFragment) fragmentManager.findFragmentByTag(SettingsPrivacySelectionFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsPrivacySelectionFragment.newInstance(privacyEntry, title);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsPrivacySelectionFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showPrivacyBlockedUsersFragment() {
		addSettingsPrivacyBlockedUsersFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsPrivacyBlockedUsersFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsPrivacyBlockedFragment fragment = (SettingsPrivacyBlockedFragment) fragmentManager.findFragmentByTag(SettingsPrivacyBlockedFragment.LOG_TAG);
		if (fragment == null) {
			fragment = SettingsPrivacyBlockedFragment.newInstance(null);
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					SettingsPrivacyBlockedFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showPrivacyPostVisibilitySelectionFragment(SettingsPrivacySelectionFragment.PrivacySubItem item, PrivacyPostVisibilityEnum type) {
		addSettingsPrivacyPostVisibilitySelectionFragment(null, item, type, Constants.NO_RESULT, true);
	}

	private void addSettingsPrivacyPostVisibilitySelectionFragment(Fragment target,
	                                                               SettingsPrivacySelectionFragment.PrivacySubItem item,
	                                                               PrivacyPostVisibilityEnum type,
	                                                               int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsPrivacyPostVisibilitySelectionFragment fragment = (SettingsPrivacyPostVisibilitySelectionFragment) fragmentManager.findFragmentByTag(SettingsPrivacyPostVisibilitySelectionFragment.LOG_TAG);
		if (fragment == null) {
			fragment = SettingsPrivacyPostVisibilitySelectionFragment.newInstance(item, type);
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					SettingsPrivacyPostVisibilitySelectionFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* SETTINGS SECURITY FRAGMENT */
	public static void openSettingsSecurityFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_SETTINGS_SECURITY, Constants.NO_RESULT, SettingsActivity.class);
	}

	@Override
	public void showSettingsSecurityFragment() {
		addSettingsSecurityFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsSecurityFragment(Fragment target, int requestCode, boolean animate) {
		addSettingsEntriesFragment(target, SettingsInnerEntriesFragment.ViewType.SECURITY, requestCode, animate);
	}

	@Override
	public void showSecurityDeleteAccountFragment() {
		addSettingsSecurityDeleteAccount(null, Constants.NO_RESULT, true);
	}

	private void addSettingsSecurityDeleteAccount(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsSecurityDeleteAccountFragment fragment = (SettingsSecurityDeleteAccountFragment) fragmentManager.findFragmentByTag(SettingsSecurityDeleteAccountFragment.LOG_TAG);
		if (fragment == null) {
			fragment = SettingsSecurityDeleteAccountFragment.newInstance();
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					SettingsSecurityDeleteAccountFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showSecurityLegacyContactTriggerFragment() {
		addSettingsSecurityLegacyContactTrigger(null, Constants.NO_RESULT, true);
	}

	private void addSettingsSecurityLegacyContactTrigger(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsSecurityLegacyContactTriggerFragment fragment = (SettingsSecurityLegacyContactTriggerFragment) fragmentManager.findFragmentByTag(SettingsSecurityLegacyContactTriggerFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsSecurityLegacyContactTriggerFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsSecurityLegacyContactTriggerFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showSecurityLegacy2StepFragment(boolean isSelection, @Nullable ArrayList<String> filters) {
		addSettingsSecurityLegacy2StepFragment(null, isSelection, filters, Constants.NO_RESULT, true);
	}

	private void addSettingsSecurityLegacy2StepFragment(Fragment target, boolean isSelection, @Nullable ArrayList<String> filters,
	                                                    int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsSecurityLegacy2StepFragment fragment = (SettingsSecurityLegacy2StepFragment) fragmentManager.findFragmentByTag(SettingsSecurityLegacy2StepFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsSecurityLegacy2StepFragment.newInstance(isSelection, filters);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsSecurityLegacy2StepFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* SETTINGS PAYMENT FRAGMENT */
	public static void openSettingsPaymentFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_SETTINGS_PAYMENT, Constants.NO_RESULT, SettingsActivity.class);
	}

	private void addSettingsPaymentFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		UserDetailFragment fragment = (UserDetailFragment) fragmentManager.findFragmentByTag(UserDetailFragment.LOG_TAG);
		if (fragment == null) {
			fragment = UserDetailFragment.newInstance("");
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					UserDetailFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* SETTINGS HELP FRAGMENT */
	public static void openSettingsHelpFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_SETTINGS_HELP, Constants.NO_RESULT, SettingsActivity.class);
	}

	@Override
	public void showSettingsHelpListFragment(@NonNull SettingsHelpElement element, boolean stopNavigation) {
		addSettingsHelpFragment(null, element, stopNavigation, Constants.NO_RESULT, true);
	}

	private void addSettingsHelpFragment(Fragment target, @NonNull SettingsHelpElement element, boolean stopNavigation, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsHelpListFragment fragment = (SettingsHelpListFragment) fragmentManager.findFragmentByTag(SettingsHelpListFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsHelpListFragment.newInstance(element, stopNavigation);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsHelpListFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showSettingsYesNoUIFragment(@NonNull SettingsHelpElement element) {
		addSettingsYesNoUIFragment(null, element, null, SettingsHelpYesNoUIFragment.UsageType.SERVER,
				Constants.NO_RESULT, true);
	}

	@Override
	public void showSettingsYesNoUIFragmentStatic(@NonNull SettingsHelpElement element, @NonNull String itemId, SettingsHelpYesNoUIFragment.UsageType type) {
		addSettingsYesNoUIFragment(null, element, itemId, type, Constants.NO_RESULT, true);
	}

	private void addSettingsYesNoUIFragment(Fragment target, @NonNull SettingsHelpElement element,
	                                        @Nullable String itemId,
	                                        SettingsHelpYesNoUIFragment.UsageType type,
	                                        int requestCode, boolean animate) {

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsHelpYesNoUIFragment fragment = (SettingsHelpYesNoUIFragment) fragmentManager.findFragmentByTag(SettingsHelpYesNoUIFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsHelpYesNoUIFragment.newInstance(element, itemId, type);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsHelpYesNoUIFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showSettingsHelpContactFragment() {
		addSettingsHelpContactFragment(null, Constants.NO_RESULT, true);
	}

	private void addSettingsHelpContactFragment(Fragment target, int requestCode, boolean animate) {

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsHelpContactFragment fragment = (SettingsHelpContactFragment) fragmentManager.findFragmentByTag(SettingsHelpContactFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsHelpContactFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsHelpContactFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void goToUserGuide() {
		Intent intent = new Intent(this, UserGuideActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, UserGuideActivity.ViewType.SETTINGS);
		startActivity(intent);
	}


	@Override
	public void showSettingsRedeemHeartsSelectFragment(@NonNull HLIdentity identity) {
		addSettingsManagePageFragment(null, identity, Constants.NO_RESULT, true);
	}

	private void addSettingsManagePageFragment(Fragment target, HLIdentity identity, int requestCode, boolean animate) {

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsRedeemHeartsSelectFragment fragment = (SettingsRedeemHeartsSelectFragment) fragmentManager.findFragmentByTag(SettingsRedeemHeartsSelectFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsRedeemHeartsSelectFragment.newInstance(identity);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsRedeemHeartsSelectFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showSettingsRedeemHeartsConfirmFragment(@NonNull HLIdentity identity, @NonNull MarketPlace marketPlace,
	                                                    double heartsValue) {
		addSettingsRedeemHeartsConfirmFragment(null, identity, marketPlace, heartsValue, Constants.NO_RESULT, true);
	}

	private void addSettingsRedeemHeartsConfirmFragment(Fragment target, HLIdentity identity, MarketPlace marketPlace,
	                                                    double heartsValue, int requestCode, boolean animate) {

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		SettingsRedeemHeartsConfirmFragment fragment = (SettingsRedeemHeartsConfirmFragment) fragmentManager.findFragmentByTag(SettingsRedeemHeartsConfirmFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SettingsRedeemHeartsConfirmFragment.newInstance(identity, marketPlace, heartsValue);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SettingsRedeemHeartsConfirmFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	//endregion


	//region == Timeline interface ==

	@Override
	public void actionsForLandscape(@NonNull String postId, View view) {

		// TODO: 3/23/2018     needs implementation

	}

	@Override
	public void setLastAdapterPosition(int position) {}

	@Override
	public Integer getLastAdapterPosition() {
		return null;
	}

	@Override
	public FullScreenHelper getFullScreenListener() {
		return fullScreenListener;
	}

	@Override
	public Toolbar getToolbar() {
		return null;
	}

	@Override
	public View getBottomBar() {
		return null;
	}

	@Override
	public MediaHelper getMediaHelper() {
		if (mediaHelper == null)
			mediaHelper = new MediaHelper();

		return mediaHelper;
	}

	@Override
	public void goToInteractionsActivity(@NonNull String postId) {
		Intent intent = new Intent(this, PostOverlayActionActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, postId);
		startActivityForResult(intent, Constants.RESULT_TIMELINE_INTERACTIONS);
	}


	@Override
	public void saveFullScreenState() {
		fullScreenSavedState = fullScreenListener.getFullScreenType();
	}

	@Override
	public int getPageIdToCall() {
		return lastPageID + 1;
	}

	@Override
	public int getLastPageID() {
		return lastPageID;
	}

	@Override
	public void setLastPageID(int lastPageID) {
		this.lastPageID = lastPageID;
	}

	@Override
	public HLActivity getActivity() {
		return this;
	}

	@Override
	public void setFsStateListener(FullScreenHelper.RestoreFullScreenStateListener fsStateListener) {
		this.fsStateListener = fsStateListener;
	}

	@Override
	public void openPostSheet(@NonNull String postId, boolean isOwnPost) {
		TimelineFragment fragment = (TimelineFragment) fragmentManager.findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				fragment.getBottomSheetHelper().openPostSheet(postId, isOwnPost);
		}
	}

	@Override
	public void closePostSheet() {
		TimelineFragment fragment = (TimelineFragment) fragmentManager.findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				fragment.getBottomSheetHelper().closePostSheet();
		}
	}

	@Override
	public boolean isPostSheetOpen() {
		TimelineFragment fragment = (TimelineFragment) fragmentManager.findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				return fragment.getBottomSheetHelper().isPostSheetOpen();
		}

		return false;
	}

	@Override
	public void goToProfile(@NonNull String userId, boolean isInterest) {
		if (mUser.getId().equals(userId)) {
			Intent intent = new Intent(this, HomeActivity.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
			startActivity(intent);
			finish();
		}
		else {
			ProfileHelper.ProfileType type = isInterest ? ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND;
			ProfileActivity.openProfileCardFragment(this, type, userId, HomeActivity.PAGER_ITEM_PROFILE);
		}
	}

	@Override
	public void viewAllTags(Post post) {
		if (post != null) {
			if (post.hasTags()) {
				ArrayList<Tag> temp = new ArrayList<>();
				temp.addAll(post.getTags());

				Intent intent = new Intent(this, ViewAllTagsActivity.class);
				intent.putParcelableArrayListExtra(Constants.EXTRA_PARAM_1, temp);
				intent.putExtra(Constants.EXTRA_PARAM_2, true);
				startActivity(intent);
			}
		}
	}
	
	//endregion


	//region == Getters and setters ==

	public OnBackPressedListener getBackListener() {
		return backListener;
	}
	public void setBackListener(OnBackPressedListener backListener) {
		this.backListener = backListener;
	}

	//endregion

}
