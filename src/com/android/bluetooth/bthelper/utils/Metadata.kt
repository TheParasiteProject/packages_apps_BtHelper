/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothDevice
import android.net.Uri
import java.util.Locale

fun BluetoothDevice.setMetadataValue(key: Int, value: ByteArray): Boolean {
    if (this.getMetadata(key) == null) {
        return this.setMetadata(key, value)
    }
    return true
}

fun BluetoothDevice.setMetadataUri(key: Int, uri: Uri?): Boolean {
    uri?.let {
        return this.setMetadataValue(key, it.toString().toByteArray())
    }
    return true
}

fun BluetoothDevice.setMetadataString(key: Int, value: String): Boolean {
    return this.setMetadataValue(key, value.toByteArray())
}

fun BluetoothDevice.setMetadataBoolean(key: Int, value: Boolean): Boolean {
    return this.setMetadataString(key, value.toString().uppercase(Locale.getDefault()))
}

fun BluetoothDevice.setMetadataInt(key: Int, value: Int): Boolean {
    return this.setMetadataString(key, value.toString())
}
