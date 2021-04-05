/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.voiceVideoCalls

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.telephony.TelephonyManager
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.twilio.video.*
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.features.HomeActivity
import it.keybiz.lbsapp.corporate.features.splash.SplashActivity
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp.ACTION_MAIN
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp.VoiceVideoCallType
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp.VoiceVideoUsageType
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper
import kotlinx.android.synthetic.main.activity_calls.*
import kotlinx.android.synthetic.main.activity_calls_content_video.*
import kotlinx.android.synthetic.main.luiss_toolbar_home_profile_others.*

/**
 * @author mbaldrighi on 11/10/2018.
 */
class CallsActivityNoService: HLActivity() {

    companion object {
        val LOG_TAG = CallsActivityNoService::class.qualifiedName

        const val SERVICE_NOTIFICATION_ID = 101
        const val ACTION_CALL_REJECT = "reject_call"
        const val ACTION_CALL_CLOSE = "close_call"
        const val ACTION_CALL_ACCEPT = "accept_call"
        const val ACTION_CALL_MUTE = "mute_call"

        var currentCallID: String? = null
    }

    private var callType = VoiceVideoCallType.VOICE
    private var usageType = VoiceVideoUsageType.OUTGOING

    private var participantId: String? = null
    private var roomName: String? = null
    private var showedName: String? = null
    private var showedAvatar: String? = null
    private var showedWall: String? = null

    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    private var room: Room? = null
    private var localParticipant: LocalParticipant? = null

    /*
     * Encoding parameters represent the sender side bandwidth constraints.
     */
    private val encodingParameters = EncodingParameters(0, 0)

    private var localAudioTrack: LocalAudioTrack? = null
    private var localVideoTrack: LocalVideoTrack? = null

    private val cameraCapturerCompat by lazy {
        CameraCapturerCompat(this@CallsActivityNoService, getAvailableCameraSource())
    }
    private val audioManager by lazy {
        this@CallsActivityNoService.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var soundPoolManager: SoundPoolManager? = null

    private var participantIdentity: String? = ""

    private var previousAudioMode = 0
    private var previousMicrophoneMute = false
    private var localVideoView: VideoRenderer? = null
    private var disconnectedFromOnDestroy = false

    private var timeoutTimer: CallTimeoutTimer? = null

    private var isReceiverRegistered: Boolean = false
    private var localBroadcastReceiver: CallActionsReceiver? = null

    private var isAudioEnabled: Boolean? = null
    private var areSpeakersOn: Boolean? = null
    private var isVideoEnabled: Boolean? = null

    private var finishedCalled = false
    private var lostCallNotificationShowed = false

    private var phoneCallReceiver: IncomingPhoneCallReceiver? = null
    private var isPhoneCallReceiverRegistered: Boolean = false
    private var hasIncomingCall: Boolean = false

    private var outsideCallDialog: AlertDialog? = null

    private var hasCutoutAndValue: Pair<Boolean, Int>? = null


    //region == Listeners ==

    /*
     * Room events listener
     */
    private val roomListener = object : Room.Listener {
        override fun onConnected(room: Room) {
            localParticipant = room.localParticipant

            soundPoolManager?.playRinging(this@CallsActivityNoService)
            if (usageType == VoiceVideoUsageType.OUTGOING) {

                callTypeView.setText(R.string.call_ringing)

                // TODO: 11/26/2018    FUTURE: handle views visibility??
//                groupHiddenWhenVideo.baseAnimateAlpha(1000, false)
//
                timeoutTimer = CallTimeoutTimer(30000, 1000)
                timeoutTimer?.start()
            }
            else {
                timeoutTimer?.run {
                    this.cancel()
                    timeoutTimer = null
                }

                setDisconnectAction()
            }

            // Only one participant is supported
            room.remoteParticipants.firstOrNull()?.let { addRemoteParticipant(it) }
        }

        override fun onConnectFailure(room: Room, e: TwilioException) {
            LogUtils.e(CallsActivityNoService.LOG_TAG, e.message, e)

            soundPoolManager?.stopRinging()
            soundPoolManager?.playError()
            configureAudio(false)

            finishCall(delay = 1000, notificationAction = NotificationUtils.CALL_ACTION_CLOSED)
        }

        override fun onDisconnected(room: Room, e: TwilioException?) {
            LogUtils.e(CallsActivityNoService.LOG_TAG, e?.message, e)

            localParticipant = null
            // Only reinitialize the UI if disconnect was not called from onDestroy()
            if (!disconnectedFromOnDestroy) {
                configureAudio(false)

                moveLocalVideoToPrimaryView()
            }
        }

        override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
            callTypeView.setText(R.string.call_connecting)

            soundPoolManager?.stopRinging()
            timeoutTimer?.run {
                this.cancel()
                timeoutTimer = null
            }
            addRemoteParticipant(participant)
        }

        override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
            removeRemoteParticipant(participant)

            finishCall(getString(R.string.call_participant_left, showedName))
        }

        override fun onRecordingStarted(room: Room) {
            /*
             * Indicates when media shared to a Room is being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            LogUtils.d(CallsActivityNoService.LOG_TAG, "onRecordingStarted")
        }

        override fun onRecordingStopped(room: Room) {
            /*
             * Indicates when media shared to a Room is no longer being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            LogUtils.d(CallsActivityNoService.LOG_TAG, "onRecordingStopped")
        }
    }

    /*
     * RemoteParticipant events listener
     */
    private val participantListener = object : RemoteParticipant.Listener {
        override fun onAudioTrackPublished(remoteParticipant: RemoteParticipant,
                                           remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onAudioTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteAudioTrackPublication.trackName}]")

            outsideCallDialog?.dismiss()
        }

