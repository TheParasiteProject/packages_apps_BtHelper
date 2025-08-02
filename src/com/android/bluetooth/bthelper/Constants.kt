/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper

import android.content.Context
import android.content.SharedPreferences
import android.os.ParcelUuid

object Constants {
    const val TAG: String = "BtHelper"

    /* Authority (package name) */
    const val AUTHORITY_BTHELPER: String = "com.android.bluetooth.bthelper"
    const val AUTHORITY_FILE: String = AUTHORITY_BTHELPER + ".fileprovider"
    const val PACKAGE_BTHELPER_ADAPTER: String = AUTHORITY_BTHELPER + ".adapter"

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

    /* Shared Preferences */
    const val PREFERENCES_BTHELPER: String = AUTHORITY_BTHELPER + "_preferences"

    /* Shared Preferences Keys */
    const val KEY_LOW_LATENCY_AUDIO: String = "low_latency_audio_pref"
    const val KEY_SINGLE_DEVICE: String = "key_single_device"
    const val KEY_MAC_ADDRESS: String = "mac_address"
    const val KEY_AUTOMATIC_EAR_DETECTION: String = "automatic_ear_detection"
    const val KEY_CONVERSATIONAL_AWARENESS_PAUSE_MUSIC: String =
        "conversational_awareness_pause_music"
    const val KEY_RELATIVE_CONVERSATIONAL_AWARENESS_VOLUME: String =
        "relative_conversational_awareness_volume"
    const val KEY_HEAD_GESTURES: String = "head_gestures"
    const val KEY_DISCONNECT_WHEN_NOT_WEARING: String = "disconnect_when_not_wearing"
    const val KEY_CONVERSATIONAL_AWARENESS_VOLUME: String = "conversational_awareness_volume"
    const val KEY_USE_ALTERNATE_HEAD_TRACKING_PACKETS: String =
        "use_alternate_head_tracking_packets"
    const val KEY_LEFT_SINGLE_PRESS_ACTION: String = "left_single_press_action"
    const val KEY_RIGHT_SINGLE_PRESS_ACTION: String = "right_single_press_action"
    const val KEY_LEFT_DOUBLE_PRESS_ACTION: String = "left_double_press_action"
    const val KEY_RIGHT_DOUBLE_PRESS_ACTION: String = "right_double_press_action"
    const val KEY_LEFT_TRIPLE_PRESS_ACTION: String = "left_triple_press_action"
    const val KEY_RIGHT_TRIPLE_PRESS_ACTION: String = "right_triple_press_action"
    const val KEY_LEFT_LONG_PRESS_ACTION: String = "left_long_press_action"
    const val KEY_RIGHT_LONG_PRESS_ACTION: String = "right_long_press_action"

    /* Intent Actions and extras */
    const val ACTION_CONNECTED: String = AUTHORITY_BTHELPER + ".CONNECTED"
    const val ACTION_DISCONNECTED: String = AUTHORITY_BTHELPER + ".DISCONNECTED"
    const val ACTION_NAME_CHANGED: String = AUTHORITY_BTHELPER + ".NAME_CHANGED"

    const val ACTION_SET_SLICE: String = AUTHORITY_BTHELPER + ".SET_SLICE"
    const val EXTRA_METADATA_KEY: String = "metadata_key"

    const val ACTION_SET_ANC_MODE: String = AUTHORITY_BTHELPER + ".SET_ANC_MODE"
    const val EXTRA_MODE: String = "mode"

    /* Hidden APIs */
    const val HIDDEN_API_BLUETOOTH_SOCKET: String = "Landroid/bluetooth/BluetoothSocket;"

    /* Connection States */
    const val STATE_UNKNOWN: String = "Unknown"
    const val STATE_DISCONNECTED: String = "Disconnected"
    const val STATE_IDLE: String = "Idle"
    const val STATE_MUSIC: String = "Music"
    const val STATE_CALL: String = "Call"
    const val STATE_RINGING: String = "Ringing"
    const val STATE_HANGING_UP: String = "Hanging Up"

