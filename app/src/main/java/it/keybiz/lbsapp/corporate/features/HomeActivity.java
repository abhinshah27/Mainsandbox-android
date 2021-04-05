/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features;

import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.realm.RealmObject;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.connection.realTime.RealTimeCommunicationHelperKotlin;
import it.keybiz.lbsapp.corporate.fcm.SendFCMTokenService;
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostActivityMod;
import it.keybiz.lbsapp.corporate.features.home.HomeWebViewFragment;
import it.keybiz.lbsapp.corporate.features.notifications.NotificationAndRequestHelper;
import it.keybiz.lbsapp.corporate.features.notifications.NotificationsFragment;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileFragment;
import it.keybiz.lbsapp.corporate.features.profile.ProfileHelper;
import it.keybiz.lbsapp.corporate.features.tags.ViewAllTagsActivity;
import it.keybiz.lbsapp.corporate.features.timeline.LandscapeMediaActivity;
import it.keybiz.lbsapp.corporate.features.timeline.OnTimelineFragmentInteractionListener;
import it.keybiz.lbsapp.corporate.features.timeline.TimelineFragment;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.FullScreenHelper;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.PostBottomSheetHelper;
import it.keybiz.lbsapp.corporate.features.timeline.interactions.PostOverlayActionActivity;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;
import it.keybiz.lbsapp.corporate.widgets.HLViewPagerNoScroll;

public class HomeActivity extends HLActivity implements View.OnClickListener, BasicInteractionListener,
		ViewPager.OnPageChangeListener, OnMissingConnectionListener,
		OnServerMessageReceivedListener,
		GestureDetector.OnGestureListener,
		OnTimelineFragmentInteractionListener,
		NotificationAndRequestHelper.OnNotificationHelpListener,
		NotificationsFragment.OnNotificationsFragmentInteractionListener {

	public static final int PAGER_ITEM_HOME_WEBVIEW = 0;
	public static final int PAGER_ITEM_TIMELINE = 1;
	public static final int PAGER_ITEM_PROFILE = 2;
	public static final int PAGER_ITEM_NOTIFICATIONS = 3;

	private static int currentPagerItem = -1;

	/**
	 * Serves for TIMELINE pagination purpose.
	 */
	private int lastPageID = 1;

	private View rootView;

	private View bottomBar;
	private ImageView ib1,ib2, ib3, ib4;
	private TransitionDrawable td1, td2, td3, td4;
	private View main;
	private View notificationsDot;
	private View notificationsDotChat;

	private HLViewPagerNoScroll viewPager;
	private HLHomePagerAdapter adapter;

	private PostBottomSheetHelper bottomSheetHelper;

	private GestureDetector mDetector;
	private MediaHelper mediaHelper;

	private Integer lastAdapterPosition = null;

	private FullScreenHelper fullScreenListener;
	private FullScreenHelper.FullScreenType fullScreenSavedState;
	private FullScreenHelper.RestoreFullScreenStateListener fsStateListener;

	private OnBackPressedListener backListener;

	private NotificationAndRequestHelper notificationHelper;
	private boolean hasOrFromNotification = false/*, hasChatRelatedNotification = false*/;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(rootView = LayoutInflater.from(this).inflate(R.layout.activity_home, null, false));
		setRootContent(R.id.root_content);

		/* SETS ENTER FADE TRANSITION */
		if (Utils.hasLollipop()) {
//			getWindow().setEnterTransition(new Fade());
			getWindow().setExitTransition(new Fade());
		}

//		mDetector = new GestureDetector(this, this);
		fullScreenListener = new FullScreenHelper(this, false);
		mediaHelper = new MediaHelper();

		notificationHelper = new NotificationAndRequestHelper(this, this);

		bottomBar = findViewById(R.id.bottom_bar);

//		fullScreenListener.setBottomBarExpHeight(bottomBar.getHeight());
//		fullScreenListener.setToolbarExpHeight(toolbar.getHeight());

		viewPager = findViewById(R.id.pager);
		adapter = new HLHomePagerAdapter(getSupportFragmentManager());
		viewPager.addOnPageChangeListener(this);
		viewPager.setAdapter(adapter);
		viewPager.setOffscreenPageLimit(0);

		configurePostOptionsSheets();

		manageIntent();
		configureBottomBar(bottomBar);
	}

	@Override
	protected void onStart() {
		super.onStart();

		SendFCMTokenService.startService(getApplicationContext());

		configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!RealmObject.isValid(mUser)) {
			if (!RealmUtils.isValid(realm))
				realm = RealmUtils.getCheckedRealm();
			mUser = new HLUser().readUser(realm);
		}

		// INFO: 2/27/19    notifications not updated: I got a feeling that the bug is caused here
