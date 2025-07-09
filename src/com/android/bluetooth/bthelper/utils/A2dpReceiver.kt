/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper

interface PodsA2dpListener {
    fun onPodsA2dpDetected(device: BluetoothDevice?)
}

class A2dpReceiver(val listener: PodsA2dpListener) : BroadcastReceiver() {
    private val a2dpFilter = IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
    private val handler = Handler(Looper.getMainLooper())
    private val TIMEOUT_MILLIS = 5000L

    @Volatile private var _device: BluetoothDevice? = null
    @Volatile private var _context: Context? = null
    @Volatile private var isRegistered: Boolean = false

    private val timeoutRunnable = Runnable {
        if (isRegistered) {
            onDestroy()
        }
    }

    @Synchronized
    fun onCreate(context: Context, device: BluetoothDevice) {
        if (isRegistered) {
            onDestroy()
        }
        handler.removeCallbacksAndMessages(null)
        _context = context
        context.registerReceiver(this, a2dpFilter, Context.RECEIVER_EXPORTED)
        _device = device
        handler.postDelayed(timeoutRunnable, TIMEOUT_MILLIS)
        isRegistered = true
    }

    @Synchronized
    override fun onReceive(context: Context?, intent: Intent?) {
        if (!isRegistered) return

        if (intent == null) return
        if (intent.action != BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) return
        val state =
            intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
        val previousState =
            intent.getIntExtra(
                BluetoothProfile.EXTRA_PREVIOUS_STATE,
                BluetoothProfile.STATE_DISCONNECTED,
            )
        val device: BluetoothDevice? =
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)

        if (
            state == BluetoothProfile.STATE_CONNECTED &&
                previousState != BluetoothProfile.STATE_CONNECTED &&
                device?.equals(_device) == true
        ) {
            listener.onPodsA2dpDetected(device)
            onDestroy()
        }
    }

    @Synchronized
    fun onDestroy() {
        if (!isRegistered) {
            return
        }

        _context?.unregisterReceiver(this)
        _context = null
        _device = null
        handler.removeCallbacksAndMessages(null)
    }
}
