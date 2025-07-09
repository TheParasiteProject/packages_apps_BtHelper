/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.slices

import android.app.slice.Slice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.isLowLatencySupported

class SliceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val TAG: String = "SliceBroadcastReceiver"
    }

    private var action: String? = null
    private var enabled = false
    private var context: Context? = null
    private var mSharedPrefs: SharedPreferences? = null

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

        val extra: Int = intent.getIntExtra(Constants.ACTION_PENDING_INTENT, Constants.EXTRA_NONE)

        when (extra) {
            Constants.EXTRA_LOW_LATENCY_AUDIO_CHANGED -> {
                if (!context.isLowLatencySupported()) return
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false)
                handleSliceChange(Constants.KEY_LOW_LATENCY_AUDIO, enabled)
            }

            else -> {}
        }
    }

    private fun handleSliceChange(key: String?, enabled: Boolean) {
        val ctx = context
        val k = key

        if (k == null || ctx == null) return

        val editor: SharedPreferences.Editor =
            ctx.getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE).edit()
        editor.putBoolean(k, enabled).apply()
    }
}
