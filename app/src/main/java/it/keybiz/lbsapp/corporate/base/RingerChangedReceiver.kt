/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

/**
 * @author mbaldrighi on 11/30/2018.
 */
class RingerChangedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if (isInitialStickyBroadcast) {
            val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            parseRingerMode(audioManager?.ringerMode ?: -1)

            return
        }

        if (intent != null) {

            parseRingerMode(intent.extras?.getInt(AudioManager.EXTRA_RINGER_MODE) ?: -1)
        }
    }

    private fun parseRingerMode(mode: Int) {
        val pair = when (mode) {
            AudioManager.RINGER_MODE_NORMAL -> true to true
            AudioManager.RINGER_MODE_SILENT -> false to false
            AudioManager.RINGER_MODE_VIBRATE -> false to true
            else -> true to true
        }

        LBSLinkApp.canPlaySounds = pair.first
        LBSLinkApp.canVibrate = pair.second
    }

}