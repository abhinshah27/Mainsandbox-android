/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.createPost;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.IOException;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.LottieCompositioListener;
import it.keybiz.lbsapp.corporate.utilities.listeners.TapAndHoldGestureListener;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * This {@link Fragment} subclass handles the creation of an audio file needed to create an audio post.
 * <p>
 * Activities that contain this fragment must implement the
 * {@link OnAudioRecordFragmentInteractionListener} interface
 * to handle interaction events.<p>
 * Use the {@link AudioRecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AudioRecordFragment extends HLFragment implements View.OnClickListener,
		TapAndHoldGestureListener.OnHoldListener {
	public static final String LOG_TAG = AudioRecordFragment.class.getCanonicalName();

	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null;

	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PARAM1 = "param1";
	private static final String ARG_PARAM2 = "param2";

	// TODO: Rename and change types of parameters
	private String mParam1;
	private String mParam2;

	private OnAudioRecordFragmentInteractionListener mListener;

	private String mediaFileUri;

	private View mainLayout;

	private View recordBtn;
	private View buttonsSection;
	private View deleteBtn;
	private View confirmBtn;
	private View playBtn;
	private boolean playing = false;
	private boolean recording = true;
	private boolean deleteOnClose = false;

	private View instructionsString;
	private Chronometer elapsedTime;

	private LottieAnimationView animationView;
	private LottieComposition siriComposition;

	private boolean resetLayout = false;


	public AudioRecordFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param param1 Parameter 1.
	 * @param param2 Parameter 2.
	 * @return A new instance of fragment ProfileFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static AudioRecordFragment newInstance(String param1, String param2) {
		AudioRecordFragment fragment = new AudioRecordFragment();
		Bundle args = new Bundle();
		args.putString(ARG_PARAM1, param1);
		args.putString(ARG_PARAM2, param2);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mParam1 = getArguments().getString(ARG_PARAM1);
			mParam2 = getArguments().getString(ARG_PARAM2);
		}

		setRetainInstance(true);
	}



	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		mainLayout = inflater.inflate(R.layout.cpost_fragment_audio_rec, container, false);

		configureLayout(mainLayout);

		return mainLayout;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnAudioRecordFragmentInteractionListener) {
			mListener = (OnAudioRecordFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnAudioRecordFragmentInteractionListener");
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnAudioRecordFragmentInteractionListener) {
			mListener = (OnAudioRecordFragmentInteractionListener) activity;
		} else {
			throw new RuntimeException(activity.toString()
					+ " must implement OnAudioRecordFragmentInteractionListener");
		}
	}

	@Override
	protected void configureResponseReceiver() {}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getActivity() != null) {
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
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		setLayout(recording, mListener.isEditAudioPost());
	}

	@Override
	public void onPause() {
        stopPlaying();

        super.onPause();
	}

	@Override
	public void onStop() {
		animationView.cancelAnimation();
		animationView.setFrame(0);

		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}

		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}

		super.onStop();
	}

	@Override
	public void onDestroy() {
		String uri = mListener.getAudioMediaFileUri();
		MediaHelper.deleteMediaFile(uri, deleteOnClose);

		super.onDestroy();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
				resetUI(false);
				animationView.cancelAnimation();
				animationView.setFrame(0);
				mListener.exitFromRecordingAndSetAudioBackground();
				break;
		}
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
		setLayout(resetLayout, mListener.isEditAudioPost());
		resetLayout = false;
	}

	@Override
	public void onSlideEvent(@NotNull MotionEvent motionEvent, boolean movingLeft) {}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 */
	public interface OnAudioRecordFragmentInteractionListener {
		@NonNull String getAudioMediaFileUri();
		void exitFromRecordingAndSetAudioBackground();
		boolean isEditAudioPost();
		@NonNull String getAudioUrl();
	}


	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {
		elapsedTime = view.findViewById(R.id.chronometer);

		buttonsSection = view.findViewById(R.id.buttons_section);

		recordBtn = view.findViewById(R.id.btn_main);
		recordBtn.setOnTouchListener(new TapAndHoldGestureListener(this, null));

		deleteBtn = view.findViewById(R.id.btn_delete);
		deleteBtn.setOnClickListener(this);
		playBtn = view.findViewById(R.id.btn_play);
		playBtn.setOnClickListener(this);
		confirmBtn = view.findViewById(R.id.btn_confirm);
		confirmBtn.setOnClickListener(this);

		animationView = view.findViewById(R.id.wave);

		instructionsString = view.findViewById(R.id.record_instructions);
	}

	@Override
	protected void setLayout() {}

	private void setLayout(boolean isRecording, boolean editAudio) {
		elapsedTime.setVisibility(View.VISIBLE);
		instructionsString.setVisibility(isRecording && !editAudio ? View.VISIBLE : View.GONE);
		recordBtn.setVisibility(isRecording && !editAudio ? View.VISIBLE : View.GONE);
		buttonsSection.setVisibility(isRecording && !editAudio ? View.INVISIBLE : View.VISIBLE);

		if (!Utils.isStringValid(mediaFileUri) && editAudio && mListener != null)
			mediaFileUri = mListener.getAudioMediaFileUri();


		if (animationView != null && siriComposition != null){
			animationView.setComposition(siriComposition);
			animationView.pauseAnimation();
			animationView.cancelAnimation();
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
		try {
			if (mPlayer != null) {
				if (mPlayer.isPlaying())
					mPlayer.pause();
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

		playing = false;

		stopTimer();

		animationView.cancelAnimation();
		animationView.clearAnimation();
		animationView.setFrame(0);
	}

	private void startRecording() {
		mediaFileUri = mListener.getAudioMediaFileUri();
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
		try {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		} catch (Exception e) {
			e.printStackTrace();
			activityListener.showAlert(R.string.error_recording_audio);
			resetLayout = true;

			mRecorder.release();
			mRecorder = null;

			MediaHelper.deleteMediaFile(mediaFileUri, true);
		}

		animationView.clearAnimation();
		animationView.cancelAnimation();
		animationView.setFrame(0);

		stopTimer();
	}

	//endregion

	//endregion

}