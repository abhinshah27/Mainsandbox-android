/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.voiceVideoCalls

import android.Manifest
import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.RemoteMessage
import it.keybiz.lbsapp.corporate.BuildConfig
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.model.IncomingCall
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp.VoiceVideoCallType
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp.VoiceVideoUsageType
import it.keybiz.lbsapp.corporate.models.HLUserGeneric
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import okhttp3.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


/**
 * File containing all the Utilities classes for VOICE and VIDEO calls.
 * @author mbaldrighi on 10/4/2018.
 */

const val BASE_URL = "http://ec2-34-201-111-17.compute-1.amazonaws.com:5000/api"
const val BASE_URL_PROD = "https://chat.highlanders.app/api"

object SDKTokenUtils {

    private val TOKEN_URL =
            if (BuildConfig.USE_PROD_CONNECTION) "$BASE_URL_PROD/token"
            else "$BASE_URL/token"

    private var contextRef: WeakReference<Context?>? = null

    private var tokenListener: SDKTokenListener? = null
    private lateinit var callerIdentity: String

    var callToken: String? = null


    private val onHTTPCallback = object: Callback {
        override fun onFailure(call: Call, e: IOException) {
            tokenListener?.tokenResponse()
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                callToken = response.body()?.string()
            }
            else {
                if (contextRef?.get() is Activity) {
                    (contextRef?.get() as Activity).runOnUiThread {
                        LogUtils.d("Retrieving Twilio token", "Error retrieving token")
                    }
                }
            }

            tokenListener?.tokenResponse(callToken)
        }
    }

    @JvmStatic
    fun init(context: Context?, identity: String, listener: SDKTokenListener? = null) {
        contextRef = WeakReference(context)
        this.callerIdentity = identity
        retrieveAccessTokenFromServer(listener)
    }

    private fun retrieveAccessTokenFromServer(tokenListener: SDKTokenListener? = null) {
        this.tokenListener = tokenListener

        val client = OkHttpClient()
        val request = Request.Builder()
                .url("$TOKEN_URL?identity=$callerIdentity")
                .build()

        client.newCall(request).enqueue(onHTTPCallback)
    }

    interface SDKTokenListener {
        fun tokenResponse(token: String? = null)
    }

}


object NotificationUtils {

    private const val TAG = "NotificationUtils"
    private const val CALL_CHANNEL = "PRIMARY_CALL_CHANNEL"

    private val NOTIFICATION_URL =
            if (BuildConfig.USE_PROD_CONNECTION) "$BASE_URL_PROD/sendvoippush?"
            else "$BASE_URL/sendvoippush?"
    private val NOTIFICATION_URL_MANAGE =
            if (BuildConfig.USE_PROD_CONNECTION) "$BASE_URL_PROD/ManageCall?"
            else "$BASE_URL/ManageCall?"

    /*
     * Intent keys used to provide information about a call notification
     */
    const val CALL_NOTIFICATION_USAGE_TYPE = "CALL_NOTIFICATION_USAGE_TYPE"
    const val CALL_NOTIFICATION_CALL_TYPE = "CALL_NOTIFICATION_CALL_TYPE"
    const val CALL_NOTIFICATION_IDENTITY = "CALL_NOTIFICATION_IDENTITY"
    const val CALL_NOTIFICATION_IDENTITY_NAME = "CALL_NOTIFICATION_IDENTITY_NAME"
    const val CALL_NOTIFICATION_IDENTITY_AVATAR = "CALL_NOTIFICATION_IDENTITY_AVATAR"
    const val CALL_NOTIFICATION_IDENTITY_WALL = "CALL_NOTIFICATION_IDENTITY_WALL"
    const val CALL_NOTIFICATION_ROOM_NAME = "CALL_NOTIFICATION_ROOM_NAME"
    const val CALL_NOTIFICATION_ID = "CALL_NOTIFICATION_ID"
    const val CALL_NOTIFICATION_ACTION = "CALL_NOTIFICATION_ACTION"

    /*
     * Constant values for ACTIONS
     */
    const val CALL_ACTION_DECLINED = "declined"
    const val CALL_ACTION_CLOSED = "closed"
    const val CALL_ACTION_ANSWERED = "answeredCall"

    /*
     * The keys sent by the model.IncomingCall model class
     */
    private const val KEY_NOTIFY_CALLER_ID = "callerID"
    private const val KEY_NOTIFY_CALLER_NAME = "callerName"
    private const val KEY_NOTIFY_CALLER_AVATAR = "avatarURL"
    private const val KEY_NOTIFY_CALLER_WALL = "wallURL"
    private const val KEY_NOTIFY_ROOM_NAME = "roomName"
    private const val KEY_NOTIFY_CALL_TYPE = "isVideo"
    private const val KEY_NOTIFY_CALL_ID = "callID"
    private const val KEY_NOTIFY_CALL_ACTION = "action"

