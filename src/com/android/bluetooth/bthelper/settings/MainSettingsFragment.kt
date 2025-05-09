/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.settings

import android.app.ActionBar
import com.android.bluetooth.bthelper.Constants

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

    private fun handleSwitchBroadcast(sp: SwitchPreferenceCompat, isChecked: Boolean) {
        if (mSelfChange) {
            mSelfChange = false
            return
        }
        sp.setChecked(isChecked)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.main_settings)
        val mActionBar: ActionBar = getActivity().getActionBar()
        mActionBar.setDisplayHomeAsUpEnabled(true)
        mSharedPrefs =
            getContext().getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE)

        mOnePodModePref = findPreference(Constants.KEY_ONEPOD_MODE) as SwitchPreferenceCompat?
        mOnePodModePref.setEnabled(true)
        mOnePodModePref.setOnPreferenceChangeListener(this)
        if (PodsService.isSingleDevice()) {
            getPreferenceScreen().removePreference(mOnePodModePref)
        }

        mAutoPlayPref = findPreference(Constants.KEY_AUTO_PLAY) as SwitchPreferenceCompat?
        mAutoPlayPref.setEnabled(true)
        mAutoPlayPref.setOnPreferenceChangeListener(this)

        mAutoPausePref = findPreference(Constants.KEY_AUTO_PAUSE) as SwitchPreferenceCompat?
        mAutoPausePref.setEnabled(true)
        mAutoPausePref.setOnPreferenceChangeListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View =
            LayoutInflater.from(getContext()).inflate(R.layout.main_settings, container, false)
        (view as ViewGroup).addView(super.onCreateView(inflater, container, savedInstanceState))
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
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
