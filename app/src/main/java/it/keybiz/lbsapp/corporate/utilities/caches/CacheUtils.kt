/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.caches

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import io.realm.Realm
import io.realm.RealmModel
import io.realm.Sort
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.kotlin.deleteFromRealm
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.RealmUtils
import it.keybiz.lbsapp.corporate.utilities.media.DownloadReceiver
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType
import java.io.File


// TODO: 11/28/2018    NEED TO IMPROVE BY MAKING IT PROPORTIONAL
const val MAX_SIZE_CACHE_VIDEOS_BYTES = 500 * 1000 * 1000
const val MAX_SIZE_CACHE_PICTURES_BYTES = 200 * 1000 * 1000


/**
 * @author mbaldrighi on 11/15/2018.
 */
interface ICache {

    fun getTag(): String?

    fun getMedia(url: String?, typeEnum: HLMediaType = HLMediaType.PHOTO): Any?

    fun isFileOnDisk(name: String?, typeEnum: HLMediaType = HLMediaType.PHOTO): Pair<Boolean, File?>

    fun addToMemoryCache(path: String?, size: Long?): Any?

    fun mustFreeUpSpace(fileSize: Int, typeEnum: HLMediaType = HLMediaType.PHOTO): Triple<Boolean, Int?, File?>

    fun moveTmpFileAndRename(oldPath: String?, newPath: String?, mime: String?, fromGallery: Boolean) {
        if (!oldPath.isNullOrBlank() && !newPath.isNullOrBlank()) {
            val mHandlerThread = HandlerThread("moveTmpMediaFile")
            mHandlerThread.start()
            Handler(mHandlerThread.looper).post {

                val oldFile = File(oldPath)
                if (oldFile.exists() && oldFile.length() > 0) {
                    oldFile.renameTo(File(newPath))

                    if (!fromGallery) oldFile.delete()
                }

                mHandlerThread.quitSafely()
            }
        }
    }

    fun flushCache()

    fun download(context: Context, uri: String, name: String, mediaType: HLMediaType, cache: ICache) {

        try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(uri)
        val request = DownloadManager.Request(downloadUri)

        //Restrict the types of networks over which this download may proceed.
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        //Set whether this download may proceed over a roaming connection.
        request.setAllowedOverRoaming(false)

        //Set UI reactions
        request.setVisibleInDownloadsUi(false)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
        //Set the title of this download, to be displayed in notifications (if enabled).
        request.setTitle(name)
        //Set a description of this download, to be displayed in notifications (if enabled)
        request.setDescription(name)

        //Set the MIME type of the download file
        val pair = when (mediaType) {
            HLMediaType.AUDIO -> "audio/mp4" to Constants.PATH_EXTERNAL_DIR_MEDIA_AUDIO
            HLMediaType.VIDEO -> "video/mp4" to Constants.PATH_EXTERNAL_DIR_MEDIA_VIDEO
            HLMediaType.PHOTO -> "image/jpg" to Constants.PATH_EXTERNAL_DIR_MEDIA_PHOTO
            else -> "" to ""
        }
        request.setMimeType(pair.first)

        request.allowScanningByMediaScanner()

        request.setDestinationInExternalFilesDir(
                context,
                Constants.PATH_CUSTOM_HIGHLANDERS,
                "${pair.second}/$name"
        )

        // changes User-Agent
        request.addRequestHeader("User-Agent", "mbaldrighi")

        //Enqueue a new download and same the referenceId
        val downloadReference = downloadManager.enqueue(request)

            DownloadReceiver.linkedBlockingQueue.put(downloadReference)

            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadReference))
            if (!cursor.moveToFirst()) {
                return
            }
            val sizeOfDownloadingFile = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            cursor.close()

            val downloadTriple = cache.mustFreeUpSpace(sizeOfDownloadingFile, mediaType)
            if (downloadTriple.first) {
                LogUtils.d(cache.getTag(), "Cache max size about to be reached. Remaining bytes: ${downloadTriple.second}")

                freeUpDisk(sizeOfDownloadingFile, downloadTriple.third)
            }

            LogUtils.d(cache.getTag(), "Downloading $name")
        } catch (e: Exception) {
            val s = when (e) {
                is InterruptedException -> "Download of $name INTERRUPTED"
                else -> "ERROR with download $name"
            }

            LogUtils.d(cache.getTag(), s)
            e.printStackTrace()
        }
    }

    fun getDirCurrentSize(dir: File?): Int? {
        return if (dir?.exists() == true && dir.isDirectory) {
            var size = 0L
            val iter = dir.walkTopDown().iterator()
            iter.forEach {
                if (it.exists() && it.isFile) size += it.length()
            }
            size.toInt()
        }
        else null
    }

    fun freeUpDisk(smallestNeededSize: Int, dir: File?): AsyncTask<Int, Void, Void>? {
        return if (dir?.isDirectory == true) FreeCacheDiskTask(dir).execute(smallestNeededSize)
        else null
    }

}

/**
 * Class defining the cache object
 */
@RealmClass open class HLCacheObject(@PrimaryKey var id: String = "", @Index var creationDate: Long = 0, var size: Long = 0): RealmModel


class FreeCacheDiskTask(private val dir: File?): AsyncTask<Int, Void, Void>() {

    companion object {
        const val LOG_TAG = "FreeCacheDiskTask"
    }

    override fun doInBackground(vararg params: Int?): Void? {

        val neededSpace: Int = params[0] ?: 0
        if (neededSpace > 0 && dir?.isDirectory == true) {

            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()
                val objects = RealmUtils.readFromRealmSorted(realm, HLCacheObject::class.java, "creationDate", Sort.ASCENDING)

                var freedSpace = 0L
                realm.executeTransaction {
                    for (obj in objects) {
                        if (neededSpace >= freedSpace) {
                            val list = dir.listFiles().filter { file -> file.name == (obj as HLCacheObject).id }
                            if (list.size == 1) {
                                if (list[0].delete()) {
                                    freedSpace += (obj as HLCacheObject).size
                                    obj.deleteFromRealm()
                                }
                            }
                        }
                        else break
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(LOG_TAG, e.message, e)
            } finally {
                RealmUtils.closeRealm(realm)
            }
        }

        return null
    }

}

class StoreCacheObjectTask: AsyncTask<HLCacheObject, Void, Void>() {

    companion object {
        const val LOG_TAG = "StoreCacheObjectTask"
    }

    override fun doInBackground(vararg params: HLCacheObject?): Void? {

        var realm: Realm? = null
        try {
            realm = RealmUtils.getCheckedRealm()
            realm.executeTransaction {
               it.insertOrUpdate(params[0]!!)
            }
        } catch (e: Exception) {
            LogUtils.e(LOG_TAG, e.message, e)
        } finally {
            RealmUtils.closeRealm(realm)
        }

        return null
    }

}


fun String.isPictureType(): Boolean {
    return !this.isBlank() && this.startsWith("image")
}

fun String.isAudioType(): Boolean {
    return !this.isBlank() && this.startsWith("audio")
}

fun String.isVideoType(): Boolean {
    return !this.isBlank() && this.startsWith("video")
}