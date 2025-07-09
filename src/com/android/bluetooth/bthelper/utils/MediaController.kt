/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

@file:OptIn(ExperimentalEncodingApi::class)

package com.android.bluetooth.bthelper.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.getSharedPreferences
import kotlin.io.encoding.ExperimentalEncodingApi

object MediaController : SharedPreferences.OnSharedPreferenceChangeListener {

    @Volatile private var isRegistered: Boolean = false

    private var initialVolume: Int? = null
        @Synchronized get
        @Synchronized set

    var userPausedTheMedia = false
        @Synchronized get
        @Synchronized set

    var userPlayedTheMedia = false
        @Synchronized get
        @Synchronized set

    private var relativeVolume: Boolean = false
        @Synchronized get
        @Synchronized set

    private var conversationalAwarenessVolume: Int = 2
        @Synchronized get
        @Synchronized set

    private var conversationalAwarenessPauseMusic: Boolean = false
        @Synchronized get
        @Synchronized set

    private var handler: Handler? = null
    private val cb: AudioManager.AudioPlaybackCallback =
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(
                configs: MutableList<AudioPlaybackConfiguration>?
            ) {
                super.onPlaybackConfigChanged(configs)
                val playbackHandler = handler ?: return
                val am = audioManager ?: return
                if (configs != null && !userPausedTheMedia) {
                    playbackHandler.postDelayed({ userPlayedTheMedia = am.isMusicActive }, 7)
                }
            }
        }
    private var audioManager: AudioManager? = null
    private var sharedPreferences: SharedPreferences? = null

    @Synchronized
    fun onCreate(context: Context) {
        if (isRegistered) return

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sharedPreferences = context.getSharedPreferences()
        handler = Handler(Looper.getMainLooper())
        val audioManager = this.audioManager ?: return
        val sharedPreferences = this.sharedPreferences ?: return
        val handler = this.handler ?: return

        relativeVolume =
            sharedPreferences.getBoolean(
                Constants.KEY_RELATIVE_CONVERSATIONAL_AWARENESS_VOLUME,
                true,
            )
        conversationalAwarenessVolume =
            sharedPreferences.getInt(
                Constants.KEY_CONVERSATIONAL_AWARENESS_VOLUME,
                (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.4).toInt(),
            )
        conversationalAwarenessPauseMusic =
            sharedPreferences.getBoolean(Constants.KEY_CONVERSATIONAL_AWARENESS_PAUSE_MUSIC, false)

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        audioManager.registerAudioPlaybackCallback(cb, handler)

        isRegistered = true
    }

    @Synchronized
    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (preferences == null || key == null) return
        val audioManager = this.audioManager ?: return

        when (key) {
            Constants.KEY_RELATIVE_CONVERSATIONAL_AWARENESS_VOLUME -> {
                relativeVolume =
                    preferences.getBoolean(
                        Constants.KEY_RELATIVE_CONVERSATIONAL_AWARENESS_VOLUME,
                        true,
                    )
            }
            Constants.KEY_CONVERSATIONAL_AWARENESS_VOLUME -> {
                conversationalAwarenessVolume =
                    preferences.getInt(
                        Constants.KEY_CONVERSATIONAL_AWARENESS_VOLUME,
                        (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.4).toInt(),
                    )
            }
            Constants.KEY_CONVERSATIONAL_AWARENESS_PAUSE_MUSIC -> {
                conversationalAwarenessPauseMusic =
                    preferences.getBoolean(
                        Constants.KEY_CONVERSATIONAL_AWARENESS_PAUSE_MUSIC,
                        false,
                    )
            }
        }
    }

    @Synchronized
    fun onDestroy() {
        if (!isRegistered) return

        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        sharedPreferences = null
        audioManager?.unregisterAudioPlaybackCallback(cb)
        audioManager = null
        handler?.removeCallbacksAndMessages(null)
        handler = null
        isRegistered = false
    }

    @Synchronized
    fun getMusicActive(): Boolean {
        return audioManager?.isMusicActive ?: false
    }

    @Synchronized
    fun sendPlayPause() {
        val audioManager = this.audioManager ?: return
        if (audioManager.isMusicActive) {
            sendPause()
        } else {
            sendPlay()
        }
    }

    @Synchronized
    fun sendPreviousTrack() {
        val audioManager = this.audioManager ?: return
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        )
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        )
    }

    @Synchronized
    fun sendNextTrack() {
        val audioManager = this.audioManager ?: return
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)
        )
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT)
        )
    }

    @Synchronized
    fun sendPause(force: Boolean = false) {
        val audioManager = this.audioManager ?: return
        if ((audioManager.isMusicActive) && (!userPlayedTheMedia || force)) {
            userPausedTheMedia = if (force) audioManager.isMusicActive else true
            userPlayedTheMedia = false
            audioManager.dispatchMediaKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
            )
            audioManager.dispatchMediaKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
            )
        }
    }

    @Synchronized
    fun sendPlay() {
        val audioManager = this.audioManager ?: return
        if (userPausedTheMedia) {
            userPlayedTheMedia = false
            audioManager.dispatchMediaKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
            )
            audioManager.dispatchMediaKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY)
            )
        }
        if (!audioManager.isMusicActive) {
            userPausedTheMedia = false
        }
    }

    @Synchronized
    fun startSpeaking() {
        val audioManager = this.audioManager ?: return
        if (initialVolume == null) {
            initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val targetVolume =
                if (relativeVolume) {
                    ((initialVolume ?: 0) * conversationalAwarenessVolume / 100)
                } else if (
                    (initialVolume ?: 0) >
                        (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) *
                            conversationalAwarenessVolume / 100)
                ) {
                    (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) *
                        conversationalAwarenessVolume / 100)
                } else {
                    initialVolume
                }
            targetVolume?.let { smoothVolumeTransition((initialVolume ?: 0), it) }
            if (conversationalAwarenessPauseMusic) {
                sendPause(force = true)
            }
        }
    }

    @Synchronized
    fun stopSpeaking() {
        val audioManager = this.audioManager ?: return
        if (initialVolume != null) {
            smoothVolumeTransition(
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                (initialVolume ?: 0),
            )
            if (conversationalAwarenessPauseMusic) {
                sendPlay()
            }
            initialVolume = null
        }
    }

    @Synchronized
    private fun smoothVolumeTransition(fromVolume: Int, toVolume: Int) {
        val step = if (fromVolume < toVolume) 1 else -1
        val delay = 50L
        var currentVolume = fromVolume
        val audioManager = this.audioManager ?: return
        val handler = this.handler ?: return

        handler.post(
            object : Runnable {
                override fun run() {
                    if (currentVolume != toVolume) {
                        currentVolume += step
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                        handler.postDelayed(this, delay)
                    }
                }
            }
        )
    }
}
