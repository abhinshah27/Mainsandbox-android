/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Map;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.splash.SplashActivity;
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.NotificationUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 11/28/2017.
 */
public class FCMMessagingService extends FirebaseMessagingService {

	private static final String LOG_TAG = FCMMessagingService.class.getCanonicalName();

	private static final String KEY_NOTIFICATION_TYPE = "notificationType";

	@Override
	public void onMessageReceived(RemoteMessage message) {
		Map<String, String> data = message.getData();

		// Print notification payload
		JSONObject jsonData = new JSONObject(data);
		LogUtils.d(LOG_TAG, "FCM Notification payload: " + jsonData.toString());

		WakeLocker.acquire(getApplicationContext());

		Intent resultIntent = new Intent();
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);


//		resultIntent.putExtra("some code", true);

		if (data.containsKey(KEY_NOTIFICATION_TYPE)) {

			String code = data.get(KEY_NOTIFICATION_TYPE);
			if (Utils.isStringValid(code)) {
				switch (code) {

					case "CALL":
						NotificationUtils.processPayload(this, message);
						WakeLocker.release();
						break;

					case "CHAT":
						if (data.containsKey("message")) {
							String dataMessage = data.get("message");
							createChatChannelAndSendNotification(this, Constants.NOTIFICATION_CHAT_ID, dataMessage, null);
						}
						break;

					case "SOCIAL":
						if (data.containsKey("message")) {
							String dataMessage = data.get("message");
							sendNotification(Constants.NOTIFICATION_GENERIC_ID, dataMessage, null);
						}
						break;
				}
			}
		}
	}

	@Override
	public void onNewToken(String s) {
		super.onNewToken(s);

		LogUtils.d(LOG_TAG, "Refreshed FCM token: " + s);

		if (checkFcmToken(getApplicationContext(), s))
			SendFCMTokenService.startService(this);
	}

	/**
	 * Checks whether  the refreshed FCM token from {@link FirebaseInstanceId}.
	 * @param newToken the new FCM token caught by the Service.
	 * @return The refreshed token upon successful operation, {@code null} otherwise.
	 */
	private boolean checkFcmToken(Context context, String newToken) {
		if (!Utils.isStringValid(newToken))
			return false;

		String fcm = SharedPrefsUtils.retrieveFCMTokenId(context);
		if (!LBSLinkApp.fcmTokenSent || (Utils.isStringValid(newToken) && !fcm.equals(newToken))) {
			SharedPrefsUtils.storeFCMTokenId(context, newToken);
			return true;
		}
		else
			LogUtils.d("FCM ERROR", new FCMNotNeededException());

		return false;
	}

	private void sendNotification(int id, String msg, Intent extra) {
		NotificationManager mNotificationManager = (NotificationManager)
				this.getSystemService(Context.NOTIFICATION_SERVICE);

		if (mNotificationManager != null) {
			long when = System.currentTimeMillis();
			if (extra == null) {
				// INFO: 2/15/19    LUISS restores notifications tab
				extra = new Intent(this, LBSLinkApp.isForeground ? HomeActivity.class : SplashActivity.class);
				extra.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				extra.putExtra(Constants.KEY_NOTIFICATION_RECEIVED, Constants.CODE_NOTIFICATION_GENERIC);
				extra.putExtra(Constants.FRAGMENT_KEY_CODE, Constants.FRAGMENT_NOTIFICATIONS);
			}

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, extra,
					PendingIntent.FLAG_CANCEL_CURRENT);

//		Bitmap bpm = BitmapFactory.decodeResource(getResources(),R.drawable.ic_notification);
			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder(this, getString(R.string.notif_channel_default))

							.setSmallIcon(R.drawable.ic_notification_tray)
//						.setLargeIcon(bpm)
							.setContentTitle(getString(R.string.app_name))
							.setColor(Utils.getColor(this, R.color.colorAccent))
							.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
							.setContentText(msg)
							.setTicker(msg)
							.setPriority(Notification.PRIORITY_HIGH)
//						.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.a_nine))
							.setVibrate(new long[]{0,
									Constants.VIBE_LONG, 200,
									Constants.VIBE_LONG, 200})
//						.setLights(Color.YELLOW, 600, 1000)
							.setAutoCancel(true)
							.setDefaults(0)
							.setWhen(when)
							.setContentIntent(contentIntent)
							.setCategory(NotificationCompat.CATEGORY_SOCIAL);

			Notification notification = mBuilder.build();

			// TODO: 5/11/2018    notifications are simply updated. only one in tray
//			int _id = (!Utils.isStringValid(id) ? (int) when : id.hashCode());
			mNotificationManager.notify(id, notification);

			WakeLocker.release();
		}
	}

	public static void createChatChannelAndSendNotification(Context context, int id, String msg, Intent extra) {
		NotificationManager mNotificationManager = (NotificationManager)
				context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (mNotificationManager != null) {

			if (extra == null) {
				extra = new Intent(context, LBSLinkApp.isForeground ? HomeActivity.class : SplashActivity.class);
				extra.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				extra.putExtra(Constants.KEY_NOTIFICATION_RECEIVED, Constants.CODE_NOTIFICATION_CHAT);
			}

			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, extra,
					PendingIntent.FLAG_CANCEL_CURRENT);

			String channelID = context.getString(R.string.notif_channel_chat);
			if (Utils.hasOreo()) {
				NotificationChannel chatChannel = new NotificationChannel(channelID,
						context.getString(R.string.chat_notification_channel), NotificationManager.IMPORTANCE_HIGH);
				chatChannel.setDescription(context.getString(R.string.notif_channel_chat_description));
				chatChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
				chatChannel.enableVibration(true);
				chatChannel.setVibrationPattern(new long[]{0, Constants.VIBE_SHORT});
				chatChannel.setSound(
						Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.chat_incoming_message),
						new AudioAttributes.Builder()
								.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
								.setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
								.build()
						);
				chatChannel.enableLights(true);
				chatChannel.setLightColor(Color.YELLOW);
				mNotificationManager.createNotificationChannel(chatChannel);
			}

            long when = System.currentTimeMillis();

            String[] parts = msg.split(":");
            NotificationCompat.Style style = new NotificationCompat.BigTextStyle().bigText(msg);
            boolean wantsGroup = false;
            // if has Nougat and everything is okay with split. FOR NOW NEVER TRY TO BUNDLE
            if (/*wantsGroup = */(Utils.hasNougat() && parts.length == 2 && Utils.areStringsValid(parts[0], parts[1]))) {
                if (Utils.hasPie())
                    style = new NotificationCompat.MessagingStyle(new Person.Builder().setName("Me").build())
                            .addMessage(parts[1].substring(1), when, new Person.Builder().setName(parts[0]).build());
                else
                    style = new NotificationCompat.MessagingStyle("Me")
                            .addMessage(new NotificationCompat.MessagingStyle.Message(parts[1].trim(), when, parts[0]));
            }

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, context.getString(R.string.notif_channel_chat))
                            .setSmallIcon(R.drawable.ic_notification_chat)
                            .setContentTitle(context.getString(R.string.app_name))
                            .setColor(Utils.getColor(context, R.color.colorAccent))
                            .setStyle(style)
                            .setContentText(msg)
                            .setTicker(msg)
                            .setPriority(Notification.PRIORITY_HIGH)
                            .setVibrate(new long[]{0, Constants.VIBE_SHORT})
                            .setSound(
                                    Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.chat_incoming_message),
                                    AudioManager.STREAM_NOTIFICATION
                            )
                            .setLights(Color.YELLOW, 600, 1000)
                            .setAutoCancel(true)
                            .setDefaults(0)
                            .setWhen(when)
                            .setContentIntent(contentIntent)
                            .setCategory(NotificationCompat.CATEGORY_MESSAGE);

            Notification notification = mBuilder.build();
            Notification summaryNotification = null;

            // no grouping for now
            if (wantsGroup) {
                mBuilder.setGroup(Constants.NOTIFICATION_CHAT_GROUP);

                summaryNotification =
                        new NotificationCompat.Builder(context, context.getString(R.string.notif_channel_chat))
                                .setContentTitle("Title")
                                //set content text to support devices running API level < 24
//                                .setContentText("You have new chat message(s)")
                                .setSmallIcon(R.drawable.ic_notification_chat)
                                .setStyle(new NotificationCompat.BigTextStyle())
                                //specify which group this notification belongs to
                                .setGroup(Constants.NOTIFICATION_CHAT_GROUP)
                                //set this notification as the summary for the group
                                .setGroupSummary(true)
                                .build();

            }


            // TODO: 5/11/2018    notifications are simply updated. only one in tray [OBSOLETE]
//			int _id = (!Utils.isStringValid(id) ? (int) when : id.hashCode());
//
//          // id is always the hash extracted from the sender name
            id = parts[0].hashCode();
            mNotificationManager.notify(id, notification);

            if (summaryNotification != null)
                mNotificationManager.notify(Constants.NOTIFICATION_CHAT_ID, summaryNotification);
        }
    }


	private static class WakeLocker {
		private static PowerManager.WakeLock wakeLock;

		static void acquire(Context context) {
			if (wakeLock != null && wakeLock.isHeld())
				wakeLock.release();

			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			if (pm != null) {
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK |
						PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, LOG_TAG + "WakeLock");
				wakeLock.acquire(2000);
			}
		}

		static void release() {
			try {
				if (wakeLock != null && wakeLock.isHeld())
					wakeLock.release();
				wakeLock = null;
			}
			catch (Exception e) {
				LogUtils.d("WAKE_LOCK Exception", e.getMessage());
			}
		}
	}

}
