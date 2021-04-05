/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.createPost;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji.widget.EmojiAppCompatEditText;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;
import io.realm.RealmList;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.tags.TagAdapter;
import it.keybiz.lbsapp.corporate.features.tags.ViewAllTagsActivity;
import it.keybiz.lbsapp.corporate.features.wishes.WishCreatePostFragment;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.HLWebLink;
import it.keybiz.lbsapp.corporate.models.Initiative;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.MemoryMediaObject;
import it.keybiz.lbsapp.corporate.models.MemoryMessageObject;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.PostPrivacy;
import it.keybiz.lbsapp.corporate.models.PostVisibility;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.services.DeleteTempFileService;
import it.keybiz.lbsapp.corporate.services.GetConfigurationDataService;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.WebLinkRecognizer;
import it.keybiz.lbsapp.corporate.utilities.listeners.FlingGestureListener;
import it.keybiz.lbsapp.corporate.utilities.media.CustomGalleryAdapter;
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;
import it.keybiz.lbsapp.corporate.utilities.media.MediaUploadManager;
import it.keybiz.lbsapp.corporate.widgets.HLViewPagerNoScroll;
import it.keybiz.lbsapp.corporate.widgets.PlayerViewNoController;

/**
 * @author mbaldrighi on 3/16/2018.
 */
