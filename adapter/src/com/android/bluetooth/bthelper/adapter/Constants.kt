/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.adapter

import android.content.ContentResolver
import android.net.Uri

object Constants {
    const val TAG: String = "BtHelper"

    /* Authority (package name) */
    const val AUTHORITY_BTHELPER: String = "com.android.bluetooth.bthelper"
    const val AUTHORITY_SLICE: String = AUTHORITY_BTHELPER + ".sliceprovider"
    const val ACTIVITY_BTHELPER_MAIN_SETTINGS =
        AUTHORITY_BTHELPER + ".settings.MainSettingsActivity"

    /* Slices Paths */
    const val PATH_BTHELPER: String = "bthelper"
    const val SLICE_BTHELPER: String = "/" + PATH_BTHELPER
    const val PARAM_MAC_ADDRESS: String = "address"

    /* Slices Type */
    const val SLICE_TOGGLE: Int = 101
    const val SLICE_MAIN: Int = 102

    /* Slices Intent Action */
    const val ACTION_PENDING_INTENT: String = AUTHORITY_BTHELPER + ".ACTION_PENDING_INTENT"

    /* Slices Intent Extra */
    const val EXTRA_NONE: Int = 0
    const val EXTRA_ONEPOD_CHANGED: Int = 10001
    const val EXTRA_AUTO_PLAY_CHANGED: Int = 10002
    const val EXTRA_AUTO_PAUSE_CHANGED: Int = 10003
    const val EXTRA_LOW_LATENCY_AUDIO_CHANGED: Int = 10004

    /* Intent Actions and extras */
    const val ACTION_SET_SLICE: String = AUTHORITY_BTHELPER + ".SET_SLICE"
    const val EXTRA_METADATA_KEY: String = "metadata_key"

    const val COMPANION_TYPE_NONE = "COMPANION_NONE"
    const val METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25
}

fun getSliceUri(macAddress: String?): String {
    var uri =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(Constants.AUTHORITY_SLICE)
            .appendPath(Constants.PATH_BTHELPER)
    if (macAddress != null && !macAddress.isEmpty()) {
        uri = uri.appendQueryParameter(Constants.PARAM_MAC_ADDRESS, macAddress)
    }
    return uri.build().toString()
}

fun getFastPairSliceUri(macAddress: String?): String {
    return "${getSliceUri(macAddress)}/"
}
