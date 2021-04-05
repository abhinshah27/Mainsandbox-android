package it.keybiz.lbsapp.corporate.features.home

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import io.realm.Realm
import it.keybiz.lbsapp.corporate.BuildConfig
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.HLFragment
import it.keybiz.lbsapp.corporate.features.HomeActivity
import it.keybiz.lbsapp.corporate.features.globalSearch.GlobalSearchActivity
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper
import kotlinx.android.synthetic.main.home_fragment_webview.*
import kotlinx.android.synthetic.main.home_search_box.view.*
import kotlinx.android.synthetic.main.luiss_toolbar_home_wv.*
import kotlinx.android.synthetic.main.luiss_toolbar_home_wv.view.*

/**
 * @author mbaldrighi on 2/6/2019.
 */

const val HOME_DEFAULT_URL_DEV = "http://ec2-34-201-111-17.compute-1.amazonaws.com/luiss/courses/StudentsCoursesHPAndroid.html"
const val HOME_DEFAULT_URL_PROD = "http://web.lsm.icrossing.tech/luiss/courses/StudentsCoursesHPAndroid.html"

class HomeWebViewFragment: HLFragment(), SearchHelper.OnQuerySubmitted {

    companion object {
        val homeDefaultUrl = if (BuildConfig.DEBUG) HOME_DEFAULT_URL_DEV else HOME_DEFAULT_URL_PROD

        @JvmStatic fun newInstance(): HomeWebViewFragment {
            val fragment = HomeWebViewFragment()

            // INFO: 2/6/19    add custom params
            fragment.arguments = Bundle().also {
                //                it.putString(Constants.EXTRA_PARAM_1, "")
            }

            return fragment
        }
    }

    var currentUrl = homeDefaultUrl
    var searchOn = false
    var currentQuery = ""

    private var searchDrawable: TransitionDrawable? = null

    private val searchHelper by lazy { SearchHelper(this) }


    private val animationSetOn by lazy {
        AnimatorSet().also {
            it.duration = 350
            it.playTogether(animationOverlayOn, animationSearchOn)
        }
    }
    private val animationSetOff by lazy {
        AnimatorSet().also {
            it.duration = 350
            it.playTogether(animationOverlayOff, animationSearchOff)
        }
    }

