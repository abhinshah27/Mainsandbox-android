/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.DialogUtils
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.NotificationUtils
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.OSVersionUtils
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.PermissionsUtils
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper
import kotlinx.android.synthetic.main.activity_calls.*
import kotlinx.android.synthetic.main.activity_calls_content_video.*

/**
 * @author mbaldrighi on 11/7/2018.
 */
class CallsActivity: HLActivity(), Handler.Callback {

    companion object {
        val LOG_TAG = CallsActivity::class.qualifiedName
    }

    private var callType = VoiceVideoCallType.VOICE
    private var usageType = VoiceVideoUsageType.OUTGOING

    private lateinit var roomName: String
    private lateinit var calledName: String
    private lateinit var calledAvatar: String
    private lateinit var calledWall: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calls)

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
                        or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
                )

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
        requestPermissionForCameraAndMicrophone()

        initializeUI()

        CallsService.startService(
                this,
                Handler(this),
                CallsActivity::class.java,
                usageType, callType, roomName, primaryVideoView, thumbnailVideoView
        )
    }


    override fun onResume() {
        super.onResume()
        startService(Intent(this, CallsService::class.java).apply { this.action = ACTION_ACTIVITY_RESUMED })
    }


    override fun onPause() {
        startService(Intent(this, CallsService::class.java).apply { this.action = ACTION_ACTIVITY_PAUSED })
        super.onPause()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        try {
            startService(Intent(this, CallsService::class.java).apply {
                this.action = ACTION_PERMISSIONS
                this.putExtra(EXTRA_REQUEST_CODE, requestCode)
                this.putExtra(EXTRA_PERMISSIONS, permissions)
                this.putExtra(EXTRA_RESULTS, grantResults)
            })
        } catch (e: IllegalStateException) {
            LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
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
                calledName = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY)
            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_AVATAR))
                calledAvatar = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_AVATAR)
            if (intent.hasExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_WALL))
                calledWall = intent.getStringExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_WALL)
        }
    }


    override fun handleMessage(msg: Message?): Boolean {
        return when (msg?.what) {
            HANDLER_ACTION_FINISH -> {

                // TODO: 11/10/2018    RETHINK THE WHOLE THING

                exitAndRelease()
                true
            }
            HANDLER_ACTION_ERROR -> {
                showAlert((if (msg.obj is String) msg.obj else getString(R.string.error_generic_operation)) as String)
                true
            }
            HANDLER_ACTION_UI_SET_DISCONNECT -> { setDisconnectAction(); true }
            HANDLER_ACTION_UI_INITIALIZE -> { initializeUI(); true }
            HANDLER_ACTION_UI_HIDE_REJECT -> { setDisconnectAction(); true }
            HANDLER_ACTION_UI_PERMISSIONS_DIALOG -> { showDialogForPermissionsRationale(); true }
            HANDLER_ACTION_UI_VIDEO_VISIBILITY -> {
                groupVideo.visibility = if (msg.obj as? Boolean == true) View.VISIBLE else View.GONE
                true
            }
            HANDLER_ACTION_UI_MICROPHONE -> {
                handleMutedConversationView(msg.obj as? Boolean)
                true
            }
            else -> false
        }
    }


    //region == Class custom methods ==

    private var mutedToast: Toast? = null
    private fun handleMutedConversationView(show: Boolean?) {

        // TODO: 11/10/2018   needed something more than simple TOAST??

        if (mutedToast == null)
            mutedToast = Toast.makeText(this, "Your friend muted the conversation", Toast.LENGTH_LONG)

        if (show == true) {
            mutedToast!!.show()
        }
        else {
            mutedToast!!.cancel()
            mutedToast = null
        }
    }


    private fun exitAndRelease() {
        // TODO: 11/10/2018    RETHINK THE WHOLE THING

          startService(Intent(this, CallsService::class.java).apply { this.action = ACTION_STOP_FOREGROUND })
//        finish()
    }



    /*
     * The actions performed during disconnect.
     */
    private fun setDisconnectAction() {
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


    private val rejectCloseCallClickListener = View.OnClickListener { exitAndRelease() }
    private val actionsClickListener = View.OnClickListener {
        val action = when (it.id) {
            R.id.acceptCall -> ACTION_CALL_ACCEPT
            R.id.actionMuteMic -> ACTION_CALL_MUTE
            R.id.actionSpeakers -> ACTION_VOICE_TOGGLE_SPEAKERS
            R.id.actionSwitchCamera -> ACTION_VIDEO_SWITCH_CAMERA
            else -> ""
        }

        if (action.isNotBlank())
            startService(Intent(this, CallsService::class.java).apply { this.action = action })
    }

    private fun initializeUI() {

        userName.text = calledName

        if (callType == VoiceVideoCallType.VIDEO) {
            groupVideo.visibility = View.VISIBLE
            actionSpeakers.visibility = View.GONE

            callTypeView.setText(R.string.call_type_text_video)
        }
        else {
            GlideApp.with(userWallPicture).asDrawable().load(calledWall).into(
                    object: CustomViewTarget<ImageView, Drawable>(userWallPicture as ImageView) {
                        override fun onLoadFailed(errorDrawable: Drawable?) {}

                        override fun onResourceCleared(placeholder: Drawable?) {}

                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            MediaHelper.blurWallPicture(this@CallsActivity, resource, userWallPicture)
                        }
                    }
            )

            MediaHelper.loadProfilePictureWithPlaceholder(this, calledAvatar, userAvatar as ImageView)

            groupVideo.visibility = View.GONE
            actionSwitchCamera.visibility = View.GONE

            callTypeView.setText(R.string.call_type_text_voice)
        }

        with (closeCall) {
            this.setOnClickListener(rejectCloseCallClickListener)
            this.hide()
        }
        with (rejectCall) {
            this.setOnClickListener(rejectCloseCallClickListener)
            this.hide()
        }
        with (acceptCall) {
            this.setOnClickListener(actionsClickListener)
            this.hide()
        }

        actionMuteMic.setOnClickListener(actionsClickListener)
        actionSwitchCamera.setOnClickListener(actionsClickListener)
        actionSpeakers.setOnClickListener(actionsClickListener)

        if (usageType == VoiceVideoUsageType.OUTGOING) {
            closeCall.show()
//            setDisconnectAction()
        }
        else {
            closeCall.hide()
            groupIncomingCall.visibility = View.VISIBLE
            acceptCall.show(); rejectCall.show()
        }
    }



    //region == Permissions ==

    private fun requestPermissionForCameraAndMicrophone() {
        if (PermissionsUtils.checkPermissionForCameraAndMicrophone(this, callType)) {
            return
        }

        val condition = if (callType == VoiceVideoCallType.VIDEO)
                (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.RECORD_AUDIO))
            else ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)


