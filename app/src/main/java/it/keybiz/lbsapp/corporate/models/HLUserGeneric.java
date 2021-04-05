/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.chat.ChatRecipientStatus;
import it.keybiz.lbsapp.corporate.models.enums.RequestsStatusEnum;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 11/10/2017.
 */
@RealmClass
public class HLUserGeneric implements RealmModel, RealmModelListener, Serializable,
		JsonHelper.JsonDeSerializer, Comparable<HLUserGeneric> {

	/* VARS NEEDED FOR SHARING */
	@SerializedName(value = "userID", alternate = "_id") @Expose
	@PrimaryKey  private String id;
	@Expose private String name;
	@Expose private String avatarURL;


	/* VARS NEEDED FOR PERSON PROFILE */
	private boolean isFriend;
	private int heartsGiven;
	private int heartsReceived;
	@SerializedName("totHeartsSinceSubscribed")
	private int totHearts;
	@SerializedName("status")
	private String requestsStatusString;
	@Ignore private RequestsStatusEnum requestsStatus;
	private boolean showInnerCircle;
	private boolean canBeInvitedToIC;

	private String firstName;
	private String lastName;
	private String wallImageLink;


	/* VARS NEEDED FOR DETAILS */
	private HLUserAboutMe aboutMe;
	private RealmList<LifeEvent> lifeEvents;
	private HLUserAboutMeMore moreAboutMe;

	private RealmList<FamilyRelationship> familyRelationships;


	/* VARS NEEDED FOR FOLLOWERS */
	private String sharedPeople;


	/* VARS NEEDED FOR CHAT */
	private String chatRoomID;
 	@SerializedName("pstatus")
    private int chatStatus = ChatRecipientStatus.OFFLINE.getValue();
    private String lastSeenDate;

    @Index private boolean canChat;
    private boolean canVideocall;
    private boolean canAudiocall;


	@Ignore private boolean selected;


	public HLUserGeneric() {}

	public HLUserGeneric(String id, String name, String avatarURL) {
		this.id = id;
		this.name = name;
		this.avatarURL = avatarURL;
	}


	@Override
	public int hashCode() {
		if (Utils.isStringValid(id))
			return id.hashCode();
		else
			return super.hashCode();
	}

	@Override
	public int compareTo(@NonNull HLUserGeneric o) {
		if (Utils.areStringsValid(this.name, o.name))
			return this.name.compareTo(o.name);

		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HLUserGeneric && Utils.areStringsValid(getId(), ((HLUserGeneric) obj).getId()))
			return getId().equals(((HLUserGeneric) obj).getId());
		return super.equals(obj);
	}

	public String getCompleteName() {
		if (Utils.isStringValid(name))
			return name.trim().replaceAll("\\s+"," ");
		else if (Utils.areStringsValid(firstName, lastName))
			return firstName.trim() + " " + lastName.trim();
		else
			return "";
	}

	public String getNameForCircleMember() {
		String first = "", singleLetter = "";
		if (Utils.isStringValid(name)) {
			String name = this.name.trim().replaceAll("\\s+"," ");
			String[] arr = name.split(" ");
			if (arr.length > 0) first = arr[0];
			if (arr.length > 1) singleLetter = arr[1].substring(0, 1);
		}
		else if (Utils.areStringsValid(firstName, lastName)) {
			first = firstName;
			singleLetter = lastName.substring(0, 1);
		}

		return first + " " + singleLetter;
	}

	@StringRes
	public int getRightStringForStatus() {
		if (requestsStatus != null && !isFriend) {
			switch (requestsStatus) {
				case NOT_AVAILABLE:
						return R.string.invite_to_inner_circle;
				case PENDING:
						return R.string.notification_action_result_pending;
				case DECLINED:
						return R.string.invite_to_inner_circle_resend;
			}
		}

		return R.string.invite_to_inner_circle;
	}

	public String getAboutBirthday() {
		return aboutMe != null ? aboutMe.getFormattedDate() : null;
	}

	public String getAboutStatus() {
		return aboutMe != null ? aboutMe.getStatus() : null;
	}
	public int getAboutStatusSelection(Resources res) {
		return aboutMe != null ? aboutMe.getStatusPositionInArray(res) : -1;
	}

	public String getAboutCity() {
		return aboutMe != null ? aboutMe.getCity() : null;
	}

	public String getAboutDescription() {
		return aboutMe != null ? aboutMe.getDescription() : null;
	}


	public String getMoreAboutBirthplace() {
		return moreAboutMe != null ? moreAboutMe.getBirthPlace() : null;
	}
	public String getMoreAboutGender() {
		return moreAboutMe != null ? moreAboutMe.getGender() : null;
	}
	public int getMoreAboutGenderSelection(Resources res) {
		return moreAboutMe != null ? moreAboutMe.getSpinnerPositionInArray(res, false) : 0;
	}
	public String getMoreAboutInterestedIn() {
		return moreAboutMe != null ? moreAboutMe.getInterestedIn() : null;
	}
	public int getMoreAboutInterestedInSelection(Resources res) {
		return moreAboutMe != null ? moreAboutMe.getSpinnerPositionInArray(res, true) : -1;
	}
	public String getMoreAboutLanguages() {
		return moreAboutMe != null ? moreAboutMe.getSerializedLanguagesString() : null;
	}
	public String getMoreAboutAddress() {
		return moreAboutMe != null ? moreAboutMe.getAddress() : null;
	}
	public String getMoreAboutOtherNames() {
		return moreAboutMe != null ? moreAboutMe.getOtherNames() : null;
	}

	public boolean hasValidChatRoom() {
		return Utils.isStringValid(chatRoomID);
	}

	public ChatRecipientStatus getChatStatusEnum() {
		if (chatStatus == ChatRecipientStatus.ONLINE.getValue())
			return ChatRecipientStatus.ONLINE;
		else
			return ChatRecipientStatus.OFFLINE;
	}



	public HLUserGeneric deserializeToClass(JSONObject json) {

		HLUserGeneric u =  (HLUserGeneric) deserialize(json, HLUserGeneric.class);

		if (!Utils.isStringValid(name) && Utils.areStringsValid(firstName, lastName))
			name = firstName + " " + lastName;

		return u;
	}

	public HLUserGeneric deserializeComplete(JSONObject json, boolean update) {
		return deserializeComplete(json.toString(), update);
	}

	public HLUserGeneric deserializeComplete(String jsonString, boolean update) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(HLUserGeneric.class, new GenericUserDeserializer(update ? this : null))
				.create();
		HLUserGeneric u = gson.fromJson(jsonString, HLUserGeneric.class);
		if (!Utils.isStringValid(u.name) && Utils.areStringsValid(u.firstName, u.lastName))
			u.name = u.firstName + " " + u.lastName;
		if (Utils.isStringValid(u.getRequestsStatusString()))
			u.requestsStatus = RequestsStatusEnum.toEnum(u.requestsStatusString);
		return u;
	}

	private class GenericUserDeserializer implements JsonDeserializer<HLUserGeneric> {

		private HLUserGeneric user;

		public GenericUserDeserializer(HLUserGeneric user) {
			this.user = user;
		}

		@Override
		public HLUserGeneric deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			Gson gson = new Gson();
			if (user == null)
				user = gson.fromJson(json, HLUserGeneric.class);
			JsonObject jsonObject = json.getAsJsonObject();
			if (jsonObject.has("person")) {
				JsonObject elem = jsonObject.getAsJsonObject("person");
				if (elem != null && !elem.isJsonNull()) {
					if (elem.has("avatarURL"))
						user.setAvatarURL(elem.get("avatarURL").getAsString());
					if (elem.has("wallImageLink"))
						user.setWallImageLink(elem.get("wallImageLink").getAsString());
					if (elem.has("firstName"))
						user.setFirstName(elem.get("firstName").getAsString());
					if (elem.has("lastName"))
						user.setLastName(elem.get("lastName").getAsString());
					if (elem.has("totHeartsSinceSubscribed"))
						user.setTotHearts(elem.get("totHeartsSinceSubscribed").getAsInt());
					if (elem.has("aboutMe")) {
						JsonObject about = elem.getAsJsonObject("aboutMe");
						if (user.getAboutMe() == null)
							user.setAboutMe(gson.fromJson(about, HLUserAboutMe.class));
						else {
							if (about.has("description"))
								user.getAboutMe().setDescription(about.get("description").getAsString());
						}
					}
				}
			}

//			if (jsonObject.has("lifeEvents")) {
//				JsonArray elem = jsonObject.getAsJsonArray("lifeEvents");
//				if (elem != null && !elem.isJsonNull() && elem.getAsJsonArray().size() > 0) {
//					lifeEvents = new RealmList<>();
//
//					for (JsonElement anElem : elem) {
//						JsonObject le = anElem.getAsJsonObject();
//						String date = null, description = null;
//						if (le.has("date"))
//							date = le.get("date").getAsString();
//						if (le.has("description"))
//							description = le.get("description").getAsString();
//
//						if (Utils.areStringsValid(date, description))
//							lifeEvents.add(new LifeEvent(date, description));
//					}
//				}
//			}

			return user;
		}
	}


	//region == Serialization methods ==

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

	//endregion


	//region == Realm model listener ==

	@Override
	public void reset() {}

	@Override
	public Object read(@Nullable Realm realm) {
		return null;
	}

	@Override
	public RealmModel read(Realm realm, Class<? extends RealmModel> model) {
		return null;
	}

	@Override
	public void deserializeStringListFromRealm() {}

	@Override
	public void serializeStringListForRealm() {}

	@Override
	public void write(@Nullable Realm realm) {}

	@Override
	public void write(Object object) {}

	@Override
	public void write(JSONObject json) {}

	@Override
	public void write(Realm realm, RealmModel model) {}

	@Override
	public void update() {}

	@Override
	public void update(Object object) {}

	@Override
	public void update(JSONObject json) {}

	@Override
	public RealmModelListener updateWithReturn() {
		return null;
	}

	@Override
	public RealmModelListener updateWithReturn(Object object) {
		return null;
	}

	@Override
	public RealmModelListener updateWithReturn(JSONObject json) {
		return null;
	}

	//endregion


	//region == Getters and setters

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getAvatarURL() {
		return avatarURL;
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}



	public boolean isFriend() {
		return isFriend;
	}
	public void setFriend(boolean friend) {
		isFriend = friend;
	}

	public boolean isShowInnerCircle() {
		return showInnerCircle;
	}
	public void setShowInnerCircle(boolean showInnerCircle) {
		this.showInnerCircle = showInnerCircle;
	}

	public boolean canBeInvitedToIC() {
		return canBeInvitedToIC;
	}
	public void setCanBeInvitedToIC(boolean canBeInvitedToIC) {
		this.canBeInvitedToIC = canBeInvitedToIC;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getWallImageLink() {
		return wallImageLink;
	}
	public void setWallImageLink(String wallImageLink) {
		this.wallImageLink = wallImageLink;
	}

	public int getHeartsGiven() {
		return heartsGiven;
	}
	public void setHeartsGiven(int heartsGiven) {
		this.heartsGiven = heartsGiven;
	}

	public int getHeartsReceived() {
		return heartsReceived;
	}
	public void setHeartsReceived(int heartsReceived) {
		this.heartsReceived = heartsReceived;
	}

	public int getTotHearts() {
		return totHearts;
	}
	public void setTotHearts(int totHearts) {
		this.totHearts = totHearts;
	}

	public String getRequestsStatusString() {
		return requestsStatusString;
	}
	public void setRequestsStatusString(String requestsStatusString) {
		this.requestsStatusString = requestsStatusString;
	}

	public RequestsStatusEnum getRequestsStatus() {
		return requestsStatus;
	}
	public void setRequestsStatus(RequestsStatusEnum requestsStatus) {
		this.requestsStatus = requestsStatus;
		if (requestsStatus != null)
			this.requestsStatusString = requestsStatus.toString();
	}

	public List<LifeEvent> getLifeEvents() {
		return lifeEvents;
	}
	public void setLifeEvents(RealmList<LifeEvent> lifeEvents) {
		this.lifeEvents = lifeEvents;
	}

	public HLUserAboutMe getAboutMe() {
		return aboutMe;
	}
	public void setAboutMe(HLUserAboutMe aboutMe) {
		this.aboutMe = aboutMe;
	}

	public HLUserAboutMeMore getMoreAboutMe() {
		return moreAboutMe;
	}
	public void setMoreAboutMe(HLUserAboutMeMore moreAboutMe) {
		this.moreAboutMe = moreAboutMe;
	}

	public RealmList<FamilyRelationship> getFamilyRelationships() {
		return familyRelationships;
	}
	public void setFamilyRelationships(RealmList<FamilyRelationship> familyRelationships) {
		this.familyRelationships = familyRelationships;
	}


	public String getSharedPeople() {
		return sharedPeople;
	}
	public void setSharedPeople(String sharedPeople) {
		this.sharedPeople = sharedPeople;
	}

	public String getChatRoomID() {
		return chatRoomID;
	}
	public void setChatRoomID(String chatRoomID) {
		this.chatRoomID = chatRoomID;
	}

	public int getChatStatus() {
		return chatStatus;
	}
	public void setChatStatus(int chatStatus) {
		this.chatStatus = chatStatus;
	}

	public String getLastSeenDate() {
		return lastSeenDate;
	}
	public void setLastSeenDate(String lastSeenDate) {
		this.lastSeenDate = lastSeenDate;
	}

	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean canChat() {
		return canChat;
	}
	public void setCanChat(boolean canChat) {
		this.canChat = canChat;
	}

	public boolean canVideocall() {
		return canVideocall;
	}
	public void setCanVideocall(boolean canVideocall) {
		this.canVideocall = canVideocall;
	}

	public boolean canAudiocall() {
		return canAudiocall;
	}
	public void setCanAudiocall(boolean canAudiocall) {
		this.canAudiocall = canAudiocall;
	}

	//endregion

}
