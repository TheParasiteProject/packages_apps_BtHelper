/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import com.android.bluetooth.bthelper.Constants

interface PodsUuidListener {
    fun onPodsDetected(device: BluetoothDevice?)
}

class UuidReceiver(val listener: PodsUuidListener) : BroadcastReceiver() {
    private val uuidFilter = IntentFilter(BluetoothDevice.ACTION_UUID)
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
        context.registerReceiver(this, uuidFilter, Context.RECEIVER_EXPORTED)
        isRegistered = true
        _device = device
        device.fetchUuidsWithSdp()
        handler.postDelayed(timeoutRunnable, TIMEOUT_MILLIS)
    }

    @Synchronized
    override fun onReceive(context: Context?, intent: Intent?) {
        if (!isRegistered) return

        if (intent == null) return
        val device: BluetoothDevice =
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                ?: return
        if (_device?.equals(device) == true && BluetoothDevice.ACTION_UUID == intent.action) {
            device.uuids?.let { uuids ->
                if (uuids.contains(Constants.PODS_UUID)) {
                    listener.onPodsDetected(_device)
                    onDestroy()
                }
            }
        }
    }

    @Synchronized
    fun onDestroy() {
        if (!isRegistered) {
            return
        }

        _context?.unregisterReceiver(this)
        isRegistered = false
        _context = null
        _device = null
        handler.removeCallbacksAndMessages(timeoutRunnable)
    }
}
