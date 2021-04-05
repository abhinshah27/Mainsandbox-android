/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostActivityMod;
import it.keybiz.lbsapp.corporate.models.HLInterests;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserSettings;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.enums.UnBlockUserEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.ShareHelper;
import it.keybiz.lbsapp.corporate.utilities.listeners.SlidingPanelHandler;

/**
 * @author mbaldrighi on 12/29/2017.
 */
public class PostBottomSheetHelper implements Serializable,
		OnMissingConnectionListener, OnServerMessageReceivedListener,
		View.OnClickListener, ShareHelper.ShareableProvider {

	public static final String LOG_TAG = PostBottomSheetHelper.class.getCanonicalName();

	private SlidingUpPanelLayout bottomSheetPost;
	private View itemEdit, itemDelete;
	private View itemHide, itemReport, itemBlock, itemUnFollow;
	private TextView textUnFollow;
	private ImageView iconUnFollow;
	private String interestId = null;

	private View itemShare, progressShare;
	private String contentToShare = null;
	private ShareHelper mSHareHelper;

	private HLActivity activity;

	private ServerMessageReceiver messageReceiver;

	private OnActionsResultFromBottom postActionsListener;

	private MaterialDialog moderationDialog;

	private SlidingPanelHandler mHandler;


	public PostBottomSheetHelper(HLActivity activity) {
		this.activity = activity;

		messageReceiver = new ServerMessageReceiver();
		messageReceiver.setListener(this);

		mSHareHelper = new ShareHelper(activity, this);
	}

	public void configurePostOptionsSheets(@NonNull View view) {
		bottomSheetPost = view.findViewById(R.id.sliding_post);
		if (bottomSheetPost != null) {
			bottomSheetPost.setTouchEnabled(true);

			View innerLayout = bottomSheetPost.findViewById(R.id.post_bottom_sheet);

			itemEdit = innerLayout.findViewById(R.id.item_edit);
			itemEdit.setOnClickListener(this);
			itemDelete = innerLayout.findViewById(R.id.item_delete);
			itemDelete.setOnClickListener(this);

			itemHide = innerLayout.findViewById(R.id.item_hide);
			itemHide.setOnClickListener(this);
			itemReport = innerLayout.findViewById(R.id.item_report);
			itemReport.setOnClickListener(this);
			itemBlock = innerLayout.findViewById(R.id.item_block);
			itemBlock.setOnClickListener(this);
			itemUnFollow = innerLayout.findViewById(R.id.item_un_follow);
			itemUnFollow.setOnClickListener(this);
			textUnFollow = itemUnFollow.findViewById(R.id.text_un_follow);
			iconUnFollow = itemUnFollow.findViewById(R.id.icon_un_follow);

			itemShare = innerLayout.findViewById(R.id.item_share);
			itemShare.setOnClickListener(this);
			progressShare = itemShare.findViewById(R.id.progress_share);

			mHandler = null;
			mHandler = new SlidingPanelHandler(bottomSheetPost);
		}
	}

	public void onResume() {
		if (messageReceiver == null)
			messageReceiver = new ServerMessageReceiver();
		messageReceiver.setListener(this);

//		activity.registerReceiver(messageReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
		LocalBroadcastManager.getInstance(activity).registerReceiver(messageReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));

		mSHareHelper.onResume();
	}

	public void onPause() {

		mSHareHelper.onStop();

		try {
//			activity.unregisterReceiver(messageReceiver);
			LocalBroadcastManager.getInstance(activity).unregisterReceiver(messageReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.item_edit:
				editPost();
				break;
			case R.id.item_delete:
				deletePost();
				break;
			case R.id.item_hide:
				flagPost(FlagType.HIDE);
				break;
			case R.id.item_report:
				flagPost(FlagType.REPORT);
				break;
			case R.id.item_block:
				flagPost(FlagType.BLOCK);
				break;
			case R.id.item_un_follow:
				followUnfollowInterest();
				break;

			case R.id.item_share:
				if (Utils.areStringsValid(activity.getUser().getId(), contentToShare)) {
					mSHareHelper.initOps(false);
					return;
				}
		}
		closePostSheet();
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {

		DialogUtils.closeDialog(moderationDialog);

		final JSONObject jsonObject = responseObject.optJSONObject(0);
		switch (operationId) {
			case Constants.SERVER_OP_DELETE_POST:
				String postId = responseObject.optJSONObject(0).optString("_id");
				postActionsListener.onPostDeleted(postId);
				break;

			case Constants.SERVER_OP_INTEREST_FOLLOW_UNFOLLOW:
				if (interestAction == InterestActionType.UNFOLLOW) {
					String id = "";
					if (jsonObject != null && jsonObject.has("intID"))
						id = jsonObject.optString("intID", "");

					HLInterests.getInstance().removeInterest(id);

					// deletes preferred interest after un-following if it was preferred
					HLUser user = activity.getUser();
					HLUserSettings settings = user.getSettings();
					if (settings != null && settings.hasPreferredInterest()) {
						if (user.getPreferredInterest().getId().equals(id)) {
							activity.getRealm().executeTransaction(new Realm.Transaction() {
								@Override
								public void execute(@NonNull Realm realm) {
									user.setHasAPreferredInterest(false);

									if (user.getSettings() != null)
										user.getSettings().setPreferredInterest(null);
								}
							});
						}
					}
				}

				if (interestAction != null && postActionsListener != null) {
					postActionsListener.onInterestUnFollowed(postIdForActions,
							interestAction == InterestActionType.FOLLOW);
				}
				break;

			case Constants.SERVER_OP_SETTING_BLOCK_UNBLOCK_USER:
				postActionsListener.onUserBlocked(postIdForActions);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {

		if (!(activity instanceof HomeActivity)) {
			switch (operationId) {
				case Constants.SERVER_OP_DELETE_POST:
				case Constants.SERVER_OP_MODERATION:
				case Constants.SERVER_OP_SETTING_BLOCK_UNBLOCK_USER:
					activity.showGenericError();
					break;
			}
		}
	}

	@Override
	public void onMissingConnection(int operationId) {}


	//region == Share link ==

	@org.jetbrains.annotations.Nullable
	@Override
	public View getProgressView() {
		return progressShare;
	}

	@Override
	public void afterOps() {
		closePostSheet();
	}

	@NotNull
	@Override
	public String getUserID() {
		return activity.getUser().getId();
	}

	@NotNull
	@Override
	public String getPostOrMessageID() {
		return postIdForActions;
	}

	//endregion


	public void setPostActionsListener(OnActionsResultFromBottom postActionsListener) {
		this.postActionsListener = postActionsListener;
	}


	private String postIdForActions;
	private enum CallType { DELETE, FLAG, INTEREST }
	private enum FlagType { REPORT, HIDE, BLOCK}
	private enum InterestActionType { FOLLOW, UNFOLLOW }
	private InterestActionType interestAction = null;
	public void openPostSheet(@NonNull String postId, boolean isOwnPost) {
		Post post = HLPosts.getInstance().getPost(postId);

		boolean editable = post != null && post.isEditable();
		itemEdit.setVisibility((isOwnPost && editable) ? View.VISIBLE : View.GONE);
		itemDelete.setVisibility(isOwnPost ? View.VISIBLE : View.GONE);

		boolean interest = post != null && post.isInterest();
		boolean followed = post != null && post.isYouFollow();
		interestAction = followed ? InterestActionType.UNFOLLOW : InterestActionType.FOLLOW;
		interestId = interest ? post.getAuthorId() : null;

		itemHide.setVisibility(!isOwnPost ? View.VISIBLE : View.GONE);
		itemReport.setVisibility(!isOwnPost ? View.VISIBLE : View.GONE);
		itemBlock.setVisibility(!isOwnPost && !interest ? View.VISIBLE : View.GONE);
		itemUnFollow.setVisibility(!isOwnPost && interest ? View.VISIBLE : View.GONE);
		if (textUnFollow != null)
			textUnFollow.setText(followed ? R.string.action_unfollow : R.string.action_follow);
		if (iconUnFollow != null)
			iconUnFollow.setImageResource(followed ? R.drawable.ic_close_black : R.drawable.ic_bottomsheet_plus);

		boolean shareCondition = post != null && (post.isAudioPost() || post.isPicturePost() || post.isVideoPost() || post.hasWebLink());
		itemShare.setVisibility(shareCondition ? View.VISIBLE : View.GONE);
		if (post != null) {
		    if (post.hasWebLink())
		        contentToShare = post.getWebLinkUrl();
		    else
		        contentToShare = post.getContent();
        }

		Message message = new Message();
		message.what = SlidingPanelHandler.ACTION_EXPAND;
		mHandler.sendMessage(message);

		postIdForActions = postId;
	}

	public boolean closePostSheet() {
		if (bottomSheetPost != null && isPostSheetOpen(bottomSheetPost)) {
			bottomSheetPost.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
			return true;
		}
		return false;
	}

	public boolean isPostSheetOpen() {
		return isPostSheetOpen(null);
	}


	public boolean isPostSheetOpen(SlidingUpPanelLayout layout) {
		if (layout != null)
			return layout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED;
		else
			return (bottomSheetPost != null && bottomSheetPost.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED);
	}

	private void goToCreatePost(boolean edit) {
		Intent intent = new Intent(activity, CreatePostActivityMod.class);
		if (edit) intent.putExtra(Constants.EXTRA_PARAM_1, postIdForActions);
		activity.startActivityForResult(intent, Constants.RESULT_CREATE_POST);
	}

	private void editPost() {
		goToCreatePost(true);
	}

	private void deletePost() {
		callToServer(CallType.DELETE, null, null, null);
	}

	private void flagPost(final FlagType type) {
		moderationDialog = DialogUtils.createGenericAlertCustomView(activity, R.layout.custom_dialog_flag_post);

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
				bottomSheetPost.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
			}
		}
	}

	private void blockUser() {
		callToServer(CallType.FLAG, FlagType.BLOCK, null, null);
	}

	private void hidePost() {
		callToServer(CallType.FLAG, FlagType.HIDE, null, null);
	}

	private void reportPost(String reasons) {
		callToServer(CallType.FLAG, FlagType.REPORT, reasons, null);
	}

	private void followUnfollowInterest() {
		callToServer(CallType.INTEREST, null, null, interestAction);
	}

	private void callToServer(CallType type, @Nullable FlagType flagType, @Nullable String reportMessage,
	                          @Nullable InterestActionType interestAction) {
		Post post = HLPosts.getInstance().getPost(postIdForActions);

		Object[] results = null;
		if (type != null) {
			try {
				HLUser user = activity.getUser();
				if (user != null) {
					if (type == CallType.DELETE || (type == CallType.FLAG && flagType == FlagType.HIDE))
						results = HLServerCalls.deletePost(user.getId(), postIdForActions);
					else if (type == CallType.INTEREST && interestAction != null && Utils.isStringValid(interestId)) {
						if (interestAction == InterestActionType.UNFOLLOW)
							results = HLServerCalls.doNegativeActionOnInterest(
									HLServerCalls.InterestActionType.FOLLOWING,
									user.getId(),
									interestId);
						else
							results = HLServerCalls.doPositiveActionOnInterest(
									HLServerCalls.InterestActionType.FOLLOWING,
									user.getId(),
									interestId);
					}
					else if (flagType != null && post != null) {
						switch (flagType) {
							case BLOCK:
								results = HLServerCalls.blockUnblockUsers(
										activity.getUser().getId(),
										post.getAuthorId(),
										UnBlockUserEnum.BLOCK
								);
								break;
							case REPORT:
								if (Utils.isStringValid(reportMessage)) {
									results = HLServerCalls.report(
											HLServerCalls.CallType.POST,
											activity.getUser().getId(),
											postIdForActions,
											reportMessage
									);
								}
								break;
						}
					}

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			HLRequestTracker.getInstance(((LBSLinkApp) activity.getApplication())).handleCallResult(this, activity, results);
		}
	}

}
