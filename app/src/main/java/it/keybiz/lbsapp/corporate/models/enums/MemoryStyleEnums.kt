/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums

import android.content.res.Resources
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import it.keybiz.lbsapp.corporate.R


/**
 * Enum class representing how the media must be shown: fitting or filling the available screen.
 */
enum class FitFillTypeEnum(val value: Int) {
    FIT(1), FILL(2);

    companion object {

        @JvmStatic
        fun toEnum(value: Int): FitFillTypeEnum {
            return when (value) {
                FIT.value -> FIT
                FILL.value -> FILL
                else -> FILL
            }
        }
    }

}


/**
 * Enum class referring to the available colors to style a [it.keybiz.lbsapp.models.Post]
 * for both background and text color.
 */
enum class MemoryColorEnum(val value: String) {

    RED("b1"),
    ORANGE("b2"),
    YELLOW("b3"),
    BROWN("b4"),
    GREEN_L("b5"),
    GREEN_D("b6"),
    PURPLE_L("b7"),
    PURPLE_D("b8"),
    BLUE_D("b9"),
    BLUE_L("b10"),
    BLACK("b11"),
    WHITE("b12");

    companion object {

        @JvmStatic
        @ColorRes fun getColor(value: String?): Int {
            return when (value) {
                RED.value -> R.color.hl_cpost_bckgr_red
                ORANGE.value -> R.color.hl_cpost_bckgr_orange
                YELLOW.value -> R.color.hl_cpost_bckgr_yellow
                BROWN.value -> R.color.hl_cpost_bckgr_brown
                GREEN_L.value -> R.color.hl_cpost_bckgr_green_l
                GREEN_D.value -> R.color.hl_cpost_bckgr_green_d
                PURPLE_L.value -> R.color.hl_cpost_bckgr_purple_l
                PURPLE_D.value -> R.color.hl_cpost_bckgr_purple_d
                BLUE_D.value -> R.color.hl_cpost_bckgr_blue_d
                BLUE_L.value -> R.color.hl_cpost_bckgr_blue_l
                BLACK.value -> R.color.hl_cpost_bckgr_black
                WHITE.value -> R.color.hl_cpost_bckgr_white
                else -> R.color.hl_cpost_bckgr_black
            }
        }

        @JvmStatic
        @ColorInt fun getColor(resources: Resources, value: String?): Int {
            return ResourcesCompat.getColor(resources, getColor(value), null)
        }

        @JvmStatic
        @ColorInt fun getColor(resources: Resources, value: MemoryColorEnum): Int {
            return ResourcesCompat.getColor(resources, getColor(value.value), null)
        }
    }

    override fun toString(): String {
        return value.toLowerCase()
    }

}


const val MEMORY_TEXT_SIZE_SMALL = 18f
const val MEMORY_TEXT_SIZE_MEDIUM = 25f
const val MEMORY_TEXT_SIZE_LARGE = 40f

/**
 * Enum class referring to the available text sizes for a [it.keybiz.lbsapp.models.Post]
 * message.
 */
enum class MemoryTextSizeEnum(val value: String) {

    TEXT_SIZE_MEDIUM("medium"),
    TEXT_SIZE_SMALL("small"),
    TEXT_SIZE_LARGE("large");

    companion object {

        @JvmStatic
        fun getSize(value: String?): Float {
            return when (value) {
                TEXT_SIZE_MEDIUM.value -> MEMORY_TEXT_SIZE_MEDIUM
                TEXT_SIZE_SMALL.value -> MEMORY_TEXT_SIZE_SMALL
                TEXT_SIZE_LARGE.value -> MEMORY_TEXT_SIZE_LARGE
                else -> MEMORY_TEXT_SIZE_MEDIUM
            }
        }

        @JvmStatic
        fun getSize(value: MemoryTextSizeEnum): Float {
            return when (value) {
                TEXT_SIZE_MEDIUM -> MEMORY_TEXT_SIZE_MEDIUM
                TEXT_SIZE_SMALL -> MEMORY_TEXT_SIZE_SMALL
                TEXT_SIZE_LARGE -> MEMORY_TEXT_SIZE_LARGE
            }
        }

        @JvmStatic
        fun toEnum(value: String?): MemoryTextSizeEnum {
            return when (value) {
                TEXT_SIZE_MEDIUM.value -> TEXT_SIZE_MEDIUM
                TEXT_SIZE_SMALL.value -> TEXT_SIZE_SMALL
                TEXT_SIZE_LARGE.value -> TEXT_SIZE_LARGE
                else -> TEXT_SIZE_MEDIUM
            }
        }
    }

    override fun toString(): String {
        return value.toLowerCase()
    }
}


/**
 * Enum class referring to the available positions for a [it.keybiz.lbsapp.models.Post]
 * message.
 */
enum class MemoryTextPositionEnum(val value: String) {

    TOP_LEFT("p1"),
    TOP_CENTER("p2"),
    TOP_RIGHT("p3"),
    CENTER_LEFT("p4"),
    CENTER("p5"),
    CENTER_RIGHT("p6"),
    BOTTOM_LEFT("p7"),
    BOTTOM_CENTER("p8"),
    BOTTOM_RIGHT("p9");

    companion object {

        @JvmStatic
        fun getGravityPosition(value: String?): Int {
            return getGravityPosition(toEnum(value))
        }

        @JvmStatic
        fun getGravityPosition(value: MemoryTextPositionEnum): Int {
            return when (value) {
                TOP_LEFT -> Gravity.TOP or Gravity.START
                TOP_CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                TOP_RIGHT ->  Gravity.TOP or Gravity.END
                CENTER_LEFT -> Gravity.CENTER_VERTICAL or Gravity.START
                CENTER -> Gravity.CENTER
                CENTER_RIGHT -> Gravity.CENTER_VERTICAL or Gravity.END
                BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
                BOTTOM_CENTER -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
            }
        }

        @JvmStatic
        fun toEnum(value: String?): MemoryTextPositionEnum {
            return when (value) {
                TOP_LEFT.value -> TOP_LEFT
                TOP_CENTER.value -> TOP_CENTER
                TOP_RIGHT.value -> TOP_RIGHT
                CENTER_LEFT.value -> CENTER_LEFT
                CENTER.value -> CENTER
                CENTER_RIGHT.value -> CENTER_RIGHT
                BOTTOM_LEFT.value -> BOTTOM_LEFT
                BOTTOM_CENTER.value -> BOTTOM_CENTER
                BOTTOM_RIGHT.value -> BOTTOM_RIGHT
                else -> CENTER_LEFT
            }
        }
    }

    override fun toString(): String {
        return value.toLowerCase()
    }

}