/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.viewers

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.*
import android.webkit.WebView
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.connection.HLServerCallsChat
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.utilities.*
import it.keybiz.lbsapp.corporate.utilities.helpers.ShareHelper
import kotlinx.android.synthetic.main.activity_webview_doc.*
import kotlinx.android.synthetic.main.luiss_toolbar_transp_wv_doc.view.*


/**
 * A screen dedicated to the visualization of web content.
 */
class WebViewActivityDocuments : HLActivity(), ShareHelper.ShareableProvider {

    companion object {
        val LOG_TAG = WebViewActivityDocuments::class.qualifiedName

        private const val TOOLBAR_ANIM_DURATION = 200L
        private var TOOLBAR_EXPANDED_HEIGHT: Int = 0
    }

    private var intentUrl: String? = null
    private var urlToLoad: String? = null
    private var title: String? = null
    private var messageID: String? = null

    private val mShareHelper by lazy { ShareHelper(this@WebViewActivityDocuments, null) }

    private val clickHandler by lazy { WebViewClickHandler(toolbar) }


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_webview_doc)
        setRootContent(R.id.rootContent)
        setProgressIndicator(R.id.genericProgressIndicator)

        manageIntent()

        toolbar.backArrow.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down)
        }

        if (Utils.hasKitKat()) {
            if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                setProgressMessage(R.string.webview_page_loading)
                showProgressIndicator(true)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                webView!!.loadUrl(
                        "javascript:(function() { " +
                                "var element = document.getElementsByClassName('ndfHFb-c4YZDc-Wrql6b')[0];"
                                + "element.parentNode.removeChild(element);" +
                                "})()"
                )

                this@WebViewActivityDocuments.toolbar?.baseAnimateHeight(TOOLBAR_ANIM_DURATION, false, TOOLBAR_EXPANDED_HEIGHT, 2000)
                Handler().postDelayed(
                        { showProgressIndicator(false) },
                        1000
                )

            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                showGenericError()
                LogUtils.e(LOG_TAG, "Error loading url: $urlToLoad with error: ${error.toString()}")
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                showGenericError()
                LogUtils.e(LOG_TAG, "Error loading url: $urlToLoad with error: $description")
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                LogUtils.e(LOG_TAG, "Error loading url: $urlToLoad with error: ${errorResponse.toString()}")
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
                LogUtils.e(LOG_TAG, "Error loading url: $urlToLoad with error: ${error.toString()}")
            }
        }

        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP)
                clickHandler.sendEmptyMessageDelayed(0, 250)
            false
        }

        webView.loadUrl(urlToLoad)

        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                TOOLBAR_EXPANDED_HEIGHT = this@WebViewActivityDocuments.toolbar?.height ?: 0
                this@WebViewActivityDocuments.toolbar?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })

        toolbar.shareBtn.setOnClickListener {
            mShareHelper.initOps(true)
        }
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(this, AnalyticsUtils.CHAT_VIEW_DOCUMENT)

        attemptSendHack()

        mShareHelper.onResume()

        toolbar.toolbarTitle.text = title

        toolbar.shareBtn.visibility = if (!messageID.isNullOrBlank()) View.VISIBLE else View.INVISIBLE
    }

    override fun onStop() {
        mShareHelper.onStop()

        super.onStop()
    }

    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        val intent = intent
        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_1))
            intentUrl = intent.getStringExtra(Constants.EXTRA_PARAM_1)
        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_2))
            title = intent.getStringExtra(Constants.EXTRA_PARAM_2)
        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_3))
            messageID = intent.getStringExtra(Constants.EXTRA_PARAM_3)

        urlToLoad = getEditedUrl(intentUrl)
    }

    override fun onBackPressed() {

        if (webView!!.canGoBack())
            webView!!.goBack()
        else {
            finish()
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down)
        }
    }

    private fun getEditedUrl(current: String?): String {
        return if (!current.isNullOrBlank()) "https://docs.google.com/viewer?url=$current" else ""
    }

    private fun attemptSendHack() {
        if (!messageID.isNullOrBlank()) {
            val message = RealmUtils.readFirstFromRealmWithId(realm, ChatMessage::class.java, "messageID", messageID!!) as? ChatMessage
            if (!message?.chatRoomID.isNullOrBlank())
                HLServerCallsChat.sendChatHack(HLServerCallsChat.HackType.DOC_OPENED, mUser.userId, message!!.chatRoomID!!, messageID)
        }
    }


    class WebViewClickHandler(private val toolbar: View): Handler() {

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)

            if (toolbar.height == 0) {
                toolbar.baseAnimateHeight(
                        TOOLBAR_ANIM_DURATION,
                        true,
                        TOOLBAR_EXPANDED_HEIGHT,
                        automaticCollapse = 2000
                )
            }
        }

    }


    //region == SHARE ==

    override fun getUserID(): String {
        return mUser.userId
    }

    override fun getPostOrMessageID(): String {
        return messageID ?: ""
    }

    //endregion

}

