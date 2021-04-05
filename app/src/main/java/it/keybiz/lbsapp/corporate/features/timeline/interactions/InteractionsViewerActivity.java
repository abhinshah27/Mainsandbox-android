/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Predicate;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnApplicationContextNeeded;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.connection.realTime.RealTimeCommunicationHelperKotlin;
import it.keybiz.lbsapp.corporate.features.timeline.RealTimeCommunicationListener;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.InteractionComment;
import it.keybiz.lbsapp.corporate.models.InteractionHeart;
import it.keybiz.lbsapp.corporate.models.InteractionPost;
import it.keybiz.lbsapp.corporate.models.InteractionShare;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.LoadMoreResponseHandlerTask;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;
import it.keybiz.lbsapp.corporate.utilities.listeners.SlidingPanelHandler;

public class InteractionsViewerActivity extends HLActivity implements View.OnClickListener,
		OnServerMessageReceivedListener, RealTimeCommunicationListener,
		LoadMoreResponseHandlerTask.OnDataLoadedListener,
		InteractionHeartsAdapter.OnHeartActionListener,
		InteractionSharesAdapter.OnShareActionListener,
		InteractionCommentsAdapter.OnCommentActionListener, OnMissingConnectionListener {

	private View mainContainer;

	private SlidingUpPanelLayout slidingLayout;
	private View itemReply, itemEdit, itemDelete;
	private SlidingPanelHandler handlerOwn;

	private View interactionsWrapper;
	private RecyclerView interactionsList;
	private List<InteractionHeart> heartsList = new ArrayList<>();
	private List<InteractionComment> commentsList = new ArrayList<>();
	private List<InteractionShare> sharesList = new ArrayList<>();
	private InteractionHeartsAdapter heartsAdapter;
	private InteractionSharesAdapter sharesAdapter;
	private InteractionCommentsAdapter commentsAdapter;
	private LinearLayoutManager llm;

	private View listsView;
	private ViewGroup listsContainer;
	private View createNewListBtn;

	private View bottomSectionLower;
	private TextView cntHeartsPost;
	private TextView cntCommentsPost;
	private TextView cntSharesPost;

	private ImageView ivHearts;
	private ImageView ivComments;
	private ImageView ivShares;
	private ImageView ivLists;
	private TransitionDrawable tdHearts;
	private TransitionDrawable tdComments;
	private TransitionDrawable tdShares;
	private TransitionDrawable tdLists;
	private ImageView triangle_lists;
	private ImageView triangle_shares;
	private ImageView triangle_comments;
	private ImageView triangle_hearts;

	private String currentListSelectionName;
	private int currentListSelectionId;

	private int[] padding;
	private InteractionPost.Type type;
	private Post mPost;
	private String mPostId;
	boolean hUp = false, cUp = false, sUp = false, pUp = false;

	@IdRes private int viewHitResId = -1;

	private JSONArray responseObject;

	private boolean fromLoadMore = false;
	private int newItemsCount = 0;

	private MaterialDialog createListDialog;


	// new comment placeholder section
    private View commentPlaceholder;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_interactions_rvs);
		setRootContent(findViewById(R.id.root_content));

		if (Utils.hasLollipop()) {
			getWindow().setEnterTransition(new Fade());
			getWindow().setExitTransition(new Fade());
		}

		RealTimeCommunicationHelperKotlin.Companion.getInstance(this).setListener(this);

		manageIntent();

		configureCommentOptionsSheets();

		mainContainer = findViewById(R.id.main_container);

		interactionsWrapper = findViewById(R.id.interactions_wrapper);
		interactionsWrapper.setVisibility(View.GONE);
		interactionsList = findViewById(R.id.interactions_rec_view);
		interactionsList.setHasFixedSize(false);
		interactionsList.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				call(true);
				fromLoadMore = true;
			}
		});
		heartsAdapter = new InteractionHeartsAdapter(heartsList);
		heartsAdapter.setHasStableIds(true);
		commentsAdapter = new InteractionCommentsAdapter(commentsList, mUser, mPost);
		commentsAdapter.setHasStableIds(true);
		sharesAdapter = new InteractionSharesAdapter(sharesList);
		sharesAdapter.setHasStableIds(true);
		llm = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
		interactionsList.setLayoutManager(llm);
		interactionsList.setAdapter(heartsAdapter);

		bottomSectionLower = findViewById(R.id.bottom_section);
		cntHeartsPost = findViewById(R.id.count_hearts_post);
		cntCommentsPost = findViewById(R.id.count_comments);
		cntSharesPost = findViewById(R.id.count_shares);

		listsView = findViewById(R.id.lists_rv);
		listsContainer = findViewById(R.id.lists_item_container);
		createNewListBtn = findViewById(R.id.create_new_btn);

		ivHearts = findViewById(R.id.icon_hearts);
		ivHearts.setImageResource((mPost.getHeartsLeft() != null && mPost.getHeartsLeft() > 0) ?
				R.drawable.transition_stars_accent : R.drawable.transition_stars);
		tdHearts = (TransitionDrawable) ivHearts.getDrawable();
		tdHearts.setCrossFadeEnabled(true);

		ivComments = findViewById(R.id.icon_comments);
		ivComments.setImageResource(mPost.isYouLeftComments() ? R.drawable.transition_comments_orange : R.drawable.transition_comments);
		tdComments = (TransitionDrawable) ivComments.getDrawable();
		tdComments.setCrossFadeEnabled(true);

		ivShares = findViewById(R.id.icon_shares);
		ivShares.setImageResource(mPost.isYouDidShares() ? R.drawable.transition_shares_orange : R.drawable.transition_shares);
		tdShares = (TransitionDrawable) ivShares.getDrawable();
		tdShares.setCrossFadeEnabled(true);


		ivLists = findViewById(R.id.icon_pin);
		ivLists.setImageResource(mPost.hasLists() ? R.drawable.transition_pin_orange : R.drawable.transition_pin);
		tdLists = (TransitionDrawable) ivLists.getDrawable();
		tdLists.setCrossFadeEnabled(true);
		triangle_hearts = findViewById(R.id.triangle_hearts);
		triangle_comments =  findViewById(R.id.triangle_comments);
		triangle_shares = findViewById(R.id.triangle_shares);
		triangle_lists = findViewById(R.id.triangle_lists);


		commentPlaceholder = findViewById(R.id.comment_placeholder);
		commentPlaceholder.setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		configureResponseReceiver();

		initOverlay();
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_MEMORY_INTERACTIONS_VIEW);

		// TODO: 11/6/2017    se what to do with this
		getWindow().setWindowAnimations(0);


		registerRealTimeReceiver();

		if (Utils.isStringValid(mPostId) && mPost == null)
			mPost = HLPosts.getInstance().getPost(mPostId);

		if (mPost != null) {
			commentsAdapter.setPost(mPost);
			updateTextViews();
		}

		if (!hUp && !cUp && !sUp && !pUp)
			handleInteractions();
	}

	@Override
	protected void onPause() {
		super.onPause();

		setIntent(null);

		type = hUp ? InteractionPost.Type.HEARTS : (cUp ? InteractionPost.Type.COMMENT :
				(sUp ? InteractionPost.Type.SHARE : InteractionPost.Type.PIN));
		hUp = false; cUp = false; sUp = false; pUp = false;

		unregisterRealTimeReceiver();
	}

	@Override
	public void onBackPressed() {
		if (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
			slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
		else
			super.onBackPressed();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		findViewById(R.id.root_content).setVisibility(View.VISIBLE);

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case Constants.RESULT_EDIT_COMMENT:
				case Constants.RESULT_REPLY_TO_COMMENT:
					LBSLinkApp.pageIdComments = 1;
					type = InteractionPost.Type.COMMENT;
					viewHitResId = R.id.button_comments;
					hUp = false; cUp = false; sUp = false; pUp = false;

					String s = data != null ? data.getStringExtra(Constants.EXTRA_PARAM_1) : null;
					if (Utils.isStringValid(s)) {
						try {
							updateManagedComment(new JSONArray(s), false);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					break;
			}
		}
		else if (resultCode == RESULT_CANCELED) {

		}
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
			case R.id.root_content:
				if (isPanelOpen())
					closePanel();
				else {
					setResult(RESULT_CANCELED);
					finish();
				}
				break;

			case R.id.button_hearts:
			case R.id.button_shares:
				if (isPanelOpen())
					closePanel();
				else {
					if (mPost != null && mPost.isShowHeartsSharesDetails()) {
						boolean conditionHearts = mPost.getCountHeartsPost() > 0;
						boolean conditionShares = mPost.getCountShares() > 0;
						if (id == R.id.button_hearts ? conditionHearts : conditionShares) {
							viewHitResId = id;
							handleInteractions();
						}
					}
				}
				break;

			case R.id.button_comments:
			case R.id.button_pin:
				if (isPanelOpen())
					closePanel();
				else {
					if (id == R.id.button_pin || mPost.getCountComments() > 0) {
						viewHitResId = id;
						handleInteractions();
					}
				}
				break;

			// PANEL COMMENT
			case R.id.item_reply:
				replyToComment(currentComment);
				break;
			case R.id.item_edit:
				goToComment(true);
				break;
			case R.id.item_delete:
				deleteComment();
				break;

			// LISTS MANAGEMENT
			case R.id.create_new_btn:
				setCreateListDialogAndShow();
				break;

            case R.id.comment_placeholder:
                goToComment(false);
                break;
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		this.responseObject = responseObject;

		InteractionPost.Type type;
		switch (operationId) {
			case Constants.SERVER_OP_GET_HEARTS:
				type = InteractionPost.Type.HEARTS;
				break;
			case Constants.SERVER_OP_GET_COMMENTS:
				type = InteractionPost.Type.COMMENT;
				break;
			case Constants.SERVER_OP_GET_SHARES:
				type = InteractionPost.Type.SHARE;
				break;

			case Constants.SERVER_OP_LIKE_COMMENT:
				Toast.makeText(this, R.string.heart_sent_to_comment, Toast.LENGTH_SHORT).show();
				updateCommentForLike(responseObject);
				closePanel();
				return;
			case Constants.SERVER_OP_MANAGE_COMMENT:
				updateManagedComment(responseObject, true);
				closePanel();
				return;

			case Constants.SERVER_OP_FOLDERS_CREATE:
				DialogUtils.closeDialog(createListDialog);
				createListDialog = null;
				addList(currentListSelectionName, true);
				return;
			case Constants.SERVER_OP_FOLDERS_ADD_POST:
				addPostToList(currentListSelectionId, currentListSelectionName);
				return;
			case Constants.SERVER_OP_FOLDERS_REMOVE_POST:
				removePostFromList(currentListSelectionId, currentListSelectionName);
				return;

			default:
				return;
		}

		if (fromLoadMore) {
			new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.INTERACTIONS, type, mPostId)
					.execute(responseObject);
		}
		else {
			setData(realm, false);
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_GET_HEARTS:
			case Constants.SERVER_OP_GET_COMMENTS:
			case Constants.SERVER_OP_GET_SHARES:
			case Constants.SERVER_OP_LIKE_COMMENT:
			case Constants.SERVER_OP_FOLDERS_CREATE:
			case Constants.SERVER_OP_FOLDERS_ADD_POST:
			case Constants.SERVER_OP_FOLDERS_REMOVE_POST:
				showAlert(R.string.error_generic_list);
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {

	}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(Constants.EXTRA_PARAM_1))
				padding = intent.getIntArrayExtra(Constants.EXTRA_PARAM_1);
			if (intent.hasExtra(Constants.EXTRA_PARAM_2))
				type = (InteractionPost.Type) intent.getSerializableExtra(Constants.EXTRA_PARAM_2);
			if (intent.hasExtra(Constants.EXTRA_PARAM_3))
				mPostId = intent.getStringExtra(Constants.EXTRA_PARAM_3);

			if (Utils.isStringValid(mPostId))
				mPost = HLPosts.getInstance().getPost(mPostId);

			if (type != null) {
				switch (type) {
					case HEARTS:
						viewHitResId = R.id.button_hearts;
						break;
					case COMMENT:
						viewHitResId = R.id.button_comments;
						break;
					case SHARE:
						viewHitResId = R.id.button_shares;
						break;
					case PIN:
						viewHitResId = R.id.button_pin;
						break;
				}
			}
		}
	}


	//region == Load more methods ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return null;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		if (hUp)
			return heartsAdapter;
		else if (cUp)
			return commentsAdapter;
		else if (sUp)
			return sharesAdapter;

		return null;
	}

	@Override
	public void setData(Realm realm) {
		setData(realm, true);
	}

	@Override
	public void setData(JSONArray array) {}

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
		if (hUp)
			return LBSLinkApp.pageIdHearts;
		else if (cUp)
			return LBSLinkApp.pageIdComments;
		else if (sUp)
			return LBSLinkApp.pageIdShares;

		return 0;
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}

	//endregion


	//region == Custom class methods ==

	private void initOverlay() {
		int startEnd = Utils.dpToPx(16f, getResources());
		int startEndRec = Utils.dpToPx(8f, getResources());
		if (padding != null) {
			mainContainer.setPaddingRelative(0, padding[0], 0, padding[1]);
			bottomSectionLower.setPaddingRelative(startEnd, 0, 0, 0);
			interactionsList.setPaddingRelative(startEndRec, 0, 0, 0);
//			((FrameLayout.LayoutParams) mainContainer.getLayoutParams()).setMargins(startEnd, padding[0], 0, padding[1]);
		}
	}
	private InteractionComment getCommentFromPayload(JSONArray responseObject) {
		if (responseObject != null && responseObject.length() == 1) {
			JSONObject comment = responseObject.optJSONObject(0);
			if (comment != null) {
				String commentId = comment.optString("commentID");
				return getCommentFromPayload(commentId);
			}
		}
		return null;
	}

	private InteractionComment getCommentFromPayload(@NonNull String commentId) {
		return  HLPosts.getInstance().getCommentInteractionById(mPostId, commentId);
	}

	private void updateManagedComment(JSONArray responseObject, boolean delete) {
		InteractionComment comment = getCommentFromPayload(responseObject);
		if (comment != null)
			updateManagedComment(comment, delete);
	}

	private void updateManagedComment(@NonNull InteractionComment comment, boolean delete) {
		int position = commentsList.indexOf(comment);
		if (position != -1) {
			if (delete) {
				mPost.setCountComments(mPost.getCountComments() - 1);
				cntCommentsPost.setText(String.valueOf(mPost.getCountComments()));
				commentsList.remove(position);
				commentsAdapter.notifyItemRemoved(position);

				mPost.setYouLeftComments(mPost.checkYouLeftComments(mUser.getId()));

				HLPosts.getInstance().setBackupPost(mPost, realm);

				if (mPost.getCountComments() == 1)
					commentsAdapter.notifyItemChanged(0);
			}
			else {
				commentsAdapter.notifyItemChanged(position);
			}

			ivComments.setImageResource(mPost.isYouLeftComments() ? R.drawable.transition_comments_orange : R.drawable.transition_comments);
			(tdComments = ((TransitionDrawable) ivComments.getDrawable())).startTransition(0);
			tdComments.setCrossFadeEnabled(true);

//			@ColorRes int colorRes;
//			if (cUp)
//				colorRes = mPost.isYouLeftComments() ? R.color.colorAccent : R.color.white;
//			else
//				colorRes = mPost.isYouLeftComments() ? R.color.luiss_fuchsia_alpha_med : R.color.white_50;
//			cntCommentsPost.setTextColor(Utils.getColor(this, colorRes));

			handleTriangleVisibility(R.id.triangle_comments);
		}
	}

	private void updateCommentForLike(JSONArray responseObject) {
		InteractionComment ic = getCommentFromPayload(responseObject);
		if (ic != null) {
			switch (likeType) {
				case SEND:
					ic.setYouLiked(true);
					ic.setTotHearts(ic.getTotHearts() + 1);
					break;
				case REMOVE:
					ic.setYouLiked(false);
					ic.setTotHearts(ic.getTotHearts() - 1);
					break;
			}
			int position = commentsList.indexOf(ic);
			if (position != -1) {
				commentsAdapter.notifyItemChanged(position);
			}

			// handles addition/subtraction of the heart to/from the other comments from the same user.
			final int hearts = ic.getTotHearts();
			final String authorId = ic.getAuthorId();
			final String commentId = ic.getId();
			if (likeType != null && likeType != LikeActionType.NONE) {
				final int toAdd = likeType == LikeActionType.SEND ? +1 : -1;
				Stream.of(commentsList).filter(new Predicate<InteractionComment>() {
					@Override
					public boolean test(InteractionComment value) {
						return value != null && !value.getId().equals(commentId) &&
								value.getAuthorId().equals(authorId);
					}
				}).forEach(new Consumer<InteractionComment>() {
					@Override
					public void accept(InteractionComment comment) {
						if (comment.getTotHearts() != hearts) {
							comment.setTotHearts(comment.getTotHearts() + toAdd);

							int position = commentsList.indexOf(comment);
							if (position != -1) {
								commentsAdapter.notifyItemChanged(position);
							}
						}
					}
				});

				HLPosts.getInstance().updateAuthorHeartsForAllPosts(mPost);
				new Handler().post(new Runnable() {
					@Override
					public void run() {
						Realm realm = null;
						try {
							realm = RealmUtils.getCheckedRealm();
							realm.executeTransaction(new Realm.Transaction() {
								@Override
								public void execute(@NonNull final Realm realm) {
									List<Post> posts = new ArrayList<>(HLPosts.getInstance().getPosts());
									Stream.of(posts).filter(new Predicate<Post>() {
										@Override
										public boolean test(Post value) {
											return value != null && value.getAuthorId().equals(authorId);
										}
									}).forEach(new Consumer<Post>() {
										@Override
										public void accept(Post post) {
											if (post.getCountHeartsUser() != hearts) {
												post.setCountHeartsUser(post.getCountHeartsUser() + toAdd);
												HLPosts.getInstance().setBackupPostInTransaction(post, realm);
											}
										}
									});
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							RealmUtils.closeRealm(realm);
						}
					}
				});
			}

			likeType = LikeActionType.NONE;
		}
	}


	// region - Lists section -

	private void callForLists(@NonNull HLServerCalls.ListsCallType type, @NonNull String listName) {
		Object[] result = null;

		Bundle bundle = new Bundle();
		bundle.putString("listID", listName);
		try {
			switch (type) {
				case CREATE:
					result = HLServerCalls.manageLists(type, mUser.getId(), bundle);
					break;
				case ADD_POST:
					bundle.putString("postID", mPostId);
					result = HLServerCalls.manageLists(type, mUser.getId(), bundle);
					break;
				case REMOVE_POST:
					bundle.putString("postID", mPostId);
					result = HLServerCalls.manageLists(type, mUser.getId(), bundle);
					break;
			}
		} catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
			return;
		}

		HLRequestTracker.getInstance(((LBSLinkApp) getApplication())).handleCallResult(this, this, result);
	}

	private void setCreateListDialogAndShow() {
		createListDialog = DialogUtils.createGenericAlertCustomView(this, R.layout.custom_dialog_create_list);

		if (createListDialog != null) {
			View v = createListDialog.getCustomView();
			if (v != null) {
				final EditText listName = v.findViewById(R.id.create_list_edittext);
				Button pos = v.findViewById(R.id.button_positive);
				pos.setText(R.string.action_create);
				pos.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						currentListSelectionName = listName.getText().toString();
						callForLists(HLServerCalls.ListsCallType.CREATE, currentListSelectionName);
					}
				});

				Button neg = v.findViewById(R.id.button_negative);
				neg.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						createListDialog.dismiss();
					}
				});
			}

			createListDialog.show();
		}
	}

	private void addList(@NonNull final String name, boolean fromCall) {
		if (Utils.isStringValid(name) && listsContainer != null) {
			if (listsContainer.findViewById(Math.abs(name.hashCode())) != null)
				return;

			final View v = LayoutInflater.from(this).inflate(R.layout.item_list, listsContainer, false);
			if (v != null) {
				v.setId(Math.abs(name.hashCode()));
				final TextView text = v.findViewById(R.id.list_name);
				final View icon = v.findViewById(R.id.icon);
				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						currentListSelectionName = (String) view.getTag();
						currentListSelectionId = view.getId();

						if (view.isSelected())
							callForLists(HLServerCalls.ListsCallType.REMOVE_POST, currentListSelectionName);
						else
							callForLists(HLServerCalls.ListsCallType.ADD_POST, currentListSelectionName);
					}
				});
				v.setTag(name);
				text.setText(name);

				if (mPost != null && mPost.getLists() != null) {
					boolean selected = mPost.getLists().contains(name);
					v.setSelected(selected);
					text.setSelected(selected);
					icon.setSelected(selected);
				}

				listsContainer.addView(v);

				if (!mUser.getFolders().contains(name) && RealmUtils.isValid(realm)) {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							mUser.getFolders().add(name);
						}
					});
				}

				// the server doesn't work like that
