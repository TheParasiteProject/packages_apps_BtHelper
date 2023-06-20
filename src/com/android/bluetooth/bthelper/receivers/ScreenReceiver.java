/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public abstract class ScreenReceiver extends BroadcastReceiver {

    public abstract void onStart ();

    public abstract void onStop ();

    public static IntentFilter buildFilter () {
        IntentFilter screenIntentFilter = new IntentFilter();
        screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        return screenIntentFilter;
    }

    @Override
    public void onReceive (Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_OFF: onStop();
                break;
            case Intent.ACTION_SCREEN_ON: onStart();
                break;
        }
    }

}