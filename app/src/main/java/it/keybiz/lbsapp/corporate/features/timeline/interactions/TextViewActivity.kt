/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.Utils
import kotlinx.android.synthetic.main.activity_text_view.*
import kotlinx.android.synthetic.main.activity_text_view_2.view.*
import kotlinx.android.synthetic.main.luiss_toolbar_transp_text_viewer.*
import java.lang.reflect.Method

/**
 * @author mbaldrighi on 1/22/2018.
 */
class TextViewActivity : HLActivity() {

    companion object {
        val LOG_TAG = TextViewActivity::class.qualifiedName
    }

    private val textSteps = arrayOf(14f, 16f, 18f, 20f, 22f, 24f)

    private var caption: String? = null
    private var userName: String? = null
    private var textSizeInArray = 2

    private var scrollbarMethod: Method? = null
    private var scrollbarObject: Any? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_view_2)
        setRootContent(R.id.root_content)
        setProgressIndicator(R.id.progress)

        manageIntent()

        actionsBtns.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                actionsBtns.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val lp = back_arrow.layoutParams
                lp.width = actionsBtns.width
                back_arrow.layoutParams = lp
            }
        })
        back_arrow.setOnClickListener { finish(); overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down) }
        textSmaller.setOnClickListener {
            textBigger.isEnabled = true

            val int = textSizeInArray - 1
            if (int >= 0) {
                text.textSize = textSteps[int]
                textSizeInArray = int

                if (int == 0) textSmaller.isEnabled = false
            }
        }
        textBigger.setOnClickListener {
            textSmaller.isEnabled = true

            val int = textSizeInArray + 1
            if (int <= textSteps.size - 1) {
                text.textSize = textSteps[int]
                textSizeInArray = int

                if (int == textSteps.size - 1) textBigger.isEnabled = false
            }
        }
        nightMode.setOnClickListener {
            switchMode()
        }
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(this, AnalyticsUtils.FEED_TEXT_VIEWER)

        toolbar_title.text = userName
        text.text = caption
        text.textSize = textSteps[textSizeInArray]
    }

    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        val intent = intent
        if (intent != null) {
            if (intent.hasExtra(Constants.EXTRA_PARAM_1))
                caption = intent.getStringExtra(Constants.EXTRA_PARAM_1)
            if (intent.hasExtra(Constants.EXTRA_PARAM_2))
                userName = intent.getStringExtra(Constants.EXTRA_PARAM_2)
        }
    }


    private fun switchMode() {
        rootContent.run {
            isSelected = !isSelected
            textSmaller.isSelected = isSelected
            textBigger.isSelected = isSelected

            when {
                Utils.hasLollipop() -> nightMode.imageTintList = ContextCompat.getColorStateList(this@TextViewActivity, R.color.state_list_text_viewer_icons)
                isSelected -> nightMode.setColorFilter(Utils.getColor(this@TextViewActivity, R.color.white), PorterDuff.Mode.SRC_ATOP)
                else -> nightMode.setColorFilter(Utils.getColor(this@TextViewActivity, R.color.black_87), PorterDuff.Mode.SRC_ATOP)
            }

            // uses reflection to change scrollbar thumb
            try {
                if (scrollbarMethod == null && scrollbarObject == null) {
                    val mScrollCacheField = View::class.java.getDeclaredField("mScrollCache")
                    mScrollCacheField.isAccessible = true
                    val mScrollCache = mScrollCacheField.get(scrollView)

                    val scrollBarField = mScrollCache::class.java.getDeclaredField("scrollBar")
                    scrollBarField.isAccessible = true
                    scrollbarObject = scrollBarField.get(mScrollCache)

                    if (scrollbarObject != null) {
                        scrollbarMethod = scrollbarObject!!::class.java.getDeclaredMethod("setVerticalThumbDrawable", Drawable::class.java)
                        scrollbarMethod?.isAccessible = true
                    }
                }

                if (scrollbarMethod != null && scrollbarObject != null) {
                    scrollbarMethod!!.invoke(
                            scrollbarObject,
                            ResourcesCompat.getDrawable(
                                    resources,
                                    if (isSelected) R.drawable.custom_scrollbar_thumb_night
                                    else R.drawable.custom_scrollbar_thumb,
                                    null
                            )
                    )
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
