/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Message
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.twilio.video.*
import io.realm.Realm
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.*
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import java.lang.ref.WeakReference


const val HANDLER_ACTION_FINISH = 0
const val HANDLER_ACTION_ERROR = 1
const val HANDLER_ACTION_UI_HIDE_REJECT = 2
const val HANDLER_ACTION_UI_INITIALIZE = 3
const val HANDLER_ACTION_UI_SET_DISCONNECT = 4
const val HANDLER_ACTION_UI_PERMISSIONS_DIALOG = 5
const val HANDLER_ACTION_UI_MICROPHONE = 6
const val HANDLER_ACTION_UI_VIDEO_VISIBILITY = 7

enum class VoiceVideoCallType { VOICE, VIDEO }
enum class VoiceVideoUsageType { INCOMING, OUTGOING }

/**
 * @author mbaldrighi on 11/7/2018.
 */
class VoiceVideoCallsHelper(private val context: Context,
                            private var activityHandler: Handler?,
                            private val serviceHandler: Handler,
                            activity: Activity?,
                            private var primaryVideoView: WeakReference<VideoView?>,
                            private var thumbnailVideoView: WeakReference<VideoView?>,
                            private val roomName: String,
                            private val callType: VoiceVideoCallType = VoiceVideoCallType.VOICE,
                            private val usageType: VoiceVideoUsageType = VoiceVideoUsageType.OUTGOING) {

    companion object {
        const val TAG = "VoiceVideoCallsHelper"
    }

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

    private val cameraCapturerCompat = CameraCapturerCompat(context, getAvailableCameraSource())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val soundPoolManager: SoundPoolManager = SoundPoolManager.getInstance(context, usageType)

    private var participantIdentity: String? = ""

    private var previousAudioMode = 0
    private var previousMicrophoneMute = false
    private var localVideoView: VideoRenderer? = primaryVideoView.get() as VideoRenderer
    private var disconnectedFromOnDestroy = false

    private var timeoutTimer: CallTimeoutTimer? = null

    private var isReceiverRegistered: Boolean = false
    private var localBroadcastReceiver: CallActionsReceiver? = null

    private var canConnectToRoom: Boolean = false
    private var isAudioEnabled: Boolean? = null
    private var areSpeakersOn: Boolean? = null
    private var isVideoEnabled: Boolean? = null

    private var finishCalled = false


    //region == Listeners ==

    /*
     * Room events listener
     */
    private val roomListener = object : Room.Listener {
        override fun onConnected(room: Room) {
            localParticipant = room.localParticipant

            soundPoolManager.playRinging(context)
            if (usageType == VoiceVideoUsageType.OUTGOING) {

                // TODO    restore TIMER
//                timeoutTimer = CallTimeoutTimer(30000, 1000)
//                timeoutTimer?.start()
            }
            else {
                timeoutTimer?.run {
                    this.cancel()
                    timeoutTimer = null
                }

                if (activityHandler != null) {
                    activityHandler!!.sendEmptyMessage(HANDLER_ACTION_UI_HIDE_REJECT)
                }
            }

            // Only one participant is supported
            room.remoteParticipants.firstOrNull()?.let { addRemoteParticipant(it) }
        }

        override fun onConnectFailure(room: Room, e: TwilioException) {
            LogUtils.e(TAG, e.message, e)

            soundPoolManager.stopRinging()
            configureAudio(false)


            // TODO    REF UI
//            initializeUI()

            soundPoolManager.playError()
            finish(true)
        }

        override fun onDisconnected(room: Room, e: TwilioException?) {
            LogUtils.e(TAG, e?.message, e)

            localParticipant = null
            this@VoiceVideoCallsHelper.room = null
            // Only reinitialize the UI if disconnect was not called from onDestroy()
            if (!disconnectedFromOnDestroy) {
                configureAudio(false)

                initializeUI()

                moveLocalVideoToPrimaryView()

                finish()
            }
        }

        override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
            soundPoolManager.stopRinging()
            timeoutTimer?.run {
                this.cancel()
                timeoutTimer = null
            }
            addRemoteParticipant(participant)
        }

        override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
            removeRemoteParticipant(participant)

            finish()
        }

        override fun onRecordingStarted(room: Room) {
            /*
             * Indicates when media shared to a Room is being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            LogUtils.d(TAG, "onRecordingStarted")
        }

        override fun onRecordingStopped(room: Room) {
            /*
             * Indicates when media shared to a Room is no longer being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            LogUtils.d(TAG, "onRecordingStopped")
        }
    }

    /*
     * RemoteParticipant events listener
     */
    private val participantListener = object : RemoteParticipant.Listener {
        override fun onAudioTrackPublished(remoteParticipant: RemoteParticipant,
                                           remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.i(TAG, "onAudioTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteAudioTrackPublication.trackName}]")
        }

        override fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant,
                                             remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.i(TAG, "onAudioTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteAudioTrackPublication.trackName}]")
        }

        override fun onDataTrackPublished(remoteParticipant: RemoteParticipant,
                                          remoteDataTrackPublication: RemoteDataTrackPublication) {
            LogUtils.i(TAG, "onDataTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteDataTrackPublication.trackName}]")
        }

        override fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant,
                                            remoteDataTrackPublication: RemoteDataTrackPublication) {
            LogUtils.i(TAG, "onDataTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteDataTrackPublication.trackName}]")
        }

        override fun onVideoTrackPublished(remoteParticipant: RemoteParticipant,
                                           remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.i(TAG, "onVideoTrackPublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteVideoTrackPublication.trackName}]")
        }

        override fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant,
                                             remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.i(TAG, "onVideoTrackUnpublished: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                    "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                    "name=${remoteVideoTrackPublication.trackName}]")
        }

        override fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant,
                                            remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                            remoteAudioTrack: RemoteAudioTrack) {
            LogUtils.i(TAG, "onAudioTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                    "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                    "name=${remoteAudioTrack.name}]")
        }

        override fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                              remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                              remoteAudioTrack: RemoteAudioTrack) {
            LogUtils.i(TAG, "onAudioTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                    "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                    "name=${remoteAudioTrack.name}]")
        }

        override fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                    remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                    twilioException: TwilioException) {
            LogUtils.i(TAG, "onAudioTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                    "name=${remoteAudioTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")
        }

        override fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant,
                                           remoteDataTrackPublication: RemoteDataTrackPublication,
                                           remoteDataTrack: RemoteDataTrack) {
            LogUtils.i(TAG, "onDataTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                    "name=${remoteDataTrack.name}]")
        }

        override fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                             remoteDataTrackPublication: RemoteDataTrackPublication,
                                             remoteDataTrack: RemoteDataTrack) {
            LogUtils.i(TAG, "onDataTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                    "name=${remoteDataTrack.name}]")
        }

        override fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                   remoteDataTrackPublication: RemoteDataTrackPublication,
                                                   twilioException: TwilioException) {
            LogUtils.i(TAG, "onDataTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                    "name=${remoteDataTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")
        }

        override fun onVideoTrackSubscribed(remoteParticipant: RemoteParticipant,
                                            remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                            remoteVideoTrack: RemoteVideoTrack) {
            LogUtils.i(TAG, "onVideoTrackSubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                    "name=${remoteVideoTrack.name}]")
            addRemoteParticipantVideo(remoteVideoTrack)

            activityHandler?.sendMessage(Message.obtain().apply {
                this.what = HANDLER_ACTION_UI_VIDEO_VISIBILITY
                this.obj = true
            })
        }

        override fun onVideoTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                              remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                              remoteVideoTrack: RemoteVideoTrack) {
            LogUtils.i(TAG, "onVideoTrackUnsubscribed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                    "name=${remoteVideoTrack.name}]")
            removeParticipantVideo(remoteVideoTrack)

            activityHandler?.sendMessage(Message.obtain().apply {
                this.what = HANDLER_ACTION_UI_VIDEO_VISIBILITY
                this.obj = false
            })
        }

        override fun onVideoTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                    remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                                    twilioException: TwilioException) {
            LogUtils.i(TAG, "onVideoTrackSubscriptionFailed: " +
                    "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                    "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                    "name=${remoteVideoTrackPublication.trackName}]" +
                    "[TwilioException: code=${twilioException.code}, " +
                    "message=${twilioException.message}]")


            activityHandler?.sendMessage(
                    Message.obtain().apply {
                        this.what = HANDLER_ACTION_ERROR
                        this.obj = context.getString(R.string.error_calls_video)
                    }
            )
        }

        override fun onAudioTrackEnabled(remoteParticipant: RemoteParticipant,
                                         remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.i(TAG, "AUDIO track ENABLED")
            handleMutedConversationView(false)
        }

        override fun onAudioTrackDisabled(remoteParticipant: RemoteParticipant,
                                          remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.i(TAG, "AUDIO track DISABLED")
            handleMutedConversationView(true)
        }

        override fun onVideoTrackEnabled(remoteParticipant: RemoteParticipant,
                                         remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.i(TAG, "VIDEO track ENABLED")

            // TODO: 11/10/2018    NO HANDLING OF VIDEO VISIBILITY??
//            handleNoVideoView(false)
        }

        override fun onVideoTrackDisabled(remoteParticipant: RemoteParticipant,
                                          remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.i(TAG, "VIDEO track DISABLED")

            // TODO: 11/10/2018    NO HANDLING OF VIDEO VISIBILITY??
//            handleMutedConversationView(true)
//            handleNoVideoView(true)
        }
    }


    /*
     * Twilio SDK event listener
     */
    private val sdkListener = object: SDKTokenUtils.SDKTokenListener {
        override fun tokenResponse(token: String?) {
            if (!token.isNullOrBlank() && usageType == VoiceVideoUsageType.OUTGOING)
                connectToRoom()
            else finish()
        }
    }

    //endregion


    init {

        /*
        * Set local video view to primary view
        */

        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        activity?.volumeControlStream = AudioManager.STREAM_VOICE_CALL

        /*
         * Needed for being able to use speakers.
         */
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        /*
         * Needed for setting/abandoning audio focus during call
         */
        audioManager.isSpeakerphoneOn = true
        areSpeakersOn = true

        /*
         * Setup the broadcast receiver to be notified of video notification messages
         */
        localBroadcastReceiver = CallActionsReceiver()

        if (usageType == VoiceVideoUsageType.INCOMING)
            handleCallStartup()

        /*
         * Set access token
         */
        setAccessToken()

        /*
         * Check permissions and, in case, wait for activity to notify service.
         */
        requestPermissionForCameraAndMicrophone()
    }


    fun onRequestPermissionsResult(requestCode: Int,
                                   permissions: Array<String>,
                                   grantResults: IntArray) {
        if (requestCode == Constants.PERMISSIONS_REQUEST_CAMERA_MIC_CALLS) {
            var cameraAndMicPermissionGranted = true

            for (i in 0 until grantResults.size) {
                val result = grantResults[i]
                val permission = permissions[i]

                if (callType == VoiceVideoCallType.VOICE && permission == Manifest.permission.CAMERA)
                    continue

                cameraAndMicPermissionGranted = cameraAndMicPermissionGranted and
                        (result == PackageManager.PERMISSION_GRANTED)
            }

            canConnectToRoom = if (cameraAndMicPermissionGranted) {
                createAudioAndVideoTracks()
                usageType == VoiceVideoUsageType.OUTGOING
            } else {
                if (activityHandler != null)
                    activityHandler!!.sendEmptyMessage(HANDLER_ACTION_UI_PERMISSIONS_DIALOG)
                false
            }
        }
    }


    fun onResume() {

        registerReceiver()

        val fromBackground = localAudioTrack == null && localVideoTrack == null

        val hasPermissions = PermissionsUtils.checkPermissionForCameraAndMicrophone(context, callType)

        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        localVideoTrack = if (localVideoTrack == null && hasPermissions && callType == VoiceVideoCallType.VIDEO) {
            LocalVideoTrack.create(context,
                    true,
                    cameraCapturerCompat.videoCapturer)
        } else {
            localVideoTrack
        }
        if (localVideoView != null) localVideoTrack?.addRenderer(localVideoView!!)

        localAudioTrack = if (localAudioTrack == null && hasPermissions) {
            LocalAudioTrack.create(context, true)
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

        if (room == null || room!!.state == Room.State.DISCONNECTED)
            if ((fromBackground || usageType == VoiceVideoUsageType.OUTGOING || canConnectToRoom) && hasPermissions)
                connectToRoom()
        canConnectToRoom = false
    }

    fun onPause() {

        if (!finishCalled) {

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
        }
    }


    fun onDestroy() {

        unregisterReceiver()

        disconnectedFromOnDestroy = true

        timeoutTimer?.cancel()
        timeoutTimer = null

        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        room?.disconnect()

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        localAudioTrack?.release()
        localVideoTrack?.release()

//        soundPoolManager?.playDisconnect()
        soundPoolManager.release()
    }


    private fun setAccessToken() {
        if (SDKTokenUtils.callToken.isNullOrBlank()) {

            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()
                val id = HLUser().userId
                SDKTokenUtils.init(primaryVideoView.get()?.context, id, sdkListener)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            } finally {
                RealmUtils.closeRealm(realm)
            }
        }
    }

    fun finish(delayMessage: Boolean = false) {

        finishCalled = true

        if (delayMessage)
            activityHandler?.sendEmptyMessageDelayed(HANDLER_ACTION_FINISH, 1000)
        else
            activityHandler?.sendEmptyMessage(HANDLER_ACTION_FINISH)

        serviceHandler.sendEmptyMessage(HANDLER_ACTION_FINISH)
    }


    /*
    * The initial state when there is no active room.
    */
    private fun initializeUI() {
        activityHandler?.sendEmptyMessage(HANDLER_ACTION_UI_INITIALIZE)
    }

    /*
     * The actions performed during disconnect.
     */
    private fun setDisconnectAction() {
        activityHandler?.sendEmptyMessage(HANDLER_ACTION_UI_SET_DISCONNECT)
    }


    //region == Permissions ==

    private fun requestPermissionForCameraAndMicrophone() {
        if (PermissionsUtils.checkPermissionForCameraAndMicrophone(context, callType))
            createAudioAndVideoTracks()

//        var condition = false
//        if (activity != null && !activity!!.isFinishing && !activity!!.isDestroyed) {
//            condition = if (callType == VoiceVideoCallType.VIDEO)
//                (ActivityCompat.shouldShowRequestPermissionRationale(activity!!, Manifest.permission.CAMERA) ||
//                        ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
//                                Manifest.permission.RECORD_AUDIO))
//            else ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
//                    Manifest.permission.RECORD_AUDIO)
//
//
////                || ActivityCompat.shouldShowRequestPermissionRationale(this,
////                        Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
////                ActivityCompat.shouldShowRequestPermissionRationale(this,
////                        Manifest.permission.READ_EXTERNAL_STORAGE)
//        }
//
//        if (condition) {
//            showDialogForPermissionsRationale()
//        } else {
//            askPermissions()
//        }
    }