    @JvmStatic
    fun processPayload(serviceContext: Context, message: RemoteMessage?) {

        val messageData = message!!.data
        LogUtils.d(TAG, messageData.toString())

        if (messageData.containsKey(KEY_NOTIFY_CALL_ACTION)) {
            broadcastManageNotification(serviceContext, messageData)
        }
        else {
            val incomingCall = IncomingCall(
                    messageData[KEY_NOTIFY_CALL_ID],
                    messageData[KEY_NOTIFY_CALLER_ID],
                    messageData[KEY_NOTIFY_CALLER_NAME],
                    messageData[KEY_NOTIFY_CALLER_AVATAR],
                    messageData[KEY_NOTIFY_CALLER_WALL],
                    messageData[KEY_NOTIFY_ROOM_NAME],
                    if (messageData[KEY_NOTIFY_CALL_TYPE] == "1") VoiceVideoCallType.VIDEO
                    else VoiceVideoCallType.VOICE
            )

            LogUtils.d(TAG, "From: ${incomingCall.fromIdentity}, ${incomingCall.fromIdentityName}," +
                    "\n${incomingCall.fromIdentityAvatar},\n${incomingCall.fromIdentityWall}")
            LogUtils.d(TAG, "Room Name: " + incomingCall.roomName)

            if (!incomingCall.roomName.isNullOrBlank())
                broadcastCallNotification(serviceContext, incomingCall)
        }
    }


    @JvmStatic
    fun sendCallNotificationAndGoToActivity(context: HLActivity, callerID: String,
                                            callerName: String, calledIdentity: HLUserGeneric,
                                            callType: VoiceVideoCallType = VoiceVideoCallType.VOICE) {

        val calledId = calledIdentity.id
        val calledName = calledIdentity.completeName
        val calledAvatar = calledIdentity.avatarURL
        val calledWall = calledIdentity.wallImageLink
        val uuid = UUID.randomUUID().toString()

        context.run { this.setProgressMessage(this.getString(R.string.loading_call, calledName)); this.openProgress() }

        val client = OkHttpClient()
        val request = Request.Builder()
                .url(NOTIFICATION_URL +
                        "callerName=$callerName" +
                        "&callerID=$callerID" +
                        "&userID=$calledId" +
                        "&isVideo=${(if (callType == VoiceVideoCallType.VIDEO) 1 else 0)}" +
                        "&roomName=$calledId" +
                        "&callID=$uuid"
                )
                .build()
        LogUtils.d(TAG, request.url().toString())

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                context.run { runOnUiThread { closeProgress(); showAlert(context.getString(R.string.error_calls_start)) } }
            }

