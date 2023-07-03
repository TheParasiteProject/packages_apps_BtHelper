/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper;

public class Constants {
    /* Authority (package name) */
    public static final String AUTHORITY_BTHELPER =
            "com.android.bluetooth.bthelper";

    /* Slices Paths */
    public static final String PATH_BTHELPER = "bthelper";
    public static final String SLICE_BTHELPER = "/"+PATH_BTHELPER;

    /* Slices Intent Action */
    public static final String ACTION_ONEPOD_CHANGED =
            "com.android.bluetooth.bthelper.action.ONEPOD_CHANGED";
    public static final String ACTION_AUTO_PLAY_CHANGED =
            "com.android.bluetooth.bthelper.action.AUTO_PLAY_CHANGED";
    public static final String ACTION_AUTO_PAUSE_CHANGED =
            "com.android.bluetooth.bthelper.action.AUTO_PAUSE_CHANGED";
    public static final String ACTION_LOW_LATENCY_AUDIO_CHANGED =
            "com.android.bluetooth.bthelper.action.LOW_LATENCY_AUDIO_CHANGED";

    /* Shared Preferences */
    public static final String PREFERENCES_BTHELPER = AUTHORITY_BTHELPER+"_preferences";

    /* Shared Preferences Keys */
    public static final String KEY_ONEPOD_MODE = "onepod_mode_pref";
    public static final String KEY_AUTO_PLAY = "auto_play_pref";
    public static final String KEY_AUTO_PAUSE = "auto_pause_pref";
    public static final String KEY_LOW_LATENCY_AUDIO = "low_latency_audio_pref";
}