//		if (notificationHelper != null)
//			notificationHelper.updateNotifications();

		// INFO: 2/7/19    fixes glitch that always brings temporarily the 1st fragment visible before switching
//		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(currentPagerItem);

		if (bottomSheetHelper != null)
			bottomSheetHelper.onResume();

//		handleChatToReadDot(currentPagerItem);
//		LBSLinkApp.profileFragmentVisible = currentPagerItem == HomeActivity.PAGER_ITEM_PROFILE;
		NotificationAndRequestHelper.handleDotVisibility(notificationsDot, mUser.isValid());
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (notificationHelper != null)
			notificationHelper.onPause();

		if (bottomSheetHelper != null)
			bottomSheetHelper.onPause();
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();

		if (weakTimeline != null && weakTimeline.get() != null && weakTimeline.get().hasFilterVisible()) {
			weakTimeline.get().handleFilterToggle();
		}
		else {
			if (!Utils.checkAndOpenLogin(this, mUser, HomeActivity.PAGER_ITEM_TIMELINE)) {
				switch (id) {
					case R.id.main_action_btn:
						goToCreatePost();
						break;

					case R.id.bottom_timeline:
						viewPager.setCurrentItem(PAGER_ITEM_TIMELINE);
						break;
					case R.id.bottom_profile:
						viewPager.setCurrentItem(PAGER_ITEM_PROFILE);
						break;
					case R.id.bottom_notifications:
						viewPager.setCurrentItem(PAGER_ITEM_NOTIFICATIONS);
						break;
					case R.id.bottom_global_search:
						viewPager.setCurrentItem(PAGER_ITEM_HOME_WEBVIEW);
						break;
				}
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			// caught when returning from PostOverlayActivity
			case Constants.RESULT_TIMELINE_INTERACTIONS:
				restoreFullScreenState();
				break;

			case Constants.RESULT_SINGLE_POST:
				break;

			case Constants.RESULT_SELECT_IDENTITY:
				if (currentPagerItem == PAGER_ITEM_PROFILE && weakProfile != null) {
					ProfileFragment frg = weakProfile.get();
					if (frg != null && frg.isVisible()) {
						frg.setProfileType(mUser.getProfileType());
						frg.callServer(ProfileFragment.CallType.PROFILE, null);
					}
				}
				break;
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		manageIntent(intent, true);
	}

	@Override
	public void onBackPressed() {
		if (backListener != null)
			backListener.onBackPressed();
		else {

			showExitActionMessage();
		}
	}

	private void configureBottomBar(final View bar) {
		if (bar != null) {
			View l1 = bar.findViewById(R.id.bottom_timeline);
			l1.setOnClickListener(this);
			View l2 = bar.findViewById(R.id.bottom_profile);
			l2.setOnClickListener(this);
			View l3 = bar.findViewById(R.id.bottom_notifications);
			l3.setOnClickListener(this);
			View l4 = bar.findViewById(R.id.bottom_global_search);
			l4.setOnClickListener(this);

			ib1 = bar.findViewById(R.id.icon_timeline);
			td1 = (TransitionDrawable) ib1.getDrawable();
			td1.setCrossFadeEnabled(true);
			ib2 = bar.findViewById(R.id.icon_profile);
			td2 = (TransitionDrawable) ib2.getDrawable();
			td2.setCrossFadeEnabled(true);
			ib3 = bar.findViewById(R.id.icon_notifications);
			td3 = (TransitionDrawable) ib3.getDrawable();
			td3.setCrossFadeEnabled(true);
			ib4 = bar.findViewById(R.id.icon_global_search);
			td4 = (TransitionDrawable) ib4.getDrawable();
			td4.setCrossFadeEnabled(true);

			if (mUser.isValid()) {
				if (!hasOrFromNotification/* && !hasChatRelatedNotification*/) {
					ib1.setSelected(true);
					td1.startTransition(0);
				}
				else {
					if (hasOrFromNotification) {
						ib3.setSelected(true);
						td3.startTransition(0);
						hasOrFromNotification = false;
					}
//					else {
//						ib3.setSelected(true);
//						td3.startTransition(0);
//						hasChatRelatedNotification = false;
//					}
				}
			}
			else {
				ib4.setSelected(true);
				td4.startTransition(0);
			}

			main = bar.findViewById(R.id.main_action_btn);
			main.setOnClickListener(this);

			notificationsDot = bar.findViewById(R.id.notification_dot);

			notificationsDotChat = bar.findViewById(R.id.notification_dot);

			/*
			bar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					fullScreenListener.setBottomBarExpHeight(bar.getHeight());
					bar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			});
			*/
		}
	}

//	private void setBottomBar(int currentPagerItem) {
//		if (bottomBar != null) {
//			switch (currentPagerItem) {
//				case PAGER_ITEM_TIMELINE:
//					td1.setCrossFadeEnabled(true);
//					td1.startTransition(0);
//					td2.setCrossFadeEnabled(true);
//					td3.setCrossFadeEnabled(true);
//					td4.setCrossFadeEnabled(true);
//					break;
//				case PAGER_ITEM_PROFILE:
//					td1.setCrossFadeEnabled(true);
//					td2.setCrossFadeEnabled(true);
//					td2.startTransition(0);
//					td3.setCrossFadeEnabled(true);
//					td4.setCrossFadeEnabled(true);
//					break;
//				case PAGER_ITEM_NOTIFICATIONS:
//					td1.setCrossFadeEnabled(true);
//					td2.setCrossFadeEnabled(true);
//					td3.setCrossFadeEnabled(true);
//					td3.startTransition(0);
//					td4.setCrossFadeEnabled(true);
//					break;
//				case PAGER_ITEM_HOME_WEBVIEW:
//					td1.setCrossFadeEnabled(true);
//					td2.setCrossFadeEnabled(true);
//					td3.setCrossFadeEnabled(true);
//					td4.setCrossFadeEnabled(true);
//					td4.startTransition(0);
//					break;
//			}
//		}
//	}

	// TODO: 2/6/19    replace with LUISS custom action
	private void goToCreatePost() {
		startActivityForResult(new Intent(this, CreatePostActivityMod.class), Constants.RESULT_CREATE_POST);
	}


	@Override public void openPostSheet(@NonNull String postId, boolean isOwnPost) {
		if (bottomSheetHelper != null)
			bottomSheetHelper.openPostSheet(postId, isOwnPost);
	}

	@Override
	public void closePostSheet() {
		if (bottomSheetHelper != null)
			bottomSheetHelper.closePostSheet();
	}

	@Override
	public boolean isPostSheetOpen() {
		return isPostSheetOpen(null);
	}

	public boolean isPostSheetOpen(SlidingUpPanelLayout layout) {
		return bottomSheetHelper != null && bottomSheetHelper.isPostSheetOpen(layout);
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

	/*
	private void editPost() {
		goToCreatePost(true);
	}

	private void deletePost() {
		callToServer(CallType.DELETE, null, null);
	}

	private void flagPost(final FlagType type) {
		moderationDialog = DialogUtils.createGenericAlertCustomView(this, R.layout.custom_dialog_flag_post);

		@StringRes int positive = -1;
		@StringRes int title = -1;
		@StringRes int message = -1;
		if (type != null) {
			switch (type) {
				case BLOCK:
					positive = R.string.action_block;
					title = R.string.option_post_block;
					message = R.string.flag_block_user_message;
					break;
				case HIDE:
					positive = R.string.action_hide;
					title = R.string.option_post_hide;
					message = R.string.flag_hide_post_message;
					break;
				case REPORT:
					positive = R.string.action_report;
					title = R.string.flag_report_post_title;
					message = R.string.flag_report_post_message;
					break;
			}

			if (moderationDialog != null) {
				View v = moderationDialog.getCustomView();
				if (v != null) {
					((TextView) v.findViewById(R.id.dialog_flag_title)).setText(title);
					((TextView) v.findViewById(R.id.dialog_flag_message)).setText(message);

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
								case HIDE:
									hidePost();
									break;
								case REPORT:
									String msg = editText.getText().toString();
									if (Utils.isStringValid(msg))
										reportPost(msg);
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
				bottomSheetPostOther.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
			}
		}
	}

	private void blockUser() {
		callToServer(CallType.FLAG, FlagType.BLOCK, null);
	}

	private void hidePost() {
		callToServer(CallType.FLAG, FlagType.HIDE, null);
	}

	private void reportPost(String reasons) {
		callToServer(CallType.FLAG, FlagType.REPORT, reasons);
	}

	private void callToServer(CallType type, @Nullable FlagType flagType, String reportMessage) {
		Post post = HLPosts.getInstance().getPost(postIdForActions);

		Object[] results = null;
		if (type != null) {
			try {
				if (mUser != null) {
					if (type == CallType.DELETE || (type == CallType.FLAG && flagType == FlagType.HIDE))
						results = HLServerCalls.deletePost(mUser.getId(), postIdForActions);
					else if (flagType != null && post != null) {
						switch (flagType) {
							case BLOCK:
								results = HLServerCalls.blockUnblockUsers(
										mUser.getId(),
										post.getAuthorId(),
										UnBlockUserEnum.BLOCK
								);
								break;
							case REPORT:
								results = HLServerCalls.report(
										HLServerCalls.CallType.POST,
										mUser.getId(),
										postIdForActions,
										reportMessage
								);
								break;
						}
					}

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			HLRequestTracker.getInstance(((LBSLinkApp) getApplication())).handleCallResult(this, this, results);
		}
	}
	*/

	private void configurePostOptionsSheets() {
		if (bottomSheetHelper == null) {
			bottomSheetHelper = new PostBottomSheetHelper(this);
		}
		bottomSheetHelper.configurePostOptionsSheets(rootView);
	}


	/*
	 * SERVER CALLS HANDLING
	 */
	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		if (responseObject == null || responseObject.length() == 0)
			return;
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		switch (operationId) {

		}
	}

	@Override
	public void onMissingConnection(int operationId) {}

	/*
	 * TIMELINE fragment
	 */

//	@Override
//	public void onManageTapForFullScreen(View clickedView, ValueAnimator maskOnUpper, ValueAnimator maskOnLower,
//	                                     ValueAnimator maskOffUpper, ValueAnimator maskOffLower) {
//		int id = clickedView.getId();
//		switch (id) {
//			case R.id.post_main_view:
//				if (fullScreenListener != null) {
////					if (fullScreenListener.getFullScreenType() == FullScreenHelper.FullScreenType.NONE)
////						fullScreenListener.goFullscreen(toolbar, bottomBar);
////					else if (fullScreenListener.getFullScreenType() == FullScreenHelper.FullScreenType.FULL_SCREEN &&
////							maskOnUpper != null && maskOnLower != null)
////						fullScreenListener.applyPostMask(maskOnUpper, maskOnLower);
////					else if (fullScreenListener.getFullScreenType() == FullScreenHelper.FullScreenType.POST_MASK &&
////							maskOffUpper != null && maskOffLower != null) {
////						fullScreenListener.exitFullscreen(toolbar, bottomBar,
////								maskOffUpper, maskOffLower);
////					}
//				}
//				break;
//
//			case R.id.button_hearts:
//				Toast.makeText(this, "Open HEARTS", Toast.LENGTH_SHORT).show();
//				break;
//
//			case R.id.button_comments:
//				Toast.makeText(this, "Open COMMENTS", Toast.LENGTH_SHORT).show();
//				break;
//
//			case R.id.button_shares:
//				Toast.makeText(this, "Open SHARES", Toast.LENGTH_SHORT).show();
//				break;
//
//			case R.id.button_pin:
//				Toast.makeText(this, "Open PIN", Toast.LENGTH_SHORT).show();
//				break;
//		}
//	}

	@Override
	public Toolbar getToolbar() {
		return null;
	}

	@Override
	public View getBottomBar() {
		return bottomBar;
	}

	@Override
	public FullScreenHelper getFullScreenListener() {
		return fullScreenListener;
	}

	@Override
	public void actionsForLandscape(@NonNull String postId, View view) {
		Intent intent = new Intent(this, LandscapeMediaActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, postId);
		if (Utils.hasLollipop()) {
			ActivityOptions options = ActivityOptions
					.makeSceneTransitionAnimation(this, view, Constants.TRANSITION_LANDSCAPE);
			startActivity(intent, options.toBundle());
		}
		else {
			startActivity(intent);
			overridePendingTransition(0, 0);
		}
	}

	@Override
	public void setLastAdapterPosition(int position) {
		this.lastAdapterPosition = position;
	}
	@Override
	public Integer getLastAdapterPosition() {
		return lastAdapterPosition;
	}

	public boolean isPostMaskUpVisible() {
		return fullScreenListener != null && fullScreenListener.getFullScreenType() == FullScreenHelper.FullScreenType.POST_MASK;
	}

	public ValueAnimator getMaskAlphaAnimation(boolean on, View mask) {
		if (fullScreenListener != null)
			return on ? fullScreenListener.getMaskAlphaAnimationOn(mask) : fullScreenListener.getMaskAlphaAnimationOff(mask);

		return null;
	}

	@Override
	public MediaHelper getMediaHelper() {
		return mediaHelper != null ? mediaHelper :(mediaHelper = new MediaHelper());
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
	public void setFsStateListener(FullScreenHelper.RestoreFullScreenStateListener fsStateListener) {
		this.fsStateListener = fsStateListener;
	}

	private void restoreFullScreenState() {
		if (fsStateListener != null)
			fsStateListener.restoreFullScreenState(fullScreenSavedState);
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
	public void goToProfile(@NonNull String userId, boolean isInterest) {
		if (Utils.isStringValid(userId)) {
			if (userId.equals(mUser.getId()))
				viewPager.setCurrentItem(PAGER_ITEM_PROFILE);
			else {
				ProfileActivity.openProfileCardFragment(
						this,
						isInterest ?
								ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND,
						userId,
						HomeActivity.PAGER_ITEM_TIMELINE
				);
			}
		}
	}

	@Override
	protected void manageIntent() {
		manageIntent(getIntent(), false);
	}


	private void manageIntent(Intent intent, boolean newIntent) {
		if (intent != null) {
			if (intent.hasExtra(Constants.KEY_NOTIFICATION_RECEIVED)) {
				String code = intent.getStringExtra(Constants.KEY_NOTIFICATION_RECEIVED);
				if (Utils.isStringValid(code)) {
					switch (code) {
						case Constants.CODE_NOTIFICATION_GENERIC:
							hasOrFromNotification = true;
							break;
						case Constants.CODE_NOTIFICATION_CHAT_UNSENT_MESSAGES:
						case Constants.CODE_NOTIFICATION_CHAT:
//							hasChatRelatedNotification = true;
					}
				}
			}

			// HomeActivity should no longer receive generic notifications
			if (hasOrFromNotification) {
//				intent.putExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_PROFILE);
				intent.putExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_NOTIFICATIONS);
				hasOrFromNotification = false;
			}
//			else if (hasChatRelatedNotification) {
//				intent.putExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_NOTIFICATIONS);
//				hasChatRelatedNotification = false;
//			}

			if (newIntent) {
				currentPagerItem = intent.getIntExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_TIMELINE);

				if (weakTimeline != null && weakTimeline.get() != null) {
					weakTimeline.get().setUsageType(TimelineFragment.FragmentUsageType.TIMELINE);
					weakTimeline.get().setPostListName(null);
				}
				if (weakHomeWebView != null && weakHomeWebView.get() != null) {
					weakHomeWebView.get().resetSearch();
				}
			}
			else {
				if (intent.hasExtra(Constants.EXTRA_PARAM_1)) {
					currentPagerItem = intent.getIntExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_TIMELINE);
					if (currentPagerItem == PAGER_ITEM_NOTIFICATIONS)
//					if (currentPagerItem == PAGER_ITEM_PROFILE)
						hasOrFromNotification = true;
//					else if (currentPagerItem == PAGER_ITEM_NOTIFICATIONS)
//						hasChatRelatedNotification = true;
				}
				else {
					if (!mUser.isValid())
						currentPagerItem = PAGER_ITEM_HOME_WEBVIEW;
					else {
						if (hasOrFromNotification)
							currentPagerItem = PAGER_ITEM_NOTIFICATIONS;
//						else if (hasChatRelatedNotification)
//							currentPagerItem = PAGER_ITEM_NOTIFICATIONS;
						else
							currentPagerItem = PAGER_ITEM_TIMELINE;
					}
				}
			}
		}
	}


	private void showExitActionMessage() {
		showAlert(R.string.action_exit_snack, R.string.action_leave, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}


	//region == NotificationHelper interface


//	@Override
//	public NotificationAndRequestHelper getNotificationHelper() {
//		return notificationHelper;
//	}

	@Override
	public View getBottomBarNotificationDot() {
		return notificationsDot;
	}

	@Override
	public String getUserId() {
		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);
		if (mUser.isValid())
			return mUser.getId();
		else
			return null;
	}

