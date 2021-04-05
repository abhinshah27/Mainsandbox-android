/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.helpers;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.models.HLNotifications;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.InteractionPost;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;

/**
 * Extension of {@link AsyncTask} handling the data update for new posts.
 * It accepts as param the {@link JSONArray} server response.
 * @author mabaldrighi on 11/3/2017.
 */
public class LoadMoreResponseHandlerTask extends AsyncTask<JSONArray, Void, Void> {

	public static final String LOG_TAG = LoadMoreResponseHandlerTask.class.getCanonicalName();

	public enum Type {
		POSTS, INTERACTIONS, NOTIFICATIONS, REQUESTS, CIRCLES, SEARCH, INNER_CIRCLE, FOLLOWERS, WISH_POSTS,
		GLOBAL_SEARCH, INTERESTS, CHAT_MESSAGE
	}
	private final Type type;

	private final OnDataLoadedListener mListener;

	private final String postId;
	private final InteractionPost.Type interactionType;

	private boolean exception = false;

	public LoadMoreResponseHandlerTask(@NonNull OnDataLoadedListener listener, @NonNull Type type,
	                                   @Nullable InteractionPost.Type interactionType, @Nullable String postId) {
		super();
		this.mListener = listener;
		this.type = type;
		this.interactionType = interactionType;
		this.postId = postId;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected Void doInBackground(JSONArray... params) {
		Realm realm = null;
		try {
			realm = RealmUtils.getCheckedRealm();
			HLPosts instance = HLPosts.getInstance();
			try {
				switch (type) {
					case POSTS:
						instance.setPosts(params[0], realm, true);
						mListener.setData(realm);
//						instance.cleanRealmPostsNewSession(realm, false);
						break;

					case INTERACTIONS:
						if (interactionType != null) {
							switch (interactionType) {
								case HEARTS:
									instance.setInteractionsHearts(params[0], postId, realm, instance.isPostToBePersisted(postId));
									break;
								case COMMENT:
									instance.setInteractionsComments(params[0], postId, realm, instance.isPostToBePersisted(postId));
									break;
								case SHARE:
									instance.setInteractionsShares(params[0], postId, realm, instance.isPostToBePersisted(postId));
									break;
							}

							mListener.setData(realm);
						}

					case NOTIFICATIONS:
						HLNotifications.getInstance().setNotifications(params[0]);
						mListener.setData((Realm) null);
						break;

					case REQUESTS:
						HLNotifications.getInstance().setRequests(params[0]);
						mListener.setData((Realm) null);
						break;

					case CIRCLES:
					case INNER_CIRCLE:
					case SEARCH:
					case FOLLOWERS:
					case WISH_POSTS:
					case GLOBAL_SEARCH:
					case INTERESTS:
					case CHAT_MESSAGE:
						mListener.setData(params[0]);
						break;
				}
			}
			catch (JSONException e) {
				LogUtils.e(LOG_TAG, e.getMessage(), e);
				mListener.setData(realm);
				exception = true;
			}
		} finally {
			RealmUtils.closeRealm(realm);
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		super.onPostExecute(aVoid);

		if (exception) {
			if (mListener instanceof HLActivity)
				((HLActivity) mListener).showAlert(R.string.error_generic_list);
			else
				mListener.getActivityListener().showAlert(R.string.error_generic_list);
		}

		if (mListener.isFromLoadMore()) {
			mListener.getAdapter().notifyItemRangeInserted(
					Constants.PAGINATION_AMOUNT * mListener.getLastPageId(),
					mListener.getNewItemsCount()
			);
			mListener.resetFromLoadMore();
		}
		else mListener.getAdapter().notifyDataSetChanged();
	}


	public interface OnDataLoadedListener {
		BasicInteractionListener getActivityListener();
		RecyclerView.Adapter getAdapter();
		void setData(Realm realm);
		void setData(JSONArray array);
		boolean isFromLoadMore();
		void resetFromLoadMore();
		int getLastPageId();
		int getNewItemsCount();
	}

}
