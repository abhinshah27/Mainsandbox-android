/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.PostList;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 12/29/2017.
 */
public class WishPostSelectionAdapter extends RecyclerView.Adapter<WishPostSelectionAdapter.PostTileVH> {

	private List<Post> items;

	private OnPostTileActionListener mListener;

	public WishPostSelectionAdapter(List<Post> items, OnPostTileActionListener listener) {
		this.items = items;
		this.mListener = listener;
	}

	@Override
	public PostTileVH onCreateViewHolder(ViewGroup parent, int viewType) {
		return new PostTileVH(LayoutInflater.from(parent.getContext())
				.inflate(viewType, parent, false));
	}

	@Override
	public void onBindViewHolder(PostTileVH holder, int position) {
		Post post = items.get(position);
		holder.setPost(post, position);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).hashCode();
	}

	@Override
	public int getItemViewType(int position) {
		Post p = items.get(position);
		if (p != null)
			return p.getRightDiaryLayoutItem();

		return super.getItemViewType(position);
	}

	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link PostList}.
	 */
	class PostTileVH extends RecyclerView.ViewHolder {

		private final View itemView;

		private final TextView caption;
		private final TextView date;
		private final ImageView picture;
		private final View iconNoMessage;

		private final View overlay;


		PostTileVH(View itemView) {
			super(itemView);

			this.itemView = itemView;

			caption = itemView.findViewById(R.id.text);
			picture = itemView.findViewById(R.id.post_preview);
			iconNoMessage = itemView.findViewById(R.id.icon_no_message);
			date = itemView.findViewById(R.id.date);
			overlay = itemView.findViewById(R.id.selection_overlay);
		}

		void setPost(final Post p, final int position) {
			if (p == null)
				return;

			if (itemView != null) {

				int margin = Utils.dpToPx(5f, itemView.getResources());
				RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) itemView.getLayoutParams();
				lp.setMargins(margin, margin, margin, margin);

				itemView.setLayoutParams(lp);

				if (date != null && p.getCreationDate() != null) {
					SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
					date.setText(sdf.format(p.getCreationDate()));
					date.setVisibility(View.VISIBLE);
				}

				if (p.isPicturePost() || p.isWebLinkPost()) {
					if (picture != null) {
						if (Utils.hasLollipop())
							MediaHelper.loadPictureWithGlide(picture.getContext(), p.getContent(false), picture);
						else
							MediaHelper.roundPictureCorners(picture, p.getContent(false));
					}
				}
				else if (p.isVideoPost()) {
					ImageView iv = itemView.findViewById(R.id.video_view_thumbnail);
					if (iv != null) {
						if (Utils.hasLollipop())
							MediaHelper.loadPictureWithGlide(iv.getContext(), p.getVideoThumbnail(), iv);
						else
							MediaHelper.roundPictureCorners(iv, p.getVideoThumbnail());
					}
				}
				else if (p.isAudioPost()) {
					iconNoMessage.setVisibility(View.VISIBLE);
				}

				// show caption/message only on text posts preview
				if (p.isTextPost())
					caption.setText(p.getCaption());

				overlay.setVisibility(mListener.isPostSelected(p.getId()) ? View.VISIBLE : View.GONE);

				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						mListener.selectDeselect(p.getId(), position);
					}
				});

				itemView.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						mListener.openPostPreview(v, p);
						return true;
					}
				});
			}
		}
	}


	public interface OnPostTileActionListener {
		void openPostPreview(View view, Post post);
		void selectDeselect(String postId, int position);
		boolean isPostSelected(String postId);
	}

}