            override fun onResponse(call: Call, response: Response) {

                context.run { runOnUiThread { closeProgress() } }

                if (response.isSuccessful) {
                    val message = response.body()?.string()
                    when (message) {
                        "OK" -> {
                            context.startActivity(
                                    Intent(context, CallsActivityNoService::class.java).apply {
                                        this.putExtra(NotificationUtils.CALL_NOTIFICATION_CALL_TYPE, callType)
                                        this.putExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY, calledId)
                                        this.putExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_NAME, calledName)
                                        this.putExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_AVATAR, calledAvatar)
                                        this.putExtra(NotificationUtils.CALL_NOTIFICATION_IDENTITY_WALL, calledWall)
                                        this.putExtra(NotificationUtils.CALL_NOTIFICATION_ROOM_NAME, calledId)
                                        this.putExtra(NotificationUtils.CALL_NOTIFICATION_ID, uuid)
                                    }
                            )
                        }
                        "KO" -> {
                            context.runOnUiThread {
                                val alertDialogBuilder = AlertDialog.Builder(context)
                                alertDialogBuilder.setPositiveButton(context.getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                                alertDialogBuilder.setCancelable(true)
                                alertDialogBuilder.setMessage(context.getString(R.string.error_calls_busy, calledName))
                                alertDialogBuilder.create().show()
                            }
                        }
                    }
                }
                else {
                    context.runOnUiThread { context.showAlert(context.getString(R.string.error_calls_start)) }
                }
            }
        })

    }

    fun sendCallManageNotification(userID: String, action: String, callID: String,
                                   wantsToken: Boolean = false, context: Context? = null) {

        val client = OkHttpClient()
        val request = Request.Builder()
                .url(NOTIFICATION_URL_MANAGE +
                        "userID=$userID" +
                        "&action=$action" +
                        "&callID=$callID" +
                        if (wantsToken) "&token=${SharedPrefsUtils.retrieveFCMTokenId(context)}" else ""
                )
                .build()
        LogUtils.d(TAG, request.url().toString())

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogUtils.d(TAG, "Send MANAGE NOTIFICATION FAILURE: $action")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    LogUtils.d(TAG, "Send MANAGE NOTIFICATION SUCCESS: $action")
                }
                else LogUtils.d(TAG, "Send MANAGE NOTIFICATION FAILURE: $action")
            }
        })

    }



    /**
     * Build a notification ad hoc for calls.
     * @param primaryPendingIntent the pending intent common to all CALL notifications
     * @return the [Notification.Builder]
     */
    fun getBaseBuilder(context: Context, primaryPendingIntent: PendingIntent,
                       callType: VoiceVideoCallType, usageType: VoiceVideoUsageType, ongoing: Boolean = true,
                       title: String? = null, body: String? = null): NotificationCompat.Builder {

        val channelID = context.getString(R.string.notif_channel_calls)
        if (Utils.hasOreo()) {
            val callInviteChannel = NotificationChannel(channelID,
                    context.getString(R.string.call_notification_channel), NotificationManager.IMPORTANCE_HIGH)
            callInviteChannel.description = context.getString(R.string.notif_channel_calls_description)
            callInviteChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            callInviteChannel.enableVibration(false)
            callInviteChannel.enableLights(false)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(callInviteChannel)
        }

        return NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.drawable.ic_notification_tray)
                .setContentTitle(if (!title.isNullOrBlank()) title else "${context.getString(R.string.app_name)} call")
                .setContentText(
                        if (!body.isNullOrBlank()) body
                        else "${if (usageType == VoiceVideoUsageType.OUTGOING) "Outgoing" else "Incoming"} " +
                                "${if (callType == VoiceVideoCallType.VOICE) "voice" else "video"} call"
                )
                .setOngoing(ongoing)
                .setTicker("${context.getString(R.string.app_name)} call")
                .setContentIntent(primaryPendingIntent)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
    }

    /*
     * Broadcast the Call Notification to the Activity
     */
    private fun broadcastCallNotification(context: Context, incomingCall: IncomingCall) {
        context.startActivity(Intent(context, CallsActivityNoService::class.java).run {
            this.putExtra(CALL_NOTIFICATION_IDENTITY, incomingCall.fromIdentity)
            this.putExtra(CALL_NOTIFICATION_IDENTITY_NAME, incomingCall.fromIdentityName)
            this.putExtra(CALL_NOTIFICATION_IDENTITY_AVATAR, incomingCall.fromIdentityAvatar)
            this.putExtra(CALL_NOTIFICATION_IDENTITY_WALL, incomingCall.fromIdentityWall)
            this.putExtra(CALL_NOTIFICATION_ROOM_NAME, incomingCall.roomName)
            this.putExtra(CALL_NOTIFICATION_CALL_TYPE, incomingCall.callType)
            this.putExtra(CALL_NOTIFICATION_USAGE_TYPE, VoiceVideoUsageType.INCOMING)
            this.putExtra(CALL_NOTIFICATION_ID, incomingCall.id)
            this.action = Intent.ACTION_MAIN
            this.addCategory(Intent.CATEGORY_LAUNCHER)
            this.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /*
     * Broadcast the Call Notification to the Activity
     */
    private fun broadcastManageNotification(context: Context, messageData: Map<String, String>) {
        val callID = messageData[KEY_NOTIFY_CALL_ID]
        val action = messageData[KEY_NOTIFY_CALL_ACTION]

        if (Utils.areStringsValid(callID, action)) {

            if (!callID.equals(CallsActivityNoService.currentCallID, true)) return

            context.startActivity(Intent(context, CallsActivityNoService::class.java).run {
                this.putExtra(CALL_NOTIFICATION_ID, callID)
                this.putExtra(CALL_NOTIFICATION_ACTION, action)
                this.action = Intent.ACTION_MAIN
                this.addCategory(Intent.CATEGORY_LAUNCHER)
                this.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}


object PermissionsUtils {

    fun checkPermissionForCameraAndMicrophone(context: Context, callType: VoiceVideoCallType): Boolean {
        if (OSVersionUtils.hasOSVersion(Build.VERSION_CODES.M)) {
            val resultCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            val resultMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)

            return (
                    if (callType == VoiceVideoCallType.VIDEO)
                        resultCamera == PackageManager.PERMISSION_GRANTED && resultMic == PackageManager.PERMISSION_GRANTED
                    else
                        resultMic == PackageManager.PERMISSION_GRANTED
                    )
        }

        return true
    }

    fun checkPermissionForPhoneState(context: Context): Boolean {
        return if (OSVersionUtils.hasOSVersion(Build.VERSION_CODES.M))
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        else true
    }

}


object DialogUtils {

    fun createDialog(context: Context,
                     title: String,
                     body: String?,
                     @DrawableRes icon: Int = 0,
                     positiveString: String, negativeString: String,
                     positiveClick: DialogInterface.OnClickListener,
                     negativeClick: DialogInterface.OnClickListener,
                     vararg views: View? = emptyArray()): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context)
        if (icon != 0) alertDialogBuilder.setIcon(icon)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setPositiveButton(positiveString, positiveClick)
        alertDialogBuilder.setNegativeButton(negativeString, negativeClick)
        alertDialogBuilder.setCancelable(false)
        if (views.isNotEmpty()) {
            for (it in views)
                alertDialogBuilder.setView(it)
        }
        else if (!body.isNullOrBlank())
            alertDialogBuilder.setMessage(body)
        return alertDialogBuilder.create()
    }

}


object OSVersionUtils {

    fun hasOSVersion(osIntVersion: Int): Boolean {
        return Build.VERSION.SDK_INT >= osIntVersion
    }

}