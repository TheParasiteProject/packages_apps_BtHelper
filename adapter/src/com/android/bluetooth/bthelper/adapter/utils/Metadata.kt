/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.adapter.utils

import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.util.Log
import com.android.bluetooth.bthelper.adapter.Constants.TAG
import java.util.Locale

fun BluetoothDevice.setMetadataValue(key: Int, value: ByteArray, force: Boolean = false): Boolean {
    val isValueAlreadySet = this.getMetadata(key)?.contentEquals(value) == true
    if (force || !isValueAlreadySet) {
        try {
            val ret = this.setMetadata(key, value)
            if (!ret) {
                Log.w(TAG, "Failed to set metadata ${key}")
            }
            return ret
        } catch (e: Exception) {
            Log.w(TAG, "Excepttion while setting metadata ${key}", e)
        }
    }
    return true
}

fun BluetoothDevice.setMetadataUri(key: Int, uri: Uri?, force: Boolean = false): Boolean {
    uri?.let {
        return this.setMetadataValue(key, it.toString().toByteArray(), force)
    }
    return true
}

fun BluetoothDevice.setMetadataString(key: Int, value: String, force: Boolean = false): Boolean {
    return this.setMetadataValue(key, value.toByteArray(), force)
}

fun BluetoothDevice.setMetadataBoolean(key: Int, value: Boolean, force: Boolean = false): Boolean {
    return this.setMetadataString(key, value.toString().uppercase(Locale.getDefault()), force)
}

fun BluetoothDevice.setMetadataInt(key: Int, value: Int, force: Boolean = false): Boolean {
    return this.setMetadataString(key, value.toString(), force)
}
