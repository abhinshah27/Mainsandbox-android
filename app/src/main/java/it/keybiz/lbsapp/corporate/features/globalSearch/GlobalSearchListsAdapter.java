/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.globalSearch;

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
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.GlobalSearchObject;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.InterestCategory;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.PostList;
import it.keybiz.lbsapp.corporate.models.UsersBundle;
import it.keybiz.lbsapp.corporate.models.enums.GlobalSearchTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.MemoryColorEnum;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 4/10/2018.
 */
public class GlobalSearchListsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final int TYPE_CARDS = 0;
	private final int TYPE_SQUARED = 1;

	private List<GlobalSearchObject> items;

	private OnGlobalSearchActionListener mSearchListener;

	public GlobalSearchListsAdapter(List<GlobalSearchObject> items, OnGlobalSearchActionListener listener) {
		this.items = items;
		this.mSearchListener = listener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		RecyclerView.ViewHolder vh = new PostListVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_my_diary_main, parent, false), mSearchListener);

		if (viewType == TYPE_SQUARED)
			vh = new InterestPeopleContainerVH(LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_global_search_squared, parent, false),
					mSearchListener);

		return vh;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		GlobalSearchObject bundle = items.get(position);
		if (bundle != null) {
			switch (getItemViewType(position)) {
				case TYPE_SQUARED:
					if (bundle.isUsers() || bundle.isInterests()) {
						InterestPeopleContainerVH vHolder = ((InterestPeopleContainerVH) holder);
						vHolder.setItem(bundle.getMainObject());
						vHolder.setObjectType(bundle.getObjectType());

						if (position > 0) {
							int paddingLarge = vHolder.itemView.getResources().getDimensionPixelSize(R.dimen.activity_margin_lg);

							vHolder.itemView.setPadding(
									paddingLarge,
									Utils.dpToPx(10f, vHolder.itemView.getResources()),
									paddingLarge,
									paddingLarge
							);
						}
					}
					break;

				case TYPE_CARDS:
					if (bundle.isPosts()) {
						PostListVH vHolder1 = ((PostListVH) holder);
						vHolder1.setPostList(((PostList) bundle.getMainObject()), bundle.getReturnType(), position);
					}
			}
		}
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
		GlobalSearchObject obj = items.get(position);
		if (obj != null) {
			switch(obj.getUIType()) {
				case SQUARED:
					return TYPE_SQUARED;
				case CARDS:
					return TYPE_CARDS;
			}
		}
		return super.getItemViewType(position);
	}

	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link PostList}.
	 */
	static class PostListVH extends RecyclerView.ViewHolder {

		private final TextView listName;
		private final HorizontalScrollView postsScrollView;
		private final ViewGroup postsContainer;
		private final TextView noResult;

		private String returnTypeString;

		private final OnGlobalSearchActionListener mListener;

		PostListVH(View itemView, OnGlobalSearchActionListener listener) {
			super(itemView);

			listName = itemView.findViewById(R.id.list_name);
			postsScrollView = itemView.findViewById(R.id.scroll_view);
			postsContainer = itemView.findViewById(R.id.card_container);
			noResult = itemView.findViewById(R.id.no_result);

			mListener = listener;
		}

		void setPostList(final PostList list, final String returnType, int position) {
			if (list == null)
				return;

			returnTypeString = returnType;

			listName.setText(list.getNameToDisplay());

			if (postsScrollView != null) {
				postsScrollView.setTag(null);
				postsScrollView.setTag(position);
				mListener.saveScrollView(position, postsScrollView);

				List<Post> posts = list.getPosts();
				if (posts.isEmpty()) {
					postsScrollView.setVisibility(View.GONE);
					noResult.setText(R.string.no_posts_in_list);
					noResult.setVisibility(View.VISIBLE);
				} else {
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
									} else if (p.isVideoPost()) {
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
											mListener.goToTimeline(returnTypeString, p.getId());
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
										mListener.goToTimeline(returnTypeString, null);
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
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link InterestCategory} or {@link it.keybiz.lbsapp.corporate.models.UsersBundle}.
	 */
	class InterestPeopleContainerVH extends RecyclerView.ViewHolder {

		private final View itemView;

		private final TextView listName;
		private final TextView noResult;
		private final GridLayout gridLayout;
		private final TextView viewMore;

		private final OnGlobalSearchActionListener mListener;

		private GlobalSearchTypeEnum objectType = null;
		private String name = "";


		InterestPeopleContainerVH(View itemView, OnGlobalSearchActionListener listener) {
			super(itemView);

			this.itemView = itemView;

			listName = itemView.findViewById(R.id.title);
			noResult = itemView.findViewById(R.id.no_result);

			gridLayout = itemView.findViewById(R.id.grid_layout);
			gridLayout.setColumnCount(3);
			viewMore = itemView.findViewById(R.id.view_more);

			mListener = listener;
		}

		void setItem(final Object list) {

			List<Object> objects = new ArrayList<>();
			boolean moreData = false;
			if (list instanceof InterestCategory) {
				name = ((InterestCategory) list).getNameToDisplay();
				objects.addAll(((InterestCategory) list).getInterests());
				moreData = ((InterestCategory) list).hasMoreData();
			}
			else if (list instanceof UsersBundle) {
				name = ((UsersBundle) list).getNameToDisplay();
				objects.addAll(((UsersBundle) list).getUsers());
				moreData = ((UsersBundle) list).hasMoreData();
			}

			if (Utils.isStringValid(name))
				listName.setText(name);

			gridLayout.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);

			gridLayout.removeAllViews();

			int rows = objects.size() % 3;
			gridLayout.setRowCount((rows > 0) ? (objects.size() / 3) + 1 : (objects.size() / 3));

			for (int i = 0; i < objects.size(); i++) {
				final Object obj = objects.get(i);

				if (obj instanceof Interest)
					setInterestItem(gridLayout.getContext(), ((Interest) obj));
				else if (obj instanceof HLUserGeneric)
					setUserItem(gridLayout.getContext(), ((HLUserGeneric) obj));
			}

			viewMore.setVisibility(View.GONE);
			viewMore.setText(Utils.getFormattedHtml(viewMore.getResources().getString(R.string.view_more)));
			viewMore.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mListener != null && objectType != null)
						mListener.goToInterestsUsersList(objectType, name);
				}
			});

			if (moreData) {
				View v = LayoutInflater.from(gridLayout.getContext())
						.inflate(R.layout.item_interest_global_search_more, gridLayout, false);

				if (v != null) {
					v.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if (mListener != null && objectType != null)
								mListener.goToInterestsUsersList(objectType, name);
						}
					});