    /* Crypto */
    const val CRYPTO_AES_ECB_NO_PADDING: String = "AES/ECB/NoPadding"

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

    val PODS_UUID: ParcelUuid = ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")
    val BEATS_UUID: ParcelUuid = ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")

    val PodsUUIDS: MutableSet<ParcelUuid> = HashSet<ParcelUuid>()

    init {
        PodsUUIDS.add(PODS_UUID)
        // PodsUUIDS.add(BEATS_UUID)
    }

    const val ACTION_AVRCP_CONNECTION_STATE_CHANGED =
        "android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED"

    /**
     * Intent used to broadcast the headset's indicator status
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link #EXTRA_HF_INDICATORS_IND_ID} - The Assigned number of headset Indicator which is
     *   supported by the headset ( as indicated by AT+BIND command in the SLC sequence) or whose
     *   value is changed (indicated by AT+BIEV command) </li>
     * <li> {@link #EXTRA_HF_INDICATORS_IND_VALUE} - Updated value of headset indicator. </li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - Remote device. </li>
     * </ul>
     *
     * <p>{@link #EXTRA_HF_INDICATORS_IND_ID} is defined by Bluetooth SIG and each of the indicators
     * are given an assigned number. Below shows the assigned number of Indicator added so far
     * - Enhanced Safety - 1, Valid Values: 0 - Disabled, 1 - Enabled
     * - Battery Level - 2, Valid Values: 0~100 - Remaining level of Battery
     */
    const val ACTION_HF_INDICATORS_VALUE_CHANGED =
        "android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED"

    /**
     * A int extra field in {@link #ACTION_HF_INDICATORS_VALUE_CHANGED} intents that contains the
     * assigned number of the headset indicator as defined by Bluetooth SIG that is being sent.
     * Value range is 0-65535 as defined in HFP 1.7
     */
    const val EXTRA_HF_INDICATORS_IND_ID = "android.bluetooth.headset.extra.HF_INDICATORS_IND_ID"

    /**
     * A int extra field in {@link #ACTION_HF_INDICATORS_VALUE_CHANGED} intents that contains the
     * value of the Headset indicator that is being sent.
     */
    const val EXTRA_HF_INDICATORS_IND_VALUE =
        "android.bluetooth.headset.extra.HF_INDICATORS_IND_VALUE"

    // Match up with bthf_hf_ind_type_t of bt_hf.h
    const val HF_INDICATOR_BATTERY_LEVEL_STATUS = 2

    /**
     * Broadcast Action: Indicates the battery level of a remote device has been retrieved for the
     * first time, or changed since the last retrieval
     *
     * <p>Always contains the extra fields {@link BluetoothDevice#EXTRA_DEVICE} and {@link
     * BluetoothDevice#EXTRA_BATTERY_LEVEL}.
     */
    const val ACTION_BATTERY_LEVEL_CHANGED: String =
        "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

    /**
     * Used as an Integer extra field in {@link #ACTION_BATTERY_LEVEL_CHANGED} intent. It contains
     * the most recently retrieved battery level information ranging from 0% to 100% for a remote
     * device, {@link #BATTERY_LEVEL_UNKNOWN} when the valid is unknown or there is an error,
     * {@link #BATTERY_LEVEL_BLUETOOTH_OFF} when the bluetooth is off
     */
    const val EXTRA_BATTERY_LEVEL: String = "android.bluetooth.device.extra.BATTERY_LEVEL"

    // Target Android Settings Intelligence package that have battery widget for data update
    const val PACKAGE_ASI = "com.google.android.settings.intelligence"

    /**
     * Intent used to broadcast bluetooth data update for the Settings Intelligence package's
     * battery widget
     */
    const val ACTION_ASI_UPDATE_BLUETOOTH_DATA = "batterywidget.impl.action.update_bluetooth_data"

    const val COMPANION_TYPE_NONE = "COMPANION_NONE"
    const val METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25
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

fun Context.getUriAllowlist(): Array<String> {
    return this.resources.getStringArray(R.array.config_uri_access_allow_list)
}
