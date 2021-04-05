/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.text.Spannable
import android.text.SpannableString
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityOptionsCompat
import androidx.core.text.PrecomputedTextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import im.ene.toro.ToroPlayer
import im.ene.toro.ToroUtil
import im.ene.toro.exoplayer.ExoPlayerViewHelper
import im.ene.toro.exoplayer.Playable
import im.ene.toro.media.PlaybackInfo
import im.ene.toro.media.VolumeInfo
import im.ene.toro.widget.Container
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.features.viewers.PhotoViewActivity
import it.keybiz.lbsapp.corporate.features.viewers.VideoViewActivity
import it.keybiz.lbsapp.corporate.models.HLWebLink
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.models.chat.ChatMessageType
import it.keybiz.lbsapp.corporate.utilities.*
import it.keybiz.lbsapp.corporate.utilities.caches.AudioVideoCache
import it.keybiz.lbsapp.corporate.utilities.caches.PicturesCache
import it.keybiz.lbsapp.corporate.utilities.helpers.WebLinkRecognizer
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper
import it.keybiz.lbsapp.corporate.widgets.PlayerViewNoController
import it.keybiz.lbsapp.corporate.widgets.RoundedCornersBackgroundSpan
import java.io.File
import java.util.*

/**
 * Base class for chat items' view holders.
 * @author mbaldrighi on 10/22/2018.
 */
open class ChatMessageVH(itemView: View, internal val fragment: ChatMessagesFragment):
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

    private var dateHeader: TextView? = null

    private var prevMessage: ChatMessage? = null
    var nextMessage: ChatMessage? = null
    var currentMessage: ChatMessage? = null
    var isIncoming: Boolean = false

    val context: Context by lazy { itemView.context }

    var callingSendMessageInError = false

    var currentPosition: Int = 0

    init {
        with(itemView) {
            this.setOnClickListener(this@ChatMessageVH)
            dateHeader = this.findViewById(R.id.dateHeader)
        }
    }

    open fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        currentMessage = messages[position]
        currentPosition = position
        isIncoming = currentMessage?.getDirectionType(fragment.getUser().userId) == ChatMessage.DirectionType.INCOMING

        if (callingSendMessageInError && (currentMessage?.isError == false))
            callingSendMessageInError = false

        val paddingTop = when (position) {
            messages.size - 1 -> context.resources.getDimensionPixelSize(R.dimen.activity_margin)
            else -> context.resources.getDimensionPixelSize(R.dimen.activity_margin_md)
        }
        val paddingBottom = context.resources.getDimensionPixelSize(R.dimen.activity_margin_md)
//        when(position) {
//            messages.size - 1 -> context.resources.getDimensionPixelSize(R.dimen.activity_margin_lg)
//            else -> context.resources.getDimensionPixelSize(R.dimen.activity_margin_md)
//        }

        itemView.setPadding(0, paddingTop, 0, paddingBottom)

        prevMessage = if (messages.size > position + 1) messages[position + 1] else null
        nextMessage = if (position > 0) messages[position - 1] else null

        dateHeader?.text = Utils.formatDateWithTime(fragment.context, currentMessage?.creationDateObj, "EEE, dd MMM", true)
        dateHeader?.visibility = if (currentMessage?.isSameDateAs(prevMessage) == true) {
            when {
                this@ChatMessageVH is ChatMessageVHText -> View.GONE
                this@ChatMessageVH is ChatMessageVHDocument -> View.GONE
                this@ChatMessageVH is ChatMessageVHMedia -> View.INVISIBLE
                else -> View.VISIBLE
            }
        } else View.VISIBLE
    }

    internal fun getActivity(): Activity? {
        return context as? Activity
    }

    internal fun getInfoText(messageItem: ChatMessage?): String {
        return if (!isIncoming) {
            when (messageItem?.getStatusEnum()) {
                ChatMessage.Status.ERROR -> context.getString(R.string.chat_message_status_error)
                ChatMessage.Status.SENDING -> context.getString(R.string.chat_message_status_sending)
                ChatMessage.Status.SENT -> context.getString(R.string.chat_message_status_sent)
                ChatMessage.Status.DELIVERED -> context.getString(R.string.chat_message_status_delivered)
                ChatMessage.Status.READ -> {
                    context.getString(
                            R.string.chat_message_status_read,
                            ", ${getFormattedDateForMessages(messageItem.readDateObj)}"
                    )
                }
                ChatMessage.Status.OPENED -> {
                    context.getString(
                            R.string.chat_message_status_opened,
                            ", ${getFormattedDateForMessages(messageItem.openedDateObj)}"
                    )
                }
                else -> ""
            }
        } else getFormattedDateForMessages(messageItem?.creationDateObj)
    }

    override fun onClick(v: View?) {
        fragment.lastScrollPosition = currentPosition

        if (currentMessage?.isError == true) {
            fragment.callServer(ChatMessagesFragment.CallType.NEW_MESSAGE, currentMessage)
            callingSendMessageInError = true
        }
    }


    private fun getFormattedDateForMessages(date: Date?): String {
        return if (date?.isSameDateAsNow() == true)
            Utils.formatTime(fragment.context, date)
        else Utils.formatDateWithTime(fragment.context, date, "EEE, dd MMM", true)
    }

}

