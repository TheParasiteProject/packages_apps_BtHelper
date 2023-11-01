/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2019 The MoKee Open Source Project
 *               2021-2023 Matthias Urhahn
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.pods;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.content.SharedPreferences;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.UserHandle;

import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.android.bluetooth.bthelper.pods.models.IPods;
import com.android.bluetooth.bthelper.pods.models.RegularPods;
import com.android.bluetooth.bthelper.pods.models.SinglePods;
import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.Constants;
import com.android.bluetooth.bthelper.slices.BtHelperSliceProvider;
import com.android.bluetooth.bthelper.utils.MediaControl;
import static com.android.bluetooth.bthelper.pods.PodsStatusScanCallback.getScanFilters;

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 */
public class PodsService extends Service {

    /**
     * Intent used to broadcast the headset's indicator status
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link #EXTRA_HF_INDICATORS_IND_ID} - The Assigned number of headset Indicator which
     * is supported by the headset ( as indicated by AT+BIND command in the SLC
     * sequence) or whose value is changed (indicated by AT+BIEV command) </li>
     * <li> {@link #EXTRA_HF_INDICATORS_IND_VALUE} - Updated value of headset indicator. </li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - Remote device. </li>
     * </ul>
     * <p>{@link #EXTRA_HF_INDICATORS_IND_ID} is defined by Bluetooth SIG and each of the indicators
     * are given an assigned number. Below shows the assigned number of Indicator added so far
     * - Enhanced Safety - 1, Valid Values: 0 - Disabled, 1 - Enabled
     * - Battery Level - 2, Valid Values: 0~100 - Remaining level of Battery
     */
    private static final String ACTION_HF_INDICATORS_VALUE_CHANGED =
            "android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED";

    /**
     * A int extra field in {@link #ACTION_HF_INDICATORS_VALUE_CHANGED}
     * intents that contains the assigned number of the headset indicator as defined by
     * Bluetooth SIG that is being sent. Value range is 0-65535 as defined in HFP 1.7
     */
    private static final String EXTRA_HF_INDICATORS_IND_ID =
            "android.bluetooth.headset.extra.HF_INDICATORS_IND_ID";

    /**
     * A int extra field in {@link #ACTION_HF_INDICATORS_VALUE_CHANGED}
     * intents that contains the value of the Headset indicator that is being sent.
     */
    private static final String EXTRA_HF_INDICATORS_IND_VALUE =
            "android.bluetooth.headset.extra.HF_INDICATORS_IND_VALUE";

    // Match up with bthf_hf_ind_type_t of bt_hf.h
    private static final int HF_INDICATOR_BATTERY_LEVEL_STATUS = 2;

    /**
     * Broadcast Action: Indicates the battery level of a remote device has
     * been retrieved for the first time, or changed since the last retrieval
     * <p>Always contains the extra fields {@link BluetoothDevice#EXTRA_DEVICE}
     * and {@link BluetoothDevice#EXTRA_BATTERY_LEVEL}.
     */
    public static final String ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";

    // Target Android Settings Intelligence package that have battery widget for data update
    private static final String PACKAGE_ASI = "com.google.android.settings.intelligence";

    /** 
     * Intent used to broadcast bluetooth data update
     * for the Settings Intelligence package's battery widget
     */
    private static final String ACTION_ASI_UPDATE_BLUETOOTH_DATA = "batterywidget.impl.action.update_bluetooth_data";

    private static final String COMPANION_TYPE_NONE = "COMPANION_NONE";

    private BluetoothLeScanner btScanner;
    private PodsStatus status = PodsStatus.DISCONNECTED;

    private BroadcastReceiver btReceiver = null;
    private PodsStatusScanCallback scanCallback = null;

    private static BluetoothDevice mCurrentDevice;

    private static boolean isSinglePods = false;
    private boolean isMetaDataSet = false;
    private boolean isSliceSet = false;
    private boolean isModelDataSet = false;
    
    private boolean statusChanged = false;

    private static SharedPreferences mSharedPrefs;
    private static MediaControl mediaControl;
    private static boolean previousWorn = false;

    public PodsService () {
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        try {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                mCurrentDevice = device;
                setLowLatencyAudio();
                startAirPodsScanner();
            }
        } catch (NullPointerException e) {
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        stopAirPodsScanner();
    }

    // Reset currently set device
    public static void shouldResetDevice (boolean reset) {
        if (reset) mCurrentDevice = null;
    }

