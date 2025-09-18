/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import com.android.bluetooth.bthelper.pods.PodsService
import com.android.bluetooth.bthelper.utils.BtActionsFilter

class StartupReceiver : BroadcastReceiver() {

    init {
        BtActionsFilter.onCreate()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        val action = intent.action
        if (action == Intent.ACTION_SHUTDOWN) {
            stopPodsService(context)
            return
        }
        if (action == null || !BtActionsFilter.shouldHandleAction(action)) return

        val device: BluetoothDevice =
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                ?: return

        onPodsDetected(context, intent, device)
    }

    private fun isPods(device: BluetoothDevice?): Boolean {
        return device?.uuids?.contains(Constants.PODS_UUID) ?: false
    }

    private fun onPodsDetected(context: Context, intent: Intent, device: BluetoothDevice) {
        if (!isPods(device)) return

        when (intent.action) {
            BluetoothDevice.ACTION_NAME_CHANGED -> {
                maybeRenamePods(context, intent, device)
            }
            else -> {
                btProfileChanges(context, intent, device)
            }
        }
    }

    private fun btProfileChanges(context: Context, intent: Intent, device: BluetoothDevice) {
        var state: Int = BluetoothAdapter.ERROR
        try {
            state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile state", e)
            return
        }

        when (state) {
            BluetoothProfile.STATE_CONNECTED -> maybeStartPodsService(context, device)
            BluetoothProfile.STATE_DISCONNECTED -> maybeStopPodsService(context, device)
        }
    }

    private fun maybeStartPodsService(context: Context, device: BluetoothDevice) {
        val serviceIntent =
            Intent(context, PodsService::class.java).apply {
                this.action = Constants.ACTION_CONNECTED
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            }
        context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
    }

    private fun maybeStopPodsService(context: Context, device: BluetoothDevice) {
        val serviceIntent =
            Intent(context, PodsService::class.java).apply {
                this.action = Constants.ACTION_DISCONNECTED
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            }
        context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
    }

    private fun stopPodsService(context: Context) {
        context.stopServiceAsUser(Intent(context, PodsService::class.java), UserHandle.CURRENT)
    }

    private fun maybeRenamePods(context: Context, intent: Intent, device: BluetoothDevice) {
        val name: String? = intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: ""
        val serviceIntent =
            Intent(context, PodsService::class.java).apply {
                this.action = Constants.ACTION_NAME_CHANGED
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                putExtra(BluetoothDevice.EXTRA_NAME, name)
            }
        context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
    }

    companion object {
        const val TAG: String = "StartupReceiver"
    }
}
