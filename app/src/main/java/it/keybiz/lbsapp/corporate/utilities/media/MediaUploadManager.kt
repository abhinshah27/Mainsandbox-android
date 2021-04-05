/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.media

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.StringRes
import it.keybiz.lbsapp.corporate.BuildConfig
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.BaseHelper
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.HLFragment
import it.keybiz.lbsapp.corporate.base.LBSLinkApp
import it.keybiz.lbsapp.corporate.connection.HLServerCalls
import it.keybiz.lbsapp.corporate.connection.ProgressRequestBody
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostHelper
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.models.Post
import it.keybiz.lbsapp.corporate.models.chat.ChatMessageType
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.listeners.OnPermissionsDenied
import it.keybiz.lbsapp.corporate.utilities.listeners.OnTargetMediaUriSelectedListener
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * @author mbaldrighi on 10/18/2018.
 */
enum class HLMediaType(val value: String) {
    AUDIO("audio"),
    PHOTO("photo"),
    PHOTO_PROFILE("photoprofile"),
    PHOTO_WALL("photowall"),
    VIDEO("video"),
    DOCUMENT("document");

    override fun toString(): String {
        return value.toLowerCase()
    }
}


const val MEDIA_UPLOAD_VALUE = Constants.HTTP_KEY
const val MEDIA_UPLOAD_NEW_PIC_PROFILE = "pp"
const val MEDIA_UPLOAD_NEW_PIC_PROFILE_INTEREST = "ppi"
const val MEDIA_UPLOAD_NEW_PIC_WALL = "wp"
const val MEDIA_UPLOAD_NEW_PIC_WALL_INTEREST = "wpi"
const val MEDIA_UPLOAD_CLAIM = "claim"
const val MEDIA_UPLOAD_CHAT_VIDEO = "videoForChat"
const val MEDIA_UPLOAD_CHAT_DOCUMENT = "document"
const val LIMIT_MIN_SIZE_FOR_UPLOAD_VIDEO = 5

