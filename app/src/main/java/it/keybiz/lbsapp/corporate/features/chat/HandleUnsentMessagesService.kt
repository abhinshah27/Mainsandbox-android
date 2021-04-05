/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.realm.Realm
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.features.HomeActivity
import it.keybiz.lbsapp.corporate.features.splash.SplashActivity
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import it.keybiz.lbsapp.corporate.utilities.Utils


const val UNSENT_NOTIFICATION_ID = 102

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
class HandleUnsentMessagesService : IntentService(HandleUnsentMessagesService::class.qualifiedName) {

    companion object {

        private val LOG_TAG = HandleUnsentMessagesService::class.qualifiedName

        @JvmStatic
        fun startService(context: Context) {
            try{
                context.startService(Intent(context, HandleUnsentMessagesService::class.java))
            } catch (e: IllegalStateException) {
                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        var realm: Realm? = null
        try {
            realm = RealmUtils.getCheckedRealm()
            if (ChatMessage.handleUnsentMessages(realm)) sendNotification()
            else {
                // cancel the notification
                (this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(UNSENT_NOTIFICATION_ID)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        finally {
            realm?.close()
        }
    }


    private fun sendNotification() {
        val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val msg = getString(R.string.notification_unsent_messages_text)

        val extra = Intent(this, if (LBSLinkApp.isForeground) HomeActivity::class.java else SplashActivity::class.java)
        extra.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        extra.putExtra(Constants.KEY_NOTIFICATION_RECEIVED, Constants.CODE_NOTIFICATION_CHAT_UNSENT_MESSAGES)

        val contentIntent = PendingIntent.getActivity(this, 0, extra,
                PendingIntent.FLAG_CANCEL_CURRENT)

        if (Utils.hasOreo()) createDefaultNotificationChannel()
        val mBuilder = NotificationCompat.Builder(this, getString(R.string.notif_channel_chat_unsent))
                .setSmallIcon(R.drawable.ic_notification_chat)
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                .setContentTitle(getString(R.string.notification_unsent_messages_title))
                .setColor(Utils.getColor(this, R.color.colorAccent))
                .setContentText(msg)
                .setTicker(msg)
                .setAutoCancel(true)
                .setDefaults(0)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        val notification = mBuilder.build()

        mNotificationManager.notify(UNSENT_NOTIFICATION_ID, notification)
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createDefaultNotificationChannel() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // The id of the channel.
        val id = getString(R.string.notif_channel_chat_unsent)
        // The user-visible name of the channel.
        val name = getString(R.string.notification_unsent_messages_title)
        // The user-visible description of the channel.
        val description = getString(R.string.notif_channel_chat_unsent_description)
        val importance = NotificationManager.IMPORTANCE_LOW

        val mChannel = NotificationChannel(id, name, importance)
        // Configure the notification channel.
        mChannel.description = description
        mChannel.enableLights(true)
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.lightColor = Color.RED
        mChannel.enableVibration(false)
        mNotificationManager.createNotificationChannel(mChannel)
    }


}