//    /*
//     * Depending on {@link VoiceVideoCallType) users are asked for both audio and video permissions (#VoiceVideoCallType.VIDEO)
//     * or only audio (#VoiceVideoCallType.VOICE)
//     */
//    private fun askPermissions() {
//        if (activity != null && !activity!!.isFinishing && !activity!!.isDestroyed) {
//            ActivityCompat.requestPermissions(
//                    activity!!,
//                    if (callType == VoiceVideoCallType.VIDEO) arrayOf(
//                            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
////                            , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
//                    ) else arrayOf(Manifest.permission.RECORD_AUDIO),
//                    Constants.PERMISSIONS_REQUEST_CAMERA_MIC_CALLS)
//        }
//    }
//
//    private fun showDialogForPermissionsRationale() {
//        DialogUtils.createDialog(
//                context,
//                "Permissions needed",
//                "By granting ${context.getString(R.string.app_name)} audio and video permissions you will be able to make and receive audio and video calls",
//                positiveString = "Ask again",
//                negativeString = "not now",
//                positiveClick = DialogInterface.OnClickListener { dialog, _ ->
//                    askPermissions()
//                    dialog.dismiss()
//                },
//                negativeClick = DialogInterface.OnClickListener { dialog, _ ->
//                    dialog.dismiss()
//
//                    finish()
//                }
//        ).show()
//    }

    //endregion


    //region == Call methods ==

    fun connectToRoom() {
        val token = SDKTokenUtils.callToken
        if (!token.isNullOrBlank()) {
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

            room = Video.connect(context, connectOptionsBuilder.build(), roomListener)

            setDisconnectAction()
        }
        else {
            if (activityHandler != null) {
                LogUtils.e(TAG, "Unexpected call token error")

                activityHandler!!.sendMessage(Message.obtain().apply {
                    this.what = HANDLER_ACTION_ERROR
                    this.obj = context.getString(R.string.error_calls_connection)
                })
            }
        }
    }

    /*
     * Called when participant joins the room
     */
    private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.get()?.visibility == View.VISIBLE) {

            if (activityHandler != null) {
                LogUtils.e(TAG, "Multiple participants are not currently supported in this UI")

                activityHandler!!.sendMessage(Message.obtain().apply {
                    this.what = HANDLER_ACTION_ERROR
                    this.obj = context.getString(R.string.error_calls_unexpected)
                })
            }

            return
        }

        soundPoolManager.stopRinging()

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

        LogUtils.e(TAG, "Multiple participants are not currently supported in this UI")
        finish(true)
    }

    //region - AUDIO -

    private fun createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(context, true)

        if (callType == VoiceVideoCallType.VIDEO) {
            // Share your camera
            localVideoTrack = LocalVideoTrack.create(context,
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

                if (usageType == VoiceVideoUsageType.INCOMING && callType == VoiceVideoCallType.VOICE)
                    isSpeakerphoneOn = false
            }
            else {
                mode = previousAudioMode
                abandonAudioFocus()
                isMicrophoneMute = previousMicrophoneMute
            }
        }
    }

    private fun handleMutedConversationView(show: Boolean) {
        activityHandler?.sendMessage(Message.obtain().apply {
            this.what = HANDLER_ACTION_UI_MICROPHONE
            this.obj = show
        })
    }

    fun handleSpeakersToggle(forced: Boolean? = null) {
        val enable = forced ?: !audioManager.isSpeakerphoneOn
        audioManager.isSpeakerphoneOn = enable
    }

    private fun handleMicrophoneDevice() {

    }

    //endregion

    //region - VIDEO -

    private fun moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.get()?.visibility == View.GONE) {
            thumbnailVideoView.get()?.visibility = View.VISIBLE

            if (primaryVideoView.get() != null) {
                with(localVideoTrack) {
                    this?.removeRenderer(primaryVideoView.get()!!)
                    this?.addRenderer(thumbnailVideoView.get()!!)
                }
            }

            if (thumbnailVideoView.get() != null) {
                localVideoView = thumbnailVideoView.get()!!
                thumbnailVideoView.get()!!.mirror = cameraCapturerCompat.cameraSource ==
                        CameraCapturer.CameraSource.FRONT_CAMERA
            }
        }
    }

    private fun moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView.get()?.visibility == View.VISIBLE) {
            thumbnailVideoView.get()?.visibility = View.GONE

            if (thumbnailVideoView.get() != null) {
                with(localVideoTrack) {
                    this?.removeRenderer(thumbnailVideoView.get()!!)
                    this?.addRenderer(primaryVideoView.get()!!)
                }
            }

            if (primaryVideoView.get() != null) {
                localVideoView = primaryVideoView.get()!!
                primaryVideoView.get()!!.mirror = cameraCapturerCompat.cameraSource ==
                        CameraCapturer.CameraSource.FRONT_CAMERA
            }
        }
    }

    /*
     * Set primary view as renderer for participant video track
     */
    private fun addRemoteParticipantVideo(videoTrack: VideoTrack) {
        if (primaryVideoView.get() != null) {
            moveLocalVideoToThumbnailView()
            primaryVideoView.get()!!.mirror = false
            videoTrack.addRenderer(primaryVideoView.get()!!)
        }
    }

    private fun removeParticipantVideo(videoTrack: VideoTrack) {
        if (primaryVideoView.get() != null)
            videoTrack.removeRenderer(primaryVideoView.get()!!)
    }


    private fun getAvailableCameraSource(): CameraCapturer.CameraSource {
        return if (CameraCapturer.isSourceAvailable(CameraCapturer.CameraSource.FRONT_CAMERA))
            CameraCapturer.CameraSource.FRONT_CAMERA
        else
            CameraCapturer.CameraSource.BACK_CAMERA
    }

