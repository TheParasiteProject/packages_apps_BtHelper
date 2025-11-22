/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: LibrePods contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPods1(color: Int) : RegularPods(color) {
    override val modelId: Int
        get() = 0x0220

    override val modelNumbers: List<String>
        get() = listOf("A1523", "A1722")

    override val drawable: Int
        get() = Icons.AirPods

    override val leftDrawable: Int
        get() = Icons.AirPods_Left

    override val rightDrawable: Int
        get() = Icons.AirPods_Right

    override val caseDrawable: Int
        get() = Icons.AirPods_Case

    override val model: String
        get() = Constants.MODEL_AIRPODS_GEN1

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE
}
