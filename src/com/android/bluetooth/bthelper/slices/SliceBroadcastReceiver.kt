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
import com.android.bluetooth.bthelper.Constants

class SliceBroadcastReceiver : BroadcastReceiver() {
    private var action: String? = null
    private var enabled = false
    private var context: Context? = null
    private var mSharedPrefs: SharedPreferences? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (intent == null || context == null) return
            this.context = context
            action = intent.getAction()
            if (action == null) return
        } catch (e: NullPointerException) {
            return
        }

        val extra: Int = intent.getIntExtra(Constants.ACTION_PENDING_INTENT, Constants.EXTRA_NONE)

        when (extra) {
            Constants.EXTRA_ONEPOD_CHANGED -> {
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false)
                handleSliceChange(Constants.KEY_ONEPOD_MODE, enabled)
                return
            }

            Constants.EXTRA_AUTO_PLAY_CHANGED -> {
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false)
                handleSliceChange(Constants.KEY_AUTO_PLAY, enabled)
                return
            }

            Constants.EXTRA_AUTO_PAUSE_CHANGED -> {
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false)
                handleSliceChange(Constants.KEY_AUTO_PAUSE, enabled)
                return
            }

            else -> return
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
