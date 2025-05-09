/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.os.UserHandle
import com.android.bluetooth.bthelper.pods.PodsService

class StartupReceiver : BroadcastReceiver() {

    val PodsUUIDS: MutableSet<ParcelUuid> = HashSet<ParcelUuid>()

    init {
        PodsUUIDS.add(ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"))
        PodsUUIDS.add(ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74"))
    }

    private val ACTION_AVRCP_CONNECTION_STATE_CHANGED =
        "android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED"

    val btActions: MutableSet<String> = HashSet<String>()

    init {
        btActions.add(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED)
        btActions.add(ACTION_AVRCP_CONNECTION_STATE_CHANGED)
        btActions.add(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED)
        btActions.add(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        btActions.add(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
        btActions.add(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        btActions.add(BluetoothAdapter.ACTION_STATE_CHANGED)
        btActions.add(BluetoothDevice.ACTION_ACL_CONNECTED)
        btActions.add(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        btActions.add(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        btActions.add(BluetoothDevice.ACTION_NAME_CHANGED)
        btActions.add(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        btActions.add(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return
        if (btActions.contains(intent.action!!)) {
            try {
                val state: Int =
                    intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR)
                val device: BluetoothDevice =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
                btProfileChanges(context, state, device)
            } catch (e: NullPointerException) {
                return
            }
        }
    }

    fun isPods(device: BluetoothDevice): Boolean {
        for (uuid in device.getUuids()) {
            if (PodsUUIDS.contains(uuid)) {
                return true
            }
        }
        return false
    }

    private fun startPodsService(context: Context, device: BluetoothDevice) {
        if (!isPods(device)) {
            return
        }
        val intent: Intent = Intent(context, PodsService::class.java)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        context.startServiceAsUser(intent, UserHandle.CURRENT)
    }

    private fun stopPodsService(context: Context) {
        context.stopServiceAsUser(Intent(context, PodsService::class.java), UserHandle.CURRENT)
    }

    private fun btProfileChanges(context: Context, state: Int, device: BluetoothDevice) {
        when (state) {
            BluetoothProfile.STATE_CONNECTED -> startPodsService(context, device)
            BluetoothProfile.STATE_DISCONNECTING,
            BluetoothProfile.STATE_DISCONNECTED -> stopPodsService(context)
        }
    }
}
