/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

data class RegularPodsMetadata(
    val manufacturer: String,
    val model: String,
    val lowBattThreshold: Int,
    val drawable: Int,
    val leftDrawable: Int,
    val rightDrawable: Int,
    val caseDrawable: Int,
)

open class RegularPods(val color: Int) : IPods {
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

    override val lowBattThreshold: Int
        get() = // Most AirPods have same Low Battery Threshold to 20
        20

    override val manufacturer: String
        get() = Constants.UNKNOWN
}
