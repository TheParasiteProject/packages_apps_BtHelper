/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods.models

import com.android.bluetooth.bthelper.pods.Pod

abstract class SinglePods(val color: String, val pod: Pod) : IPods {
    abstract val drawable: Int

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

    override val menufacturer: String
        get() = Constants.UNKNOWN

    val isInEar: Boolean
        get() = pod.isInEar

    val isCharging: Boolean
        get() = pod.isCharging
}
