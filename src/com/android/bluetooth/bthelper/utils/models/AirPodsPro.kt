/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: LibrePods contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPodsPro : RegularPods() {
    override val modelId: Int
        get() = 0x0E20

    override val modelNumbers: List<String>
        get() = listOf("A2084", "A2083")

    override val drawable: Int
        get() = Icons.AirPods_Pro

    override val leftDrawable: Int
        get() = Icons.AirPods_Pro_Left

    override val rightDrawable: Int
        get() = Icons.AirPods_Pro_Right

    override val caseDrawable: Int
        get() = Icons.AirPods_Pro_Case

    override val model: String
        get() = Constants.MODEL_AIRPODS_PRO

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE

    override val capabilities: Set<Constants.Capability>
        get() = setOf(Constants.Capability.LISTENING_MODE)
}
