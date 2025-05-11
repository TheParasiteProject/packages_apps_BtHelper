/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.settings

import android.app.slice.Slice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.UserHandle
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
import com.android.bluetooth.bthelper.pods.PodsService

class MainSettingsFragment : PreferenceFragment(), OnPreferenceChangeListener {
    private var mSharedPrefs: SharedPreferences? = null
    private var mOnePodModePref: SwitchPreferenceCompat? = null
    private var mAutoPlayPref: SwitchPreferenceCompat? = null
    private var mAutoPausePref: SwitchPreferenceCompat? = null

    private var mSelfChange = false

    private val stateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    if (intent == null || context == null) return
                    val action: String = intent.getAction() ?: return

                    val extra: Int =
                        intent.getIntExtra(Constants.ACTION_PENDING_INTENT, Constants.EXTRA_NONE)

                    when (extra) {
                        Constants.EXTRA_ONEPOD_CHANGED -> {
                            handleSwitchBroadcast(
                                mOnePodModePref,
                                intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false),
                            )
                            return
                        }

                        Constants.EXTRA_AUTO_PLAY_CHANGED -> {
                            handleSwitchBroadcast(
                                mAutoPlayPref,
                                intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false),
                            )
                            return
                        }

                        Constants.EXTRA_AUTO_PAUSE_CHANGED -> {
                            handleSwitchBroadcast(
                                mAutoPausePref,
                                intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false),
                            )
                            return
                        }
                    }
                } catch (e: NullPointerException) {}
            }
        }

    private fun handleSwitchBroadcast(sp: SwitchPreferenceCompat?, isChecked: Boolean) {
        if (mSelfChange) {
            mSelfChange = false
            return
        }
        sp?.setChecked(isChecked)
    }

    // Check whether current device is single model (e.g. AirPods Max)
    private fun isSingleDevice(): Boolean {
        return mSharedPrefs?.getBoolean(Constants.KEY_SINGLE_DEVICE, false) ?: false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.main_settings)
        getActivity().getActionBar()?.apply { setDisplayHomeAsUpEnabled(true) }
        mSharedPrefs =
            getContext().getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE)

        mOnePodModePref =
            findPreference<SwitchPreferenceCompat>(Constants.KEY_ONEPOD_MODE)?.apply {
                setEnabled(true)
                setOnPreferenceChangeListener(this@MainSettingsFragment)
                if (isSingleDevice()) {
                    getPreferenceScreen().removePreference(this)
                }
            }

        mAutoPlayPref =
            findPreference<SwitchPreferenceCompat>(Constants.KEY_AUTO_PLAY)?.apply {
                setEnabled(true)
                setOnPreferenceChangeListener(this@MainSettingsFragment)
            }

        mAutoPausePref =
            findPreference<SwitchPreferenceCompat>(Constants.KEY_AUTO_PAUSE)?.apply {
                setEnabled(true)
                setOnPreferenceChangeListener(this@MainSettingsFragment)
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

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.getKey()) {
            Constants.KEY_ONEPOD_MODE ->
                sendSwitchBroadcast(
                    Constants.ACTION_PENDING_INTENT,
                    Constants.EXTRA_ONEPOD_CHANGED,
                    newValue as Boolean,
                )

            Constants.KEY_AUTO_PLAY ->
                sendSwitchBroadcast(
                    Constants.ACTION_PENDING_INTENT,
                    Constants.EXTRA_AUTO_PLAY_CHANGED,
                    newValue as Boolean,
                )

            Constants.KEY_AUTO_PAUSE ->
                sendSwitchBroadcast(
                    Constants.ACTION_PENDING_INTENT,
                    Constants.EXTRA_AUTO_PAUSE_CHANGED,
                    newValue as Boolean,
                )

            else -> {}
        }
        return true
    }

    private fun sendSwitchBroadcast(action: String, extra: Int, isChecked: Boolean) {
        mSelfChange = true
        val intent: Intent = Intent(action)
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
        intent.putExtra(Slice.EXTRA_TOGGLE_STATE, extra)
        intent.putExtra(Slice.EXTRA_TOGGLE_STATE, isChecked)
        getContext().sendBroadcastAsUser(intent, UserHandle.CURRENT)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() === android.R.id.home) {
            getActivity().onBackPressed()
            return true
        }
        return false
    }
}