//    private var noVideoToast: Toast? = null
    private fun handleNoVideoView(show: Boolean) {
//        if (noVideoToast == null)
//            noVideoToast = Toast.makeText(context, "Your friend has disabled video", Toast.LENGTH_LONG)
//
//        if (show) {
//            noVideoToast!!.show()
//        }
//        else {
//            noVideoToast!!.cancel()
//            noVideoToast = null
//        }

        activityHandler?.sendMessage(Message.obtain().apply {
            this.what = HANDLER_ACTION_UI_VIDEO_VISIBILITY
            this.obj = !show
        })
    }

    //endregion

    //endregion


    //region == Click listeners ==

//    private fun disconnectClickListener(): View.OnClickListener {
//        return View.OnClickListener {
//
//            initializeUI()
//
//            finish()
//        }
//    }

    fun switchCamera() {
        val cameraSource = cameraCapturerCompat.cameraSource
        cameraCapturerCompat.switchCamera()
        if (thumbnailVideoView.get()?.visibility == View.VISIBLE) {
            thumbnailVideoView.get()?.mirror = cameraSource == CameraCapturer.CameraSource.BACK_CAMERA
        } else {
            primaryVideoView.get()?.mirror = cameraSource == CameraCapturer.CameraSource.BACK_CAMERA
        }
    }

    fun toggleMute() {
        /*
         * Enable/disable the local audio track. The results of this operation are
         * signaled to other Participants in the same Room. When an audio track is
         * disabled, the audio is muted.
         */
        localAudioTrack?.let {
            val enable = !it.isEnabled
            it.enable(enable)
        }
    }

    // TODO: 11/8/2018    REMOVE???
