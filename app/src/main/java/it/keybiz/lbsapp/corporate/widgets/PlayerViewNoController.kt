/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView

/**
 * Subclass of [PlayerView] used because the defined specs applied to Toro library need that users
 * may want to actually control video playback through touch events like with a controller view,
 * but with custom controls, and not with autoplay.
 * <p>
 * To achieve so, Toro library <a href="https://github.com/eneim/toro/issues/263">workaround</a>.
 * Which necessarily leads to edit behavior when touched of provided PlayerView: we are forced to
 * "use view's controller", but we have to force that it is never shown.
 * @author mbaldrighi on 10/25/2018.
 */
class PlayerViewNoController constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0):
        PlayerView(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0) {
        PlayerViewNoController(context, attrs, 0)
    }

    constructor(context: Context) : this(context, null, 0) {
        PlayerViewNoController(context, null, 0)
    }

    override fun dispatchMediaKeyEvent(event: KeyEvent?): Boolean {
        useController = true
        val bool = super.dispatchMediaKeyEvent(event)
        useController = false
        return bool
    }

    fun isPlaying(): Boolean {
        return player?.playbackState == Player.STATE_READY && player.playWhenReady
    }

    fun play(delay: Boolean = false) {
        postDelayed(
                { player?.playWhenReady = true },
                if (delay) 700L else 0L
        )
    }

    fun pause() {
        player?.playWhenReady = false
    }

    fun stopPlayback() {
        player?.stop()
        player?.release()
        player = null
    }

    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0L
    }

    fun getBufferedPercentage(): Int {
        return player?.bufferedPercentage ?: 0
    }

    fun isValid(): Boolean {
        return player != null
    }

}