/**
 * Specific VH for text messages, handling an own "single" footer with message status.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHText(itemView: View, fragment: ChatMessagesFragment): ChatMessageVH(itemView, fragment) {
    private var infoFooter: TextView? = null
    var message: TextView? = null

    init {
        with(itemView) {
            infoFooter = this.findViewById(R.id.infoFooter)
            message = this.findViewById(R.id.message)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        with(infoFooter) {
            this?.visibility = View.VISIBLE
            this?.text = getInfoText(currentMessage)
        }

        message?.text = currentMessage?.text

        // start web link recognition
        if (Utils.isContextValid(fragment.context)) {
            WebLinkRecognizer.getInstance(fragment.context!!)
                    .recognize(currentMessage?.text, currentMessage?.messageID)
        }
    }


    // for now we're allowed to use always the "arrowed" background
    private fun handleBalloonBackground(messageItem: ChatMessage?, nextMessage: ChatMessage?) {
        val userId = fragment.getUser().userId
        with(message) {
            when (messageItem?.getDirectionType(userId)) {
                ChatMessage.DirectionType.INCOMING -> {
                    when {
                        nextMessage?.getDirectionType(userId) == ChatMessage.DirectionType.INCOMING -> this?.setBackgroundResource(R.drawable.background_chat_incoming)
                        else -> this?.setBackgroundResource(R.drawable.background_chat_incoming_last)
                    }
                }
                ChatMessage.DirectionType.OUTGOING -> {
                    when {
                        nextMessage?.getDirectionType(userId) == ChatMessage.DirectionType.OUTGOING -> this?.setBackgroundResource(R.drawable.background_chat_outgoing)
                        else -> this?.setBackgroundResource(R.drawable.background_chat_outgoing_last)
                    }
                }
                else -> Toast.makeText(context, "Error in choosing right background", Toast.LENGTH_SHORT).show()
            }
        }
    }

}


/**
 * Specific VH for [ChatMessageType.DOCUMENT] messages, handling an own "single" footer with message
 * status, and the icon for the document extension.
 * @author mbaldrighi on 12/14/2018.
 */
class ChatMessageVHDocument(itemView: View, fragment: ChatMessagesFragment): ChatMessageVH(itemView, fragment) {
    private var infoFooter: TextView? = null
    var message: TextView? = null
    private var icon: ImageView? = null

