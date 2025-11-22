/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

@file:Suppress("PrivatePropertyName")

package com.android.bluetooth.bthelper.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.android.bluetooth.bthelper.R
import java.util.concurrent.atomic.AtomicBoolean

class GestureFeedback(private val context: Context) {

    private val soundsLoaded = AtomicBoolean(false)

    private val soundPool =
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            )
            .build()

    private var soundId = 0
    private var confirmYesId = 0
    private var confirmNoId = 0

    private var lastHorizontalTime = 0L
    private var lastLeftTime = 0L
    private var lastRightTime = 0L

    private var lastVerticalTime = 0L
    private var lastUpTime = 0L
    private var lastDownTime = 0L

    private val MIN_TIME_BETWEEN_SOUNDS = 150L
    private val MIN_TIME_BETWEEN_DIRECTION = 200L

    private var currentHorizontalStreamId = 0
    private var currentVerticalStreamId = 0

    private val LEFT_VOLUME = Pair(1.0f, 0.0f)
    private val RIGHT_VOLUME = Pair(0.0f, 1.0f)
    private val VERTICAL_VOLUME = Pair(1.0f, 1.0f)

    init {
        soundId = soundPool.load(context, R.raw.blip_no, 1)
        confirmYesId = soundPool.load(context, R.raw.confirm_yes, 1)
        confirmNoId = soundPool.load(context, R.raw.confirm_no, 1)

        soundPool.setOnLoadCompleteListener { _, _, _ ->
            soundsLoaded.set(true)

            soundPool.play(soundId, 0.0f, 0.0f, 1, 0, 1.0f)
        }
    }

    fun playDirectional(isVertical: Boolean, value: Double) {
        if (!soundsLoaded.get()) {
            return
        }

        val now = SystemClock.uptimeMillis()

        if (isVertical) {
            val isUp = value > 0

            if (now - lastVerticalTime < MIN_TIME_BETWEEN_SOUNDS) {
                return
            }

            if (isUp && now - lastUpTime < MIN_TIME_BETWEEN_DIRECTION) {
                return
            }

            if (!isUp && now - lastDownTime < MIN_TIME_BETWEEN_DIRECTION) {
                return
            }

            if (currentVerticalStreamId > 0) {
                soundPool.stop(currentVerticalStreamId)
            }

            val (leftVol, rightVol) = VERTICAL_VOLUME

            currentVerticalStreamId = soundPool.play(soundId, leftVol, rightVol, 1, 0, 1.0f)

            lastVerticalTime = now
            if (isUp) {
                lastUpTime = now
            } else {
                lastDownTime = now
            }
            return
        }

        if (now - lastHorizontalTime < MIN_TIME_BETWEEN_SOUNDS) {
            return
        }

        val isRight = value > 0

        if (isRight && now - lastRightTime < MIN_TIME_BETWEEN_DIRECTION) {
            return
        }

        if (!isRight && now - lastLeftTime < MIN_TIME_BETWEEN_DIRECTION) {
            return
        }

        if (currentHorizontalStreamId > 0) {
            soundPool.stop(currentHorizontalStreamId)
        }

        val (leftVol, rightVol) = if (isRight) RIGHT_VOLUME else LEFT_VOLUME

        currentHorizontalStreamId = soundPool.play(soundId, leftVol, rightVol, 1, 0, 1.0f)

        lastHorizontalTime = now
        if (isRight) {
            lastRightTime = now
        } else {
            lastLeftTime = now
        }
    }

    fun playConfirmation(isYes: Boolean) {
        if (currentHorizontalStreamId > 0) {
            soundPool.stop(currentHorizontalStreamId)
        }
        if (currentVerticalStreamId > 0) {
            soundPool.stop(currentVerticalStreamId)
        }

        val soundId = if (isYes) confirmYesId else confirmNoId
        if (soundId != 0 && soundsLoaded.get()) {
            val streamId = soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    companion object {
        const val TAG: String = "GestureFeedback"
    }
}
