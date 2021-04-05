/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.view.View
import androidx.core.app.NotificationCompat
import com.twilio.video.VideoView
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import java.lang.ref.WeakReference


const val ACTION_START_FOREGROUND = "start_foreground"
const val ACTION_STOP_FOREGROUND = "stop_foreground"
const val ACTION_MAIN = "main"

const val ACTION_PERMISSIONS = "result_permissions"
const val EXTRA_REQUEST_CODE = "extra_code"
const val EXTRA_PERMISSIONS = "extra_permissions"
const val EXTRA_RESULTS = "extra_results"

const val ACTION_ACTIVITY_PAUSED = "activity_paused"
const val ACTION_ACTIVITY_RESUMED = "activity_resumed"

const val ACTION_CALL_REJECT = "reject_call"
const val ACTION_CALL_CLOSE = "close_call"
const val ACTION_CALL_ACCEPT = "accept_call"
const val ACTION_CALL_MUTE = "mute_call"
const val ACTION_VOICE_TOGGLE_SPEAKERS = "toggle_speakers"
const val ACTION_VIDEO_SWITCH_CAMERA = "switch_camera"

const val SERVICE_NOTIFICATION_ID = 101
const val CALL_CHANNEL_ID = "PRIMARY_CALL_CHANNEL"



/**
 * @author mbaldrighi on 11/7/2018.
 */
class CallsService: Service(), Handler.Callback {

