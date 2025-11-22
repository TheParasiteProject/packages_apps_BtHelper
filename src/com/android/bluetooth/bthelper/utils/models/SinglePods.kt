/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: LibrePods contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.R

data class SinglePodsMetadata(
    val manufacturer: String = Constants.UNKNOWN,
    val model: String = Constants.UNKNOWN,
    val lowBattThreshold: Int = 20,
    val drawable: Int = R.drawable.ic_headphones,
)

open class SinglePods(var color: Int = 0) : IPods {
    open val modelId: Int
        get() = 0

    open val modelNumbers: List<String>
        get() = emptyList()

    open val drawable: Int
        get() = R.drawable.ic_headphones

    override val model: String
        get() = Constants.UNKNOWN

    override val isSingle: Boolean
        get() = true

    override val lowBattThreshold: Int
        get() = // Most AirPods have same Low Battery Threshold to 20
        20

    override val manufacturer: String
        get() = Constants.UNKNOWN

    open val capabilities: Set<Constants.Capability>
        get() = emptySet()
}
