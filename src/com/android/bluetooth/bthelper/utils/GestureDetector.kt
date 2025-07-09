/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

@file:OptIn(ExperimentalEncodingApi::class)

package com.android.bluetooth.bthelper.utils

import android.content.Context
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GestureDetector(context: Context, private val stopHeadTracking: () -> Unit) {
    private val audio = GestureFeedback(context)
    private val gestureProcessor = GestureProcessor()

    private var isRunning = false
    private var detectionJob: Job? = null
    private var gestureDetectedCallback: ((Boolean) -> Unit)? = null

    fun startDetection(doNotStop: Boolean = false, onGestureDetected: (Boolean) -> Unit) {
        if (isRunning) return

        isRunning = true
        gestureDetectedCallback = onGestureDetected

        gestureProcessor.clearData()

        detectionJob =
            CoroutineScope(Dispatchers.Default).launch {
                while (isRunning) {
                    delay(50)

                    val gesture = gestureProcessor.detectGestures()
                    if (gesture != null) {
                        withContext(Dispatchers.Main) {
                            audio.playConfirmation(gesture)

                            gestureDetectedCallback?.invoke(gesture)
                            stopDetection(doNotStop)
                        }
                        break
                    }
                }
            }
    }

    fun stopDetection(doNotStop: Boolean = false) {
        if (!isRunning) return

        isRunning = false

        if (!doNotStop) stopHeadTracking()

        detectionJob?.cancel()
        detectionJob = null
        gestureDetectedCallback = null
    }

    fun processHeadOrientation(horizontal: Int, vertical: Int) {
        if (!isRunning) return
        gestureProcessor.processHeadOrientation(horizontal, vertical, audio)
    }
}
