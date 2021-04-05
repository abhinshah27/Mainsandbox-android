/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.PostList;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.listeners.LoadMoreScrollListener;

/**
 * @author mbaldrighi on 3/8/20178.
 */
public class PostListsAdapterWishes extends RecyclerView.Adapter<PostListsAdapterWishes.ListVH> {

	private List<PostList> items;

	private WishPostSelectionAdapterFolders.OnPostTileActionListener mListener;
	private WishPostListAdapterListener mListListener;
	private OnHandlingScrollPositionListener mScrollListener;

	public PostListsAdapterWishes(List<PostList> items, WishPostSelectionAdapterFolders.OnPostTileActionListener listener,
	                              WishPostListAdapterListener listListener,
	                              OnHandlingScrollPositionListener scrollListener) {
		this.items = items;
		this.mListener = listener;
		this.mListListener = listListener;
		this.mScrollListener = scrollListener;
	}

	@NonNull
	@Override
	public ListVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ListVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_my_diary_main_wish_folders, parent, false));
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
		private final TextView noResult;

		private final RecyclerView postsScrollView;
		private final LinearLayoutManager llm;
		private final WishPostSelectionAdapterFolders adapter;
		private List<Post> postList = new ArrayList<>();

		private PostList currentPostList;

		private int lastPageId = 1;

		ListVH(View itemView) {
			super(itemView);

			listName = itemView.findViewById(R.id.list_name);

			if (Utils.isContextValid(itemView.getContext()) &&
					itemView.getContext() instanceof WishPostSelectionAdapterFolders.OnPostTileActionListener) {
				mListener = ((WishPostSelectionAdapterFolders.OnPostTileActionListener) itemView.getContext());
			}

			postsScrollView = itemView.findViewById(R.id.rec_view);
			postsScrollView.addOnScrollListener(
					new LoadMoreScrollListener(LoadMoreScrollListener.Type.GENERAL, LoadMoreScrollListener.Orientation.HORIZONTAL) {
						@Override
						public void onLoadMore() {
							mListListener.onLoadMore(currentPostList.getName(), lastPageId, adapter, postList);
						}
					}
			);

			llm = new LinearLayoutManager(postsScrollView.getContext(), LinearLayoutManager.HORIZONTAL, false);
			adapter = new WishPostSelectionAdapterFolders(postList, mListener);

			postsScrollView.setLayoutManager(llm);

			noResult = itemView.findViewById(R.id.no_result);
		}

		void setPostList(final PostList list, int position) {
			if (list == null)
				return;

			currentPostList = list;

			adapter.setListName(list.getName());

			listName.setText(list.getNameToDisplay());

			if (postsScrollView != null) {
				postsScrollView.setTag(null);
				postsScrollView.setTag(position);
				mScrollListener.saveScrollView(position, postsScrollView);

				List<Post> posts = list.getPosts();
				if (posts.isEmpty()) {
					postsScrollView.setVisibility(View.GONE);
					noResult.setText(R.string.no_posts_in_list);
					noResult.setVisibility(View.VISIBLE);
				} else {
					postsScrollView.setVisibility(View.VISIBLE);
					noResult.setVisibility(View.GONE);

					postsScrollView.setLayoutManager(llm);
					postsScrollView.setAdapter(adapter);

					postList.addAll(posts);

					adapter.notifyDataSetChanged();
				}
			}

			mScrollListener.restoreScrollView(position);
		}
	}

	public interface WishPostListAdapterListener {
		void onLoadMore(String listName, int lastPageId, RecyclerView.Adapter adapter, List<Post> postList);
	}

}
