/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.R

data class RegularPodsMetadata(
    val manufacturer: String = Constants.UNKNOWN,
    val model: String = Constants.UNKNOWN,
    val lowBattThreshold: Int = 20,
    val drawable: Int = R.drawable.ic_earbuds,
    val leftDrawable: Int = R.drawable.ic_earbud_left,
    val rightDrawable: Int = R.drawable.ic_earbud_right,
    val caseDrawable: Int = R.drawable.ic_earbud_case,
)

open class RegularPods(val color: Int) : IPods {
    open val drawable: Int
        get() = R.drawable.ic_earbuds

    open val leftDrawable: Int
        get() = R.drawable.ic_earbud_left

    open val rightDrawable: Int
        get() = R.drawable.ic_earbud_right

    open val caseDrawable: Int
        get() = R.drawable.ic_earbud_case

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
