/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.caches

import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import it.keybiz.lbsapp.corporate.base.BaseSingletonWithArgument
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.media.DownloadReceiver
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper
import java.io.File

/**
 * @author mbaldrighi on 11/15/2018.
 */
class PicturesCache private constructor(private val context: Context): ICache {

    companion object: BaseSingletonWithArgument<PicturesCache, Context>(::PicturesCache) {
        val LOG_TAG = PicturesCache::class.qualifiedName
    }

    private val picturesMap = mutableMapOf<String, Drawable>()
    private val picturesMapUri = mutableMapOf<String, Uri>()

    init {
        DownloadReceiver.getInstance().also {
            context.applicationContext.registerReceiver(it, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            it.picturesCache = this
        }
    }


    override fun getMedia(url: String?, typeEnum: HLMediaType): Any? {
        if (url.isNullOrBlank()) return null

        val name = MediaHelper.getFileNameForMedia(url, typeEnum)
        val checkDisk = isFileOnDisk(name, typeEnum)
        return when {
            picturesMapUri[name] != null -> picturesMapUri[name]
            checkDisk.first -> {
                addToMemoryCache(checkDisk.second?.path, checkDisk.second?.length()); checkDisk.second
            }
            else -> {
                download(context, url, name, typeEnum, this)
                null
            }
        }
    }

    override fun isFileOnDisk(name: String?, typeEnum: HLMediaType): Pair<Boolean, File?> {
        val dir = context.getExternalFilesDir(Constants.PATH_CUSTOM_HIGHLANDERS + "/" + Constants.PATH_EXTERNAL_DIR_MEDIA_PHOTO)
        if (dir?.exists() == true && dir.isDirectory) {
            val arrFiles =  dir.listFiles { _, fileName ->
                fileName == name
            }
            if (arrFiles.size == 1)
                return true to arrFiles[0]
        }
        return false to null
    }

//    override fun addToMemoryCache(path: String?) {
//        FileToDrawableTask(picturesMap).execute(path)
//    }

    override fun addToMemoryCache(path: String?, size: Long?): Uri? {
        val fileName = path?.substring(path.lastIndexOf("/") + 1)
        return if (!fileName.isNullOrBlank()) {
            val uri = Uri.parse(path)
            picturesMapUri[fileName] = uri

            if (size != null) StoreCacheObjectTask().execute(HLCacheObject(fileName, System.currentTimeMillis(), size))

            /*return*/ uri
        }
        else null
    }

//    override fun moveTmpFileAndRename(oldPath: String, newPath: String?, mime: String?, fromGallery: Boolean) {
//        val fileName = MediaHelper.getFileNameFromPath(oldPath)
//
//        val checkFile = File(Uri.parse(newPath ?: "").path)
//        if (!checkFile.exists() || checkFile.length() <= 0) {
//            val file = File(context.filesDir, Constants.PATH_EXTERNAL_DIR_MEDIA_PHOTO)
//            var exists = true
//            if (!file.exists()) exists = file.mkdir()
//
//            val newFile = if (exists) File(file, fileName) else null
//            if (newFile != null)
//                super.moveTmpFileAndRename(oldPath, newFile.path, mime, fromGallery)
//        }
//    }

    override fun mustFreeUpSpace(fileSize: Int, typeEnum: HLMediaType): Triple<Boolean, Int?, File?> {
        val dir = context.getExternalFilesDir(Constants.PATH_CUSTOM_HIGHLANDERS + "/" + Constants.PATH_EXTERNAL_DIR_MEDIA_PHOTO)
        val currentSize = getDirCurrentSize(dir)

        return if (currentSize != null) {
            val sizeNeeded = currentSize + fileSize
            Triple(sizeNeeded > MAX_SIZE_CACHE_PICTURES_BYTES, sizeNeeded - MAX_SIZE_CACHE_PICTURES_BYTES, dir)
        }
        else Triple(false, null, null)
    }

    override fun flushCache() {
        picturesMapUri.clear()
//        picturesMap.clear()
    }

    override fun getTag(): String? {
        return LOG_TAG
    }



    private class FileToDrawableTask(private val picturesMap: MutableMap<String, Drawable>):
            AsyncTask<String?, Void, Drawable?>() {

        private var path: String? = null

        override fun doInBackground(vararg params: String?): Drawable? {
            path = params[0]
            return if (!path.isNullOrBlank()) Drawable.createFromPath(path) else null
        }

        override fun onPostExecute(result: Drawable?) {
            super.onPostExecute(result)

            val fileName = path?.substring(path!!.lastIndexOf("/") + 1)
            if (!fileName.isNullOrBlank())
                if (result != null) picturesMap[fileName] = result
        }
    }


}