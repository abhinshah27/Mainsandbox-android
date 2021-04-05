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
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 3/28/2018.
 */
public class Settings2StepContactsAdapter extends RecyclerView.Adapter<Settings2StepContactsAdapter.MemberCircleVH> {


	private List<HLUserGeneric> items;
	private BasicAdapterInteractionsListener mListener;

	private ArrayList<String> filters;

	private boolean isInnerCircle;


	public Settings2StepContactsAdapter(List<HLUserGeneric> items, boolean isInnerCircle,
	                                    @Nullable ArrayList<String> filters,
	                                    BasicAdapterInteractionsListener mListener) {
		this.items = items;
		this.isInnerCircle = isInnerCircle;
		this.mListener = mListener;
		this.filters = filters;
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
		holder.setMember(member, isInnerCircle);
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

	public void applyFilter() {
		final List<HLUserGeneric> list = new ArrayList<>();

		int posToRemove = -1;
		for (int i = 0; i < items.size(); i++) {
			HLUserGeneric user = items.get(i);
			if (filters == null || (user != null && Utils.isStringValid(user.getId()) &&
					!filters.contains(user.getId()))) {
				list.add(user);
			}
			else
				posToRemove = i;
		}

//		Stream.of(items).forEach(new Consumer<HLUserGeneric>() {
//			@Override
//			public void accept(HLUserGeneric userGeneric) {
//				if (filters == null || (userGeneric != null && Utils.isStringValid(userGeneric.getId()) &&
//						!filters.contains(userGeneric.getId()))) {
//					list.add(userGeneric);
//				}
//			}
//		});

		items.clear();
		items.addAll(list);
		if (posToRemove > -1)
			notifyItemRemoved(posToRemove);
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

			itemView.setOnClickListener(this);
		}


		private void setMember(HLUserGeneric member, boolean isInnerCircle) {
			if (member != null) {

				currentMember = member;

				if (Utils.isStringValid(member.getAvatarURL())) {
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
							member.getAvatarURL(), profilePicture);
				}
				else profilePicture.setImageResource(R.drawable.ic_profile_placeholder);

				name.setText(member.getCompleteName());

				icon.setActivated(!isInnerCircle);
			}
		}

		@Override
		public void onClick(View v) {
			mListener.onItemClick(currentMember);
		}
	}

}
