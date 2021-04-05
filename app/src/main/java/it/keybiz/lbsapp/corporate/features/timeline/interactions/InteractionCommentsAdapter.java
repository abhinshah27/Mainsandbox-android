/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.InteractionComment;
import it.keybiz.lbsapp.corporate.models.InteractionPost;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * This is the adapter of the {@link RecyclerView} dedicated to display the {@link InteractionPost}
 * objects of type COMMENT of a {@link it.keybiz.lbsapp.corporate.models.Post}.
 *
 * @author mbaldrighi on 9/30/2017.
 */
public class InteractionCommentsAdapter extends RecyclerView.Adapter<InteractionCommentsAdapter.InterCommentsVH> {

	private static final int TYPE_COMMENT = 0;
	private static final int TYPE_SUB_COMMENT = 1;

	private List<InteractionComment> items;

	private final HLUser user;

	private Post post;

	public InteractionCommentsAdapter(List<InteractionComment> items, HLUser user, Post post) {
		this.items = items;
		this.user = user;
		this.post = post;
	}

	@NonNull
	@Override
	public InterCommentsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		@LayoutRes int resId;

		switch (viewType) {
			case TYPE_COMMENT:
				resId = R.layout.item_interact_comments;
				break;
			case TYPE_SUB_COMMENT:
				resId = R.layout.item_interact_sub_comment;
				break;
			default:
				resId = R.layout.item_interact_comments;
		}

		return new InterCommentsVH(LayoutInflater.from(parent.getContext())
				.inflate(resId, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull InterCommentsVH holder, int position) {
		InteractionComment ip = items.get(position);
		holder.setInteraction(ip, user, post);
		holder.setDividerVisibility(position != (items.size() - 1));
		if (items.size() > position + 1) {
			holder.setDividerMargins(getItemViewType(position + 1));
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		if (items.get(position) != null)
			return items.get(position).hashCode();

		return 0;
	}

	@Override
	public int getItemViewType(int position) {
		InteractionComment ic = items.get(position);
		if (ic != null)
			return ic.getLevel();
		return super.getItemViewType(position);
	}

	public HLUser getUser() {
		return user;
	}


	public void setPost(Post post) {
		this.post = post;
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link InteractionComment}.
	 */
	static class InterCommentsVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePic;
		private final TextView userName;

		final TextView message;
		final TextView commentDetails;
		final View replyBtn;

		final View horDots;
		final View heart;

		final View divider;

		final ViewGroup subCommContainer;

		private InteractionComment currentComment;
		private OnCommentActionListener mListener;

		private static int dpToPx5;
		private static int dpToPx6;
		private static int dpToPx10;

		InterCommentsVH(View itemView) {
			super(itemView);

			profilePic = itemView.findViewById(R.id.user_profile_pic);
			userName = itemView.findViewById(R.id.user_name);

			message = itemView.findViewById(R.id.text);
			commentDetails = itemView.findViewById(R.id.comment_details);
			replyBtn = itemView.findViewById(R.id.reply_btn);

			horDots = itemView.findViewById(R.id.horizontal_dots);
			heart = itemView.findViewById(R.id.heart);
			heart.setSelected(false);

			divider = itemView.findViewById(R.id.divider);

			subCommContainer = itemView.findViewById(R.id.sub_comment_container);

			replyBtn.setOnClickListener(this);
			horDots.setOnClickListener(this);
			heart.setOnClickListener(this);
			profilePic.setOnClickListener(this);

			Resources res = heart.getResources();
			dpToPx5 = Utils.dpToPx(5f, res);
			dpToPx6 = Utils.dpToPx(6f, res);
			dpToPx10 = Utils.dpToPx(10f, res);

			if (Utils.isContextValid(itemView.getContext()) && itemView.getContext() instanceof OnCommentActionListener) {
				mListener = ((OnCommentActionListener) itemView.getContext());
			}
		}

		void setInteraction(InteractionComment interaction, HLUser user, Post post) {
			if (interaction == null)
				return;

			currentComment = interaction;

			if (Utils.isStringValid(interaction.getAuthorUrl())) {
				MediaHelper.loadProfilePictureWithPlaceholder(profilePic.getContext(),
						interaction.getAuthorUrl(), profilePic);
			}
			else profilePic.setImageResource(R.drawable.ic_profile_placeholder);

			userName.setText(interaction.getAuthor());

			message.setText(interaction.getMessage());

			StringBuilder s = new StringBuilder();
			s.append(InteractionPost.getTimeStamp(commentDetails.getResources(), interaction.getCreationDate()));
			s.append(".");

			// disables hearts
//			if (interaction.getTotHearts() > 0) {
//				s.append(" ");
//				s.append(String.format(
//						Locale.getDefault(),
//						commentDetails.getContext().getResources()
//								.getQuantityString(R.plurals.plural_hearts_uc, interaction.getTotHearts()),
//						interaction.getTotHearts()));
//				s.append(".");
//			}

			commentDetails.setText(s);
			boolean showReply = post != null && post.canCommentPost();
			replyBtn.setVisibility(interaction.isSubComment() || !showReply ? View.GONE : View.VISIBLE);

			// INFO: 2/5/19   horDots is now always visible. no need to handle padding and margins
//			if (mListener != null && horDots != null && post != null) {
//				boolean userPostAuthor = post.isActiveUserAuthor(mListener.getRealm());
//				boolean userCommentAuthor = interaction.isActingUserCommentAuthor(mListener.getUser());
//				horDots.setVisibility((interaction.isSubComment() && !userPostAuthor && !userCommentAuthor) ? View.GONE : View.VISIBLE);
//
//				heart.setPadding(
//						dpToPx10,
//						dpToPx10,
//						horDots.getVisibility() == View.GONE ? dpToPx5 : dpToPx10,
//						dpToPx10
//				);
//				((LinearLayout.LayoutParams) heart.getLayoutParams()).setMarginEnd(horDots.getVisibility() == View.GONE ? dpToPx6 : 0);
//			}

			this.heart.setVisibility(interaction.isActingUserCommentAuthor(user) ? View.GONE : View.VISIBLE);
			this.heart.setSelected(interaction.isYouLiked());
		}

		void setDividerMargins(int typePlusOne) {
			int start = Utils.dpToPx(typePlusOne == TYPE_SUB_COMMENT ? 35f : 0, divider.getResources());
			((LinearLayout.LayoutParams) divider.getLayoutParams()).setMarginStart(start);
		}

		void setDividerVisibility(boolean visible) {
			this.divider.setVisibility(visible ? View.VISIBLE : View.GONE);
		}


		@Override
		public void onClick(View view) {
			if (mListener != null) {

				if (mListener.isPanelOpen()) {
					mListener.closePanel();
				}
				else {
					int id = view.getId();
					switch (id) {
						case R.id.user_profile_pic:
							mListener.goToUserProfile(currentComment.getAuthorId());
							break;
						case R.id.heart:
							mListener.sendLikeToComment(currentComment);
							break;
						case R.id.horizontal_dots:
							mListener.openOptionsSheet(currentComment);
							break;
						case R.id.reply_btn:
							mListener.replyToComment(currentComment);
							break;
					}
				}
			}
		}
	}

	public interface OnCommentActionListener {
		void openOptionsSheet(@NonNull InteractionComment comment);
		void goToUserProfile(@NonNull String authorId);
		void sendLikeToComment(@NonNull InteractionComment comment);
		void replyToComment(@NonNull InteractionComment comment);
		void reportComment(@NonNull InteractionComment comment);
		HLUser getUser();
		Realm getRealm();
		boolean isPanelOpen();
		void closePanel();
	}
}
