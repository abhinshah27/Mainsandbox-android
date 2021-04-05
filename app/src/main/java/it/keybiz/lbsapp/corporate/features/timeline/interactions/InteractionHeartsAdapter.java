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
import it.keybiz.lbsapp.corporate.models.InteractionHeart;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author Massimo on 9/30/2017.
 */
public class InteractionHeartsAdapter extends RecyclerView.Adapter<InteractionHeartsAdapter.InterHeartsVH> {

	private List<InteractionHeart> items;

	public InteractionHeartsAdapter(List<InteractionHeart> items) {
		this.items = items;
	}

	@Override
	public InterHeartsVH onCreateViewHolder(ViewGroup parent, int viewType) {
		return new InterHeartsVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_interact_hearts_shares, parent, false));
	}

	@Override
	public void onBindViewHolder(InterHeartsVH holder, int position) {
		InteractionHeart ip = items.get(position);
		holder.setInteraction(ip);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).hashCode();
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link InteractionHeart}.
	 */
	static class InterHeartsVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePic;
		private final TextView userName;

		final ImageView h1, h2, h3, h4, h5;
		final View heartsLayout;

		private OnHeartActionListener mListener;
		private InteractionHeart currentHeart;

		InterHeartsVH(View itemView) {
			super(itemView);

			profilePic = itemView.findViewById(R.id.user_profile_pic);
			profilePic.setOnClickListener(this);
			userName = itemView.findViewById(R.id.user_name);

			heartsLayout = itemView.findViewById(R.id.rating_hearts);
			h1 = itemView.findViewById(R.id.rating_h1);
			h2 = itemView.findViewById(R.id.rating_h2);
			h3 = itemView.findViewById(R.id.rating_h3);
			h4 = itemView.findViewById(R.id.rating_h4);
			h5 = itemView.findViewById(R.id.rating_h5);

			if (Utils.isContextValid(itemView.getContext()) && itemView.getContext() instanceof InteractionCommentsAdapter.OnCommentActionListener) {
				mListener = ((InteractionHeartsAdapter.OnHeartActionListener) itemView.getContext());
			}
		}

		protected void setInteraction(InteractionHeart interaction) {

			if (interaction == null)
				return;

			currentHeart = interaction;

			if (Utils.isStringValid(interaction.getAuthorUrl())) {
				MediaHelper.loadProfilePictureWithPlaceholder(profilePic.getContext(),
						interaction.getAuthorUrl(), profilePic);
			}
			else profilePic.setImageResource(R.drawable.ic_profile_placeholder);

			userName.setText(interaction.getAuthor());

			int cnt = interaction.getCount();
			heartsLayout.setVisibility( cnt > 0 ? View.VISIBLE :View.GONE);
			h2.setVisibility( cnt > 1 ? View.VISIBLE :View.INVISIBLE);
			h3.setVisibility( cnt > 2 ? View.VISIBLE :View.INVISIBLE);
			h4.setVisibility( cnt > 3 ? View.VISIBLE :View.INVISIBLE);
			h5.setVisibility( cnt > 4 ? View.VISIBLE :View.INVISIBLE);
		}

		@Override
		public void onClick(View view) {
			if (mListener != null) {
				int id = view.getId();
				switch (id) {
					case R.id.user_profile_pic:
						mListener.goToUserProfile(currentHeart.getAuthorId());
						break;
				}
			}
		}
	}

	public interface OnHeartActionListener {
		void goToUserProfile(@NonNull String authorId);
	}
}
