/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.vincent.videocompressor.VideoCompress
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.BaseHelper
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.models.chat.ChatMessage
import it.keybiz.lbsapp.corporate.models.chat.ChatMessageType
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.baseAnimateAlpha
import it.keybiz.lbsapp.corporate.utilities.caches.AudioVideoCache
import it.keybiz.lbsapp.corporate.utilities.caches.PicturesCache
import it.keybiz.lbsapp.corporate.utilities.listeners.*
import it.keybiz.lbsapp.corporate.utilities.media.*
import kotlinx.android.synthetic.main.bottom_sheet_chat_attach.*
import kotlinx.android.synthetic.main.fragment_chat_messages.*
import java.io.File
import java.lang.ref.WeakReference
import kotlin.math.absoluteValue


/**
 * @author mbaldrighi on 10/18/2018.
 */
class ChatMessagesInteractionsHelper(view: View, private val fragment: ChatMessagesFragment):
        BaseHelper(view.context),
        AudioRecordingHelper.RecordingCallbacks,
        OnPermissionsDenied, OnPermissionsNeeded,
        VideoCompress.CompressListener,
        MediaUploadManager.OnUploadCompleteListener, OnCompleteListener<Location>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    companion object {
        val LOG_TAG = ChatMessagesInteractionsHelper::class.qualifiedName
    }

    private val weakView: WeakReference<View> by lazy {
        WeakReference<View>(view)
    }

    private val uploadHelper: MediaUploadManager by lazy {
        MediaUploadManager(view.context, this, this).also { it.fragment = fragment }
    }
    private val audioHelper: AudioRecordingHelper by lazy {
        AudioRecordingHelper(view.context, this)
    }

    private val fusedLocationClient by lazy {
        if (Utils.checkPlayServices(contextRef.get()))
            LocationServices.getFusedLocationProviderClient(contextRef.get() as Activity)
        else null
    }


    private val googleApiClient by lazy {
        GoogleApiClient.Builder(contextRef.get()!!)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
    }

    private val activity: HLActivity = contextRef.get() as HLActivity


    /* BOTTOM SHEET */
    private var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null

    private val galleryClickPicture = View.OnClickListener {
        // would need refactoring for using PostTypeEnum
        MediaHelper.checkPermissionForGallery(activity, HLMediaType.PHOTO, fragment)
        uploadHelper.mediaType = HLMediaType.PHOTO
        closeSheet()
    }
    private val galleryClickVideo = View.OnClickListener {
        // would need refactoring for using PostTypeEnum
        MediaHelper.checkPermissionForGallery(activity, HLMediaType.VIDEO, fragment)
        uploadHelper.mediaType = HLMediaType.VIDEO
        closeSheet()
    }
    private val documentsClick = View.OnClickListener {
        // would need refactoring for using PostTypeEnum
        MediaHelper.checkPermissionForDocuments(activity, fragment, false)
        uploadHelper.mediaType = HLMediaType.DOCUMENT
        closeSheet()
    }
    private val locationClick = View.OnClickListener {
        if (!Utils.checkPlayServices(contextRef.get())) return@OnClickListener

        if (!googleApiClient.isConnected)
            googleApiClient.connect()
        else if (googleApiClient.isConnected) {
            handlePermissions(
                    {
                        fragment.callServer(ChatMessagesFragment.CallType.UPDATE_ACTIVITY, userActivity = fragment.getString(R.string.chat_activity_location))
                        closeSheet()
                    },
                    askLocationPermissions
            )
        }
    }
    private val askLocationPermissions = {
        Utils.askRequiredPermissionForFragment(
                fragment,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                Constants.PERMISSIONS_REQUEST_LOCATION)
    }

    /* ACTIONS ATTACH */
    private val actionClickPlus = object: RemoveFocusClickListener(fragment.messageBox) {
        override fun onClick(v: View?) {
            super.onClick(v)
            openSheet()
        }
    }
    private val actionClickCamera = object: RemoveFocusClickListener(fragment.messageBox) {
        override fun onClick(v: View?) {
            super.onClick(v)
            MediaHelper.openPopupMenu(
                    view.context,
                    R.menu.popup_menu_camera,
                    fragment.btnActionCamera,
                    PictureOrVideoMenuItemClickListener(activity, fragment, uploadHelper)
            )
        }
    }

    private lateinit var chatMessageType: ChatMessageType
    private var documentName: String = ""

    private var lastKnownLocation: Location? = null

    private var mediaFromGallery = false


    init {
        onCreate(weakView.get())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray){
        if (requestCode == Constants.PERMISSIONS_REQUEST_LOCATION) {
            handlePermissions()
            return
        }
        uploadHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mediaFromGallery = (requestCode == Constants.PERMISSIONS_REQUEST_GALLERY)
        uploadHelper.onActivityResult(requestCode, resultCode, data)
    }


    override fun onCreate(view: View?) {
        /* BOTTOM SHEET */
        bottomSheetBehavior = BottomSheetBehavior.from(fragment.bottomSheetChat).apply {
            this.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(p0: View, p1: Float) {}

                override fun onStateChanged(p0: View, p1: Int) {
                    LogUtils.d(LOG_TAG, "New State: $p1")
                }
            })
        }
        fragment.btnGalleryPicture.setOnClickListener(galleryClickPicture)
        fragment.btnGalleryVideo.setOnClickListener(galleryClickVideo)
        fragment.btnDocument.setOnClickListener(documentsClick)
        fragment.btnLocation.setOnClickListener(locationClick)
        fragment.closeArrow.setOnClickListener { closeSheet() }

        /* ACTION */
        fragment.btnPlus.setOnClickListener(actionClickPlus)
        fragment.btnActionCamera.setOnClickListener(actionClickCamera)
        fragment.btnActionMic.setOnTouchListener(TapAndHoldGestureListener(audioHelper, this@ChatMessagesInteractionsHelper))
    }

    override fun onPause() {
        if (googleApiClient.isConnected || googleApiClient.isConnecting)
            googleApiClient.disconnect()
        super.onPause()
    }

    // region == Bottom sheet =

    fun isSheetOpen(): Boolean {
        return bottomSheetBehavior?.state == STATE_EXPANDED
//                && bottomSheetBehavior != null && bottomSheetBehavior!!.peekHeight > 0
    }

    fun closeSheet() {
        fragment.bottomSheetChat.post { bottomSheetBehavior?.state = STATE_HIDDEN }
    }

    private fun openSheet() {
        fragment.bottomSheetChat.post { bottomSheetBehavior?.state = STATE_EXPANDED }
    }

    //endregion


    //region == Location listener ==

    private var retried = false
    override fun onComplete(p0: Task<Location>) {
        if (p0.isSuccessful) {
            lastKnownLocation = p0.result

            if (lastKnownLocation == null && !retried) {
                fusedLocationClient?.lastLocation?.addOnCompleteListener(this)
                retried = true
            }
            else if (lastKnownLocation != null) {
                retried = false
                fragment.activity?.runOnUiThread {
                    fragment.addNewMessage(
                            fragment.initializeMessage(ChatMessage()).getNewOutgoingMessage(
                                    type = ChatMessageType.LOCATION.value,
                                    mediaPayload = "${lastKnownLocation?.latitude};${lastKnownLocation?.longitude}",
                                    initialized = true
                            )
                    )

                    fragment.callServer(ChatMessagesFragment.CallType.UPDATE_ACTIVITY)
                }
            }
            else {
                val alertDialogBuilder = AlertDialog.Builder(contextRef.get()!!)
                alertDialogBuilder.setPositiveButton(contextRef.get()!!.getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.setMessage(contextRef.get()!!.getString(R.string.error_chat_location_dialog_body))
                alertDialogBuilder.create().show()
            }
        }
    }

    override fun onConnected(p0: Bundle?) {

        handlePermissions(
                ifOps = {
                    fragment.callServer(ChatMessagesFragment.CallType.UPDATE_ACTIVITY, userActivity = fragment.getString(R.string.chat_activity_location))
                    closeSheet()
                },
                elseOps = {
                    Utils.askRequiredPermissionForFragment(
                        fragment,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        Constants.PERMISSIONS_REQUEST_LOCATION)
                }
        )
    }

    override fun onConnectionSuspended(p0: Int) {}

    override fun onConnectionFailed(p0: ConnectionResult) {
        (fragment.activity as? HLActivity)?.showAlert(R.string.error_chat_location)
    }

    private fun handlePermissions(ifOps: () -> Unit = {}, elseOps: () -> Unit = {}) {
        if (Utils.hasApplicationPermission(contextRef.get() as? Activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                Utils.hasApplicationPermission(contextRef.get() as? Activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            fusedLocationClient?.lastLocation?.addOnCompleteListener(this)

            ifOps()
        }
        else elseOps()
    }

    //endregion


    //region == Upload listener ==

    override fun handlePictureResult(requestCode: Int, resultCode: Int, data: Intent?, file: File?) {
        LogUtils.d(ChatMessagesFragment.LOG_TAG, "Picture selected correctly: ${file?.absolutePath}")

        chatMessageType = ChatMessageType.PICTURE
        uploadHelper.uploadMedia(
                contextRef.get(),
                file = file,
                mediaType = HLMediaType.PHOTO,
                userId = activity.user.userId,
                path = file?.absolutePath,
                uploadCompleteListener = this
        )
    }

    override fun handleVideoResult(requestCode: Int, resultCode: Int, data: Intent?, file: File?, mediaFileUri: String?) {
        LogUtils.d(ChatMessagesFragment.LOG_TAG, "Video selected correctly: ${file?.absolutePath}")

        val size = file?.length() ?: 0
        if (size == 0L) {
            activity.showAlert(R.string.error_upload_media)
            activity.closeProgress()
        }
        else if (size > (LIMIT_MIN_SIZE_FOR_UPLOAD_VIDEO * Constants.ONE_MB_IN_BYTES)) {

            chatMessageType = ChatMessageType.VIDEO

            val mediaFile = MediaHelper.getNewTempFile(activity, "compressed", ".mp4")
            if (mediaFile != null && mediaFile.exists()) {
                destVideoPath = mediaFile.absolutePath

                var processedUri = mediaFileUri
                if (Utils.isStringValid(mediaFileUri)) {
                    if (mediaFileUri!!.startsWith("file:"))
                        processedUri = mediaFileUri.substring(5)
                }

                LogUtils.d(LOG_TAG, "STARTS COMPRESSION - " + System.currentTimeMillis() + "\nPATH BEFORE COMPRESSION: " + destVideoPath)

                activity.showProgressIndicator(true)
                VideoCompress.compressVideoMedium(processedUri, destVideoPath, this)
            }
        }
        else {
            chatMessageType = ChatMessageType.VIDEO
            uploadHelper.uploadMedia(
                    contextRef.get(),
                    file = file,
                    type = MEDIA_UPLOAD_CHAT_VIDEO,
                    mediaType = HLMediaType.VIDEO,
                    userId = activity.user.userId,
                    path = file?.absolutePath,
                    uploadCompleteListener = this
            )
        }
    }

    override fun handleMediaFinalOps(file: File?) {}

    override fun onUploadComplete(path: String?, mediaLink: String?) {
        if (!path.isNullOrBlank()) {
            LogUtils.d(ChatMessagesFragment.LOG_TAG, "File uploaded correctly from $path to $mediaLink ?: \"\"")

            val newPath = MediaHelper.getNewMediaFile(contextRef.get(), mediaLink, chatMessageType.toHLMediaTypeEnum())?.path
            when (chatMessageType) {
                ChatMessageType.PICTURE -> PicturesCache.getInstance(contextRef.get()!!).moveTmpFileAndRename(path, newPath, null, mediaFromGallery)
                ChatMessageType.VIDEO -> AudioVideoCache.getInstance(contextRef.get()!!).moveTmpFileAndRename(path, newPath, Constants.MIME_VIDEO, mediaFromGallery)
                ChatMessageType.AUDIO -> AudioVideoCache.getInstance(contextRef.get()!!).moveTmpFileAndRename(path, newPath, Constants.MIME_AUDIO, false)
                else -> {}
            }

            if (!mediaLink.isNullOrBlank()) {
                with(activity) {
                    if (Utils.isContextValid(this)) {
                        this.runOnUiThread {
                            fragment.addNewMessage(
                                    fragment.initializeMessage(ChatMessage()).getNewOutgoingMessage(
                                            type = chatMessageType.value,
                                            mediaPayload = mediaLink,
                                            initialized = true,
                                            fileName = documentName
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun setChatMessageType(chatType: ChatMessageType) {
        chatMessageType = chatType
    }

    override fun setDocumentName(name: String?) {
        documentName = name ?: ""
    }

    //endregion


    //region == Video compression interface ==

    private var destVideoPath: String? = null
    override fun onSuccess() {
        val file = File(Uri.parse(destVideoPath)?.path)
        if (MediaUploadManager.isMediaFileValid(file)) {
            val size = file.length()
            LogUtils.d(LOG_TAG, "SUCCESS COMPRESSION - " + System.currentTimeMillis() + "\nwith SIZE: " + size)

            uploadHelper.uploadMedia(
                    contextRef.get(),
                    file,
                    type = MEDIA_UPLOAD_CHAT_VIDEO,
                    mediaType = HLMediaType.VIDEO,
                    userId = activity.user.userId,
                    path = destVideoPath,
                    uploadCompleteListener = this
            )
        } else {
            onFail()
        }
    }

    override fun onFail() {
        activity.showAlert(R.string.error_upload_media)
        activity.closeProgress()

        LogUtils.d(LOG_TAG, "FAILURE COMPRESSION - " + System.currentTimeMillis())
    }

    override fun onProgress(percent: Float) {
        activity.setProgressMessage(activity.getString(R.string.uploading_media_compression, percent.toInt()))
    }

    //endregion


    //region == Record audio listener ==

    override fun handlePermissionsDenied(requestedPermission: Int) {
        when (requestedPermission) {
//            Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO -> animateMediaPanel()
        }
    }

    override fun handlePermissionsNeeded(neededPermissions: Int): Boolean {
        val fileUri = MediaHelper.checkPermissionForAudio(activity, HLMediaType.AUDIO, fragment)

        return if (!fileUri.isNullOrBlank()) {
            uploadHelper.mediaFileUri = fileUri
            audioHelper.mediaFileUri = fileUri
            true
        }
        else false
    }

    private var micAnimation: ViewPropertyAnimator? = null
    override fun onStartRecording() {
        fragment.callServer(ChatMessagesFragment.CallType.UPDATE_ACTIVITY, userActivity = fragment.getString(R.string.chat_activity_recording_audio))

        fragment.btnActionCamera.visibility = View.INVISIBLE
        fragment.recordOverlay.baseAnimateAlpha(1000, true)
        micAnimation = fragment.recordingMic.baseAnimateAlpha(750, false, true)

        fragment.recordElapsedTime?.start()
        fragment.recordElapsedTime?.base = SystemClock.elapsedRealtime()
    }


    private var isCancelingAudio = false
    override fun onStopRecording(mediaFileUri: String?, exceptionCaught: Boolean) {
        fragment.callServer(ChatMessagesFragment.CallType.UPDATE_ACTIVITY)

        fragment.recordElapsedTime?.stop()

        micAnimation?.cancel()

        fragment.btnActionCamera?.visibility = View.VISIBLE

        if (!exceptionCaught) {
            if (!mediaFileUri.isNullOrBlank()) {
                val u = Uri.parse(mediaFileUri)
                val file = File(u.path!!)
                if (file.exists()) {
                    fragment.recordOverlay.baseAnimateAlpha(200, false)

                    if (isCancelingAudio) {
                        if (file.delete()) {
                            LogUtils.d(LOG_TAG, "Audio recording successfully canceled")
                            Toast.makeText(contextRef.get(), R.string.chat_rec_audio_cancel, Toast.LENGTH_SHORT).show()
                        }

                        isCancelingAudio = false
                    } else {
                        LogUtils.d(LOG_TAG, "Audio recording SUCCESSFUL: uploading media")

                        chatMessageType = ChatMessageType.AUDIO
                        uploadHelper.uploadMedia(
                                contextRef.get(),
                                file = file,
                                mediaType = HLMediaType.AUDIO,
                                userId = activity.user.userId,
                                path = file.absolutePath,
                                uploadCompleteListener = this
                        )
                    }
                }
            } else (contextRef.get() as? HLActivity)?.showAlert(R.string.error_upload_media)
        }
        else {
            fragment.recordOverlay.visibility = View.GONE
            fragment.recordOverlay.alpha = 0f
        }
    }

    override fun onSlideToCancel(motionEvent: MotionEvent, movingLeft: Boolean) {

        val view = fragment.recordOverlay
        val viewWidth = view.width

        val offsetViewBounds = Rect()
        //returns the visible bounds
        fragment.btnActionMic.getDrawingRect(offsetViewBounds)
        // calculates the relative coordinates to the parent
        fragment.frame.offsetDescendantRectToMyCoords(fragment.btnActionMic, offsetViewBounds)

        val buttonLeftPlus = offsetViewBounds.left - 10

        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        val screenWidth = size.x

        val canAnimate = (motionEvent.rawX < buttonLeftPlus) || !movingLeft
        val offScreen = motionEvent.rawX - screenWidth                  // should always be negative Int
        val visibleViewWidth = viewWidth - offScreen.absoluteValue
        val alphaOffset = visibleViewWidth / viewWidth

        if (canAnimate) {
            view.animate()
                    .alpha(alphaOffset)
                    .translationX(offScreen)
                    .setDuration(0)
                    .start()
        }

        // when slider view is visible for half of the screen width, stop recording
        if (movingLeft && visibleViewWidth <= (screenWidth*.35)) {
            view.baseAnimateAlpha(500, false)

            view.post {
                // reset original view position (??)
                view.translationX = 0f
            }


            isCancelingAudio = true
        }

    }

    //endregion

}