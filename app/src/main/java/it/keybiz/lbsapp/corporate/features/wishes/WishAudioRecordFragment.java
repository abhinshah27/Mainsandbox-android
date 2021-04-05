/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.LottieCompositioListener;
import it.keybiz.lbsapp.corporate.utilities.listeners.TapAndHoldGestureListener;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * This {@link Fragment} subclass handles the creation of an audio file needed to create an audio post.
 * <p>
 * Use the {@link WishAudioRecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WishAudioRecordFragment extends HLFragment implements View.OnClickListener, OnMissingConnectionListener,
		TapAndHoldGestureListener.OnHoldListener, MediaHelper.MediaUploadListenerWithCallback.OnUploadCompleteListener,
		WishesActivity.OnNextClickListener {

	public static final String LOG_TAG = WishAudioRecordFragment.class.getCanonicalName();

	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null;

	private boolean isEditAudio = false;

	private String mediaFileUri;

	private View recordBtn;
	private View buttonsSection;
	private boolean playing = false;
	private boolean recording = true;
	private boolean deleteOnClose = false;

	private View instructionsString;
	private Chronometer elapsedTime;

	private LottieAnimationView animationView;
	private LottieComposition siriComposition;

	private File audioFile;
	private Bundle dataToSent;

	private WishListElement selectedElement;


	public WishAudioRecordFragment() {
		// Required empty public constructor
	}


	public static WishAudioRecordFragment newInstance(boolean editAudio) {
		WishAudioRecordFragment fragment = new WishAudioRecordFragment();
		Bundle args = new Bundle();
		args.putBoolean(Constants.EXTRA_PARAM_1, editAudio);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}



	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.cpost_fragment_audio_rec, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
				if (json != null && json.length() == 1) {
					selectedElement = new WishListElement().deserializeToClass(json.optJSONObject(0));
				}

				wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
				wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {
		activityListener.closeProgress();
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getActivity() instanceof HLActivity) {
			siriComposition = LBSLinkApp.siriComposition;
			if (siriComposition == null) {
				LBSLinkApp.initLottieAnimation(getActivity(), new LottieCompositioListener() {
					@Override
					public void onResult(@org.jetbrains.annotations.Nullable LottieComposition result) {
						super.onResult(result);

						siriComposition = result;

						if (animationView != null && siriComposition != null)
							animationView.setComposition(siriComposition);
					}
				});

//				new Handler().post(new Runnable() {
//					@Override
//					public void run() {
//						LottieComposition.Factory.fromAssetFileName(getActivity(), "siri.json",
//								new OnCompositionLoadedListener() {
//									@Override
//									public void onCompositionLoaded(@Nullable LottieComposition composition) {
//										siriComposition = composition;
//
//										if (animationView != null && siriComposition != null)
//											animationView.setComposition(siriComposition);
//									}
//								});
//					}
//				});
			}

			mediaFileUri = MediaHelper.checkPermissionForAudio((HLActivity) getActivity(), HLMediaType.AUDIO, this);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_AUDIO_REC);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		setLayout(recording, isEditAudio);
	}

	@Override
	public void onPause() {
		super.onPause();

		animationView.clearAnimation();
		animationView.cancelAnimation();
		animationView.setFrame(0);
	}

	@Override
	public void onStop() {
		animationView.cancelAnimation();
		animationView.setFrame(0);

		super.onStop();

		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}

		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	@Override
	public void onDestroy() {
		if (Utils.isStringValid(mediaFileUri)) {
			File f = new File(Uri.parse(mediaFileUri).getPath());
			boolean deleteAnyway =  f.length() == 0 || f.length() < 1000;
			if (deleteOnClose || deleteAnyway) {
				if (f.exists()) {
					if (f.delete())
						LogUtils.d(LOG_TAG, "Audio file deleted successfully");
					else
						LogUtils.e(LOG_TAG, "COULDN'T DELETE audio file");
				}
			}
		}

		super.onDestroy();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				isEditAudio = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_1);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (!Utils.isContextValid(getActivity()) || !(getActivity() instanceof HLActivity))
			return;

		HLActivity activity = ((HLActivity) getActivity());

		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO:
				if (grantResults.length > 0) {
					if (grantResults.length == 3 &&
							grantResults[0] == PackageManager.PERMISSION_GRANTED &&
							grantResults[1] == PackageManager.PERMISSION_GRANTED &&
							grantResults[2] == PackageManager.PERMISSION_GRANTED) {
						mediaFileUri = MediaHelper.checkPermissionForAudio(activity, HLMediaType.AUDIO, this);
					} else if (grantResults.length == 2 &&
							grantResults[0] == PackageManager.PERMISSION_GRANTED &&
							grantResults[1] == PackageManager.PERMISSION_GRANTED) {
						mediaFileUri = MediaHelper.checkPermissionForAudio(activity, HLMediaType.AUDIO, this);
					} else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
						mediaFileUri = MediaHelper.checkPermissionForAudio(activity, HLMediaType.AUDIO, this);
				}
				break;
		}
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();

		switch (id) {
			case R.id.btn_play:
				onPlay(!playing);
				break;

			case R.id.btn_delete:
				resetUI(true);
				stopPlaying();
				animationView.cancelAnimation();
				animationView.clearAnimation();
				animationView.setFrame(0);
				break;

			case R.id.btn_confirm:
				if (audioFile != null && audioFile.exists()) {
					attemptFileUpload(audioFile);

					resetUI(false);
					animationView.cancelAnimation();
					animationView.setFrame(0);
				}
				break;
		}
	}

	@Override
	public void onNextClick() {
		wishesActivityListener.setSelectedWishListElement(selectedElement);
		wishesActivityListener.resumeNextNavigation();
	}

	private void resetUI(boolean delete) {
		if (delete) {
			Utils.showTopToast(getContext(), R.string.audio_file_deleted);
			deleteOnClose = true;
		}

		elapsedTime.setBase(SystemClock.elapsedRealtime());

		recording = true;
		setLayout(true, false);
	}

	/*
	 * Tap and Hold gesture
	 */
	@Override
	public void onHoldActivated() {
		instructionsString.setVisibility(View.GONE);
		onRecord(true);
	}

	@Override
	public void onHoldReleased() {
		recording = false;
		onRecord(false);
		setLayout(false, isEditAudio);

		audioFile = null;
		if (Utils.isStringValid(mediaFileUri)) {
			Uri uri = Uri.parse(mediaFileUri);
			if (uri != null) {
				audioFile = new File(uri.getPath());
				wishesActivityListener.enableDisableNextButton(audioFile.exists());

				if (!audioFile.exists())
					activityListener.showAlert(R.string.error_recording_audio);
			}
		}

	}

	@Override
	public void onSlideEvent(@NotNull MotionEvent motionEvent, boolean movingLeft) { }


	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {
		elapsedTime = view.findViewById(R.id.chronometer);

		buttonsSection = view.findViewById(R.id.buttons_section);

		recordBtn = view.findViewById(R.id.btn_main);
		recordBtn.setOnTouchListener(new TapAndHoldGestureListener(this, null));

		View deleteBtn = view.findViewById(R.id.btn_delete);
		deleteBtn.setOnClickListener(this);
		View playBtn = view.findViewById(R.id.btn_play);
		playBtn.setOnClickListener(this);
		View confirmBtn = view.findViewById(R.id.btn_confirm);
		confirmBtn.setOnClickListener(this);

		animationView = view.findViewById(R.id.wave);
		animationView.cancelAnimation();
		animationView.clearAnimation();
		animationView.setFrame(0);

		instructionsString = view.findViewById(R.id.record_instructions);
	}

	@Override
	protected void setLayout() {}

	private void setLayout(boolean isRecording, boolean editAudio) {

		elapsedTime.setVisibility(View.VISIBLE);
		instructionsString.setVisibility(isRecording && !editAudio ? View.VISIBLE : View.GONE);
		recordBtn.setVisibility(isRecording && !editAudio ? View.VISIBLE : View.GONE);
		buttonsSection.setVisibility(isRecording && !editAudio ? View.INVISIBLE : View.VISIBLE);

//		if (!Utils.isStringValid(mediaFileUri) && editAudio && mListener != null)
//			mediaFileUri = mListener.getAudioMediaFileUri();


		if (animationView != null && siriComposition != null){
			animationView.setComposition(siriComposition);
			animationView.setFrame(0);
		}
	}


	//region - Recorder/Player methods -

	private void onRecord(boolean start) {
		if (start) {
			startRecording();
		} else {
			stopRecording();
		}
	}

	private void onPlay(boolean start) {
		if (start) {
			startPlaying();
		} else {
			stopPlaying();
		}
	}

	private void startPlaying() {
		if (Utils.isStringValid(mediaFileUri)) {
			animationView.playAnimation();
			mPlayer = new MediaPlayer();
			try {
				mPlayer.setDataSource(mediaFileUri);
				mPlayer.prepare();
				mPlayer.start();
				mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mediaPlayer) {
						playing = false;
						onPlay(false);
					}
				});
			} catch (IOException e) {
				LogUtils.e(LOG_TAG, "prepare() failed with message: " + e.getMessage(), e);
			}

			playing = true;
			resetTimerAndStart();

			return;
		}

		if (activityListener != null)
			activityListener.showGenericError();
	}

	private void stopPlaying() {
		if (mPlayer != null)
			mPlayer.release();
		mPlayer = null;

		playing = false;

		stopTimer();

		animationView.cancelAnimation();
		animationView.clearAnimation();
		animationView.setFrame(0);
	}

	private void startRecording() {
		if (Utils.isStringValid(mediaFileUri)) {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
			mRecorder.setOutputFile((Uri.parse(mediaFileUri).getPath()));

			animationView.playAnimation();

			try {
				mRecorder.prepare();
				mRecorder.start();

				Utils.showTopToast(getContext(), R.string.recording_audio);
				deleteOnClose = false;

				resetTimerAndStart();
			} catch (IOException e) {
				LogUtils.e(LOG_TAG, "prepare() failed with message: " + e.getMessage(), e);
			}

			return;
		}

		if (activityListener != null)
			activityListener.showGenericError();
	}

	private void resetTimerAndStart() {
		if (elapsedTime != null) {
			elapsedTime.start();
			elapsedTime.setBase(SystemClock.elapsedRealtime());
		}
	}

	private void stopTimer() {
		if (elapsedTime != null)
			elapsedTime.stop();
	}

	private void stopRecording() {
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}

		animationView.clearAnimation();
		animationView.cancelAnimation();
		animationView.setFrame(0);

		stopTimer();
	}

	//endregion



	@Override
	public void onUploadComplete(String path, String mediaLink) {
		if (Utils.isStringValid(mediaLink) && Utils.isContextValid(getActivity())) {
			Bundle bundle = new Bundle();
			bundle.putString("audioLink", mediaLink);
			wishesActivityListener.setDataBundle(bundle);

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					wishesActivityListener.resumeNextNavigation();
				}
			});
		}
	}


	private void attemptFileUpload(final File file) {
		if (file != null && file.exists() && getActivity() instanceof HLActivity) {
			((HLActivity) getActivity()).openProgress();
			((HLActivity) getActivity()).setShowProgressAnyway(true);

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					((HLActivity) getActivity()).setProgressMessage(getString(R.string.uploading_media));
				}
			}, 500);

			final MediaHelper.UploadService service = new MediaHelper.UploadService(((HLActivity) getActivity()));
			try {
				MediaHelper.MediaUploadListener listener = new MediaHelper.MediaUploadListenerWithCallback(((HLActivity) getActivity()),
						file.getAbsolutePath(), this);

				service.uploadMedia(getActivity(), file, mUser.getUserId(), listener);
			}
			catch (IllegalArgumentException e) {
				LogUtils.e(LOG_TAG, e.getMessage(), e);
				((HLActivity) getActivity()).showAlertWithRetry(R.string.error_upload_media, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((HLActivity) getActivity()).openProgress();
						try {
							MediaHelper.MediaUploadListener listener =
									new MediaHelper.MediaUploadListenerWithCallback(((HLActivity) getActivity()),
											file.getAbsolutePath(), WishAudioRecordFragment.this);
							service.uploadMedia(getActivity(), file, mUser.getUserId(), listener);
						} catch (IllegalArgumentException e1) {
							LogUtils.e(LOG_TAG, e.getMessage() + "\n\n2nd time in a row!", e);
							((HLActivity) getActivity()).showAlert(R.string.error_upload_media);
						}
					}
				});

				if (getActivity() instanceof HLActivity)
					((HLActivity) getActivity()).closeProgress();
			}
		}
	}



	//endregion

}