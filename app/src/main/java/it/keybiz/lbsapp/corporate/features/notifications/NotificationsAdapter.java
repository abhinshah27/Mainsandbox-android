/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.notifications;

import android.graphics.Typeface;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.HLNotifications;
import it.keybiz.lbsapp.corporate.models.Notification;
import it.keybiz.lbsapp.corporate.models.enums.ActionTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.NotificationTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.RequestsStatusEnum;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 12/11/2017.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationVH> {

	public static final String LOG_TAG = NotificationsAdapter.class.getCanonicalName();

	private static StyleSpan boldSpan;

	private final int TYPE_NOTIFICATION = 0;
	private final int TYPE_REQUEST = 1;

	private List<Notification> items;

	private OnNotificationItemClickListener mListener;

	public NotificationsAdapter(List<Notification> items) {
		this.items = items;
		boldSpan = new StyleSpan(Typeface.BOLD);
	}

	@NonNull
	@Override
	public NotificationVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		@LayoutRes int layout = R.layout.item_notification;
		if (viewType == TYPE_REQUEST)
			layout = R.layout.item_notification_request;

		return new NotificationVH(LayoutInflater.from(parent.getContext())
				.inflate(layout, parent, false));
	}

	@Override
	public void onBindViewHolder(NotificationVH holder, int position) {
		if (items != null && !items.isEmpty()) {
			Notification ip = items.get(position);
			holder.setNotification(ip);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		if (items != null && !items.isEmpty()) {
			Notification n = items.get(position);
			if (n != null) {
				return n.isRequest() ? TYPE_REQUEST : TYPE_NOTIFICATION;
			}
		}
		return super.getItemViewType(position);
	}

	@Override
	public long getItemId(int position) {
		if (items != null && !items.isEmpty()) {
			return items.get(position).hashCode();
		}

		return super.getItemId(position);
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link it.keybiz.lbsapp.corporate.models.Notification}.
	 */
	class NotificationVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final View itemView;
		private final ImageView profilePic, icon;
		private final TextView name, text, stamp;

		private final View buttonsSection;
		private final TextView actionResult;

		private Notification currentNotification;

		NotificationVH(View itemView) {
			super(itemView);

			this.itemView = itemView;
			profilePic = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.notification_name);
			text = itemView.findViewById(R.id.notification_text);
			stamp = itemView.findViewById(R.id.notification_stamp);
			icon = itemView.findViewById(R.id.notification_icon);

			buttonsSection = itemView.findViewById(R.id.notification_action_btns);
			View allowBtn = itemView.findViewById(R.id.btn_allow);
			View denyBtn = itemView.findViewById(R.id.btn_deny);
			actionResult = itemView.findViewById(R.id.notification_action_result);

			profilePic.setOnClickListener(this);
			if (allowBtn != null)
				allowBtn.setOnClickListener(this);
			if (denyBtn != null)
				denyBtn.setOnClickListener(this);
		}

		protected void setNotification(Notification notification) {
			if (notification == null)
				return;

			currentNotification = notification;

			itemView.setBackgroundColor(Utils.getColor(itemView.getContext(),
					notification.isRead() ? R.color.white : R.color.divider_on_white));
			itemView.setOnClickListener(this);

			if (Utils.isStringValid(notification.getAvatarURL()))
				MediaHelper.loadProfilePictureWithPlaceholder(profilePic.getContext(), notification.getAvatarURL(), profilePic);
			else
				profilePic.setImageResource(R.drawable.ic_profile_placeholder);

			name.setText(notification.getName());
			text.setText(notification.getText().replaceAll("\\\\n", "").trim());

			try {
				icon.setImageResource(NotificationTypeEnum.getNotificationIcon(notification.getType()));
			} catch (IllegalArgumentException e) {
				LogUtils.e(LOG_TAG, e.getMessage(), e);
			}

			stamp.setText(HLNotifications.getTimeStamp(stamp.getResources(), notification.getDate()));

			if (notification.isRequest()) {
				buttonsSection.setVisibility(notification.getStatus() != RequestsStatusEnum.PENDING ? View.GONE : View.VISIBLE);
				actionResult.setVisibility(notification.getStatus() != RequestsStatusEnum.PENDING ? View.VISIBLE : View.GONE);

				String result = "";
				if (notification.getStatus() == RequestsStatusEnum.AUTHORIZED)
					result = actionResult.getContext().getString(R.string.action_request_authorized);
				else if (notification.getStatus() == RequestsStatusEnum.DECLINED)
					result = actionResult.getContext().getString(R.string.action_request_denied);
				actionResult.setText(actionResult.getContext().getString(R.string.notification_action_result, result));
			}
		}

		@Override
		public void onClick(View view) {

			switch (view.getId()) {
				case R.id.profile_picture:
					// TODO: 12/13/2017    CORRECT?
//						mListener.openProfileFromNotification();
					break;

				case R.id.btn_allow:
					view.setSelected(true);
					mListener.allowDenyRequest(currentNotification, ActionTypeEnum.ALLOW, view);
					break;
				case R.id.btn_deny:
					view.setSelected(true);
					mListener.allowDenyRequest(currentNotification, ActionTypeEnum.DENY, view);
					break;


				default:
					mListener.onNotificationItemClick(currentNotification);
			}
		}
	}


	public void setListener(OnNotificationItemClickListener mListener) {
		this.mListener = mListener;
	}

	public interface OnNotificationItemClickListener {
		void onNotificationItemClick(@NonNull Notification notification);
		void allowDenyRequest(@NonNull Notification notification, @NonNull ActionTypeEnum type,
		                      @NonNull View view);
	}

}
