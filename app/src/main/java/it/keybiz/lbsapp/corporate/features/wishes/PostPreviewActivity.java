/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;

import java.text.SimpleDateFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.ResizingTextWatcher;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 3/5/2018.
 */
public class PostPreviewActivity extends HLActivity implements View.OnClickListener {

	private MediaPlayer mediaPlayer;

	private CardView postPreviewCard;

	private String transitionName;

	/* COMMON VIEWS */
	private CircleImageView profilePic;
	private TextView userName;
	private TextView postCaption;
	private TextView postDate, postLocation;

	private View postPicture, postVideo, postText, postAudio;

	/* PICTURE VIEWS */
	private ImageView postImage;

	/* VIDEO VIEWS */
	private VideoView videoView;
	private ImageView thumbnailView;
	private View thumbnailLayout;
	private View playBtnVideo;
	private View pauseLayoutVideo;
	private View progressViewVideo;
	private TextView progressMessageVideo;
	private View iconPlaceholder;

	/* AUDIO VIEWS */
	private View playBtnAudio;
	private View pauseLayoutAudio;
	private View progressViewAudio;
	private TextView progressMessageAudio;
	private int pauseFrame = 0;
	private LottieAnimationView animationView;
	private LottieComposition siriComposition;

	/* TEXT VIEWS */
	private TextView postTextView;

	private int lastPlayerPosition;
	private boolean playing = false;


	private Post selectedPost;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wish_post_enlargement);
		View mainView;
		setRootContent(mainView = findViewById(R.id.root_content));
//		supportPostponeEnterTransition();

		selectedPost = HLPosts.getInstance().getSelectedPostForWish();

		mediaPlayer = new MediaPlayer();
		siriComposition = LBSLinkApp.siriComposition;

		manageIntent();

		postPreviewCard = findViewById(R.id.post_preview);

