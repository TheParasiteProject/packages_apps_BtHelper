/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.settings

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

class MainSettingsActivity : CollapsingToolbarBaseActivity() {
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getFragmentManager()
            .beginTransaction()
            .replace(R.id.content_frame, MainSettingsFragment(), TAG)
            .commit()
    }

    companion object {
        private const val TAG = "BtHelper"
    }
}