//                || ActivityCompat.shouldShowRequestPermissionRationale(this,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
//                ActivityCompat.shouldShowRequestPermissionRationale(this,
//                        Manifest.permission.READ_EXTERNAL_STORAGE)

        if (condition) {
            showDialogForPermissionsRationale()
        } else {
            askPermissions()
        }
    }

    /*
     * Depending on {@link VoiceVideoCallType) users are asked for both audio and video permissions (#VoiceVideoCallType.VIDEO)
     * or only audio (#VoiceVideoCallType.VOICE)
     */
    private fun askPermissions() {
        ActivityCompat.requestPermissions(
                this,
                if (callType == VoiceVideoCallType.VIDEO) arrayOf(
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
//                            , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
                ) else arrayOf(Manifest.permission.RECORD_AUDIO),
                Constants.PERMISSIONS_REQUEST_CAMERA_MIC_CALLS)
    }

    private fun showDialogForPermissionsRationale() {
        DialogUtils.createDialog(
                this,
                "Permissions needed",
                "By granting ${getString(R.string.app_name)} audio and video permissions you will be able to make and receive audio and video calls",
                positiveString = "Ask again",
                negativeString = "not now",
                positiveClick = DialogInterface.OnClickListener { dialog, _ ->
                    askPermissions()
                    dialog.dismiss()
                },
                negativeClick = DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()

                    exitAndRelease()
                }
        ).show()
    }

    //endregion




    //endregion
}