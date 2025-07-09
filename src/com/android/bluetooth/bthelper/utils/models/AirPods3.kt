/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPods3(color: Int) : RegularPods(color) {
    override val drawable: Int
        get() = Icons.AirPods_Gen3

    override val leftDrawable: Int
        get() = Icons.AirPods_Gen3_Left

    override val rightDrawable: Int
        get() = Icons.AirPods_Gen3_Right

    override val caseDrawable: Int
        get() = Icons.AirPods_Gen3_Case

    override val model: String
        get() = Constants.MODEL_AIRPODS_GEN3

    override val manufacturer: String
        get() = Constants.MANUFACTURER_APPLE
}
