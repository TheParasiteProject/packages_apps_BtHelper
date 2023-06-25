package com.android.bluetooth.bthelper.settings;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.R;

import com.android.bluetooth.bthelper.pods.PodsService;

public class MainSettingsActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG = "BtHelper";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(R.id.content_frame,
                new MainSettingsFragment(), TAG).commit();
    }
}
