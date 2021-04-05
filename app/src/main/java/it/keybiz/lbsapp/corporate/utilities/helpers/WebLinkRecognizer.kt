/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.helpers

import android.content.Context
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import io.realm.Realm
import it.keybiz.lbsapp.corporate.base.BaseHelper
import it.keybiz.lbsapp.corporate.connection.HLServerCalls
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListenerWithIdOperation
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver
import it.keybiz.lbsapp.corporate.models.HLWebLink
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import org.json.JSONArray
import org.json.JSONException
import java.util.concurrent.ConcurrentHashMap

class WebLinkRecognizer private constructor(context: Context): BaseHelper(context), OnServerMessageReceivedListenerWithIdOperation, OnMissingConnectionListener {

    companion object {
        private val LOG_TAG = WebLinkRecognizer::class.qualifiedName

        private const val WEB_LINK_REGEX = "(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\." +
                "\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\." +
                "\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]" +
                "\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]" +
                "\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\." +
                "(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}" +
                "-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?"

        private const val WEB_LINK_REGEX_2 = "^((ftp|http|https):\\/\\/)?(www.)?(?!.*(ftp|http|https|www.))[a-zA-Z0-9_-]+(\\.[a-zA-Z]+)+((\\/)[\\w#]+)*(\\/\\w+\\?[a-zA-Z0-9_]+=\\w+(&[a-zA-Z0-9_]+=\\w+)*)?$"

        private var instance: WebLinkRecognizer? = null

        @JvmStatic fun getInstance(context: Context): WebLinkRecognizer {
            if (instance == null)
                instance = WebLinkRecognizer(context)
            return instance!!
        }

        fun extractAppendedText(link: String?, messageText: String?): String {
            return if (!link.isNullOrBlank() && !messageText.isNullOrBlank()) {
                messageText.replaceFirst(Regex(link), "").trim()
            } else ""
        }
    }

    private val serverReceiver by lazy { ServerMessageReceiver().apply { this.setListener(this@WebLinkRecognizer) } }

    /**
     * Map persisting messages and link, to be queried after server response
     */
    private val linksMap = ConcurrentHashMap<String, String>()


    fun recognize(text: String?, messageID: String?, listener: LinkRecognitionListener? = null) {
        val mHandlerThread = HandlerThread("webLinkRecognition")
        mHandlerThread.start()
        Handler(mHandlerThread.looper).post {

            if (!text.isNullOrBlank()) {
//                val matcher = Utils.getMatcherOfMatching(text.toLowerCase(), WEB_LINK_REGEX_2)
//
//                if (matcher != null) {
                    val found = isURLValid(text)
                    if (found) {
//                        val group = matcher.group()
                        if (listener != null) {
                            listener.onLinkRecognized(text)
                            serverReceiver.setListener(null)
                            unregisterReceiver(serverReceiver)
                        }
                        else
                            callForLinkParsing(text, messageID)
                    }
//                }
            }

            mHandlerThread.quitSafely()
        }
    }

    private fun callForLinkParsing(link: String?, messageID: String?) {

        if (!link.isNullOrBlank() && !messageID.isNullOrBlank() && !linksMap.contains(messageID)) {

            registerReceiver(serverReceiver, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))


            var result: Array<Any>? = null

            try {
                result = HLServerCalls.getParsedWebLink(link, messageID)
                linksMap[result[2] as String] = messageID
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            // not handling "result", because we don't want messages
//            if (Utils.isContextValid(contextRef.get()) && contextRef.get() is HLActivity) {
//                HLRequestTracker.getInstance((contextRef.get() as HLActivity).application as LBSLinkApp)
//                        .handleCallResult(this, (contextRef.get() as HLActivity), result)
//            }
        }
    }


    override fun onResume() {
        super.onResume()

        registerReceiver(serverReceiver, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))
    }

    override fun onStop() {
        unregisterReceiver(serverReceiver)

        super.onStop()
    }

    override fun handleSuccessResponse(operationUUID: String?, operationId: Int, responseObject: JSONArray?) {
        if (operationId == Constants.SERVER_OP_GET_PARSED_WEB_LINK) {
            if (responseObject?.length() != 1 || responseObject.optJSONObject(0).length() == 0) {
                handleErrorResponse(operationId, 0)
                return
            }

            val wl = HLWebLink().deserializeToClass(responseObject.optJSONObject(0))
            if (wl == null) {
                handleErrorResponse(operationId, 0)
                return
            }

            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()
                realm.executeTransactionAsync {

                    val messageID = if (!operationUUID.isNullOrBlank()) linksMap[operationUUID] else null
                    if (!messageID.isNullOrBlank()) {
                        val message = RealmUtils.readFirstFromRealmWithId(it, ChatMessage::class.java, "messageID", messageID) as ChatMessage
                        message.webLink = it.copyToRealm(wl)
                    }

                    linksMap.remove(operationUUID)
                }
            } catch (e: Exception) {
                handleErrorResponse(operationId, 0)
            } finally {
                RealmUtils.closeRealm(realm)
            }
        }
    }

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {}

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        if (operationId == Constants.SERVER_OP_GET_PARSED_WEB_LINK)
            LogUtils.e(LOG_TAG, "Error recognizing Web Link with error: $errorCode")
    }

    override fun onMissingConnection(operationId: Int) {
        if (operationId == Constants.SERVER_OP_GET_PARSED_WEB_LINK)
            LogUtils.e(LOG_TAG, "Error recognizing Web Link with error: NO CONNECTION")
    }


//    private fun getMessageIdFromLink(idOp: String?): String? {
//        return if (!idOp.isNullOrBlank()) {
//            var result: String? = null
//            for (it in linksMap.entries) {
//                result = it.key; break }
//                else continue
//            }
//            result
//        } else null
//    }


    private fun isURLValid(url: String): Boolean {
        return url.startsWith("http://", true) || url.startsWith("https://", true) ||
                url.startsWith("rtsp://", true) || url.startsWith("www.", true) ||
                url.startsWith("ftp://", true)
    }


    interface LinkRecognitionListener {
        fun onLinkRecognized(group: String)
    }


}