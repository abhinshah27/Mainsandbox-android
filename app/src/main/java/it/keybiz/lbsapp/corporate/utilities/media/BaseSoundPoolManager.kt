/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.media

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import it.keybiz.lbsapp.corporate.base.LBSLinkApp

/**
 * Manager class defining basic functions.
 * @author mbaldrighi on 11/30/2018.
 */
open class BaseSoundPoolManager(
        val context: Context,
        private val sounds: Array<Int>?,
        private val loadListener: SoundPool.OnLoadCompleteListener?
) {

    companion object {
        val LOG_TAG = BaseSoundPoolManager::class.qualifiedName
    }

    private val soundPool by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            android.media.SoundPool.Builder().setMaxStreams(1).build()
        else android.media.SoundPool(1, AudioManager.STREAM_SYSTEM, 0)
    }

    private var actualVolume: Float = 0.toFloat()
    private var maxVolume: Float = 0.toFloat()
    private var volume: Float = 0.toFloat()

    private var soundsLoaded = arrayOfNulls<Boolean?>(sounds?.size ?: 0)
    private var soundIds = arrayOfNulls<Int?>(sounds?.size ?: 0)


    open fun init() {
        // AudioManager audio settings for adjusting the volume
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        actualVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        if (maxVolume != 0f)
            volume = actualVolume / maxVolume

        soundPool.setOnLoadCompleteListener(loadListener ?: SoundPool.OnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                val index = soundIds.indexOf(sampleId)
                if (index > -1)
                    soundsLoaded[index] = true
            }
        })

        if (!sounds.isNullOrEmpty()) {
            for (i in 0 until sounds.size) {
                soundsLoaded[i] = false
                soundIds[i] = soundPool.load(context, sounds[i], 1)
            }
        }

    }


    open fun playOnce(toneId: Int) {
        if (LBSLinkApp.canPlaySounds && !sounds.isNullOrEmpty()) {
            val index = sounds.indexOf(toneId)
            if (index > -1) {
                if (soundsLoaded[index] == true && soundIds[index] != null)
                    soundPool.play(soundIds[index]!!, volume, volume, 1, 0, 1f)
            }
        }
    }

    open fun playLoop(toneId: Int) {
        if (LBSLinkApp.canPlaySounds && !sounds.isNullOrEmpty()) {
            val index = sounds.indexOf(toneId)
            if (index > -1) {
                if (soundsLoaded[index] == true && soundIds[index] != null)
                    soundPool.play(soundIds[index]!!, volume, volume, 1, -1, 1f)
            }
        }
    }

}