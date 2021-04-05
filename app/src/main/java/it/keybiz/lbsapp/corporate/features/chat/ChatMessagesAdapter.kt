/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.content.Context
import android.media.MediaPlayer
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import im.ene.toro.CacheManager
import im.ene.toro.exoplayer.ExoPlayerViewHelper
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import java.util.concurrent.atomic.AtomicInteger


/**
 * Custom adapter for chat MESSAGES view.
 * @author mbaldrighi on 10/15/2018.
 */
class ChatMessagesAdapter(val context: Context, messages: OrderedRealmCollection<ChatMessage>,
                          private val fragment: ChatMessagesFragment):
        RealmRecyclerViewAdapter<ChatMessage, ChatMessageVH>(messages, true), CacheManager {

    private val TYPE_INCOMING = 0
    private val TYPE_OUTGOING = 1
    private val TYPE_INCOMING_PHOTO = 2
    private val TYPE_OUTGOING_PHOTO = 3
    private val TYPE_INCOMING_VIDEO = 4
    private val TYPE_OUTGOING_VIDEO = 5
    private val TYPE_INCOMING_AUDIO = 6
    private val TYPE_OUTGOING_AUDIO = 7
    private val TYPE_INCOMING_LOCATION = 8
    private val TYPE_OUTGOING_LOCATION = 9
    private val TYPE_INCOMING_DOCUMENT = 10
    private val TYPE_OUTGOING_DOCUMENT = 11
    private val TYPE_INCOMING_WEBLINK = 12
    private val TYPE_OUTGOING_WEBLINK = 13

    var playAudioTask: AsyncTask<*,*,*>? = null
    var mediaPlayer: MediaPlayer? = null

    var videoView: PlayerView? = null
    var videoHelper: ExoPlayerViewHelper? = null
    var videoHolder: ChatMessageVHMediaVideo? = null

    var manualItem: AtomicInteger = AtomicInteger(-1)
    val dispatcher = DefaultControlDispatcher()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageVH {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            TYPE_INCOMING_PHOTO, TYPE_OUTGOING_PHOTO -> ChatMessageVHMediaPhoto(inflater.inflate(R.layout.item_chat_media_photo, parent, false), fragment)
            TYPE_INCOMING_AUDIO, TYPE_OUTGOING_AUDIO -> ChatMessageVHMediaAudio(inflater.inflate(R.layout.item_chat_media_audio, parent, false), fragment)
            TYPE_INCOMING_VIDEO, TYPE_OUTGOING_VIDEO -> ChatMessageVHMediaVideo(inflater.inflate(R.layout.item_chat_media_video, parent, false), fragment)
            TYPE_INCOMING_LOCATION, TYPE_OUTGOING_LOCATION -> ChatMessageVHMediaLocation(inflater.inflate(R.layout.item_chat_media_location, parent, false), fragment)
            TYPE_INCOMING_DOCUMENT -> ChatMessageVHDocument(inflater.inflate(R.layout.item_chat_balloon_incoming_document, parent, false), fragment)
            TYPE_OUTGOING_DOCUMENT -> ChatMessageVHDocument(inflater.inflate(R.layout.item_chat_balloon_outgoing_document, parent, false), fragment)
            TYPE_INCOMING_WEBLINK, TYPE_OUTGOING_WEBLINK -> ChatMessageVHMediaWebLink(inflater.inflate(R.layout.item_chat_media_weblink, parent, false), fragment)
            TYPE_INCOMING -> ChatMessageVHText(inflater.inflate(R.layout.item_chat_balloon_incoming, parent, false), fragment)
            else -> ChatMessageVHText(inflater.inflate(R.layout.item_chat_balloon_outgoing, parent, false), fragment)
        }
    }

    override fun onBindViewHolder(holder: ChatMessageVH, position: Int) {

        if (holder is ChatMessageVHMediaVideo) {
            (holder.playerView as? PlayerView)?.setControlDispatcher(object: ControlDispatcher {

                override fun dispatchSetPlayWhenReady(player: Player, playWhenReady: Boolean): Boolean {
                    val dispatched = dispatcher.dispatchSetPlayWhenReady(player, playWhenReady)
                    if (dispatched) {
                        this@ChatMessagesAdapter.manualItem.set(
                                if (!playWhenReady) -1 else holder.playerOrder
                        )
                    }
                    return dispatched
                }

                override fun dispatchSetShuffleModeEnabled(player: Player?, shuffleModeEnabled: Boolean): Boolean {
                    return dispatcher.dispatchSetShuffleModeEnabled(player, shuffleModeEnabled)
                }

                override fun dispatchSeekTo(player: Player, windowIndex: Int, positionMs: Long): Boolean {
                    return dispatcher.dispatchSeekTo(player, windowIndex, positionMs)
                }

                override fun dispatchSetRepeatMode(player: Player, repeatMode: Int): Boolean {
                    return dispatcher.dispatchSetRepeatMode(player, repeatMode)
                }

                override fun dispatchStop(player: Player?, reset: Boolean): Boolean {
                    return dispatcher.dispatchStop(player, reset)
                }
            })
        }

        if (data != null) holder.setMessage(data!!, position)
    }

    override fun getItemViewType(position: Int): Int {
        val message = data?.get(position)
        return when(message?.getDirectionType(fragment.getUser().userId)) {
            ChatMessage.DirectionType.INCOMING ->
                when {
                    message.hasLocation() -> TYPE_INCOMING_LOCATION
                    message.hasDocument() -> TYPE_INCOMING_DOCUMENT
                    message.hasWebLink() -> TYPE_INCOMING_WEBLINK
                    else -> when {
                        message.hasPicture() -> TYPE_INCOMING_PHOTO
                        message.hasVideo() -> TYPE_INCOMING_VIDEO
                        message.hasAudio() -> TYPE_INCOMING_AUDIO
                        else -> TYPE_INCOMING
                    }
                }
            ChatMessage.DirectionType.OUTGOING ->
                when {
                    message.hasLocation() -> TYPE_OUTGOING_LOCATION
                    message.hasDocument() -> TYPE_OUTGOING_DOCUMENT
                    message.hasWebLink() -> TYPE_OUTGOING_WEBLINK
                    else -> when {
                        message.hasPicture() -> TYPE_OUTGOING_PHOTO
                        message.hasVideo() -> TYPE_OUTGOING_VIDEO
                        message.hasAudio() -> TYPE_OUTGOING_AUDIO
                        else -> TYPE_OUTGOING
                    }
                }
            else -> -1
        }
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun getItemId(position: Int): Long {
        return data?.get(position)?.messageID.hashCode().toLong()
    }


    override fun onViewAttachedToWindow(holder: ChatMessageVH) {
        super.onViewAttachedToWindow(holder)

        when (holder) {
            is ChatMessageVHMediaAudio -> {
                // TODO: 10/23/2018     disables even this kind of auto play
//                if (holder.stoppedForScrolling && holder.lastPlayerPosition > 0) {
//                    holder.stoppedForScrolling = false
//                    holder.preparePlayerAndStart()
//                }

                playAudioTask = holder.playTask
                mediaPlayer = holder.mediaPlayer
            }

            is ChatMessageVHMediaVideo ->  {
                if (holder.playEnded) holder.playEnded = false
                videoHelper = holder.playerHelper
                videoHolder = holder
            }

            is ChatMessageVHMediaLocation -> {
                holder.initMap()
            }

            is ChatMessageVHMediaWebLink -> {
                holder.messageOutgoing?.setTextIsSelectable(true)
                holder.messageIncoming?.setTextIsSelectable(true)
            }
            is ChatMessageVHText, is ChatMessageVHDocument -> {
                (holder as? ChatMessageVHText)?.message?.setTextIsSelectable(true)
                (holder as? ChatMessageVHDocument)?.message?.setTextIsSelectable(true)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ChatMessageVH) {
        super.onViewDetachedFromWindow(holder)

        when (holder) {
            is ChatMessageVHMediaAudio -> {
                if (holder.playing) {
                    holder.mediaPlayer?.pause()
//                    holder.stoppedForScrolling = true         unused property
                }

                holder.setLastPlayerPosition()
                holder.lottieView?.pauseAnimation()
                holder.resetViews()
            }

            is ChatMessageVHMediaVideo -> {
                if (holder.isPlaying) {
                    holder.playerHelper?.pause()
                    holder.setLastPlayerPosition()

                    holder.resetViews(false)
                }
            }

            // TODO: 11/13/2018    RETURN HERE IF NEEDED
//            is ChatMessageVHMediaLocation -> {
//                holder.removeMap()
//            }
        }
    }

    override fun onViewRecycled(holder: ChatMessageVH) {
        super.onViewRecycled(holder)

        when (holder) {
            is ChatMessageVHMediaAudio -> {
                if (holder.playTask != null && !holder.playTask!!.isCancelled)
                    holder.playTask?.cancel(true)

                val mp = holder.mediaPlayer
                if (mp != null) {
                    if (mp.isPlaying)
                        mp.stop()

                    mp.release()
                    holder.mediaPlayer = null
                }
            }

            is ChatMessageVHMediaVideo ->  {
                holder.playerInError = false
                holder.playerHelper?.release()
                holder.playerHelper = null
                holder.resetViews(true)
            }

            is ChatMessageVHMediaLocation -> {
                holder.removeMap()
            }
        }
    }


    override fun getKeyForOrder(order: Int): Any? {
        if (data?.isNotEmpty() == true && data!!.size > order)
            return data!![order]
        return null
    }

    override fun getOrderForKey(key: Any): Int? {
        return if (key is ChatMessage && data?.isNotEmpty() == true) {
            val i = data?.indexOf(key) ?: 0
            if (i != -1) i else 0
        } else 0
    }


    //region == Class custom methods ==

    fun getMessageByID(messageID: String): Pair<Int, ChatMessage?> {
        val mess = data?.filter { chatMessage -> chatMessage.messageID == messageID }

        return if (mess?.size == 1) data!!.indexOf(mess[0]) to mess[0]
        else -1 to null
    }

    //endregion

}