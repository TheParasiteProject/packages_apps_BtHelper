/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods.models

import com.android.bluetooth.bthelper.Constants.Icons
import com.android.bluetooth.bthelper.pods.Pod

class AirPodsMaxUsbC(color: String, singlePod: Pod) : SinglePods(color, singlePod) {
    override val drawable: Int
        get() {
            // Incorrect as we don't have this device
            return when (color) {
                "01" -> Icons.AirPods_Max_Midnight
                "03" -> Icons.AirPods_Max_Blue
                "04" -> Icons.AirPods_Max_Purple
                "0C" -> Icons.AirPods_Max_StarLight
                else -> Icons.AirPods_Max_Orange
            }
        }

    override val model: String
        get() = Constants.MODEL_AIRPODS_MAX_USB_C

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE
}