class MediaUploadManager(context: Context,
                         private val uploadListener: OnUploadCompleteListener?,
                         private val permissionsListener: OnPermissionsDenied?):
        BaseHelper(context), OnTargetMediaUriSelectedListener, FileFromUriListener {

    companion object {
        val LOG_TAG = MediaUploadManager::class.qualifiedName

        fun isMediaFileValid(file: File?): Boolean {
            return file?.exists() == true && file.length() > 0
        }
    }

    private val uploadService: UploadService by lazy {
        UploadService(context as HLActivity)
    }

    var mediaFileUri: String? = null
    var mediaType: HLMediaType = HLMediaType.PHOTO
    var fragment: HLFragment? = null


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA ->
                if (grantResults.isNotEmpty()) {
                    mediaFileUri = if (grantResults.size == 3 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                        MediaHelper.checkPermissionAndFireCameraIntent(contextRef.get() as? HLActivity, mediaType, fragment)
                    } else if (grantResults.size == 2 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        MediaHelper.checkPermissionAndFireCameraIntent(contextRef.get() as? HLActivity, mediaType, fragment)
                    } else if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        MediaHelper.checkPermissionAndFireCameraIntent(contextRef.get() as? HLActivity, mediaType, fragment)
                    else {
                        permissionsListener?.handlePermissionsDenied(Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA)
                        null
                    }
                } else permissionsListener?.handlePermissionsDenied(Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA)

            Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO ->
                if (grantResults.isNotEmpty()) {
                    mediaFileUri = if (grantResults.size == 3 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                        MediaHelper.checkPermissionForAudio(contextRef.get() as? HLActivity, HLMediaType.AUDIO, fragment)
                    } else if (grantResults.size == 2 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        MediaHelper.checkPermissionForAudio(contextRef.get() as? HLActivity, HLMediaType.AUDIO, fragment)
                    } else if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        MediaHelper.checkPermissionForAudio(contextRef.get() as? HLActivity, HLMediaType.AUDIO, fragment)
                    else {
                        permissionsListener?.handlePermissionsDenied(Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO)
                        null
                    }
                } else permissionsListener?.handlePermissionsDenied(Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO)

            Constants.PERMISSIONS_REQUEST_GALLERY ->
                if (grantResults.isNotEmpty()) {
                    if (grantResults.size == 2 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        MediaHelper.checkPermissionForGallery(contextRef.get() as? HLActivity, mediaType, fragment)
                    } else if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        MediaHelper.checkPermissionForGallery(contextRef.get() as? HLActivity, mediaType, fragment)
                    else
                        permissionsListener?.handlePermissionsDenied(Constants.PERMISSIONS_REQUEST_GALLERY)
                } else permissionsListener?.handlePermissionsDenied(Constants.PERMISSIONS_REQUEST_GALLERY)

            Constants.PERMISSIONS_REQUEST_DOCUMENTS ->
                if (grantResults.isNotEmpty()) {
                    if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        MediaHelper.checkPermissionForDocuments(contextRef.get() as? HLActivity, fragment, false)
                    else
                        permissionsListener?.handlePermissionsDenied(Constants.PERMISSIONS_REQUEST_DOCUMENTS)
                } else permissionsListener?.handlePermissionsDenied(Constants.PERMISSIONS_REQUEST_DOCUMENTS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            LBSLinkApp.fromMediaTaking = true

            (fragment?.activity as? HLActivity)?.setProgressMessage(R.string.processing_file)
            (fragment?.activity as? HLActivity)?.handleProgress(true)

            var f: File? = null
            if (requestCode == Constants.RESULT_PHOTO || requestCode == Constants.RESULT_VIDEO) {
                if (!mediaFileUri.isNullOrBlank()) {
                    val u = Uri.parse(mediaFileUri)
                    f = File(u.path!!)
                    if (f.exists()) {
                        when (requestCode) {
                            Constants.RESULT_PHOTO -> uploadListener?.handlePictureResult(requestCode, resultCode, data, f)
                            Constants.RESULT_VIDEO -> uploadListener?.handleVideoResult(requestCode, resultCode, data, f, mediaFileUri)
                        }
                    }
                }
                else {
                    (contextRef.get() as? HLActivity)?.showAlert(R.string.error_upload_media)
                    (contextRef.get() as? HLActivity)?.handleProgress(false)
                }
            }
            else if (requestCode == Constants.RESULT_GALLERY) {
                if (data != null && data.data != null) {
                    val selectedFile = data.data
                    val filePathColumn = arrayOfNulls<String>(1)
                    if (mediaType == HLMediaType.PHOTO ||
                            mediaType == HLMediaType.PHOTO_PROFILE ||
                            mediaType == HLMediaType.PHOTO_WALL) {

                        filePathColumn[0] = MediaStore.Images.Media.DATA
                    } else if (mediaType == HLMediaType.VIDEO) {
                        filePathColumn[0] = MediaStore.Video.Media.DATA
                    }

                    val cursor = (contextRef.get() as Activity).contentResolver.query(selectedFile!!,
                            filePathColumn, null, null, null)
                    if (cursor != null) {
                        cursor.moveToFirst()

                        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                        mediaFileUri = cursor.getString(columnIndex)
                        cursor.close()

                        if (!mediaFileUri.isNullOrBlank()) {
                            f = File(Uri.parse(mediaFileUri).path!!)

                            if (mediaType == HLMediaType.PHOTO ||
                                    mediaType == HLMediaType.PHOTO_PROFILE ||
                                    mediaType == HLMediaType.PHOTO_WALL) {
                                uploadListener?.handlePictureResult(requestCode, resultCode, data, f)
                            } else if (mediaType == HLMediaType.VIDEO) {
                                uploadListener?.handleVideoResult(requestCode, resultCode, data, f, mediaFileUri)
                            }
                        }
                        else {
                            (contextRef.get() as? HLActivity)?.showAlert(R.string.error_upload_media)
                            (contextRef.get() as? HLActivity)?.handleProgress(false)
                        }
                    }
                }
            }
            else if (requestCode == Constants.RESULT_DOCUMENTS) {
                if (data != null && data.data != null) {
                    MediaHelper.getFileFromContentUri(data.data, this, false)
                    return
                }

                showAlert(R.string.error_upload_media)
                (contextRef.get() as? HLActivity)?.handleProgress(false)
            }

            uploadListener?.handleMediaFinalOps(f)
        }
    }


    override fun onTargetMediaUriSelect(mediaFileUri: String?) {
        this.mediaFileUri = mediaFileUri
    }

    private fun parseMediaUploadResponse(activity: HLActivity, body: ResponseBody?): String? {
        if (body != null) {
            try {
                val s = body.string()
                if (Utils.isStringValid(s)) {
                    val jResponse = JSONObject(s)
                    if (jResponse.length() > 0) {
                        val result = jResponse.getJSONArray("results")
                        if (result != null && result.length() == 1) {
                            val res = result.getJSONObject(0)
                            if (res.length() > 0) {

                                if (res.has("error") && res.has("responseStatus") && res.has("description")) {
                                    val resStatus = res.getInt("responseStatus")
                                    LogUtils.e(LOG_TAG, "Media upload FAIL with error code " + resStatus + " and error " + res.getString("error"))

                                    if (resStatus == Constants.SERVER_ERROR_PIC_MODERATION || resStatus == Constants.SERVER_ERROR_UPLOAD_NO_SIZE) {
                                        activity.showAlert(res.getString("description"))
                                        return null
                                    }
                                    activity.showAlert(R.string.error_upload_media)
                                }
                                else if (res.has("url")) {
                                    val link = res.getString("url")
                                    LogUtils.d(LOG_TAG, "Media link: $link")

                                    return link
                                }
                            }
                        }
                    }
                }
            } catch (e: JSONException) {
                LogUtils.e(LOG_TAG, "Media upload FAIL: " + e.message, e)
                activity.showAlert(R.string.error_upload_media)
            } catch (e: IOException) {
                LogUtils.e(LOG_TAG, "Media upload FAIL: " + e.message, e)
                activity.showAlert(R.string.error_upload_media)
            }

        }

        return null
    }

    fun uploadMedia(
            context: Context?,
            file: File?,
            type: String? = null,
            userId: String?,
            name: String? = null,
            post: Post? = null,                                                         // constructor for MediaUploadListener
            mediaType: HLMediaType? = null,                                             // constructor for MediaUploadListener
            viewType: CreatePostHelper.ViewType? = CreatePostHelper.ViewType.NORMAL,    // constructor for MediaUploadListener
            path: String? = null,                                                       // constructor for MediaUploadListener
            uploadCompleteListener: OnUploadCompleteListener? = null                    // constructor for MediaUploadListener
    ) {

        if (context is HLActivity) context.handleProgress(true)

        val success = try {
            if (context !is HLActivity) false
            else if (isMediaFileValid(file) && !userId.isNullOrBlank()) {
                val uploadListener = MediaUploadListener(
                        context,
                        post,
                        mediaType,
                        viewType,
                        path,
                        uploadCompleteListener
                )
                uploadService.uploadMedia(context, file!!, type, userId, name, uploadListener)
            }
            else
                false
        } catch (e: Exception) {
            false
        }

        if (!success && context is HLActivity) {
            context.isShowProgressAnyway = false
            context.closeProgress()
            context.showAlert(if (errorMsg != 0) errorMsg else R.string.error_upload_media)
        }
    }


    inner class UploadService(private val activity: HLActivity) {

        private val MEDIA_UPLOAD_URL = if (BuildConfig.USE_PROD_CONNECTION) Constants.MEDIA_UPLOAD_URL_PROD else Constants.MEDIA_UPLOAD_URL_DEV

        /**
         * Connects to provided URL to send the media file.
         * @param context  the application's/activity's [Context] instance.
         * @param file     the file to be uploaded.
         * @param type     the eventual type of upload. Possible values: [MEDIA_UPLOAD_NEW_PIC_PROFILE],
         * [MEDIA_UPLOAD_NEW_PIC_WALL], [MEDIA_UPLOAD_NEW_PIC_PROFILE_INTEREST], [MEDIA_UPLOAD_NEW_PIC_WALL_INTEREST],
         * [MEDIA_UPLOAD_CLAIM], [MEDIA_UPLOAD_CHAT_VIDEO].
         * @param userId   the [it.keybiz.lbsapp.models.HLUser._id] of the user uploading the picture.
         * @param name     the [HLUser.getCompleteName] of the user uploading the picture.
         * @param listener the callback receiver when uploading is concluded.
         * @return True if the operation is successful.
         * @throws IOException if the operation is not successful.
         * @throws IllegalArgumentException if some of or all the provided pieces are invalid.
         */
        @Throws(IllegalArgumentException::class)
        fun uploadMedia(context: Context, file: File, type: String? = null,
                        userId: String? = null, name: String? = null,
                        listener: MediaUploadListener): Boolean {

            if (!Utils.isDeviceConnected(context))
                return false

            val mediaType = MediaHelper.getMimeType(file.absolutePath)
            val format = MediaHelper.getFileExtension(file.absolutePath)

            if (Utils.areStringsValid(mediaType, format, file.name)) {
                val client = OkHttpClient.Builder()
                        .connectTimeout(1, TimeUnit.MINUTES)
                        .readTimeout(1, TimeUnit.MINUTES)
                        .writeTimeout(1, TimeUnit.MINUTES)
                        .build()

                try {
                    val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                            .addFormDataPart("file",
                                    file.name,
                                    ProgressRequestBody(file, mediaType, ProgressRequestBody.ProgressListener { num ->
                                        activity.setProgressMessage(
                                                activity.getString(
                                                        R.string.uploading_media_progress,
                                                        num
                                                )
                                        )
                                    })
                            )
                            .addFormDataPart("k", MEDIA_UPLOAD_VALUE)
                            .addFormDataPart("f", format!!)
                            .addFormDataPart("m", mediaType!!)
                            .addFormDataPart("t", type ?: "")
                            .addFormDataPart("userID", userId ?: "")
                            .addFormDataPart("name", name ?: "")
                            .addFormDataPart("date", if (Utils.isStringValid(type))
                                Utils.formatDateForDB(System.currentTimeMillis())
                            else
                                "")
                            .build()

                    val request = Request.Builder().url(MEDIA_UPLOAD_URL)
                            .post(requestBody).build()


                    client.newCall(request).enqueue(listener)
                } catch (e: Exception) {
                    LogUtils.e(LOG_TAG, e.message, e)
                    if (Utils.isContextValid(activity)) {
                        activity.isShowProgressAnyway = false
                        activity.closeProgress()
                        activity.showGenericError()
                    }
                }

                return true
            }

            activity.isShowProgressAnyway = false
            activity.closeProgress()
            throw IllegalArgumentException("Provided arguments are not valid")
        }
    }

    /**
     * Callback receiver of upload operations, shared between all the upload usages
     *
     * @param activity the application/activity Context
     * @param post the related new post
     * @param mediaType the related post's type
     * @param viewType the current usage of CreatePostHelper class (NORMAL/WISH)
     * @param path the path of the uploaded file
     * @param uploadCompleteListener listener for the upload events
     */
    inner class MediaUploadListener(private val activity: HLActivity,
                                    private val post: Post? = null,
                                    private val mediaType: HLMediaType? = null,
                                    private val viewType: CreatePostHelper.ViewType? = CreatePostHelper.ViewType.NORMAL,
                                    private val path: String? = null,
                                    private val uploadCompleteListener: OnUploadCompleteListener? = null)
        : Callback {

        override fun onFailure(call: Call, e: IOException) {
            LogUtils.e(LOG_TAG, "Media upload FAIL: " + e.message, e)
            activity.isShowProgressAnyway = false
            activity.runOnUiThread {
                activity.closeProgress()
                activity.showAlert(R.string.error_upload_media)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            activity.isShowProgressAnyway = false
            activity.runOnUiThread { activity.closeProgress() }

            if (response.isSuccessful) {
                val body = response.body()
                val link = parseMediaUploadResponse(activity, body)
                if (!Utils.isStringValid(link)) {
                    LogUtils.d(LOG_TAG, "Media upload FAIL")
                    return
                }

                LogUtils.d(LOG_TAG, "Media upload SUCCESS")
                if (post == null) uploadCompleteListener?.onUploadComplete(path, link)
                else {
                    activity.runOnUiThread { activity.setProgressMessage(R.string.sending_post) }
                    post.content = link
                    post.type = mediaType.toString()

                    try {
                        if (viewType == CreatePostHelper.ViewType.NORMAL)
                            HLServerCalls.createEditPost(post)
                        else if (viewType == CreatePostHelper.ViewType.WISH)
                            HLServerCalls.createEditPostForWish(post)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            } else {
                LogUtils.e(LOG_TAG, "Media upload FAIL: " + response.message())
                activity.runOnUiThread {
                    activity.closeProgress()
                    activity.showAlert(R.string.error_upload_media)
                }
            }
        }
    }


    interface OnUploadCompleteListener {
        fun handlePictureResult(requestCode: Int, resultCode: Int, data: Intent?, file: File?)
        fun handleVideoResult(requestCode: Int, resultCode: Int, data: Intent?, file: File?, mediaFileUri: String?)
        fun handleMediaFinalOps(file: File?)
        fun onUploadComplete(path: String?, mediaLink: String?)
        fun setChatMessageType(chatType: ChatMessageType) {}
        fun setDocumentName(name: String?) {}
    }


    //region == File from Uri ==

    @StringRes private var errorMsg: Int = 0
    override fun onProcessingComplete(pair: Pair<File?, Int>) {

        uploadListener?.setChatMessageType(ChatMessageType.DOCUMENT)

        errorMsg = pair.second

        uploadMedia(
                contextRef.get(),
                file = pair.first,
                type = MEDIA_UPLOAD_CHAT_DOCUMENT,
                mediaType = HLMediaType.DOCUMENT,
                userId = (contextRef.get() as? HLActivity)?.user?.userId,
                path = pair.first?.absolutePath,
                uploadCompleteListener = uploadListener
        )
    }

    override fun showHideProgress(msg: String?, show: Boolean) {
        if (show && !msg.isNullOrBlank())
            (contextRef.get() as? HLActivity)?.setProgressMessage(msg)
        (contextRef.get() as? HLActivity)?.handleProgress(show)
    }

    override fun showHideProgress(msg: Int, show: Boolean) {
        if (show && msg != 0)
            (contextRef.get() as? HLActivity)?.setProgressMessage(msg)
        (contextRef.get() as? HLActivity)?.handleProgress(show)
    }

    override fun showAlert(msg: Int?) {
        if (msg != null)
            (contextRef.get() as? HLActivity)?.showAlert(msg)
    }

    override fun getTmpDirectory(): File? {
        return contextRef.get()?.getExternalFilesDir("tmp")
    }

    override fun getContentResolver(): ContentResolver? {
        return contextRef.get()?.contentResolver
    }

    override fun setDocumentName(name: String?) {
        uploadListener?.setDocumentName(name)
    }

    //endregion

}