//		if (Utils.hasLollipop()) {
//			postPreviewCard.setTransitionName(transitionName);
//			postPreviewCard.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//				@Override
//				public boolean onPreDraw() {
//					postPreviewCard.getViewTreeObserver().removeOnPreDrawListener(this);
//					supportStartPostponedEnterTransition();
//					return true;
//				}
//			});
//		}

		if (mainView != null)
			mainView.setOnClickListener(this);

		configureLayout();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();

		setLayout();
	}

	@Override
	protected void onPause() {
		mediaPlayer.pause();
		mediaPlayer.stop();
		mediaPlayer.release();

		super.onPause();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.play_btn:
				if (selectedPost != null) {
					if (selectedPost.isAudioPost()) {
						MediaHelper.playAudioWithResources(mediaPlayer, lastPlayerPosition, selectedPost.getContent(),
								playBtnAudio, pauseLayoutAudio, progressViewAudio, progressMessageAudio,
								animationView);
					}
					else if (selectedPost.isVideoPost()) {
						MediaHelper.playVideoWithResources(videoView, thumbnailLayout, selectedPost.getContent(),
								playBtnVideo, progressViewVideo, progressMessageVideo, false,
								lastPlayerPosition);
					}
				}
				break;

			case R.id.pause_layout:
				if (selectedPost.isAudioPost())
					pauseFrame = animationView.getFrame();

				lastPlayerPosition = mediaPlayer.getCurrentPosition();
				mediaPlayer.pause();

			default:
				returnHome();

		}

	}

	@Override
	protected void configureResponseReceiver() {}

	@Override
	protected void manageIntent() {
		Bundle b = getIntent().getExtras();
		if (b != null && b.containsKey(Constants.EXTRA_PARAM_1))
			transitionName = b.getString(Constants.EXTRA_PARAM_1);

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		returnHome();
	}



	private void configureLayout() {
		profilePic = findViewById(R.id.post_enlarge_avatar);
		userName = findViewById(R.id.post_enlarge_author);
		postCaption = findViewById(R.id.post_caption);
		postDate = findViewById(R.id.post_enlarge_date);
		postLocation = findViewById(R.id.post_enlarge_location);


		/* PICTURE POST */
		postPicture = findViewById(R.id.prev_post_photo);
		postImage = findViewById(R.id.image_preview);

		/* TEXT POST */
		postText = findViewById(R.id.prev_post_text);
		postTextView = findViewById(R.id.post_text_view);

		/* AUDIO POST */
		postAudio = findViewById(R.id.prev_post_audio);
		playBtnAudio = postAudio.findViewById(R.id.play_btn);
		playBtnAudio.setOnClickListener(this);
		pauseLayoutAudio = postAudio.findViewById(R.id.pause_layout);
		progressViewAudio = postAudio.findViewById(R.id.progress_layout);
		progressMessageAudio = postAudio.findViewById(R.id.progress_message);
		animationView = postAudio.findViewById(R.id.wave3);
		animationView.setComposition(siriComposition);
		animationView.playAnimation();
		animationView.cancelAnimation();
		animationView.setFrame(0);

		/* VIDEO POST */
		postVideo = findViewById(R.id.prev_post_video);
		videoView = postVideo.findViewById(R.id.video_view);
		thumbnailView = postVideo.findViewById(R.id.video_view_thumbnail);
		thumbnailLayout = postVideo.findViewById(R.id.video_view_thumbnail_layout);
		playBtnVideo = postVideo.findViewById(R.id.play_btn);
		playBtnVideo.setOnClickListener(this);
		pauseLayoutVideo = postVideo.findViewById(R.id.pause_layout);
		iconPlaceholder = postVideo.findViewById(R.id.icon_placeholder);
		progressViewVideo = postVideo.findViewById(R.id.progress_layout);
		progressMessageVideo = postVideo.findViewById(R.id.progress_message);
	}

	private void setLayout() {
		if (selectedPost != null) {

			MediaHelper.loadProfilePictureWithPlaceholder(this, selectedPost.getAuthorUrl(), profilePic);
			userName.setText(selectedPost.getAuthor());

			postCaption.setText(selectedPost.getCaption());
			ResizingTextWatcher.resizeTextInView(postCaption);

			if (selectedPost.getCreationDate() != null)
				postDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedPost.getCreationDate()));
			else
				postDate.setVisibility(View.GONE);

			// TODO: 3/5/2018    RETURN HERE WHEN READY
			// location temporarily always hidden
			postLocation.setVisibility(View.GONE);


			if (selectedPost.isTextPost())
				handleTextPost();
			else if (selectedPost.isPicturePost())
				handlePicturePost();
			else if (selectedPost.isAudioPost())
				handleAudioPost();
			else if (selectedPost.isVideoPost())
				handleVideoPost();
		}
	}

	private void handlePicturePost() {
		postText.setVisibility(View.GONE);
		postAudio.setVisibility(View.GONE);
		postPicture.setVisibility(View.VISIBLE);
		postVideo.setVisibility(View.GONE);

		postImage.setVisibility(Utils.isStringValid(selectedPost.getContent()) ? View.VISIBLE : View.GONE);
		MediaHelper.loadPictureWithGlide(this, selectedPost.getContent(), postImage);
	}

	private void handleVideoPost() {
		postPreviewCard.setCardBackgroundColor(Utils.getColor(this, R.color.black));

		postText.setVisibility(View.GONE);
		postAudio.setVisibility(View.GONE);
		postPicture.setVisibility(View.GONE);
		postVideo.setVisibility(View.VISIBLE);

		MediaHelper.loadPictureWithGlide(this, selectedPost.getVideoThumbnail(), thumbnailView);

		MediaHelper.playVideoWithResources(videoView, thumbnailLayout, selectedPost.getContent(), playBtnVideo,
				progressViewVideo, progressMessageVideo, false, 0);

		iconPlaceholder.setVisibility(View.GONE);

	}

	private void handleTextPost() {
		postPreviewCard.setCardBackgroundColor(Utils.getColor(this, R.color.black));

		postText.setVisibility(View.VISIBLE);
		postAudio.setVisibility(View.GONE);
		postPicture.setVisibility(View.GONE);
		postVideo.setVisibility(View.GONE);

		// post own dedicated TextView
		postCaption.setVisibility(View.GONE);

		postTextView.setText(selectedPost.getCaption());
		ResizingTextWatcher.resizeTextInView(postTextView);
	}

	private void handleAudioPost() {
		postPreviewCard.setCardBackgroundColor(Utils.getColor(this, R.color.black));

		postText.setVisibility(View.GONE);
		postAudio.setVisibility(View.VISIBLE);
		postPicture.setVisibility(View.GONE);
		postVideo.setVisibility(View.GONE);

		if (mediaPlayer != null) {
			MediaHelper.playAudioWithResources(mediaPlayer, lastPlayerPosition,
					selectedPost.getContent(), playBtnAudio, null, progressViewAudio,
					progressMessageAudio, animationView);
			if (pauseFrame != 0){
				animationView.setFrame(pauseFrame);
				animationView.resumeAnimation();
			}
			else animationView.playAnimation();
		}
	}


	private void returnHome() {
		finish();

//		if (Utils.hasLollipop())
//			supportFinishAfterTransition();
//		else {
//			finish();
//			overridePendingTransition(0, 0);
//		}
	}
}
