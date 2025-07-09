/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothSocket

object BluetoothConnectionManager {
    var currentSocket: BluetoothSocket? = null
        @Synchronized get
        @Synchronized set

    var isConnected = false
        @Synchronized get
        @Synchronized set
}