    init {
        with(itemView) {
            infoFooter = this.findViewById(R.id.infoFooter)
            message = this.findViewById(R.id.message)
            icon = this.findViewById(R.id.documentIcon)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        with (infoFooter) {
            this?.visibility = View.VISIBLE
            this?.text = getInfoText(currentMessage)
        }

        with (currentMessage?.sharedDocumentFileName) {
            message?.text = this; handleDocumentIcon(this)
        }
    }


    private fun handleDocumentIcon(fileName: String?) {
        val ext = MediaHelper.getFileExtension(fileName)
        if (!ext.isNullOrBlank()) {
            val icon = when(ext) {
                "docx", "doc" -> if (isIncoming) R.drawable.ic_doc_black else R.drawable.ic_doc_white
                "xlsx", "xls" -> if (isIncoming) R.drawable.ic_xls_black else R.drawable.ic_xls_white
                "pptx", "ppt" -> if (isIncoming) R.drawable.ic_ppt_black else R.drawable.ic_ppt_white
                "pdf" -> if (isIncoming) R.drawable.ic_pdf_black else R.drawable.ic_pdf_white
//                "rtf" -> 0
//                "txt" -> 0
                else -> 0
            }

            if (icon != 0)
                this@ChatMessageVHDocument.icon?.setImageResource(icon)
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (!currentMessage?.sharedDocumentFileName.isNullOrBlank()) {
            Utils.fireBrowserIntentForDocs(
                    fragment.context,
                    currentMessage!!.mediaURL,
                    currentMessage!!.sharedDocumentFileName,
                    if (isIncoming && currentMessage?.isOpened() == false) currentMessage?.messageID else null
            )
        } else fragment.getActivityListener().showGenericError()
    }

}


/**
 * Class handling messages whose [ChatMessage.hasMedia] method returns TRUE. We could not use
 * [ChatMessageVH] class because this type of view holders has to manage "both" profile pictures and
 * footers.
 * @author mbaldrighi on 10/22/2018.
 */
open class ChatMessageVHMedia(itemView: View, fragment: ChatMessagesFragment): ChatMessageVH(itemView, fragment), View.OnClickListener {
    private var groupIncoming: Group? = null
    private var groupOutgoing: Group? = null
    private var infoIncoming: TextView? = null
    private var infoOutgoing: TextView? = null
    private var profileIncoming: ImageView? = null
    private var profileOutgoing: ImageView? = null

    init {
        with(itemView) {
            //            this.setOnClickListener(this@ChatMessageVHMedia)
            groupIncoming = this.findViewById(R.id.groupIncoming)
            groupOutgoing = this.findViewById(R.id.groupOutgoing)
            infoIncoming = this.findViewById(R.id.infoFooterIncoming)
            infoOutgoing = this.findViewById(R.id.infoFooterOutgoing)
            profileIncoming = this.findViewById(R.id.profileIncoming)
            profileOutgoing = this.findViewById(R.id.profileOutgoing)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        val userId = fragment.getUser().userId
        if (currentMessage?.getDirectionType(userId) == ChatMessage.DirectionType.INCOMING) {
            groupIncoming?.visibility = View.VISIBLE
            groupOutgoing?.visibility = View.INVISIBLE
        } else {
            groupOutgoing?.visibility = View.VISIBLE
            groupIncoming?.visibility = View.INVISIBLE
        }

        val triple: Triple<String?, ImageView?, TextView?> =
                if (currentMessage?.getDirectionType(userId) == ChatMessage.DirectionType.INCOMING)
                    Triple(fragment.participantAvatar, profileIncoming, infoIncoming)
                else
                    Triple(fragment.getUser().userAvatarURL, profileOutgoing, infoOutgoing)

        MediaHelper.loadProfilePictureWithPlaceholder(itemView.context, triple.first, triple.second)
        triple.third?.text = getInfoText(currentMessage)
    }

    internal open fun setLastPlayerPosition() {}
    internal open fun preparePlayerAndStart(resetMedia: Boolean = false) {}

    override fun onClick(v: View?) {
        super.onClick(v)
    }
}

/**
 * Class handling [ChatMessageType.PICTURE] message type.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaPhoto(itemView: View, fragment: ChatMessagesFragment): ChatMessageVHMedia(itemView, fragment) {
    private var imageView: ImageView? = null

    init {
        with(itemView) {
            imageView = this.findViewById(R.id.imageView)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        val instance = PicturesCache.getInstance(fragment.context!!)
        val media = instance.getMedia(currentMessage?.mediaURL, HLMediaType.PHOTO)

        if (imageView != null) {
            when (media) {
                is File -> GlideApp.with(fragment).load(media).centerCrop().into(imageView!!)
                is Uri -> GlideApp.with(fragment).load(File(media.path)).centerCrop().into(imageView!!)
                else -> MediaHelper.loadPictureWithGlide(itemView.context, currentMessage?.mediaURL, imageView)
            }
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (callingSendMessageInError) return

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                context as Activity,
                imageView!!,
                ViewCompat.getTransitionName(imageView!!)!!)

        context.startActivity(
                Intent(context, PhotoViewActivity::class.java).let {
                    it.putExtra(Constants.EXTRA_PARAM_1, currentMessage?.mediaURL)
                    it.putExtra(Constants.EXTRA_PARAM_2, ViewCompat.getTransitionName(imageView!!))
                    it.putExtra(Constants.EXTRA_PARAM_3, currentMessage?.messageID)
                    it.putExtra(Constants.EXTRA_PARAM_4, true)
                },
                options.toBundle()
        )
    }
}

/**
 * Class handling [ChatMessageType.AUDIO] message type.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaAudio(itemView: View, fragment: ChatMessagesFragment): ChatMessageVHMedia(itemView, fragment) {
    private var audioView: View? = null
    private var playBtn: View? = null
    private var pauseBtn: View? = null
    private var progressView: View? = null
    private var pauseLayout: View? = null
    private var pauseFrame = 0
    private var siriComposition: LottieComposition? = null
    var lottieView: LottieAnimationView? = null

    var playTask: AsyncTask<*, *, *>? = null
    var mediaPlayer: MediaPlayer? = null
    var playing = false
        get() { return if (isMediaPlayerInError) false else (mediaPlayer?.isPlaying ?: false) }
    var lastPlayerPosition: Int = 0

    var stoppedForScrolling = false

    var isMediaPlayerInError = false


    init {
        with(itemView) {

            audioView = this.findViewById(R.id.audioView)

            lottieView = this.findViewById(R.id.wave3)
            playBtn = this.findViewById(R.id.play_btn)
            playBtn!!.setOnClickListener {
                if (Utils.isStringValid(currentMessage?.mediaURL)) preparePlayerAndStart()
            }
            pauseBtn = this.findViewById(R.id.pause_btn)
            progressView = this.findViewById(R.id.progress_layout)
            pauseLayout = this.findViewById(R.id.pause_layout)
            pauseLayout?.setOnClickListener {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
                lastPlayerPosition = mediaPlayer?.currentPosition ?: 0
//                pauseLayout?.visibility = View.GONE
                pauseBtn?.visibility = View.GONE
                playing = false
                playBtn?.visibility = View.VISIBLE
                lottieView?.pauseAnimation()
                pauseFrame = lottieView?.frame ?: 0
            }

            (this.findViewById(R.id.shareBtn) as View).setOnClickListener {
                fragment.lastScrollPosition = currentPosition

                if (!currentMessage?.mediaURL.isNullOrBlank()) {
                    fragment.currentMessageId = currentMessage?.messageID
                    fragment.shareHelper.initOps(true)
                }
            }
        }

        siriComposition = LBSLinkApp.siriComposition
        if (siriComposition != null)
            lottieView?.setComposition(siriComposition!!)
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        audioView?.setBackgroundColor(Color.BLACK)

        // TODO: 5/6/2018    DISABLES AUTOPLAY AUDIO
//		if (getActivity().getUser().feedAutoplayAllowed(getActivity()) &&
//				super.fragment != null && super.fragment.isVisibleToUser()) {
//
//			if (mediaPlayer == null)
//				mediaPlayer = new MediaPlayer();
//
//			if (!mediaPlayer.isPlaying()) {
//				playBtn.setVisibility(View.GONE);
//				pauseBtn.setVisibility(View.GONE);
//				progressView.setVisibility(View.GONE);
//
//				playTask = new PlayAudioTask(mediaPlayer, playBtn, pauseBtn, progressView, playing,
//						lastPlayerPosition, animationView, pauseFrame)
//						.execute(mItem.getContent());
//			}
//		}
//		else {
        playBtn?.visibility = View.VISIBLE
        pauseBtn?.visibility = View.GONE
        progressView?.visibility = View.GONE
//		}


        //TO MAKE SURE WAVE IS SET TO 0 WHEN VIEW IS ATTACHED.
        lottieView?.frame = pauseFrame
    }

    fun resetViews() {
        lottieView?.frame = pauseFrame

        playBtn?.visibility = View.VISIBLE
        pauseBtn?.visibility = View.GONE
    }

    override fun preparePlayerAndStart(resetMedia: Boolean) {

        var media = AudioVideoCache.getInstance(fragment.context!!).getMedia(currentMessage?.mediaURL, HLMediaType.AUDIO)
        if (media == null)
            media = currentMessage?.mediaURL

        if (mediaPlayer == null) mediaPlayer = MediaPlayer()
        isMediaPlayerInError = false
        (mediaPlayer as MediaPlayer).setOnErrorListener { mp, _, _ ->
            isMediaPlayerInError = true
            mp.reset()
            true
        }
        playTask = MediaHelper.getAudioTask(if (mediaPlayer != null) mediaPlayer else MediaPlayer(), playBtn, pauseBtn,
                progressView, playing, lastPlayerPosition, lottieView, pauseFrame)
                .execute(media)
    }

    override fun setLastPlayerPosition() {
        lastPlayerPosition = mediaPlayer?.currentPosition ?: 0
        pauseFrame = lottieView?.frame ?: 0
    }
}


/**
 * Class handling [ChatMessageType.VIDEO] message type.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaVideo(itemView: View, fragment: ChatMessagesFragment): ChatMessageVHMedia(itemView, fragment),
        ToroPlayer {

    internal var playerHelper: ExoPlayerViewHelper? = null

    private var videoView: PlayerViewNoController? = null
    private var thumbnailView: ImageView? = null
    private var thumbnailViewLayout: View? = null

    private var playBtn: View? = null
    private var progressView: View? = null
    private var progressBar: View? = null
    private var progressMessage: TextView? = null

    private var lastPlayerPosition = 0L

    internal var playEnded: Boolean = false
    internal var playerInError: Boolean = false

    private var wantsLandscape: Boolean = false


    init {
        with(itemView) {
            videoView = (this.findViewById(R.id.video_view) as PlayerViewNoController).apply {
                this.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER)
                this.setBackgroundColor(Color.BLACK)
            }
            thumbnailView = this.findViewById(R.id.video_view_thumbnail)
            thumbnailViewLayout = this.findViewById(R.id.video_view_thumbnail_layout)

            playBtn = (this.findViewById(R.id.play_btn) as View).apply {
                this.setOnClickListener {
                    if (fragment.isAutoPlayOn()) {
                        videoView?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                        preparePlayerAndStart(playEnded)
                    }
                    else if (!playerInError) openFullScreenActivity()
                }
            }
            progressView = this.findViewById(R.id.progress_layout)
            progressBar = progressView!!.findViewById(R.id.progress)
            progressMessage = progressView!!.findViewById(R.id.progress_message)
            progressMessage!!.setText(R.string.buffering_video)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        videoView?.setBackgroundColor(Color.BLACK)

        if (!isPlaying) {
            playBtn?.visibility = View.VISIBLE
            progressView?.visibility = View.GONE
        }

        if (Utils.hasLollipop())
            videoView?.transitionName = currentMessage?.mediaURL

        applyThumbnail(true)
    }

    override fun setLastPlayerPosition() {
        lastPlayerPosition = playerHelper?.latestPlaybackInfo?.resumePosition ?: 0
    }

    override fun preparePlayerAndStart(resetMedia: Boolean) {
        if (resetMedia) videoView?.player?.seekTo(0)
        playEnded = false
        play()
    }

    fun resetViews(recycled: Boolean) {
        playBtn?.visibility = View.VISIBLE
        progressMessage?.setText(R.string.buffering_video)
        if (recycled)
            thumbnailViewLayout?.visibility = View.VISIBLE
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (callingSendMessageInError || playerInError) return

        openFullScreenActivity()

        /*
         * Due to bug for some situations dispatchMediaKeyEvent is triggered but almost ignored by another change in player state with playWhenReady==true
         */
//        when {
//            isPlaying ->  {
//                videoView?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
//                playBtn?.visibility = View.VISIBLE
//                setLastPlayerPosition()
//                pause()
//            }
//            else -> openFullScreenActivity()
//        }
    }

    override fun isPlaying(): Boolean {
        return playerHelper?.isPlaying ?: false
    }

    override fun getPlayerView(): View {
        return videoView!!
    }

    override fun pause() {
        playerHelper?.pause()
    }

    override fun wantsToPlay(): Boolean {
        return fragment.isAutoPlayOn() && ToroUtil.visibleAreaOffset(this, itemView.parent) >= 0.40
    }

    override fun play() {
        if (!playEnded) playerHelper?.play()
    }

    override fun getCurrentPlaybackInfo(): PlaybackInfo {
        return playerHelper?.latestPlaybackInfo!!
    }

    override fun release() {
        playerHelper?.release()
    }

    override fun initialize(container: Container, playbackInfo: PlaybackInfo) {

        var media = AudioVideoCache.getInstance(context).getMedia(currentMessage?.mediaURL, HLMediaType.VIDEO)
        if (media == null)
            media = Uri.parse(currentMessage?.mediaURL ?: "")

        if (playerHelper == null)
            playerHelper = ExoPlayerViewHelper(this, media as Uri)

        playerHelper?.addEventListener(object: Playable.DefaultEventListener() {

            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                wantsLandscape = width > height
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)

                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        progressBar?.visibility = View.VISIBLE
                        progressView?.visibility = if (playEnded) View.GONE else View.VISIBLE
                        playBtn?.visibility = View.GONE
                    }

                    Player.STATE_READY -> {
                        progressView?.visibility = View.GONE
                        playEnded = playEnded && !playWhenReady
                        applyThumbnail(!playWhenReady || playEnded)
                        playBtn?.visibility = if (playWhenReady) View.GONE else View.VISIBLE
                    }

                    Player.STATE_ENDED -> {
                        playEnded = true
                        videoView?.player?.seekTo(0)
                        videoView?.pause()
                    }
                }
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                super.onPlayerError(error)

                playerInError = true
                progressBar?.visibility = View.GONE
                progressMessage?.setText(R.string.error_playback_video)
                LogUtils.e("ExoPlayer in FEED", "ERROR: ${error?.message}")
            }
        })
        playbackInfo.volumeInfo = VolumeInfo(true, 0f)

        playerHelper?.initialize(container, playbackInfo)
    }

    override fun getPlayerOrder(): Int {
        return adapterPosition
    }

    private fun applyThumbnail(show: Boolean) {
        when (playerHelper?.latestPlaybackInfo?.resumePosition) {
            null, 0L -> {
                MediaHelper.loadPictureWithGlide(
                        getActivity(),
                        extractThumbnailFromUrl(),
                        RequestOptions.fitCenterTransform(),
                        0,
                        0,
                        object: CustomViewTarget<ImageView, Drawable>(thumbnailView as ImageView) {
                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                thumbnailView?.visibility = View.GONE
                            }

                            override fun onResourceCleared(placeholder: Drawable?) {}

                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                thumbnailView?.setImageDrawable(resource)
                                thumbnailView?.visibility = View.VISIBLE
                            }
                        }
                )
                thumbnailViewLayout?.visibility = if (show) View.VISIBLE else View.GONE
            }
            else -> thumbnailViewLayout?.visibility = View.GONE
        }
    }

    private fun extractThumbnailFromUrl(): String {
        return if (!currentMessage?.videoThumbnail.isNullOrBlank()) currentMessage!!.videoThumbnail ?: ""
        else if (!currentMessage?.mediaURL.isNullOrBlank()) {
            val path = currentMessage!!.mediaURL.substring(0, currentMessage!!.mediaURL.lastIndexOf("/") + 1)
            val fileName = currentMessage!!.mediaURL.substring(currentMessage!!.mediaURL.lastIndexOf("/") + 1)
            val thumbParts = fileName.split(".")
            if (thumbParts.size == 2) {
                val thumb = path.plus(thumbParts[0]).plus("_thumb.jpg")
                val id = currentMessage?.messageID
                (fragment.activity as? HLActivity)?.realm?.executeTransactionAsync {
                    (RealmUtils.readFirstFromRealmWithId(it, ChatMessage::class.java, "messageID", id ?: "") as? ChatMessage)?.videoThumbnail = thumb
                }
                thumb
            } else ""
        } else ""
    }

    private fun openFullScreenActivity() {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                context as Activity,
                videoView!!,
                ViewCompat.getTransitionName(videoView!!)!!)

        // bundle deactivated because of ugly lag  during activity start.

        fragment.startActivityForResult(
                Intent(context, VideoViewActivity::class.java).let {
                    it.putExtra(Constants.EXTRA_PARAM_1, currentMessage?.mediaURL)
                    it.putExtra(Constants.EXTRA_PARAM_2, currentMessage?.videoThumbnail)
                    it.putExtra(Constants.EXTRA_PARAM_3, ViewCompat.getTransitionName(videoView!!))
                    it.putExtra(Constants.EXTRA_PARAM_4, lastPlayerPosition)
                    it.putExtra(Constants.EXTRA_PARAM_5, wantsLandscape)
                    it.putExtra(Constants.EXTRA_PARAM_6, currentMessage?.messageID)
                    it.putExtra(Constants.EXTRA_PARAM_7, true)
                },
                Constants.RESULT_FULL_VIEW_VIDEO
//                ,options.toBundle()
        )
    }
}

