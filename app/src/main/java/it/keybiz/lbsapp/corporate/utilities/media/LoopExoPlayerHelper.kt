/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.media

import android.net.Uri
import im.ene.toro.ToroPlayer
import im.ene.toro.exoplayer.Config
import im.ene.toro.exoplayer.ExoPlayerViewHelper
import im.ene.toro.exoplayer.MediaSourceBuilder

/**
 * @author mbaldrighi on 10/24/2018.
 */
class LoopExoPlayerHelper(player: ToroPlayer, mediaUri: Uri, extension: String? = null):
        ExoPlayerViewHelper(player, mediaUri, extension, Config.Builder().setMediaSourceBuilder(MediaSourceBuilder.LOOPING).build())
