/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners

import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieListener
import it.keybiz.lbsapp.corporate.base.LBSLinkApp

abstract class LottieCompositioListener: LottieListener<LottieComposition> {

    override fun onResult(result: LottieComposition?) {
        LBSLinkApp.siriComposition = result
    }
}