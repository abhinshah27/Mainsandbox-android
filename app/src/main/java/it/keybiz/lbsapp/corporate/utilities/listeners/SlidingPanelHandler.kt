/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners

import android.os.Handler
import android.os.Message
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import java.lang.ref.WeakReference

/**
 * @author mbaldrighi on 9/13/2018.
 */
class SlidingPanelHandler internal constructor(panel: SlidingUpPanelLayout) : Handler() {

    companion object {
        const val ACTION_EXPAND = 0
    }

    private val panel: WeakReference<SlidingUpPanelLayout> = WeakReference(panel)

    override fun handleMessage(msg: Message) {
        if (msg.what == ACTION_EXPAND)
            panel.get()?.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
        super.handleMessage(msg)
    }
}