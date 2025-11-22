/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothSocket
import com.android.bluetooth.bthelper.utils.models.IPods

object BluetoothConnectionManager {
    var currentModel: IPods? = null
        @Synchronized get
        @Synchronized set

    var currentSocket: BluetoothSocket? = null
        @Synchronized get
        @Synchronized set

    var isConnected = false
        @Synchronized get
        @Synchronized set

    var currentAttSocket: BluetoothSocket? = null
        @Synchronized get
        @Synchronized set

    var isAttConnected = false
        @Synchronized get
        @Synchronized set
}
