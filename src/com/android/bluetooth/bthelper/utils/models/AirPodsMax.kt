/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPodsMax(color: Int) : SinglePods(color) {
    override val drawable: Int
        get() {
            return when (color) {
                0x03 -> Icons.AirPods_Max_SkyBlue
                0x04 -> Icons.AirPods_Max_Pink
                0x06 -> Icons.AirPods_Max_Silver
                0x09 -> Icons.AirPods_Max_SpaceGray
                0x10 -> Icons.AirPods_Max_Green // Guess
                else -> Icons.AirPods_Max_Silver
            }
        }

    override val model: String
        get() = Constants.MODEL_AIRPODS_MAX

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE
}
