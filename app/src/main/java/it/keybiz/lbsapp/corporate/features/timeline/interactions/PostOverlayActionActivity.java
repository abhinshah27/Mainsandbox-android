/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnApplicationContextNeeded;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.connection.realTime.RealTimeCommunicationHelperKotlin;
import it.keybiz.lbsapp.corporate.features.timeline.RealTimeCommunicationListener;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.InteractionComment;
import it.keybiz.lbsapp.corporate.models.InteractionHeart;
import it.keybiz.lbsapp.corporate.models.InteractionPost;
import it.keybiz.lbsapp.corporate.models.InteractionShare;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;
import it.keybiz.lbsapp.corporate.utilities.listeners.FlingGestureListener;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

public class PostOverlayActionActivity extends HLActivity implements View.OnClickListener,
		FlingGestureListener.OnFlingCompletedListener, RealTimeCommunicationListener,
		SharePeopleAdapter.OnShareElementClickedListener, SearchHelper.OnQuerySubmitted,
		OnMissingConnectionListener {


	private View rootContent;

	private View baseLayout;
	private View btnComments;
	private View btnShares;

	/* HEARTS */
	private View sectionHearts;
	private View iconSwipeHearts;
	private TextView heartsString;
	private RatingBar heartsRatingBar;
	private Integer chosenRating;
	private Boolean ratingSent = null;

	/* COMMENTS */
	private View sectionComments;
	private ImageView authorProfilePic;
	private EditText commentField;

	/* SHARES */
	private View sectionShares;
	private View sharesTextSearchContainer;
	private View sectionSharesText;
	private View sectionSharesSearch;
	private View sectionSharesEditText;
	private View searchBtn;
	private float initialExpViewX;
	private RecyclerView peopleRecView;
	private EditText searchField;
	private TextView sharesSelected;
	private SharePeopleAdapter peopleAdapter;
	private LinearLayoutManager peopleLlm;
	private List<Object> sharesValues = new ArrayList<>();

	private SearchHelper mSearchHelper;

	/* CONFIRMATION OVERLAY */
	private View confirmationOverlay;
	private TextView confirmationCaption;
	private ImageView targetProfilePic;

	private View confirmationHearts;
	private ImageView heart1, heart2, heart3, heart4, heart5;

	private ImageView confirmCommSharesIcon;

	private String mPostId;
	private Post mPost;
	private boolean
			activeHearts = true,
			activeComment = false,
			activeShare = false;

	private GestureDetector gdt;

	private String interactionIdFromServer = null;
	private InteractionHeart currentInterHeart = null;
	private InteractionComment currentInterComment = null, currentInterCommentParent;
	private String parentCommentId = null;
	private InteractionShare currentInterShare = null;

	private boolean replyAddEditComment = false;
	private int resultType;
	private JSONArray responseObject;

	private BackgroundHelper backgroundHelper;

	private Map<String, HLCircle> cSelMap = new HashMap<>();
	private List<HLCircle> circlesList = new ArrayList<>();
	private Map<String, HLUserGeneric> fSelMap = new HashMap<>();
	private List<HLUserGeneric> friendsList = new ArrayList<>();

	private boolean actingAsInterest = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post_overlay);
		setRootContent(rootContent = findViewById(R.id.root_content));

		if (Utils.hasLollipop()) {
			getWindow().setEnterTransition(new Fade());
			getWindow().setExitTransition(new Fade());
		}
		baseLayout = findViewById(R.id.base_layout);

		btnComments = findViewById(R.id.comment_btn);
		btnComments.setVisibility(mPost == null || !mPost.canCommentPost() || replyAddEditComment ? View.GONE : View.VISIBLE);

		btnShares = findViewById(R.id.share_btn);
		btnShares.setVisibility(mPost == null || !mPost.isPublic() ? View.GONE : View.VISIBLE);

		/* HEARTS */
		sectionHearts = findViewById(R.id.section_hearts);
		iconSwipeHearts = findViewById(R.id.swipe_icon_hearts);
		heartsString = findViewById(R.id.changing_string);
		heartsRatingBar = findViewById(R.id.rating_hearts);
		heartsRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
				setHeartsSection(true);
				chosenRating = ((int) rating);
			}
		});

		/* COMMENTS */
		sectionComments = findViewById(R.id.section_comment);
		authorProfilePic = findViewById(R.id.author_s_avatar);
		commentField = findViewById(R.id.comment_et);


		actingAsInterest = mUser.getSelectedIdentity() != null && mUser.getSelectedIdentity().isInterest();

		/* SHARES */
		peopleAdapter = new SharePeopleAdapter(sharesValues, this, actingAsInterest);
		peopleAdapter.setHasStableIds(true);
		peopleLlm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
		sectionShares = findViewById(R.id.section_share);
		sharesTextSearchContainer = findViewById(R.id.super_container);
		sectionSharesText = findViewById(R.id.share_text_container);
		sectionSharesEditText = findViewById(R.id.search_field);
		sharesSelected = findViewById(R.id.shared_with);
		sectionSharesSearch = findViewById(R.id.share_search_container);
		searchField = findViewById(R.id.share_search);
