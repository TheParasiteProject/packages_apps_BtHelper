/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothDevice
import android.net.Uri
import java.util.Locale

fun BluetoothDevice.setMetadataValue(key: Int, value: ByteArray, force: Boolean = false): Boolean {
    if (force || this.getMetadata(key) == null) {
        return this.setMetadata(key, value)
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
