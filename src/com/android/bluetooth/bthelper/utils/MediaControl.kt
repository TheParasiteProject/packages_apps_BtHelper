/*
 * SPDX-FileCopyrightText: Matthias Urhahn
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils

import android.content.Context

class MediaControl(context: Context) {
    init {
        audioManager = context.getSystemService(AudioManager::class.java)
    }

    companion object {
        private var audioManager: AudioManager
        private var mInstance: MediaControl? = null

        @Synchronized
        fun getInstance(context: Context): MediaControl {
            if (mInstance == null) {
                mInstance = MediaControl(context)
            }
            return mInstance!!
        }

        @JvmStatic
        val isPlaying: Boolean
            get() = audioManager.isMusicActive()

        @JvmStatic
        fun sendPlay() {
            if (isPlaying) {
                return
            }
            sendKey(KeyEvent.KEYCODE_MEDIA_PLAY)
        }

        @JvmStatic
        fun sendPause() {
            if (!isPlaying) {
                return
            }
            sendKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
        }

        fun sendPlayPause() {
            if (isPlaying) {
                sendPause()
            } else {
                sendPlay()
            }
        }

        private fun sendKey(keyCode: Int) {
            val eventTime: Long = SystemClock.uptimeMillis()
            audioManager.dispatchMediaKeyEvent(
                KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            )
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            audioManager.dispatchMediaKeyEvent(
                KeyEvent(eventTime + 200, eventTime + 200, KeyEvent.ACTION_UP, keyCode, 0)
            )
        }
    }
}
