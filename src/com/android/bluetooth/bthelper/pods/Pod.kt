/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods

import android.bluetooth.BluetoothDevice

class Pod(val status: Int, val isCharging: Boolean, val isInEar: Boolean) {
    fun parseStatus(arg: Boolean): Int {
        if (arg) {
            return if (
                status == MAX_CONNECTED_STATUS || (status < MAX_CONNECTED_STATUS && status > 0)
            )
                status - 1
            else BluetoothDevice.BATTERY_LEVEL_UNKNOWN
        }

        return if (status == MAX_CONNECTED_STATUS) 100
        else
            (if (status < MAX_CONNECTED_STATUS) (status * 10)
            else BluetoothDevice.BATTERY_LEVEL_UNKNOWN)
    }

    val isConnected: Boolean
        get() = status <= MAX_CONNECTED_STATUS

    val isDisconnected: Boolean
        get() = status == DISCONNECTED_STATUS

    val isLowBattery: Boolean
        get() = status <= LOW_BATTERY_STATUS

    companion object {
        const val DISCONNECTED_STATUS: Int = 15
        const val MAX_CONNECTED_STATUS: Int = 10
        const val LOW_BATTERY_STATUS: Int = 1
    }
}
