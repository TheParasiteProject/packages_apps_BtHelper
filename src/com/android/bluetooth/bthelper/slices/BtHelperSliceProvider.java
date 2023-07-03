/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.slices;

import android.annotation.ColorInt;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;

import com.android.bluetooth.bthelper.Constants;
import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.slices.SliceCreator;

public class BtHelperSliceProvider extends SliceProvider {
    private Context mContext;
    private static SharedPreferences mSharedPrefs;

    /**
     * Constant representing infinity.
     */
    private static final long INFINITY = -1;

    @Override
    public boolean onCreateSliceProvider() {
        try {
            mContext = getContext();
            mSharedPrefs = mContext.getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE);
        } catch (NullPointerException e) {
        }
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        final String path = sliceUri.getPath();
        switch (path) {
            case Constants.SLICE_BTHELPER:
                return createBtHelperSlice(sliceUri);
        }
        return null;
    }

    private Slice createBtHelperSlice(Uri sliceUri) {
        try {
            if (mContext == null) return null;
        } catch (NullPointerException e) {
        }

        final String ONEPOD_TITLE = mContext.getString(R.string.onepod_mode_title);
        final boolean onePodModeEnabled = mSharedPrefs.getBoolean(Constants.KEY_ONEPOD_MODE, false);

        final String AUTO_PLAY_TITLE = mContext.getString(R.string.auto_play_title);
        final boolean autoPlayEnabled = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PLAY, false);

        final String AUTO_PAUSE_TITLE = mContext.getString(R.string.auto_pause_title);
        final boolean autoPauseEnabled = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PAUSE, false);

        final String LOW_LATENCY_TITLE = mContext.getString(R.string.low_latency_audio_title);
        final String LOW_LATENCY_SUBTITLE = mContext.getString(R.string.low_latency_audio_slice_subtitle);
        final boolean lowLatencyEnabled = mSharedPrefs.getBoolean(Constants.KEY_LOW_LATENCY_AUDIO, false);

        ListBuilder listBuilder = new ListBuilder(mContext, sliceUri, INFINITY);

        listBuilder.addRow(new SliceCreator(
                ONEPOD_TITLE,
                null,
                onePodModeEnabled,
                Constants.ACTION_ONEPOD_CHANGED, 
                mContext
            ).getSettingRow(sliceUri));

        listBuilder.addRow(new SliceCreator(
                AUTO_PLAY_TITLE,
                null,
                autoPlayEnabled,
                Constants.ACTION_AUTO_PLAY_CHANGED, 
                mContext
            ).getSettingRow(sliceUri));

        listBuilder.addRow(new SliceCreator(
                AUTO_PAUSE_TITLE,
                null,
                autoPauseEnabled,
                Constants.ACTION_AUTO_PAUSE_CHANGED, 
                mContext
            ).getSettingRow(sliceUri));

        listBuilder.addRow(new SliceCreator(
                LOW_LATENCY_TITLE,
                LOW_LATENCY_SUBTITLE,
                lowLatencyEnabled,
                Constants.ACTION_LOW_LATENCY_AUDIO_CHANGED, 
                mContext
            ).getSettingRow(sliceUri));

        listBuilder.setAccentColor(getColorAccentDefaultColor(mContext));
        return listBuilder.build();
    }

    @ColorInt
    private static int getColorAccentDefaultColor (Context context) {
        return getColorAttrDefaultColor(context, android.R.attr.colorAccent, 0);
    }

    private static int getColorAttrDefaultColor (Context context, int attr, @ColorInt int defValue) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        @ColorInt int colorAccent = ta.getColor(0, defValue);
        ta.recycle();
        return colorAccent;
    }
}
