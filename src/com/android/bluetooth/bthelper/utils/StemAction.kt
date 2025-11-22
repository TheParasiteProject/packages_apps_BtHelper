/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import com.android.bluetooth.bthelper.utils.StemAction.entries

enum class StemAction {
    PLAY_PAUSE,
    PREVIOUS_TRACK,
    NEXT_TRACK,
    CAMERA_SHUTTER,
    DIGITAL_ASSISTANT,
    CYCLE_NOISE_CONTROL_MODES;

    companion object {
        fun fromString(action: String): StemAction? {
            return entries.find { it.name == action }
        }

        val defaultActions: Map<AACPManager.StemPressType, StemAction> =
            mapOf(
                AACPManager.StemPressType.SINGLE_PRESS to PLAY_PAUSE,
                AACPManager.StemPressType.DOUBLE_PRESS to NEXT_TRACK,
                AACPManager.StemPressType.TRIPLE_PRESS to PREVIOUS_TRACK,
                AACPManager.StemPressType.LONG_PRESS to CYCLE_NOISE_CONTROL_MODES,
            )
    }
}
