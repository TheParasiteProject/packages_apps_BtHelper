/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.adapter.slices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SliceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val TAG: String = "SliceBroadcastReceiver"
    }

    private var action: String? = null
    private var enabled = false
    private var context: Context? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (intent == null || context == null) return
            this.context = context
            action = intent.action
            if (action == null) return
        } catch (e: NullPointerException) {
            Log.e(TAG, "Error in onReceive", e)
            return
        }
    }
}
