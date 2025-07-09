/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import com.android.bluetooth.bthelper.Constants

object BtActionsFilter {
    private val btActions: MutableSet<String> = mutableSetOf()
    @Volatile private var isRegistered: Boolean = false

    @Synchronized
    fun shouldHandleAction(action: String): Boolean {
        return btActions.contains(action)
    }

    @Synchronized
    fun onCreate() {
        if (isRegistered) return
        btActions.add(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED)
        btActions.add(Constants.ACTION_AVRCP_CONNECTION_STATE_CHANGED)
        btActions.add(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED)
        btActions.add(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
        btActions.add(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        btActions.add(BluetoothAdapter.ACTION_STATE_CHANGED)
        btActions.add(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        btActions.add(BluetoothDevice.ACTION_NAME_CHANGED)
        btActions.add(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        btActions.add(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
        isRegistered = true
    }
}