//					LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) v.getLayoutParams());
//					lp.setMarginEnd(Utils.dpToPx(20f, v.getResources()));
//					v.setLayoutParams(lp);

					gridLayout.addView(v);
				}
			}
			else {
				for (int j = 0; j < 3 - rows; j++) {
					setInvisibleItem(gridLayout.getContext());
				}
			}
		}

		private void setUserItem(Context context, @NonNull final HLUserGeneric user) {
			View userView = LayoutInflater.from(context).inflate(R.layout.item_user_global_search, gridLayout, false);
			if (userView != null) {
				ImageView pictureView = userView.findViewById(R.id.profile_picture);
				if (Utils.isStringValid(user.getAvatarURL()))
					MediaHelper.loadProfilePictureWithPlaceholder(context, user.getAvatarURL(), pictureView);
				else
					pictureView.setImageResource(R.drawable.ic_profile_placeholder);

				((TextView) userView.findViewById(R.id.user_name)).setText(user.getCompleteName());

				userView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mListener != null)
							mListener.goToInterestUserProfile(user.getId(), false);
					}
				});

				gridLayout.addView(userView);
			}
		}

		private void setInterestItem(Context context, @NonNull final Interest interest) {
			View interestView = LayoutInflater.from(context).inflate(R.layout.item_interest_global_search, gridLayout, false);
			if (interestView != null) {
				ImageView iv = interestView.findViewById(R.id.interest_image);
				if (iv != null) {
					MediaHelper.roundPictureCorners(iv, interest.getAvatarURL());
				}

				TextView intName = interestView.findViewById(R.id.interest_name);
				intName.setText(interest.getName());
				// INFO: 2/27/19    interest name hidden
				intName.setVisibility(View.GONE);

				interestView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mListener != null)
							mListener.goToInterestUserProfile(interest.getId(), true);
					}
				});

				gridLayout.addView(interestView);
			}
		}

		private void setInvisibleItem(Context context) {
			View interestView = LayoutInflater.from(context).inflate(R.layout.item_interest_global_search, gridLayout, false);
			if (interestView != null) {
				interestView.setVisibility(View.INVISIBLE);
				gridLayout.addView(interestView);
			}
		}

		void setObjectType(GlobalSearchTypeEnum objectType) {
			this.objectType = objectType;
		}

	}
	public interface OnGlobalSearchActionListener {
		void goToTimeline(@NonNull String listName, @Nullable String postId);
		void goToInterestsUsersList(GlobalSearchTypeEnum returnType, String title);
		void goToInterestUserProfile(String id, boolean isInterest);
		void saveScrollView(int position, HorizontalScrollView scrollView);
		void restoreScrollView(int position);
		Resources getResources();
	}

}
