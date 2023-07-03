/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.slices;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.settings.MainSettingsActivity;
import com.android.bluetooth.bthelper.slices.SliceBroadcastReceiver;

public class SliceCreator {
    private final String title;
    private final String summary;
    private final boolean enabled;
    private final String action;
    private final Context context;

    public SliceCreator (String title, String summary, 
            boolean enabled, String action, Context context) {
        this.title = title;
        this.summary = summary;
        this.enabled = enabled;
        this.action = action;
        this.context = context;
    }

    protected ListBuilder.RowBuilder getSettingRow (Uri sliceUri) {
        ListBuilder.RowBuilder settingRow = new ListBuilder.RowBuilder(sliceUri);
        settingRow.setTitle(title);
        if (summary != null) {
            settingRow.setSubtitle(summary);
        }
        settingRow.addEndItem(SliceAction.createToggle(
                getBroadcastIntent(action),
                null /* actionTitle */, enabled));
        settingRow.setPrimaryAction(getMainSettingsActivity(context, sliceUri));

        return settingRow;
    }

    private PendingIntent getBroadcastIntent (String action) {
        final Intent intent = new Intent(action);
        intent.setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    private SliceAction getMainSettingsActivity (Context context, Uri sliceUri) {
        final Intent intent = new Intent(context, MainSettingsActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, sliceUri.hashCode(),
                intent, PendingIntent.FLAG_IMMUTABLE);

        return new SliceAction(pendingIntent,
                IconCompat.createWithResource(context, R.mipmap.ic_bthelper).toIcon(),
                null /* actionTitle */);
    }
}
