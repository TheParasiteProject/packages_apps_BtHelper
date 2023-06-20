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
import android.os.Build;

import com.android.bluetooth.bthelper.pods.PodsService;

import java.util.Objects;

/**
 * A simple startup class that starts the service when the device is booted, or after an update
 */
public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive (Context context, Intent intent) {
        switch (Objects.requireNonNull(intent.getAction())) {
            case android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED:
                startPodsService(context);
                break;
        }
    }

    public static void startPodsService (Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(new Intent(context, PodsService.class));
        else
            context.startService(new Intent(context, PodsService.class));
    }

    public static void restartPodsService (Context context) {
        context.stopService(new Intent(context, PodsService.class));
        try {
            Thread.sleep(500);
        } catch (Throwable ignored) {
        }
        startPodsService(context);
    }

}
