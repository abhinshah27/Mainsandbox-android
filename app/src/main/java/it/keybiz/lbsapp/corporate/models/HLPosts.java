/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Predicate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import it.keybiz.lbsapp.corporate.features.timeline.TimelineFragment;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 9/20/2017.
 */
public class HLPosts {

	public static final String LOG_TAG = HLPosts.class.getCanonicalName();

	private static final int BACKUP_LENGTH = 100;

	private static HLPosts instance = null;
	private static final Object mutex = new Object();

	private List<String> postIdsSorted;
	private List<Post> backupPosts;
	private Map<String, Post> postsMap;

	private Map<String, Post> postsForDiaryOrGlobalSearch;

	private Post selectedPostForWish;

	// TODO: 9/13/2018
	// to revert and try something else just uncomment property + l. 82 and remove Realm argument
	// from method declarations. Correct issues throughout app module.
//	private Realm realm;

	public static String lastPostSeenId = "";
	public static String lastPostSeenIdGlobalDiary = "";

	private Comparator<Post> sortRecent, sortLoved;


	public static HLPosts getInstance() {
		if (instance == null) {
			synchronized (mutex){
				if (instance == null)
					instance = new HLPosts();
			}
		}

		return instance;
	}

	private HLPosts() {}

	/**
	 * This method initializes vars like maps and lists. It is called at the singleton instantiation.
	 */
	public void init() {
		postsMap = new ConcurrentHashMap<>();
		postsForDiaryOrGlobalSearch = new ConcurrentHashMap<>();
		postIdsSorted = new ArrayList<>();

		backupPosts = new RealmList<>();

		sortRecent = new Post.PostDateComparator();
		sortLoved = new Post.PostHeartsComparator();

		setBackupFromRealm();
	}

	public void setPosts(String array, Realm realm, boolean writeRealm) throws JSONException {
		if (Utils.isStringValid(array)) {

			/* boolean forced TRUE until implementation of pagination method */
			setPosts(new JSONArray(array), realm, true);
		}
	}