//	private ViewProviderForNotifications viewProvider;
//	public interface ViewProviderForNotifications {
//		TextView getUnreadTextView();
//		View getToolbarNotificationsDot();
//		View getToolbarNotificationsIcon();
//	}
//
//	public void setViewProvider(ViewProviderForNotifications viewProvider) {
//		this.viewProvider = viewProvider;
//	}

	//endregion


	//region == GestureDetector Events ==

	@Override
	public boolean onTouchEvent(MotionEvent event) {
//		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
//				fullScreenListener.goFullscreen(toolbar, bottomBar);
			}
		}, 2 * Constants.TIME_UNIT_SECOND);
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	//endregion


	private WeakReference<ProfileFragment> weakProfile;
	private WeakReference<TimelineFragment> weakTimeline;
	private WeakReference<NotificationsFragment> weakNotifications;
	private WeakReference<HomeWebViewFragment> weakHomeWebView;
	private class HLHomePagerAdapter extends FragmentStatePagerAdapter {

		public HLHomePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case PAGER_ITEM_HOME_WEBVIEW:
					weakHomeWebView = new WeakReference<>(HomeWebViewFragment.newInstance());
					return weakHomeWebView.get();

				case PAGER_ITEM_TIMELINE:
					realm = RealmUtils.checkAndFetchRealm(realm);
					mUser = RealmUtils.checkAndFetchUser(realm, mUser);

					TimelineFragment.FragmentUsageType type = TimelineFragment.FragmentUsageType.GUEST;
					String listName = "publicposts";
					if (mUser.isValid()) {
						type = TimelineFragment.FragmentUsageType.TIMELINE;
						listName = null;
					}

					weakTimeline = new WeakReference<>(
							TimelineFragment.newInstance(type, null, null, null,
									null, listName, null)
					);
					return weakTimeline.get();

				case PAGER_ITEM_PROFILE:

					realm = RealmUtils.checkAndFetchRealm(realm);
					mUser = RealmUtils.checkAndFetchUser(realm, mUser);

					weakProfile = new WeakReference<>(
							ProfileFragment.newInstance(
									mUser.getProfileType(),
									null,
									PAGER_ITEM_PROFILE
							)
					);
					return weakProfile.get();

				case PAGER_ITEM_NOTIFICATIONS:
					realm = RealmUtils.checkAndFetchRealm(realm);
					mUser = RealmUtils.checkAndFetchUser(realm, mUser);

					weakNotifications = new WeakReference<>(NotificationsFragment.newInstance());
					return weakNotifications.get();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 4;
		}
	}


	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

	@Override
	public void onPageSelected(int position) {

		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);

		LBSLinkApp.notificationsFragmentVisible = false;
