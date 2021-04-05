/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewPropertyAnimator
import java.util.*


// ========================================================== //
// Class containing all the utility methods needed in Kotlin. //
// ========================================================== //


object KoltinUtils {

    // TODO: 11/13/2018    PUT HERE ALL STATIC METHODS

}



/**
 * Class containing all the utility methods needed in Kotlin.
 * @author mbaldrighi on 11/13/2018.
 */
fun View.baseAnimateAlpha(duration: Long, show: Boolean, repeat: Boolean = false, delay: Long = 0):
        ViewPropertyAnimator {

    var localShow = show

    val animator = animate()
            .alpha(if (show) 1f else 0f)
            .setDuration(duration)
            .setListener(object: Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    if (show)
                        this@baseAnimateAlpha.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator?) {

                    if (!repeat && !show)
                        this@baseAnimateAlpha.visibility = View.GONE
                    else if (repeat) {
                        localShow = !localShow
                        this@baseAnimateAlpha.baseAnimateAlpha(duration, localShow, repeat, delay)
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                    // reset initial value deducted from the initial value of show argument
                    alpha = if (show) 0f else 1f
                }

                override fun onAnimationRepeat(animation: Animator?) {}
            })
            .setStartDelay(delay)

            animator?.start()

    return animator
}

fun View.baseAnimateHeight(duration: Long, expand: Boolean, fullHeight: Int, delay: Long = 0,
                           automaticCollapse: Long = 0, customListener: Animator.AnimatorListener? = null,
                           start: Boolean = true):
        ValueAnimator? {

    if (fullHeight > 0) {
        val animator = if (expand) ValueAnimator.ofInt(0, fullHeight) else ValueAnimator.ofInt(fullHeight, 0)
        animator.duration = duration
        animator.startDelay = delay
        animator.addUpdateListener {
            val lp = this.layoutParams
            lp.height = it.animatedValue as Int
            this.layoutParams = lp
        }
        if (customListener != null) animator.addListener(customListener)
        else {
            animator.addListener(object: Animator. AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    if (automaticCollapse > 0 && expand) {
                        this@baseAnimateHeight.postDelayed(
                                { baseAnimateHeight(duration, false, fullHeight, delay, 0) },
                                automaticCollapse
                        )
                    }
                }

                override fun onAnimationStart(animation: Animator?) {}
            })
        }
        if (start) animator.start()
        return animator
    }

    return null

}

/**
 * NEVER TESTED and NEVER USED
 */
fun View.baseAnimateY(duration: Long, show: Boolean, hide: Boolean = false, repeat: Boolean = false, delay: Long = 0):
        ViewPropertyAnimator {

    var localShow = show

    val animator = animate()
            .translationY(if (show) 1f else 0f)
            .setDuration(duration)
            .setListener(object: Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    if (hide)
                        this@baseAnimateY.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator?) {

                    if (!repeat && !hide)
                        this@baseAnimateY.visibility = View.GONE
                    else if (repeat) {
                        localShow = !localShow
                        this@baseAnimateY.baseAnimateY(duration, localShow, hide, repeat, delay)
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                    // reset initial value deducted from the initial value of show argument
                    alpha = if (show) 0f else 1f
                }

                override fun onAnimationRepeat(animation: Animator?) {}
            })
            .setStartDelay(delay)

    animator?.start()

    return animator
}


fun Date.isSameDateAsNow(): Boolean {

    val cal = Calendar.getInstance().apply { this.time = this@isSameDateAsNow }
    val calNow = Calendar.getInstance()

    return cal[Calendar.YEAR] == calNow[Calendar.YEAR] &&
            cal[Calendar.MONTH] == calNow[Calendar.MONTH] &&
            cal[Calendar.DAY_OF_MONTH] == calNow[Calendar.DAY_OF_MONTH]

}


fun vibrateForChat(context: Context?) {
    if (Utils.isContextValid(context)) {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        if (vibrator?.hasVibrator() == true) {
            if (Utils.hasOreo())
                vibrator.vibrate(VibrationEffect.createOneShot(25, 15))
            else
                vibrator.vibrate(25)
        }
    }
}

fun vibrateForCalls(context: Context?, cancel: Boolean = false) {
    if (Utils.isContextValid(context)) {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        if (vibrator?.hasVibrator() == true) {
            if (cancel) vibrator.cancel()
            else {
                val pattern = longArrayOf(0, 1000, 500, 1000)
                if (Utils.hasOreo())
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, intArrayOf(0, 200, 0, 100), 0))
                else
                    vibrator.vibrate(pattern, 0)
            }
        }
    }
}
