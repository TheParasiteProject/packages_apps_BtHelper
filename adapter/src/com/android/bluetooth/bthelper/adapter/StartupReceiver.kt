/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.adapter

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.bluetooth.bthelper.adapter.utils.BtActionsFilter
import com.android.bluetooth.bthelper.adapter.utils.setMetadataString

class StartupReceiver : BroadcastReceiver() {

    init {
        BtActionsFilter.onCreate()
    }

    var macAddress = ""

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        val device: BluetoothDevice =
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                ?: return

        macAddress = device?.address ?: ""

        val action = intent.action
        if (action == null || !BtActionsFilter.shouldHandleAction(action)) return

        onActionReceived(context, intent, device)
    }

    private fun onActionReceived(context: Context, intent: Intent, device: BluetoothDevice) {
        if (intent.action != Constants.ACTION_SET_SLICE) {
            return
        }

        when (intent.getIntExtra(Constants.EXTRA_METADATA_KEY, -255335)) {
            BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI -> {
                device.setMetadataString(
                    BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI,
                    getSliceUri(macAddress),
                )
            }
            Constants.METADATA_FAST_PAIR_CUSTOMIZED_FIELDS -> {
                device.setMetadataString(
                    Constants.METADATA_FAST_PAIR_CUSTOMIZED_FIELDS,
                    getFastPairSliceUri(macAddress),
                )
            }
        }
    }

    companion object {
        const val TAG: String = "StartupReceiver"
    }
}