        override fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant,
                                             remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onAudioTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteAudioTrackPublication.trackName}]")

            outsideCallDialog = AlertDialog.Builder(this@CallsActivityNoService)
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                    .setCancelable(true)
                    .setMessage(getString(R.string.error_calls_hold, showedName))
                    .create().also { it.show() }
        }

        override fun onDataTrackPublished(remoteParticipant: RemoteParticipant,
                                          remoteDataTrackPublication: RemoteDataTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onDataTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteDataTrackPublication.trackName}]")
        }

        override fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant,
                                            remoteDataTrackPublication: RemoteDataTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onDataTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteDataTrackPublication.trackName}]")
        }

        override fun onVideoTrackPublished(remoteParticipant: RemoteParticipant,
                                           remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onVideoTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteVideoTrackPublication.trackName}]")
        }

        override fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant,
                                             remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onVideoTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteVideoTrackPublication.trackName}]")
        }

        override fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant,
                                            remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                            remoteAudioTrack: RemoteAudioTrack) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onAudioTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                    "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                    "name=${remoteAudioTrack.name}]")
        }

        override fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                              remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                              remoteAudioTrack: RemoteAudioTrack) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onAudioTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                    "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                    "name=${remoteAudioTrack.name}]")
        }

        override fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                    remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                    twilioException: TwilioException) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onAudioTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "name=${remoteAudioTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")
        }

        override fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant,
                                           remoteDataTrackPublication: RemoteDataTrackPublication,
                                           remoteDataTrack: RemoteDataTrack) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onDataTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                    "name=${remoteDataTrack.name}]")
        }

        override fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                             remoteDataTrackPublication: RemoteDataTrackPublication,
                                             remoteDataTrack: RemoteDataTrack) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onDataTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                    "name=${remoteDataTrack.name}]")
        }

        override fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                   remoteDataTrackPublication: RemoteDataTrackPublication,
                                                   twilioException: TwilioException) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onDataTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "name=${remoteDataTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")
        }

        override fun onVideoTrackSubscribed(remoteParticipant: RemoteParticipant,
                                            remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                            remoteVideoTrack: RemoteVideoTrack) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onVideoTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                    "name=${remoteVideoTrack.name}]")
            addRemoteParticipantVideo(remoteVideoTrack)
        }

        override fun onVideoTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                              remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                              remoteVideoTrack: RemoteVideoTrack) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onVideoTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                    "name=${remoteVideoTrack.name}]")
            removeParticipantVideo(remoteVideoTrack)
        }

        override fun onVideoTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                    remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                                    twilioException: TwilioException) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "onVideoTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "name=${remoteVideoTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")

            showAlert(R.string.error_calls_video)
        }

        override fun onAudioTrackEnabled(remoteParticipant: RemoteParticipant,
                                         remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "AUDIO track ENABLED")
            handleMutedConversationView(true)
        }

        override fun onAudioTrackDisabled(remoteParticipant: RemoteParticipant,
                                          remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "AUDIO track DISABLED")
            handleMutedConversationView(false)
        }

        override fun onVideoTrackEnabled(remoteParticipant: RemoteParticipant,
                                         remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "VIDEO track ENABLED")

            // TODO: 11/10/2018    NO HANDLING OF VIDEO VISIBILITY??
//            handleNoVideoView(true)
        }

        override fun onVideoTrackDisabled(remoteParticipant: RemoteParticipant,
                                          remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.i(CallsActivityNoService.LOG_TAG, "VIDEO track DISABLED")

            // TODO: 11/10/2018    NO HANDLING OF VIDEO VISIBILITY??
//            handleNoVideoView(false)
        }
    }


    /*
     * Twilio SDK event listener
     */
    private val sdkListener = object: SDKTokenUtils.SDKTokenListener {
        override fun tokenResponse(token: String?) {
            if (!token.isNullOrBlank() && usageType == VoiceVideoUsageType.OUTGOING)
                connectToRoom()
//            else finishCall()
        }
    }

    //endregion


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calls)
        setRootContent(R.id.rootContent)

        if (OSVersionUtils.hasOSVersion(Build.VERSION_CODES.O_MR1)) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        }
        else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        setImmersiveValue(true)

        if (Utils.hasPie()) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

            rootContent.setOnApplyWindowInsetsListener { _, insets ->
                val cutout = insets.displayCutout
                if (cutout != null)
                    hasCutoutAndValue = (cutout.safeInsetTop > 0) to cutout.safeInsetTop
                else
                    params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER

                insets.consumeDisplayCutout()
            }
        }

        if (OSVersionUtils.hasOSVersion(Build.VERSION_CODES.O)) {
            val kgm = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (kgm.isKeyguardLocked)
                kgm.requestDismissKeyguard(
                        this,
                        object: KeyguardManager.KeyguardDismissCallback() {
                            override fun onDismissSucceeded() {
                                super.onDismissSucceeded()
                                LogUtils.i(LOG_TAG, "Lock dismissed")
                            }
                        }
                )
        }

        manageIntent()

        /*
        * Set local video view to primary view
        */
        localVideoView = primaryVideoView

        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

        /*
         * Needed for being able to use speakers.
         */
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        /*
         * Needed for setting/abandoning audio focus during call
         */
        areSpeakersOn = (callType == VoiceVideoCallType.VIDEO || usageType == VoiceVideoUsageType.INCOMING)
        audioManager.isSpeakerphoneOn = areSpeakersOn!!

        /*
         * Setup the broadcast receiver to be notified of video notification messages
         */
        localBroadcastReceiver = CallActionsReceiver()

        /*
         * Set access token
         */
        setAccessToken()

        handleCallStartup()

        requestPermissionForCameraAndMicrophone()