    // Set Low Latency Audio mode to current device
    public void setLowLatencyAudio () {
        mSharedPrefs = getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE);
        mCurrentDevice.setLowLatencyAudioAllowed(mSharedPrefs.getBoolean(Constants.KEY_LOW_LATENCY_AUDIO, false));
    }

    /**
     * The following method (startAirPodsScanner) creates a bluetooth LE scanner.
     * This scanner receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3 things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     *
     * After decoding a beacon, the status is written to PodsStatus.
     */
    private void startAirPodsScanner () {
        try {
            BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = btManager.getAdapter();

            if (btAdapter == null) {
                return;
            }

            if (btScanner != null && scanCallback != null) {
                btScanner.stopScan(scanCallback);
                scanCallback = null;
            }

            if (!btAdapter.isEnabled()) {
                return;
            }

            btScanner = btAdapter.getBluetoothLeScanner();

            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1) // DON'T USE 0
                    .build();

            scanCallback = new PodsStatusScanCallback() {
                @Override
                public void onStatus (PodsStatus newStatus) {
                    setStatusChanged(status, newStatus);
                    status = newStatus;
                    handlePlayPause(status, getApplicationContext());
                    updatePodsStatus(status, mCurrentDevice);
                }
            };

            btScanner.startScan(getScanFilters(), scanSettings, scanCallback);
        } catch (Throwable t) {
        }
    }

    private void stopAirPodsScanner () {
        try {
            if (btScanner != null && scanCallback != null) {

                btScanner.stopScan(scanCallback);
                scanCallback = null;
            }
            status = PodsStatus.DISCONNECTED;
        } catch (Throwable t) {
        }
    }

    // Set boolean value to true if device's status has changed
    private void setStatusChanged (PodsStatus status, PodsStatus newStatus) {
        if (!Objects.equals(status, newStatus)) {
            statusChanged = true;
        }
    }

    // Check whether current device is single model (e.g. AirPods Max)
    public static boolean isSingleDevice () {
        return isSinglePods;
    }

    // Handle Play/Pause media control event based on device wear status
    private void handlePlayPause (PodsStatus status, Context context) {
        mSharedPrefs = getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE);

        final boolean onePodMode = mSharedPrefs.getBoolean(Constants.KEY_ONEPOD_MODE, false);
        final boolean autoPlay = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PLAY, false);
        final boolean autoPause = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PAUSE, false);
        final boolean autoPlayPause = autoPlay && autoPause;

        try {
            mediaControl = MediaControl.getInstance(context);
        } catch (Exception e) {
        }

        if (mediaControl == null) return;

        final IPods airpods = status.getAirpods();
        final boolean single = airpods.isSingle();
        boolean currentWorn = false;

        if (!single) {
            final RegularPods regularPods = (RegularPods)airpods;
            if (onePodMode) {
                currentWorn = regularPods.isInEar(RegularPods.LEFT) || regularPods.isInEar(RegularPods.RIGHT);
            } else {
                currentWorn = regularPods.isInEar(RegularPods.LEFT) && regularPods.isInEar(RegularPods.RIGHT);
            }
        } else {
            final SinglePods singlePods = (SinglePods)airpods;
            currentWorn = singlePods.isInEar();
        }

        if (!previousWorn && currentWorn && !mediaControl.isPlaying()) {
            if (autoPlayPause || autoPlay) {
                mediaControl.sendPlay();
            }
        } else if (previousWorn && !currentWorn && mediaControl.isPlaying()) {
            if (autoPlayPause || autoPause) {
                mediaControl.sendPause();
            }
        }

        previousWorn = currentWorn;
    }

    // Convert internal content address combined with recieved path value to URI
    public static Uri getUri (String path) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(Constants.AUTHORITY_BTHELPER)
                .appendPath(path)
                .build();
    }

    // Convert internal resource address to URI
    private Uri resToUri (int resId) {
        try {
            Uri uri = (new Uri.Builder())
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(Constants.AUTHORITY_BTHELPER)
                        .appendPath(getApplicationContext().getResources().getResourceTypeName(resId))
                        .appendPath(getApplicationContext().getResources().getResourceEntryName(resId))
                        .build();
            return uri;
        } catch (NotFoundException e) {
            return null;
        }
    }

    private boolean setMetadata(BluetoothDevice device, int key, byte[] value) {
        if (device.getMetadata(key) == null) {
            return device.setMetadata(key, value);
        }
        return true;
    }

    // Set metadata (icon, battery, charging status, etc.) for current device
    // and send broadcast that device status has changed
    private void updatePodsStatus (PodsStatus status, BluetoothDevice device) {
        final IPods airpods = status.getAirpods();
        final boolean single = airpods.isSingle();
        isSinglePods = single;
        int batteryUnified = 0;
        boolean chargingMain = false;
        
        if (!isMetaDataSet) {
            isSliceSet = setMetadata(device, device.METADATA_COMPANION_APP, Constants.AUTHORITY_BTHELPER.getBytes());
            isSliceSet = setMetadata(device, device.METADATA_SOFTWARE_VERSION, COMPANION_TYPE_NONE.getBytes());
            isSliceSet = setMetadata(device, device.METADATA_ENHANCED_SETTINGS_UI_URI, getUri(Constants.PATH_BTHELPER).toString().getBytes());
        }

        if (!single) {
            final RegularPods regularPods = (RegularPods)airpods;
            if (!isMetaDataSet) {
                isModelDataSet =
                    setMetadata(device, device.METADATA_MANUFACTURER_NAME, regularPods.getMenufacturer().getBytes())
                    && setMetadata(device, device.METADATA_MODEL_NAME, regularPods.getModel().getBytes())
                    && setMetadata(device, device.METADATA_DEVICE_TYPE, device.DEVICE_TYPE_UNTETHERED_HEADSET.getBytes())
                    && setMetadata(device, device.METADATA_MAIN_LOW_BATTERY_THRESHOLD, (regularPods.getLowBattThreshold() + "").getBytes())
                    && setMetadata(device, device.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD, (regularPods.getLowBattThreshold() + "").getBytes())
                    && setMetadata(device, device.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD, (regularPods.getLowBattThreshold() + "").getBytes())
                    && setMetadata(device, device.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD, (regularPods.getLowBattThreshold() + "").getBytes())
                    && setMetadata(device, device.METADATA_MAIN_ICON, resToUri(regularPods.getDrawable()).toString().getBytes())
                    && setMetadata(device, device.METADATA_UNTETHERED_LEFT_ICON, resToUri(regularPods.getLeftDrawable()).toString().getBytes())
                    && setMetadata(device, device.METADATA_UNTETHERED_RIGHT_ICON, resToUri(regularPods.getRightDrawable()).toString().getBytes())
                    && setMetadata(device, device.METADATA_UNTETHERED_CASE_ICON, resToUri(regularPods.getCaseDrawable()).toString().getBytes());
            }

            if (statusChanged) {
                final boolean leftCharging = regularPods.isCharging(RegularPods.LEFT);
                final boolean rightCharging = regularPods.isCharging(RegularPods.RIGHT);
                final boolean caseCharging = regularPods.isCharging(RegularPods.CASE);
                final int leftBattery = regularPods.getParsedStatus(RegularPods.LEFT);
                final int rightBattery = regularPods.getParsedStatus(RegularPods.RIGHT);
                final int caseBattery = regularPods.getParsedStatus(RegularPods.CASE);

                device.setMetadata(device.METADATA_UNTETHERED_LEFT_CHARGING, (leftCharging + "").toUpperCase().getBytes());
                device.setMetadata(device.METADATA_UNTETHERED_RIGHT_CHARGING, (rightCharging + "").toUpperCase().getBytes());
                device.setMetadata(device.METADATA_UNTETHERED_CASE_CHARGING, (caseCharging + "").toUpperCase().getBytes());
                device.setMetadata(device.METADATA_UNTETHERED_LEFT_BATTERY, (leftBattery + "").getBytes());
                device.setMetadata(device.METADATA_UNTETHERED_RIGHT_BATTERY, (rightBattery + "").getBytes());
                device.setMetadata(device.METADATA_UNTETHERED_CASE_BATTERY, (caseBattery + "").getBytes());

                chargingMain = leftCharging && rightCharging;
                batteryUnified = Math.min(leftBattery, rightBattery);
            }
        } else {
            final SinglePods singlePods = (SinglePods)airpods;
            if (!isMetaDataSet) {
                isModelDataSet =
                    setMetadata(device, device.METADATA_MANUFACTURER_NAME, singlePods.getMenufacturer().getBytes())
                    && setMetadata(device, device.METADATA_DEVICE_TYPE, device.DEVICE_TYPE_UNTETHERED_HEADSET.getBytes())
                    && setMetadata(device, device.METADATA_MODEL_NAME, singlePods.getModel().getBytes())
                    && setMetadata(device, device.METADATA_MAIN_LOW_BATTERY_THRESHOLD, (singlePods.getLowBattThreshold() + "").getBytes())
                    && setMetadata(device, device.METADATA_MAIN_ICON, resToUri(singlePods.getDrawable()).toString().getBytes());
            }
            chargingMain = singlePods.isCharging();
            batteryUnified = singlePods.getParsedStatus();
        }

        if (statusChanged) {
            device.setMetadata(device.METADATA_MAIN_CHARGING, (chargingMain + "").toUpperCase().getBytes());
        }

        if (!isMetaDataSet) {
            isMetaDataSet = isSliceSet && isModelDataSet;
        }

        if (statusChanged) {
            broadcastHfIndicatorEventIntent(batteryUnified, device);
            statusChanged = false;
        }
    }

    private static final String[] btPermissions = new String[] {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_PRIVILEGED,
    };

    // Send broadcasts to Android Settings Intelligence, Bluetooth app, System Settings
    // to reflect current device status changes
    private void broadcastHfIndicatorEventIntent (int battery, BluetoothDevice device) {
        // Update battery status for this device
        final Intent intent = new Intent(ACTION_HF_INDICATORS_VALUE_CHANGED);
        intent.putExtra(EXTRA_HF_INDICATORS_IND_ID, HF_INDICATOR_BATTERY_LEVEL_STATUS);
        intent.putExtra(EXTRA_HF_INDICATORS_IND_VALUE, battery);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        sendBroadcastAsUserMultiplePermissions(intent, UserHandle.ALL, btPermissions);

        if (statusChanged) {
            // Update Android Settings Intelligence's battery widget
            final Intent statusIntent = 
                new Intent(ACTION_ASI_UPDATE_BLUETOOTH_DATA).setPackage(PACKAGE_ASI);
            statusIntent.putExtra(ACTION_BATTERY_LEVEL_CHANGED, intent);
            sendBroadcastAsUser(statusIntent, UserHandle.ALL);
        }
    }
}
