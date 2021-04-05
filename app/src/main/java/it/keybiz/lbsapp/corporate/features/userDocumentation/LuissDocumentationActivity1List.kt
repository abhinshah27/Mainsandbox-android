package it.keybiz.lbsapp.corporate.features.userDocumentation

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import android.widget.TextView
import io.realm.Realm
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.connection.*
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.utilities.*
import it.keybiz.lbsapp.corporate.utilities.media.DownloadReceiver
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper
import kotlinx.android.synthetic.main.activity_empty_webview.*
import org.json.JSONArray

class LuissDocumentationActivity1List: HLActivity(), DownloadReceiver.OnDownloadCompletedListener,
        OnServerMessageReceivedListener, OnMissingConnectionListener {

    companion object {
        val LOG_TAG = LuissDocumentationActivity2Share::class.qualifiedName
    }

    private var urlToLoad: String? = null

    private var documentToDownload: String? = null
    private var documentDownloadedUri: String? = null
    private var downloadReference: Long? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty_webview)
        setRootContent(R.id.rootContent)
        setProgressIndicator(R.id.genericProgressIndicator)

        manageIntent()

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
                handleProgress(true)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                handleProgress(false)

//                webView!!.loadUrl(
//                        "javascript:(function() { " +
//                                "var element = document.getElementsByClassName('ndfHFb-c4YZDc-Wrql6b')[0];"
//                                + "element.parentNode.removeChild(element);" +
//                                "})()"
//                )

            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)

//                showGenericError()
                LogUtils.e(LOG_TAG, "GENERIC Error loading url: $urlToLoad with error: ${error.toString()}")
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                // method deprecated in Java
//                    super.onReceivedError(view, errorCode, description, failingUrl)

//                showGenericError()
                LogUtils.e(LOG_TAG, "Error loading url: $urlToLoad with error: $description")
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)

//                showGenericError()
                LogUtils.e(LOG_TAG, "HTTP Error loading url: $urlToLoad with error: ${errorResponse.toString()}")
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)

//                showGenericError()
                LogUtils.e(LOG_TAG, "SSL Error loading url: $urlToLoad with error: ${error.toString()}")
            }
        }
        webView.setDownloadListener { url, _, _, mimetype, _ ->
            if (!url.isNullOrBlank()) {
                if (mimetype == MediaHelper.MIME_PDF) {
                    documentToDownload = url
                    checkAndShowDialog()
                }
            }
        }

        loadUrl()
    }

    override fun onStart() {
        super.onStart()

        configureResponseReceiver()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(this, AnalyticsUtils.ME_DOCS_LIST)
    }

