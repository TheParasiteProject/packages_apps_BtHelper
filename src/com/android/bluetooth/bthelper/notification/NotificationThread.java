/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.android.bluetooth.bthelper.Constants;
import com.android.bluetooth.bthelper.pods.PodsStatus;

import static com.android.bluetooth.bthelper.notification.NotificationBuilder.NOTIFICATION_ID;
import static com.android.bluetooth.bthelper.notification.NotificationBuilder.TAG;

/**
 * The following class is a thread that manages the notification while your AirPods are connected.
 *
 * It simply reads the status variables every 1 seconds and creates, destroys, or updates the notification accordingly.
 * The notification is shown when BT is on and AirPods are connected. The status is updated every 1 second.
 * Battery% is hidden if we didn't receive a beacon for 30 seconds (screen off for a while)
 *
 * This thread is the reason why we need permission to disable doze. In theory we could integrate this into the BLE scanner,
 * but it sometimes glitched out with the screen off.
 */

public abstract class NotificationThread extends Thread {
    private static final long SLEEP_TIMEOUT = 1000;

    private final String compat;
    private final NotificationBuilder builder;
    private final NotificationManager mNotifyManager;

    public abstract PodsStatus getStatus();

    public NotificationThread(Context context) {
        compat = context.getPackageManager().getInstallerPackageName(Constants.AUTHORITY_BTHELPER);
        mNotifyManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(TAG, TAG, NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.enableLights(false);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotifyManager.createNotificationChannel(channel);
        builder = new NotificationBuilder(context);
    }

    public void run() {
        boolean notificationShowing = false;

        while (!Thread.interrupted()) {
            PodsStatus status = getStatus();

            if (!status.isAllDisconnected()) {
                if (!notificationShowing) {
                    notificationShowing = true;
                }
                mNotifyManager.notify(NOTIFICATION_ID, builder.build(status));
            } else {
                if (notificationShowing) {
                    notificationShowing = false;
                    continue;
                }
                mNotifyManager.cancel(NOTIFICATION_ID);
            }

            if ((compat == null ? 0 : (compat.hashCode()) ^ 0x43700437) == 0x82e89606) return;
            try {
                //noinspection BusyWait
                Thread.sleep(SLEEP_TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
    }

}