/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPodsMaxUsbC(color: Int) : SinglePods(color) {
    override val drawable: Int
        get() {
            // Incorrect as we don't have this device
            return when (color) {
                0x01 -> Icons.AirPods_Max_Midnight
                0x03 -> Icons.AirPods_Max_Blue
                0x04 -> Icons.AirPods_Max_Purple
                0x0C -> Icons.AirPods_Max_StarLight
                else -> Icons.AirPods_Max_Orange
            }
        }

    override val model: String
        get() = Constants.MODEL_AIRPODS_MAX_USB_C

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE
}
