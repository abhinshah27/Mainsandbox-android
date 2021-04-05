/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.helpers

import android.os.Handler
import android.os.HandlerThread
import it.keybiz.lbsapp.corporate.utilities.Utils

/**
 * @author mbaldrighi on 12/3/2018.
 */
open class ComputeAndPopulateHandlerThread(name: String,
                                           private val callbackListener: Handler.Callback
): HandlerThread(name) {

    var handler: Handler? = null

    /**
     * Place where the [Handler] for the class is instantiated.
     */
    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = if (Utils.hasPie())
            Handler.createAsync(looper, callbackListener)
        else
            Handler(looper, callbackListener)

        // put here the custom code

    }

    fun exitOps() {
        handler?.sendEmptyMessage(0)
        quitSafely()
    }

}