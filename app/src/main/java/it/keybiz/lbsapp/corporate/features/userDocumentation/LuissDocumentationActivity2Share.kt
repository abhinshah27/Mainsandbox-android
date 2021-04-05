/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.userDocumentation

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.*
import android.webkit.WebView
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import it.keybiz.lbsapp.corporate.BuildConfig
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.utilities.*
import kotlinx.android.synthetic.main.activity_webview_doc.*
import kotlinx.android.synthetic.main.luiss_toolbar_transp_wv_doc.view.*
import java.io.File


/**
 * A screen dedicated to the visualization of a PDF through Google Drive viewer in a [WebView].
 * It also functions as "global" sharing platform of the file.
 */
class LuissDocumentationActivity2Share : HLActivity() {

    companion object {
        val LOG_TAG = LuissDocumentationActivity2Share::class.qualifiedName

        private const val TOOLBAR_ANIM_DURATION = 200L
        private var TOOLBAR_EXPANDED_HEIGHT: Int = 0
    }

    private var intentUrl: String? = null
    private var urlToLoad: String? = null
    private var title: String? = null
    private var path: String? = null

    private val clickHandler by lazy { WebViewClickHandler(toolbar) }


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_webview_doc)
        setRootContent(R.id.rootContent)
        setProgressIndicator(R.id.genericProgressIndicator)

        manageIntent()

        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                TOOLBAR_EXPANDED_HEIGHT = this@LuissDocumentationActivity2Share.toolbar?.height ?: 0
                this@LuissDocumentationActivity2Share.toolbar?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })

        toolbar.shareBtn.setOnClickListener {
            fireShareIntentDoc(path)
        }

        toolbar.backArrow.setOnClickListener {
            setResult(Activity.RESULT_OK, Intent())
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

                this@LuissDocumentationActivity2Share.toolbar?.baseAnimateHeight(TOOLBAR_ANIM_DURATION, false, TOOLBAR_EXPANDED_HEIGHT, 2000)
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

    }

    override fun onStart() {
        super.onStart()

        toolbar.toolbarTitle.text = title
        toolbar.shareBtn.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(this, AnalyticsUtils.ME_DOCS_VIEWER)
    }

    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        val intent = intent
        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_1))
            intentUrl = intent.getStringExtra(Constants.EXTRA_PARAM_1)
        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_2))
            title = intent.getStringExtra(Constants.EXTRA_PARAM_2)
        if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_3))
            path = intent.getStringExtra(Constants.EXTRA_PARAM_3)

        urlToLoad = getEditedUrl(intentUrl)
    }

    override fun onBackPressed() {

        if (webView!!.canGoBack())
            webView!!.goBack()
        else {
            setResult(Activity.RESULT_OK, Intent())
            finish()
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down)
        }
    }


    //region == Class custom methods ==

    private fun getEditedUrl(current: String?): String {
        return if (!current.isNullOrBlank()) "https://docs.google.com/viewer?url=$current" else ""
    }

    private fun fireShareIntentDoc(path: String?) {
        if (path.isNullOrBlank()) return

        val file = File(Uri.parse(path).path)
        if (file.exists()) {
            val fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)

            val shareIntent = ShareCompat.IntentBuilder
                    .from(this)
                    .setStream(fileUri)
                    .setType("application/pdf")
                    .setChooserTitle(R.string.action_share_title)
                    .createChooserIntent()
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(shareIntent)
        }
    }

    //endregion


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

}