//        requestPermissionForPhoneState()

        initializeUI()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        with (intent) {
            if (this != null) {
                when (this.action) {
                    ACTION_CALL_ACCEPT -> connectToRoom().also {
                        if (!currentCallID.isNullOrBlank())
                            NotificationUtils.sendCallManageNotification(mUser.userId, NotificationUtils.CALL_ACTION_ANSWERED, currentCallID!!, true, this@CallsActivityNoService)
                    }
                    ACTION_CALL_CLOSE -> finishCall(notificationAction = NotificationUtils.CALL_ACTION_CLOSED)
                    ACTION_CALL_REJECT -> finishCall(notificationAction = NotificationUtils.CALL_ACTION_DECLINED)
                    ACTION_CALL_MUTE -> toggleMute()

                    // it's a MANAGE NOTIFICATION
                    Intent.ACTION_MAIN -> {
                        val callID = intent?.getStringExtra(NotificationUtils.CALL_NOTIFICATION_ID)
                        val action = intent?.getStringExtra(NotificationUtils.CALL_NOTIFICATION_ACTION)

                        if (Utils.areStringsValid(callID, action)) {
                            handleActionFromNotification(action ?: "")
                        }
                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(this, AnalyticsUtils.CALLS)

        registerReceiver()

        val fromBackground = localAudioTrack == null && localVideoTrack == null

        val hasPermissions = PermissionsUtils.checkPermissionForCameraAndMicrophone(this, callType)

        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        localVideoTrack = if (localVideoTrack == null && hasPermissions && callType == VoiceVideoCallType.VIDEO) {
            LocalVideoTrack.create(this,
                    true,
                    cameraCapturerCompat.videoCapturer)
        } else {
            localVideoTrack
        }
        if (localVideoView != null) localVideoTrack?.addRenderer(localVideoView!!)

        localAudioTrack = if (localAudioTrack == null && hasPermissions) {
            LocalAudioTrack.create(this, true)
        } else {
            localAudioTrack
        }

        /*
         * If connected to a Room then share the local video and audio track, m
         */
        localVideoTrack?.let {
            if (isVideoEnabled != null) it.enable(isVideoEnabled!!)
            localParticipant?.publishTrack(it)
        }
        localAudioTrack?.let {
            if (isAudioEnabled != null) it.enable(isAudioEnabled!!)
            localParticipant?.publishTrack(it)
        }

        val pair = audioManager.areThereConnectedDevicesOut()
        val devicePresent = pair.first
        val bluetooth = pair.second
        handleSpeakersToggle(!devicePresent && areSpeakersOn!!)
        if (bluetooth) {
            audioManager.isBluetoothScoOn = true
            audioManager.startBluetoothSco()
        }

        /*
         * Update encoding parameters if they have changed.
         */
        localParticipant?.setEncodingParameters(encodingParameters)

        if (room == null || room!!.state == Room.State.DISCONNECTED) {
            if (
                    (
                            usageType == VoiceVideoUsageType.OUTGOING ||                        // call is OUTGOING
                                    (
                                            room != null &&                                     //
                                                    fromBackground &&                                   // only if user was already connected in INCOMING call
                                                    usageType == VoiceVideoUsageType.INCOMING &&        // and received another call
                                                    localParticipant != null                            //
                                            )
                            )
                    && hasPermissions
            ) {
                connectToRoom()
            }
        }
    }


    override fun onPause() {

        if (finishedCalled) {
            closingAndReleaseOps(!lostCallNotificationShowed)
        }
        else {
            /*
             * If this local video track is being shared in a Room, remove from local
             * participant before releasing the video track. Participants will be notified that
             * the track has been removed.
             */
            localVideoTrack?.let {
                isVideoEnabled = it.isEnabled
                localParticipant?.unpublishTrack(it)
            }

            /*
             * Release the local video track before going in the background. This ensures that the
             * camera can be used by other applications while this app is in the background.
             */
            localVideoTrack?.release()
            localVideoTrack = null


            if (hasIncomingCall) {
                /*
                 * Only if an incoming call is detected.
                 */
                localAudioTrack?.let {
                    isAudioEnabled = it.isEnabled
                    localParticipant?.unpublishTrack(it)
                }
                soundPoolManager?.stopRinging()
                configureAudio(false)

                /*
                 * Release the local audio track before going in the background.
                 */
                localAudioTrack?.release()
                localAudioTrack = null
            }
        }

        super.onPause()
    }

    override fun onDestroy() {

        currentCallID = null
        closingAndReleaseOps(true)

        super.onDestroy()
    }

    override fun onBackPressed() {
        showDialogEndCallOnBack()
    }

    private fun showDialogEndCallOnBack(fromLayoutBtn: Boolean = false) {
        DialogUtils.createDialog(
                this,
                getString(R.string.calls_dialog_end_title),
                getString(R.string.calls_dialog_end_body),
                positiveString = getString(R.string.calls_dialog_end_pos),
                negativeString = getString(R.string.action_cancel),
                positiveClick = DialogInterface.OnClickListener { dialog, _ ->
                    if (fromLayoutBtn) {
                        if (!currentCallID.isNullOrBlank() && !participantId.isNullOrBlank())
                            NotificationUtils.sendCallManageNotification(participantId!!, NotificationUtils.CALL_ACTION_CLOSED, currentCallID!!)

                        this@CallsActivityNoService.startActivity(
                                Intent(this, HomeActivity::class.java)
                                        .apply { this.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE)}
                        )
                        finishCall()
                    }
                    else
                        finishCall(notificationAction = NotificationUtils.CALL_ACTION_CLOSED)
                    dialog.dismiss()
                },
                negativeClick = DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                }
        ).show()
    }

    private fun finishCall(message: String? = null, delay: Long = 750, notificationAction: String? = null) {

        if (!notificationAction.isNullOrBlank() && !currentCallID.isNullOrBlank() && !participantId.isNullOrBlank())
            NotificationUtils.sendCallManageNotification(participantId!!, notificationAction, currentCallID!!)

        currentCallID = null
        finishedCalled = true

        soundPoolManager?.stopRinging()

        callTypeView.text = if (!message.isNullOrBlank()) message else getString(R.string.call_disconnecting)
        if (delay > 0) {
            Handler().postDelayed({ finish()}, delay)
        }
        else finish()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Constants.PERMISSIONS_REQUEST_CAMERA_MIC_CALLS -> {
                var cameraAndMicPermissionGranted = true

                var hasPhonePermission = false
                var wantsPhoneRationale = false

                for (i in 0 until grantResults.size) {
                    val result = grantResults[i]
                    val permission = permissions[i]

                    if (callType == VoiceVideoCallType.VOICE && permission == Manifest.permission.CAMERA)
                        continue
                    else if (permission == Manifest.permission.READ_PHONE_STATE) {
                        if (result == PackageManager.PERMISSION_GRANTED) {
                            hasPhonePermission = true
                            registerPhoneCallReceiver()
                        }
                        else {
                            wantsPhoneRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.READ_PHONE_STATE)
                        }
                        continue
                    }

                    cameraAndMicPermissionGranted = cameraAndMicPermissionGranted and
                            (result == PackageManager.PERMISSION_GRANTED)

                    LogUtils.d(LOG_TAG, "$permission: $result >>> hasPermissions: $cameraAndMicPermissionGranted")
                }

                if (cameraAndMicPermissionGranted) createAudioAndVideoTracks()
                else showDialogForPermissionsRationale(wantsPhoneRationale, !hasPhonePermission)
            }

            Constants.PERMISSIONS_REQUEST_PHONE_STATE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    registerPhoneCallReceiver()
            }
        }
    }


    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        if (intent != null) {
            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_CALL_TYPE))
                callType =  intent.getSerializableExtra(NotificationUtils.CALL_NOTIFICATION_CALL_TYPE) as VoiceVideoCallType
            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_USAGE_TYPE))
                usageType = intent.getSerializableExtra(NotificationUtils.CALL_NOTIFICATION_USAGE_TYPE) as VoiceVideoUsageType
            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_ROOM_NAME))
                roomName = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_ROOM_NAME) ?: ""

            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY))
                participantId = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY) ?: ""
            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_NAME))
                showedName = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_NAME) ?: ""
            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_AVATAR))
                showedAvatar = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_AVATAR) ?: ""
            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_WALL))
                showedWall = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_WALL) ?: ""

            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_ID))
                currentCallID = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_ID)
        }
    }


    private val actionsClickListener = View.OnClickListener {
        when (it.id) {
            R.id.rejectCall -> finishCall(notificationAction = NotificationUtils.CALL_ACTION_DECLINED)
            R.id.closeCall -> finishCall(notificationAction = NotificationUtils.CALL_ACTION_CLOSED)
            R.id.acceptCall -> {
                connectToRoom()
                NotificationUtils.sendCallManageNotification(mUser.userId, NotificationUtils.CALL_ACTION_ANSWERED, currentCallID!!, true, this)
            }
            R.id.actionMuteMic -> toggleMute()
            R.id.actionSpeakers -> handleSpeakersToggle()
            R.id.actionSwitchCamera -> switchCamera()
        }
    }

    private fun initializeUI() {

        (toolbar as View).viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (hasCutoutAndValue?.first == true) {
                    val lp = (this@CallsActivityNoService.toolbar as View).layoutParams as ConstraintLayout.LayoutParams
                    lp.topMargin = hasCutoutAndValue!!.second
                    (this@CallsActivityNoService.toolbar as View).layoutParams = lp
                }

                this@CallsActivityNoService.toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        } )

        userName.text = showedName

        if (callType == VoiceVideoCallType.VIDEO) {
            groupVideo.visibility = View.VISIBLE
            actionSpeakers.visibility = View.GONE

            callTypeView.setText(R.string.call_type_text_video)

            // TODO: 11/26/2018    FUTURE: handle views visibility??
//            primaryVideoView.setOnClickListener {
//                groupHiddenWhenVideo.baseAnimateAlpha(1000, groupHiddenWhenVideo.visibility != 1).setListener(object: Animator.AnimatorListener {
//                    override fun onAnimationEnd(animation: Animator?) {
//                        if (groupHiddenWhenVideo.alpha == 1f)
//                            groupHiddenWhenVideo.baseAnimateAlpha(1000, false, delay = 3000)
//                    }
//
//                    override fun onAnimationRepeat(animation: Animator?) {}
//                    override fun onAnimationCancel(animation: Animator?) {}
//                    override fun onAnimationStart(animation: Animator?) {}
//                })
//            }
        }
        else {
            GlideApp.with(userWallPicture).asDrawable().load(showedWall).into(
                    object: CustomViewTarget<ImageView, Drawable>(userWallPicture as ImageView) {
                        override fun onLoadFailed(errorDrawable: Drawable?) {}

                        override fun onResourceCleared(placeholder: Drawable?) {}

                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            MediaHelper.blurWallPicture(this@CallsActivityNoService, resource, userWallPicture)
                        }
                    }
            )

            MediaHelper.loadProfilePictureWithPlaceholder(this, showedAvatar, userAvatar as ImageView)

            groupVideo.visibility = View.GONE
            actionSwitchCamera.visibility = View.GONE

            callTypeView.setText(R.string.call_type_text_voice)
        }

        with (closeCall) {
            this.setOnClickListener(actionsClickListener)
            this.hide()
        }
        with (rejectCall) {
            this.setOnClickListener(actionsClickListener)
            this.hide()
        }
        with (acceptCall) {
            this.setOnClickListener(actionsClickListener)
            this.hide()
        }

        actionMuteMic.setOnClickListener(actionsClickListener)
        actionSpeakers.setOnClickListener(actionsClickListener)
        actionSwitchCamera.setOnClickListener(actionsClickListener)
        actionSwitchCamera.visibility = if (Utils.hasDeviceCamera(this)) View.VISIBLE else View.GONE

        if (usageType == VoiceVideoUsageType.OUTGOING) {
            closeCall.show()
//            setDisconnectAction()
        }
        else {
            closeCall.hide()
            groupIncomingCall.visibility = View.VISIBLE
            acceptCall.show(); rejectCall.show()
        }

        dots.visibility = View.GONE
        back_arrow.setOnClickListener {
            showDialogEndCallOnBack(true)
        }
    }

    private fun closingAndReleaseOps(cancelNotif:Boolean = true) {

        if (cancelNotif) {
            // cancel notification from tray
            notificationManager.cancel(SERVICE_NOTIFICATION_ID)
        }

        unregisterReceiver()

        disconnectedFromOnDestroy = true

        timeoutTimer?.cancel()
        timeoutTimer = null

        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        room?.disconnect()
        room = null

        configureAudio(false)

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        localAudioTrack?.release(); localAudioTrack = null
        localVideoTrack?.release(); localVideoTrack = null

        soundPoolManager?.release()

    }


    //region == Permissions ==

    private fun requestPermissionForCameraAndMicrophone() {
        val hasPhonePermission =
                if (PermissionsUtils.checkPermissionForPhoneState(this)) {
                    registerPhoneCallReceiver(); true
                }
                else false

        val hasCameraMicPermission =
                if (PermissionsUtils.checkPermissionForCameraAndMicrophone(this, callType)) {
                    createAudioAndVideoTracks(); true
                }
                else false

        val shouldPhoneRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE)

        val condition = if (callType == VoiceVideoCallType.VIDEO)
            (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.RECORD_AUDIO))
        else ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)

        if (hasCameraMicPermission && hasPhonePermission) {
            // all permissions are granted
            return
        } else if (!hasCameraMicPermission) {
            if (condition)
                showDialogForPermissionsRationale(shouldPhoneRationale, !hasPhonePermission)
            else
                askPermissions(!hasPhonePermission)
        } else {
            if (shouldPhoneRationale)
                showDialogForPermissionsRationalePhone()
            else
                askPermissionsForPhone()
        }
    }

    /*
     * Depending on {@link VoiceVideoCallType) users are asked for both audio and video permissions (#VoiceVideoCallType.VIDEO)
     * or only audio (#VoiceVideoCallType.VOICE)
     */
    private fun askPermissions(addPhoneState: Boolean) {

        val permArray = when (callType) {
            VoiceVideoCallType.VIDEO -> {
                if (addPhoneState)
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE)
                else
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            }
            VoiceVideoCallType.VOICE -> {
                if (addPhoneState)
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE)
                else
                    arrayOf(Manifest.permission.RECORD_AUDIO)
            }
        }

        ActivityCompat.requestPermissions(
                this,
                permArray,
                Constants.PERMISSIONS_REQUEST_CAMERA_MIC_CALLS
        )
    }

    private fun askPermissionsForPhone() {
        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                Constants.PERMISSIONS_REQUEST_PHONE_STATE
        )
    }

    private fun showDialogForPermissionsRationale(addPhoneRationale: Boolean, addPhonePermission: Boolean) {
        DialogUtils.createDialog(
                this,
                getString(R.string.calls_permiss_rationale_title),
                getString(
                        if (addPhoneRationale)
                            R.string.calls_permiss_rationale_body_w_phonestate
                        else
                            R.string.calls_permiss_rationale_body,
                        getString(R.string.app_name)),
                positiveString = getString(R.string.calls_permiss_rationale_pos),
                negativeString = getString(R.string.calls_permiss_rationale_neg),
                positiveClick = DialogInterface.OnClickListener { dialog, _ ->
                    askPermissions(addPhonePermission)
                    dialog.dismiss()
                },
                negativeClick = DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()

                    finishCall(notificationAction = NotificationUtils.CALL_ACTION_CLOSED)
                }
        ).show()
    }

    private fun showDialogForPermissionsRationalePhone() {
        DialogUtils.createDialog(
                this,
                getString(R.string.calls_permiss_rationale_title),
                getString(R.string.calls_permiss_rationale_phonestate_body, getString(R.string.app_name)),
                positiveString = getString(R.string.calls_permiss_rationale_pos),
                negativeString = getString(R.string.calls_permiss_rationale_neg),
                positiveClick = DialogInterface.OnClickListener { dialog, _ ->
                    askPermissionsForPhone()
                    dialog.dismiss()
                },
                negativeClick = DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                }
        ).show()
    }

    //endregion







    //region == Call methods ==

    private fun setAccessToken() {
        if (SDKTokenUtils.callToken.isNullOrBlank()) {
            val id = mUser.userId
            SDKTokenUtils.init(this, id, sdkListener)
        }
    }

    fun connectToRoom() {
        val token = SDKTokenUtils.callToken
        if (!token.isNullOrBlank()) {

            if (usageType == VoiceVideoUsageType.INCOMING)
                callTypeView.setText(R.string.call_connecting)

            configureAudio(true)
            val connectOptionsBuilder = ConnectOptions.Builder(token)
                    .roomName(roomName)

            /*
             * Add local audio track to connect options to share with participants.
             */
            localAudioTrack?.let { connectOptionsBuilder.audioTracks(listOf(it)) }

            /*
             * Add local video track to connect options to share with participants.
             */
            localVideoTrack?.let { connectOptionsBuilder.videoTracks(listOf(it)) }

            /*
             * Set the preferred audio and video codec for media.
             */
//            connectOptionsBuilder.preferAudioCodecs(listOf(audioCodec))
//            connectOptionsBuilder.preferVideoCodecs(listOf(videoCodec))

            /*
             * Set the sender side encoding parameters.
             */
            connectOptionsBuilder.encodingParameters(encodingParameters)

            room = Video.connect(this, connectOptionsBuilder.build(), roomListener)

            setDisconnectAction()
        }
        else {
            LogUtils.e(CallsActivityNoService.LOG_TAG, "Unexpected call token error")
            showAlert(R.string.error_calls_connection)
        }
    }

    /*
     * Called when participant joins the room
     */
    private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.visibility == View.VISIBLE) {

            LogUtils.e(CallsActivityNoService.LOG_TAG, "Multiple participants are not currently supported in this UI")
            showAlert(R.string.error_calls_connection)

            return
        }

        soundPoolManager?.stopRinging()

        if (callType == VoiceVideoCallType.VOICE && usageType == VoiceVideoUsageType.INCOMING)
            handleSpeakersToggle(false)

        participantIdentity = remoteParticipant.identity

        /*
         * Add participant renderer
         */
        remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
            if (remoteVideoTrackPublication.isTrackSubscribed) {
                remoteVideoTrackPublication.remoteVideoTrack?.let { addRemoteParticipantVideo(it) }
            }
        }

        /*
         * Start listening for participant events
         */
        remoteParticipant.setListener(participantListener)

        callTypeView.postDelayed(
                { this@CallsActivityNoService.callTypeView.setText(R.string.call_connected) },
                750
        )
    }

    /*
     * Called when participant leaves the room
     */
    private fun removeRemoteParticipant(remoteParticipant: RemoteParticipant) {
        if (remoteParticipant.identity != participantIdentity) {
            return
        }

        /*
         * Remove participant renderer
         */
        remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
            if (remoteVideoTrackPublication.isTrackSubscribed) {
                remoteVideoTrackPublication.remoteVideoTrack?.let { removeParticipantVideo(it) }
            }
        }
        moveLocalVideoToPrimaryView()

        LogUtils.e(CallsActivityNoService.LOG_TAG, "Multiple participants are not currently supported in this UI")
    }

    /*
     * The actions performed during disconnect.
     */
    private fun setDisconnectAction() {

        showNotification(NotificationType.ONGOING)

        acceptCall.hide()

        if (!rejectCall.isOrWillBeHidden) {
            rejectCall.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton?) {
                    super.onHidden(fab)

                    groupIncomingCall.visibility = View.GONE

                    if (fab?.id == R.id.rejectCall) closeCall.show()
                }
            })
        }
        else {
            groupIncomingCall.visibility = View.GONE
            closeCall.show()
        }
    }

    //region - AUDIO -

    private fun createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(this, true)

        if (callType == VoiceVideoCallType.VIDEO) {
            // Share your camera
            localVideoTrack = LocalVideoTrack.create(this,
                    true,
                    cameraCapturerCompat.videoCapturer)
        }
    }

    private fun configureAudio(enable: Boolean) {
        with(audioManager) {
            if (enable) {
                previousAudioMode = audioManager.mode
                // Request audio focus before making any device switch
                requestAudioFocus()
                /*
                 * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
                 * to be in this mode when playout and/or recording starts for the best
                 * possible VoIP performance. Some devices have difficulties with
                 * speaker mode if this is not set.
                 */
                mode = AudioManager.MODE_IN_COMMUNICATION
                /*
                 * Always disable microphone mute during a WebRTC call.
                 */
                previousMicrophoneMute = isMicrophoneMute
                isMicrophoneMute = false
            }
            else {
                mode = previousAudioMode
                abandonAudioFocus()
                isMicrophoneMute = previousMicrophoneMute
                isSpeakerphoneOn = true
            }
        }
    }

    private fun handleMutedConversationView(trackEnabled: Boolean) {

        // TODO: 11/10/2018   needed something more than simple TOAST??

        val mutedToast = Toast.makeText(
                this,
                getString(
                        if (trackEnabled) R.string.calls_user_action_unmute else R.string.calls_user_action_mute,
                        Utils.getFirstNameForUI(showedName)
                ),
                Toast.LENGTH_LONG
        )

        mutedToast!!.show()
    }

    fun handleSpeakersToggle(forced: Boolean? = null) {
        val enable = forced ?: !audioManager.isSpeakerphoneOn
        audioManager.isSpeakerphoneOn = enable
        areSpeakersOn = enable

        actionSpeakers.isSelected = enable
    }

    private fun toggleMute() {
        /*
         * Enable/disable the local audio track. The results of this operation are
         * signaled to other Participants in the same Room. When an audio track is
         * disabled, the audio is muted.
         */
        localAudioTrack?.let {
            val enable = !it.isEnabled
            it.enable(enable)

            actionMuteMic.isSelected = !enable      // icon must be selected when microphone is DISABLED
        }
    }

    //endregion

    //region - VIDEO -

    private fun moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.visibility == View.GONE) {
            thumbnailVideoView.visibility = View.VISIBLE

            with(localVideoTrack) {
                this?.removeRenderer(primaryVideoView)
                this?.addRenderer(thumbnailVideoView)
            }

            localVideoView = thumbnailVideoView
            thumbnailVideoView.mirror = cameraCapturerCompat.cameraSource ==
                    CameraCapturer.CameraSource.FRONT_CAMERA
        }
    }

    private fun moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView.visibility == View.VISIBLE) {
            thumbnailVideoView.visibility = View.GONE

            with(localVideoTrack) {
                this?.removeRenderer(thumbnailVideoView)
                this?.addRenderer(primaryVideoView)
            }

            localVideoView = primaryVideoView
            primaryVideoView.mirror = cameraCapturerCompat.cameraSource ==
                    CameraCapturer.CameraSource.FRONT_CAMERA
        }
    }

    /*
     * Set primary view as renderer for participant video track
     */
    private fun addRemoteParticipantVideo(videoTrack: VideoTrack) {
        moveLocalVideoToThumbnailView()
        primaryVideoView.mirror = false
        videoTrack.addRenderer(primaryVideoView)
    }

    private fun removeParticipantVideo(videoTrack: VideoTrack) {
        videoTrack.removeRenderer(primaryVideoView)
    }


    private fun getAvailableCameraSource(): CameraCapturer.CameraSource {
        return if (CameraCapturer.isSourceAvailable(CameraCapturer.CameraSource.FRONT_CAMERA))
            CameraCapturer.CameraSource.FRONT_CAMERA
        else
            CameraCapturer.CameraSource.BACK_CAMERA
    }

    private var noVideoToast: Toast? = null
    private fun handleVideoView(videoEnabled: Boolean) {
        if (noVideoToast == null)
            noVideoToast = Toast.makeText(this, getString(R.string.calls_user_action_video, Utils.getFirstNameForUI(showedName)), Toast.LENGTH_LONG)

        if (videoEnabled) {
            noVideoToast!!.cancel()
            noVideoToast = null
        }
        else if (!finishedCalled) noVideoToast!!.show()

        groupVideo.visibility = if (videoEnabled) View.VISIBLE else View.GONE
    }

    private fun switchCamera() {
        val cameraSource = cameraCapturerCompat.cameraSource
        cameraCapturerCompat.switchCamera()
        if (thumbnailVideoView.visibility == View.VISIBLE) {
            thumbnailVideoView.mirror = cameraSource == CameraCapturer.CameraSource.BACK_CAMERA
        } else {
            primaryVideoView.mirror = cameraSource == CameraCapturer.CameraSource.BACK_CAMERA
        }
    }

    //endregion

    //endregion


    //region == Audio Manager section ==

    private var focusRequest: AudioFocusRequest? = null
    private fun AudioManager.requestAudioFocus() {
        if (OSVersionUtils.hasOSVersion(Build.VERSION_CODES.O)) {
            val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { }
                    .build()
            if (focusRequest != null)
                this.requestAudioFocus(focusRequest!!)
        } else {
            this.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun AudioManager.abandonAudioFocus() {
        if (OSVersionUtils.hasOSVersion(Build.VERSION_CODES.O)) {
            if (focusRequest != null)
                abandonAudioFocusRequest(focusRequest!!)
        }
        else
            abandonAudioFocus(null)
    }

    private fun AudioManager.areThereConnectedDevicesOut(): Pair<Boolean, Boolean> {
        var res = false
        var deviceInfo = false

        if (OSVersionUtils.hasOSVersion(Build.VERSION_CODES.M)) {
            val arr = this.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (it in arr) {
                if (it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    res = true
                    deviceInfo = false
                    break
                }
                else if (it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    res = true
                    deviceInfo = true
                    break
                }
            }
        }
        else {
            res = this.isWiredHeadsetOn || this.isBluetoothA2dpOn || this.isBluetoothScoOn
            deviceInfo = this.isBluetoothScoOn || this.isBluetoothA2dpOn
        }

        return res to deviceInfo
    }

    //endregion


    //region == Handles notification ==

    private fun handleCallStartup() {
        showNotification(NotificationType.STARTUP)

        if (soundPoolManager == null)
            soundPoolManager = SoundPoolManager.getInstance(this@CallsActivityNoService, usageType)

        /*
         * Only handle the notification if not already connected to a Video Room
         */
        LogUtils.d(LOG_TAG, "Call: $callType")
        if (room == null && usageType == VoiceVideoUsageType.INCOMING)
            soundPoolManager?.playRinging(this)
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()

            // add all the needed IntentFilters
            intentFilter.addAction(
                    if (OSVersionUtils.hasOSVersion(Build.VERSION_CODES.LOLLIPOP))
                        AudioManager.ACTION_HEADSET_PLUG
                    else
                        Intent.ACTION_HEADSET_PLUG
            )
            intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)

            registerReceiver(localBroadcastReceiver!!, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun registerPhoneCallReceiver() {
        if (!isPhoneCallReceiverRegistered) {
            if (phoneCallReceiver == null)
                phoneCallReceiver = IncomingPhoneCallReceiver()
            registerReceiver(phoneCallReceiver!!, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
            isPhoneCallReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        try {
            unregisterReceiver(localBroadcastReceiver!!)
            isReceiverRegistered = false

            unregisterReceiver(phoneCallReceiver)
            isPhoneCallReceiverRegistered = false
        } catch (e: IllegalArgumentException) {
            LogUtils.e(CallsActivityNoService.LOG_TAG, e.message, e)
        }
    }

    private enum class NotificationType { STARTUP, ONGOING, MISSED_CALL }
    private fun showNotification(type: NotificationType) {
        val notificationIntent = Intent(this, CallsActivityNoService::class.java)
        notificationIntent.action = ACTION_MAIN
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val closeIntent = Intent(this, CallsActivityNoService::class.java)
        closeIntent.action = ACTION_CALL_CLOSE
        val closePending = PendingIntent.getActivity(this, 0,
                closeIntent, 0)

        val notification: Notification = when (type) {
            NotificationType.STARTUP -> {

                // initialize pending intents
                val rejectIntent = Intent(this, CallsActivityNoService::class.java)
                rejectIntent.action = ACTION_CALL_REJECT
                val rejectPending = PendingIntent.getActivity(this, 0,
                        rejectIntent, 0)

                val acceptIntent = Intent(this, CallsActivityNoService::class.java)
                acceptIntent.action = ACTION_CALL_ACCEPT
                val acceptPending = PendingIntent.getActivity(this, 0,
                        acceptIntent, 0)

                val builder = NotificationUtils.getBaseBuilder(this, pendingIntent, callType, usageType)
                with(builder) {
                    if (usageType == VoiceVideoUsageType.OUTGOING) {
                        this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_hangup), closePending).build())
                    } else {
                        this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_accept), acceptPending).build())
                        this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_reject), rejectPending).build())
                    }
                }
                builder.build()
            }

            NotificationType.ONGOING -> {
//                val muteIntent = Intent(this, CallsActivityNoService::class.java)
//                muteIntent.action = ACTION_CALL_MUTE
//                val mutePending = PendingIntent.getService(this, 0, muteIntent, 0)

                val builder = NotificationUtils.getBaseBuilder(this, pendingIntent, callType, usageType)
                with(builder) {
                    this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_hangup), closePending).build())
