/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.R
import com.android.bluetooth.bthelper.isLowLatencySupported

class MainSettingsFragment : PreferenceFragmentCompat(), OnPreferenceChangeListener {
    companion object {
        const val TAG: String = "MainSettingsFragment"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_settings, rootKey)

        findPreference<SwitchPreferenceCompat>(Constants.KEY_AUTOMATIC_EAR_DETECTION)?.apply {
            isEnabled = true
            onPreferenceChangeListener = this@MainSettingsFragment
        }

        findPreference<SwitchPreferenceCompat>(Constants.KEY_CONVERSATIONAL_AWARENESS_PAUSE_MUSIC)
            ?.apply {
                isEnabled = true
                onPreferenceChangeListener = this@MainSettingsFragment
            }

        findPreference<SwitchPreferenceCompat>(
                Constants.KEY_RELATIVE_CONVERSATIONAL_AWARENESS_VOLUME
            )
            ?.apply {
                isEnabled = true
                onPreferenceChangeListener = this@MainSettingsFragment
            }

        findPreference<SwitchPreferenceCompat>(Constants.KEY_HEAD_GESTURES)?.apply {
            isEnabled = true
            onPreferenceChangeListener = this@MainSettingsFragment
        }

        findPreference<SwitchPreferenceCompat>(Constants.KEY_DISCONNECT_WHEN_NOT_WEARING)?.apply {
            isEnabled = true
            onPreferenceChangeListener = this@MainSettingsFragment
        }

        findPreference<SwitchPreferenceCompat>(Constants.KEY_LOW_LATENCY_AUDIO)?.apply {
            if (!requireContext().isLowLatencySupported()) {
                preferenceScreen.removePreference(this)
            } else {
                isEnabled = true
                onPreferenceChangeListener = this@MainSettingsFragment
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.getKey()) {
            else -> {}
        }
        return true
    }
}