//		LBSLinkApp.chatRoomsFragmentVisible = false;

//		handleChatToReadDot(position);

		RealTimeCommunicationHelperKotlin.Companion.getInstance(this).setListenerChat(null);

		Utils.closeKeyboard((weakHomeWebView != null && weakHomeWebView.get() != null) ? weakHomeWebView.get().getSearchView() : null);

		switch (position) {
			case PAGER_ITEM_TIMELINE:

				AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_TIMELINE_FEED);

				switch (currentPagerItem) {
					case PAGER_ITEM_PROFILE:
//						setBottomBar(false, position);
						td2.reverseTransition(100);
						break;
					case PAGER_ITEM_NOTIFICATIONS:
//						setBottomBar(false, position);
						td3.reverseTransition(100);
						break;
					case PAGER_ITEM_HOME_WEBVIEW:
//						setBottomBar(false, position);
						td4.reverseTransition(100);
						break;
				}
				ib1.setSelected(true);
				td1.startTransition(200);

				ib2.setSelected(false);
				ib3.setSelected(false);
				ib4.setSelected(false);
				break;

			case PAGER_ITEM_PROFILE:

//				LBSLinkApp.notificationsFragmentVisible = true;
//				NotificationAndRequestHelper.handleDotVisibility(notificationsDot, mUser.isValid());

				AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_ME_PAGE);

				switch (currentPagerItem) {
					case PAGER_ITEM_TIMELINE:
//						setBottomBar(false, position);
						td1.reverseTransition(100);
						break;
					case PAGER_ITEM_NOTIFICATIONS:
//						setBottomBar(false, position);
						td3.reverseTransition(100);
						break;
					case PAGER_ITEM_HOME_WEBVIEW:
//						setBottomBar(false, position);
						td4.reverseTransition(100);
						break;
				}
				ib2.setSelected(true);
				td2.startTransition(200);

				ib1.setSelected(false);
				ib3.setSelected(false);
				ib4.setSelected(false);

				if (weakProfile != null) {
					ProfileFragment frg = weakProfile.get();
//					if (frg != null && frg.isVisible())
//						frg.callServer(ProfileFragment.CallType.PROFILE, null);
				}

				// INFO: 2/27/19    NOOO!!!
