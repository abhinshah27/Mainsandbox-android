/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners

import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper

/**
 * @author mbaldrighi on 10/22/2018.
 */
open class RemoveFocusClickListener(private val victim: View): View.OnClickListener {
    override fun onClick(v: View?) {
        victim.clearFocus()
    }
}


class EditPictureMenuItemClickListenerKt(private val activity: HLActivity, private val type: HLMediaType,
                                         private val fragment: Fragment? = null,
                                         private val mListener: OnTargetMediaUriSelectedListener): PopupMenu.OnMenuItemClickListener {

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.take_picture -> {
                val mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, type, fragment)
                mListener.onTargetMediaUriSelect(mediaFileUri)
            }

            R.id.pick_from_gallery -> MediaHelper.checkPermissionForGallery(activity, type, fragment)
        }
        return true
    }

}


class PictureOrVideoMenuItemClickListener(private val activity: HLActivity,
                                          private val fragment: Fragment? = null,
                                          private val mListener: OnTargetMediaUriSelectedListener):
        PopupMenu.OnMenuItemClickListener {

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        var mediaFileUri: String? = null
        when (menuItem.itemId) {
            R.id.take_picture -> {
                mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.PHOTO, fragment)
            }
            R.id.record_video -> {
                mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.VIDEO, fragment)
            }
        }
        if (!mediaFileUri.isNullOrBlank())
            mListener.onTargetMediaUriSelect(mediaFileUri)
        return true
    }

}

interface OnTargetMediaUriSelectedListener {
    fun onTargetMediaUriSelect(mediaFileUri: String?)
}