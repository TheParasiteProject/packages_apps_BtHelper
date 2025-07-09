/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

data class SinglePodsMetadata(
    val manufacturer: String,
    val model: String,
    val lowBattThreshold: Int,
    val drawable: Int,
)

open class SinglePods(val color: Int) : IPods {
    open val drawable: Int
        get() = -1

    override val model: String
        get() = Constants.UNKNOWN

    override val isSingle: Boolean
        get() = true

    override val lowBattThreshold: Int
        get() = // Most AirPods have same Low Battery Threshold to 20
        20

    override val manufacturer: String
        get() = Constants.UNKNOWN
}