//				if (fromCall)
//					addPostToList(v, name);
			}

			if (fromCall)
				Toast.makeText(this, R.string.new_list_created, Toast.LENGTH_SHORT).show();
		}
	}

	private void addPostToList(@NonNull View view, @NonNull String name) {
		if (listsContainer == null) return;

		view.findViewById(R.id.icon).setSelected(true);
		view.findViewById(R.id.list_name).setSelected(true);
		view.setSelected(true);

		mPost.getLists().add(name);
		HLPosts posts = HLPosts.getInstance();
		posts.setPost(mPost, realm, posts.isPostToBePersisted(mPostId));

		Toast.makeText(InteractionsViewerActivity.this,
				getString(R.string.post_added_to_list, name), Toast.LENGTH_SHORT).show();

		ivLists.setImageResource(mPost.hasLists() ? R.drawable.transition_pin_orange : R.drawable.transition_pin);
		(tdLists = ((TransitionDrawable) ivLists.getDrawable())).startTransition(0);
		tdLists.setCrossFadeEnabled(true);
	}

	private void addPostToList(int viewId, @NonNull String name) {
		if (listsContainer == null) return;

		View v = listsContainer.findViewById(viewId);
		if (v != null)
			addPostToList(v, name);
	}

	private void removePostFromList(@NonNull View view, @NonNull String name) {
		if (listsContainer == null || listsContainer.getChildCount() == 0) return;

		view.setSelected(false);
		view.findViewById(R.id.list_name).setSelected(false);
		view.findViewById(R.id.icon).setSelected(false);

		mPost.getLists().remove(name);
		HLPosts posts = HLPosts.getInstance();
		posts.setPost(mPost, realm, posts.isPostToBePersisted(mPostId));

		Toast.makeText(InteractionsViewerActivity.this,
				getString(R.string.post_removed_from_list, name), Toast.LENGTH_SHORT).show();

		ivLists.setImageResource(mPost.hasLists() ? R.drawable.transition_pin_orange : R.drawable.transition_pin);
		(tdLists = ((TransitionDrawable) ivLists.getDrawable())).startTransition(0);
		tdLists.setCrossFadeEnabled(true);
	}

	private void removePostFromList(int viewId, @NonNull String name) {
		if (listsContainer == null || listsContainer.getChildCount() == 0) return;

		View v = listsContainer.findViewById(viewId);
		if (v != null)
			removePostFromList(v, name);
	}

	//endregion

	private void setData(Realm realm, boolean background) {
		HLPosts posts = HLPosts.getInstance();
		newItemsCount = responseObject != null ? responseObject.length() : 0;

		switch (viewHitResId) {
			case R.id.button_hearts:
				if (LBSLinkApp.pageIdHearts == 1) {
					posts.setInteractionsHearts(responseObject, mPostId, realm, posts.isPostToBePersisted(mPostId));
					if (heartsList == null) heartsList = new ArrayList<>();
					else heartsList.clear();
					heartsList.addAll(mPost.getInteractionsHeartsPost());
					if (!background)
						heartsAdapter.notifyDataSetChanged();
				}
				else {
					if (heartsList != null) {
						heartsList.clear();
						heartsList.addAll(mPost.getInteractionsHeartsPost());
						if (!background) {
							heartsAdapter.notifyItemRangeInserted(
									(Constants.PAGINATION_AMOUNT * (LBSLinkApp.pageIdHearts - 1)), newItemsCount
							);
						}
					}
				}
				handleTriangleVisibility(R.id.triangle_hearts);
				break;

			case R.id.button_comments:
				if (LBSLinkApp.pageIdComments == 1) {
					// handles the times when after sending the single heart for comment, the system tries to update the comments list.
					boolean setComment = (newItemsCount != 1) || !responseObject.optJSONObject(0).has("like");
					if (setComment)
						posts.setInteractionsComments(responseObject, mPostId, realm, posts.isPostToBePersisted(mPostId));

					if (commentsList == null) commentsList = new ArrayList<>();
					else commentsList.clear();
					posts.sortComments(mPostId, realm);
					commentsList.addAll(mPost.getInteractionsComments());
//						commentsList.addAll(HLPosts.getInstance().getVisibleCommentsForPost(mPostId));
					if (!background)
						commentsAdapter.notifyDataSetChanged();
				}
				else {
					if (commentsList != null) {
						commentsList.clear();
						posts.sortComments(mPostId, realm);
						commentsList.addAll(mPost.getInteractionsComments());
//							commentsList.addAll(HLPosts.getInstance().getVisibleCommentsForPost(mPostId));
						if (!background) {
							commentsAdapter.notifyItemRangeInserted(
									(Constants.PAGINATION_AMOUNT * (LBSLinkApp.pageIdComments) - 1), newItemsCount
							);
						}
					}
				}
				handleTriangleVisibility(R.id.triangle_comments);
				break;

			case R.id.button_shares:
				if (LBSLinkApp.pageIdShares == 1) {
					posts.setInteractionsShares(responseObject, mPostId, realm, posts.isPostToBePersisted(mPostId));
					if (sharesList == null) sharesList = new ArrayList<>();
					else sharesList.clear();
					sharesList.addAll(mPost.getInteractionsShares());
					if (!background)
						sharesAdapter.notifyDataSetChanged();
				}
				else {
					if (sharesList != null) {
						sharesList.clear();
						sharesList.addAll(mPost.getInteractionsShares());
						if (!background) {
							sharesAdapter.notifyItemRangeInserted(
									(Constants.PAGINATION_AMOUNT * (LBSLinkApp.pageIdShares - 1)), newItemsCount
							);
						}
					}
				}
				handleTriangleVisibility(R.id.triangle_shares);
				break;

			case R.id.button_pin:
				if (listsContainer.getChildCount() > 0)
					listsContainer.removeAllViews();

				List<String> lists = mUser.getFolders();
				if (lists != null && !lists.isEmpty()) {
					for (String list : lists)
						addList(list, false);

					listsView.setVisibility(View.VISIBLE);
					interactionsWrapper.setVisibility(View.GONE);
				}

				handleTriangleVisibility(R.id.triangle_lists);
				return;
		}

		updateTextViews();
		listsView.setVisibility(View.GONE);
		interactionsWrapper.setVisibility(View.VISIBLE);
	}

	private void updateTextViews() {
		if (mPost != null) {
			if (cntHeartsPost != null) {
				cntHeartsPost.setText(String.valueOf(mPost.getCountHeartsPost()));
//				cntHeartsPost.setSelected(mPost.getHeartsLeft() != null && mPost.getHeartsLeft() > 0);
			}
			if (cntCommentsPost != null) {
				cntCommentsPost.setText(String.valueOf(mPost.getCountComments()));
//				cntCommentsPost.setSelected(mPost.isYouLeftComments());
			}
			if (cntSharesPost != null) {
				cntSharesPost.setText(String.valueOf(mPost.getCountShares()));
//				cntSharesPost.setSelected(mPost.isYouDidShares());
			}
		}
	}

	private void callInteractions(boolean first) {
		switch (viewHitResId) {
			case R.id.button_hearts:
				if (heartsAdapter == null)
					heartsAdapter = new InteractionHeartsAdapter(heartsList);
				interactionsList.setAdapter(heartsAdapter);

				if (first) {
					call(false);
				}
				else {
					if (heartsList == null) heartsList = new ArrayList<>();
					else heartsList.clear();
					heartsList.addAll(mPost.getInteractionsHeartsPost());
					heartsAdapter.notifyDataSetChanged();
				}
				break;

			case R.id.button_comments:
				if (commentsAdapter == null)
					commentsAdapter = new InteractionCommentsAdapter(commentsList, mUser, mPost);
				interactionsList.setAdapter(commentsAdapter);

				if (first) {
					call(false);
				}
				else {
					if (commentsList == null) commentsList = new ArrayList<>();
					else commentsList.clear();
					commentsList.addAll(HLPosts.getInstance().getVisibleCommentsForPost(mPostId));
					commentsAdapter.notifyDataSetChanged();
				}
				break;

			case R.id.button_shares:
				if (first) {
					call(false);
				}
				else {
					if (sharesList == null) sharesList = new ArrayList<>();
					else sharesList.clear();
					sharesList.addAll(mPost.getInteractionsShares());
					sharesAdapter.notifyDataSetChanged();
				}
				break;
		}
	}

	private void call(boolean usePageId) {
		Object[] results = null;

		InteractionPost.Type type = null;
		int pageId = 0;
		switch (viewHitResId) {
			case R.id.button_hearts:
				type = InteractionPost.Type.HEARTS;
				if (usePageId) pageId = ++LBSLinkApp.pageIdHearts;
				else pageId = -1;
				break;

			case R.id.button_comments:
				type = InteractionPost.Type.COMMENT;
				if (usePageId) pageId = ++LBSLinkApp.pageIdComments;
				else pageId = -1;
				break;

			case R.id.button_shares:
				type = InteractionPost.Type.SHARE;
				if (usePageId) pageId = ++LBSLinkApp.pageIdShares;
				else pageId = -1;
				break;
		}

		try {
			if (type != null)
				results = HLServerCalls.getInteractionsForPost(mUser, mPostId, type, pageId);
		} catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}

		if (results != null) {
			HLRequestTracker.getInstance((OnApplicationContextNeeded) this.getApplication())
					.handleCallResult(this, this, results);
		}
	}


	private void handleInteractions() {
		if (interactionsList != null && llm != null) {
			boolean first = true;
			switch (viewHitResId) {
				case R.id.button_hearts:
					first = (LBSLinkApp.pageIdHearts == 1);
//					if (mPost != null)
//						first = mPost.getInteractionsHeartsPost() == null || mPost.getInteractionsHeartsPost().isEmpty();
//					else
//						first = heartsList == null || heartsList.isEmpty();
					handleTransitionDrawables(first);
					if (!hUp) {

					    commentPlaceholder.setVisibility(View.GONE);

						if (heartsAdapter == null)
							heartsAdapter = new InteractionHeartsAdapter(heartsList);
						interactionsList.setAdapter(heartsAdapter);
						if (heartsList == null) heartsList = new ArrayList<>();
						else heartsList.clear();
						heartsList.addAll(mPost.getInteractionsHeartsPost());
						heartsAdapter.notifyDataSetChanged();

						interactionsWrapper.setVisibility(View.VISIBLE);
						listsView.setVisibility(View.GONE);
						handleTriangleVisibility(R.id.triangle_hearts);
						hUp = true;
						cUp = false;
						sUp = false;
						pUp = false;
					}
					else {

						setResult(RESULT_OK);
						finish();
						return;

//						interactionsList.setVisibility(View.GONE);
//						hUp = false;
					}
					break;
				case R.id.button_comments:
					first = (LBSLinkApp.pageIdComments == 1);
//					if (mPost != null)
//						first = mPost.getInteractionsComments() == null || mPost.getInteractionsComments().isEmpty();
//					else
//						first = commentsList == null || commentsList.isEmpty();
					handleTransitionDrawables(first);
					if (!cUp) {

                        commentPlaceholder.setVisibility(View.VISIBLE);

                        if (commentsAdapter == null)
							commentsAdapter = new InteractionCommentsAdapter(commentsList, mUser, mPost);
						interactionsList.setAdapter(commentsAdapter);
						if (commentsList == null) commentsList = new ArrayList<>();
						else commentsList.clear();
						commentsList.addAll(HLPosts.getInstance().getVisibleCommentsForPost(mPostId));
						commentsAdapter.notifyDataSetChanged();

						interactionsWrapper.setVisibility(View.VISIBLE);
						listsView.setVisibility(View.GONE);
						handleTriangleVisibility(R.id.triangle_comments);
						hUp = false;
						cUp = true;
						sUp = false;
						pUp = false;
					}
					else {

						setResult(RESULT_OK);
						finish();
						return;

//						interactionsList.setVisibility(View.GONE);
//						cUp = false;
					}
					break;
				case R.id.button_shares:
					first = (LBSLinkApp.pageIdShares == 1);
//					if (mPost != null)
//						first = mPost.getInteractionsShares() == null || mPost.getInteractionsShares().isEmpty();
//					else
//						first = sharesList == null || sharesList.isEmpty();
					handleTransitionDrawables(first);
					if (!sUp) {

                        commentPlaceholder.setVisibility(View.GONE);

                        if (sharesAdapter == null)
							sharesAdapter = new InteractionSharesAdapter(sharesList);
						interactionsList.setAdapter(sharesAdapter);
						if (sharesList == null) sharesList = new ArrayList<>();
						else sharesList.clear();
						sharesList.addAll(mPost.getInteractionsShares());
						sharesAdapter.notifyDataSetChanged();

						interactionsWrapper.setVisibility(View.VISIBLE);
						listsView.setVisibility(View.GONE);
						handleTriangleVisibility(R.id.triangle_shares);
						hUp = false;
						cUp = false;
						sUp = true;
						pUp = false;
					}
					else {

						setResult(RESULT_OK);
						finish();
						return;

//						interactionsList.setVisibility(View.GONE);
//						sUp = false;
					}
					break;

				case R.id.button_pin:
					handleTransitionDrawables(first);
					if (!pUp) {
						hUp = false;
						cUp = false;
						sUp = false;
						pUp = true;

						if (mUser.getFolders() != null && !mUser.getFolders().isEmpty()) {
							if (listsContainer != null)
								listsContainer.removeAllViews();
							for (String list : mUser.getFolders()) {
								if (Utils.isStringValid(list))
									addList(list, false);
							}
						}


						interactionsWrapper.setVisibility(View.GONE);
						listsView.setVisibility(View.VISIBLE);
						handleTriangleVisibility(R.id.triangle_lists);
					}
					else {
						setResult(RESULT_OK);
						finish();
						return;
					}
					break;
			}

			callInteractions(first);
		}
	}

	// ----------------- method to control the visibility of the little triangles.---------------
	private void handleTriangleVisibility(int viewId){
		if (interactionsList != null && llm != null) {
			switch (viewId) {
				case R.id.triangle_hearts:
					if (heartsList != null && !heartsList.isEmpty()){
						triangle_hearts.setVisibility(View.VISIBLE);
						triangle_comments.setVisibility(View.GONE);
						triangle_shares.setVisibility(View.GONE);
						triangle_lists.setVisibility(View.GONE);
					}
					else {
						triangle_hearts.setVisibility(View.GONE);
						triangle_comments.setVisibility(View.GONE);
						triangle_shares.setVisibility(View.GONE);
						triangle_lists.setVisibility(View.GONE);
					}
					break;

				case R.id.triangle_comments:
					if (commentsList != null && !commentsList.isEmpty() ) {
						triangle_comments.setVisibility(View.VISIBLE);
						triangle_shares.setVisibility(View.GONE);
						triangle_hearts.setVisibility(View.GONE);
						triangle_lists.setVisibility(View.GONE);
					}
					else {
						triangle_comments.setVisibility(View.GONE);
						triangle_shares.setVisibility(View.GONE);
						triangle_hearts.setVisibility(View.GONE);
						triangle_lists.setVisibility(View.GONE);
					}
					break;

				case R.id.triangle_shares:
					if (sharesList != null && !sharesList.isEmpty()) {
						triangle_shares.setVisibility(View.VISIBLE);
						triangle_hearts.setVisibility(View.GONE);
						triangle_comments.setVisibility(View.GONE);
						triangle_lists.setVisibility(View.GONE);
					}
					else {
						triangle_shares.setVisibility(View.GONE);
						triangle_hearts.setVisibility(View.GONE);
						triangle_comments.setVisibility(View.GONE);
						triangle_lists.setVisibility(View.GONE);
					}
					break;

				case R.id.triangle_lists:
					triangle_lists.setVisibility(View.VISIBLE);
					triangle_shares.setVisibility(View.GONE);
					triangle_hearts.setVisibility(View.GONE);
					triangle_comments.setVisibility(View.GONE);
					break;

				default:
					triangle_hearts.setVisibility(View.GONE);
					triangle_shares.setVisibility(View.GONE);
					triangle_comments.setVisibility(View.GONE);
					triangle_lists.setVisibility(View.GONE);
					break;
			}
		}
		else {
			triangle_hearts.setVisibility(View.GONE);
			triangle_shares.setVisibility(View.GONE);
			triangle_comments.setVisibility(View.GONE);
			triangle_lists.setVisibility(View.GONE);
		}
	}

	private void handleTransitionDrawables(boolean first) {
		@ColorRes int colorInactive = R.color.white_50;
		@ColorRes int colorInactiveOrange = R.color.luiss_blue_alpha_med;
		boolean conditionHearts = mPost.getHeartsLeft() != null && mPost.getHeartsLeft() > 0;
		boolean conditionComments = mPost.isYouLeftComments();
		boolean conditionShares = mPost.isYouDidShares();

		switch (viewHitResId) {
			case R.id.button_hearts:
				if (cUp)
					tdComments.reverseTransition(100);
				else if (sUp)
					tdShares.reverseTransition(100);
				else if (pUp)
					tdLists.reverseTransition(100);

				tdHearts.startTransition(first ? 0 : 200);
//				cntHeartsPost.setTextColor(Utils.getColor(this, conditionHearts ? R.color.colorAccent : R.color.white));
//				cntCommentsPost.setTextColor(Utils.getColor(this, conditionComments ? colorInactiveOrange : colorInactive));
//				cntSharesPost.setTextColor(Utils.getColor(this, conditionShares ? colorInactiveOrange : colorInactive));
				break;
			case R.id.button_comments:
				if (hUp)
					tdHearts.reverseTransition(100);
				else if (sUp)
					tdShares.reverseTransition(100);
				else if (pUp)
					tdLists.reverseTransition(100);

				tdComments.startTransition(first ? 0 : 200);
//				cntCommentsPost.setTextColor(Utils.getColor(this, conditionComments ? R.color.colorAccent : R.color.white));
//				cntHeartsPost.setTextColor(Utils.getColor(this, conditionHearts ? colorInactiveOrange : colorInactive));
//				cntSharesPost.setTextColor(Utils.getColor(this, conditionShares ? colorInactiveOrange : colorInactive));
				break;
			case R.id.button_shares:
				if (hUp)
					tdHearts.reverseTransition(100);
				else if (cUp)
					tdComments.reverseTransition(100);
				else if (pUp)
					tdLists.reverseTransition(100);

				tdShares.startTransition(first ? 0 : 200);
//				cntSharesPost.setTextColor(Utils.getColor(this, conditionShares ? R.color.colorAccent : R.color.white));
//				cntCommentsPost.setTextColor(Utils.getColor(this, conditionComments ? colorInactiveOrange : colorInactive));
//				cntHeartsPost.setTextColor(Utils.getColor(this, conditionHearts ? colorInactiveOrange : colorInactive));
				break;

			case R.id.button_pin:
				if (cUp)
					tdComments.reverseTransition(100);
				else if (sUp)
					tdShares.reverseTransition(100);
				else if (hUp)
					tdHearts.reverseTransition(100);

				tdLists.startTransition(first ? 0 : 200);
//				cntHeartsPost.setTextColor(Utils.getColor(this, conditionHearts ? colorInactiveOrange : colorInactive));
//				cntCommentsPost.setTextColor(Utils.getColor(this, conditionComments ? colorInactiveOrange : colorInactive));
//				cntSharesPost.setTextColor(Utils.getColor(this, conditionShares ? colorInactiveOrange : colorInactive));
				break;
		}
	}

	private InteractionComment currentComment;
	@Override
	public void openOptionsSheet(@NonNull InteractionComment comment) {
		currentComment = comment;

		itemReply.setVisibility(!mPost.canCommentPost() ? View.GONE : View.VISIBLE);
		itemEdit.setVisibility(comment.isActingUserCommentAuthor(mUser) ? View.VISIBLE : View.GONE);
		itemDelete.setVisibility(
				comment.isActingUserCommentAuthor(mUser) || mPost.isActiveUserAuthor(realm) ?
						View.VISIBLE : View.GONE
		);

		Message message = new Message();
		message.what = SlidingPanelHandler.ACTION_EXPAND;
		handlerOwn.sendMessage(message);
	}

	@Override
	public boolean isPanelOpen() {
		return slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED;
	}

	@Override
	public void closePanel() {
		if (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
			slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
	}


	@Override
	public void goToUserProfile(@NonNull String authorId) {

		/*
		if (Utils.isStringValid(authorId)) {
			if (authorId.equals(mUser.getId())) {
				Intent intent = new Intent(this, HomeActivity.class);
				intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
				startActivity(intent);
				finish();
			}
			else {

				// FIXME: 2/5/2018    MISSING isInterest in interactions?

//				ProfileHelper.ProfileType type = isInterest ? ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND;
//				ProfileActivity.openProfileCardFragment(this, type, userId);
			}
		}
		*/

	}

	private enum LikeActionType { NONE, SEND, REMOVE }
	private LikeActionType likeType = LikeActionType.NONE;
	@Override
	public void sendLikeToComment(@NonNull InteractionComment comment) {
		boolean removeLike = comment.isYouLiked();
		likeType = removeLike ? LikeActionType.REMOVE : LikeActionType.SEND;

		Toast.makeText(this, removeLike ? R.string.removing_heart : R.string.sending_heart, Toast.LENGTH_LONG).show();

		Object[] result = null;
		try {
			result = HLServerCalls.likeComment(comment, mUser, mPostId, removeLike);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (result != null) {
			HLRequestTracker.getInstance(((OnApplicationContextNeeded) getApplication()))
					.handleCallResult(this, this, result);
		}
	}

	@Override
	public void replyToComment(@NonNull InteractionComment comment) {
		Intent intent = new Intent(this, PostOverlayActionActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, mPostId);
		intent.putExtra(Constants.EXTRA_PARAM_2, Constants.RESULT_REPLY_TO_COMMENT);
		intent.putExtra(Constants.EXTRA_PARAM_3, comment.getId());
		startActivityForResult(intent, Constants.RESULT_REPLY_TO_COMMENT);
		overridePendingTransition(R.anim.slide_in_bottom, R.anim.no_animation);

		findViewById(R.id.root_content).setVisibility(View.INVISIBLE);
		closePanel();
	}

	@Override
	public void reportComment(@NonNull InteractionComment comment) {
		currentComment = comment;
		// TODO: 11/6/2017    TO BE POSTPONED
//		Toast.makeText(this, "Let's do some moderation", Toast.LENGTH_SHORT).show();
	}

	@Override
	public HLUser getUser() {
		return mUser;
	}

	private void goToComment(boolean isEdit) {
		Intent intent = new Intent(this, PostOverlayActionActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, mPostId);

		if (isEdit) {
			intent.putExtra(Constants.EXTRA_PARAM_2, Constants.RESULT_EDIT_COMMENT);
			intent.putExtra(Constants.EXTRA_PARAM_3, currentComment.getId());
            if (currentComment.isSubComment())
                intent.putExtra(Constants.EXTRA_PARAM_4, currentComment.getParentCommentID());
			startActivityForResult(intent, Constants.RESULT_EDIT_COMMENT);
		}
		else {
			intent.putExtra(Constants.EXTRA_PARAM_2, Constants.RESULT_ADD_COMMENT);
			startActivityForResult(intent, Constants.RESULT_ADD_COMMENT);
		}
		overridePendingTransition(R.anim.slide_in_bottom, R.anim.no_animation);

		findViewById(R.id.root_content).setVisibility(View.INVISIBLE);
		closePanel();
	}

	private void deleteComment() {
		if (currentComment != null) {
			Object[] result = null;
			try {
				currentComment.setVisible(false);
				result = HLServerCalls.manageComment(currentComment, mUser, mPostId);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			HLRequestTracker.getInstance(((OnApplicationContextNeeded) getApplication()))
					.handleCallResult(this, this, result);
		}
	}

	private void configureCommentOptionsSheets() {
		slidingLayout = findViewById(R.id.sliding_comment);
		if (slidingLayout != null) {
			slidingLayout.setTouchEnabled(true);

			View innerLayout = slidingLayout.findViewById(R.id.comment_bottom_sheet);
			itemReply = innerLayout.findViewById(R.id.item_reply);
			itemReply.setOnClickListener(this);
			itemEdit = innerLayout.findViewById(R.id.item_edit);
			itemEdit.setOnClickListener(this);
			itemDelete = innerLayout.findViewById(R.id.item_delete);
			itemDelete.setOnClickListener(this);

			handlerOwn = new SlidingPanelHandler(slidingLayout);
		}
	}

	//endregion


	//region == Real-time communication interface methods ==

	@Override
	public void onPostAdded(@Nullable Post post, int position) {}

	@Override
	public void onPostUpdated(@NonNull String postId, int position) {
		if (Utils.areStringsValid(postId, mPostId) && postId.equals(mPostId)) {
			updateTextViews();

			if (cUp) {
				int prevSize = commentsList != null ? commentsList.size() : 0;
				if (commentsList == null) commentsList = new ArrayList<>();
				else commentsList.clear();
				commentsList.addAll(HLPosts.getInstance().getVisibleCommentsForPost(mPostId));

				if (position == -1)
					commentsAdapter.notifyDataSetChanged();
				else if (position >= 0) {
					if (prevSize > commentsList.size())
						commentsAdapter.notifyItemRemoved(position);
					else
						commentsAdapter.notifyItemChanged(position);
				}

				handleTriangleVisibility(R.id.triangle_comments);
			}
		}
	}

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