    companion object {
        const val LOG_TAG = "CallsService"

        private var handler: Handler? = null
        private var activity: WeakReference<Activity>? = null

        private var callHelper: VoiceVideoCallsHelper? = null

        private lateinit var activityClass: Class<*>

        private var usageType = VoiceVideoUsageType.OUTGOING
        private var callType = VoiceVideoCallType.VOICE
        private lateinit var roomName: String

        private lateinit var weakPrimaryView: WeakReference<VideoView?>
        private lateinit var weakThumbnailView: WeakReference<VideoView?>

        @JvmStatic
        fun startService(context: Context, handler: Handler, activityClass: Class<*>,
                         usageType: VoiceVideoUsageType, callType: VoiceVideoCallType, roomName: String,
                         primaryVideoView: View, thumbnailVideoView: View) {
            try {
                Companion.handler = handler
                Companion.activityClass = activityClass

                activity = if (context is Activity) WeakReference(context) else null

                Companion.usageType = usageType
                Companion.callType = callType
                Companion.roomName = roomName

                if (primaryVideoView is VideoView)
                    weakPrimaryView = WeakReference(primaryVideoView)
                if (thumbnailVideoView is VideoView)
                    weakThumbnailView = WeakReference(thumbnailVideoView)

                context.startService(Intent(context, CallsService::class.java).apply { this.action = ACTION_START_FOREGROUND })

                //// TODO: 11/8/2018    BIND SERVICE???

            } catch (e: IllegalStateException) {
                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
            }
        }
    }

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }





    override fun onCreate() {
        super.onCreate()

        if (callHelper == null) {
            callHelper = VoiceVideoCallsHelper(
                    this,
                    handler,
                    Handler(this),
                    activity?.get(),
                    weakPrimaryView, weakThumbnailView,
                    roomName, callType, usageType)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notificationIntent = Intent(this, activityClass)
        notificationIntent.action = ACTION_MAIN
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0)

        when (intent?.action) {

            ACTION_START_FOREGROUND -> {
                LogUtils.i(LOG_TAG, "Received Start Foreground Intent")

                val closeIntent = Intent(this, CallsService::class.java)
                closeIntent.action = ACTION_CALL_CLOSE
                val closePending = PendingIntent.getService(this, 0,
                        closeIntent, 0)

                val rejectIntent = Intent(this, CallsService::class.java)
                rejectIntent.action = ACTION_CALL_REJECT
                val rejectPending = PendingIntent.getService(this, 0,
                        rejectIntent, 0)

                val acceptIntent = Intent(this, CallsService::class.java)
                acceptIntent.action = ACTION_CALL_ACCEPT
                val acceptPending = PendingIntent.getService(this, 0,
                        acceptIntent, 0)

                val builder = getBaseBuilder(pendingIntent)
                with(builder) {
                    if (usageType == VoiceVideoUsageType.OUTGOING) {
                        this.addAction(NotificationCompat.Action.Builder(0, "Hang up", closePending).build())
                    } else {
                        this.addAction(NotificationCompat.Action.Builder(0, "Accept", acceptPending).build())
                        this.addAction(NotificationCompat.Action.Builder(0, "Reject", rejectPending).build())
                    }
                }
                val notification = builder.build()   // build actual notification

//                val muteIntent = Intent(this, CallsService::class.java)
//                muteIntent.action = ACTION_CALL_MUTE
//                val mutePending = PendingIntent.getService(this, 0,
//                        muteIntent, 0)

                if (usageType == VoiceVideoUsageType.OUTGOING) callHelper?.connectToRoom()

                startForeground(SERVICE_NOTIFICATION_ID, notification)
            }

            ACTION_STOP_FOREGROUND -> {
                LogUtils.i(LOG_TAG, "Received Stop Foreground Intent")

                callHelper?.finish()
            }

            ACTION_CALL_REJECT -> {
                callHelper?.finish()
            }
            ACTION_CALL_CLOSE -> callHelper?.finish()
            ACTION_CALL_ACCEPT -> {
                val closeIntent = Intent(this, CallsService::class.java)
                closeIntent.action = ACTION_CALL_CLOSE
                val closePending = PendingIntent.getService(this, 0,
                        closeIntent, 0)

                val notification = getBaseBuilder(pendingIntent).apply {
                    this.addAction(NotificationCompat.Action.Builder(0, "Hang up", closePending).build())
                }.build()

                notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)

                callHelper?.connectToRoom()
            }

            ACTION_CALL_MUTE -> callHelper?.toggleMute()
            ACTION_VIDEO_SWITCH_CAMERA -> callHelper?.switchCamera()
            ACTION_VOICE_TOGGLE_SPEAKERS -> callHelper?.handleSpeakersToggle()

            ACTION_PERMISSIONS -> {
                val code = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)
                val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)
                val results = intent.getIntArrayExtra(EXTRA_RESULTS)

                if (permissions != null)
                    callHelper?.onRequestPermissionsResult(code, permissions, results)
            }

            ACTION_ACTIVITY_RESUMED -> callHelper?.onResume()
            ACTION_ACTIVITY_PAUSED -> callHelper?.onPause()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun handleMessage(msg: Message?): Boolean {
        callHelper?.onDestroy()

        stopForeground(true)
        stopSelf()
        return true
    }


    /**
     * Build a notification.
     * @param primaryPendingIntent the pending intent common to all CALL notifications
     * @return the [Notification.Builder]
     */
    private fun getBaseBuilder(primaryPendingIntent: PendingIntent): NotificationCompat.Builder {
        if (Utils.hasOreo()) {
            val callInviteChannel = NotificationChannel(CALL_CHANNEL_ID,
                    getString(R.string.call_notification_channel), NotificationManager.IMPORTANCE_HIGH)
            callInviteChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            callInviteChannel.enableVibration(false)
            callInviteChannel.enableLights(false)
            notificationManager.createNotificationChannel(callInviteChannel)
        }

        return NotificationCompat.Builder(this, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_tray)
                .setContentTitle("${getString(R.string.app_name)} call")
                .setContentText(
                        "${if (usageType == VoiceVideoUsageType.OUTGOING) "Outgoing" else "Incoming"} " +
                                "${if (callType == VoiceVideoCallType.VOICE) "voice" else "video"} call"
                )
                .setOngoing(true)
                .setTicker("${getString(R.string.app_name)} call")
                .setContentIntent(primaryPendingIntent)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
    }

    /**
     * Build a notification.
     * @param primaryPendingIntent the pending intent common to all CALL notifications
     * @return the [NotificationCompat.Builder]
     */
    private fun getBaseBuilderCompat(primaryPendingIntent: PendingIntent): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_tray)
                .setContentTitle("${getString(R.string.app_name)} call")
                .setContentText(
                        "${if (usageType == VoiceVideoUsageType.OUTGOING) "Outgoing" else "Incoming"} " +
                                "${if (callType == VoiceVideoCallType.VOICE) "voice" else "video"} call"
                )
                .setOngoing(true)
                .setTicker("${getString(R.string.app_name)} call")
                .setContentIntent(primaryPendingIntent)
    }

}