/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.helpers

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.hbb20.CountryCodePicker
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils

/**
 * @author mbaldrighi on 9/20/2018.
 */

class PhoneNumberHelper(val type: UsageType) {

    companion object {
        val LOG_TAG = PhoneNumberHelper::class.qualifiedName
    }

    enum class UsageType {
        INTEREST_CLAIM, SETTINGS_ACCOUNT
    }

    private var ccp: CountryCodePicker? = null
    private var phoneNumberET: EditText? = null
    private var mPrefix: String? = null

    fun configurePicker(view: View?) {
        if (view != null) {
            ccp = view.findViewById(R.id.ccp)

            val lang = CountryCodePicker.Language.ENGLISH
            ccp?.changeDefaultLanguage(lang)

            // overrides library settings
            val img = ccp?.findViewById<ImageView>(R.id.imageView_arrow)
            img?.visibility = View.GONE

            val ccpTextView = ccp?.findViewById<TextView>(R.id.textView_selectedCountry)
            ccpTextView?.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            ccpTextView?.textSize = 16f
            Utils.applyFontToTextView(ccpTextView, R.string.fontRegular)

            val newImg = ImageView(view.context)
            val lp = RelativeLayout.LayoutParams(
                    Utils.dpToPx(16f, view.context.resources), Utils.dpToPx(16f, view.context.resources)
            )
            lp.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            newImg.layoutParams = lp
            newImg.setImageResource(R.drawable.ic_signup_down_indicator)
            ccp?.addView(newImg)

            ccp?.setOnCountryChangeListener {
                LogUtils.d(LOG_TAG, ccp?.selectedCountryCodeWithPlus + "(" + ccp?.selectedCountryName + ")")
                mPrefix = ccp?.selectedCountryCodeWithPlus
                ccpTextView?.text = mPrefix
                Utils.applyFontToTextView(ccpTextView, R.string.fontRegular)
            }
            ccp?.setAutoDetectedCountry(true)

            phoneNumberET = view.findViewById(R.id.phone_number)
            phoneNumberET?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    if (s.isNotEmpty() && Utils.isStringValid(mPrefix))
                        phoneNumberET?.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_tick_green_small, 0)
                    else
                        phoneNumberET?.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                }
            })

            mPrefix = ccp?.selectedCountryCodeWithPlus
        }
    }

    fun getCompleteNumber(): String? {
        val mNumber = phoneNumberET?.text.toString()
        var numComplete: String? = ""
        if (Utils.isStringValid(mNumber)) run {
            numComplete = (mPrefix + mNumber).replace("\\s".toRegex(), "").trim { it <= ' ' }
        }

        return numComplete
    }



}