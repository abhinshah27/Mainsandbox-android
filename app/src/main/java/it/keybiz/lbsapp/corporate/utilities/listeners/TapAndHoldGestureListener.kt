/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners

import android.os.Handler
import android.view.MotionEvent
import android.view.View
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils

/**
 * Former TapAndHoldGestureListener.java (@author mabldrighi on 11/17/2017).
 * @author mabldrighi on 11/13/2018.
 */
class TapAndHoldGestureListener(private val listener: OnHoldListener,
                                private val permissionListener: OnPermissionsNeeded? = null):
        View.OnTouchListener {

    companion object {

        val LOG_TAG = TapAndHoldGestureListener::class.qualifiedName

        private const val LIMIT_FOR_HOLD: Long = 750
    }

    private var isDown = false
    private var hasPermissions = false

    private var isHolding = false
    private var downX = 0f
    private var holdPointerId: Int? = null

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val action = motionEvent.action

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downX = motionEvent.rawX

                isDown = true
                view.isSelected = true

                hasPermissions = permissionListener?.handlePermissionsNeeded(Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO) ?: true

                if (hasPermissions) {
                    Handler().postDelayed({
                        LogUtils.d(LOG_TAG, "CHECKING Hold")

                        holdPointerId = motionEvent.getPointerId(0)

                        if (isDown) {
                            isHolding = true
                            listener.onHoldActivated()

                            LogUtils.d(LOG_TAG, "Hold ACTIVATED")
                        }
                    }, LIMIT_FOR_HOLD)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                isDown = false
                if (isHolding && hasPermissions) {
                    isHolding = false
                    listener.onHoldReleased()
                    view.isSelected = false

                    LogUtils.d(LOG_TAG, "Hold RELEASED")
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                LogUtils.d(LOG_TAG, "DOWN position X: $downX, with pointerID: $holdPointerId \n CURRENT position X: ${motionEvent.rawX}, with pointerID: ${motionEvent.getPointerId(0)}")

                val movingLeft = motionEvent.rawX < downX
                val samePointer = holdPointerId == motionEvent.getPointerId(0)

                if (isHolding && samePointer) {
                    listener.onSlideEvent(motionEvent, movingLeft)
                    downX = motionEvent.rawX
                }
                return true
            }
        }

        return false
    }

    interface OnHoldListener {
        fun onHoldActivated()
        fun onHoldReleased()
        fun onSlideEvent(motionEvent: MotionEvent, movingLeft: Boolean)
    }
}
