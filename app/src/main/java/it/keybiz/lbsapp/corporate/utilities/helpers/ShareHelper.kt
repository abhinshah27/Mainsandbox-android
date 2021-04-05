/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.helpers

import android.content.Context
import android.content.IntentFilter
import android.os.Handler
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.BaseHelper
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.connection.*
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.Utils
import org.json.JSONArray
import org.json.JSONException

class ShareHelper(context: Context, private val provider: ShareableProvider?): BaseHelper(context),
        OnServerMessageReceivedListener, OnMissingConnectionListener {

    private val mReceiver by lazy {
        ServerMessageReceiver().apply { setListener(this@ShareHelper) }
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(contextRef.get()!!).registerReceiver(mReceiver, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))
    }

    override fun onStop() {
        super.onStop()

        handleProgress(false)

        try {
            LocalBroadcastManager.getInstance(contextRef.get()!!).unregisterReceiver(mReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        if (operationId == Constants.SERVER_OP_GET_SHAREABLE_LINK) {

            handleProgress(false)

            val link = responseObject?.getJSONObject(0)?.optString("uri")

            Utils.fireShareIntent(contextRef.get(), link)

            provider?.afterOps()
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        handleProgress(false)

        if (operationId == Constants.SERVER_OP_GET_SHAREABLE_LINK)
            (contextRef.get() as? HLActivity)?.showAlert(R.string.error_shareable_creation)
        else
            (contextRef.get() as? HLActivity)?.showGenericError()
    }

    override fun onMissingConnection(operationId: Int) {
        handleProgress(false)
    }

    fun initOps(isChat: Boolean) {
        var result: Array<Any?>? = null

        try {
            Handler().postDelayed({
                result = HLServerCalls.performShareCreation(provider?.getUserID(), provider?.getPostOrMessageID(), isChat)
            }, 500)

        } catch (e: JSONException) {
            e.printStackTrace()
        }

        handleProgress(true)

        if (contextRef.get() is HLActivity)
           HLRequestTracker.getInstance((contextRef.get() as HLActivity).application as? LBSLinkApp)
                   .handleCallResult(this, contextRef.get() as HLActivity, result)
    }


    private fun handleProgress(show: Boolean) {
        if (provider?.getProgressView() != null) {
            if (show) provider.getProgressView()!!.visibility = View.VISIBLE
            else provider.getProgressView()!!.visibility = View.GONE
        }
        else {
            (contextRef.get() as? HLActivity)?.setProgressMessage(R.string.creating_shareable_link)
            (contextRef.get() as? HLActivity)?.handleProgress(show)
        }
    }



    interface ShareableProvider {
        fun getProgressView(): View? { return null }
        fun afterOps() {
            // do nothing by default
        }

        fun getUserID(): String
        fun getPostOrMessageID(): String
    }

}