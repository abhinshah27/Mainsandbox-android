/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.models.enums.NotificationNatureEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Singleton class containing and handling all the operations on and about {@link Notification} objects.
 * @author mbaldrighi on 12/5/2017.
 */
public class HLNotifications {

	public static final String LOG_TAG = HLNotifications.class.getCanonicalName();

	private static HLNotifications instance = null;
	private static final Object mutex = new Object();

	private Map<String, Notification> notificationsMap;
	private Map<String, Notification> requestsMap;

	private Set<String> requestUUIDs;

	private int unreadCount = -1;

	public static HLNotifications getInstance() {
		if (instance == null) {
			synchronized (mutex){
				if (instance == null)
					instance = new HLNotifications();
			}
		}

		return instance;
	}

	private HLNotifications() {
		init();
	}

	/**
	 * This method initializes vars like maps and lists. It is called at the singleton instantiation.
	 */
	private void init() {
		notificationsMap = new ConcurrentHashMap<>();
		requestsMap = new ConcurrentHashMap<>();
		requestUUIDs = new ConcurrentSkipListSet<>();

		unreadCount = 0;
	}

	public void setNotifications(String array, boolean reset) throws JSONException {
		if (Utils.isStringValid(array)) {

			/* boolean forced TRUE until implementation of pagination method */
			setNotifications(new JSONArray(array), false);
		}
	}

	public void setNotifications(JSONArray jsonArray) throws JSONException {
		setNotifications(jsonArray, false);
	}

	public void setRequests(JSONArray jsonArray) throws JSONException {
		setNotifications(jsonArray, true);
	}

	private void setNotifications(JSONArray jsonArray, boolean requests) throws JSONException {
		if (jsonArray != null && jsonArray.length() > 0) {
			JSONObject json = jsonArray.optJSONObject(0);
			if (json != null && json.has("toRead"))
				unreadCount = json.optInt("toRead");
			JSONArray notifs = jsonArray.optJSONObject(0).optJSONArray("notifications");
			if (notifs != null && notifs.length() > 0) {
				for (int i = 0; i < notifs.length(); i++) {
					JSONObject j = notifs.getJSONObject(i);
					if (j != null) {
						String id = j.optString("notificationID");
						if (Utils.isStringValid(id)) {
							Notification n = new Notification().returnUpdatedNotification(j);
							if (!requests) {
								if (notificationsMap.containsKey(id)) {
									Notification ood = notificationsMap.get(id);
									ood.returnUpdatedNotification(n);
								} else {
									notificationsMap.put(id, n);
								}
							}
							else if (n.isRequest()) {

								// 1) fetches from notifications in order for it to be the same object
								// 2) if not in notifications, fetch from requests
								// 3) anyways, put it back into requests

								if (notificationsMap.containsKey(id)) {
									Notification ood = notificationsMap.get(id);
									n = ood.returnUpdatedNotification(n);
								}
								else if (requestsMap.containsKey(id)) {
									Notification ood = requestsMap.get(id);
									ood.returnUpdatedNotification(n);
									return;
								}

								requestsMap.put(id, n);
							}
						}
					}
				}
			}
		}
	}


	public void resetCollectionsForSwitch() {
		init();
	}


	public Notification getNotification(@NonNull String id) {
		if (notificationsMap != null && !notificationsMap.isEmpty()) {
			return notificationsMap.get(id);
		}

		return null;
	}

	public Collection<Notification> getNotifications() {
		if (notificationsMap != null)
			return notificationsMap.values();

		return new ArrayList<>();
	}

	public int getNotificationsCount() {
		if (notificationsMap != null)
			return notificationsMap.size();

		return 0;
	}

	public Collection<Notification> getRequests() {
		if (requestsMap != null)
			return requestsMap.values();

		return new ArrayList<>();
	}

	public int getRequestsCount() {
		if (requestsMap != null)
			return requestsMap.size();

		return 0;
	}

	public List<Notification> getSortedNotifications() {
		List<Notification> res = new ArrayList<>(getNotifications());
		if (!res.isEmpty()) {
			Collections.sort(res);
		}

		return res;
	}

	public List<Notification> getSortedRequests() {
		List<Notification> res = new ArrayList<>(getRequests());
		if (!res.isEmpty()) {
			Collections.sort(res);
		}
		return res;
	}

	public List<Notification> getRequestNotificationsLocal() {
		return Stream.of(getSortedNotifications()).filter(new Predicate<Notification>() {
			@Override
			public boolean test(Notification notification) {
				return notification.isRequest();
			}
		}).collect(Collectors.toList());
	}