	public void setPosts(JSONArray jsonArray, Realm realm, boolean writeRealm) throws JSONException {
		if (jsonArray != null && jsonArray.length() > 0) {
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject j = jsonArray.getJSONObject(i);
				if (j != null) {
					String id = j.optString("_id");
					if (Utils.isStringValid(id)) {
						postIdsSorted.add(id);

						Post p = new Post().returnUpdatedPost(j);
						if (postsMap.containsKey(id)) {
							Post p1 = postsMap.get(id);
							p1.update(p);
						}
						else {
							postsMap.put(id, p);
						}

						if (writeRealm)
							setBackupPost(p, realm);
					}
				}
			}
		}
	}

	public void setPost(Post post, Realm realm, boolean writeRealm) {
		if (post != null) {
			if (postsMap == null)
				postsMap = new HashMap<>();

			String id = post.getId();
			if (Utils.isStringValid(id)) {
				Post p;
				if (postsMap.containsKey(id)) {
					p = postsMap.get(id);
					boolean updateOthers = p.getCountHeartsUser() != post.getCountHeartsUser();
					p.update(post);
					if (updateOthers) updateAuthorHeartsForAllPosts(p);
				}
				else {
					p = new Post().returnUpdatedPost(post);
					postsMap.put(id, p);
				}

				if (writeRealm)
					setBackupPost(p, realm);
			}
		}
	}

	public void setPost(JSONObject post, Realm realm, boolean writeRealm) {
		if (post != null) {
			if (postsMap == null)
				postsMap = new HashMap<>();

			String id = post.optString("_id");
			if (Utils.isStringValid(id)) {
				Post p = new Post().returnUpdatedPost(post);
				if (postsMap.containsKey(id)) {
					Post p1 = postsMap.get(id);
					boolean updateOthers = p1.getCountHeartsUser() != p.getCountHeartsUser();
					p1.update(p);
					if (updateOthers) updateAuthorHeartsForAllPosts(p1);
				}
				else postsMap.put(id, p);

				if (writeRealm)
					setBackupPost(p, realm);
			}
		}
	}

	public void resetCollectionsForSwitch(Realm realm) {
		postsMap = new ConcurrentHashMap<>();
		postsForDiaryOrGlobalSearch = new ConcurrentHashMap<>();
		postIdsSorted = new ArrayList<>();

		backupPosts = new RealmList<>();
		cleanRealmPostsNewSession(realm);
	}


	public Post getPost(@NonNull String id) {
		return getPost(id, false);
	}

	public Post getPost(@NonNull String id, boolean onlyDiary) {
		if (onlyDiary) {
			if (postsForDiaryOrGlobalSearch.containsKey(id))
				return postsForDiaryOrGlobalSearch.get(id);

			return null;
		}
		else {
			if (postsMap != null && !postsMap.isEmpty()) {
				if (postsMap.containsKey(id))
					return postsMap.get(id);
				else if (postsForDiaryOrGlobalSearch.containsKey(id))
					return postsForDiaryOrGlobalSearch.get(id);
			}
			return null;
		}
	}

	public Collection<Post> getPosts() {
		if (postsMap != null)
			return postsMap.values();

		return new ArrayList<>();
	}

	public Collection<Post> getPostsForDiaryOrGlobalSearch() {
		if (postsForDiaryOrGlobalSearch != null)
			return postsForDiaryOrGlobalSearch.values();

		return new ArrayList<>();
	}

	public void deletePost(final @NonNull String id, Realm realm, boolean writeRealm) {
		if (Utils.isStringValid(id)) {
			postsMap.remove(id);

			if (writeRealm && RealmUtils.isValid(realm)) {
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						Post p = realm.where(Post.class).contains("id", id).findFirst();
						if (p != null)
							RealmObject.deleteFromRealm(p);
					}
				});
			}
		}
	}

	public List<Post> getSortedPosts() {
		List<Post> res = new ArrayList<>();
		if (getPosts() != null) {
			res.addAll(getPosts());
			sortMainFeedPosts(res);
		}

		return res;
	}

	public void sortMainFeedPosts(List<Post> list) {
		if (list != null && !list.isEmpty()) {
			Realm realm = null;
			try {
				realm = RealmUtils.getCheckedRealm();
				HLUser user = new HLUser().readUser(realm);
				int sortOrder = user.getSettingSortOrder();

				switch (sortOrder) {
					case Constants.SERVER_SORT_TL_ENUM_DEFAULT:
					case Constants.SERVER_SORT_TL_ENUM_RECENT:
						Collections.sort(list, sortRecent);
						break;
					case Constants.SERVER_SORT_TL_ENUM_LOVED:
						Collections.sort(list, sortLoved);
						break;
					case Constants.SERVER_SORT_TL_ENUM_SHUFFLE:
						Collections.sort(list);
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RealmUtils.closeRealm(realm);
			}
		}
	}

	public List<Post> getSortedPostsForDiary() {
		List<Post> res = new ArrayList<>();
		res.addAll(new ArrayList<>(getPostsForDiaryOrGlobalSearch()));
		if (!res.isEmpty())
			Collections.sort(res);

		return res;
	}

	public List<Post> getVisiblePosts() {
		return Stream.of(getSortedPosts()).filter(new Predicate<Post>() {
			@Override
			public boolean test(Post post) {
				return post.isVisible();
			}
		}).collect(Collectors.toList());
	}

	public List<Post> getVideoPosts() {
		return Stream.of(getPosts()).filter(new Predicate<Post>() {
			@Override
			public boolean test(Post post) {
				return post.isVideoPost();
			}
		}).collect(Collectors.toList());
	}

	public List<Post> getPostsSortedByServer() {
		if (postIdsSorted != null && !postIdsSorted.isEmpty() &&
				postsMap != null && !postsMap.isEmpty()) {
			List<Post> result = new ArrayList<>();
			for (String id : postIdsSorted) {
				if (postsMap.containsKey(id))
					result.add(postsMap.get(id));
			}

			return result;
		}

		return new ArrayList<>();
	}

	public int getFeedPostsSkip(TimelineFragment.FragmentUsageType type) {
		return type == TimelineFragment.FragmentUsageType.TIMELINE ?
				getSortedPosts().size() : getSortedPostsForDiary().size();
	}


	public void clearPosts() {
		if (postsMap != null)
			postsMap.clear();
		if (postsForDiaryOrGlobalSearch != null)
			postsForDiaryOrGlobalSearch.clear();
		if (postIdsSorted != null)
			postIdsSorted.clear();
	}


	public void deletePostsByAuthorId(String authorId, Handler handler) {
		if (!Utils.isStringValid(authorId)) return;

		HandlerThread newThread = new HandlerThread("deletePostsByAuthorId");
		newThread.start();

		new Handler(newThread.getLooper()).post(new Runnable() {
			@Override
			public void run() {
				Realm realm = null;
				try {
					realm = RealmUtils.getCheckedRealm();
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull final Realm realm) {
							Stream.of(getPosts()).forEach(new Consumer<Post>() {
								@Override
								public void accept(Post post) {
									if (post != null) {
										if (Utils.isStringValid(post.getAuthorId()) && post.getAuthorId().equals(authorId))
											deletePostInTransaction(post.getId(), realm);
									}
								}
							});

							if (handler != null) handler.sendEmptyMessage(0);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					RealmUtils.closeRealm(realm);

					newThread.quitSafely();
				}
			}
		});
	}

	private void deletePostInTransaction(final @NonNull String id, Realm realm) {
		if (Utils.isStringValid(id)) {
			postsMap.remove(id);

			Post p = realm.where(Post.class).contains("id", id).findFirst();
			if (p != null)
				RealmObject.deleteFromRealm(p);
		}
	}

	//region == INTERACTIONS ==

	public void setInteractionsHearts(JSONArray jsonArray, String pid, Realm realm, boolean writeRealm) {

		if (jsonArray != null && jsonArray.length() > 0) {
			if (Utils.isStringValid(pid)) {
				Post p = getPost(pid);
//				Post p = postsMap.get(pid);
				if (p != null) {
					InteractionHeart ih;
					int finalCount = 0;
					RealmList<InteractionHeart> list = (RealmList<InteractionHeart>) p.getInteractionsHeartsPost();
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject j = jsonArray.optJSONObject(i);
						if (j == null) continue;
						String postId = j.optString("_id");
						JSONObject heart = j.optJSONObject("hearts");
						if (heart == null) continue;
						ih = new InteractionHeart().returnUpdatedInteraction(heart);
						ih.setPostId(postId);
						finalCount += ih.getCount();

						if (list == null) {
							list = new RealmList<>();
							list.add(ih);
							p.setInteractionsHeartsPost(list);
						}
						else {
							if (!list.isEmpty()) {
								boolean heartIn = false;
								for (int k = 0; k < list.size(); k++) {
									InteractionHeart ih2 = list.get(k);
									if (ih2 != null && Utils.areStringsValid(ih2.getId(), ih.getId())) {
										if (ih2.getId().equals(ih.getId())) {
											heartIn = true;
											ih2.update(ih);
											break;
										}
									}
								}

								if (!heartIn)
									list.add(ih);
							}
							else list.add(ih);
						}
					}

					p.setCountHeartsPost(finalCount);

					if (writeRealm)
						setBackupPost(p, realm);
				}
			}
		}
	}

	public void updateAuthorHeartsForAllPosts(String authorId, int heartsToSet, int heartsToAdd) {
		if (!Utils.isStringValid(authorId)) return;

		HandlerThread newThread = new HandlerThread("heartsUpdate");
		newThread.start();

		new Handler(newThread.getLooper()).post(new Runnable() {
			@Override
			public void run() {
				Realm realm = null;
				try {
					realm = RealmUtils.getCheckedRealm();
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull final Realm realm) {
							Stream.of(getPosts()).filter(new Predicate<Post>() {
								@Override
								public boolean test(Post value) {
									return value.getAuthorId().equals(authorId);
								}
							}).forEach(new Consumer<Post>() {
								@Override
								public void accept(Post post) {
									if (heartsToSet != -1)
										post.setCountHeartsUser(heartsToSet);
									else if (-5 <= heartsToAdd && heartsToAdd <= 5)
										post.setCountHeartsUser(post.getCountHeartsUser() + heartsToAdd);

									setBackupPostInTransaction(post, realm);
								}
							});
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					RealmUtils.closeRealm(realm);

					newThread.quitSafely();
				}
			}
		});
	}

	public void updateAuthorHeartsForAllPosts(Post p) {
		if (p == null) return;

		String authorId = p.getAuthorId();
		int hearts = p.getCountHeartsUser();
		updateAuthorHeartsForAllPosts(authorId, hearts, -1);
	}

	public void setInteractionsComments(JSONArray jsonArray, String pid, Realm realm, boolean writeRealm) {

		if (jsonArray != null && jsonArray.length() > 0) {
			if (Utils.isStringValid(pid)) {
				Post p = getPost(pid);
				if (p != null) {
					InteractionComment ic;
					RealmList<InteractionComment> list = (RealmList<InteractionComment>) p.getVisibleInteractionsComments();
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject j = jsonArray.optJSONObject(i);
						if (j == null) continue;
						ic = new InteractionComment().returnUpdatedInteraction(j);

						if (ic == null) continue;

						if (list == null) {
							list = new RealmList<>();
							list.add(ic);
							p.setInteractionsComments(list);
						}
						else {
							if (!list.isEmpty()) {
								boolean commentIn = false;
								for (int k = 0; k < list.size(); k++) {
									InteractionComment ic2 = list.get(k);
									if (ic2 != null && Utils.areStringsValid(ic2.getId(), ic.getId())) {
										if (ic2.getId().equals(ic.getId())) {
											commentIn = true;
											ic2.update(ic);
											break;
										}
									}
								}

								if (!commentIn)
									list.add(ic);
							}
							else list.add(ic);
						}
					}

					p.setCountComments(list.size());

					if (writeRealm)
						setBackupPost(p, realm);
				}
			}
		}
	}

	public void setInteractionsShares(JSONArray jsonArray, String pid, Realm realm, boolean writeRealm) {

		if (jsonArray != null && jsonArray.length() > 0) {
			if (Utils.isStringValid(pid)) {
				Post p = getPost(pid);
				if (p != null) {
					InteractionShare is;
					RealmList<InteractionShare> list = (RealmList<InteractionShare>) p.getInteractionsShares();
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject j = jsonArray.optJSONObject(i).optJSONObject("shares");
						if (j == null) continue;
						is = new InteractionShare().returnUpdatedInteraction(j);

						if (list == null) {
							list = new RealmList<>();
							list.add(is);
							p.setInteractionsShares(list);
						}
						else {
							if (!list.isEmpty()) {
								boolean shareIn = false;
								for (int k = 0; k < list.size(); k++) {
									InteractionShare is2 = list.get(k);
									if (is2 != null && Utils.areStringsValid(is2.getId(), is.getId())) {
										if (is2.getId().equals(is.getId())) {
											shareIn = true;
											is2.update(is);
											break;
										}
									}
								}

								if (!shareIn)
									list.add(is);
							}
							else list.add(is);
						}
					}

					p.setCountShares(list.size());

					if (writeRealm)
						setBackupPost(p, realm);
				}
			}
		}
	}

	public List<InteractionComment> getVisibleCommentsForPost(@NonNull String postId) {
		if (postsMap == null || !postsMap.containsKey(postId))
			return new ArrayList<>();

		Post p = getPost(postId);
		if (p != null) {
			List<InteractionComment> comments = p.getInteractionsComments();
			if (comments != null && !comments.isEmpty()) {
				return Stream.of(comments).filter(new Predicate<InteractionComment>() {
					@Override
					public boolean test(InteractionComment ic) {
						return ic.isVisible();
					}
				}).collect(Collectors.toList());
			}
		}

		return new ArrayList<>();
	}

	public List<InteractionComment> getVisibleSubCommentsForComment(@NonNull String postId,
																	@NonNull final String parentCommentId,
																	boolean sorted) {
		List<InteractionComment> result = new ArrayList<>();
		if (postsMap == null || !postsMap.containsKey(postId))
			return result;

		Post p = getPost(postId);
		if (p != null) {
			List<InteractionComment> comments = p.getInteractionsComments();
			if (comments != null && !comments.isEmpty()) {
				result = Stream.of(comments).filter(new Predicate<InteractionComment>() {
					@Override
					public boolean test(InteractionComment ic) {
						if (ic.getLevel() == 0 || !Utils.isStringValid(ic.getParentCommentID()))
							return false;

						return ic.isVisible() &&
								ic.getParentCommentID().equals(parentCommentId);
					}
				}).collect(Collectors.toList());

				if (sorted)
					Collections.sort(result);

				return result;
			}
		}

		return result;
	}

	public void updateCommentsForNewComment(@NonNull String postId,
											@NonNull InteractionComment ic) {

		if (postsMap == null || !postsMap.containsKey(postId))
			return;

		Post p = getPost(postId);
		if (p != null) {
			List<InteractionComment> comments = p.getInteractionsComments();
			if (comments != null) {
				if (!comments.isEmpty()) {
					if (!ic.isSubComment()) {
						comments.add(ic);
					}
					else {
						List<InteractionComment> subs =
								getVisibleSubCommentsForComment(postId, ic.getParentCommentID(), true);
						subs.add(ic);

						InteractionComment comment = getCommentInteractionById(postId, ic.getParentCommentID());
						if (comment != null) {
							int position = comments.indexOf(comment);
							if (position > -1) {
								comments.subList(position + 1, (position + subs.size())).clear();
								comments.addAll(position + 1, subs);
							}
						}
					}
				}
				else comments.add(ic);
			}
		}
	}

	public void sortComments(final @NonNull String postId, Realm realm) {

		if (postsMap == null || !postsMap.containsKey(postId))
			return;

		Post p = getPost(postId);
		if (p != null) {
			final List<InteractionComment> comments = p.getInteractionsComments();
			if (comments != null && !comments.isEmpty()) {

				final List<InteractionComment> level0 =  Stream.of(comments).filter(new Predicate<InteractionComment>() {
					@Override
					public boolean test(InteractionComment ic) {
						return (ic.getLevel() == 0 && ic.isVisible());
					}
				}).collect(Collectors.toList());

				if (!level0.isEmpty()) {
					Collections.sort(level0);
					if (RealmUtils.isValid(realm)) {
						realm.executeTransaction(new Realm.Transaction() {
							@Override
							public void execute(@NonNull Realm realm) {
								SparseArray<List<InteractionComment>> commentsMap = new SparseArray<>();
								for (int i = 0; i < level0.size(); i++) {
									InteractionComment ic = level0.get(i);
									if (ic != null) {
										List<InteractionComment> subs = getVisibleSubCommentsForComment(postId, ic.getId(), true);
										commentsMap.append(i, subs);
									}
								}

								if (commentsMap.size() > 0) {
									comments.clear();
									for (int i = 0; i < commentsMap.size(); i++) {
										comments.add(level0.get(i));
										comments.addAll(commentsMap.valueAt(i));
									}
								}
							}
						});
					}
				}
			}
		}
	}

	public InteractionHeart getHeartInteractionById(@NonNull String postId, @NonNull final String heartId) {
		if (postsMap == null || !postsMap.containsKey(postId))
			return null;

		Post p = getPost(postId);
		if (p != null) {
			List<InteractionHeart> hearts = p.getInteractionsHeartsPost();
			if (hearts != null && !hearts.isEmpty()) {
				List<InteractionHeart> list = Stream.of(hearts).filter(new Predicate<InteractionHeart>() {
					@Override
					public boolean test(InteractionHeart ih) {
						return ih.getId().equals(heartId);
					}
				}).collect(Collectors.toList());

				if (list != null && list.size() == 1)
					return list.get(0);
			}
		}

		return null;
	}

	public InteractionComment getCommentInteractionById(@NonNull String postId, @NonNull final String commentId) {
		if (postsMap == null || !postsMap.containsKey(postId))
			return null;

		Post p = getPost(postId);
		if (p != null) {
			List<InteractionComment> comments = p.getInteractionsComments();
			if (comments != null && !comments.isEmpty()) {
				List<InteractionComment> list = Stream.of(comments).filter(new Predicate<InteractionComment>() {
					@Override
					public boolean test(InteractionComment ic) {
						return ic != null && ic.getId() != null && ic.getId().equals(commentId);
					}
				}).collect(Collectors.toList());

				if (list != null && list.size() == 1)
					return list.get(0);
			}
		}

		return null;
	}

	public InteractionShare getShareInteractionById(@NonNull String postId, @NonNull final String shareId) {
		if (postsMap == null || !postsMap.containsKey(postId))
			return null;

		Post p = getPost(postId);
		if (p != null) {
			List<InteractionShare> share = p.getInteractionsShares();
			if (share != null && !share.isEmpty()) {
				List<InteractionShare> list = Stream.of(share).filter(new Predicate<InteractionShare>() {
					@Override
					public boolean test(InteractionShare is) {
						return is.getId().equals(shareId);
					}
				}).collect(Collectors.toList());

				if (list != null && list.size() == 1)
					return list.get(0);
			}
		}

		return null;
	}

	public void updateRenamedList(String oldName, String newName) {
		if (Utils.areStringsValid(oldName, newName)) {

			HandlerThread newThread = new HandlerThread("listsUpdate");
			newThread.start();

			new Handler(newThread.getLooper()).post(() -> {
				Realm realm = null;
				try {
					realm = RealmUtils.getCheckedRealm();
					realm.executeTransaction(realm1 ->
							Stream.of(getPosts())
									.filter(Post::hasLists)
									.forEach(
											post -> {
												RealmList<String> lists = post.getLists();
												if (lists.contains(oldName)) {
													lists.remove(oldName);
													lists.add(newName);
												}
												setBackupPostInTransaction(post, realm1);
											}
									)
					);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					RealmUtils.closeRealm(realm);

					newThread.quitSafely();
				}
			});
		}
	}

	public void updateDeletedList(String deletedList) {
		if (Utils.isStringValid(deletedList)) {

			HandlerThread newThread = new HandlerThread("listsDeleted");
			newThread.start();

			new Handler(newThread.getLooper()).post(() -> {
				Realm realm = null;
				try {
					realm = RealmUtils.getCheckedRealm();
					realm.executeTransaction(realm1 ->
							Stream.of(getPosts())
									.filter(Post::hasLists)
									.forEach(
											post -> {
												RealmList<String> lists = post.getLists();
												lists.remove(deletedList);
												setBackupPostInTransaction(post, realm1);
											}
									)
					);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					RealmUtils.closeRealm(realm);

					newThread.quitSafely();
				}
			});
		}
	}

	//endregion


	//region == DIARY/GLOBAL SEARCH SECTION ==

	public void populatePostsForDiaryOrGlobalSearch(JSONArray array, Realm realm, boolean clear) {
		if (array != null && array.length() > 0) {

			if (clear) {
				if (postsForDiaryOrGlobalSearch == null)
					postsForDiaryOrGlobalSearch = new ConcurrentHashMap<>();
				else
					postsForDiaryOrGlobalSearch.clear();
			}

			for (int i = 0; i < array.length(); i++) {
				JSONObject post = array.optJSONObject(i);
				if (post != null) {
					String id = post.optString("_id");
					if (Utils.isStringValid(id)) {
						Post p = new Post().returnUpdatedPost(post);
						if (postsForDiaryOrGlobalSearch.containsKey(id)) {
							Post p1 = postsForDiaryOrGlobalSearch.get(id);
							p1.update(p, true);

							postsForDiaryOrGlobalSearch.put(id, p1);
						}
						else {
							postsForDiaryOrGlobalSearch.put(id, p);
						}

						if (postsMap.containsKey(id)) {
							Post p1 = postsMap.get(id);
							p1.update(p, false);
							setBackupPost(p1, realm);

							postsForDiaryOrGlobalSearch.put(id, p);

						}
					}
				}
			}
		}
	}

	public void resetPropertiesForDiaryOrGlobalSearch() {
		postsForDiaryOrGlobalSearch.clear();
		lastPostSeenIdGlobalDiary = "";
	}

	//endregion


	//region == BACKUP AND REALM ==

	public List<Post> getBackupPosts() {
		return backupPosts;
	}
	public void setBackupPost(final Post p, Realm realm) {
		if (backupPosts != null && backupPosts.size() <= BACKUP_LENGTH) {
			if (RealmUtils.isValid(realm)) {
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						p.serializeStringListForRealm();
						realm.copyToRealmOrUpdate(p);
					}
				});

				if (!backupPosts.contains(p))
					backupPosts.add(p);
				// otherwise it's already up-to-date