//    override fun onStop() {
//        unregisterReceiver(DownloadReceiver.getInstance())
//        super.onStop()
//    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            webView.reload()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.PERMISSIONS_REQUEST_DOCUMENTS) {
            if (grantResults.isNotEmpty()) {
                for (i in grantResults) {
                    if (i != PackageManager.PERMISSION_GRANTED) break
                }

                downloadPDF(documentToDownload)
            }
        }
    }


    override fun configureResponseReceiver() {
        if (serverMessageReceiver == null)
            serverMessageReceiver = ServerMessageReceiver()
        serverMessageReceiver.setListener(this)
    }


    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        if (operationId == Constants.SERVER_OP_SEND_HACK_DOC_DOWNLOADED) {
            LogUtils.d(LOG_TAG, "Hack for >>$documentToDownload<< SENT")
            goToDoc(documentToDownload)
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        super.handleErrorResponse(operationId, errorCode)

        if (operationId == Constants.SERVER_OP_SEND_HACK_DOC_DOWNLOADED) {
            LogUtils.d(LOG_TAG, "Hack for >>$documentToDownload<< ERROR")
            goToDoc(documentToDownload)
        }
    }

    override fun onMissingConnection(operationId: Int) {
        goToDoc(documentToDownload)
    }

    override fun manageIntent() {
        if (intent?.hasExtra(Constants.EXTRA_PARAM_1) == true)
            urlToLoad = intent.getStringExtra(Constants.EXTRA_PARAM_1)
    }


    override fun onDownloadCompleted(reference: Long, downloadFileLocalUri: String?, mime: String?) {
        if (downloadReference == null || downloadReference != reference) return

        documentDownloadedUri = downloadFileLocalUri

        LogUtils.d(LOG_TAG, "DOWNLOAD_COMPLETED_CALLBACK")

        DownloadReceiver.getInstance().listeners.remove(this)

        sendDocDownloadedHack()

    }


    //region == Class custom methods ==

    private fun loadUrl() {
        webView.loadUrl(urlToLoad, mapOf("x-k" to Constants.HTTP_KEY, "x-userID" to mUser.id))
    }

    private fun goToDoc(url: String?) {
        if (!documentToDownload.isNullOrBlank()) {
            startActivityForResult(
                    Intent(this, LuissDocumentationActivity2Share::class.java).apply {
                        this.putExtra(Constants.EXTRA_PARAM_1, url)
                        this.putExtra(Constants.EXTRA_PARAM_2, url!!.substringAfterLast("/"))
                        this.putExtra(Constants.EXTRA_PARAM_3, documentDownloadedUri)
                    },
                    Constants.RESULT_DOC_DOWNLOAD
            )
        }
    }

    private fun downloadPDF(intentUrl: String?) {
        if (intentUrl.isNullOrBlank()) return

        DownloadReceiver.getInstance().also {
            applicationContext.registerReceiver(it, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            it.listeners.add(this)
        }

        try {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadUri = Uri.parse(intentUrl)
            val request = DownloadManager.Request(downloadUri)

            val title = intentUrl.substringAfterLast("/")

            //Restrict the types of networks over which this download may proceed.
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            //Set whether this download may proceed over a roaming connection.
            request.setAllowedOverRoaming(false)

            //Set UI reactions
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            //Set the title of this download, to be displayed in notifications (if enabled).
            request.setTitle(title)
            //Set a description of this download, to be displayed in notifications (if enabled)
            request.setDescription(title)

            request.setMimeType(MediaHelper.MIME_PDF)

            request.allowScanningByMediaScanner()

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)

            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(intentUrl))
            // changes User-Agent
            request.addRequestHeader("User-Agent", "mbaldrighi")

            //Enqueue a new download and same the referenceId
            downloadReference = downloadManager.enqueue(request)

            DownloadReceiver.linkedBlockingQueue.put(downloadReference)

            LogUtils.d(LOG_TAG, "Downloading $title")
        } catch (e: Exception) {
            val s = when (e) {
                is InterruptedException -> "Download of $title INTERRUPTED"
                else -> "ERROR with download $title"
            }

            LogUtils.d(LOG_TAG, s)
            e.printStackTrace()
        }
    }

    private fun sendDocDownloadedHack() {
        // realm invoked because running on other thread
        if (!documentToDownload.isNullOrBlank()) {
            var result: Array<Any>? = null
            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()
                result = HLServerCalls.sendDownloadHack(HLUser().readUser(realm).userId, documentToDownload)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                RealmUtils.closeRealm(realm)
            }

            HLRequestTracker.getInstance(application as LBSLinkApp).handleCallResult(this, this, result)
        }
    }

    private fun checkAndShowDialog() {
        if (Utils.hasApplicationPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            downloadPDF(documentToDownload)
        } else {
            val dialog = DialogUtils.createGenericAlertCustomView(this@LuissDocumentationActivity1List, R.layout.custom_dialog_generic_title_text_btns)
            val view = dialog.customView
            view?.findViewById<TextView>(R.id.dialog_title)?.text = "Scarica documento"
            view?.findViewById<TextView>(R.id.dialog_message)?.text = "Per poter scaricare il documento, devi garantire il permesso a Luiss di scrivere il file sul dispositivo.\n\nVuoi garantirlo ora?"
            view?.findViewById<TextView>(R.id.button_positive)?.apply {
                this.text = "Richiedi permesso"
                this.setOnClickListener {
                    Utils.askRequiredPermission(this@LuissDocumentationActivity1List, Manifest.permission.WRITE_EXTERNAL_STORAGE, Constants.PERMISSIONS_REQUEST_DOCUMENTS)
                    DialogUtils.closeDialog(dialog)
                }
            }
            view?.findViewById<TextView>(R.id.button_negative)?.apply {
                this.text = "Annulla"
                this.setOnClickListener {
                    DialogUtils.closeDialog(dialog)
                }
            }
            DialogUtils.showDialog(dialog)
        }
    }

    //endregion

}