/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods.models

import com.android.bluetooth.bthelper.pods.Pod

data class RegularPodsMetadata(
    val manufacturer: String,
    val model: String,
    val lowBattThreshold: Int,
    val drawable: Int,
    val leftDrawable: Int,
    val rightDrawable: Int,
    val caseDrawable: Int,
)

open class RegularPods(val color: String?, leftPod: Pod, rightPod: Pod, casePod: Pod) : IPods {
    private val pods = arrayOf(leftPod, rightPod, casePod)

    fun getPod(pos: Int): Pod {
        return pods[pos]
    }

    fun getParsedStatus(arg: Boolean, pos: Int): Int {
        return pods[pos].parseStatus(arg)
    }

    open val drawable: Int
        get() = -1

    open val leftDrawable: Int
        get() = -1

    open val rightDrawable: Int
        get() = -1

    open val caseDrawable: Int
        get() = -1

    override val model: String
        get() = Constants.UNKNOWN

    override val isSingle: Boolean
        get() = false

    override val isDisconnected: Boolean
        get() = pods[LEFT].isDisconnected && pods[RIGHT].isDisconnected && pods[CASE].isDisconnected

    override val lowBattThreshold: Int
        get() = // Most AirPods have same Low Battery Threshold to 20
        20

    override val manufacturer: String
        get() = Constants.UNKNOWN

    fun isInEar(pos: Int): Boolean {
        return pods[pos].isInEar
    }

    fun isCharging(pos: Int): Boolean {
        return pods[pos].isCharging
    }

    companion object {
        const val LEFT: Int = 0
        const val RIGHT: Int = 1
        const val CASE: Int = 2
    }
}
