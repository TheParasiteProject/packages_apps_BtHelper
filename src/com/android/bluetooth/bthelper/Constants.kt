/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper

object Constants {
    /* Authority (package name) */
    const val AUTHORITY_BTHELPER: String = "com.android.bluetooth.bthelper"

    /* Slices Paths */
    const val PATH_BTHELPER: String = "bthelper"
    const val SLICE_BTHELPER: String = "/" + PATH_BTHELPER

    /* Slices Type */
    const val SLICE_TOGGLE: Int = 101
    const val SLICE_MAIN: Int = 102

    /* Slices Intent Action */
    const val ACTION_PENDING_INTENT: String = "com.android.bluetooth.bthelper.ACTION_PENDING_INTENT"

    /* Slices Intent Extra */
    const val EXTRA_NONE: Int = 0
    const val EXTRA_ONEPOD_CHANGED: Int = 10001
    const val EXTRA_AUTO_PLAY_CHANGED: Int = 10002
    const val EXTRA_AUTO_PAUSE_CHANGED: Int = 10003
    const val EXTRA_LOW_LATENCY_AUDIO_CHANGED: Int = 10004

    /* Shared Preferences */
    const val PREFERENCES_BTHELPER: String = AUTHORITY_BTHELPER + "_preferences"

    /* Shared Preferences Keys */
    const val KEY_ONEPOD_MODE: String = "onepod_mode_pref"
    const val KEY_AUTO_PLAY: String = "auto_play_pref"
    const val KEY_AUTO_PAUSE: String = "auto_pause_pref"

    object Icons {
        @JvmField val AirPods: Int = R.drawable.AirPods
        @JvmField val AirPods_Left: Int = R.drawable.AirPods_Left
        @JvmField val AirPods_Right: Int = R.drawable.AirPods_Right
        @JvmField val AirPods_Case: Int = R.drawable.AirPods_Case
        @JvmField val AirPods_Gen3: Int = R.drawable.AirPods_Gen3
        @JvmField val AirPods_Gen3_Left: Int = R.drawable.AirPods_Gen3_Left
        @JvmField val AirPods_Gen3_Right: Int = R.drawable.AirPods_Gen3_Right
        @JvmField val AirPods_Gen3_Case: Int = R.drawable.AirPods_Gen3_Case
        @JvmField val AirPods_Pro: Int = R.drawable.AirPods_Pro
        @JvmField val AirPods_Pro_Left: Int = R.drawable.AirPods_Pro_Left
        @JvmField val AirPods_Pro_Right: Int = R.drawable.AirPods_Pro_Right
        @JvmField val AirPods_Pro_Case: Int = R.drawable.AirPods_Pro_Case
        @JvmField val AirPods_Max_Green: Int = R.drawable.AirPods_Max_Green
        @JvmField val AirPods_Max_Pink: Int = R.drawable.AirPods_Max_Pink
        @JvmField val AirPods_Max_Silver: Int = R.drawable.AirPods_Max_Silver
        @JvmField val AirPods_Max_SkyBlue: Int = R.drawable.AirPods_Max_SkyBlue
        @JvmField val AirPods_Max_SpaceGray: Int = R.drawable.AirPods_Max_SpaceGray

        val defaultIcons: IntArray =
            intArrayOf(
                AirPods,
                AirPods_Left,
                AirPods_Right,
                AirPods_Case,
                AirPods_Gen3,
                AirPods_Gen3_Left,
                AirPods_Gen3_Right,
                AirPods_Gen3_Case,
                AirPods_Pro,
                AirPods_Pro_Left,
                AirPods_Pro_Right,
                AirPods_Pro_Case,
                AirPods_Max_Green,
                AirPods_Max_Pink,
                AirPods_Max_Silver,
                AirPods_Max_SkyBlue,
                AirPods_Max_SpaceGray,
            )
    }
}