//				if (!mUser.isActingAsInterest()) {
//					HLNotifications.getInstance().callForNotifications(this, this, mUser.getId(), false);
//					HLNotifications.getInstance().callForNotifRequests(this, this, mUser.getId(), false);
//				}
				break;

			case PAGER_ITEM_NOTIFICATIONS:

				// logic moved to profile
				LBSLinkApp.notificationsFragmentVisible = true;

				AnalyticsUtils.trackScreen(this, AnalyticsUtils.ME_NOTIFICATION);

				switch (currentPagerItem) {
					case PAGER_ITEM_TIMELINE:
//						setBottomBar(true, position);
						td1.reverseTransition(100);
						break;
					case PAGER_ITEM_PROFILE:
//						setBottomBar(true, position);
						td2.reverseTransition(100);
						break;
					case PAGER_ITEM_HOME_WEBVIEW:
//						setBottomBar(true, position);
						td4.reverseTransition(100);
						break;
				}
				ib3.setSelected(true);
				td3.startTransition(200);

				ib1.setSelected(false);
				ib2.setSelected(false);
				ib4.setSelected(false);

				notificationHelper.updateNotifications();
				break;

//			case PAGER_ITEM_CHATS:
//
//				NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//				if (mManager != null)
//					mManager.cancel(Constants.NOTIFICATION_CHAT_ID);
//
//				LBSLinkApp.chatRoomsFragmentVisible = true;
//				notificationsDotChat.setVisibility(View.GONE);
//
//				AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_CHAT_ROOMS);
//				if (weakChatRooms != null && weakChatRooms.get() != null)
//					RealTimeCommunicationHelperKotlin.Companion.getInstance(this).setListenerChat(weakChatRooms.get());
//
//				switch (currentPagerItem) {
//					case PAGER_ITEM_TIMELINE:
////						setBottomBar(true, position);
//						td1.reverseTransition(100);
//						break;
//					case PAGER_ITEM_PROFILE:
////						setBottomBar(true, position);
//						td2.reverseTransition(100);
//						break;
//					case PAGER_ITEM_HOME_WEBVIEW:
////						setBottomBar(true, position);
//						td4.reverseTransition(100);
//						break;
//				}
//				ib3.setSelected(true);
//				td3.startTransition(200);
//
//				ib1.setSelected(false);
//				ib2.setSelected(false);
//				ib4.setSelected(false);
//				break;

			case PAGER_ITEM_HOME_WEBVIEW:

				AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_WEB_VIEW);

				switch (currentPagerItem) {
					case PAGER_ITEM_TIMELINE:
//						setBottomBar(true, position);
						td1.reverseTransition(100);
						break;
					case PAGER_ITEM_PROFILE:
//						setBottomBar(true, position);
						td2.reverseTransition(100);
						break;
					case PAGER_ITEM_NOTIFICATIONS:
//						setBottomBar(true, position);
						td3.reverseTransition(100);
						break;
				}
				ib4.setSelected(true);
				td4.startTransition(200);

				ib1.setSelected(false);
				ib2.setSelected(false);
				ib3.setSelected(false);
				break;
		}

		NotificationAndRequestHelper.handleDotVisibility(notificationsDot, mUser.isValid());
		currentPagerItem = position;
	}

	@Override
	public void onPageScrollStateChanged(int state) {}

	private void handleChatToReadDot(int pageSelected) {
		if (pageSelected == PAGER_ITEM_NOTIFICATIONS) {
			notificationsDotChat.setVisibility(View.GONE);
		} else {
			if (mUser.isValid() && ChatRoom.Companion.areThereUnreadMessages(null, realm))
				notificationsDotChat.setVisibility(View.VISIBLE);
			else
				notificationsDotChat.setVisibility(View.GONE);
		}
	}

	@Override
	public NotificationAndRequestHelper getNotificationHelper() {
		return notificationHelper;
	}


	//region == Getters and setters ==

	public void setBackListener(OnBackPressedListener backListener) {
		this.backListener = backListener;
	}

	public PostBottomSheetHelper getBottomSheetHelper() {
		return bottomSheetHelper;
	}

	public View getNotificationsDotChat() {
		return notificationsDotChat;
	}

	public static int getCurrentPagerItem() {
		return currentPagerItem;
	}

	//	public OnActionsResultFromBottom getPostDeletedListener() {
//		return postDeletedListener;
//	}
//	public void setPostDeletedListener(OnActionsResultFromBottom postDeletedListener) {
//		this.postDeletedListener = postDeletedListener;
//	}

	//endregion

}