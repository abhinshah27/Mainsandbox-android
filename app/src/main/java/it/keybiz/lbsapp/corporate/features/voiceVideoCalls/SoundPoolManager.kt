/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.voiceVideoCalls

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.*
import android.os.Build
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.tmp.VoiceVideoUsageType
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.media.BaseSoundPoolManager
import it.keybiz.lbsapp.corporate.utilities.vibrateForCalls

class SoundPoolManager private constructor(context: Context,
                                           private val usageType: VoiceVideoUsageType = VoiceVideoUsageType.OUTGOING):
        BaseSoundPoolManager(context, null, null), SoundPool.OnLoadCompleteListener {

    companion object {

        private var instance: SoundPoolManager? = null

        fun getInstance(context: Context, usageType: VoiceVideoUsageType): SoundPoolManager {
            if (instance == null) {
                instance = SoundPoolManager(context, usageType)
            }
            return instance as SoundPoolManager
        }
    }

    private var playingRingtone = false
    private var playingWaiting = false
    private var loadedWaiting = false
    private var loadedError = false
    private var playingCalled = false
    private var actualVolume: Float = 0.toFloat()
    private var maxVolume: Float = 0.toFloat()
    private var volume: Float = 0.toFloat()

    // using MediaPlayer instead of SoundPool until custom file is provided
    private var mediaPlayer: MediaPlayer? = null
    private val soundPool: SoundPool by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            SoundPool.Builder().setMaxStreams(1).build()
        else SoundPool(1, AudioManager.STREAM_MUSIC, 0)
    }
    private var waitingToneId: Int = 0
    private var waitingToneStream: Int = 0
    private var errorSoundId: Int = 0

    init {
        // AudioManager audio settings for adjusting the volume
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        actualVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        if (maxVolume != 0f)
            volume = actualVolume / maxVolume

        if (usageType == VoiceVideoUsageType.INCOMING) {
            mediaPlayer =
                    if (Utils.hasLollipop())
                        MediaPlayer.create(
                                context,
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                                null,
                                AudioAttributes.Builder().setUsage(
                                        AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .setLegacyStreamType(AudioManager.STREAM_RING)
                                        .build(),
                                audioManager.generateAudioSessionId())
                    else MediaPlayer.create(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            mediaPlayer!!.isLooping = true
        }

        soundPool.setOnLoadCompleteListener(this)
        waitingToneId = soundPool.load(context, R.raw.ring_tone, 1)
        errorSoundId = soundPool.load(context, R.raw.call_error, 1)
    }

    override fun onLoadComplete(soundPool: SoundPool, sampleId: Int, status: Int) {
        if (status == 0) {
            loadedWaiting = true

            if (sampleId == waitingToneId && usageType == VoiceVideoUsageType.OUTGOING && playingCalled) {
                playRinging(context)
                playingCalled = false
            }
        }
    }

    fun playRinging(context: Context) {
        if (usageType == VoiceVideoUsageType.OUTGOING) {
            if (loadedWaiting && !playingWaiting) {
                waitingToneStream = soundPool.play(waitingToneId, volume, volume, 1, -1, 1f)
                playingWaiting = true
            } else playingCalled = true
        }
        else {
            if (LBSLinkApp.canPlaySounds && mediaPlayer != null) {
                mediaPlayer!!.start()
                playingRingtone = true
            }

            if (LBSLinkApp.canVibrate) vibrateForCalls(context)
        }
    }

    fun stopRinging() {
        if (playingWaiting && usageType == VoiceVideoUsageType.OUTGOING) {
            soundPool.stop(waitingToneStream)
            playingWaiting = false
        }
        else {
            if (playingRingtone && mediaPlayer != null) {
                mediaPlayer!!.stop()
                playingRingtone = false
            }

            vibrateForCalls(context, true)
        }
    }

    fun playError() {
        if (loadedError) {
            stopRinging()
            soundPool.play(errorSoundId, volume, volume, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.unload(waitingToneId)
        soundPool.unload(errorSoundId)
        soundPool.release()

        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying)
                mediaPlayer!!.pause()

            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }

        instance = null
    }

}
