/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.InteractionShare;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 9/30/2017.
 */
public class InteractionSharesAdapter extends RecyclerView.Adapter<InteractionSharesAdapter.InterSharesVH> {

	private List<InteractionShare> items;

	public InteractionSharesAdapter(List<InteractionShare> items) {
		this.items = items;
	}

	@NonNull
	@Override
	public InterSharesVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new InterSharesVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_interact_hearts_shares, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull InterSharesVH holder, int position) {
		InteractionShare ip = items.get(position);
		holder.setInteraction(ip);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		if (items != null && !items.isEmpty()) {
			if (items.get(position) != null)
				return items.get(position).hashCode();
		}

		return 0;
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link InteractionShare}.
	 */
	static class InterSharesVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePic;
		private final TextView userName;

		final View heartsLayout;

		private OnShareActionListener mListener;
		private InteractionShare currentShare;

		InterSharesVH(View itemView) {
			super(itemView);

			profilePic = itemView.findViewById(R.id.user_profile_pic);
			profilePic.setOnClickListener(this);
			userName = itemView.findViewById(R.id.user_name);

			heartsLayout = itemView.findViewById(R.id.rating_hearts);

			if (Utils.isContextValid(itemView.getContext()) && itemView.getContext() instanceof InteractionCommentsAdapter.OnCommentActionListener) {
				mListener = ((InteractionSharesAdapter.OnShareActionListener) itemView.getContext());
			}
		}

		protected void setInteraction(InteractionShare interaction) {

			if (interaction == null)
				return;

			currentShare = interaction;

			if (Utils.isStringValid(interaction.getAuthorUrl())) {
				MediaHelper.loadProfilePictureWithPlaceholder(profilePic.getContext(),
						interaction.getAuthorUrl(), profilePic);
			}
			else profilePic.setImageResource(R.drawable.ic_profile_placeholder);

			userName.setText(interaction.getAuthor());

			heartsLayout.setVisibility(View.GONE);
		}

		@Override
		public void onClick(View view) {
			if (mListener != null) {
				int id = view.getId();
				switch (id) {
					case R.id.user_profile_pic:
						mListener.goToUserProfile(currentShare.getAuthorId());
						break;
				}
			}
		}
	}

	public interface OnShareActionListener {
		void goToUserProfile(@NonNull String authorId);
	}
}
