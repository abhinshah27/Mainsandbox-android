/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver
import it.keybiz.lbsapp.corporate.features.voiceVideoCalls.SDKTokenUtils
import it.keybiz.lbsapp.corporate.services.DeleteTempFileService
import it.keybiz.lbsapp.corporate.utilities.Constants
import it.keybiz.lbsapp.corporate.utilities.FragmentsUtils
import it.keybiz.lbsapp.corporate.utilities.helpers.WebLinkRecognizer
import it.keybiz.lbsapp.corporate.utilities.media.BaseSoundPoolManager
import kotlinx.android.synthetic.main.activity_generic_no_toolbar_progress_small.*
import java.lang.ref.WeakReference

/**
 * @author mbaldrighi on 10/15/2018.
 */
class ChatActivity : HLActivity(), BasicInteractionListener, ChatActivityListener,
        OnMissingConnectionListener, OnBackPressedListener, View.OnClickListener {

    internal var backListener: OnBackPressedListener? = null

    private var messagesFragmentRef: WeakReference<ChatMessagesFragment>? = null

    private val chatSoundPool by lazy {
        BaseSoundPoolManager(this, arrayOf(R.raw.chat_send), null)
    }


    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generic_no_toolbar_progress_small)
        setRootContent(R.id.root_content)
        setProgressIndicator(R.id.generic_progress_indicator)

        generic_progress_indicator.setOnClickListener(this@ChatActivity)

        chatSoundPool.init()

        manageIntent()

        SDKTokenUtils.init(this, mUser.userId, null)
    }

    override fun onResume() {
        super.onResume()

        WebLinkRecognizer.getInstance(this).onResume()
    }

    override fun onPause() {

        startService(Intent(this, HandleUnsentMessagesService::class.java))

        super.onPause()
    }

    override fun onStop() {
        WebLinkRecognizer.getInstance(this).onStop()
        super.onStop()
    }

    override fun onDestroy() {
        DeleteTempFileService.startService(this)
        super.onDestroy()
    }

    override fun onMissingConnection(operationId: Int) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
        }
    }

    override fun onBackPressed() {
        when {
            backListener != null -> backListener!!.onBackPressed()
            supportFragmentManager?.backStackEntryCount == 1 -> finish()
            else -> super.onBackPressed()
        }
    }

    override fun configureResponseReceiver() {
        if (serverMessageReceiver == null)
            serverMessageReceiver = ServerMessageReceiver()
        serverMessageReceiver.setListener(this)
    }

    override fun manageIntent() {
        val intent = intent
        if (intent != null) {
            val showFragment = intent.getIntExtra(Constants.FRAGMENT_KEY_CODE,
                    Constants.FRAGMENT_INVALID)
            val requestCode = intent.getIntExtra(Constants.REQUEST_CODE_KEY, Constants.NO_RESULT)
            val extras = intent.extras ?: Bundle()

            when (showFragment) {
                Constants.FRAGMENT_CHAT_MESSAGES ->  {
                    val roomId = extras.getString(Constants.EXTRA_PARAM_1)
                    val name = extras.getString(Constants.EXTRA_PARAM_2, "")
                    val avatar = extras.getString(Constants.EXTRA_PARAM_3, "")
                    if (roomId != null)
                        addChatMessagesFragment(roomId, name, avatar)
                }
                Constants.FRAGMENT_CHAT_ROOMS -> addChatRoomsFragment()
                Constants.FRAGMENT_CHAT_CREATION -> addChatCreationFragment()
            }
        }
    }

    override fun onClick(v: View?) {
        if (v == generic_progress_indicator) {
            // do not do anything
        }
    }

    fun playSendTone() {
        chatSoundPool.playOnce(R.raw.chat_send)
    }

    fun playIncomingMessageTone() {
        chatSoundPool.playOnce(R.raw.chat_incoming_message)
    }


    //region == Fragments section ==

    /* ROOMS */
    override fun showChatRoomsFragment() {
        addChatRoomsFragment()
    }

    private fun addChatRoomsFragment(target: Fragment? = null, requestCode: Int = Constants.NO_RESULT,
                                     animate: Boolean = true) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate)
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right)

        var fragment = supportFragmentManager.findFragmentByTag(ChatRoomsFragment.LOG_TAG) as? ChatRoomsFragment
        //		if (fragment == null) {
        fragment = ChatRoomsFragment.newInstance()
        FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
                ChatRoomsFragment.LOG_TAG, target, requestCode, ChatRoomsFragment.LOG_TAG)
        //		} else
        //			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode/*,
        //					ChatRoomsFragment.LOG_TAG*/);
        fragmentTransaction.commit()
    }

    /* MESSAGES */
    override fun showChatMessagesFragment(chatRoomId: String, participantName: String?, participantAvatar: String?,
                                          canFetchMessages: Boolean, finishActivity: Boolean) {
        addChatMessagesFragment(chatRoomId, participantName, participantAvatar, canFetchMessages, finishActivity)
    }

    private fun addChatMessagesFragment(chatRoomId: String, participantName: String?, participantAvatar: String?,
                                        canFetchMessages: Boolean = false, finishActivity: Boolean = false,
                                        target: Fragment? = null, requestCode: Int = Constants.NO_RESULT,
                                        animate: Boolean = true) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate)
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right)

        var fragment = supportFragmentManager.findFragmentByTag(ChatMessagesFragment.LOG_TAG) as? ChatMessagesFragment
        //		if (fragment == null) {
        fragment = ChatMessagesFragment.newInstance(chatRoomId, participantName, participantAvatar, canFetchMessages, finishActivity)
        FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
                ChatMessagesFragment.LOG_TAG, target, requestCode, ChatMessagesFragment.LOG_TAG)
        //		} else
        //			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode/*,
        //					ChatMessagesFragment.LOG_TAG*/);
        fragmentTransaction.commit()
    }

    /* CHAT CREATION */
    override fun showChatCreationFragment() {
        addChatCreationFragment()
    }

    private fun addChatCreationFragment(target: Fragment? = null, requestCode: Int = Constants.NO_RESULT,
                                        animate: Boolean = true) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate)
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down,
                    R.anim.no_animation, R.anim.no_animation)

        var fragment = supportFragmentManager.findFragmentByTag(ChatCreationFragment.LOG_TAG) as? ChatCreationFragment
        //		if (fragment == null) {
        fragment = ChatCreationFragment.newInstance()
        FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
                ChatMessagesFragment.LOG_TAG, target, requestCode, ChatMessagesFragment.LOG_TAG)
        //		} else
        //			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode/*,
        //					ChatCreationFragment.LOG_TAG*/);
        fragmentTransaction.commit()
    }

    companion object {
        @JvmStatic fun openChatRoomsFragment(context: Context) {
            FragmentsUtils.openFragment(
                    context,
                    Bundle().apply {
//                        this.put...
                    },
                    Constants.FRAGMENT_CHAT_ROOMS,
                    Constants.NO_RESULT,
                    ChatActivity::class.java)
        }

        @JvmStatic fun openChatMessageFragment(context: Context, chatRoomId: String,
                                               participantName: String?, participantAvatar: String?,
                                               canFetchMessages: Boolean = false) {
            FragmentsUtils.openFragment(
                    context,
                    Bundle().apply {
                        this.putString(Constants.EXTRA_PARAM_1, chatRoomId)
                        this.putString(Constants.EXTRA_PARAM_2, participantName)
                        this.putString(Constants.EXTRA_PARAM_3, participantAvatar)
                        this.putBoolean(Constants.EXTRA_PARAM_4, canFetchMessages)
                    },
                    Constants.FRAGMENT_CHAT_MESSAGES,
                    Constants.NO_RESULT,
                    ChatActivity::class.java)
        }

        fun openChatCreationFragment(context: Context) {
            FragmentsUtils.openFragment(
                    context,
                    Bundle().apply {
                        //                        this.put...
                    },
                    Constants.FRAGMENT_CHAT_CREATION,
                    Constants.NO_RESULT,
                    ChatActivity::class.java)
        }
    }

    //endregion
}


interface ChatActivityListener {
    fun showChatRoomsFragment()
    fun showChatCreationFragment()
    fun showChatMessagesFragment(chatRoomId: String, participantName: String?, participantAvatar: String?,
                                 canFetchMessages: Boolean = false, finishActivity: Boolean = false)
}