public class CreatePostHelper implements View.OnClickListener,
		OnServerMessageReceivedListener, ViewPager.OnPageChangeListener,
		FlingGestureListener.OnFlingCompletedListener,
		GalleryFragment.OnGalleryFragmentInteractionListener,
		AudioRecordFragment.OnAudioRecordFragmentInteractionListener,
		TagFragment.OnTagFragmentInteractionListener,
		InitiativesFragment.OnInitiativesFragmentInteractionListener,
		OnMissingConnectionListener, CustomGalleryAdapter.OnMediaClickListener,
		BasicAdapterInteractionsListener, TagAdapter.OnItemClickListener,
		WebLinkRecognizer.LinkRecognitionListener {

	public static final String LOG_TAG = CreatePostHelper.class.getCanonicalName();

	private static int ACTIONS_BAR_HEIGHT;
	private static int MEDIA_PANEL_HEIGHT;
	private static int TAG_SEARCH_PANEL_HEIGHT;


	private boolean redirectToHome;

	public enum ViewType implements Serializable { NORMAL, WISH }
	private ViewType viewType;

	private static final int ITEM_CAMERA = -1;
	private static final int PAGER_ITEM_GALLERY = 0;
	private static final int PAGER_ITEM_AUDIO = 1;
	private static final int PAGER_ITEM_TAG = 2;
	private static final int PAGER_ITEM_INITIATIVES = 3;

	private static int PAGER_COUNT;

	private int currentPagerItem = PAGER_ITEM_GALLERY;

	private HLViewPagerNoScroll viewPager;

	private View mediaActionsContainer;

	private ImageView cameraView;
	private ImageView galleryView;
	private ImageView audioRecView;
	private ImageView tagView;
	private ImageView initiativesView;

	private TransitionDrawable cameraTD;
	private TransitionDrawable galleryTD;
	private TransitionDrawable audioRecTD;
	private TransitionDrawable tagTD;
	private TransitionDrawable initiativesTD;

	private ViewGroup mediaBackgroundContainer;
	private ImageView profilePicture;
	private View btnSend, btnNext;
	private View profilePictureLayout, closeButton;
	private EmojiAppCompatEditText postEditField;

	private ImageView privacyIcon;
	private TextView privacyText;

	private View webLinkProgress, webLinkBoxLayout;
	private ImageView webLinkImage, webLinkPlaceholder;
	private TextView webLinkTitle, webLinkSource;
	private boolean ignoreLink;

	private MaterialDialog emptyPostErrorDialog;

	private GestureDetector gdt;

	private Post mPost;

	private boolean openPopup = false;

	private String mPostToEditId;

	private HLMediaType mediaCaptureType;
	private String mediaFileUri;
	private boolean newBackgroundSet;

	private MediaPlayer mediaPlayer;

	private boolean mediaPanelOpen;


	private HLActivity activity;
	private Fragment fragment;

	private OnPostSavedListener mPostSavedListener;


	private WeakReference<TagFragment> tagFragment;

	private RecyclerView tagSearchRecView;
	private TagAdapter tagSearchAdapter;
	private LinearLayoutManager tagSearchLlm;
	private List<Tag> searchList = new ArrayList<>();
	private List<Object> searchListToShow = new ArrayList<>();
	private RealmList<Tag> selectedTags = new RealmList<>();

	private View hiddenTagLayout;
	private TextView hiddenTagsAll;
	private ImageView hiddenTagsIcon, hiddenTagMore;
	private CircleImageView hiddenTag1, hiddenTag2, hiddenTag3, hiddenTag4;
	private View tagSearchContainer;
	private View tagSearchNoResult;
	private boolean tagVisible = false;

	private boolean isUploadingOrSending = false;

	private WeakReference<InitiativesFragment> initiativeFragment;
	private View initiativeLabelLayout;
	private TextView initiativeLabelText;
	private ImageView initiativeLabelIcon;


	private ImageView backgroundForImageResult = null;

	private File fileToLoad = null;

	private PlayerViewNoController exoPlayerView = null;
	private Long playerPosition = 0L;



	/* CONSTRUCTORS */
	public CreatePostHelper(HLActivity activity, Fragment fragment, ViewType viewType, @Nullable String postToEditId) {
		this.activity = activity;
		this.fragment = fragment;
		this.viewType = viewType;
		this.mPostToEditId = postToEditId;

		boolean wantsInitiative = true;
		if (Utils.isStringValid(mPostToEditId)) {
			mPost = HLPosts.getInstance().getPost(mPostToEditId);
			wantsInitiative = mPost.isCHInitiative() || mPost.isGSInitiative();
		}
		// INFO: 2/12/19    LUISS doesn't want INITIATIVEs
		wantsInitiative = false;
		PAGER_COUNT = viewType == ViewType.NORMAL && wantsInitiative ? 4 : 3;

		if (this.fragment instanceof OnPostSavedListener)
			mPostSavedListener = ((OnPostSavedListener) this.fragment);
	}

	public CreatePostHelper(Fragment fragment, ViewType viewType, @Nullable String postToEditId) {
		new CreatePostHelper(null, fragment, viewType, postToEditId);
	}


	//region == Lifecycle methods ==

	public void onCreate(View view) {
		ACTIONS_BAR_HEIGHT = activity.getResources().getDimensionPixelSize(R.dimen.create_post_actions_bar_height);
		MEDIA_PANEL_HEIGHT = activity.getResources().getDimensionPixelSize(R.dimen.create_post_media_panel_height);
		TAG_SEARCH_PANEL_HEIGHT = activity.getResources().getDimensionPixelSize(R.dimen.cpost_tag_search_rv_height);

		activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		gdt = new GestureDetector(activity, new FlingGestureListener(this));

		tagSearchAdapter = new TagAdapter(searchListToShow, this);
		tagSearchAdapter.setHasStableIds(true);
		tagSearchLlm = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);

		configureUpperSection(view);
		configureLowerSection(view);

		emptyPostErrorDialog = createEmptyErrorDialog();

		initializePost();
	}

	public void onCreateView(View view) {
		configureUpperSection(view);
		configureLowerSection(view);
	}

	public void onActivityCreated(HLActivity activity) {
		this.activity = activity;
		this.activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		ACTIONS_BAR_HEIGHT = activity.getResources().getDimensionPixelSize(R.dimen.create_post_actions_bar_height);
		MEDIA_PANEL_HEIGHT = activity.getResources().getDimensionPixelSize(R.dimen.create_post_media_panel_height);
		TAG_SEARCH_PANEL_HEIGHT = activity.getResources().getDimensionPixelSize(R.dimen.cpost_tag_search_rv_height);

		if (gdt == null)
			gdt = new GestureDetector(activity, new FlingGestureListener(this));

		tagSearchAdapter = new TagAdapter(searchListToShow, this);
		tagSearchAdapter.setHasStableIds(true);
		tagSearchLlm = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);

		if (emptyPostErrorDialog == null)
			emptyPostErrorDialog = createEmptyErrorDialog();

		initializePost();
	}

	public void onStart() {}

	public void onResume() {
		// data needed for initiatives
		GetConfigurationDataService.startService(activity);

		setData();
	}

	public void onPause() {

		if (mediaCaptureType == HLMediaType.PHOTO && mediaPanelOpen)
			animateMediaPanel(false);

		if (exoPlayerView != null)
			exoPlayerView.pause();

		try {
			if (mediaPlayer != null && mediaPlayer.isPlaying())
				mediaPlayer.pause();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	public void onStop() {
		if (exoPlayerView != null)
			exoPlayerView.stopPlayback();

		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	public void onDestroy() {
		if (Utils.isContextValid(activity))
			activity.closeProgress();

		DeleteTempFileService.startService(activity);
	}


	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.RESULT_CREATE_POST_VISIBILITY) {

			String msg;
			if (resultCode == Activity.RESULT_OK && data != null) {
				if (data.hasExtra(Constants.EXTRA_PARAM_1))
					mPost.setVisibilityChanged(data.getBooleanExtra(Constants.EXTRA_PARAM_1, false));
				if (data.hasExtra(Constants.EXTRA_PARAM_2)) {
					setPostVisibility(data, -1);
					setData();
				}

				msg = "Back from VISIBILITY: OK";
			}
			else
				msg = "Back from VISIBILITY: CANCELED";

			LogUtils.d(LOG_TAG, msg);
		}
		else if (requestCode == Constants.RESULT_CREATE_POST_PREVIEW && resultCode == Activity.RESULT_OK) {
			activity.setResult(Activity.RESULT_OK);

			if (Utils.hasLollipop())
				activity.finishAfterTransition();
			else {
				activity.finish();
				activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			}
		}
		else {
			if (resultCode == Activity.RESULT_OK) {
				LBSLinkApp.fromMediaTaking = true;

				activity.setProgressMessage(R.string.processing_file);
				activity.openProgress();

				File f = null;
				if (requestCode == Constants.RESULT_PHOTO ||
						requestCode == Constants.RESULT_VIDEO) {

					cameraView.setSelected(false);
					currentPagerItem = -2;

					Uri u = Uri.parse(mediaFileUri);
					f = new File(u.getPath());
					if (f.exists()) {
						switch (requestCode) {
							case Constants.RESULT_PHOTO:
								mPost.setType(PostTypeEnum.PHOTO.toString());
								setNewMedia(HLMediaType.PHOTO);
								LogUtils.d(LOG_TAG, "Media captured with type=PHOTO and path: " + f.getAbsolutePath()
										+ " and file exists=" + f.exists());
								break;
							case Constants.RESULT_VIDEO:
								mPost.setType(PostTypeEnum.VIDEO.toString());
								setNewMedia(HLMediaType.VIDEO);
								LogUtils.d(LOG_TAG, "Media captured with type=VIDEO and path: " + f.getAbsolutePath()
										+ " and file exists=" + f.exists());
								break;
						}

						handleNextBtn(true);
					}
				} else if (requestCode == Constants.RESULT_GALLERY) {
					if (data != null && data.getData() != null) {
						Uri selectedFile = data.getData();
						String[] filePathColumn = new String[1];
						if (mediaCaptureType == HLMediaType.PHOTO ||
								mediaCaptureType == HLMediaType.PHOTO_PROFILE ||
								mediaCaptureType == HLMediaType.PHOTO_WALL) {

							filePathColumn[0] = MediaStore.Images.Media.DATA;
							mPost.setType(PostTypeEnum.PHOTO.toString());
							setNewMedia(HLMediaType.PHOTO);
						} else if (mediaCaptureType == HLMediaType.VIDEO) {
							filePathColumn[0] = MediaStore.Video.Media.DATA;
							mPost.setType(PostTypeEnum.VIDEO.toString());
							setNewMedia(HLMediaType.VIDEO);
						}

						Cursor cursor = activity.getContentResolver().query(selectedFile,
								filePathColumn, null, null, null);
						if (cursor != null) {
							cursor.moveToFirst();

							int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
							mediaFileUri = cursor.getString(columnIndex);
							cursor.close();

							f = new File(Uri.parse(mediaFileUri).getPath());
						}
					}
				}

				handleMediaResult(f);
			}
		}
	}

	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA:
				if (grantResults.length > 0) {
					if (grantResults.length == 3 &&
							grantResults[0] == PackageManager.PERMISSION_GRANTED &&
							grantResults[1] == PackageManager.PERMISSION_GRANTED &&
							grantResults[2] == PackageManager.PERMISSION_GRANTED) {
						mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, mediaCaptureType, fragment);
					}
					else if (grantResults.length == 2 &&
							grantResults[0] == PackageManager.PERMISSION_GRANTED &&
							grantResults[1] == PackageManager.PERMISSION_GRANTED) {
						mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, mediaCaptureType, fragment);
					}
					else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
						mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, mediaCaptureType, fragment);
				}
				break;

			case Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO:
				if (grantResults.length > 0) {
					if (grantResults.length == 3 &&
							grantResults[0] == PackageManager.PERMISSION_GRANTED &&
							grantResults[1] == PackageManager.PERMISSION_GRANTED &&
							grantResults[2] == PackageManager.PERMISSION_GRANTED) {
						mediaFileUri = MediaHelper.checkPermissionForAudio(activity, HLMediaType.AUDIO, fragment);
					}
					else if (grantResults.length == 2 &&
							grantResults[0] == PackageManager.PERMISSION_GRANTED &&
							grantResults[1] == PackageManager.PERMISSION_GRANTED) {
						mediaFileUri = MediaHelper.checkPermissionForAudio(activity, HLMediaType.AUDIO, fragment);
					}
					else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
						mediaFileUri = MediaHelper.checkPermissionForAudio(activity, HLMediaType.AUDIO, fragment);
				}
				break;

			case Constants.PERMISSIONS_REQUEST_GALLERY:
				if (grantResults.length > 0) {
					if (grantResults.length == 2 &&
							grantResults[0] == PackageManager.PERMISSION_GRANTED &&
							grantResults[1] == PackageManager.PERMISSION_GRANTED) {
						MediaHelper.checkPermissionForGallery(activity, mediaCaptureType, fragment);
					} else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
						MediaHelper.checkPermissionForGallery(activity, mediaCaptureType, fragment);
				}
				break;
		}
	}

	public void onBackPressed() {
		if (!isUploadingOrSending) {
			if (tagSearchRecView.getVisibility() == View.VISIBLE) {
				animateTagSearch(false);
			} else if (mediaPanelOpen) {
				animateMediaPanel(false);
				resetAllTransitionDrawables();

				if (fragment instanceof WishCreatePostFragment)
					((WishCreatePostFragment) fragment).removeBackListener();
			} else if (fragment == null)
				closeNoPost();
		}
		else {
			final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(activity,
					R.layout.custom_dialog_cpost_leave_while_uploading);
			if (dialog != null) {
				View view = dialog.getCustomView();
				if (view != null) {
					Button positive = view.findViewById(R.id.button_positive);
					positive.setText(R.string.cancel);
					positive.setOnClickListener(v -> {
						closeNoPost();
						DialogUtils.closeDialog(dialog);
					});

					Button negative = view.findViewById(R.id.button_negative);
					negative.setText(R.string.action_stay);
					negative.setOnClickListener(v -> DialogUtils.closeDialog(dialog));
				}
			}
		}
	}

	//endregion


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		activity.setShowProgressAnyway(false);
		activity.closeProgress();

		if (responseObject == null || responseObject.length() == 0)
			return;

		if (operationId == Constants.SERVER_OP_CREATE_POST_V2 ||
				operationId == Constants.SERVER_OP_EDIT_POST ||
				operationId == Constants.SERVER_OP_WISH_CREATE_POST ||
				operationId == Constants.SERVER_OP_WISH_EDIT_POST) {

			String id = responseObject.optJSONObject(0).optString("_id");
			int totHearts = responseObject.optJSONObject(0).optInt("heartsOfUserOfThePost");
			String text = responseObject.optJSONObject(0).optString("text");
			if (Utils.isStringValid(id)) {
				Intent result = new Intent();
				switch (operationId) {
					case Constants.SERVER_OP_CREATE_POST_V2:
					case Constants.SERVER_OP_EDIT_POST:
						if (redirectToHome) {
							result.setClass(activity, HomeActivity.class);
							result.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_TIMELINE);
							activity.startActivity(result);
						} else {
							mPost.setId(id);
							mPost.setCountHeartsUser(totHearts > 0 ? totHearts : mPost.getCountHeartsUser());
							mPost.setSortId(mPost.getCreationDate().getTime() * (-1) / 1000);
							mPost.setPrivacy(new PostPrivacy(true, true));

							if (Utils.isStringValid(text))
								mPost.setGSMessage(text);

							HLPosts posts = HLPosts.getInstance();
							if (mPost.hasInitiative() && mPost.isTHInitiative()) {
								posts.updateAuthorHeartsForAllPosts(
										mPost.getInitiative().getRecipient(),
										-1,
										(int) mPost.getInitiative().getHeartsToTransfer()
								);
							}
							posts.setPost(mPost, getRealm(), true);

							if (mPost.isGSInitiative()) {
								getRealm().executeTransaction(realm -> getUser().setHasActiveGiveSupportInitiative(true));
							}
							activity.setResult(Activity.RESULT_OK);
						}
						break;

					case Constants.SERVER_OP_WISH_CREATE_POST:
						mPost = null;

						mPostSavedListener.onPostSaved(id);
						return;
				}

				if (Utils.hasLollipop() && viewType == ViewType.NORMAL)
					activity.finishAfterTransition();
				else {
					activity.finish();
					activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
				}
			}
		}
		else if (operationId == Constants.SERVER_OP_GET_PARSED_WEB_LINK) {

			if (responseObject.length() != 1 || responseObject.optJSONObject(0).length() == 0) {
				handleErrorResponse(operationId, 0);
				return;
			}

			ignoreLink = true;

			mPost.setWebLink(new HLWebLink().deserializeToClass(responseObject.optJSONObject(0)));
			setWebLinkLayout();
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		activity.setShowProgressAnyway(false);
		activity.closeProgress();
		webLinkProgress.setVisibility(View.GONE);

		if (operationId == Constants.SERVER_OP_GET_PARSED_WEB_LINK) {
			mPost.setType(PostTypeEnum.TEXT.toString());
			setNewMedia(null);
			ignoreLink = true;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		activity.setShowProgressAnyway(false);
		activity.closeProgress();

		if (operationId == Constants.SERVER_OP_GET_PARSED_WEB_LINK)
			webLinkProgress.setVisibility(View.GONE);
	}

	public interface OnPostSavedListener {
		void onPostSaved(String postId);
	}


	@Override
	public void onClick(View v) {
		int id = v.getId();
		int item = viewPager.getCurrentItem();
		switch (id) {

			//upper section
			case R.id.profile_picture:
//				Toast.makeText(activity, "Go to identity selection", Toast.LENGTH_SHORT).show();
				break;
			case R.id.btn_close:
				closeNoPost();
				break;

			case R.id.btn_send:
				attemptSendingPost();
				break;

			case R.id.btn_next:
				goNext();
				break;

			// post toolbar
			case R.id.btn_camera:
				switch (currentPagerItem) {
					case PAGER_ITEM_GALLERY:
						if (galleryView.isSelected())
							galleryTD.reverseTransition(100);
						break;
					case PAGER_ITEM_AUDIO:
						if (audioRecView.isSelected())
							audioRecTD.reverseTransition(100);
						break;
					case PAGER_ITEM_TAG:
						if (tagView.isSelected())
							tagTD.reverseTransition(100);
						break;
					case PAGER_ITEM_INITIATIVES:
						if (initiativesView.isSelected())
							initiativesTD.reverseTransition(100);
						break;
				}
				cameraView.setSelected(true);
				cameraTD.startTransition(200);

				galleryView.setSelected(false);
				audioRecView.setSelected(false);
				tagView.setSelected(false);
				initiativesView.setSelected(false);

				if (mediaPanelOpen) {
					animateMediaPanel();
					openPopup = true;
				}
				else handlePopupMenu();
				break;

			case R.id.btn_gallery:
				if (item == PAGER_ITEM_GALLERY) {
					animateMediaPanel();

					if (galleryView.isSelected()) {
						galleryView.setSelected(false);
						galleryTD.reverseTransition(100);
					} else {
						galleryView.setSelected(true);
						galleryTD.startTransition(200);
					}
				}
				else viewPager.setCurrentItem(PAGER_ITEM_GALLERY);
				break;

			case R.id.btn_audio:
				if (item == PAGER_ITEM_AUDIO) {
					animateMediaPanel();

					if (audioRecView.isSelected()) {
						audioRecView.setSelected(false);
						audioRecTD.reverseTransition(100);
					} else {
						audioRecView.setSelected(true);
						audioRecTD.startTransition(200);
					}
				}
				else viewPager.setCurrentItem(PAGER_ITEM_AUDIO);

				mediaFileUri = MediaHelper.checkPermissionForAudio(activity, HLMediaType.AUDIO, fragment);
				break;

			case R.id.btn_tag:
				if (item == PAGER_ITEM_TAG) {
					animateMediaPanel();

					if (tagView.isSelected()) {
						tagView.setSelected(false);
						tagTD.reverseTransition(100);
					}
					else {
						tagView.setSelected(true);
						tagTD.startTransition(200);
					}
				}
				else viewPager.setCurrentItem(PAGER_ITEM_TAG);
				break;

			case R.id.btn_initiative:
				if (item == PAGER_ITEM_INITIATIVES) {
					animateMediaPanel();

					if (initiativesView.isSelected()) {
						initiativesView.setSelected(false);
						initiativesTD.reverseTransition(100);
					}
					else {
						initiativesView.setSelected(true);
						initiativesTD.startTransition(200);
					}
				}
				else viewPager.setCurrentItem(PAGER_ITEM_INITIATIVES);
				break;

			// wish toolbar
			case R.id.back_arrow:
				closeNoPost();
				break;

			case R.id.weblink_delete:
				deleteWebLink();
				break;

			// tags
			case R.id.all_tags:
				ArrayList<Tag> temp = new ArrayList<>(selectedTags);

				Intent intent = new Intent(activity, ViewAllTagsActivity.class);
				intent.putParcelableArrayListExtra(Constants.EXTRA_PARAM_1, temp);

				if (fragment != null)
					fragment.startActivity(intent);
				else
					activity.startActivity(intent);
				break;

			// privacy
			case R.id.privacy_container:
				if (!mPost.hasInitiative()) {
					Intent intent1 = new Intent(activity, PostVisibilityActivity.class);
					// can pass only post ID because PostVisibility's RealmList doesn't implement Serializable
					if (Utils.isStringValid(mPostToEditId))
						intent1.putExtra(Constants.EXTRA_PARAM_1, mPostToEditId);

					if (fragment != null)
						fragment.startActivityForResult(intent1, Constants.RESULT_CREATE_POST_VISIBILITY);
					else
						activity.startActivityForResult(intent1, Constants.RESULT_CREATE_POST_VISIBILITY);

					activity.overridePendingTransition(R.anim.slide_in_right, R.anim.no_animation);
				}
				break;
		}

		Utils.closeKeyboard(postEditField);
	}


	//region == Class custom methods ==

	public HLUser getUser() {
		return activity.getUser();
	}

	public Realm getRealm() {
		return activity.getRealm();
	}

	private void configureUpperSection(View view) {
		if (view != null) {
			mediaBackgroundContainer = view.findViewById(R.id.media_background_container);

			closeButton = view.findViewById(R.id.btn_close);
			closeButton.setOnClickListener(this);

			profilePictureLayout = view.findViewById(R.id.profile_picture);
			profilePicture = profilePictureLayout.findViewById(R.id.pic);
			profilePicture.setOnClickListener(this);

			btnSend = view.findViewById(R.id.btn_send);
			btnSend.setOnClickListener(this);
			btnNext = view.findViewById(R.id.btn_next);
			btnNext.setOnClickListener(this);

			postEditField = view.findViewById(R.id.post_edit_text);
			postEditField.addTextChangedListener(new TextWatcher() {

				int count;

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					this.count = count;
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					if (ignoreLink && (this.count != count))
						ignoreLink = false;
				}

				@Override
				public void afterTextChanged(Editable s) {
					handleNextBtn((s.length() > 0) || mPost.hasMedia() || Utils.isStringValid(mediaFileUri));

					if (s.length() > 0 && !ignoreLink) {
						WebLinkRecognizer.getInstance(activity).recognize(s.toString(), null, CreatePostHelper.this);
					}
				}
			});

			view.findViewById(R.id.privacy_container).setOnClickListener(this);
			privacyIcon = view.findViewById(R.id.privacy_icon);
			privacyText = view.findViewById(R.id.privacy_description);

			hiddenTagLayout = view.findViewById(R.id.hideable_tags_layout);
			hiddenTagsIcon = view.findViewById(R.id.icon_tags);
			hiddenTag1 = view.findViewById(R.id.tag_1);
			hiddenTag2 = view.findViewById(R.id.tag_2);
			hiddenTag3 = view.findViewById(R.id.tag_3);
			hiddenTag4 = view.findViewById(R.id.tag_4);
			hiddenTagMore = view.findViewById(R.id.tags_more);
			hiddenTagsAll = view.findViewById(R.id.all_tags);
			hiddenTagsAll.setOnClickListener(this);

			webLinkProgress = view.findViewById(R.id.progress_weblink);
			webLinkBoxLayout = view.findViewById(R.id.weblink_layout);
			webLinkImage = webLinkBoxLayout.findViewById(R.id.weblink_image);
			webLinkPlaceholder = webLinkBoxLayout.findViewById(R.id.weblink_placeholder);
			webLinkTitle = webLinkBoxLayout.findViewById(R.id.weblink_title);
			webLinkSource = webLinkBoxLayout.findViewById(R.id.weblink_source);
			webLinkBoxLayout.findViewById(R.id.weblink_delete).setOnClickListener(this);

			initiativeLabelLayout = view.findViewById(R.id.initiative_label_layout);
			initiativeLabelText = view.findViewById(R.id.initiative_label_txt);
			initiativeLabelIcon = view.findViewById(R.id.balloons_icon);
		}
	}

	private void configurePostToolbar(View view) {
		if (view != null) {
			mediaActionsContainer = view.findViewById(R.id.media_actions_container);

			View cameraLayout = view.findViewById(R.id.layout_btn_camera);
			View galleryLayout = view.findViewById(R.id.layout_btn_gallery);
			View audioRecLayout = view.findViewById(R.id.layout_btn_audiorec);
			View tagLayout = view.findViewById(R.id.layout_btn_tag);
			View initiativeLayout = view.findViewById(R.id.layout_btn_initiative);

			cameraView = view.findViewById(R.id.btn_camera);
			cameraView.setSelected(false);
			cameraView.setOnClickListener(this);
			galleryView = view.findViewById(R.id.btn_gallery);
			galleryView.setSelected(false);
			galleryView.setOnClickListener(this);
			audioRecView = view.findViewById(R.id.btn_audio);
			audioRecView.setSelected(false);
			audioRecView.setOnClickListener(this);
			tagView = view.findViewById(R.id.btn_tag);
			tagView.setSelected(false);
			tagView.setOnClickListener(this);
			initiativesView = view.findViewById(R.id.btn_initiative);
			initiativesView.setSelected(false);
			initiativesView.setOnClickListener(this);

			cameraTD = (TransitionDrawable) cameraView.getDrawable();
			cameraTD.setCrossFadeEnabled(true);
			galleryTD = (TransitionDrawable) galleryView.getDrawable();
			galleryTD.setCrossFadeEnabled(true);
			audioRecTD = (TransitionDrawable) audioRecView.getDrawable();
			audioRecTD.setCrossFadeEnabled(true);
			tagTD = (TransitionDrawable) tagView.getDrawable();
			tagTD.setCrossFadeEnabled(true);
			initiativesTD = (TransitionDrawable) initiativesView.getDrawable();
			initiativesTD.setCrossFadeEnabled(true);

			LinearLayout.LayoutParams lpCamera = (LinearLayout.LayoutParams) cameraLayout.getLayoutParams();
			LinearLayout.LayoutParams lpGallery = (LinearLayout.LayoutParams) galleryLayout.getLayoutParams();
			LinearLayout.LayoutParams lpAudioRec = (LinearLayout.LayoutParams) audioRecLayout.getLayoutParams();
			LinearLayout.LayoutParams lpTag = (LinearLayout.LayoutParams) tagLayout.getLayoutParams();

			boolean wantsInitiative = true;
			if (Utils.isStringValid(mPostToEditId)) {
				if (mPost == null)
					mPost = HLPosts.getInstance().getPost(mPostToEditId);

				wantsInitiative = mPost.isCHInitiative() || mPost.isGSInitiative();
			}
			boolean conditionForInitiative = viewType == ViewType.NORMAL && wantsInitiative;

			// INFO: 2/12/19    LUISS doesn't want INITIATIVEs
			conditionForInitiative = false;

			lpCamera.weight = conditionForInitiative ? .2f : .25f;
			lpGallery.weight = conditionForInitiative ? .2f : .25f;
			lpAudioRec.weight = conditionForInitiative ? .2f : .25f;
			lpTag.weight = conditionForInitiative ? .2f : .25f;

			initiativeLayout.setVisibility(conditionForInitiative ? View.VISIBLE : View.GONE);

			lpCamera.weight = conditionForInitiative ? .2f : .25f;
			lpGallery.weight = conditionForInitiative ? .2f : .25f;
			lpAudioRec.weight = conditionForInitiative ? .2f : .25f;
			lpTag.weight = conditionForInitiative ? .2f : .25f;

			cameraLayout.setLayoutParams(lpCamera);
			galleryLayout.setLayoutParams(lpGallery);
			audioRecLayout.setLayoutParams(lpAudioRec);
			tagLayout.setLayoutParams(lpTag);
		}
	}

	private void configureLowerSection(View view) {
		if (view != null) {
			configurePostToolbar(view);

			viewPager = view.findViewById(R.id.create_post_pager);
			viewPager.addOnPageChangeListener(this);

			FragmentManager fm = fragment != null ? fragment.getChildFragmentManager() : activity.getSupportFragmentManager();
			HLCreatePostPagerAdapter pagerAdapter = new HLCreatePostPagerAdapter(fm);
			viewPager.setAdapter(pagerAdapter);

			tagSearchRecView = view.findViewById(R.id.tag_search_rv);
			tagSearchNoResult = view.findViewById(R.id.no_result);
			tagSearchContainer = view.findViewById(R.id.tag_search_container);
		}
	}

	private void fireMediaCaptureIntent(HLMediaType type) {
		mediaCaptureType = type;
		mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, mediaCaptureType, fragment);
	}

	private void setData() {
		closeButton.setVisibility(viewType == ViewType.WISH ? View.GONE : View.VISIBLE);

		if (viewType == ViewType.NORMAL) {
			profilePictureLayout.setVisibility(View.VISIBLE);
			MediaHelper.loadProfilePictureWithPlaceholder(activity, getUser().getAvatarURL(), profilePicture);
		}
		else
			profilePictureLayout.setVisibility(View.GONE);

		boolean isEdit = Utils.isStringValid(mPostToEditId);
		if (isEdit) {
			mediaBackgroundContainer.setBackgroundColor(mPost.getBackgroundColor(activity.getResources()));
			newBackgroundSet = true;
		}

		@ColorInt int colorEdit = isEdit ?
				mPost.getTextStyle(activity.getResources()).getFirst() : (
						Utils.getColor(activity, newBackgroundSet ? R.color.white : R.color.black_87
				)
		);
		@ColorInt int colorPrivacy = Utils.getColor(activity, newBackgroundSet ? R.color.white : R.color.black_87);
		@DrawableRes int tags = newBackgroundSet ? R.drawable.ic_tag_white : R.drawable.ic_tag_black;
		@DrawableRes int tagsMore = newBackgroundSet ? R.drawable.ic_more_white : R.drawable.ic_more_black;
		@ColorInt int tagsBorderColor = Utils.getColor(activity, newBackgroundSet ? R.color.white : R.color.black_87);

		postEditField.setTextColor(colorEdit);
		postEditField.setHintTextColor(Utils.getColor(activity, newBackgroundSet ? R.color.white_50 : R.color.black_38));
		postEditField.setHint(activity.getString(R.string.hint_post_et, getUser().getSelectedIdentity() != null ?
				getUser().getSelectedIdentity().getName() : getUser().getFirstName()));
		String s = mPost != null ? mPost.getCaption() : null;
		String s1 = postEditField.getText().toString();
		if (Utils.isStringValid(s1))
			s = s1;
		if (!Utils.isStringValid(s))
			s = "";
		postEditField.setText(s);
		if (newBackgroundSet)
			postEditField.setShadowLayer(1f, 2f, 2f, Color.BLACK);
		else
			postEditField.setShadowLayer(0, 0, 0, 0);

		privacyText.setTextColor(colorPrivacy);
		int[] resources = PrivacyPostVisibilityEnum.getResources(mPost != null ? mPost.getPostVisibilityEnum() : null, newBackgroundSet);
		if (resources != null && resources.length == 2) {
			privacyIcon.setImageResource(resources[0]);
			privacyText.setText(resources[1]);
		}
		if (newBackgroundSet) {
			privacyText.setShadowLayer(1f, 2f, 2f, Color.BLACK);
			hiddenTagsAll.setShadowLayer(1f, 2f, 2f, Color.BLACK);

			initiativeLabelText.setShadowLayer(1f, 2f, 2f, Color.BLACK);
			initiativeLabelIcon.setImageResource(R.drawable.layer_initiatives);
		}
		else {
			privacyText.setShadowLayer(0, 0, 0, 0);
			hiddenTagsAll.setShadowLayer(0, 0, 0, 0);

			initiativeLabelText.setShadowLayer(0, 0, 0, 0);
			initiativeLabelIcon.setImageResource(R.drawable.ic_initiative_icon);
		}

		tagSearchRecView.setLayoutManager(tagSearchLlm);
		tagSearchRecView.setAdapter(tagSearchAdapter);

		hiddenTagsAll.setTextColor(colorEdit);
		hiddenTagsIcon.setImageResource(tags);
		hiddenTag1.setBorderColor(tagsBorderColor);
		hiddenTag2.setBorderColor(tagsBorderColor);
		hiddenTag3.setBorderColor(tagsBorderColor);
		hiddenTag4.setBorderColor(tagsBorderColor);
		hiddenTagMore.setImageResource(tagsMore);

		handleTagsLayout();
		if (mPost.hasInitiative())
			showHideInitiativeLabel(true, mPost.getInitiative().getText());

		boolean wantsFill = isEdit && !mPost.doesMediaWantFitScale();

		if (mediaCaptureType == HLMediaType.VIDEO && exoPlayerView != null) {
			MediaHelper.playVideo(
					activity,
					exoPlayerView,
					isEdit ? mPost.getContent(false) : fileToLoad,
					true, !wantsFill, true, playerPosition, null);
		}
		else if (mediaCaptureType == HLMediaType.PHOTO && backgroundForImageResult != null && !mediaPanelOpen) {

			backgroundForImageResult.setScaleType(wantsFill ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);

			GlideApp.with(activity)
					.load(isEdit ? mPost.getContent(false) : fileToLoad)
					.into(backgroundForImageResult);
		}

	}

	private void initializePost() {
		if (!Utils.isStringValid(mPostToEditId)) {
			mPost = new Post();

			mPost.setAuthor(getUser().getCompleteName());
			mPost.setAuthorId(getUser().getId());
			mPost.setAuthorUrl(getUser().getAvatarURL());

			setNewMedia(null);
			setTextMessage("");

			mPost.setContainers(new RealmList<>());
			mPost.setLists(new RealmList<>());
			mPost.setTags(new RealmList<>());
			mPost.setInterest(getUser().isActingAsInterest());

			mPost.setVisibility(getUser().getDefaultPostVisibility());
		}
		else {

			if (viewType == ViewType.NORMAL) {
				if (mPost == null)
					mPost = HLPosts.getInstance().getPost(mPostToEditId);

				if (mPost.hasMedia())
					mediaCaptureType = mPost.getTypeEnum().toMediaTypeEnum();

				if (mPost != null) {
					if (mPost.isPicturePost())
						changeBackgroundForMedia(HLMediaType.PHOTO, true);
					else if (mPost.isAudioPost())
						changeBackgroundForMedia(HLMediaType.AUDIO, true);
					else if (mPost.isVideoPost())
						changeBackgroundForMedia(HLMediaType.VIDEO, true);
					else if (mPost.isWebLinkPost()) {
						setWebLinkLayout();
						setData();
					}

					// initialize tags string starting from Post Tag objects
					selectedTags = mPost.getTags();
					if (mPost.hasInitiative() && selectedTags != null) {
						String recipientId = mPost.getInitiative().getRecipient();
						for (Tag tag : selectedTags) {
							if (Utils.areStringsValid(tag.getId(), recipientId) && tag.getId().equals(recipientId))
								tag.setInitiativeRecipient(true);
						}
					}
				}
			}
		}
	}

	private void handlePopupMenu() {
		PopupMenu menu = MediaHelper.openPopupMenu(activity, R.menu.popup_menu_camera, cameraView,
				menuItem -> {
					int id = menuItem.getItemId();

					switch (id) {
						case R.id.take_picture:
							fireMediaCaptureIntent(HLMediaType.PHOTO);
							break;
						case R.id.record_video:
							fireMediaCaptureIntent(HLMediaType.VIDEO);
							break;
					}
					return true;
				});

		if (menu != null) {
			menu.setOnDismissListener(menu1 -> {
				cameraView.setSelected(false);
				cameraTD.reverseTransition(100);
			});
		}
	}

	private void closeNoPost() {
		if (redirectToHome) {
			Intent result = new Intent(activity, HomeActivity.class);
			result.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_TIMELINE);
			activity.startActivity(result);
		}
		else
			activity.setResult(Activity.RESULT_CANCELED);

		if (Utils.hasLollipop() && viewType == ViewType.NORMAL)
			activity.finishAfterTransition();
		else {
			activity.finish();
			activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		}
	}

	private MaterialDialog createEmptyErrorDialog() {
		return new MaterialDialog.Builder(activity)
				.content(R.string.error_empty_post_dialog_message)
				.cancelable(true)
				.autoDismiss(true)
				.build();
	}

	private void goNext() {
		Pair<Boolean, File> result = composePostAndValidate();
		if (!result.first) return;

		activity.startActivityForResult(new Intent(activity, CreatePostPreviewActivity.class) {{
			this.putExtra(Constants.EXTRA_PARAM_1, mPost.serializeToString());
			this.putExtra(Constants.EXTRA_PARAM_2, mediaCaptureType);
			this.putExtra(Constants.EXTRA_PARAM_3, mediaFileUri);
		}}, Constants.RESULT_CREATE_POST_PREVIEW);
	}

	private void attemptSendingPost() {
		Pair<Boolean, File> result = composePostAndValidate();
		if (!result.first) return;

		File file = result.second;

		if (isMediaFileValid(file)) {
			new Handler().postDelayed(() -> activity.setProgressMessage(R.string.uploading_media), 500);
			attemptMediaUpload(file, mPost);
			return;
		}

		activity.setProgressMessage(R.string.sending_post);

		Object[] results = null;
		try {
			if (viewType == ViewType.NORMAL)
				results = HLServerCalls.createEditPost(mPost);
			else if (viewType == ViewType.WISH)
				results = HLServerCalls.createEditPostForWish(mPost);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((LBSLinkApp) activity.getApplication())).handleCallResult(this, activity, results);
	}

	private Pair<Boolean, File> composePostAndValidate() {
		if (mPost == null) {
			activity.showGenericError();
			return new Pair<>(false, null);
		}

		File file = null;
		if (Utils.isStringValid(mediaFileUri))
			file = new File(Uri.parse(mediaFileUri).getPath());
		String caption = postEditField.getText().toString();
		if (!Utils.isStringValid(caption) && !Utils.isStringValid(mPost.getContent()) &&
				!mPost.hasCaption() && !isMediaFileValid(file) && !mPost.hasWebLink()) {
			DialogUtils.showDialog(emptyPostErrorDialog);
			new Handler().postDelayed(() -> DialogUtils.closeDialog(emptyPostErrorDialog), 2500);
			return new Pair<>(false, null);
		}

		isUploadingOrSending = true;

		activity.openProgress();

		// TODO: 11/21/2017    TEMPORARY: RETAIN ORIGINAL POST'S DATE
		if (!Utils.isStringValid(mPost.getId())) {
			// sets common value
			mPost.setDate(Utils.formatDateForDB(System.currentTimeMillis()));
		}

		// if a caption is present, sets the caption
		setTextMessage(caption);

		// if tags are present, sets the tags in temporary String list
		if (selectedTags != null && !selectedTags.isEmpty())
			mPost.setTags(selectedTags);

		if (!isMediaFileValid(file) && !Utils.isStringValid(mPost.getContent()) && !mPost.hasWebLink()) {
			// if a media is not present AND the post doesn't contain a contentURL (!= editMode),
			// then the post is only-text -> post what we have
			mPost.setType(PostTypeEnum.TEXT.toString());
			setNewMedia(null);
		}

		return new Pair<>(true, file);
	}

	private boolean isMediaFileValid(File file) {
		return file != null && file.exists() && file.length() > 0;
	}

	public void animateMediaPanel(Boolean actionOpen) {
		if (mediaActionsContainer == null) return;

		ValueAnimator anim;
		int targetHeight;
		int baseHeight = mediaActionsContainer.getHeight();

		if (actionOpen == null)
			actionOpen = !mediaPanelOpen;
		if (!actionOpen)
			targetHeight = ACTIONS_BAR_HEIGHT;
		else
			targetHeight = ACTIONS_BAR_HEIGHT +	MEDIA_PANEL_HEIGHT;

		if (baseHeight != targetHeight) {
			anim = ValueAnimator.ofInt(baseHeight, targetHeight);
			anim.setDuration(500);
			anim.addUpdateListener(new MediaPanelAnimationUpdateListener());
			anim.addListener(new MediaPanelAnimationListener());
			anim.start();
		}

		if (fragment instanceof WishCreatePostFragment) {
			if (actionOpen)
				((WishCreatePostFragment) fragment).setBackListener();
			else
				((WishCreatePostFragment) fragment).removeBackListener();
		}

	}

	private void animateMediaPanel() {
		animateMediaPanel(null);
	}

	private void resetAllTransitionDrawables() {
		cameraTD.resetTransition();
		cameraView.setSelected(false);
		galleryTD.resetTransition();
		galleryView.setSelected(false);
		audioRecTD.resetTransition();
		audioRecView.setSelected(false);
		tagTD.resetTransition();
		tagView.setSelected(false);
		initiativesTD.resetTransition();
		initiativesView.setSelected(false);

		// TODO: 3/16/2018    add future drawables and views here
	}

	private void attemptMediaUpload(final File f, final @NonNull Post post) {
		if (mPost.isVideoPost())
			activity.setProgressMessage(R.string.uploading_media);
		activity.openProgress();

		activity.setShowProgressAnyway(false);

		final MediaHelper.UploadService service = new MediaHelper.UploadService(activity);
		try {
			MediaHelper.MediaUploadListener listener = new MediaHelper.MediaUploadListener(post,
					activity, mediaCaptureType, viewType);
			service.uploadMedia(activity, f, getUser().getId(), listener);
		} catch (IllegalArgumentException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
			activity.showAlertWithRetry(R.string.error_upload_media, v -> {
				activity.openProgress();
				try {
					MediaHelper.MediaUploadListener listener =
							new MediaHelper.MediaUploadListener(post, activity,
									mediaCaptureType, viewType);
					service.uploadMedia(activity, f, getUser().getId(), listener);
				} catch (IllegalArgumentException e1) {
					LogUtils.e(LOG_TAG, e.getMessage() + "\n\n2nd time in a row!", e);
					activity.showAlert(R.string.error_upload_media);
				}
			});
			activity.closeProgress();
		}
	}

	private void handleMediaResult(final File f) {
		if (isMediaFileValid(f)) {

			fileToLoad = f;

			deleteWebLink();

			if (mediaBackgroundContainer != null)
				mediaBackgroundContainer.removeAllViews();

			if (mediaPanelOpen) animateMediaPanel(false);

			if (mediaCaptureType == HLMediaType.PHOTO ||
					mediaCaptureType == HLMediaType.PHOTO_PROFILE ||
					mediaCaptureType == HLMediaType.PHOTO_WALL) {

				if (f.exists() && Utils.isContextValid(activity)) {

					changeBackgroundForMedia(HLMediaType.PHOTO, false);
				}
			}
			else {
				String extension = MimeTypeMap.getFileExtensionFromUrl(mediaFileUri);
				if (Utils.isStringValid(extension) && !extension.equals("mp4")) {
					final MaterialDialog dialog = new MaterialDialog.Builder(activity)
							.content(R.string.error_wrong_video_extension)
							.cancelable(true)
							.autoDismiss(true)
							.show();
					new Handler().postDelayed(() -> DialogUtils.closeDialog(dialog), 3000);

					return;
				}

				changeBackgroundForMedia(HLMediaType.VIDEO, false);
			}
		}
		else {
			activity.setShowProgressAnyway(false);
			activity.closeProgress();
		}
	}


	// FIXME: 3/16/2018    do something in activity

	public void onTouchEvent(MotionEvent event) {
		gdt.onTouchEvent(event);
	}


	private void callForLinkParsing(String link) {
		if (link != null) {
			activity.runOnUiThread(() -> webLinkProgress.setVisibility(View.VISIBLE));

			Object[] result = null;

			try {
				result = HLServerCalls.getParsedWebLink(link, null);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (Utils.isContextValid(activity)) {
				HLRequestTracker.getInstance(((LBSLinkApp) activity.getApplication()))
						.handleCallResult(this, activity, result);
			}
		}
	}

	private void setWebLinkLayout() {
		if (mPost != null && mPost.hasWebLink()) {
			MediaHelper.loadPictureWithGlide(
					activity,
					Utils.isStringValid(mPost.getWebLinkImage()) ? mPost.getWebLinkImage() : Constants.WEB_LINK_PLACEHOLDER_URL,
					null,
					0, 0,
					new CustomViewTarget<ImageView, Drawable>(webLinkImage) {
						@Override
						protected void onResourceCleared(@Nullable Drawable placeholder) {
							webLinkPlaceholder.setVisibility(View.VISIBLE);
							MediaHelper.loadPictureWithGlide(activity, Constants.WEB_LINK_PLACEHOLDER_URL, getView());
						}

						@Override
						public void onLoadFailed(@Nullable Drawable errorDrawable) {
							webLinkPlaceholder.setVisibility(View.VISIBLE);
							MediaHelper.loadPictureWithGlide(activity, Constants.WEB_LINK_PLACEHOLDER_URL, getView());
						}

						@Override
						public void onResourceReady(@NonNull Drawable resource, @Nullable Transition transition) {
							webLinkPlaceholder.setVisibility(View.GONE);
							getView().setImageDrawable(resource);
						}
					}
			);
			webLinkTitle.setText(mPost.getWebLinkTitle());
			webLinkSource.setText(mPost.getWebLinkSource());
			webLinkBoxLayout.setVisibility(View.VISIBLE);
			webLinkProgress.setVisibility(View.GONE);

			if (!Utils.isStringValid(postEditField.getText().toString()) ||
					postEditField.getText().toString().contains(mPost.getWebLinkUrl()))
				postEditField.setText(R.string.cpost_weblink_def_hint);

			mPost.setType(PostTypeEnum.WEB_LINK.toString());
			setNewMedia(null);

			if (mediaBackgroundContainer != null) {
				mediaBackgroundContainer.removeAllViews();
				mediaBackgroundContainer.setBackground(null);
				newBackgroundSet = false;
				setData();
			}
		}
	}

	private void deleteWebLink() {
		webLinkBoxLayout.setVisibility(View.GONE);

		if (Utils.isStringValid(mPostToEditId)) {
			getRealm().executeTransaction(realm -> mPost.setWebLink(null));
		}
		else mPost.setWebLink(null);

		if (postEditField.getText().toString().equals(activity.getString(R.string.cpost_weblink_def_hint)))
			postEditField.setText("");

		if (mediaBackgroundContainer != null)
			mediaBackgroundContainer.removeAllViews();
	}

	private void setPostVisibility(Intent data, int rawValue) {
		ArrayList<String> values = new ArrayList<>();
		if (data != null) {
			if (data.hasExtra(Constants.EXTRA_PARAM_2))
				rawValue = data.getIntExtra(Constants.EXTRA_PARAM_2, 0);
			if (data.hasExtra(Constants.EXTRA_PARAM_3))
				values = data.getStringArrayListExtra(Constants.EXTRA_PARAM_3);
		}

		if (rawValue >= 0) {
			PostVisibility visibility = new PostVisibility();
			visibility.setRawVisibilityType(rawValue);

			if (values != null && !values.isEmpty())
				visibility.setValuesArrayList(values);

			mPost.setVisibility(visibility);
		}
	}


	private void handleNextBtn(boolean visible) {
		boolean wantsNext = visible && viewType != ViewType.WISH && !mPost.hasWebLink();

		btnNext.setVisibility(wantsNext ? View.VISIBLE : View.GONE);
		btnSend.setVisibility(wantsNext ? View.GONE : View.VISIBLE);
	}


	private void setNewMedia(HLMediaType type) {
		if (mPost != null) {

			if (mPost.hasMedia())
				mPost.getMediaObjects().clear();
			else
				mPost.setMediaObjects(new RealmList<>());

			if (type != null) {
				MemoryMediaObject newMedia = new MemoryMediaObject();
				newMedia.setType(type.toString());

				mPost.getMediaObjects().add(newMedia);
			}
		}
	}

	private void setTextMessage(String message) {
		if (mPost != null) {

			if (mPost.hasCaption())
				mPost.getMessageObject().setMessage("");
			else
				mPost.setMessageObject(new MemoryMessageObject());

			if (Utils.isStringValid(message))
				mPost.getMessageObject().setMessage(message);
		}
	}

	private void changeBackgroundForMedia(HLMediaType mediaType, boolean isEdit) {
		View bck = null;
		backgroundForImageResult = null;
		exoPlayerView = null;

		switch (mediaType) {
			case AUDIO:
				bck = LayoutInflater.from(activity).inflate(R.layout.item_post_audio, mediaBackgroundContainer, false);
				if (bck != null) {
					bck.findViewById(R.id.mask_container).setVisibility(View.GONE);
					bck.findViewById(R.id.progress_layout).setVisibility(View.GONE);

					final View playBtn = bck.findViewById(R.id.play_btn);
					playBtn.setVisibility(View.GONE);
					View pauseBtn = bck.findViewById(R.id.pause_btn);
					pauseBtn.setVisibility(View.GONE);
					final View pauseLayout = bck.findViewById(R.id.pause_layout);
					pauseLayout.setVisibility(View.GONE);

					final LottieAnimationView animationView = bck.findViewById(R.id.wave3);
					if (siriComposition == null)
						siriComposition = LBSLinkApp.siriComposition;


					// currently not needed since PLAY and PAUSE are disabled

//					playBtn.setOnClickListener(v -> {
//						playBtn.setVisibility(View.GONE);
//						pauseLayout.setVisibility(View.VISIBLE);
//						MediaHelper.playAudio(mediaPlayer, mediaFileUri, false);
//
//						if (pauseFrame != 0){
//							animationView.setFrame(pauseFrame);
//							animationView.resumeAnimation();
//						}
//						else animationView.playAnimation();
//					});
//					pauseLayout.setOnClickListener(v -> {
//						playBtn.setVisibility(View.VISIBLE);
//						pauseLayout.setVisibility(View.GONE);
//
//						if (mediaPlayer.isPlaying())
//							mediaPlayer.pause();
//
//						animationView.pauseAnimation();
//						pauseFrame = animationView.getFrame();
//					});

					if (animationView != null && siriComposition != null) {
						animationView.setComposition(siriComposition);
						animationView.cancelAnimation();
						animationView.setFrame(10);
					}
				}
				break;

			case PHOTO:
				backgroundForImageResult = new ImageView(activity);
				FrameLayout.LayoutParams lp = (isEdit && mPost.doesMediaWantFitScale()) ?
						new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER) :
						new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
				backgroundForImageResult.setLayoutParams(lp);
				backgroundForImageResult.setScaleType(ImageView.ScaleType.FIT_CENTER);

				bck = backgroundForImageResult;
				break;

			case VIDEO:
				exoPlayerView = new PlayerViewNoController(activity);
				FrameLayout.LayoutParams lp1 = (isEdit && mPost.doesMediaWantFitScale()) ?
						new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER) :
						new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
				exoPlayerView.setLayoutParams(lp1);
				exoPlayerView.setControllerAutoShow(false);
				exoPlayerView.setUseController(false);

				bck = exoPlayerView;
				break;
		}

		if (bck != null) {
			mediaBackgroundContainer.setBackgroundColor(Color.BLACK);
			mediaBackgroundContainer.addView(bck);
			newBackgroundSet = true;

			setData();

			handleNextBtn(true);
		}
	}

	//endregion



	//region == Interfaces methods ==

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

	@Override
	public void onPageSelected(int position) {
		switch (position) {
			case PAGER_ITEM_GALLERY:
				switch (currentPagerItem) {
					case ITEM_CAMERA:
						cameraTD.reverseTransition(100);
						return;
					case PAGER_ITEM_AUDIO:
						if (audioRecView.isSelected())
							audioRecTD.reverseTransition(100);
						break;
					case PAGER_ITEM_TAG:
						if (tagView.isSelected())
							tagTD.reverseTransition(100);
						break;
					case PAGER_ITEM_INITIATIVES:
						if (initiativesView.isSelected())
							initiativesTD.reverseTransition(100);
						break;
				}
				galleryView.setSelected(true);
				galleryTD.startTransition(200);

				cameraView.setSelected(false);
				audioRecView.setSelected(false);
				tagView.setSelected(false);
				initiativesView.setSelected(false);
				break;

			case PAGER_ITEM_AUDIO:
				switch (currentPagerItem) {
					case ITEM_CAMERA:
						cameraTD.reverseTransition(100);
						break;
					case PAGER_ITEM_GALLERY:
						if (galleryView.isSelected())
							galleryTD.reverseTransition(100);
						break;
					case PAGER_ITEM_TAG:
						if (tagView.isSelected())
							tagTD.reverseTransition(100);
						break;
					case PAGER_ITEM_INITIATIVES:
						if (initiativesView.isSelected())
							initiativesTD.reverseTransition(100);
						break;
				}
				audioRecView.setSelected(true);
				audioRecTD.startTransition(200);

				cameraView.setSelected(false);
				galleryView.setSelected(false);
				tagView.setSelected(false);
				initiativesView.setSelected(false);
				break;

			case PAGER_ITEM_TAG:
				// switches visibility between Tag and Initiatives fragments
				tagVisible = true;

				switch (currentPagerItem) {
					case ITEM_CAMERA:
						cameraTD.reverseTransition(100);
						break;
					case PAGER_ITEM_GALLERY:
						if (galleryView.isSelected())
							galleryTD.reverseTransition(100);
						break;
					case PAGER_ITEM_AUDIO:
						if (audioRecView.isSelected())
							audioRecTD.reverseTransition(100);
						break;
					case PAGER_ITEM_INITIATIVES:
						if (initiativesView.isSelected())
							initiativesTD.reverseTransition(100);
						break;
				}

				tagView.setSelected(true);
				tagTD.startTransition(200);

				cameraView.setSelected(false);
				galleryView.setSelected(false);
				audioRecView.setSelected(false);
				initiativesView.setSelected(false);

				TagAdapter.setFromInitiatives(false);

				if (tagFragment != null && tagFragment.get() != null) {
					tagFragment.get().getAdapterPeople().notifyDataSetChanged();
					tagFragment.get().getAdapterInterests().notifyDataSetChanged();
				}
				break;

			case PAGER_ITEM_INITIATIVES:
				// switches visibility between Tag and Initiatives fragments
				tagVisible = false;

				switch (currentPagerItem) {
					case ITEM_CAMERA:
						cameraTD.reverseTransition(100);
						break;
					case PAGER_ITEM_GALLERY:
						if (galleryView.isSelected())
							galleryTD.reverseTransition(100);
						break;
					case PAGER_ITEM_AUDIO:
						if (audioRecView.isSelected())
							audioRecTD.reverseTransition(100);
						break;
					case PAGER_ITEM_TAG:
						if (tagView.isSelected())
							tagTD.reverseTransition(100);
						break;
				}

				initiativesView.setSelected(true);
				initiativesTD.startTransition(200);

				cameraView.setSelected(false);
				galleryView.setSelected(false);
				audioRecView.setSelected(false);
				tagView.setSelected(false);

				TagAdapter.setFromInitiatives(true);
				break;
		}

		currentPagerItem = position;

		animateMediaPanel(true);
	}

	@Override
	public void onPageScrollStateChanged(int state) {}

	@Override
	public void onFlingCompleted() {
		attemptSendingPost();
	}

	/* Gallery Click Listeners */
	@Override
	public void onClickImage(String imageUri) {

		activity.setProgressMessage(R.string.processing_file);
		activity.openProgress();

		mediaFileUri = imageUri;
		setPostType(PostTypeEnum.PHOTO);
		setMediaCaptureType(HLMediaType.PHOTO);
		File f = new File(Uri.parse(imageUri).getPath());
		handleMediaResult(f);
	}

	@Override
	public void onClickVideo(String videoUri) {
		mediaFileUri = videoUri;
		setPostType(PostTypeEnum.VIDEO);
		setMediaCaptureType(HLMediaType.VIDEO);
		File f = new File(Uri.parse(videoUri).getPath());
		handleMediaResult(f);
		setData();
	}

	// TAG
	@Override
	public Object[] isObjectForTagSelected(String id, boolean fromInitiatives) {
		if (selectedTags != null && !selectedTags.isEmpty()) {
			for (Tag tag :
					selectedTags) {
				if (Utils.areStringsValid(tag.getId(), id) && tag.getId().equals(id)) {
					boolean conditionOnFragment = tagVisible;
					return new Object[]{
							!fromInitiatives || tag.isInitiativeRecipient(),
							tag.isInitiativeRecipient() && conditionOnFragment ?
									R.drawable.shape_circle_white_alpha_light : R.drawable.shape_circle_orange_alpha
					};
				}
			}
		}
		return new Object[] { false, null };
	}

	@Override
	public void onItemClick(Object object) {}

	@Override
	public void onItemClick(Object object, boolean fromInitiatives) {

		Tag tag = null;
		if (object instanceof Tag)
			tag = (Tag) object;
		else if (object instanceof HLUserGeneric)
			tag = Tag.convertFromGenericUser((HLUserGeneric) object);
		else if  (object instanceof Interest)
			tag = Tag.convertFromInterest((Interest) object);
		else {
			// TODO: 4/3/2018    to be completed when available #Strings
		}

		if (tag != null) {
			if (fromInitiatives)
				handleTagClickFromInitiatives(tag);
			else
				handleTagClickFromTags(tag);
		}
	}

	private void handleTagClickFromInitiatives(@NonNull Tag tag) {
		boolean remove = false;
		if (selectedTags.contains(tag)) {
			Tag selTag = selectedTags.get(selectedTags.indexOf(tag));

			if (selTag != null) {
				boolean isRecipient = selTag.isInitiativeRecipient();
				if (!isRecipient && haveTagsInitiativeRecipient()) {
					activity.showAlert(R.string.error_cpost_initiative_recipient);
					return;
				}
				selTag.setInitiativeRecipient(!isRecipient);
				remove = !selTag.isInitiativeRecipient();
			}
		}
		else {
			tag.setInitiativeRecipient(true);
			selectedTags.add(tag);
		}

		if (initiativeFragment != null) {
			InitiativesFragment frg = initiativeFragment.get();
			if (frg != null) {
				frg.getAdapterPeople().notifyDataSetChanged();
				frg.getAdapterInterests().notifyDataSetChanged();

				frg.setInitiativeRecipient(!remove ? tag : null);
			}
		}
		if (tagFragment != null) {
			TagFragment frg = tagFragment.get();
			if (frg != null && !remove) {
				frg.getAdapterPeople().notifyDataSetChanged();
				frg.getAdapterInterests().notifyDataSetChanged();
			}
		}

		handleTagsLayout();
	}

	private void handleTagClickFromTags(@NonNull Tag tag) {
		if (selectedTags.contains(tag)) {
			Tag selTag = selectedTags.get(selectedTags.indexOf(tag));
			if (selTag != null && !selTag.isInitiativeRecipient())
				selectedTags.remove(selTag);
		}
		else {
			tag.setInitiativeRecipient(false);
			selectedTags.add(tag);
		}

		if (tagFragment != null) {
			TagFragment frg = tagFragment.get();
			if (frg != null) {
				frg.getAdapterPeople().notifyDataSetChanged();
				frg.getAdapterInterests().notifyDataSetChanged();
			}
		}

		handleTagsLayout();
	}

	public boolean haveTagsInitiativeRecipient() {
		if (selectedTags == null || selectedTags.isEmpty())
			return false;

		for (Tag tag : selectedTags) {
			if (tag.isInitiativeRecipient())
				return true;
		}

		return false;
	}

	/**
	 * Checks whether the selectedTags list contains the provided id and it actually is the Initiative's
	 * recipient.
	 * @param id The provided id as String
	 * @return True if the id belongs to the Initiative's recipient, false otherwise.
	 */
	public boolean checkInitiativeRecipientId(final String id) {
		List<Tag> recipients = Stream.of(selectedTags)
				.filter(
						value ->
								Utils.areStringsValid(value.getId(), id) && value.getId().equals(id) &&
										value.isInitiativeRecipient()
				)
				.collect(Collectors.toList());

		return recipients != null && recipients.size() == 1;
	}

	private void handleTagsLayout() {
		if (hiddenTagLayout != null) {
			if (selectedTags != null && !selectedTags.isEmpty()) {
				hiddenTagMore.setVisibility(selectedTags.size() > 4 ? View.VISIBLE : View.GONE);

				hiddenTag2.setVisibility(selectedTags.size() >= 2 ? View.VISIBLE : View.GONE);
				hiddenTag3.setVisibility(selectedTags.size() >= 3 ? View.VISIBLE : View.GONE);
				hiddenTag4.setVisibility(selectedTags.size() >= 4 ? View.VISIBLE : View.GONE);

				for (int i = 0; i < selectedTags.size(); i++) {
					Tag tag = selectedTags.get(i);
					ImageView v;
					if (i == 0)
						v = hiddenTag1;
					else if (i == 1)
						v = hiddenTag2;
					else if (i == 2)
						v = hiddenTag3;
					else if (i == 3)
						v = hiddenTag4;
					else
						break;

					if (v != null) {
						if (tag != null && Utils.isStringValid(tag.getUserUrl()))
							MediaHelper.loadProfilePictureWithPlaceholder(activity, tag.getUserUrl(), v);
						else
							v.setImageResource(R.drawable.ic_profile_placeholder);
					}
				}

				if (selectedTags.size() >= 1) {
					hiddenTagLayout.animate().alpha(1).setDuration(300).setListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {
							hiddenTagLayout.setVisibility(View.VISIBLE);
						}

						@Override
						public void onAnimationEnd(Animator animation) {}

						@Override
						public void onAnimationCancel(Animator animation) {}

						@Override
						public void onAnimationRepeat(Animator animation) {}
					}).start();
				}
			}
			else {
				hiddenTagLayout.animate().alpha(0).setDuration(300).setListener(new Animator.AnimatorListener() {
					@Override
					public void onAnimationStart(Animator animation) {}

					@Override
					public void onAnimationEnd(Animator animation) {
						hiddenTagLayout.setVisibility(View.GONE);
					}

					@Override
					public void onAnimationCancel(Animator animation) {}

					@Override
					public void onAnimationRepeat(Animator animation) {}
				}).start();
			}
		}
	}

	@Override
	public void onItemClick(Object object, View view) {}

	@Override
	public BasicAdapterInteractionsListener getCreatePostHelper() {
		return this;
	}

	@Override
	public void addTagToSearchList(Tag tag) {
		if (searchList == null)
			searchList = new ArrayList<>();

		if (!searchList.contains(tag))
			searchList.add(tag);
	}

	private String query = null;
	@Override
	public void updateSearchData(final String query) {
		if (Utils.isStringValid(query)) {

			if (searchListToShow == null)
				searchListToShow = new ArrayList<>();
			else
				searchListToShow.clear();

			searchListToShow.addAll(Stream.of(searchList).filter(new TagSearchPredicate(query.toLowerCase(), false))
					.collect(Collectors.toList()));

			List<Object> tempList =
					new ArrayList<>(Stream.of(searchList).filter(new TagSearchPredicate(query.toLowerCase(), true))
							.collect(Collectors.toList()));

			if (!tempList.isEmpty()) {
				if (!searchListToShow.isEmpty())
					searchListToShow.add(new View(activity));

				searchListToShow.addAll(tempList);
			}

			if (searchListToShow.isEmpty()) {
				tagSearchRecView.setVisibility(View.GONE);
				tagSearchNoResult.setVisibility(View.VISIBLE);
			}
			tagSearchRecView.setVisibility(View.VISIBLE);
			tagSearchNoResult.setVisibility(View.GONE);

			tagSearchAdapter.notifyDataSetChanged();

			if (!Utils.areStringsValid(this.query))
				animateTagSearch(true);

			this.query = query;
		}
		else {
			this.query = null;
			animateTagSearch(false);
		}
	}

	private void animateTagSearch(boolean open) {

		ValueAnimator animationRecView;
		if (open)
			animationRecView = ValueAnimator.ofInt(0, TAG_SEARCH_PANEL_HEIGHT);
		else
			animationRecView = ValueAnimator.ofInt(TAG_SEARCH_PANEL_HEIGHT, 0);
		animationRecView.setDuration(300);
		animationRecView.addUpdateListener(new TagSearchAnimationUpdateListener());
		animationRecView.addListener(new TagSearchAnimationListener(open));

		ValueAnimator animationContainer;
		if (open) {
			animationContainer = ValueAnimator.ofInt(
					MEDIA_PANEL_HEIGHT + ACTIONS_BAR_HEIGHT,
					MEDIA_PANEL_HEIGHT + TAG_SEARCH_PANEL_HEIGHT
			);
			animationContainer.setStartDelay(150);
		}
		else {
			animationContainer = ValueAnimator.ofInt(
					MEDIA_PANEL_HEIGHT + TAG_SEARCH_PANEL_HEIGHT,
					MEDIA_PANEL_HEIGHT + ACTIONS_BAR_HEIGHT
			);
		}
		animationContainer.setDuration(150);
		animationContainer.addUpdateListener(new MediaPanelAnimationUpdateListener());

		AnimatorSet set = new AnimatorSet();
		set.playTogether(animationRecView, animationContainer);
		set.start();
	}

	// INITIATIVES
	@Override
	public void attachInitiativeToPost(Initiative initiative) {
		if (mPost != null) {
			mPost.setInitiative(initiative);
			mPost.setInitiative(true);
		}
	}

	@Override
	public CreatePostHelper getHelperObject() {
		return this;
	}

	@Override
	public void showHideInitiativeLabel(boolean show, @Nullable String text) {
		if (show && Utils.isStringValid(text)) {
			initiativeLabelText.setText(text);
			initiativeLabelLayout.setVisibility(View.VISIBLE);
		}
		else
			initiativeLabelLayout.setVisibility(View.GONE);
	}

	@Override
	public void updateVisibilityForInitiative() {
		setPostVisibility(null, PrivacyPostVisibilityEnum.PUBLIC.getValue());
		setData();
	}

	// GALLERY
	public void setPostType(PostTypeEnum type) {
		mPost.setType(type.toString());
		setNewMedia(type.toMediaTypeEnum());
	}

	public void setMediaCaptureType(HLMediaType type) {
		mediaCaptureType = type;
	}

	public void checkPermissionForGallery(HLMediaType type) {
		MediaHelper.checkPermissionForGallery(activity, type, fragment);
	}

	// AUDIO RECORDING
	@NonNull
	public String getAudioMediaFileUri() {
		return mediaFileUri;
	}

	private LottieComposition siriComposition;
	public void exitFromRecordingAndSetAudioBackground() {
		mPost.setType(PostTypeEnum.AUDIO.toString());
		mediaCaptureType = HLMediaType.AUDIO;
		setNewMedia(HLMediaType.AUDIO);

		animateMediaPanel(false);
		audioRecView.setSelected(false);
		audioRecTD.resetTransition();

		deleteWebLink();

		if (mediaBackgroundContainer != null)
			changeBackgroundForMedia(HLMediaType.AUDIO, false);
	}

	public boolean isEditAudioPost() {
		return Utils.isStringValid(mPostToEditId) && mPost.isAudioPost();
	}

	@NonNull
	public String getAudioUrl() {
		return mPost.getContent();
	}


	// web link recognition

	@Override
	public void onLinkRecognized(@NotNull String group) {
		callForLinkParsing(group);
	}


	//endregion


	//region == Custom inner classes ==

	private class HLCreatePostPagerAdapter extends FragmentStatePagerAdapter/*WithTags*/ {

		HLCreatePostPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case PAGER_ITEM_GALLERY:
					return GalleryFragment.newInstance();
				case PAGER_ITEM_AUDIO:
					return AudioRecordFragment.newInstance("", "");
				case PAGER_ITEM_TAG:
					tagFragment = new WeakReference<>(TagFragment.newInstance());
					return tagFragment.get();
				case PAGER_ITEM_INITIATIVES:
					boolean editMode = Utils.isStringValid(mPostToEditId);
					Initiative toEdit = editMode ? mPost.getInitiative() : null;
					initiativeFragment = new WeakReference<>(InitiativesFragment.newInstance(editMode, toEdit));
					return initiativeFragment.get();

				// TODO: 11/14/2017    ADD NEW CASES
//				case PAGER_ITEM_...:
//					return ...Fragment.newInstance("", "");
			}
			return null;
		}

		@Override
		public int getCount() {
			return PAGER_COUNT;
		}
	}


	private class MediaPanelAnimationUpdateListener implements ValueAnimator.AnimatorUpdateListener {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			mediaActionsContainer.getLayoutParams().height = (Integer) animation.getAnimatedValue();
			mediaActionsContainer.setLayoutParams(mediaActionsContainer.getLayoutParams());
		}
	}


	private class MediaPanelAnimationListener implements ValueAnimator.AnimatorListener {

		@Override
		public void onAnimationStart(Animator animation) {}

		@Override
		public void onAnimationEnd(Animator animation) {
			if (mediaPanelOpen) {
				resetAllTransitionDrawables();

				if (openPopup) {
					handlePopupMenu();
					openPopup = false;
				}
			}
			mediaPanelOpen = !mediaPanelOpen;

			if (!mediaPanelOpen)
				activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			else
				activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

			if (activity.isProgressVisible() && MediaUploadManager.Companion.isMediaFileValid(fileToLoad)
					&& backgroundForImageResult != null) {

				GlideApp.with(activity).load(fileToLoad).into(backgroundForImageResult);

				activity.setShowProgressAnyway(false);
				activity.closeProgress();
			}

		}

		@Override
		public void onAnimationCancel(Animator animation) {}

		@Override
		public void onAnimationRepeat(Animator animation) {}
	}


	private class TagSearchAnimationUpdateListener implements ValueAnimator.AnimatorUpdateListener {

		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			tagSearchRecView.getLayoutParams().height = ((int) animation.getAnimatedValue());
			tagSearchRecView.setLayoutParams(tagSearchRecView.getLayoutParams());
		}
	}

	private class TagSearchAnimationListener implements ValueAnimator.AnimatorListener {

		private boolean open;

		TagSearchAnimationListener(boolean open) {
			this.open = open;
		}

		@Override
		public void onAnimationStart(Animator animation) {
			if (open)
				tagSearchRecView.setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			if (!open) {
				tagSearchRecView.setVisibility(View.GONE);

				tagSearchContainer.setPadding(0, 0, 0, 0);
			}
			else {
				tagSearchContainer.setPadding(
						0,
						Utils.dpToPx(5f, activity.getResources()),
						0,
						Utils.dpToPx(5f, activity.getResources())
				);
			}
		}

		@Override
		public void onAnimationCancel(Animator animation) {}

		@Override
		public void onAnimationRepeat(Animator animation) {}
	}


	private class TagSearchPredicate implements Predicate<Tag> {

		final String query;
		final boolean handleInterest;

		TagSearchPredicate(String query, boolean interest) {
			this.query = query;
			handleInterest = interest;
		}

		@Override
		public boolean test(Tag value) {
			boolean condition = value != null &&
					Utils.isStringValid(value.getUserName()) &&
					value.getUserName().toLowerCase().contains(query);

			if (value != null) {
				if (handleInterest)
					return value.isInterest() && condition;
				else
					return !value.isInterest() && condition;
			}
			return false;
		}
	}

	//endregion


	//region == Getters and setters ==

	public void setRedirectToHome(boolean redirectToHome) {
		this.redirectToHome = redirectToHome;
	}

	public HLActivity getActivity() {
		return activity;
	}
	public void setActivity(HLActivity activity) {
		this.activity = activity;
	}

	//endregion
}