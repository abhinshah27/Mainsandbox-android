/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 3/22/2018.
 */
public class SettingsSingleCircleAdapter extends RecyclerView.Adapter<SettingsSingleCircleAdapter.MemberCircleVH> {


	private List<HLUserGeneric> items;
	private BasicAdapterInteractionsListener mListener;

	private boolean isInnerCircle, wantsFilter, isFamily;


	public SettingsSingleCircleAdapter(List<HLUserGeneric> items, boolean isInnerCircle, boolean wantsFilter,
	                                   boolean isFamily, BasicAdapterInteractionsListener mListener) {
		this.items = items;
		this.isInnerCircle = isInnerCircle;
		this.wantsFilter = wantsFilter;
		this.isFamily = isFamily;
		this.mListener = mListener;
	}

	@NonNull
	@Override
	public MemberCircleVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new MemberCircleVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_settings_circle_member, parent, false), mListener);
	}

	@Override
	public void onBindViewHolder(@NonNull MemberCircleVH holder, int position) {
		HLUserGeneric member = items.get(position);
		holder.setMember(member, isInnerCircle, wantsFilter, isFamily);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}


	@Override
	public long getItemId(int position) {
		HLUserGeneric user = items.get(position);
		if (user != null)
			return user.hashCode();
		return super.getItemId(position);
	}



	static class MemberCircleVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePicture;
		private final TextView name;
		private final View icon;

		private final BasicAdapterInteractionsListener mListener;

		private HLUserGeneric currentMember;


		MemberCircleVH(View itemView, BasicAdapterInteractionsListener mListener) {
			super(itemView);

			profilePicture = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.name);
			icon = itemView.findViewById(R.id.check);

			this.mListener = mListener;

			icon.setOnClickListener(this);
			itemView.setOnClickListener(this);
		}


		private void setMember(HLUserGeneric member, boolean isInnerCircle, boolean wantsFilter, boolean isFamily) {
			if (member != null) {

				currentMember = member;

				if (Utils.isStringValid(member.getAvatarURL()))
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(), member.getAvatarURL(), profilePicture);
				else
					profilePicture.setImageResource(R.drawable.ic_profile_placeholder);
				name.setText(member.getCompleteName());

				icon.setActivated(!isInnerCircle || !wantsFilter);

				// TODO: 5/25/2018    TEMPORARILY DISABLES REMOVE ACTION FROM CIRCLE FAMILY
				icon.setVisibility(isFamily ? View.GONE : View.VISIBLE);
			}
		}

		@Override
		public void onClick(View v) {
			mListener.onItemClick(currentMember, v);
		}
	}

}
