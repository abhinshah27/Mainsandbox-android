/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.enums.FitFillTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.InitiativeTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.InteractionTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.MemoryColorEnum;
import it.keybiz.lbsapp.corporate.models.enums.MemoryTextPositionEnum;
import it.keybiz.lbsapp.corporate.models.enums.MemoryTextSizeEnum;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import kotlin.Triple;

/**
 * Class for providing the model of a Highlanders Post.
 * <p>
 */
@RealmClass
public class Post implements RealmModel, RealmModelListener, Serializable, Comparable<Post>,
		JsonHelper.JsonDeSerializer {


	@SerializedName(value = "postID", alternate = "_id")
	@PrimaryKey
	private String id;

	private Date creationDate;
	@Expose
	private String date;

	@Expose
	private String type;

	@SerializedName("userID")
	@Expose
	private String authorId;

	@SerializedName(value = "name", alternate = "userName")
	@Expose
	private String author;

	@SerializedName("avatarURL")
	@Expose
	private String authorUrl;
	@SerializedName("heartsOfUserOfThePost")
	private int countHeartsUser;

	/**
	 * Property Realm-@Ignored since 3.2(name)-32000(code). Use {@link #mediaObjects} instead.
	 * <p>
	 * It lives only as legacy when queried as MemoryPreview in {@link PostList} objects.
	 */
	@Ignore
	@SerializedName("mediaURL")
	private String content;

	/**
	 * Property Realm-@Ignored since 3.2(name)-32000(code). Use {@link #mediaObjects} instead.
	 * <p>
	 * It lives only as legacy when queried as MemoryPreview in {@link PostList} objects.
	 */
	@Ignore
	@SerializedName("thumbnailURL")
	private String videoThumbnail;

	/**
	 * Property Realm-@Ignored since 3.2(name)-32000(code). Use {@link #messageObject} instead.
	 * <p>
	 * It lives only as legacy when queried as MemoryPreview in {@link PostList} objects.
	 */
	@Ignore
	@SerializedName("message")
	private String caption;

	/**
	 * @since 3.2(name)-32000(code).
	 * <p>
	 * It lives only as legacy when queried as MemoryPreview in {@link PostList} objects.
	 */
	@Ignore private String textColor = MemoryColorEnum.WHITE.toString();

	@Expose
	private RealmList<String> lists;

	@Expose
	private RealmList<String> containers;

	private RealmList<Tag> tags = new RealmList<>();

	@SerializedName("totHearts")
	private int countHeartsPost;
	@SerializedName("hearts")
	private transient RealmList<InteractionHeart> interactionsHeartsPost = new RealmList<>();

	@SerializedName("totComments")
	private int countComments;
	@SerializedName("comments")
	private transient RealmList<InteractionComment> interactionsComments = new RealmList<>();

	@SerializedName("totShares")
	private int countShares;
	@SerializedName("shares")
	private transient RealmList<InteractionShare> interactionsShares = new RealmList<>();

	@SerializedName("isVisible")
	private boolean visible = true;

	@SerializedName("heartsYouLeft")
	private Integer heartsLeft;
	@SerializedName("heartID")
	private String heartsLeftID;

	private boolean youLeftComments = false;
	private boolean youDidShares = false;

	private String originalPostID;
	private String sharedPostID;

	private transient String sharedByRealm;
	@Expose @Ignore
	private InteractionShare sharedBy;

	@Expose private boolean isInterest;
	private boolean youFollow;

	@Expose private HLWebLink webLink;

	@Ignore private boolean isVisibilityChanged;

	@SerializedName(value = "customVisibility", alternate = "visibility")
	@Expose private PostVisibility visibility;

	@SerializedName("idx")
	private long sortId;

	private PostPrivacy privacy;

	@SerializedName("canShowHeartsDetails")
	private boolean showHeartsSharesDetails = true;

	@Expose private Initiative initiative;
	private boolean isInitiative;
	private String GSMessage;
	private String GSRecipientID;

	@SerializedName("interestID")
	private String followedInterestId;

	/**
	 * Storage container for the media file captured during createPost operations.
	 */
	@Ignore
	private transient File mediaFile;

	/**
	 * @since 3.2(name)-32000(code)
	 */
	@Expose private String backgroundColor = MemoryColorEnum.BLACK.toString();

	/**
	 * Contains the information to style a {@link Post} media content.
	 *
	 * @since 3.2(name)-32000(code): Accepting only audio + photo media
	 */
	@Expose private RealmList<MemoryMediaObject> mediaObjects;

	/**
	 * Contains the information to style a {@link Post} text message.
	 * @since 3.2(name)-32000(code)
	 */
	@Expose private MemoryMessageObject messageObject;

	@Ignore private transient MemoryMediaObject primaryMediaObject = null;


	public Post() {
	}

	public Post(String id, String type, Date creationDate, String author, String authorUrl, int countHeartsUser,
				String content, String caption, int countHeartsPost, int countComments, int countShares, boolean isInterest) {
		this.id = id;
		this.type = type;
		this.creationDate = creationDate;
		this.author = author;
		this.authorUrl = authorUrl;
		this.countHeartsUser = countHeartsUser;
		this.content = content;
		this.caption = caption;
		this.countHeartsPost = countHeartsPost;
		this.countComments = countComments;
		this.countShares = countShares;
		this.isInterest = isInterest;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Post post = (Post) o;

		return getId().equals(post.getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}


	//region == COMPARATORS ==

	public static class PostDateComparator implements Comparator<Post> {
		@Override
		public int compare(Post o1, Post o2) {
			long thisMillis = o1.getCreationDate() != null ? o1.getCreationDate().getTime() : Utils.getDateMillisFromDB(o1.getDate());
			long oMillis = o2.getCreationDate() != null ? o2.getCreationDate().getTime() : Utils.getDateMillisFromDB(o2.getDate());

			return Long.compare(oMillis, thisMillis);
		}
	}

	public static class PostHeartsComparator implements Comparator<Post> {
		@Override
		public int compare(Post o1, Post o2) {
			return Integer.compare(o2.getCountHeartsPost(), o1.getCountHeartsPost());
		}
	}

	@Override
	public int compareTo(@NonNull Post o) {
		return Long.compare(getSortId(), o.getSortId());
	}

	//endregion


	@Override
	public String toString() {
		if (Utils.isStringValid(getId()))
			return "Post _id = " + getId();

		return super.toString();
	}

	@LayoutRes
	public int getRightDiaryLayoutItem() {
		switch ((getTypeEnum())) {
			case TEXT:
				return R.layout.item_diary_text;
			case PHOTO:
			case PHOTO_PROFILE:
			case PHOTO_WALL:
			case WEB_LINK:
			case NEWS:
			case FOLLOW_INTEREST:
				return R.layout.item_diary_photo;
			case VIDEO:
				return R.layout.item_diary_video_new;
			case AUDIO:
				return R.layout.item_diary_audio;

			default:
				return R.layout.item_diary_text;
		}
	}

	public void update (Post post, boolean updateSortId) {
		setId(post.getId());
		setType(post.getType());
		setCreationDate(post.getCreationDate());
		setDate(post.getDate());
		setAuthorId(post.getAuthorId());
		setAuthor(post.getAuthor());
		setAuthorUrl(post.getAuthorUrl());
		setCountHeartsUser(post.getCountHeartsUser());
		setContent(post.getContent(false));
		setVideoThumbnail(post.getVideoThumbnail());
		setCaption(post.getCaption());
		setCountHeartsPost(post.getCountHeartsPost());
		setCountComments(post.getCountComments());
		setCountShares(post.getCountShares());
		setInteractionsHeartsPost(post.getInteractionsHeartsPost());
		setInteractionsComments(post.getInteractionsComments());
		setInteractionsShares(post.getInteractionsShares());
		setTags(post.getTags());
		setLists(post.getLists());
		setContainers(post.getContainers());
		setVisible(post.isVisible());
		setHeartsLeft(post.getHeartsLeft());
		setHeartsLeftID(post.getHeartsLeftID());
		setOriginalPostID(post.getOriginalPostID());
		setSharedPostID(post.getSharedPostID());
		setSharedBy(post.getSharedBy());
		setInterest(post.isInterest());
		setYouFollow(post.isYouFollow());
		setWebLink(post.getWebLink());
		setVisibility(post.getVisibility());
		setVisibilityChanged(post.isVisibilityChanged());
		setYouLeftComments(post.isYouLeftComments());
		setYouDidShares(post.isYouDidShares());
		setPrivacy(post.getPrivacy());
		setShowHeartsSharesDetails(post.isShowHeartsSharesDetails());
		setInitiative(post.getInitiative());
		setInitiative(post.isInitiative());
		setGSMessage(post.getGSMessage());
		setGSRecipient(post.getGSRecipient());
		setFollowedInterestId(post.getFollowedInterestId());

		setBackgroundColor(post.getBackgroundColor());
		setMediaObjects(post.getMediaObjects());
		setMessageObject(post.getMessageObject());
		setTextColor(post.getTextColor(), false);

		if (updateSortId)
			setSortId(post.getSortId());
	}

	@Override
	public void update(Object object) {

		if (object instanceof Post) {
			update((Post) object, true);
		}
	}

	@Override
	public void update(JSONObject json) {
		deserialize(json, Post.class);
	}

	public Post returnUpdatedPost(JSONObject json) {
		Post p = (Post) updateWithReturn(json);
		p.setCreationDate(Utils.getDateFromDB(p.getDate()));
		return p;
	}

	Post returnUpdatedPost(Object post) {
		return (Post) updateWithReturn(post);
	}

	@Override
	public RealmModelListener updateWithReturn() {
		return null;
	}

	@Override
	public RealmModelListener updateWithReturn(Object object) {
		update(object);
		return this;
	}

	@Override
	public RealmModelListener updateWithReturn(JSONObject json) {
		return (RealmModelListener) deserialize(json.toString(), Post.class);
	}

	@Override
	public void write(JSONObject json) {
		update(json);
	}

	@Override
	public void reset() {

	}

	@Override
	public Object read(Realm realm) {
		return null;
	}

	@Override
	public RealmModel read(Realm realm, Class<? extends RealmModel> model) {
		return null;
	}

	@Override
	public void deserializeStringListFromRealm() throws JSONException {
		if (Utils.isStringValid(sharedByRealm) && !sharedByRealm.equals("{ }")) {
			sharedBy = new InteractionShare().returnUpdatedInteraction(new JSONObject(sharedByRealm));
		}
	}

	@Override
	public void serializeStringListForRealm() {
		if (sharedBy != null)
			sharedByRealm = sharedBy.serializeToStringWithExpose();
	}

	@Override
	public void write(Realm realm) {
		RealmUtils.writeToRealm(realm, this);
	}

	@Override
	public void write(Object object) {
	}

	@Override
	public void write(Realm realm, RealmModel model) {
	}

	@Override
	public void update() {
	}


	@Override
	public JsonElement serializeWithExpose() {
		return JsonHelper.serializeWithExpose(this);
	}

	@Override
	public String serializeToStringWithExpose() {
		return JsonHelper.serializeToStringWithExpose(this);
	}

	@Override
	public JsonElement serialize() {
		return JsonHelper.serialize(this);
	}

	@Override
	public String serializeToString() {
		return JsonHelper.serializeToString(this);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JSONObject json, Class myClass) {
		return JsonHelper.deserialize(json, myClass);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		return null;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
		return JsonHelper.deserialize(jsonString, myClass);
	}

	@Override
	public Object getSelfObject() {
		return this;
	}


	public String serializeWithTags(boolean expose) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Post.class, new Post.PostSerializer(expose))
				.create();
		return gson.toJson(this, Post.class);
	}


	private class PostSerializer implements JsonSerializer<Post> {

		private boolean wantsExpose;

		PostSerializer(boolean expose) {
			wantsExpose = expose;
		}

		@Override
		public JsonElement serialize(Post src, Type typeOfSrc, JsonSerializationContext context) {
			JsonElement jElem = wantsExpose ? src.serializeWithExpose() : src.serialize();

			if (src.hasTags()) {
				JsonObject jPost = jElem.getAsJsonObject();
				jPost.remove("canShowHeartsDetails");
				jPost.add("tags", src.getTagsSerialized());
			}

			return jElem;
		}
	}


	public Date getCreationDate() {
		if (creationDate == null)
			creationDate = Utils.getDateFromDB(date);

		return creationDate;
	}

	public String getFirstNameForUI() {
		String name = getAuthor();
		String firstName = Utils.isStringValid(name) ? name : "";
		if (Utils.isStringValid(firstName)) {
			String[] parts = firstName.split("\\s");
			if (parts.length > 0)
				firstName = parts[0];
		}

		return firstName;
	}

	public String getNameFromShared() {
		if (sharedBy != null) {
			return sharedBy.getAuthor();
		}

		return null;
	}

	/**
	 * Checks whether the user acting on the Highlanders is the post's author or not.
	 * @param realm the {@link Realm} instance used to retrieve the persisted {@link HLUser}.
	 * @return True if the acting user is also the post's author.
	 */
	public boolean isActiveUserAuthor(Realm realm) {
		if (RealmUtils.isValid(realm)) {
			HLUser user = new HLUser().readUser(realm);

			if (user != null && Utils.areStringsValid(user.getUserId(), authorId))
				return user.getId().equals(authorId);
		}

		return false;
	}

	private boolean isAuthorAmongUserIdentities(Realm realm) {
		if (RealmUtils.isValid(realm)) {
			HLUser user = new HLUser().readUser(realm);

			if (RealmObject.isValid(user) && user.hasIdentities() && Utils.isStringValid(authorId)) {
				List<HLIdentity> list = Stream.of(user.getIdentities()).filter(new Predicate<HLIdentity>() {
					@Override
					public boolean test(HLIdentity value) {
						return Utils.isStringValid(value.getId()) && value.getId().equals(authorId);
					}
				}).collect(Collectors.toList());

				return list != null && !list.isEmpty();
			}
		}

		return false;
	}

	public boolean canPostBeHeartedByUser(Realm realm) {
		return !isActiveUserAuthor(realm) && !isAuthorAmongUserIdentities(realm);
	}

	public String getReadableInteractionCount(Context context, InteractionTypeEnum type, boolean post) {
		int cnt = -1;
		switch (type) {
			case HEARTS:
				cnt = post ? getCountHeartsPost() : getCountHeartsUser();
				break;
			case COMMENT:
				cnt = getCountComments();
				break;
			case SHARE:
				cnt = getCountShares();
				break;
		}

		return Utils.getReadableCount(context.getResources(), cnt);
	}

	// bug on comments calls for edit: needed filter on visible comments
	List<InteractionComment> getVisibleInteractionsComments() {
		if (interactionsComments == null)
			interactionsComments = new RealmList<>();

		List<InteractionComment> result = Stream.of(interactionsComments).filter(new Predicate<InteractionComment>() {
			@Override
			public boolean test(InteractionComment interactionComment) {
				return interactionComment.isVisible();
			}
		}).collect(Collectors.toList());

		if (result != null) {
			interactionsComments.clear();
			interactionsComments.addAll(result);
		}

		return interactionsComments;
	}

	public boolean checkYouLeftComments(String userId) {
		if (!Utils.isStringValid(userId)) return false;

		List<InteractionComment> result = Stream.of(getVisibleInteractionsComments()).filter(new Predicate<InteractionComment>() {
			@Override
			public boolean test(InteractionComment interactionComment) {
				return Utils.isStringValid(interactionComment.getAuthorId()) && interactionComment.getAuthorId().equals(userId);
			}
		}).collect(Collectors.toList());

		return result != null && !result.isEmpty();
	}

	/**
	 * Checks whether the acting {@link HLUser} has already left hears for the current {@link Post},.
	 *
	 * @return True and the hearts count if the user has left hearts, false and null otherwise.
	 */
	public boolean hasUserAlreadySentHearts() {
		return heartsLeft != null && heartsLeft > 0;
	}

	public PostTypeEnum getTypeEnum() {
		return PostTypeEnum.toEnum(type);
	}

	/**
	 * @return True if the {@link Post} contains a media, false otherwise.
	 */
	public boolean isMediaPost() {
		PostTypeEnum type = getTypeEnum();
		return type == PostTypeEnum.AUDIO ||
				type == PostTypeEnum.PHOTO ||
				type == PostTypeEnum.PHOTO_PROFILE ||
				type == PostTypeEnum.PHOTO_WALL ||
				type == PostTypeEnum.FOLLOW_INTEREST ||
				type == PostTypeEnum.VIDEO;
	}

	/**
	 * @return True if the {@link Post} is of type {@link PostTypeEnum#AUDIO}, false otherwise.
	 */
	public boolean isAudioPost() {
		return getTypeEnum() == PostTypeEnum.AUDIO;
	}

	/**
	 * @return True if the {@link Post} is of type {@link PostTypeEnum#PHOTO}, false otherwise.
	 */
	public boolean isPicturePost() {
		return getTypeEnum() == PostTypeEnum.PHOTO ||
				getTypeEnum() == PostTypeEnum.PHOTO_PROFILE ||
				getTypeEnum() == PostTypeEnum.PHOTO_WALL ||
				getTypeEnum() == PostTypeEnum.FOLLOW_INTEREST;
	}

	/**
	 * @return True if the {@link Post} is of type {@link PostTypeEnum#TEXT}, false otherwise.
	 */
	public boolean isTextPost() {
		return getTypeEnum() == PostTypeEnum.TEXT;
	}

	/**
	 * @return True if the {@link Post} is of type {@link PostTypeEnum#VIDEO}, false otherwise.
	 */
	public boolean isVideoPost() {
		return getTypeEnum() == PostTypeEnum.VIDEO;
	}

	/**
	 * @return True if the {@link Post} is of type {@link PostTypeEnum#WEB_LINK}, false otherwise.
	 */
	public boolean isWebLinkPost() {
		return getTypeEnum() == PostTypeEnum.WEB_LINK ||
				getTypeEnum() == PostTypeEnum.NEWS;
	}

	public boolean hasCaption() {
		return getMessageObject() != null && Utils.isStringValid(getMessageObject().getMessage());
	}

	public String getCaption() {
		if (Utils.isStringValid(caption))
			return caption;
		else
			return (getMessageObject() != null) ? getMessageObject().getMessage() : null;
	}
	public void setCaption(String caption) {
		if (getMessageObject() == null)
			setMessageObject(new MemoryMessageObject());

		getMessageObject().setMessage(caption);
	}


	public String getContent() {
		return getContent(true);
	}
	public String getContent(boolean checkWebLink) {
		if (isWebLinkPost() && checkWebLink)
			return webLink.getMediaURL();
		else {
			if (Utils.isStringValid(content))
				return content;
			else {
				MemoryMediaObject obj = getPrimaryMediaObject();
				return (obj != null) ? obj.getMediaURL() : null;
			}
		}
	}
	public void setContent(String content) {
		MemoryMediaObject obj = getPrimaryMediaObject();
		if (obj == null) {
			// something's not right
			return;
		}

		obj.setMediaURL(content);
	}

	public String getVideoThumbnail() {
		if (Utils.isStringValid(videoThumbnail))
			return videoThumbnail;
		else {
			MemoryMediaObject obj = getPrimaryMediaObject();
			return (obj != null) ? obj.getThumbnailURL() : null;
		}
	}
	private void setVideoThumbnail(String videoThumbnail) {
		MemoryMediaObject obj = getPrimaryMediaObject();
		if (obj == null) {
			// something's not right
			return;
		}

		obj.setThumbnailURL(videoThumbnail);
	}

	private MemoryMediaObject getPrimaryMediaObject() {

		if (!isMediaPost()) return null;

		if (primaryMediaObject != null) {
			if (getMediaObjects() != null && getMediaObjects().contains(primaryMediaObject))
				return primaryMediaObject;
			else
				primaryMediaObject = null;
		}

		// create list if it does not exist
		if (getMediaObjects() == null)
			setMediaObjects(new RealmList<>());

		if (getMediaObjects().isEmpty()) {
			// if list is empty create a new MediaObject initialized with the same type of the post
			MemoryMediaObject newMedia = new MemoryMediaObject();
			newMedia.setTypeFromPost(getType());
			getMediaObjects().add(newMedia);
		}

		if (!getMediaObjects().isEmpty()) {
			for (MemoryMediaObject media : getMediaObjects()) {
				if (isAudioPost() && media.isAudio()) {
					primaryMediaObject = media;
					break;
				}
				else if (isPicturePost() && media.isPhoto()) {
					primaryMediaObject = media;
					break;
				}
				else if (isVideoPost() && media.isVideo()) {
					primaryMediaObject = media;
					break;
				}
			}
		}

		return primaryMediaObject;
	}

	public boolean hasMedia() {
	    return getMediaObjects() != null && !getMediaObjects().isEmpty();
    }

	public boolean hasWebLink() {
		return webLink != null;
	}

	public String getWebLinkImage() {
		return hasWebLink() ? getWebLink().getMediaURL() : null;
	}

	public String getWebLinkTitle() {
		return hasWebLink() ? getWebLink().getTitle() : null;
	}

	public String getWebLinkSource() {
		return hasWebLink() ? getWebLink().getSource() : null;
	}

	public String getWebLinkUrl() {
		return hasWebLink() ? getWebLink().getLink() : null;
	}


	public boolean hasTags() {
		return getTags() != null && !getTags().isEmpty();
	}

	public JsonArray getTagsSerialized() {
		JsonArray jTags = new JsonArray();
		if (hasTags()) {
			for (Tag tag : getTags()) {
				jTags.add(tag.getId());
			}
		}

		return jTags;
	}

	public PrivacyPostVisibilityEnum getPostVisibilityEnum() {
		if (visibility != null) {
			return PrivacyPostVisibilityEnum.toEnum(visibility.getRawVisibilityType());
		}

		return PrivacyPostVisibilityEnum.INNER_CIRCLE;
	}


	public boolean canLookAuthorUp() {
		return privacy != null && privacy.canLookAuthorUp();
	}

	public boolean canCommentPost() {
		return privacy != null && privacy.canComment();
	}


	public boolean isEditable() {
		return getTypeEnum() != PostTypeEnum.PHOTO_PROFILE && getTypeEnum() != PostTypeEnum.PHOTO_WALL;
	}

	public boolean hasLists() {
		return getLists() != null && !getLists().isEmpty();
	}

	public boolean isPublic() {
		return getPostVisibilityEnum() == PrivacyPostVisibilityEnum.PUBLIC;
	}

	public boolean hasInitiative() {
		return isInitiative() && getInitiative() != null;
	}

	public boolean isTHInitiative() {
		return hasInitiative() && getInitiative().getTypeEnum() == InitiativeTypeEnum.TRANSFER_HEARTS;
	}

	public boolean isCHInitiative() {
		return hasInitiative() && getInitiative().getTypeEnum() == InitiativeTypeEnum.COLLECT_HEARTS;
	}

	public boolean isGSInitiative() {
		return hasInitiative() && getInitiative().getTypeEnum() == InitiativeTypeEnum.GIVE_SUPPORT;
	}

	public boolean isGSSecondaryPost() {
		return hasGSMessage() && hasGSRecipient();
	}

	public boolean hasGSMessage() {
		return Utils.isStringValid(getGSMessage());
	}
	private boolean hasGSRecipient() {
		return Utils.isStringValid(getGSRecipient());
	}

	public boolean hasNewFollowedInterest() {
		return Utils.isStringValid(getFollowedInterestId());
	}


	/**
	 * @param resources the application's {@link Resources}.
	 * @return the wanted color as @ColorInt or BLACK as default.
	 */
	@ColorInt public int getBackgroundColor(Resources resources) {
		return MemoryColorEnum.getColor(resources, getBackgroundColor());
	}

	/**
	 * @param resources the application's {@link Resources}.
	 * @return the wanted color as @ColorInt or BLACK as default.
	 */
	public Triple<Integer, Float, Integer> getTextStyle(Resources resources) {
		@ColorInt int color = MemoryColorEnum.getColor(resources, MemoryColorEnum.WHITE);
		float size = MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_MEDIUM);
		int position = MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.TOP_LEFT);
		if (messageObject != null) {
			color = messageObject.getColor(resources);
			size = messageObject.getSize();
			position = messageObject.getPosition();
		}

		return new Triple<>(color, size, position);
	}

	public boolean doesMediaWantFitScale() {
		MemoryMediaObject obj = getPrimaryMediaObject();
		return obj != null && obj.isFit();
	}

	public void setTextPosition(String value) {
		if (Utils.isStringValid(value)) {
			if (getMessageObject() == null)
				setMessageObject(new MemoryMessageObject());

			getMessageObject().setTextPosition(value);
		}
	}

	public void setTextColor(String value, boolean messageProperty) {
		if (Utils.isStringValid(value)) {
			if (messageProperty) {
				if (getMessageObject() == null)
					setMessageObject(new MemoryMessageObject());

				getMessageObject().setTextColor(value);
			}
			else textColor = value;
		}
	}

	public void setTextSize(String value) {
		if (Utils.isStringValid(value)) {
			if (getMessageObject() == null)
				setMessageObject(new MemoryMessageObject());

			getMessageObject().setTextSize(value);
		}
	}

	public void setMediaContentMode(int value) {
		MemoryMediaObject obj = getPrimaryMediaObject();
		if (obj == null) {
			// something's not right
			return;
		}

		if (value == FitFillTypeEnum.FIT.getValue() || value == FitFillTypeEnum.FILL.getValue())
			obj.setContentMode(value);
		else
			obj.setContentMode(FitFillTypeEnum.FILL.getValue());
	}


	//region == Getters and setters ==

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthorUrl() {
		return authorUrl;
	}
	public void setAuthorUrl(String authorUrl) {
		this.authorUrl = authorUrl;
	}

	public int getCountHeartsUser() {
		return countHeartsUser;
	}
	public void setCountHeartsUser(int countHeartsUser) {
		this.countHeartsUser = countHeartsUser;
	}

	public int getCountHeartsPost() {
		return countHeartsPost;
	}
	public void setCountHeartsPost(int countHeartsPost) {
		this.countHeartsPost = countHeartsPost;
	}

	public List<InteractionHeart> getInteractionsHeartsPost() {
		return interactionsHeartsPost;
	}
	public void setInteractionsHeartsPost(List<InteractionHeart> interactionsHeartsPost) {
		this.interactionsHeartsPost = (RealmList<InteractionHeart>) interactionsHeartsPost;
	}

	public int getCountComments() {
		return countComments;
	}
	public void setCountComments(int countComments) {
		this.countComments = countComments;
	}

	public List<InteractionComment> getInteractionsComments() {
		return interactionsComments;
	}
	public void setInteractionsComments(List<InteractionComment> interactionsComments) {
		this.interactionsComments = (RealmList<InteractionComment>) interactionsComments;
	}

	public int getCountShares() {
		return countShares;
	}
	public void setCountShares(int countShares) {
		this.countShares = countShares;
	}

	public List<InteractionShare> getInteractionsShares() {
		return interactionsShares;
	}
	public void setInteractionsShares(List<InteractionShare> interactionsShares) {
		this.interactionsShares = (RealmList<InteractionShare>) interactionsShares;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}

	public String getAuthorId() {
		return authorId;
	}
	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	public RealmList<String> getLists() {
		if (lists == null)
			lists = new RealmList<>();
		return lists;
	}
	public void setLists(RealmList<String> lists) {
		this.lists = lists;
	}

	public RealmList<String> getContainers() {
		return containers;
	}
	public void setContainers(RealmList<String> containers) {
		this.containers = containers;
	}

	public RealmList<Tag> getTags() {
		return tags;
	}
	public void setTags(RealmList<Tag> tags) {
		this.tags = tags;
	}

	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public Integer getHeartsLeft() {
		return heartsLeft;
	}
	public void setHeartsLeft(Integer heartsLeft) {
		this.heartsLeft = heartsLeft;
	}

	public String getHeartsLeftID() {
		return heartsLeftID;
	}
	public void setHeartsLeftID(String heartsLeftID) {
		this.heartsLeftID = heartsLeftID;
	}

	public boolean isYouLeftComments() {
		return youLeftComments;
	}
	public void setYouLeftComments(boolean youLeftComments) {
		this.youLeftComments = youLeftComments;
	}

	public boolean isYouDidShares() {
		return youDidShares;
	}
	public void setYouDidShares(boolean youDidShares) {
		this.youDidShares = youDidShares;
	}

	public String getOriginalPostID() {
		return originalPostID;
	}
	public void setOriginalPostID(String originalPostID) {
		this.originalPostID = originalPostID;
	}

	public String getSharedPostID() {
		return sharedPostID;
	}
	public void setSharedPostID(String sharedPostID) {
		this.sharedPostID = sharedPostID;
	}

	public InteractionShare getSharedBy() {
		return sharedBy;
	}
	public void setSharedBy(InteractionShare sharedBy) {
		this.sharedBy = sharedBy;
	}

	public boolean isInterest() {
		return isInterest;
	}
	public void setInterest(boolean interest) {
		isInterest = interest;
	}

	public boolean isYouFollow() {
		return youFollow;
	}
	public void setYouFollow(boolean youFollow) {
		this.youFollow = youFollow;
	}

	public HLWebLink getWebLink() {
		return webLink;
	}
	public void setWebLink(HLWebLink webLink) {
		this.webLink = webLink;
	}

	public boolean isVisibilityChanged() {
		return isVisibilityChanged;
	}
	public void setVisibilityChanged(boolean visibilityChanged) {
		isVisibilityChanged = visibilityChanged;
	}

	public PostVisibility getVisibility() {
		return visibility;
	}
	public void setVisibility(PostVisibility visibility) {
		this.visibility = visibility;
	}

	public long getSortId() {
		return sortId;
	}
	public void setSortId(long sortId) {
		this.sortId = sortId;
	}

	public PostPrivacy getPrivacy() {
		return privacy;
	}
	public void setPrivacy(PostPrivacy privacy) {
		this.privacy = privacy;
	}

	public boolean isShowHeartsSharesDetails() {
		return showHeartsSharesDetails;
	}
	public void setShowHeartsSharesDetails(boolean showHeartsSharesDetails) {
		this.showHeartsSharesDetails = showHeartsSharesDetails;
	}

	public Initiative getInitiative() {
		return initiative;
	}
	public void setInitiative(Initiative initiative) {
		this.initiative = initiative;
	}

	public boolean isInitiative() {
		return isInitiative;
	}
	public void setInitiative(boolean initiative) {
		isInitiative = initiative;
	}

	public String getGSMessage() {
		return GSMessage;
	}
	public void setGSMessage(String GSMessage) {
		this.GSMessage = GSMessage;
	}

	public String getGSRecipient() {
		return GSRecipientID;
	}
	public void setGSRecipient(String GSRecipient) {
		this.GSRecipientID = GSRecipient;
	}

	public String getFollowedInterestId() {
		return followedInterestId;
	}
	public void setFollowedInterestId(String followedInterestId) {
		this.followedInterestId = followedInterestId;
	}


	public RealmList<MemoryMediaObject> getMediaObjects() {
		return mediaObjects;
	}
	public void setMediaObjects(RealmList<MemoryMediaObject> mediaObjects) {
		this.mediaObjects = mediaObjects;
	}

	public String getBackgroundColor() {
		return backgroundColor;
	}
	public void setBackgroundColor(String backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public MemoryMessageObject getMessageObject() {
		return messageObject;
	}
	public void setMessageObject(MemoryMessageObject messageObject) {
		this.messageObject = messageObject;
	}

    public String getTextColor() {
        return textColor;
    }

    public File getMediaFile() {
		return mediaFile;
	}
	public void setMediaFile(File mediaFile) {
		this.mediaFile = mediaFile;
	}

	//endregion

}
