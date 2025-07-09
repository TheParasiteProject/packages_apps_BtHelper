/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragment
import androidx.preference.SwitchPreferenceCompat
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.R
import com.android.bluetooth.bthelper.isLowLatencySupported

class MainSettingsFragment : PreferenceFragment(), OnPreferenceChangeListener {
    companion object {
        const val TAG: String = "MainSettingsFragment"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.main_settings)
        activity?.actionBar?.apply { setDisplayHomeAsUpEnabled(true) }

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
            if (!context.isLowLatencySupported()) {
                preferenceScreen.removePreference(this)
            } else {
                isEnabled = true
                onPreferenceChangeListener = this@MainSettingsFragment
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View =
            LayoutInflater.from(getContext()).inflate(R.layout.main_settings, container, false)
        (view as ViewGroup).addView(super.onCreateView(inflater, container, savedInstanceState))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() === android.R.id.home) {
            getActivity().onBackPressed()
            return true
        }
        return false
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.getKey()) {
            else -> {}
        }
        return true
    }
}
