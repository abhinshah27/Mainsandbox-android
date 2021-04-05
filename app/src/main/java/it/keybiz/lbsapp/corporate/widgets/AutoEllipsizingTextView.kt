/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.emoji.widget.EmojiAppCompatTextView

class AutoEllipsizingTextView constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0):
        EmojiAppCompatTextView(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0) {
        AutoEllipsizingTextView(context, attrs, 0)
    }

    constructor(context: Context) : this(context, null, 0) {
        AutoEllipsizingTextView(context, null, 0)
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // calculate the max lines we can show
        val maxLines = h / this.lineHeight
        if (maxLines > 0) {
            setMaxLines(maxLines)
            ellipsize = TextUtils.TruncateAt.END
        }
    }



    fun getEllipsisCount(): Int {
        return if (lineCount > 0)
            (layout?.getEllipsisCount(lineCount - 1) ?: 0)
        else 0
    }

}