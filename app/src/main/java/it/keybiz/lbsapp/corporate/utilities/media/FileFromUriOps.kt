/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.media

import android.content.ContentResolver
import android.net.Uri
import android.os.AsyncTask
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class FileFromUriTask(private val listener: FileFromUriListener?, private val forClaim: Boolean): AsyncTask<Uri?, Void, Pair<File?, @androidx.annotation.StringRes Int>>() {


    companion object {
        val LOG_TAG = FileFromUriTask::class.qualifiedName
    }

    private var extension: String? = null

    override fun onPreExecute() {
        super.onPreExecute()

        listener?.showHideProgress(R.string.processing_file, true)
    }

    override fun doInBackground(vararg params: Uri?): Pair<File?, Int> {
        var mediaFile: File? = null

        if (params[0] != null) {
            val uri = params[0]
            val input = listener?.getContentResolver()?.openInputStream(uri!!)
            val mime = listener?.getContentResolver()?.getType(uri!!)

            if (input != null && !mime.isNullOrBlank()) {

                dumpFileName(uri!!)

                extension = MediaHelper.getFileExtension(mime, true)
                if (!Utils.isStringValid(extension)) {
                    listener?.showHideProgress(null, false)
//                    listener?.showAlert(R.string.error_processing_file)
                    return null to R.string.error_processing_file
                } else if (forClaim && mime != MIME_DOC && mime != MIME_DOCX && mime != MIME_PDF &&
                        mime != MIME_JPEG_JPG && mime != MIME_PNG) {
                    listener?.showHideProgress(null, false)
//                    listener?.showAlert(R.string.error_claim_files_allowed_extensions)
                    return null to R.string.error_claim_files_allowed_extensions
                } else if (!forClaim && mime != MIME_DOC && mime != MIME_DOCX && mime != MIME_PDF &&
                        mime != MIME_XLS && mime != MIME_XLSX && mime != MIME_PPT && mime != MIME_PPTX) {
                    listener?.showHideProgress(null, false)
//                    listener?.showAlert(R.string.error_chat_files_allowed_extensions)
                    return null to R.string.error_chat_files_allowed_extensions
                }

                var outputStream: OutputStream? = null
                try {
                    try {
                        val dir = listener?.getTmpDirectory()
                        if (dir?.exists() == true && dir.isDirectory)
                            mediaFile = File.createTempFile("tmp", ".$extension", dir)
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        LogUtils.e(LOG_TAG, ex.message, ex)
                        return null to 0
                    }

                    if (mediaFile?.exists() == true) {
                        // write the inputStream to a FileOutputStream
                        outputStream = FileOutputStream(mediaFile)

                        val bytes = ByteArray(1024)
                        var read = input.read(bytes)
                        while (read != -1) {
                            outputStream.write(bytes, 0, read)
                            read = input.read(bytes)
                        }

                        LogUtils.d(LOG_TAG, "File processed from Uri: DONE")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    listener?.showHideProgress(null, false)
                    return null to 0
                } finally {
                    try {
                        input.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        listener?.showHideProgress(null, false)
                    }

                    if (outputStream != null) {
                        try {
                            outputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                }
            }
        }

        return mediaFile to 0
    }

    override fun onPostExecute(result: Pair<File?, Int>) {
        super.onPostExecute(result)

        listener?.onProcessingComplete(result)
    }


    private fun dumpFileName(uri: Uri) {

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        val cursor = listener?.getContentResolver()?.query(uri, null, null, null, null, null)

        try {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                var displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                LogUtils.i(LOG_TAG, "Display Name: $displayName")

                if (displayName.isNullOrBlank()) {
                    val ext = displayName.substring(displayName.lastIndexOf("."))
                    displayName = "unknown$ext"
                }

                listener?.setDocumentName(displayName)

                // TODO   NOT NEEDED FOR NOW
//                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
//                // If the size is unknown, the value stored is null.  But since an
//                // int can't be null in Java, the behavior is implementation-specific,
//                // which is just a fancy term for "unpredictable".  So as
//                // a rule, check if it's null before assigning to an int.  This will
//                // happen often:  The storage API allows for remote files, whose
//                // size might not be locally known.
//                val size = if (!cursor.isNull(sizeIndex)) {
//                    // Technically the column stores an int, but cursor.getString()
//                    // will do the conversion automatically.
//                    cursor.getString(sizeIndex)
//                } else {
//                    "Unknown"
//                }
//                LogUtils.i(LOG_TAG, "Size: $size")
            }
        } finally {
            cursor?.close()
        }
    }

}


interface FileFromUriListener {

    fun onProcessingComplete(pair: Pair<File?, Int>)
    fun showHideProgress(msg: String?, show: Boolean)
    fun showHideProgress(@StringRes msg: Int = 0, show: Boolean)
    fun showAlert(@StringRes msg: Int?)
    fun getTmpDirectory(): File?
    fun getContentResolver(): ContentResolver?
    fun setDocumentName(name: String?)

}