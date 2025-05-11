/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods.models

import com.android.bluetooth.bthelper.pods.Pod

data class SinglePodsMetadata(
    val manufacturer: String,
    val model: String,
    val lowBattThreshold: Int,
    val drawable: Int,
)

open class SinglePods(val color: String, val pod: Pod) : IPods {
    open val drawable: Int
        get() = -1

    fun getParsedStatus(arg: Boolean): Int {
        return pod.parseStatus(arg)
    }

    override val isSingle: Boolean
        get() = true

    override val isDisconnected: Boolean
        get() = pod.isDisconnected

    override val lowBattThreshold: Int
        get() = // Most AirPods have same Low Battery Threshold to 20
        20

    override val manufacturer: String
        get() = Constants.UNKNOWN

    open val isInEar: Boolean
        get() = pod.isInEar

    open val isCharging: Boolean
        get() = pod.isCharging
}