    private val animationOverlayOn by lazy {
        ObjectAnimator.ofFloat(this@HomeWebViewFragment.overlay, "alpha",1f).also {
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {
                    this@HomeWebViewFragment.overlay?.visibility = View.VISIBLE
                    searchOn = true
                }
            })
        }
    }

    private val animationOverlayOff by lazy {
        ObjectAnimator.ofFloat(this@HomeWebViewFragment.overlay, "alpha",0f).also {
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    this@HomeWebViewFragment.overlay?.visibility = View.GONE
                    searchOn = false
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
        }
    }

    private val animationSearchOn by lazy {
        ObjectAnimator.ofFloat(this@HomeWebViewFragment.searchBox, "translationY", resources.getDimensionPixelSize(R.dimen.toolbar_height).toFloat()).also {
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    searchOn = true
                }
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {
                    searchDrawable?.startTransition(0)
                }
            })
        }
    }

    private val animationSearchOff by lazy {
        ObjectAnimator.ofFloat(this@HomeWebViewFragment.searchBox, "translationY", 0f).also {
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    searchOn = false
                }
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {
                    searchDrawable?.reverseTransition(0)
                }
            })
        }
    }

    private var afterLogin = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.home_fragment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onRestoreInstanceState(savedInstanceState ?: arguments)

        configureLayout(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        webView.addJavascriptInterface(LuissHomeInterface(context!!), "Android")
    }

    override fun onPause() {
        Utils.closeKeyboard(searchBox)
        val bundle = Bundle()
        onSaveInstanceState(bundle)

        if (searchOn) animationSetOff.start()

        super.onPause()
    }

    override fun onStart() {
        super.onStart()

        // if login triggered by onboarding, resuming fragment causes the webview to scroll back to 0,0 [and reload]
        if (afterLogin) {
            webView.post { this@HomeWebViewFragment.webView.scrollTo(0, 0) }
//            webView.reload()
            afterLogin = false
        }
    }

    override fun onResume() {
        super.onResume()

        if (searchOn or !currentQuery.isBlank())
            animationSetOn.start()
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        currentUrl = savedInstanceState?.getString(Constants.EXTRA_PARAM_1, homeDefaultUrl) ?: homeDefaultUrl
        searchOn = savedInstanceState?.getBoolean(Constants.EXTRA_PARAM_2, false) ?: false
        currentQuery = savedInstanceState?.getString(Constants.EXTRA_PARAM_3, "") ?: ""
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(Constants.EXTRA_PARAM_1, currentUrl)
        outState.putBoolean(Constants.EXTRA_PARAM_2, searchOn)
        outState.putString(Constants.EXTRA_PARAM_3, (searchBox.search_field as? TextView)?.text?.toString())
    }

    override fun configureResponseReceiver() {}

    override fun configureLayout(view: View) {
        toolbar.backArrow.setOnClickListener { webView.goBack() }

        if (Utils.hasKitKat()) {
            if (0 != (context?.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE)
                WebView.setWebContentsDebuggingEnabled(true)
        }

        webView.settings?.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                backArrow?.visibility = if (url != homeDefaultUrl) View.VISIBLE  else View.GONE

                currentUrl = url

                activityListener?.setProgressMessage(R.string.webview_page_loading)
                activityListener.openProgress()
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                activityListener.closeProgress()
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                activityListener?.showAlert(R.string.error_generic_operation)

                LogUtils.e(LOG_TAG, "Error loading url: " + currentUrl + "with error: " + description)
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                activityListener?.showAlert(R.string.error_generic_operation)
                LogUtils.e(LOG_TAG, "Error loading url: $currentUrl with error: ${error?.description}")
            }
        }

        webView.loadUrl(currentUrl)

        searchDrawable = ((toolbar.globalSearchBtn?.drawable) as? TransitionDrawable)?.also {
            it.isCrossFadeEnabled = true
        }

        toolbar.globalSearchContainer.setOnClickListener {
            if (!searchOn) animationSetOn.start()
            else animationSetOff.start()
        }

        searchBox.search_field.setHint(R.string.action_search)
        searchHelper.configureViews(searchBox, searchBox.search_field)
    }

    override fun setLayout() {}

    override fun onQueryReceived(query: String) {

        currentQuery = query

        if (!currentQuery.isBlank() || !currentQuery.isEmpty())
            GlobalSearchActivity.openGlobalSearchFragment(activity, currentQuery)
    }


    fun resetSearch() {
        currentQuery = ""
        searchBox?.search_field?.setText("")
        if (searchOn)
            animationSetOff.start()
    }


    //region == Getters and setters ==

    fun getSearchView(): View? {
        return this@HomeWebViewFragment.searchBox
    }

    //endregion


    /** Instantiate the interface and set the context  */
    inner class LuissHomeInterface(private val mContext: Context) {

        /** GoTo courses page  */
        @JavascriptInterface
        fun showCourses(redirectUrl: String?) {

            if (mContext is Activity) {
                var realm: Realm? = null
                try {
                    realm = RealmUtils.getCheckedRealm()
                    val user = HLUser().readUser(realm)
                    if (user != null) {
                        if (user.isValid) {
                            mContext.runOnUiThread { this@HomeWebViewFragment.webView.loadUrl(redirectUrl) }
                        } else {
                            afterLogin = true
                            Utils.checkAndOpenLogin(mContext, user, HomeActivity.PAGER_ITEM_HOME_WEBVIEW)
                        }
                    } else (mContext as? HLActivity)?.showGenericError()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    RealmUtils.closeRealm(realm)
                }
            }
        }

    }

}