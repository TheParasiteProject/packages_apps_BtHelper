/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.settings;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.settings.MainSettingsActivity;

public class MainSettingsSliceProvider extends SliceProvider {
    private Context context;

    /**
     * Constant representing infinity.
     */
    private static final long INFINITY = -1;

    @Override
    public boolean onCreateSliceProvider() {
        context = getContext();
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        final String path = sliceUri.getPath();
        switch (path) {
            case "/bthelper":
                return createBtHelperSlice(sliceUri);
        }
        return null;
    }

    private Slice createBtHelperSlice(Uri sliceUri) {
        ListBuilder listBuilder = new ListBuilder(context, sliceUri, INFINITY);
        ListBuilder.RowBuilder btHeleperRow = new ListBuilder.RowBuilder(sliceUri);
        final String SLICE_TITLE = context.getString(R.string.advanced_device_settings_title);
        btHeleperRow.setTitle(SLICE_TITLE);
        Intent intent = new Intent(getContext(), MainSettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), sliceUri.hashCode(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        SliceAction openMainSettingsActivity = new SliceAction(pendingIntent,
                IconCompat.createWithResource(context, R.drawable.ic_chevron_right).toIcon(),
                SLICE_TITLE);
        btHeleperRow.setPrimaryAction(openMainSettingsActivity);
        listBuilder.addRow(btHeleperRow);
        return listBuilder.build();
    }

    public static Uri getUri(Context context, String path) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(context.getPackageName())
                .appendPath(path)
                .build();
    }
}
