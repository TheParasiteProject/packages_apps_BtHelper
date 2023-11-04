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

    public static final class Icons {
        public static final int AirPods = R.drawable.AirPods;
        public static final int AirPods_Left = R.drawable.AirPods_Left;
        public static final int AirPods_Right= R.drawable.AirPods_Right;
        public static final int AirPods_Case = R.drawable.AirPods_Case;
        public static final int AirPods_Gen3 = R.drawable.AirPods_Gen3;
        public static final int AirPods_Gen3_Left = R.drawable.AirPods_Gen3_Left;
        public static final int AirPods_Gen3_Right = R.drawable.AirPods_Gen3_Right;
        public static final int AirPods_Gen3_Case = R.drawable.AirPods_Gen3_Case;
        public static final int AirPods_Pro = R.drawable.AirPods_Pro;
        public static final int AirPods_Pro_Left = R.drawable.AirPods_Pro_Left;
        public static final int AirPods_Pro_Right = R.drawable.AirPods_Pro_Right;
        public static final int AirPods_Pro_Case = R.drawable.AirPods_Pro_Case;
        public static final int AirPods_Max_Green = R.drawable.AirPods_Max_Green;
        public static final int AirPods_Max_Pink = R.drawable.AirPods_Max_Pink;
        public static final int AirPods_Max_Silver = R.drawable.AirPods_Max_Silver;
        public static final int AirPods_Max_SkyBlue = R.drawable.AirPods_Max_SkyBlue;
        public static final int AirPods_Max_SpaceGray = R.drawable.AirPods_Max_SpaceGray;
    }
}