//                    this.addAction(NotificationCompat.Action.Builder(0, "Mute", mutePending).build())
                }
                builder.build()
            }

            NotificationType.MISSED_CALL -> {
                val appIntent = Intent(this, if (LBSLinkApp.isForeground) HomeActivity::class.java else SplashActivity::class.java)
                appIntent.action = Intent.ACTION_MAIN
                val appPending = PendingIntent.getService(this, 0, appIntent, 0)

                val builder = NotificationUtils.getBaseBuilder(
                        this,
                        appPending,
                        callType,
                        usageType,
                        ongoing = false,
                        body = getString(R.string.calls_missed_from, showedName)
                )
                // TODO: 11/26/2018    place for future actions like CALL BACK
//                with(builder) {
//                    this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_hangup), closePending).build())
////                    this.addAction(NotificationCompat.Action.Builder(0, "Mute", mutePending).build())
//                }

                builder.setAutoCancel(true).build().also { lostCallNotificationShowed = true }
            }
        }

        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun handleActionFromNotification(action: String) {

        when (action) {
            NotificationUtils.CALL_ACTION_DECLINED -> finishCall(getString(R.string.call_declined))

            NotificationUtils.CALL_ACTION_ANSWERED -> finishCall()
            NotificationUtils.CALL_ACTION_CLOSED -> {
                showNotification(NotificationType.MISSED_CALL)
                finishCall()
            }

            else -> {
                //no action
            }
        }

    }

    //endregion


    private inner class CallTimeoutTimer(millis: Long, interval: Long): CountDownTimer(millis, interval) {

        override fun onFinish() {
            finishCall(delay = 500, notificationAction = NotificationUtils.CALL_ACTION_CLOSED)
        }

        override fun onTick(millisUntilFinished: Long) {

            val roundedMillis = Math.round(millisUntilFinished.toDouble() / 1000) * 1000
            when (roundedMillis) {
                3000L, 2000L, 1000L -> {
                    if (roundedMillis == 3000L)
                        callTypeView.setText(R.string.calls_timer_disconnecting_3)
                    else
                        callTypeView.text = getString(R.string.calls_timer_disconnecting_num, roundedMillis/1000)
                }
            }
        }
    }

    private inner class CallActionsReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                // all the cases
                AudioManager.ACTION_HEADSET_PLUG -> {
                    if (!isInitialStickyBroadcast) {
                        val state = intent.extras?.getInt("state") ?: 0
//                        val mic = intent.extras?.getInt("microphone") ?: 0
                        handleSpeakersToggle(state != 1)
                    }
                }


                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.extras?.getInt(BluetoothProfile.EXTRA_STATE)
                    when(state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            handleSpeakersToggle(false)
                            audioManager.isBluetoothScoOn = true
                            audioManager.startBluetoothSco()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            handleSpeakersToggle(true)
                            audioManager.isBluetoothScoOn = false
                            audioManager.stopBluetoothSco()
                        }
                        else -> {}
                    }
                }
                else -> {
                    if (intent != null)
                        handleCallStartup()
                }
            }
        }
    }


    private inner class IncomingPhoneCallReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)
            val string = when (action) {

                TelephonyManager.EXTRA_STATE_RINGING -> {
                    hasIncomingCall = true
                    "RINGING"
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    "OFF HOOK"
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    hasIncomingCall = false
                    "IDLE"
                }
                else -> ""
            }

            LogUtils.d(LOG_TAG, "Incoming phone call: $string")
        }
    }


}