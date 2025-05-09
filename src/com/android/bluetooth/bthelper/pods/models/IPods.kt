/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods.models

interface IPods {
    val model: String

    val isSingle: Boolean

    val isDisconnected: Boolean

    val lowBattThreshold: Int

    val menufacturer: String
}
