/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods.models

import com.android.bluetooth.bthelper.Constants.Icons
import com.android.bluetooth.bthelper.pods.Pod

class AirPodsMax(color: String, singlePod: Pod) : SinglePods(color, singlePod) {
    override val drawable: Int
        get() {
            return when (color) {
                "03" -> Icons.AirPods_Max_SkyBlue
                "04" -> Icons.AirPods_Max_Pink
                "06" -> Icons.AirPods_Max_Silver
                "09" -> Icons.AirPods_Max_SpaceGray
                "10" -> Icons.AirPods_Max_Green // Guess
                else -> Icons.AirPods_Max_Silver
            }
        }

    override val model: String
        get() = Constants.MODEL_AIRPODS_MAX

    override val menufacturer: String
        get() = Constants.MANUFACTURER_APPLE
}
