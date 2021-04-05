/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners

import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils

/**
 * @author mbaldrighi on 11/3/2017.
 */
abstract class LoadMoreScrollListener @JvmOverloads constructor(
        private val type: Type = Type.GENERAL,
        private val orientation: Orientation = Orientation.VERTICAL
) : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {

    companion object {
        val LOG_TAG = LoadMoreScrollListener::class.qualifiedName
    }

    enum class Type {
        MAIN_FEED, GENERAL, CHAT
    }

    enum class Orientation {
        VERTICAL, HORIZONTAL
    }


    var canFetch: Boolean?

    /**
     * The total number of items in the dataset after the last load
     */
    private var mPreviousTotal = 0

    /**
     * True if we are still waiting for the last set of data to load.
     */
    private var mLoading = true


    init {
        canFetch = if (type == Type.MAIN_FEED || type == Type.CHAT) true else null
    }


    override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val correctDelta = if (orientation == Orientation.HORIZONTAL) dx else dy
        val visibleItemCount = recyclerView.childCount
        val totalItemCount = recyclerView.layoutManager!!.itemCount
        val visibleItemForCalc = (recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager).findFirstVisibleItemPosition()

        val scrollCondition = if (type == Type.CHAT) correctDelta < 0 else correctDelta > 0

        val visibleThreshold = 5
        val loadCondition =
                if (type == Type.CHAT) visibleItemForCalc <= visibleThreshold
                else totalItemCount - visibleItemCount <= visibleItemForCalc + visibleThreshold


        if (scrollCondition) {

            if (type == Type.CHAT && totalItemCount < mPreviousTotal) {
                this.mPreviousTotal = totalItemCount
                if (totalItemCount == 0) {
                    mLoading = true
                }
            }

            val canProceed =
                    if (canFetch != null)
                        totalItemCount > mPreviousTotal
                    else
                        (totalItemCount % Constants.PAGINATION_AMOUNT == 0 && totalItemCount > mPreviousTotal)
            if (mLoading && canProceed) {
                LogUtils.d(LOG_TAG, "Loading More\twith elements: $totalItemCount and $mPreviousTotal")
                mLoading = false
                mPreviousTotal = totalItemCount
            }
        }
        val canFetch = if (canFetch != null) canFetch else totalItemCount % Constants.PAGINATION_AMOUNT == 0
        if (!mLoading && canFetch!! && loadCondition) {
            // End has been reached

            LogUtils.d(LOG_TAG, "Loading More\twith elements: $totalItemCount and dy: $correctDelta and canFetch = ${this.canFetch}")

            mLoading = true
            onLoadMore()
        }
    }

    abstract fun onLoadMore()

}
