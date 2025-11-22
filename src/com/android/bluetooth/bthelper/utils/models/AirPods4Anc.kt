/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: LibrePods contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPods4Anc(color: Int) : RegularPods(color) {
    override val modelId: Int
        get() = 0x1B20

    override val modelNumbers: List<String>
        get() = listOf("A3056", "A3055", "A3057")

    override val drawable: Int
        get() = Icons.AirPods_Gen4

    override val leftDrawable: Int
        get() = Icons.AirPods_Gen4_Left

    override val rightDrawable: Int
        get() = Icons.AirPods_Gen4_Right

    override val caseDrawable: Int
        get() = Icons.AirPods_Gen4_Case

    override val model: String
        get() = Constants.MODEL_AIRPODS_GEN4_ANC

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE

    override val capabilities: Set<Constants.Capability>
        get() =
            setOf(
                Constants.Capability.LISTENING_MODE,
                Constants.Capability.CONVERSATION_AWARENESS,
                Constants.Capability.HEAD_GESTURES,
                Constants.Capability.ADAPTIVE_AUDIO,
                Constants.Capability.SLEEP_DETECTION,
                Constants.Capability.ADAPTIVE_VOLUME,
            )
}
