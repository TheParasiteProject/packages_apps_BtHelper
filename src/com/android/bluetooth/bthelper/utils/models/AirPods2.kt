/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: LibrePods contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPods2 : RegularPods() {
    override val modelId: Int
        get() = 0x0F20

    override val modelNumbers: List<String>
        get() = listOf("A2032", "A2031")

    override val drawable: Int
        get() = Icons.AirPods

    override val leftDrawable: Int
        get() = Icons.AirPods_Left

    override val rightDrawable: Int
        get() = Icons.AirPods_Right

    override val caseDrawable: Int
        get() = Icons.AirPods_Case

    override val model: String
        get() = Constants.MODEL_AIRPODS_GEN2

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE
}
