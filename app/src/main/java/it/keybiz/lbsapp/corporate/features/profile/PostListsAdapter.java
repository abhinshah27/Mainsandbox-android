/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.PostList;
import it.keybiz.lbsapp.corporate.models.enums.MemoryColorEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 12/29/2017.
 */
public class PostListsAdapter extends RecyclerView.Adapter<PostListsAdapter.ListVH> {

	private List<PostList> items;

	private OnDiaryActionListener mListener;

	private boolean mIsUser;

	public PostListsAdapter(List<PostList> items, OnDiaryActionListener listener, boolean isUser) {
		this.items = items;
		this.mListener = listener;
		this.mIsUser = isUser;
	}

	@NonNull
	@Override
	public ListVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ListVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_my_diary_main, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ListVH holder, int position) {
		PostList list = items.get(position);
		holder.setPostList(list, position);
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
	 * {@link View} objects of a {@link PostList}.
	 */
	class ListVH extends RecyclerView.ViewHolder {

		private final TextView listName;
		private final HorizontalScrollView postsScrollView;
		private final ViewGroup postsContainer;
		private final TextView noResult;

		ListVH(View itemView) {
			super(itemView);

			listName = itemView.findViewById(R.id.list_name);
			postsScrollView = itemView.findViewById(R.id.scroll_view);
			postsContainer = itemView.findViewById(R.id.card_container);
			noResult = itemView.findViewById(R.id.no_result);

			if (Utils.isContextValid(itemView.getContext()) &&
					itemView.getContext() instanceof OnDiaryActionListener) {
				mListener = ((OnDiaryActionListener) itemView.getContext());
			}
		}

		void setPostList(final PostList list, int position) {
			if (list == null)
				return;

			listName.setText(list.getNameToDisplay());

			if (postsScrollView != null) {
				postsScrollView.setTag(null);
				postsScrollView.setTag(position);
				mListener.saveScrollView(position, postsScrollView);

				List<Post> posts = list.getPosts();
				final String id = list.getName();
				if (posts.isEmpty()) {
					if (mIsUser && id != null && (id.equals(Constants.DIARY_LIST_ID_PROFILE_PIC) || id.equals(Constants.DIARY_LIST_ID_WALL_PIC))) {
						postsContainer.removeAllViews();
						postsScrollView.setVisibility(View.VISIBLE);
						noResult.setVisibility(View.GONE);

						View v = LayoutInflater.from(postsContainer.getContext())
								.inflate(R.layout.item_diary_add_picture, postsContainer, false);

						if (v != null) {
							v.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									mListener.handlePictures(id, view);
								}
							});
							postsContainer.addView(v);
						}
					}
					else {
						postsScrollView.setVisibility(View.GONE);
						noResult.setText(R.string.no_posts_in_list);
						noResult.setVisibility(View.VISIBLE);
					}
				}
				else {
					postsScrollView.setVisibility(View.VISIBLE);
					noResult.setVisibility(View.GONE);

					if (postsContainer != null) {
						postsContainer.removeAllViews();

						Context context = postsContainer.getContext();
						for (int i = 0; i < posts.size(); i++) {
							final Post p = posts.get(i);
							if (p != null && Utils.isContextValid(context)) {
								View v = LayoutInflater.from(context)
										.inflate(p.getRightDiaryLayoutItem(), postsContainer, false);

								if (v != null) {
									TextView tv = v.findViewById(R.id.text);
									if (p.isPicturePost() || p.isWebLinkPost()) {
										ImageView iv = v.findViewById(R.id.post_preview);
										if (iv != null) {
											if (Utils.hasLollipop())
												MediaHelper.loadPictureWithGlide(iv.getContext(), p.getContent(false), iv);
											else
												MediaHelper.roundPictureCorners(iv, p.getContent(false));
										}
									}
									else if (p.isVideoPost()) {
										ImageView iv = v.findViewById(R.id.video_view_thumbnail);
										if (iv != null) {
											if (Utils.hasLollipop())
												MediaHelper.loadPictureWithGlide(iv.getContext(), p.getVideoThumbnail(), iv);
											else
												MediaHelper.roundPictureCorners(iv, p.getVideoThumbnail());
										}
									}

									// show caption/message only on text posts preview
									if (p.isTextPost()) {
										tv.setText(p.getCaption());
										tv.setTextColor(MemoryColorEnum.getColor(mListener.getResources(), p.getTextColor()));
										v.setBackgroundColor(p.getBackgroundColor(mListener.getResources()));
									}

									if (i == 0) {
										LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) v.getLayoutParams());
										lp.setMarginStart(Utils.dpToPx(20f, v.getResources()));
										v.setLayoutParams(lp);
									}

									v.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View view) {
											performAction(list.getName(), p.getId());
										}
									});

									postsContainer.addView(v);
								}
							}
						}

						if (list.hasMoreData()) {
							View v = LayoutInflater.from(context)
									.inflate(R.layout.item_diary_more_post, postsContainer, false);

							if (v != null) {
								v.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View view) {
										performAction(list.getName(), null);
									}
								});

								LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) v.getLayoutParams());
								lp.setMarginEnd(Utils.dpToPx(20f, v.getResources()));
								v.setLayoutParams(lp);

								postsContainer.addView(v);
							}
						}
					}
				}
			}

			mListener.restoreScrollView(position);
		}

		private void performAction(@NonNull String listName, @Nullable String postId) {
			mListener.goToTimeline(listName, postId);
		}

	}


	public interface OnDiaryActionListener {
		void goToTimeline(@NonNull String listName, @Nullable String postId);
		HLUser getUser();
		void handlePictures(String id, View anchor);
		void saveScrollView(int position, HorizontalScrollView scrollView);
		void restoreScrollView(int position);
		Resources getResources();
	}

}
