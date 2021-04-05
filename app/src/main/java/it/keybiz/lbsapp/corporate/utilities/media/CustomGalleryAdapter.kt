/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.media

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide

import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.utilities.LogUtils
import it.keybiz.lbsapp.corporate.utilities.Utils

/**
 * Adapter class handling the custom gallery list items.
 *
 * @author VISH on 12/6/2017.
 * @author mbaldrighi on 01/5/2017.
 * @
 */
class CustomGalleryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> {

    companion object {
        private val LOG_TAG = CustomGalleryAdapter::class.qualifiedName
        private const val MEDIA_TYPE_IMAGE = 0
        private const val MEDIA_TYPE_VIDEO = 1
    }

    private var myCursor: Cursor? = null
    private val myActivity: Activity
    private var onMediaClickListener: OnMediaClickListener? = null

    interface OnMediaClickListener {
        fun onClickImage(imageUri: String)
        fun onClickVideo(videoUri: String)
    }

    constructor(activity: Activity) {
        this.myActivity = activity
        if (activity is OnMediaClickListener)
            this.onMediaClickListener = activity
    }

    constructor(activity: Activity, listener: OnMediaClickListener) {
        this.myActivity = activity
        this.onMediaClickListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        if (viewType == MEDIA_TYPE_VIDEO) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_custom_gallery_video,
                    parent, false)
            return VideoViewHolder(view)
        }

        view = LayoutInflater.from(parent.context).inflate(R.layout.item_custom_gallery_picture,
                parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var imgView: ImageView? = null
        when (holder.itemViewType) {
            MEDIA_TYPE_IMAGE -> {
                val imageViewHolder = holder as ImageViewHolder
                imgView = imageViewHolder.imageView
            }
            MEDIA_TYPE_VIDEO -> {
                val videoViewHolder = holder as VideoViewHolder
                imgView = videoViewHolder.imageView
            }
        }

        if (imgView != null) {
            Glide.with(myActivity)
                    .load(getMediaUri(position))
                    .thumbnail(0.5f)
                    .into(imgView)
        }
    }

    override fun getItemCount(): Int {
        return if (myCursor == null || myCursor!!.isClosed) 0 else myCursor!!.count
    }

    private fun swapCursor(cursor: Cursor?): Cursor? {
        if (myCursor === cursor) {
            return null
        }
        val oldCursor = myCursor
        this.myCursor = cursor
        if (cursor != null) {
            this.notifyDataSetChanged()
        }
        return oldCursor
    }

    fun changeCursor(cursor: Cursor) {
        val oldCur = swapCursor(cursor)
        oldCur?.close()
    }

    // FUNCTION TO GET URIS OF ALL MEDIA FILE TO PASS TO GLIDE.
    private fun getMediaUri(position: Int): Uri? {

        return try {
            val dataIndex = myCursor!!.getColumnIndex(MediaStore.Files.FileColumns.DATA)

            myCursor!!.moveToPosition(position)

            val dataString = myCursor!!.getString(dataIndex)
            Uri.parse("file://$dataString")
        } catch (e: NullPointerException) {
            LogUtils.e(LOG_TAG, "Null Pointer Exception at  $position")
            null
        }

    }

    override fun getItemViewType(position: Int): Int {
        val mediaTypeIndex = myCursor!!.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
        myCursor!!.moveToPosition(position)
        return when (myCursor!!.getInt(mediaTypeIndex)) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MEDIA_TYPE_IMAGE
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MEDIA_TYPE_VIDEO
            else -> -1
        }
    }


    // FUNCTION TO GET URI OF THE CLICKED IMAGE TO PASS IN ONCLICK FUNCTIONS TO DISPLAY MEDIA IN FULL-SCREEN.
    private fun getOnClickUri(position: Int) {
        try {
            val mediaTypeIndex = myCursor!!.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val dataIndex = myCursor!!.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            myCursor!!.moveToPosition(position)

            val dataString = myCursor!!.getString(dataIndex)
            if (Utils.isStringValid(dataString) && onMediaClickListener != null) {
                when (myCursor!!.getInt(mediaTypeIndex)) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> onMediaClickListener!!.onClickImage(dataString)
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> onMediaClickListener!!.onClickVideo(dataString)
                }
            }
        } catch (e: NullPointerException) {
            LogUtils.e(LOG_TAG, "Null Pointer Exception at" + position + " " + myCursor!!.getString(myCursor!!.getColumnIndex(MediaStore.Files.FileColumns.DATA)))
        }
    }


    // IMAGE VIEW-HOLDER
    inner class ImageViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        internal var imageView: ImageView = itemView.findViewById(R.id.image_view)

        init {
            this.imageView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            getOnClickUri(adapterPosition)
        }

    }

    // VIDEO VIEW-HOLDER
    private inner class VideoViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        internal var imageView: ImageView = itemView.findViewById(R.id.video_image_view)

        init {
            this.imageView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            getOnClickUri(adapterPosition)
        }
    }
}
