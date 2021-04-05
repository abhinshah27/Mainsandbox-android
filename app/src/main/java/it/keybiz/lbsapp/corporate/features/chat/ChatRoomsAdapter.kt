/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import io.realm.Case
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import io.realm.Sort
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.models.chat.ChatRecipientStatus
import it.keybiz.lbsapp.corporate.models.chat.ChatRoom
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.isSameDateAsNow
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper
import kotlinx.android.synthetic.main.fragment_chat_rooms.*
import kotlinx.android.synthetic.main.layout_search_plus_rv.view.*
import java.text.DateFormat
import java.util.*


/**
 * Custom adapter for chat ROOMS view.
 * @author mbaldrighi on 10/15/2018.
 */
class ChatRoomsAdapter(
        private val context: Context,
        rooms: OrderedRealmCollection<ChatRoom>,
        private val fragment: ChatRoomsFragment,
        private val deleteRoomListener: OnDeleteRoomListener
): RealmRecyclerViewAdapter<ChatRoom, ChatRoomsAdapter.ChatRoomVH>(rooms, true), Filterable {

    companion object {
        val LOG_TAG = ChatMessagesAdapter::class.qualifiedName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomVH {
        val inflater = LayoutInflater.from(parent.context)
        return ChatRoomVH(
                inflater.inflate(R.layout.item_chat_room, parent, false),
                fragment,
                deleteRoomListener
        )
    }

    override fun onBindViewHolder(holder: ChatRoomVH, position: Int) {
        holder.setRoom(data?.get(position), position)
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun getItemId(position: Int): Long {
        return data?.get(position)?.chatRoomID.hashCode().toLong()
    }


    //region == Class custom methods ==

    enum class FilterBy { PARTICIPANT, ROOM_ID }
    fun getRoomByID(fieldName: FilterBy, valueID: String): Pair<Int, ChatRoom?> {
        val rooms = data?.filter { chatRoom ->
            when (fieldName) {
                FilterBy.PARTICIPANT -> chatRoom.getParticipantId() == valueID
                FilterBy.ROOM_ID -> chatRoom.chatRoomID == valueID
            }
        }

        return if (rooms?.size == 1) data!!.indexOf(rooms[0]) to rooms[0]
        else -1 to null
    }

    //endregion

    //region == Filter ==

    fun filterResults(text: String) {
        val query = fragment.getRealm().where(ChatRoom::class.java)
        if(!text.toLowerCase().trim().isBlank()) {
            query.contains("roomName", text, Case.INSENSITIVE)
        }

        updateData(
                query.sort("dateObj", Sort.DESCENDING).findAllAsync()
                        .also {
                            it.addChangeListener { t, _ ->
                                fragment.activity?.runOnUiThread {
                                    fragment.chatRoomIncluded1?.no_result?.setText(
                                            if (text.isBlank()) R.string.no_result_chat_rooms
                                            else R.string.no_result_chat_rooms_search
                                    )
                                    fragment.chatRoomIncluded1?.no_result?.visibility = if (t.isEmpty()) View.VISIBLE else View.GONE
                                    fragment.chatRoomIncluded1?.generic_rv?.visibility = if (t.isEmpty()) View.GONE else View.VISIBLE
                                }
                            }
                        }
        )
    }

    override fun getFilter(): Filter {
        return ChatRoomFilter(this, fragment)
    }


    private class ChatRoomFilter(val adapter: ChatRoomsAdapter, val fragment: ChatRoomsFragment): Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults()
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            adapter.filterResults(constraint.toString())
        }
    }

    //endregion


    //region == View Holder ==

    class ChatRoomVH(itemView: View, private val fragment: ChatRoomsFragment, private val deleteRoomListener: OnDeleteRoomListener):
            RecyclerView.ViewHolder(itemView) {

        private var chatRoomPicture: ImageView
        private var chatRoomAvail: View
        private var chatRoomName: TextView
        private var chatRoomText: TextView
        private var chatLastSeen: TextView
        private var toReadCount: TextView

        private var currentRoom: ChatRoom? = null

        private var deleteMenu: PopupMenu? = null

        init {
            with (itemView) {
                this.setOnClickListener {
                    if (Utils.isContextValid(fragment.context) && currentRoom != null && currentRoom!!.isValid())
                        ChatActivity.openChatMessageFragment(
                                fragment.context!!,
                                currentRoom!!.chatRoomID!!,
                                currentRoom!!.getRoomName(),
                                currentRoom!!.getRoomAvatar()
                        )
                }
                this.setOnLongClickListener {
                    deleteMenu?.show()
                    true
                }

                val ppi = this.findViewById(R.id.picturePlusIndicator) as View
                chatRoomPicture = ppi.findViewById(R.id.profilePicture)
                chatRoomAvail = ppi.findViewById(R.id.availabilityIndicator)

                chatRoomName = this.findViewById(R.id.chatRoomName)
                chatRoomText = this.findViewById(R.id.chatText)
                chatLastSeen = this.findViewById(R.id.chatLastSeen)
                toReadCount = this.findViewById(R.id.toReadCount)

                deleteMenu = PopupMenu(this.context, this, (Gravity.BOTTOM or Gravity.END)).also {
                    it.inflate(R.menu.popup_menu_chat_delete)
                    it.setOnMenuItemClickListener { item ->
                        if (item.itemId == R.id.delete_chat && !currentRoom?.chatRoomID.isNullOrBlank()) {
                            // delete Room
                            deleteRoomListener.onRoomDeleted(currentRoom!!.chatRoomID!!)
                            true
                        } else false
                    }
                }
            }
        }

        fun setRoom(currentRoom: ChatRoom?, position: Int) {
            if (currentRoom != null) {
                this.currentRoom = currentRoom

                if (!currentRoom.getRoomAvatar().isNullOrBlank())
                   MediaHelper.loadProfilePictureWithPlaceholder(fragment.context, currentRoom.getRoomAvatar(), chatRoomPicture)
                else
                    chatRoomPicture.setImageResource(R.drawable.ic_profile_placeholder)

                chatRoomAvail.visibility =
                        if (currentRoom.recipientStatus == ChatRecipientStatus.ONLINE.value)
                            View.VISIBLE
                        else View.GONE
                chatRoomName.text = currentRoom.getRoomName()
                chatRoomText.text = currentRoom.text

                val date = currentRoom.dateObj
                if (date != null) {
                    chatLastSeen.text =
                            try {
                                if (date.isSameDateAsNow()) Utils.formatTime(fragment.context, date)
                                else DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
                            } catch (e: Exception) {
                                LogUtils.e(LOG_TAG, e)
                                ""
                            }
                }

                with(currentRoom.toRead) {
                    if (this == 0) toReadCount.visibility = View.GONE
                    else {
                        toReadCount.text = this.toString()
                        toReadCount.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    interface OnDeleteRoomListener {
        fun onRoomDeleted(chatID: String)
    }

    //endregion

}