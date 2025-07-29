/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.adapter.settings

import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import com.android.bluetooth.bthelper.adapter.Constants
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class MainSettingsActivity : CollapsingToolbarBaseActivity() {
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent =
            Intent().apply {
                setClassName(
                    Constants.AUTHORITY_BTHELPER,
                    Constants.ACTIVITY_BTHELPER_MAIN_SETTINGS,
                )
                action = Intent.ACTION_MAIN
            }
        startActivityAsUser(intent, UserHandle.CURRENT)
        finish()
    }

    companion object {
        private const val TAG = "BtHelper"
    }
}