//    private fun localVideoClickListener(): View.OnClickListener {
//        return View.OnClickListener {
//            /*
//             * Enable/disable the local video track
//             */
//            localVideoTrack?.let { track ->
//                val enable = !track.isEnabled
//                track.enable(enable)
////                val icon: Int
////                if (enable) {
////                    icon = R.drawable.ic_videocam_white_24dp
////                    switchCameraActionFab.show()
////                } else {
////                    icon = R.drawable.ic_videocam_off_black_24dp
////                    switchCameraActionFab.hide()
////                }
////                localVideoActionFab.setImageDrawable(
////                        ContextCompat.getDrawable(this@CallActivity, icon))
//            }
//        }
//    }



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
//        notificationManager!!.cancelAll()

        /*
         * Only handle the notification if not already connected to a Video Room
         */
        if (room == null) soundPoolManager.playRinging(context)
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

            context.registerReceiver(localBroadcastReceiver!!, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        try {
            context.unregisterReceiver(localBroadcastReceiver!!)
            isReceiverRegistered = false
        } catch (e: IllegalArgumentException) {
            LogUtils.e(TAG, e.message, e)
        }
    }

    //endregion


    private inner class CallTimeoutTimer(millis: Long, interval: Long): CountDownTimer(millis, interval) {

        private var bar = if (primaryVideoView.get() != null)
            Snackbar.make(primaryVideoView.get()!!, context.getString(R.string.calls_timer_disconnecting_3), Snackbar.LENGTH_INDEFINITE)
        else
            null

        override fun onFinish() {
            bar?.setText(context.getString(R.string.calls_timer_disconnecting))
            bar?.show()
            Handler().postDelayed(
                    {
                        bar?.dismiss()
                        finish()
                    },
                    500
            )
        }

        override fun onTick(millisUntilFinished: Long) {

            val roundedMillis = Math.round(millisUntilFinished.toDouble() / 1000) * 1000
            when (roundedMillis) {
                3000L, 2000L, 1000L -> {
                    if (roundedMillis == 3000L)
                        bar?.show()
                    else
                        bar?.setText(context.getString(R.string.calls_timer_disconnecting_num, roundedMillis/1000))
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

}