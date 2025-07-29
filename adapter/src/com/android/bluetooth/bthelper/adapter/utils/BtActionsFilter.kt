/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.adapter.utils

import com.android.bluetooth.bthelper.adapter.Constants

object BtActionsFilter {
    private val btActions: MutableSet<String> = mutableSetOf()
    @Volatile private var isRegistered: Boolean = false

    @Synchronized
    fun shouldHandleAction(action: String): Boolean {
        return btActions.contains(action)
    }

    @Synchronized
    fun onCreate() {
        if (isRegistered) return
        btActions.add(Constants.ACTION_SET_SLICE)
        isRegistered = true
    }
}
