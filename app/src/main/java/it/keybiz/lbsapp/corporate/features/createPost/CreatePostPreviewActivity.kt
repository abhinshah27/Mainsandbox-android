/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.createPost

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.airbnb.lottie.LottieComposition
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.vincent.videocompressor.VideoCompress
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.connection.*
import it.keybiz.lbsapp.corporate.models.HLPosts
import it.keybiz.lbsapp.corporate.models.Post
import it.keybiz.lbsapp.corporate.models.PostPrivacy
import it.keybiz.lbsapp.corporate.models.enums.*
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.listeners.LottieCompositioListener
import it.keybiz.lbsapp.corporate.utilities.media.*
import kotlinx.android.synthetic.main.activity_cpost_preview.*
import kotlinx.android.synthetic.main.cpost_preview_color.*
import kotlinx.android.synthetic.main.cpost_preview_fit_fill.*
import kotlinx.android.synthetic.main.cpost_preview_text_anchor.*
import kotlinx.android.synthetic.main.cpost_preview_text_color.*
import kotlinx.android.synthetic.main.cpost_preview_text_size.*
import kotlinx.android.synthetic.main.custom_layout_profile_picture_shadow_tl.view.*
import kotlinx.android.synthetic.main.post_mask_lower_new.view.*
import kotlinx.android.synthetic.main.post_mask_upper_new.view.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File


