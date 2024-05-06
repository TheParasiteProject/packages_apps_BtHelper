/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.settings;

import android.app.ActionBar;
import android.app.slice.Slice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreferenceCompat;

import com.android.bluetooth.bthelper.Constants;
import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.pods.PodsService;

public class MainSettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    private SharedPreferences mSharedPrefs;
    private SwitchPreferenceCompat mOnePodModePref;
    private SwitchPreferenceCompat mAutoPlayPref;
    private SwitchPreferenceCompat mAutoPausePref;

    private boolean mSelfChange = false;

    private BroadcastReceiver stateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        if (intent == null || context == null) return;
                        final String action = intent.getAction();
                        if (action == null) return;

                        final int extra =
                                intent.getIntExtra(
                                        Constants.ACTION_PENDING_INTENT, Constants.EXTRA_NONE);

                        switch (extra) {
                            case Constants.EXTRA_ONEPOD_CHANGED:
                                handleSwitchBroadcast(
                                        mOnePodModePref,
                                        intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false));
                                return;
                            case Constants.EXTRA_AUTO_PLAY_CHANGED:
                                handleSwitchBroadcast(
                                        mAutoPlayPref,
                                        intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false));
                                return;
                            case Constants.EXTRA_AUTO_PAUSE_CHANGED:
                                handleSwitchBroadcast(
                                        mAutoPausePref,
                                        intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false));
                                return;
                        }
                    } catch (NullPointerException e) {
                    }
                }
            };

    private void handleSwitchBroadcast(SwitchPreferenceCompat sp, boolean isChecked) {
        if (mSelfChange) {
            mSelfChange = false;
            return;
        }
        sp.setChecked(isChecked);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.main_settings);
        final ActionBar mActionBar = getActivity().getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mSharedPrefs =
                getContext()
                        .getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE);

        mOnePodModePref = (SwitchPreferenceCompat) findPreference(Constants.KEY_ONEPOD_MODE);
        mOnePodModePref.setEnabled(true);
        mOnePodModePref.setOnPreferenceChangeListener(this);
        if (PodsService.isSingleDevice()) {
            getPreferenceScreen().removePreference(mOnePodModePref);
        }

        mAutoPlayPref = (SwitchPreferenceCompat) findPreference(Constants.KEY_AUTO_PLAY);
        mAutoPlayPref.setEnabled(true);
        mAutoPlayPref.setOnPreferenceChangeListener(this);

        mAutoPausePref = (SwitchPreferenceCompat) findPreference(Constants.KEY_AUTO_PAUSE);
        mAutoPausePref.setEnabled(true);
        mAutoPausePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view =
                LayoutInflater.from(getContext()).inflate(R.layout.main_settings, container, false);
        ((ViewGroup) view).addView(super.onCreateView(inflater, container, savedInstanceState));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case Constants.KEY_ONEPOD_MODE:
                sendSwitchBroadcast(
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_ONEPOD_CHANGED,
                        (boolean) newValue);
                break;
            case Constants.KEY_AUTO_PLAY:
                sendSwitchBroadcast(
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_AUTO_PLAY_CHANGED,
                        (boolean) newValue);
                break;
            case Constants.KEY_AUTO_PAUSE:
                sendSwitchBroadcast(
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_AUTO_PAUSE_CHANGED,
                        (boolean) newValue);
                break;
            default:
                break;
        }
        return true;
    }

    private void sendSwitchBroadcast(String action, int extra, boolean isChecked) {
        mSelfChange = true;
        final Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        intent.putExtra(Slice.EXTRA_TOGGLE_STATE, extra);
        intent.putExtra(Slice.EXTRA_TOGGLE_STATE, isChecked);
        getContext().sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }
}