/**
 * Class handling [ChatMessageType.LOCATION] message type.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaLocation(itemView: View, fragment: ChatMessagesFragment): ChatMessageVHMedia(itemView, fragment),
        OnMapReadyCallback {

    private var mapView: MapView? = null
    private var map: GoogleMap? = null

    private var location: LatLng = LatLng(0.0, 0.0)

    init {
        mapView = itemView.findViewById(R.id.map)
        mapView?.onCreate(null)
        mapView?.getMapAsync(this@ChatMessageVHMediaLocation)
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        mapView?.onResume()

        val split = currentMessage?.location?.split(";")
        try {
            if (split?.size == 2)
                location = LatLng(split[0].toDouble(), split[1].toDouble())
        } catch (e: NumberFormatException) {
            LogUtils.e("ChatMessageLocationVH", "Corrupted location: ${currentMessage?.location}, ${split.toString()}")
        }
    }

    override fun onMapReady(p0: GoogleMap?) {
        MapsInitializer.initialize(fragment.context)
        map = p0
        initMap()
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (callingSendMessageInError) return

        val gmmIntentUri = Uri.parse("geo:${location.latitude},${location.longitude}?z=17")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(context.packageManager) != null)
            context.startActivity(mapIntent)
        else
            Toast.makeText(context, R.string.error_generic_operation, Toast.LENGTH_SHORT).show()
    }

    fun initMap() {

        map?.mapType = GoogleMap.MAP_TYPE_NORMAL

        map?.uiSettings?.isIndoorLevelPickerEnabled = false
        map?.uiSettings?.isMyLocationButtonEnabled = false
        map?.setOnMapClickListener { onClick(null) }
        map?.setOnMarkerClickListener { onClick(null);true }

        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 17f))
        map?.addMarker(
                MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker())
                        .position(location)
        )
    }

    fun removeMap() {
        map?.clear()
        map?.mapType = GoogleMap.MAP_TYPE_NONE

        mapView?.onPause()
    }

}


/**
 * Class handling [ChatMessageType.WEB_LINK] message type.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaWebLink(itemView: View, fragment: ChatMessagesFragment): ChatMessageVHMedia(itemView, fragment) {

    companion object {
        private var backgrSpan: RoundedCornersBackgroundSpan? = null
    }

    private var imageView: ImageView? = null
    var messageIncoming: TextView? = null
    var messageOutgoing: TextView? = null
    private var webLinkTitle: AppCompatTextView? = null
    private var webLinkSource: TextView? = null
    private var placeHolderIcon: View? = null

    private var webLinkObj: HLWebLink? = null


    init {
        with(itemView) {
            imageView = this.findViewById(R.id.imageView)
            messageIncoming = this.findViewById(R.id.messageIncoming)
            messageOutgoing = this.findViewById(R.id.messageOutgoing)
            webLinkTitle = this.findViewById(R.id.weblinkTitle)
            webLinkSource = this.findViewById(R.id.weblinkSource)
            placeHolderIcon = this.findViewById(R.id.placeHolder)

            if (backgrSpan == null) {
                backgrSpan = RoundedCornersBackgroundSpan(
                        Utils.getColor(this.context, R.color.black_70),
                        10,
                        0f
                )
            }
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        webLinkObj = currentMessage?.webLink

        if (!webLinkObj?.title.isNullOrBlank()) {

            webLinkTitle?.setShadowLayer(10f, 0f, 0f, Color.TRANSPARENT)
            webLinkTitle?.setPadding(10, 10, 10, 10)
            webLinkTitle?.setLineSpacing(2f, 1f)

            val spanned = SpannableString(webLinkObj!!.title)
            spanned.setSpan(
                    backgrSpan,
                    0,
                    webLinkObj!!.title!!.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )

            // Pass text computation future to AppCompatTextView,
            // which awaits result before measuring.
            webLinkTitle?.setTextFuture(
                    PrecomputedTextCompat.getTextFuture(
                            spanned,
                            TextViewCompat.getTextMetricsParams(webLinkTitle!!),
                            /*optional custom executor*/ null
                    )
            )
        } else
            webLinkTitle?.text = ""

        webLinkSource?.text = webLinkObj?.source

        with (WebLinkRecognizer.extractAppendedText(webLinkObj?.link, currentMessage?.text)) {
            if (isIncoming) {
                if (!this.isBlank()) {
                    messageIncoming?.text = this
                    messageIncoming?.visibility = View.VISIBLE
                }
                else messageIncoming?.visibility = View.GONE
            } else {
                if (!this.isBlank()) {
                    messageOutgoing?.text = this
                    messageOutgoing?.visibility = View.VISIBLE
                }
                else messageOutgoing?.visibility = View.GONE
            }
        }

        if (imageView != null) {
            val string = if (webLinkObj?.mediaURL.isNullOrBlank()) Constants.WEB_LINK_PLACEHOLDER_URL else webLinkObj?.mediaURL
            MediaHelper.loadPictureWithGlide(itemView.context, string, null, 0, 0, object : CustomViewTarget<ImageView, Drawable>(imageView!!) {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    MediaHelper.loadPictureWithGlide(itemView.context, Constants.WEB_LINK_PLACEHOLDER_URL, getView())
                }

                override fun onResourceCleared(placeholder: Drawable?) {
                    MediaHelper.loadPictureWithGlide(itemView.context, Constants.WEB_LINK_PLACEHOLDER_URL, getView())
                }

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    if (webLinkObj?.mediaURL?.contains("png", true) == true)
                        placeHolderIcon?.visibility = View.GONE

                    imageView!!.setImageDrawable(resource)
                }
            })
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (callingSendMessageInError) return

        Utils.fireBrowserIntentWithShare(fragment.context, webLinkObj?.link, webLinkObj?.source, currentMessage?.messageID, true)
    }
}