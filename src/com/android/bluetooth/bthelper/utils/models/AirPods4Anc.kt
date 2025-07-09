/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

import com.android.bluetooth.bthelper.Constants.Icons

class AirPods4Anc(color: Int) : RegularPods(color) {
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
}