class CreatePostPreviewActivity: HLActivity(), OnServerMessageReceivedListener, OnMissingConnectionListener,
        View.OnClickListener, VideoCompress.CompressListener {

    companion object {
        val LOG_TAG = CreatePostPreviewActivity::class.qualifiedName
    }

    private val uploadManager by lazy { MediaUploadManager(this, null, null) }

    private val mediaPanelHeight by lazy { resources.getDimensionPixelSize(R.dimen.create_post_preview_panel_height_no_bar) }
    private var mediaPanelOpen = false
    private var fitFillUp = false
    private var colorUp = false
    private var tColorUp = false
    private var tAnchorUp = false
    private var tSizeUp = false

    private var destVideoPath: String? = null
    private var isUploadingOrSending = false

    // VIEWS
    private var currentFitFillView: View? = null
    private var currentColorView: View? = null
    private var currentTextColorView: View? = null
    private var currentTextSizeView: View? = null
    private var currentTextAnchorView: View? = null

    // VALUES
    private var isFit = true
    @ColorInt private var currentBackgroundColorValue: Int? = null      // can't use lazy initialization (primitive type) and can't use resources this early
    @ColorInt private var currentTextColorValue: Int? = null            // can't use lazy initialization (primitive type) and can't use resources this early
    private var currentTextSizeValue = MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_MEDIUM)
    private var currentTextAnchorValue = Gravity.TOP or Gravity.START


    private var post: Post? = null
    private var mediaCaptureType: HLMediaType? = null
    private var mediaFileUri: String? = null
    private var file: Any? = null

    private var currentPosition: Long = 0

    private var textVisibleLines: Int? = null


    private val clickTextAnchor = View.OnClickListener {
        currentTextAnchorValue = when (it.id) {
            R.id.btnAnchorLeftTop -> handleTextAnchor((Gravity.TOP or Gravity.START), MemoryTextPositionEnum.TOP_LEFT.toString())
            R.id.btnAnchorCenterTop -> handleTextAnchor((Gravity.TOP or Gravity.CENTER_HORIZONTAL), MemoryTextPositionEnum.TOP_CENTER.toString())
            R.id.btnAnchorRightTop -> handleTextAnchor((Gravity.TOP or Gravity.END), MemoryTextPositionEnum.TOP_RIGHT.toString())
            R.id.btnAnchorLeftCenter -> handleTextAnchor((Gravity.CENTER_VERTICAL or Gravity.START), MemoryTextPositionEnum.CENTER_LEFT.toString())
            R.id.btnAnchorCenterCenter -> handleTextAnchor((Gravity.CENTER), MemoryTextPositionEnum.CENTER.toString())
            R.id.btnAnchorRightCenter -> handleTextAnchor((Gravity.CENTER_VERTICAL or Gravity.END), MemoryTextPositionEnum.CENTER_RIGHT.toString())
            R.id.btnAnchorLeftBottom -> handleTextAnchor((Gravity.BOTTOM or Gravity.START), MemoryTextPositionEnum.BOTTOM_LEFT.toString())
            R.id.btnAnchorCenterBottom -> handleTextAnchor((Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL), MemoryTextPositionEnum.BOTTOM_CENTER.toString())
            R.id.btnAnchorRightBottom -> handleTextAnchor((Gravity.BOTTOM or Gravity.END), MemoryTextPositionEnum.BOTTOM_RIGHT.toString())
            else -> handleTextAnchor((Gravity.TOP or Gravity.START), MemoryTextPositionEnum.TOP_LEFT.toString())
        }

        it.isSelected = !it.isSelected
        if (currentTextAnchorView != null)
            currentTextAnchorView!!.isSelected = !currentTextAnchorView!!.isSelected
        currentTextAnchorView = it
    }

    private val clickColor = View.OnClickListener {
        when (it.id) {
            R.id.btnBckgrBlack -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.BLACK), MemoryColorEnum.BLACK.toString(), false)
            R.id.btnBckgrWhite -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.WHITE), MemoryColorEnum.WHITE.toString(), false)
            R.id.btnBckgrRed -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.RED), MemoryColorEnum.RED.toString(), false)
            R.id.btnBckgrOrange -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.ORANGE), MemoryColorEnum.ORANGE.toString(), false)
            R.id.btnBckgrYellow -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.YELLOW), MemoryColorEnum.YELLOW.toString(), false)
            R.id.btnBckgrBrown -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.BROWN), MemoryColorEnum.BROWN.toString(), false)
            R.id.btnBckgrBlueD -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.BLUE_D), MemoryColorEnum.BLUE_D.toString(), false)
            R.id.btnBckgrBlueL -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.BLUE_L), MemoryColorEnum.BLUE_L.toString(), false)
            R.id.btnBckgrPurpleD -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.PURPLE_D), MemoryColorEnum.PURPLE_D.toString(), false)
            R.id.btnBckgrPurpleL -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.PURPLE_L), MemoryColorEnum.PURPLE_L.toString(), false)
            R.id.btnBckgrGreenD -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.GREEN_D), MemoryColorEnum.GREEN_D.toString(), false)
            R.id.btnBckgrGreenL -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.GREEN_L), MemoryColorEnum.GREEN_L.toString(), false)
        }

        it.isSelected = !it.isSelected
        if (currentColorView != null)
            currentColorView!!.isSelected = !currentColorView!!.isSelected
        currentColorView = it
    }

    private val clickTextColor = View.OnClickListener {
        when (it.id) {
            R.id.btnTextBlack -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.BLACK), MemoryColorEnum.BLACK.toString(), true)
            R.id.btnTextWhite -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.WHITE), MemoryColorEnum.WHITE.toString(), true)
            R.id.btnTextRed -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.RED), MemoryColorEnum.RED.toString(), true)
            R.id.btnTextOrange -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.ORANGE), MemoryColorEnum.ORANGE.toString(), true)
            R.id.btnTextYellow -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.YELLOW), MemoryColorEnum.YELLOW.toString(), true)
            R.id.btnTextBrown -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.BROWN), MemoryColorEnum.BROWN.toString(), true)
            R.id.btnTextBlueD -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.BLUE_D), MemoryColorEnum.BLUE_D.toString(), true)
            R.id.btnTextBlueL -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.BLUE_L), MemoryColorEnum.BLUE_L.toString(), true)
            R.id.btnTextPurpleD -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.PURPLE_D), MemoryColorEnum.PURPLE_D.toString(), true)
            R.id.btnTextPurpleL -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.PURPLE_L), MemoryColorEnum.PURPLE_L.toString(), true)
            R.id.btnTextGreenD -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.GREEN_D), MemoryColorEnum.GREEN_D.toString(), true)
            R.id.btnTextGreenL -> handleColor(MemoryColorEnum.getColor(resources, MemoryColorEnum.GREEN_L), MemoryColorEnum.GREEN_L.toString(), true)
        }

        it.isSelected = !it.isSelected
        if (currentTextColorView != null)
            currentTextColorView!!.isSelected = !currentTextColorView!!.isSelected
        currentTextColorView = it
    }

    private var currentSizeEnum: MemoryTextSizeEnum = MemoryTextSizeEnum.TEXT_SIZE_MEDIUM
    private var previousSizeEnum: MemoryTextSizeEnum = MemoryTextSizeEnum.TEXT_SIZE_MEDIUM
    private val clickTextSize = View.OnClickListener {
        when (it.id) {
            R.id.btnSizeLarge -> handleSize(MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_LARGE), MemoryTextSizeEnum.TEXT_SIZE_LARGE)
            R.id.btnSizeMedium -> handleSize(MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_MEDIUM), MemoryTextSizeEnum.TEXT_SIZE_MEDIUM)
            R.id.btnSizeSmall -> handleSize(MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_SMALL), MemoryTextSizeEnum.TEXT_SIZE_SMALL)
        }

        it.isSelected = !it.isSelected
        if (currentTextSizeView != null)
            currentTextSizeView!!.isSelected = !currentTextSizeView!!.isSelected
        currentTextSizeView = it
    }

    private val clickFitFill = View.OnClickListener {
        when (it.id) {
            R.id.btnFit -> {
                if (!isFit) {
                    isFit = true
                    handleFitFill()
                }
            }
            R.id.btnFill -> {
                if (isFit) {
                    isFit = false
                    handleFitFill()
                }
            }
        }

        it.isSelected = !it.isSelected
        if (currentFitFillView != null)
            currentFitFillView!!.isSelected = !currentFitFillView!!.isSelected
        currentFitFillView = it
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cpost_preview)
        setRootContent(R.id.rootContent)
        setProgressIndicator(R.id.genericProgressIndicator)

        manageIntent()


        // Click Listeners //

        btnClose.setOnClickListener { onBackPressed() }
        btnSend.setOnClickListener { attemptSendingPost(false) }

        genericProgressIndicator.setOnClickListener(this@CreatePostPreviewActivity)

        btnFitFill.setOnClickListener(this@CreatePostPreviewActivity)
        btnBackgroundColor.setOnClickListener(this@CreatePostPreviewActivity)
        btnTextColor.setOnClickListener(this@CreatePostPreviewActivity)
        btnTextSize.setOnClickListener(this@CreatePostPreviewActivity)
        btnTextAnchor.setOnClickListener(this@CreatePostPreviewActivity)

        initClickFitFill()
        initClickColor()
        initClickTextColor()
        initClickTextSize()
        initClickAnchor()


        initLayout()

    }

    override fun onStart() {
        super.onStart()

        configureResponseReceiver()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(this, AnalyticsUtils.CREATE_POST_PREVIEW)

        setLayout()
    }

    override fun onPause() {

        currentPosition = playerView?.player?.currentPosition ?: 0

        super.onPause()
    }

    override fun onStop() {

        playerView.stopPlayback()

        super.onStop()
    }

    override fun onBackPressed() {
        if (mediaPanelOpen) animateMediaPanel(null, false)
        else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }


    override fun configureResponseReceiver() {
        if (serverMessageReceiver == null)
            serverMessageReceiver = ServerMessageReceiver()
        serverMessageReceiver.setListener(this@CreatePostPreviewActivity)
    }

    override fun manageIntent() {
        val string = intent?.extras?.getString(Constants.EXTRA_PARAM_1)
        mediaCaptureType = intent?.extras?.getSerializable(Constants.EXTRA_PARAM_2) as? HLMediaType
        mediaFileUri = intent?.extras?.getString(Constants.EXTRA_PARAM_3)


        val json = JSONObject(string)
        post = Post().returnUpdatedPost(json)

        LogUtils.d(LOG_TAG, json.toString())

        isFit = post?.isTextPost == true || mediaCaptureType == HLMediaType.AUDIO || post?.doesMediaWantFitScale() == true

        currentBackgroundColorValue = post?.getBackgroundColor(resources) ?: MemoryColorEnum.getColor(resources, MemoryColorEnum.BLACK)

        val triple = post?.getTextStyle(resources)
        currentTextColorValue = triple?.first ?: MemoryColorEnum.getColor(resources, MemoryColorEnum.WHITE)
        currentTextSizeValue = triple?.second ?: MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_MEDIUM)
        currentTextAnchorValue = triple?.third ?: MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.TOP_LEFT)
    }

    override fun onClick(v: View?) {
        if (v != null) {
            if (v == genericProgressIndicator) {
                // catches all click events when on: DO NOTHING
            }
            else {
                var open: Boolean? = null
                val panelType = when (v.id) {
                    R.id.btnFitFill -> {
                        open = !fitFillUp
                        if (!open) null else PanelType.FIT_FILL
                    }
                    R.id.btnBackgroundColor -> {
                        open = !colorUp
                        if (!open) null else PanelType.COLOR
                    }
                    R.id.btnTextColor -> {
                        open = !tColorUp
                        if (!open) null else PanelType.T_COLOR
                    }
                    R.id.btnTextSize -> {
                        open = !tSizeUp
                        if (!open) null else PanelType.T_SIZE
                    }
                    R.id.btnTextAnchor -> {
                        open = !tAnchorUp
                        if (!open) null else PanelType.T_ANCHOR
                    }
                    else -> null
                }

                v.isSelected = !v.isSelected
                animateMediaPanel(panelType, open)
            }
        }
    }

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        isShowProgressAnyway = false
        closeProgress()

        if (responseObject == null || responseObject.length() == 0)
            return

        if (operationId == Constants.SERVER_OP_CREATE_POST_V2 ||
                operationId == Constants.SERVER_OP_EDIT_POST) run {

            val id = responseObject.optJSONObject(0).optString("_id")
            val totHearts = responseObject.optJSONObject(0).optInt("heartsOfUserOfThePost")
            val text = responseObject.optJSONObject(0).optString("text")
            if (Utils.isStringValid(id)) {
                when (operationId) {
                    Constants.SERVER_OP_CREATE_POST_V2, Constants.SERVER_OP_EDIT_POST ->

                        if (post != null) {
                            post!!.id = id
                            post!!.countHeartsUser = (if (totHearts > 0) totHearts else post!!.countHeartsUser)
                            post!!.sortId = (post!!.creationDate.time * -1 / 1000)

                            post!!.privacy = PostPrivacy(true, true)

                            if (Utils.isStringValid(text))
                                post!!.gsMessage = text

                            val posts = HLPosts.getInstance()
                            if (post!!.hasInitiative() && post!!.isTHInitiative) {
                                posts.updateAuthorHeartsForAllPosts(
                                        post!!.initiative.recipient,
                                        -1,
                                        post!!.initiative.heartsToTransfer.toInt()
                                )
                            }
                            posts.setPost(post!!, getRealm(), true)

                            if (post!!.isGSInitiative) {
                                getRealm().executeTransaction { user.setHasActiveGiveSupportInitiative(true) }
                            }
                            setResult(Activity.RESULT_OK)
                        }

                }

                if (Utils.hasLollipop())
                    finishAfterTransition()
                else {
                    finish()
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        super.handleErrorResponse(operationId, errorCode)

        isShowProgressAnyway = false
        closeProgress()
        showGenericError()
    }

    override fun onMissingConnection(operationId: Int) {
        isShowProgressAnyway = false
        closeProgress()
    }

    //region == Class custom methods ==

    private fun initLayout() {


        // TODO   to be restored when CROP will be implemented
        postImage.isZoomable = false


        // if media is present and it is NOT AUDIO type, enable btnFitFill
        btnFitFill.isEnabled = (post?.hasMedia() == true or !mediaFileUri.isNullOrBlank()) && mediaCaptureType != null && mediaCaptureType != HLMediaType.AUDIO

        if (post?.hasCaption() == true) {
            maskLow.post_caption.viewTreeObserver.addOnGlobalLayoutListener {
                val tempLines = Utils.calculateVisibleTextLines(maskLow.post_caption)

                var condition = false
                if (textVisibleLines != null) {
                    condition = when (previousSizeEnum) {
                        MemoryTextSizeEnum.TEXT_SIZE_LARGE -> textVisibleLines!! < tempLines
                        MemoryTextSizeEnum.TEXT_SIZE_MEDIUM -> {
                            if (currentSizeEnum == MemoryTextSizeEnum.TEXT_SIZE_SMALL)
                                textVisibleLines!! < tempLines
                            else
                                textVisibleLines!! > tempLines
                        }
                        MemoryTextSizeEnum.TEXT_SIZE_SMALL -> textVisibleLines!! > tempLines
                    }
                }

                if (textVisibleLines == null || condition)
                    textVisibleLines = tempLines

                handleAnchorEnabled()
            }
        } else {
            btnTextColor.isEnabled = false
            btnTextSize.isEnabled = false
            btnTextAnchor.isEnabled = false
        }

        if (mediaCaptureType == HLMediaType.AUDIO) {
            if (LBSLinkApp.siriComposition != null)
                waveAnimation.setComposition(LBSLinkApp.siriComposition)
            else {
                LBSLinkApp.initLottieAnimation(this, object : LottieCompositioListener() {
                    override fun onResult(result: LottieComposition?) {
                        super.onResult(result)

                        waveAnimation.setComposition(LBSLinkApp.siriComposition)
                    }
                })
            }
        }

        // TODO   add other conditions


        handleDefaultValues()

    }

    private fun handleDefaultValues() {

        if (isFit) btnFit.performClick() else btnFill.performClick()

        when (currentBackgroundColorValue) {
            MemoryColorEnum.getColor(resources, MemoryColorEnum.RED) -> btnBckgrRed.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.ORANGE) -> btnBckgrOrange.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.YELLOW) -> btnBckgrYellow.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.BROWN) -> btnBckgrBrown.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.GREEN_L) -> btnBckgrGreenL.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.GREEN_D) -> btnBckgrGreenD.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.PURPLE_L) -> btnBckgrPurpleL.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.PURPLE_D) -> btnBckgrPurpleD.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.BLUE_D) -> btnBckgrBlueD.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.BLUE_L) -> btnBckgrBlueL.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.BLACK) -> btnBckgrBlack.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.WHITE) -> btnBckgrWhite.performClick()
            else -> btnBckgrBlack.performClick()
        }

        when (currentTextColorValue) {
            MemoryColorEnum.getColor(resources, MemoryColorEnum.RED) -> btnTextRed.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.ORANGE) -> btnTextOrange.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.YELLOW) -> btnTextYellow.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.BROWN) -> btnTextBrown.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.GREEN_L) -> btnTextGreenL.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.GREEN_D) -> btnTextGreenD.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.PURPLE_L) -> btnTextPurpleL.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.PURPLE_D) -> btnTextPurpleD.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.BLUE_D) -> btnTextBlueD.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.BLUE_L) -> btnTextBlueL.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.BLACK) -> btnTextBlack.performClick()
            MemoryColorEnum.getColor(resources, MemoryColorEnum.WHITE) -> btnTextWhite.performClick()
            else -> btnTextWhite.performClick()
        }

        when (currentTextSizeValue) {
            MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_SMALL) -> btnSizeSmall.performClick()
            MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_MEDIUM) -> btnSizeMedium.performClick()
            MemoryTextSizeEnum.getSize(MemoryTextSizeEnum.TEXT_SIZE_LARGE) -> btnSizeLarge.performClick()
            else -> btnSizeMedium.performClick()
        }

        when (currentTextAnchorValue) {
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.TOP_LEFT) -> btnAnchorLeftTop.performClick()
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.TOP_CENTER) -> btnAnchorCenterTop.performClick()
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.TOP_CENTER) -> btnAnchorRightTop.performClick()
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.CENTER_LEFT) -> btnAnchorLeftCenter.performClick()
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.CENTER) -> btnAnchorCenterCenter.performClick()
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.CENTER_RIGHT) -> btnAnchorRightCenter.performClick()
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.BOTTOM_LEFT) -> btnAnchorLeftBottom.performClick()
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.BOTTOM_CENTER) -> btnAnchorCenterBottom.performClick()
            MemoryTextPositionEnum.getGravityPosition(MemoryTextPositionEnum.BOTTOM_RIGHT) -> btnAnchorRightBottom.performClick()
            else -> btnAnchorLeftTop.performClick()
        }

    }

    private fun handleAnchorEnabled() {

        maskLow.post_caption.maxLines = textVisibleLines ?: 0
        maskLow.post_caption.ellipsize = TextUtils.TruncateAt.END

        // if text is ellipsized, disable CENTER and BOTTOM positioning
        val wantsViewMoreText = maskLow.post_caption.getEllipsisCount() > 0
        with (wantsViewMoreText) {

            maskLow.view_more_text.visibility = if (this) View.VISIBLE else View.GONE

            if (this) {
                maskLow.view_more_text.text = Utils.getFormattedHtml(resources, R.string.view_more)

                btnAnchorLeftCenter.apply {
                    if (this.isSelected) {
                        this.isSelected = false
                        btnAnchorLeftTop.performClick()
                    }
                }
                btnAnchorLeftBottom.apply {
                    if (this.isSelected) {
                        this.isSelected = false
                        btnAnchorLeftTop.performClick()
                    }
                }
                btnAnchorCenterCenter.apply {
                    if (this.isSelected) {
                        this.isSelected = false
                        btnAnchorCenterTop.performClick()
                    }
                }
                btnAnchorCenterBottom.apply {
                    if (this.isSelected) {
                        this.isSelected = false
                        btnAnchorCenterTop.performClick()
                    }
                }
                btnAnchorRightCenter.apply {
                    if (this.isSelected) {
                        this.isSelected = false
                        btnAnchorRightTop.performClick()
                    }
                }
                btnAnchorRightBottom.apply {
                    if (this.isSelected) {
                        this.isSelected = false
                        btnAnchorRightTop.performClick()
                    }
                }
            }

            btnAnchorLeftCenter.isEnabled = !this
            btnAnchorLeftBottom.isEnabled = !this
            btnAnchorCenterCenter.isEnabled = !this
            btnAnchorCenterBottom.isEnabled = !this
            btnAnchorRightCenter.isEnabled = !this
            btnAnchorRightBottom.isEnabled = !this
        }
    }

    private fun setLayout() {

        // hides masks, keeping dimensions for text positioning
        maskUp.shared_via.visibility = View.GONE
        maskLow.anchor.visibility = View.INVISIBLE
        maskLow.initiative_label_layout.visibility = if (post?.hasInitiative() == true) View.INVISIBLE else View.GONE
        maskLow.tags_layout.visibility = if (post?.hasTags() == true) View.INVISIBLE else View.GONE

        maskLow.post_caption.run {
            this@run.text = post?.caption
        }

        MediaHelper.loadProfilePictureWithPlaceholder(this, mUser.avatarURL, profilePicture.pic as ImageView)

        when (mediaCaptureType) {
            HLMediaType.AUDIO -> {
                playerView.visibility = View.GONE
                postImage.visibility = View.GONE

                waveAnimation.cancelAnimation()
                waveAnimation.frame = 10
                waveAnimation.visibility = View.VISIBLE
            }

            HLMediaType.VIDEO -> {
                waveAnimation.visibility = View.GONE
                postImage.visibility = View.GONE

                playerView.visibility = View.VISIBLE
            }

            HLMediaType.PHOTO -> {
                playerView.visibility = View.GONE
                waveAnimation.visibility = View.GONE

                postImage.visibility = View.VISIBLE
            }

            else -> {
                postImage.visibility = View.GONE
                playerView.visibility = View.GONE
                waveAnimation.visibility = View.GONE
            }
        }

        handleFitFill()
    }

    private fun attemptSendingPost(fromWiFiDialog: Boolean) {
        if (post == null) {
            showGenericError()
            return
        }

        if (!checkConnectionAndShow(fromWiFiDialog)) {

            var file: File? = null
            if (Utils.isStringValid(mediaFileUri))
                file = File(Uri.parse(mediaFileUri).path)

            val fileValid = MediaUploadManager.isMediaFileValid(file)

            isUploadingOrSending = true

            openProgress()

            // if a media is present, operations shift to do media post stuff -> Returns
            if (fileValid) {

                val size = file!!.length()

                if (post!!.isVideoPost && size > (LIMIT_MIN_SIZE_FOR_UPLOAD_VIDEO * Constants.ONE_MB_IN_BYTES)) {

                    isShowProgressAnyway = true

                    Handler().postDelayed({ setProgressMessage(getString(R.string.uploading_media_compression, 0)) }, 500)

                    val mediaFile = MediaHelper.getNewTempFile(this, "compressed", ".mp4")
                    if (mediaFile?.exists() == true) {
                        destVideoPath = mediaFile.absolutePath
                        LogUtils.d(LOG_TAG, "STARTS COMPRESSION - " + System.currentTimeMillis() + "\nPATH BEFORE COMPRESSION: " + destVideoPath)

                        if (Utils.isStringValid(mediaFileUri)) {
                            if (mediaFileUri!!.startsWith("file:"))
                                mediaFileUri = mediaFileUri!!.substring(5)
                        }

                        VideoCompress.compressVideoMedium(mediaFileUri, destVideoPath, this)
                    }
                    else {
                        isShowProgressAnyway = false
                        closeProgress()
                        showGenericError()
                    }
                } else {
                    Handler().postDelayed({ setProgressMessage(R.string.uploading_media) }, 500)

                    uploadManager.uploadMedia(this, file, userId = mUser.id,
                            name = mUser.completeName, post = post, mediaType = mediaCaptureType,
                            path = mediaFileUri)
                }
                return
            } else if (!Utils.isStringValid(post!!.content) && !post!!.hasWebLink()) {
                // if a media is not present AND the post doesn't contain a contentURL (!= editMode),
                // then the post is only-text -> post what we have
                post!!.type = PostTypeEnum.TEXT.toString()
            }

            setProgressMessage(R.string.sending_post)

            var results: Array<Any>? = null
            try {
                results = HLServerCalls.createEditPost(post!!)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            HLRequestTracker.getInstance(application as LBSLinkApp).handleCallResult(this, this, results)
        }
    }

    private fun checkConnectionAndShow(fromWiFiDialog: Boolean): Boolean {
        if (!fromWiFiDialog && !Utils.isConnectionWiFi(this) && mediaCaptureType === HLMediaType.VIDEO) {

            val dialog = MaterialDialog.Builder(this)
                    .customView(R.layout.custom_dialog_check_wifi, false)
                    .cancelable(true)
                    .build()

            val v = dialog.customView
            if (v != null) {
                v.findViewById<View>(R.id.dialog_btn_confirm).setOnClickListener {
                    attemptSendingPost(true)
                    dialog.dismiss()
                }

                v.findViewById<View>(R.id.dialog_btn_cancel).setOnClickListener { dialog.dismiss() }
            }

            dialog.show()

            return true
        }

        return false
    }

    private fun animateMediaPanel(type: PanelType?, actionOpen: Boolean?) {
        var actionOpen = actionOpen
        if (panelViewToAnimate == null) return

        val anim: ValueAnimator
        val targetHeight: Int
        val baseHeight = if (panelViewToAnimate!!.height == 0) 1 else panelViewToAnimate!!.height

        if (actionOpen == null)
            actionOpen = !mediaPanelOpen
        targetHeight = if (type == null || !actionOpen) 1 else mediaPanelHeight

        handlePanelViews(type)

        if (baseHeight != targetHeight) {
            anim = ValueAnimator.ofInt(baseHeight, targetHeight)
            anim.duration = 350
            anim.addUpdateListener(MediaPanelAnimationUpdateListener())
            anim.addListener(MediaPanelAnimationListener())
            anim.start()
        }
    }

    private enum class PanelType { FIT_FILL, COLOR, T_COLOR, T_SIZE, T_ANCHOR }
    private fun handlePanelViews(type: PanelType?) {
        when (type) {
            null -> {
                fitFillUp = false; colorUp = false; tColorUp = false; tSizeUp = false; tAnchorUp = false
            }
            PanelType.FIT_FILL -> {
                fitFillUp = true; colorUp = false; tColorUp = false; tSizeUp = false; tAnchorUp = false
            }
            PanelType.COLOR -> {
                fitFillUp = false; colorUp = true; tColorUp = false; tSizeUp = false; tAnchorUp = false
            }
            PanelType.T_COLOR -> {
                fitFillUp = false; colorUp = false; tColorUp = true; tSizeUp = false; tAnchorUp = false
            }
            PanelType.T_SIZE -> {
                fitFillUp = false; colorUp = false; tColorUp = false; tSizeUp = true; tAnchorUp = false
            }
            PanelType.T_ANCHOR -> {
                fitFillUp = false; colorUp = false; tColorUp = false; tSizeUp = false; tAnchorUp = true
            }
        }

        panelFitFill.visibility = if (fitFillUp) View.VISIBLE else View.GONE; btnFitFill.isSelected = fitFillUp
        panelColor.visibility = if (colorUp) View.VISIBLE else View.GONE; btnBackgroundColor.isSelected = colorUp
        panelTextColor.visibility = if (tColorUp) View.VISIBLE else View.GONE; btnTextColor.isSelected = tColorUp
        panelTextSize.visibility = if (tSizeUp) View.VISIBLE else View.GONE; btnTextSize.isSelected = tSizeUp
        panelTextAnchor.visibility = if (tAnchorUp) View.VISIBLE else View.GONE; btnTextAnchor.isSelected = tAnchorUp
    }

    private fun initClickFitFill() {
        clickFitFill.run {
            this@CreatePostPreviewActivity.btnFit.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnFill.setOnClickListener(this@run)
        }
    }

    private fun initClickColor() {
        clickColor.run {
            this@CreatePostPreviewActivity.btnBckgrBlack.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrWhite.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrRed.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrOrange.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrYellow.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrBrown.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrBlueD.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrBlueL.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrPurpleD.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrPurpleL.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrGreenD.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnBckgrGreenL.setOnClickListener(this@run)
        }
    }

    private fun initClickTextColor() {
        clickTextColor.run {
            this@CreatePostPreviewActivity.btnTextWhite.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextBlack.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextRed.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextOrange.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextYellow.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextBrown.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextBlueD.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextBlueL.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextPurpleD.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextPurpleL.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextGreenD.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnTextGreenL.setOnClickListener(this@run)
        }
    }

    private fun initClickTextSize() {
        clickTextSize.run {
            this@CreatePostPreviewActivity.btnSizeMedium.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnSizeLarge.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnSizeSmall.setOnClickListener(this@run)
        }
    }

    private fun initClickAnchor() {
        clickTextAnchor.run {
            this@CreatePostPreviewActivity.btnAnchorLeftTop.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnAnchorCenterTop.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnAnchorRightTop.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnAnchorLeftCenter.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnAnchorCenterCenter.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnAnchorRightCenter.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnAnchorLeftBottom.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnAnchorCenterBottom.setOnClickListener(this@run)
            this@CreatePostPreviewActivity.btnAnchorRightBottom.setOnClickListener(this@run)
        }
    }

    private fun handleTextAnchor(value: Int, enumValue: String): Int {
        this@CreatePostPreviewActivity.maskLow.post_caption?.gravity = value
        this@CreatePostPreviewActivity.maskLow.view_more_text?.gravity = value
        post?.setTextPosition(enumValue)

        return value
    }

    private fun handleColor(@ColorInt color: Int, enumValue: String, isText: Boolean) {
        if (isText) {
            this@CreatePostPreviewActivity.maskLow.post_caption?.setTextColor(color)
            this@CreatePostPreviewActivity.maskLow.view_more_text?.setTextColor(color)
            post?.setTextColor(enumValue, true)
        } else {
            this@CreatePostPreviewActivity.rootContent.setBackgroundColor(color)
            post?.backgroundColor = enumValue
        }
    }

    private fun handleSize(size: Float, enumValue: MemoryTextSizeEnum) {
        textVisibleLines = null
        previousSizeEnum = currentSizeEnum
        currentSizeEnum = enumValue
        this@CreatePostPreviewActivity.maskLow.view_more_text?.visibility = View.GONE
        this@CreatePostPreviewActivity.maskLow.post_caption?.textSize = size
        post?.setTextSize(currentSizeEnum.toString())
    }

    private fun handleFitFill() {

        btnBackgroundColor.isEnabled = isFit

        post?.setMediaContentMode(if (isFit) FitFillTypeEnum.FIT.value else FitFillTypeEnum.FILL.value)

        when (mediaCaptureType) {

            HLMediaType.PHOTO -> {

                if (file == null)
                    file = if (!mediaFileUri.isNullOrBlank()) File(Uri.parse(mediaFileUri).path) else post?.getContent(false)
                val request = GlideApp.with(this).load(file)

                if (isFit) {
                    postImage.scaleType = ImageView.ScaleType.FIT_CENTER
                    request.fitCenter()
                }
                else {
                    postImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    request.centerCrop()
                }

                request.into(postImage)
            }

            HLMediaType.VIDEO -> {
                if (file == null)
                    file = if (!mediaFileUri.isNullOrBlank()) File(Uri.parse(mediaFileUri).path) else post?.getContent(false)

                if (!playerView.isValid()) {
                    MediaHelper.playVideo(this, playerView, file, true, isFit,
                            true, currentPosition, null)
                }
                else {
                    playerView.resizeMode = if (isFit) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    playerView.play()
                }
            }

            else -> {
                // DO NOTHING
            }
        }

        btnFitFill.setImageDrawable(
                if (Utils.hasLollipop()) {
                    if (isFit) resources.getDrawable(R.drawable.selector_cpost_action_fit, null)
                    else resources.getDrawable(R.drawable.selector_cpost_action_fill, null)
                } else {
                    if (isFit) resources.getDrawable(R.drawable.selector_cpost_action_fit)
                    else resources.getDrawable(R.drawable.selector_cpost_action_fill)
                }
        )

    }

    //endregion


    //region == Video Compression ==

    override fun onSuccess() {
        val file = File(Uri.parse(destVideoPath).path)
        if (MediaUploadManager.isMediaFileValid(file)) {
            val size = file.length()
            LogUtils.d(LOG_TAG, "SUCCESS COMPRESSION - " + System.currentTimeMillis() + "\nwith SIZE: " + size)


            uploadManager.uploadMedia(this, file, userId = mUser.id,
                    name = mUser.completeName, post = post, mediaType = mediaCaptureType,
                    path = mediaFileUri)
        } else {
            onFail()
        }
    }

    override fun onFail() {
        showAlert(R.string.error_upload_media)
        closeProgress()

        LogUtils.d(LOG_TAG, "FAILURE COMPRESSION - " + System.currentTimeMillis())
    }

    override fun onProgress(percent: Float) {
        setProgressMessage(getString(R.string.uploading_media_compression, percent.toInt()))
    }

    //endregion


    //region == Custom inner classes ==

    private inner class MediaPanelAnimationUpdateListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            panelViewToAnimate?.layoutParams?.height = animation.animatedValue as Int
            panelViewToAnimate?.layoutParams = panelViewToAnimate?.layoutParams
        }
    }


    private inner class MediaPanelAnimationListener : Animator.AnimatorListener {

        override fun onAnimationStart(animation: Animator) {

            if (!mediaPanelOpen) {
                (panelViewToAnimate?.layoutParams as? ConstraintLayout.LayoutParams)?.let {
                    it.height = 1
                    it.topToBottom = ConstraintLayout.LayoutParams.UNSET
                    panelViewToAnimate?.layoutParams = it
                }
            }

        }

        override fun onAnimationEnd(animation: Animator) {
            if (mediaPanelOpen) {
                (panelViewToAnimate?.layoutParams as? ConstraintLayout.LayoutParams)?.let {
                    it.topToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    panelViewToAnimate?.layoutParams = it
                }
            }
            mediaPanelOpen = !mediaPanelOpen

            if (!mediaPanelOpen) { } else { }
        }

        override fun onAnimationCancel(animation: Animator) {}

        override fun onAnimationRepeat(animation: Animator) {}
    }

    //endregion

}