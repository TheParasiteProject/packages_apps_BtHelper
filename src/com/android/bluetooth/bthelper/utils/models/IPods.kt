/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils.models

open class IPods {
    open var color: Int = 0
    open val modelId: Int = 0
    open val model: String = Constants.UNKNOWN
    open val isSingle: Boolean = false
    open val lowBattThreshold: Int = -1
    open val manufacturer: String = Constants.UNKNOWN
}