	public int getUnreadCount(boolean useInt) {
		if (!useInt) {
			List<Notification> res = Stream.of(getSortedNotifications()).filter(new Predicate<Notification>() {
				@Override
				public boolean test(Notification notification) {
					return !notification.isRead();
				}
			}).collect(Collectors.toList());

			if (res != null)
				return res.size();
			else
				return -1;
		}
		else return unreadCount;
	}

	public void updateUnreadCount() {
		--unreadCount;
	}

	public void callForNotifications(HLActivity activity, OnMissingConnectionListener connListener,
	                                 @NonNull String userId, boolean fromLoadMore) {
		callForNotifications(activity, connListener, userId, NotificationNatureEnum.NOTIFICATION, false, fromLoadMore);
	}

	private void callForNotifications(HLActivity activity, OnMissingConnectionListener connListener,
	                                  @NonNull String userId, NotificationNatureEnum type, boolean onlyRequests,
	                                  boolean fromLoadMore) {
		Object[] result = null;
		try {
			result = HLServerCalls.getNotifications(userId, fromLoadMore ? getRightPageId(type) : -1, onlyRequests);
		} catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}

		HLRequestTracker.getInstance(((LBSLinkApp) activity.getApplication())).handleCallResult(connListener, activity, result);
	}

	public void callForNotifRequests(HLActivity activity, OnMissingConnectionListener connListener,
	                                 @NonNull String userId, boolean fromLoadMore) {
		callForNotifications(activity, connListener, userId, NotificationNatureEnum.REQUEST, true, fromLoadMore);
	}

	private int getRightPageId(NotificationNatureEnum type) {
		return (type == NotificationNatureEnum.NOTIFICATION ? getLastPageId() : getLastPageIdRequests()) + 1;
	}

	public int getLastPageId() {
		if (notificationsMap == null)
			notificationsMap = new HashMap<>();

		return (notificationsMap.size() / Constants.PAGINATION_AMOUNT);
	}

	public int getLastPageIdRequests() {
		if (requestsMap == null)
			requestsMap = new HashMap<>();

		return (requestsMap.size() / Constants.PAGINATION_AMOUNT);
	}

	public void addRequestUUID(@NonNull String s) {
		if (requestUUIDs == null)
			requestUUIDs = new ConcurrentSkipListSet<String>();

		requestUUIDs.add(s);
	}

	public void removeRequestUUID(@NonNull String s) {
		if (isAllRequests(s))
			requestUUIDs.remove(s);
	}

	public boolean isAllRequests(@NonNull String s) {
		return requestUUIDs != null && requestUUIDs.contains(s);
	}

	private enum TimeUnit {
		SECONDS, MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS
	}
	public static String getTimeStamp(Resources res, Date creationDate) {
		if (creationDate != null) {
			long l = System.currentTimeMillis() - creationDate.getTime();

			TimeUnit unit = TimeUnit.YEARS;
			if (l < Constants.TIME_UNIT_MINUTE)
				unit = TimeUnit.SECONDS;
			else if (l < Constants.TIME_UNIT_HOUR)
				unit = TimeUnit.MINUTES;
			else if (l < Constants.TIME_UNIT_DAY)
				unit = TimeUnit.HOURS;
			else if (l < Constants.TIME_UNIT_WEEK)
				unit = TimeUnit.DAYS;
			else if (l < Constants.TIME_UNIT_MONTH)
				unit = TimeUnit.WEEKS;
			else if (l < Constants.TIME_UNIT_YEAR)
				unit = TimeUnit.MONTHS;

			switch (unit) {
				case SECONDS:
					return res.getString(R.string.now).toLowerCase();
				case MINUTES:
					int i = (int)(l/Constants.TIME_UNIT_MINUTE);
					return res.getQuantityString(R.plurals.notification_minutes, i, i);
				case HOURS:
					int j = (int)(l/Constants.TIME_UNIT_HOUR);
					return res.getQuantityString(R.plurals.notification_hours, j, j);
				case DAYS:
					int k = (int)(l/Constants.TIME_UNIT_DAY);
					return res.getQuantityString(R.plurals.notification_days, k, k);
				case WEEKS:
					int m = (int)(l/Constants.TIME_UNIT_WEEK);
					return res.getQuantityString(R.plurals.notification_weeks, m, m);
				case MONTHS:
					int n = (int)(l/Constants.TIME_UNIT_MONTH);
					return res.getQuantityString(R.plurals.notification_months, n, n);
				case YEARS:
					int o = (int)(l/Constants.TIME_UNIT_YEAR);
					return res.getQuantityString(R.plurals.notification_years, o, o);

			}
		}

		return null;
	}


	public void clearNotifications() {
		if (notificationsMap != null)
			notificationsMap.clear();
		if (requestsMap != null)
			requestsMap.clear();
		if (requestUUIDs != null)
			requestUUIDs.clear();
	}

}
