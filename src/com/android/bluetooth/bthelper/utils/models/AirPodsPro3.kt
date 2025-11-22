/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: LibrePods contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPodsPro3(color: Int) : RegularPods(color) {
    override val modelId: Int
        get() = 0x2720

    override val modelNumber: List<String>
        get() = listOf("A3063", "A3064", "A3065")

    override val drawable: Int
        get() = Icons.AirPods_Pro

    override val leftDrawable: Int
        get() = Icons.AirPods_Pro_Left

    override val rightDrawable: Int
        get() = Icons.AirPods_Pro_Right

    override val caseDrawable: Int
        get() = Icons.AirPods_Pro_Case

    override val model: String
        get() = Constants.MODEL_AIRPODS_PRO_3

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE

    override val capabilities: Set<Constants.Capability>
        get() =
            setOf(
                Constants.Capability.LISTENING_MODE,
                Constants.Capability.CONVERSATION_AWARENESS,
                Constants.Capability.HEAD_GESTURES,
                Constants.Capability.STEM_CONFIG,
                Constants.Capability.LOUD_SOUND_REDUCTION,
                Constants.Capability.PPE,
                Constants.Capability.SLEEP_DETECTION,
                Constants.Capability.HEARING_AID,
                Constants.Capability.ADAPTIVE_AUDIO,
                Constants.Capability.ADAPTIVE_VOLUME,
                Constants.Capability.SWIPE_FOR_VOLUME,
                Constants.Capability.HRM,
            )
}
