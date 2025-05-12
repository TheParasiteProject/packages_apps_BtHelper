/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper

import android.content.Context
import android.content.SharedPreferences

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
    const val KEY_LOW_LATENCY_AUDIO: String = "low_latency_audio_pref"
    const val KEY_SINGLE_DEVICE: String = "key_single_device"

    object Icons {
        val AirPods: Int = R.drawable.AirPods
        val AirPods_Left: Int = R.drawable.AirPods_Left
        val AirPods_Right: Int = R.drawable.AirPods_Right
        val AirPods_Case: Int = R.drawable.AirPods_Case
        val AirPods_Gen3: Int = R.drawable.AirPods_Gen3
        val AirPods_Gen3_Left: Int = R.drawable.AirPods_Gen3_Left
        val AirPods_Gen3_Right: Int = R.drawable.AirPods_Gen3_Right
        val AirPods_Gen3_Case: Int = R.drawable.AirPods_Gen3_Case
        val AirPods_Gen4: Int = R.drawable.AirPods_Gen4
        val AirPods_Gen4_Left: Int = R.drawable.AirPods_Gen4_Left
        val AirPods_Gen4_Right: Int = R.drawable.AirPods_Gen4_Right
        val AirPods_Gen4_Case: Int = R.drawable.AirPods_Gen4_Case
        val AirPods_Pro: Int = R.drawable.AirPods_Pro
        val AirPods_Pro_Left: Int = R.drawable.AirPods_Pro_Left
        val AirPods_Pro_Right: Int = R.drawable.AirPods_Pro_Right
        val AirPods_Pro_Case: Int = R.drawable.AirPods_Pro_Case
        val AirPods_Max_Blue: Int = R.drawable.AirPods_Max_Blue
        val AirPods_Max_Green: Int = R.drawable.AirPods_Max_Green
        val AirPods_Max_Midnight: Int = R.drawable.AirPods_Max_Midnight
        val AirPods_Max_Orange: Int = R.drawable.AirPods_Max_Orange
        val AirPods_Max_Pink: Int = R.drawable.AirPods_Max_Pink
        val AirPods_Max_Purple: Int = R.drawable.AirPods_Max_Purple
        val AirPods_Max_Silver: Int = R.drawable.AirPods_Max_Silver
        val AirPods_Max_SkyBlue: Int = R.drawable.AirPods_Max_SkyBlue
        val AirPods_Max_SpaceGray: Int = R.drawable.AirPods_Max_SpaceGray
        val AirPods_Max_StarLight: Int = R.drawable.AirPods_Max_StarLight

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
                AirPods_Gen4,
                AirPods_Gen4_Left,
                AirPods_Gen4_Right,
                AirPods_Gen4_Case,
                AirPods_Pro,
                AirPods_Pro_Left,
                AirPods_Pro_Right,
                AirPods_Pro_Case,
                AirPods_Max_Blue,
                AirPods_Max_Green,
                AirPods_Max_Midnight,
                AirPods_Max_Orange,
                AirPods_Max_Pink,
                AirPods_Max_Purple,
                AirPods_Max_Silver,
                AirPods_Max_SkyBlue,
                AirPods_Max_SpaceGray,
                AirPods_Max_StarLight,
            )
    }
}

fun Context.getSharedPreferences(): SharedPreferences {
    return this.getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE)
}

fun SharedPreferences.setSingleDevice(single: Boolean) {
    this.edit()?.putBoolean(Constants.KEY_SINGLE_DEVICE, single)?.apply()
}

fun SharedPreferences.isSingleDevice(): Boolean {
    return this.getBoolean(Constants.KEY_SINGLE_DEVICE, false)
}

fun Context.isLowLatencySupported(): Boolean {
    return this.resources.getBoolean(R.bool.config_low_latency_audio_supported)
}