//				realm.close();
			}
		}
	}

	public void setBackupPostInTransaction(final Post p,Realm realm) {
		if (backupPosts != null && backupPosts.size() <= BACKUP_LENGTH) {
			if (RealmUtils.isValid(realm)) {
				p.serializeStringListForRealm();
				realm.copyToRealmOrUpdate(p);

				if (!backupPosts.contains(p))
					backupPosts.add(p);
				// otherwise it's already up-to-date
			}
		}
	}

	private void setBackupFromRealm() {
		if (postsMap != null && backupPosts != null) {
			Realm realm = null;
			try {
				realm = RealmUtils.getCheckedRealm();
				RealmResults<RealmModel> list = RealmUtils.readFromRealm(realm, Post.class);
				if (list != null && !list.isEmpty()) {
					for (RealmModel rm : list) {
						if (rm instanceof Post) {
							try {
								((Post) rm).deserializeStringListFromRealm();
							} catch (JSONException e) {
								e.printStackTrace();
							}
							Post p = (Post) realm.copyFromRealm(rm);
							backupPosts.add(p);
							postsMap.put(p.getId(), p);
						}

						if (backupPosts.size() == BACKUP_LENGTH && postsMap.size() == BACKUP_LENGTH)
							break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RealmUtils.closeRealm(realm);
			}
		}
	}

	public void cleanRealmPostsNewSession(Realm realm) {
		backupPosts.clear();
		postsMap.clear();
		postIdsSorted.clear();
		if (RealmUtils.isValid(realm)) {
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(@NonNull Realm realm) {
					RealmUtils.deleteTable(realm, Post.class);
					RealmUtils.deleteTable(realm, InteractionHeart.class);
					RealmUtils.deleteTable(realm, InteractionComment.class);
					RealmUtils.deleteTable(realm, InteractionShare.class);
					RealmUtils.deleteTable(realm, Tag.class);
					RealmUtils.deleteTable(realm, PostPrivacy.class);
					RealmUtils.deleteTable(realm, PostVisibility.class);
					RealmUtils.deleteTable(realm, HLWebLink.class);
					RealmUtils.deleteTable(realm, MemoryMediaObject.class);
					RealmUtils.deleteTable(realm, MemoryMessageObject.class);
				}
			});
		}
	}

	public void cleanRealmPosts(Realm realm, Context context) {
		int index = getPostIndex(context);

		RealmResults<RealmModel> posts = RealmUtils.readFromRealm(realm, Post.class);
		if (posts != null && posts.size() > 100 && index < (posts.size() - 20)) {
			if (RealmUtils.isValid(realm)) {
				realm.executeTransactionAsync(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						RealmResults<RealmModel> posts = RealmUtils.readFromRealm(realm, Post.class);
						if (posts != null) {
							posts.sort("sortId", Sort.ASCENDING);
							for (int i = 0; i < (posts.size()-100); i++) {
								posts.deleteLastFromRealm();
							}

							LogUtils.d(LOG_TAG, "Posts size: " + posts.size() + "\tlast position: " + lastPostSeenId);
						}
					}
				});
			}
		}
	}

	public int getPostIndex(Context context) {
		String postId = SharedPrefsUtils.getLastPostSeen(context);
		return getPostIndex(postId, false);
	}

	public int getPostIndex(String postId, boolean diaryOrGlobal) {
		int lastPos = 0;
		if (Utils.isStringValid(postId)) {
			List<Post> posts = diaryOrGlobal ? getSortedPostsForDiary() : getVisiblePosts();
			Optional<Post> result = Stream.of(posts).filter(new Predicate<Post>() {
				@Override
				public boolean test(Post post) {
					return post != null && Utils.isStringValid(post.getId()) && post.getId().equals(postId);
				}
			}).findFirst();

			try {
				lastPos = posts.indexOf(result.orElseThrow());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return lastPos;
	}

	public boolean isPostToBePersisted(String postId) {
		return Utils.isStringValid(postId) && postsMap != null && postsMap.containsKey(postId);
	}

	//endregion


	//region == Getters and setters ==

	public Post getSelectedPostForWish() {
		return selectedPostForWish;
	}
	public void setSelectedPostForWish(Post selectedPostForWish) {
		this.selectedPostForWish = selectedPostForWish;
	}

	//endregion

}
