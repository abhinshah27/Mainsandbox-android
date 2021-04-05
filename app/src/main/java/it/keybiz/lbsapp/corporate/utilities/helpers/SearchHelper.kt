/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.helpers

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import it.keybiz.lbsapp.corporate.R

import it.keybiz.lbsapp.corporate.features.globalSearch.GlobalSearchFragment
import java.lang.ref.WeakReference

/**
 * @author mbaldrighi on 8/21/2018.
 */

class SearchHelper(val listener: OnQuerySubmitted) {

    // With the new setting #SearchType enum is no longer used.
    enum class SearchType {
        NONE, SINGLE_CHAR, CHARS_3
    }
    private var mSearchType: SearchType = SearchType.NONE

    private var searchBtnReference: WeakReference<View>? = null
    private var clearBtnReference: WeakReference<View>? = null
    private var searchFieldReference: WeakReference<EditText>? = null

    private var mText: Editable? = null


    fun configureViews(searchBox: View, searchField: EditText) {
        // INFO: 3/5/19    every time the method is called the references are updated
        searchBtnReference = WeakReference(searchBox.findViewById(R.id.searchButton))
        clearBtnReference = WeakReference(searchBox.findViewById(R.id.clearButton))
        searchFieldReference = WeakReference(searchField)

        clearBtnReference?.get()?.setOnClickListener {
            searchFieldReference?.get()?.text?.clear()
            clearBtnReference?.get()?.visibility = View.GONE
            searchBtnReference?.get()?.visibility = View.VISIBLE
            listener.onQueryReceived("")
        }

        searchFieldReference?.get()?.addTextChangedListener(object: TextWatcher {

            private var textChanged: Boolean = false

            override fun afterTextChanged(s: Editable?) {
                if (textChanged) {
                    mText = s

                    handleText(mText == null || mText!!.isEmpty())

                    if (mSearchType == SearchType.CHARS_3) {
                        if (mText!!.length >= 3)
                            listener.onQueryReceived(mText!!.toString())
                        else (listener as? GlobalSearchFragment)?.onQueryReceived("")
                    }
                    else if (listener == SearchType.SINGLE_CHAR) listener.onQueryReceived(mText!!.toString())

                    textChanged = false
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textChanged = true            }
        })

        searchFieldReference?.get()?.imeOptions = EditorInfo.IME_ACTION_SEARCH
        searchFieldReference?.get()?.setOnEditorActionListener  { _, _, _ ->
            if (mText != null && mText!!.isNotEmpty()) {
                listener.onQueryReceived(mText.toString())
                true
            } else false
        }

        handleText(mText == null || mText!!.isEmpty())
    }

    fun handleText(textNotValid: Boolean) {
        if (textNotValid) {
            searchBtnReference?.get()?.visibility = View.VISIBLE
            clearBtnReference?.get()?.visibility = View.GONE
        }
        else {
            searchBtnReference?.get()?.visibility = View.GONE
            clearBtnReference?.get()?.visibility = View.VISIBLE
        }
    }

    interface OnQuerySubmitted {
        fun onQueryReceived(query: String)
    }
}

//enum class SearchType {
//    NONE, SINGLE_CHAR, CHARS_3
//}
//
//class SearchTextWatcherKotlin @JvmOverloads constructor (
//        searchBtn: View,
//        clearBtn: View,
//        editField: EditText,
//        private var mListener: OnQuerySubmitted?,
//        private var searchType: SearchType? = SearchType.CHARS_3
//): TextWatcher {
//
//    private val searchBtnReference: WeakReference<View> = WeakReference(searchBtn)
//    private val clearBtnReference: WeakReference<View> = WeakReference(clearBtn)
//
//    private var text: Editable? = null
//
//    init {
//        editField.imeOptions = EditorInfo.IME_ACTION_SEARCH
//        editField.setOnEditorActionListener  { _, _, _ ->
//            if (text != null && text!!.isNotEmpty()) {
//                mListener!!.onQueryReceived(text.toString())
//                true
//            } else false
//        }
//        clearBtnReference.get()?.setOnClickListener {
//            editField.text.clear()
//            clearBtn.visibility = View.GONE
//            searchBtn.visibility = View.VISIBLE
//            mListener!!.onQueryReceived("")
//        }
//    }
//
//    private var textChanged: Boolean = false
//
//    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
//
//    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
//        textChanged = true
//    }
//
//    override fun afterTextChanged(s: Editable) {
//        if (textChanged) {
//            text = s
//
//            if (text!!.isNotEmpty()) {
//                searchBtnReference.get()?.visibility = View.GONE
//                clearBtnReference.get()?.visibility = View.VISIBLE
//            }
//            else {
//                searchBtnReference.get()?.visibility = View.VISIBLE
//                clearBtnReference.get()?.visibility = View.GONE
//            }
//
//            if (searchType == SearchType.CHARS_3) {
//                if (text!!.length >= 3)
//                    mListener!!.onQueryReceived(text!!.toString())
//                else if (mListener is GlobalSearchFragment)
//                    mListener!!.onQueryReceived("")
//            }
//            else if (searchType == SearchType.SINGLE_CHAR) mListener!!.onQueryReceived(text!!.toString())
//
//            textChanged = false
//        }
//    }
//
//
//    fun setListener(listener: OnQuerySubmitted) {
//        this.mListener = listener
//    }
//
//    fun setSearchType(searchType: SearchType) {
//        this.searchType = searchType
//    }
//
//    interface OnQuerySubmitted {
//        fun onQueryReceived(query: String)
//    }
//}