//		searchField.addTextChangedListener(new SearchTextWatcher(this, SearchTextWatcher.SearchType.SINGLE_CHAR));

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(sharesTextSearchContainer, searchField);

		peopleRecView = findViewById(R.id.share_rec_view);

		/* CONFIRMATION */
		confirmationOverlay = findViewById(R.id.confirmation_overlay);
		targetProfilePic = findViewById(R.id.confirmation_profile_pic);
		confirmationCaption = findViewById(R.id.confirmation_caption);

		confirmationHearts = findViewById(R.id.confirmation_hearts);
		heart1 = findViewById(R.id.h_1);
		heart2 = findViewById(R.id.h_2);
		heart3 = findViewById(R.id.h_3);
		heart4 = findViewById(R.id.h_4);
		heart5 = findViewById(R.id.h_5);

		confirmCommSharesIcon = findViewById(R.id.confirmation_big_icon);

		gdt = new GestureDetector(this, new FlingGestureListener(this));

		manageIntent();
		backgroundHelper = new BackgroundHelper(this, R.id.background_container, mPost);
		backgroundHelper.configureBackground();

		RealTimeCommunicationHelperKotlin.Companion.getInstance(this).setListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_MEMORY_INTERACTIONS_ADD);

		actingAsInterest = mUser.getSelectedIdentity() != null && mUser.getSelectedIdentity().isInterest();

		if (mPost == null && Utils.isStringValid(mPostId))
			mPost = HLPosts.getInstance().getPost(mPostId);

		peopleRecView.setLayoutManager(peopleLlm);
		peopleRecView.setAdapter(peopleAdapter);

		setBaseLayout();
	}

	@Override
	protected void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchField);
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	public void onClick(View v) {
		int id = v.getId();

		switch (id) {
			case R.id.close:
				Utils.closeKeyboard(commentField);
				setResult(RESULT_CANCELED);
				finish();
				overridePendingTransition(R.anim.no_animation, android.R.anim.fade_out);
				break;

			case R.id.comment_btn:
				setCommentSectionAndOpen(true);
				break;

			case R.id.share_btn:
				callForCircles();
				setShareSectionAndOpen(true);
				break;

			case R.id.searchButton:
				initialExpViewX = sectionSharesSearch.getX();
				sectionSharesText.setVisibility(View.GONE);
				sectionSharesEditText.setVisibility(View.VISIBLE);
				Utils.openKeyboard(searchField);

//				sectionSharesSearch.animate().x(0f).setDuration(400).setListener(new Animator.AnimatorListener() {
//					@Override
//					public void onAnimationStart(Animator animation) {
//						sectionSharesText.setVisibility(View.GONE);
//						sectionSharesEditText.setVisibility(View.VISIBLE);
//					}
//
//					@Override
//					public void onAnimationEnd(Animator animation) {
//						if (searchField != null) {
//							searchField.requestFocus();
//							Utils.openKeyboard(searchField);
//						}
//					}
//
//					@Override
//					public void onAnimationCancel(Animator animation) {}
//
//					@Override
//					public void onAnimationRepeat(Animator animation) {}
//				}).start();
				break;

			case R.id.close_search_btn:
				sectionSharesText.setVisibility(View.VISIBLE);
				sectionSharesEditText.setVisibility(View.GONE);
				Utils.closeKeyboard(searchField);
				setSharingData();

//				sectionSharesSearch.animate().x(initialExpViewX)
//						.setDuration(400).setListener(new Animator.AnimatorListener() {
//					@Override
//					public void onAnimationStart(Animator animation) {
//						sectionSharesEditText.setVisibility(View.GONE);
//						sectionSharesText.setVisibility(View.VISIBLE);
//					}
//
//					@Override
//					public void onAnimationEnd(Animator animation) {
//						Utils.closeKeyboard(searchField);
//					}
//
//					@Override
//					public void onAnimationCancel(Animator animation) {}
//
//					@Override
//					public void onAnimationRepeat(Animator animation) {}
//				}).start();
				break;
		}
	}

	@Override
	public void onShareElementClick(Object o, int position) {
		if (o != null) {
			if (currentInterShare == null)
				currentInterShare = new InteractionShare();

			boolean selected = false;
			if (o instanceof HLUserGeneric)
				((HLUserGeneric) o).setSelected(selected = !((HLUserGeneric) o).isSelected());
			else if (o instanceof HLCircle)
				((HLCircle) o).setSelected(selected = !((HLCircle) o).isSelected());

			if (selected) {
				if (o instanceof HLCircle) {
					if (((HLCircle) o).getName().equals(Constants.INNER_CIRCLE_NAME)) {
						// tries to fix ConcurrentModificationException
						List<Object> manageable = new CopyOnWriteArrayList<>(sharesValues);
						if (manageable.size() > 1) {
							manageable.removeAll(manageable.subList(1, manageable.size()));
							sharesValues.clear();
							sharesValues.addAll(manageable);
							fSelMap.clear();
							cSelMap.clear();
						}
					}
					else {
						List<HLUserGeneric> users = ((HLCircle) o).getUsers();
						if (users != null && !users.isEmpty()) {
							for (HLUserGeneric u : users) {
								if (sharesValues.contains(u)) {
									sharesValues.remove(u);
									fSelMap.remove(u.getId());
								}
							}
						}
					}
					cSelMap.put(((HLCircle) o).getId(), (HLCircle) o);
				}
				else {
					fSelMap.put(((HLUserGeneric) o).getId(), (HLUserGeneric) o);
				}
				setSharingString();
				peopleAdapter.notifyDataSetChanged();
			}
			else {
				if (o instanceof HLCircle) {
					if (((HLCircle) o).getName().equals(Constants.INNER_CIRCLE_NAME)) {
						setSharingData(true);
					}
					else {
						List<HLUserGeneric> users = ((HLCircle) o).getUsers();
						if (users != null && !users.isEmpty()) {
							for (HLUserGeneric u : users) {
								u.setSelected(false);
								if (!sharesValues.contains(u)) {
									sharesValues.add(u);
								}
							}
						}

						peopleAdapter.notifyDataSetChanged();
					}
					cSelMap.remove(((HLCircle) o).getId());
				}
				else if (o instanceof HLUserGeneric) {
					fSelMap.remove(((HLUserGeneric) o).getId());
					peopleAdapter.notifyDataSetChanged();
				}

				setSharingString();
			}
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (responseObject == null) {
			LogUtils.e(LOG_TAG, "Something's not right with server response to: " + operationId);
			showGenericError();
			return;
		}

		this.responseObject = responseObject;
		InteractionPost.Type type = null;
		switch (operationId) {
			case Constants.SERVER_OP_SEND_HEARTS:
				ratingSent = true;
				type = InteractionPost.Type.HEARTS;
				interactionIdFromServer = responseObject.optJSONObject(0).optString("heartID");
				break;
			case Constants.SERVER_OP_SEND_COMMENT:
			case Constants.SERVER_OP_MANAGE_COMMENT:
				type = InteractionPost.Type.COMMENT;
				interactionIdFromServer = responseObject.optJSONObject(0).optString("commentID");
				break;
			case Constants.SERVER_OP_SHARE_POST:
				type = InteractionPost.Type.SHARE;
				interactionIdFromServer = responseObject.optJSONObject(0).optString("_id");
				break;

			case Constants.SERVER_OP_GET_CIRCLE:
				try {
					parseSharingData(responseObject);
				} catch (JSONException e) {
					LogUtils.e(LOG_TAG, e.getMessage(), e);
				}
				return;
		}

		if (type != null) {
			showOverlay(type);
			try {
				if (RealmUtils.isValid(realm)) {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							HLPosts posts = HLPosts.getInstance();

							if (currentInterHeart != null) {
								currentInterHeart.setId(interactionIdFromServer);
								mPost.getInteractionsHeartsPost().add(currentInterHeart);

								// removes hearts if needed
								int toAdd = chosenRating;
								if (mPost.getHeartsLeft() != null)
									toAdd = chosenRating - mPost.getHeartsLeft();
								mPost.setCountHeartsPost(mPost.getCountHeartsPost() + toAdd);

								mPost.setHeartsLeft(chosenRating);
								mPost.setHeartsLeftID(interactionIdFromServer);

								if (mPost.isCHInitiative() || mPost.isGSInitiative())
									posts.updateAuthorHeartsForAllPosts(mPost.getInitiative().getRecipient(), -1, toAdd);
								else if (mPost.isGSSecondaryPost())
									posts.updateAuthorHeartsForAllPosts(mPost.getGSRecipient(), -1, toAdd);
								else {
									mPost.setCountHeartsUser(mPost.getCountHeartsUser() + toAdd);
									posts.updateAuthorHeartsForAllPosts(mPost);
								}
							}
							else if (currentInterComment != null) {
								currentInterComment.setId(interactionIdFromServer);
								if (!mPost.getInteractionsComments().contains(currentInterComment)) {
									posts.updateCommentsForNewComment(mPostId, currentInterComment);
									mPost.setCountComments(mPost.getCountComments() + 1);
									mPost.setYouLeftComments(true);
								}
							}
							else if (currentInterShare != null) {
								currentInterShare.setId(interactionIdFromServer);
								mPost.getInteractionsShares().add(currentInterShare);
								mPost.setCountShares(mPost.getCountShares() + 1);
								mPost.setYouDidShares(true);
							}

							if (posts.isPostToBePersisted(mPostId))
								posts.setBackupPostInTransaction(mPost, realm);
						}
					});
				}
			}
			catch (Exception e) {
				LogUtils.e(LOG_TAG, e.getMessage(), e);
			}
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_SEND_HEARTS:
			case Constants.SERVER_OP_SEND_COMMENT:
			case Constants.SERVER_OP_SHARE_POST:
			case Constants.SERVER_OP_MANAGE_COMMENT:
				showGenericError();
				break;

			case Constants.SERVER_OP_GET_CIRCLE:
				showAlert(R.string.error_generic_list);
				break;
		}
		LogUtils.e(LOG_TAG, "Error in server response for code " + operationId);
	}

	@Override
	public void onMissingConnection(int operationId) {

	}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(Constants.EXTRA_PARAM_1)) {
				mPostId = intent.getStringExtra(Constants.EXTRA_PARAM_1);
				if (Utils.isStringValid(mPostId))
					mPost = HLPosts.getInstance().getPost(mPostId);
			}
			if (replyAddEditComment = intent.hasExtra(Constants.EXTRA_PARAM_2)) {
				resultType = intent.getIntExtra(Constants.EXTRA_PARAM_2, -1);
			}
			if (intent.hasExtra(Constants.EXTRA_PARAM_3)) {
				String commentToManageId = intent.getStringExtra(Constants.EXTRA_PARAM_3);
				if (Utils.isStringValid(commentToManageId)) {
					InteractionComment comment = HLPosts.getInstance().getCommentInteractionById(mPostId, commentToManageId);
					if (resultType == Constants.RESULT_EDIT_COMMENT)
						currentInterComment = comment;
					else if (resultType == Constants.RESULT_REPLY_TO_COMMENT) {
						currentInterCommentParent = comment;

						// INFO: 2/5/19
						// from now on also comments wih level == 1 can have replies, but it's only for visualization.
						// currentInterCommentParent is always the one clicked by users, bu the parentID is always the level-0-comment's ID
						parentCommentId = comment.isSubComment() ? comment.getParentCommentID() : comment.getId();
					}
				}
			}
			if (intent.hasExtra(Constants.EXTRA_PARAM_4)) {
				String parentCommentIdIfEdit = intent.getStringExtra(Constants.EXTRA_PARAM_4);
				if (Utils.isStringValid(parentCommentIdIfEdit) && resultType == Constants.RESULT_EDIT_COMMENT)
					currentInterCommentParent = HLPosts.getInstance().getCommentInteractionById(mPostId, parentCommentIdIfEdit);
			}
		}
	}


	private void setBaseLayout() {
		if (mPost != null) {

			// sets background ImageView if it is reply/edit mode
			if (backgroundHelper != null && replyAddEditComment)
				backgroundHelper.setBackground();

			if (ratingSent == null)
				ratingSent = mPost.hasUserAlreadySentHearts();

			if (ratingSent)
				heartsRatingBar.setRating(chosenRating != null ? chosenRating : mPost.getHeartsLeft());
			activeHearts = true;

			iconSwipeHearts.setVisibility(ratingSent ? View.GONE : View.VISIBLE);
			heartsString.setVisibility(View.VISIBLE);

			setHeartsSection(false);
			setCommentSectionAndOpen(mPost.canCommentPost() && replyAddEditComment);
			setShareSectionAndOpen(false);

			btnComments.setVisibility(mPost == null || !mPost.canCommentPost() || replyAddEditComment ? View.GONE : View.VISIBLE);
			btnShares.setVisibility(mPost == null || !mPost.isPublic() ? View.GONE : View.VISIBLE);
		}
	}

	private void setHeartsSection(boolean ratingStarted) {
		if (!replyAddEditComment) {
			boolean heartsVisible = mPost.canPostBeHeartedByUser(realm);
			activeHearts = heartsVisible;

			iconSwipeHearts.setVisibility((ratingStarted) ? View.VISIBLE : View.GONE);

			if (!ratingStarted) {
				String firstName = mPost.getFirstNameForUI();
				heartsString.setText(Utils.getFormattedHtml(getString(R.string.send_hearts, firstName)));
			} else
				heartsString.setText(R.string.swipe_up_to_send_hearts);

			sectionHearts.setVisibility(!heartsVisible ? View.GONE : View.VISIBLE);
			sectionComments.setVisibility(View.GONE);
		}
	}

	private void setCommentSectionAndOpen(boolean open) {
		if (open) {
			activeComment = true;
			activeHearts = false;
			activeShare = false;

			MediaHelper.loadProfilePictureWithPlaceholder(this, mUser.getAvatarURL(), authorProfilePic);

			sectionHearts.setVisibility(View.GONE);
			btnComments.setVisibility(View.GONE);
			btnShares.setVisibility(View.GONE);

			if (currentInterComment != null && resultType == Constants.RESULT_EDIT_COMMENT)
				commentField.setText(currentInterComment.getMessage());
			else if (currentInterCommentParent != null && resultType == Constants.RESULT_REPLY_TO_COMMENT)
				commentField.setHint(getString(R.string.reply_to, currentInterCommentParent.getAuthor()));

			sectionComments.setVisibility(View.VISIBLE);
			Utils.openKeyboard(commentField);
		}
		else
			btnComments.setVisibility(mPost.canCommentPost() ? View.VISIBLE : View.GONE);
	}

	private void setShareSectionAndOpen(boolean open) {
		if (open) {
			activeComment = false;
			activeHearts = false;
			activeShare = true;

			sectionHearts.setVisibility(View.GONE);
			btnComments.setVisibility(View.GONE);
			btnShares.setVisibility(View.GONE);

			sectionShares.setVisibility(View.VISIBLE);
		}
		else {
			btnShares.setVisibility(mPost.isPublic() ? View.VISIBLE : View.GONE);
			sectionShares.setVisibility(View.GONE);
		}
	}

	private void setOverlay(InteractionPost.Type type) {
		if (Utils.isContextValid(this) && mPost != null) {
			int visibHearts = (type == InteractionPost.Type.HEARTS) ? View.VISIBLE : View.GONE;
			int visibIcon = (type == InteractionPost.Type.COMMENT || type == InteractionPost.Type.SHARE) ?
					View.VISIBLE : View.GONE;

			confirmationHearts.setVisibility(visibHearts);
			confirmCommSharesIcon.setVisibility(visibIcon);

			boolean isEditOfReply = (resultType == Constants.RESULT_EDIT_COMMENT) && currentInterComment.isSubComment();
			boolean isReply = (resultType == Constants.RESULT_REPLY_TO_COMMENT);
			String picture = mPost.getAuthorUrl();
			String name = mPost.getFirstNameForUI();
			if ((isReply || isEditOfReply) && currentInterCommentParent != null) {
				picture = currentInterCommentParent.getAuthorUrl();
				name = currentInterCommentParent.getAuthor();
			}

			if (targetProfilePic != null) {
				MediaHelper.loadProfilePictureWithPlaceholder(this,
						picture,
						targetProfilePic);
			}

			if (type == InteractionPost.Type.HEARTS) {
				switch (chosenRating) {
					case 1:
						heart1.setVisibility(View.VISIBLE);
						heart2.setVisibility(View.GONE);
						heart3.setVisibility(View.GONE);
						heart4.setVisibility(View.GONE);
						heart5.setVisibility(View.GONE);
						break;
					case 2:
						heart1.setVisibility(View.VISIBLE);
						heart2.setVisibility(View.VISIBLE);
						heart3.setVisibility(View.GONE);
						heart4.setVisibility(View.GONE);
						heart5.setVisibility(View.GONE);
						break;
					case 3:
						heart1.setVisibility(View.VISIBLE);
						heart2.setVisibility(View.VISIBLE);
						heart3.setVisibility(View.VISIBLE);
						heart4.setVisibility(View.GONE);
						heart5.setVisibility(View.GONE);
						break;
					case 4:
						heart1.setVisibility(View.VISIBLE);
						heart2.setVisibility(View.VISIBLE);
						heart3.setVisibility(View.VISIBLE);
						heart4.setVisibility(View.VISIBLE);
						heart5.setVisibility(View.GONE);
						break;
					case 5:
						heart1.setVisibility(View.VISIBLE);
						heart2.setVisibility(View.VISIBLE);
						heart3.setVisibility(View.VISIBLE);
						heart4.setVisibility(View.VISIBLE);
						heart5.setVisibility(View.VISIBLE);
						break;
				}

				confirmationCaption.setText(
						Utils.getFormattedHtml(
								String.format(
										Locale.getDefault(),
										getResources().getQuantityString(
												R.plurals.post_interaction_confirm_hearts,
												chosenRating
										),
										name,
										chosenRating
								)
						)
				);
			} else {
				@DrawableRes int confIcon = 0;
				String confString = "";
				switch (type) {
					case COMMENT:
						confIcon = R.drawable.ic_envelope;
						if (mPost.isActiveUserAuthor(realm)) {
							@StringRes int resId = R.string.post_interaction_confirm_comments_own_post;
							if (isEditOfReply || isReply)
								resId = R.string.post_interaction_confirm_reply_own_post;
							confString = getString(resId);
						}
						else {
							@StringRes int resId = R.string.post_interaction_confirm_comments;
							if (isEditOfReply || isReply)
								resId = R.string.post_interaction_confirm_reply;
							confString = getString(resId, name);
						}
						break;

					case SHARE:
						confIcon = R.drawable.ic_share;
						confString = getString(R.string.post_interaction_confirm_shares);
						break;
				}
				confirmCommSharesIcon.setImageResource(confIcon);
				confirmationCaption.setText(confString);
			}
		}
	}

	private void showOverlay(InteractionPost.Type type) {
		setOverlay(type);
		baseLayout.animate().alpha(0).setDuration(400).setListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {}

			@Override
			public void onAnimationEnd(Animator animation) {
				baseLayout.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationCancel(Animator animation) {}

			@Override
			public void onAnimationRepeat(Animator animation) {}
		});

		confirmationOverlay.animate().alpha(1).setDuration(400).setStartDelay(100).setListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				confirmationOverlay.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animator animation) {}

			@Override
			public void onAnimationCancel(Animator animation) {}

			@Override
			public void onAnimationRepeat(Animator animation) {}
		});

		Utils.closeKeyboard(commentField);
		new Handler().postDelayed(closeOverlay, 2000);
	}

	private final Runnable closeOverlay = new Runnable() {
		@Override
		public void run() {
			if (confirmationOverlay != null && baseLayout != null) {
				confirmationOverlay.animate().alpha(0).setDuration(400).setListener(new Animator.AnimatorListener() {
					@Override
					public void onAnimationStart(Animator animation) {}

					@Override
					public void onAnimationEnd(Animator animation) {
						confirmationOverlay.setVisibility(View.GONE);
					}

					@Override
					public void onAnimationCancel(Animator animation) {}

					@Override
					public void onAnimationRepeat(Animator animation) {}
				});

				baseLayout.animate().alpha(1).setDuration(400).setStartDelay(100).setListener(new Animator.AnimatorListener() {
					@Override
					public void onAnimationStart(Animator animation) {
						currentInterHeart = null;
						currentInterComment = null;
						currentInterShare = null;

						activeComment = false;
						commentField.setText("");
						activeShare = false;

						setBaseLayout();

						btnComments.setVisibility(mPost == null || !mPost.canCommentPost() || replyAddEditComment ? View.GONE : View.VISIBLE);
						btnShares.setVisibility(mPost == null || !mPost.isPublic() ? View.GONE : View.VISIBLE);
						baseLayout.setVisibility(View.VISIBLE);
					}

					@Override
					public void onAnimationEnd(Animator animation) {
						if (replyAddEditComment) {
							Intent intent = new Intent();
							intent.putExtra(Constants.EXTRA_PARAM_1, responseObject.toString());
							setResult(RESULT_OK, intent);
						}
						else
							setResult(RESULT_OK);
						finish();
						overridePendingTransition(R.anim.no_animation, android.R.anim.fade_out);

						Utils.closeKeyboard(commentField);
					}

					@Override
					public void onAnimationCancel(Animator animation) {}

					@Override
					public void onAnimationRepeat(Animator animation) {}
				});
			}
		}
	};

	/**
	 * Calls for the user's circles and friends to display for sharing.
	 * TEMPORARILY it calls for all the circles and friends together, WITHOUT pagination.
	 */
	private void callForCircles() {
		Object[] results = null;
		try {
			results = HLServerCalls.getCircles(mUser.getId(), mUser.getCircles(), null, -1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((LBSLinkApp) getApplication())).handleCallResult(this, this, results);
	}

	private void setSharingData() {
		setSharingData(false);
	}

	private void setSharingData(boolean wipeSelections) {
		sharesValues.clear();
		Collections.sort(circlesList, HLCircle.CircleNameComparator);
		if (wipeSelections)
			deselectAllCircles(circlesList);
		sharesValues.addAll(circlesList);
		Collections.sort(friendsList);
		if (wipeSelections)
			deselectAllCircles(friendsList);
		sharesValues.addAll(friendsList);

		peopleAdapter.notifyDataSetChanged();

		mSearchHelper.handleText(true);
	}

	private void deselectAllCircles(List<?> list) {
		if (list != null && !list.isEmpty()) {
			for (Object obj : list) {
				if (obj instanceof HLUserGeneric)
					((HLUserGeneric) obj).setSelected(false);
				else if (obj instanceof HLCircle)
					((HLCircle) obj).setSelected(false);
			}
		}
	}

	private void parseSharingData(JSONArray responseObject) throws JSONException {
		if (responseObject != null && responseObject.length() > 0) {
			if (circlesList == null) circlesList = new ArrayList<>();
			else circlesList.clear();
			if (friendsList == null) friendsList = new ArrayList<>();
			else friendsList.clear();
			for (int i = 0; i < responseObject.length(); i++) {
				JSONObject json = responseObject.optJSONObject(0);
				JSONArray lists = json.optJSONArray("lists");
				JSONArray users = json.optJSONArray("users");
				if (lists != null && lists.length() > 0) {
					for (int j = 0; j < lists.length(); j++) {
						HLCircle cir = new HLCircle().deserializeToClass(lists.getJSONObject(j));
						circlesList.add(cir);

						// adds check to block further consuming operations if it is an interest
						if (actingAsInterest && lists.length() == 1)
							break;

						if (cir.getUsers() != null && !cir.getUsers().isEmpty()) {
							for (HLUserGeneric ug : cir.getUsers()) {
								if (ug != null && !friendsList.contains(ug))
									friendsList.add(ug);
							}
						}
					}
				}

				if (!actingAsInterest && users != null && users.length() > 0) {
					for (int j = 0; j < users.length(); j++) {
						HLUserGeneric u = new HLUserGeneric().deserializeToClass(users.getJSONObject(j));
						if (u != null && !friendsList.contains(u))
							friendsList.add(u);
					}
				}
			}

			setSharingData(true);
		}
	}

	private void setSharingString() {
		if (sharesSelected != null) {
			StringBuilder sb = new StringBuilder();
			if (cSelMap != null && !cSelMap.isEmpty()) {
				if (actingAsInterest)
					sb.append(getString(R.string.all_followers)).append(", ");
				else {
					List<HLCircle> circles = new ArrayList<>(cSelMap.values());
					Collections.sort(circles, HLCircle.CircleNameComparator);
					for (HLCircle c : circles)
						sb.append(c.getNameToDisplay()).append(", ");
				}
			}
			if (fSelMap != null && !fSelMap.isEmpty()) {
				List<HLUserGeneric> users = new ArrayList<>(fSelMap.values());
				Collections.sort(users);
				for (HLUserGeneric u : users)
					sb.append(u.getName()).append(", ");
			}
			if (sb.length() > 1)
				sb.delete(sb.length() - 2, sb.length() - 1);
			sharesSelected.setText(sb.toString());
		}
	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		List<Object> queryResults = new ArrayList<>();
		sharesValues.clear();
		if (Utils.isStringValid(query)) {
			if (circlesList != null && !circlesList.isEmpty()) {
				queryResults.addAll(Stream.of(circlesList).filter(new Predicate<HLCircle>() {
					@Override
					public boolean test(HLCircle circle) {
						return circle.getNameToDisplay().toLowerCase().contains(query);
					}
				}).collect(Collectors.toList()));
			}

			if (friendsList != null && !friendsList.isEmpty()) {
				queryResults.addAll(Stream.of(friendsList).filter(new Predicate<HLUserGeneric>() {
					@Override
					public boolean test(HLUserGeneric user) {
						return user.getName().toLowerCase().contains(query);
					}
				}).collect(Collectors.toList()));
			}
			sharesValues.addAll(queryResults);

			if (sharesValues.isEmpty())
				setSharingData();
			else
				peopleAdapter.notifyDataSetChanged();
		}
		else {
			setSharingData();
		}
	}


	//region == Gesture methods ==

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		gdt.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public void onFlingCompleted() {
		try {
			if (activeShare) {
				if ((cSelMap == null || cSelMap.isEmpty()) &&
						(fSelMap == null || fSelMap.isEmpty())) {
					showAlert(R.string.error_no_selection_to_share);
					return;
				}

				currentInterShare = new InteractionShare();
				currentInterShare.setAuthor(mUser.getCompleteName());
				currentInterShare.setAuthorUrl(mUser.getAvatarURL());
				currentInterShare.setAuthorId(mUser.getId());
				currentInterShare.setCreationDate(new Date());

				Object[] results = HLServerCalls.sharePost(currentInterShare, cSelMap, fSelMap, mPostId);
				HLRequestTracker.getInstance((OnApplicationContextNeeded) getApplication())
						.handleCallResult(this, this, results);
			}
			else {
				if (activeHearts) {
					if (chosenRating == null || chosenRating == 0) {
						showAlert(R.string.error_no_rating_chosen);
						return;
					}

					currentInterHeart = new InteractionHeart();
					currentInterHeart.setAuthor(mUser.getCompleteName());
					currentInterHeart.setAuthorUrl(mUser.getAvatarURL());
					currentInterHeart.setAuthorId(mUser.getId());
					currentInterHeart.setCreationDate(new Date());

					chosenRating = (int) heartsRatingBar.getRating();
					currentInterHeart.setCount(chosenRating);

					Object[] results = HLServerCalls.sendHeartsOrComments(currentInterHeart, null, mUser, mPost);
					HLRequestTracker.getInstance((OnApplicationContextNeeded) getApplication())
							.handleCallResult(this, this, results);
				}
				else if (activeComment) {
					if (currentInterComment == null) {
						currentInterComment = new InteractionComment();
						currentInterComment.setAuthor(mUser.getCompleteName());
						currentInterComment.setAuthorUrl(mUser.getAvatarURL());
						currentInterComment.setAuthorId(mUser.getId());
						currentInterComment.setCreationDate(new Date());
						currentInterComment.setLevel(resultType == Constants.RESULT_REPLY_TO_COMMENT ? 1 : 0);
						currentInterComment.setParentCommentID(resultType == Constants.RESULT_REPLY_TO_COMMENT ? parentCommentId : "");
						currentInterComment.setTotHearts(mUser.getTotHearts());
					}

					currentInterComment.setMessage(commentField.getText().toString());

					Object[] results;
					if (resultType == Constants.RESULT_EDIT_COMMENT)
						results = HLServerCalls.manageComment(currentInterComment, mUser, mPostId);
					else
						results = HLServerCalls.sendHeartsOrComments(null, currentInterComment, mUser, mPost);

					HLRequestTracker.getInstance((OnApplicationContextNeeded) getApplication())
							.handleCallResult(this, this, results);
				}
			}
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage());
			e.printStackTrace();
			showGenericError();
		}
	}

	//endregion


	//region == Real-time communication interface methods ==

	@Override
	public void onPostAdded(Post post, int position) {}

	@Override
	public void onPostUpdated(@NonNull String postId, int position) {}

	@Override
	public void onPostDeleted(int position) {}

	@Override
	public void onHeartsUpdated(int position) {}

	@Override
	public void onSharesUpdated(int position) {}

	@Override
	public void onTagsUpdated(int position) {}

	@Override
	public void onCommentsUpdated(int position) {}

	@Override
	public void onNewDataPushed(boolean hasInsert) {}

	@Override
	public void registerRealTimeReceiver() {
////		registerReceiver(realTimeHelper.getServerMessageReceiver(), new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));
//		LocalBroadcastManager.getInstance(this).registerReceiver(realTimeHelper.getServerMessageReceiver(), new IntentFilter(Constants.BROADCAST_REALTIME_COMMUNICATION));
	}

	@Override
	public void unregisterRealTimeReceiver() {
////		unregisterReceiver(realTimeHelper.getServerMessageReceiver());
//		LocalBroadcastManager.getInstance(this).unregisterReceiver(realTimeHelper.getServerMessageReceiver());
	}

	@Override
	public Realm getRealm() {
		return realm;
	}

	//endregion

}
