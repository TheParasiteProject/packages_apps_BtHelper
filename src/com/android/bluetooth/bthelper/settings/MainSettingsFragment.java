package com.android.bluetooth.bthelper.settings;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.pods.PodsService;

public class MainSettingsFragment extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private SharedPreferences mSharedPrefs;
    private SwitchPreference mLowLatencyAudioSwitchPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.main_settings);
        final ActionBar mActionBar = getActivity().getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        mLowLatencyAudioSwitchPref = (SwitchPreference) findPreference(Constants.KEY_LOW_LATENCY_AUDIO);
        mLowLatencyAudioSwitchPref.setEnabled(true);
        mLowLatencyAudioSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.main_settings,
                container, false);
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
            case Constants.KEY_LOW_LATENCY_AUDIO:
                PodsService.setLowLatencyAudio(getContext());
                break;
            default:
                break;
        }
        return true;
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
