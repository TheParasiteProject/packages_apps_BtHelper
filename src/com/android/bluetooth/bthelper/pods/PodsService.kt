/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: Matthias Urhahn
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.IBinder
import android.os.UserHandle
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.pods.models.IPods
import com.android.bluetooth.bthelper.pods.models.RegularPods
import com.android.bluetooth.bthelper.pods.models.SinglePods
import com.android.bluetooth.bthelper.utils.MediaControl
import java.util.Locale
import kotlin.math.min

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them
 */
class PodsService : Service() {

    private var btScanner: BluetoothLeScanner? = null
    private var status: PodsStatus = PodsStatus.DISCONNECTED

    private val btReceiver: BroadcastReceiver? = null
    private var scanCallback: PodsStatusScanCallback? = null

    private var mCurrentDevice: BluetoothDevice? = null

    private var isMetaDataSet = false
    private var isSliceSet = false
    private var isModelDataSet = false

    private var statusChanged = false

    private var mSharedPrefs: SharedPreferences? = null
    private var mediaControl: MediaControl? = null
    private var previousWorn = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaControl = MediaControl(this)
        try {
            val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device != null) {
                mCurrentDevice = device
                startAirPodsScanner()
            }
        } catch (e: NullPointerException) {}
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaControl = null
        mCurrentDevice = null
        stopAirPodsScanner()
    }

    /**
     * The following method (startAirPodsScanner) creates a bluetooth LE scanner. This scanner
     * receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3
     * things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     *
     * After decoding a beacon, the status is written to PodsStatus.
     */
    private fun startAirPodsScanner() {
        try {
            val btManager: BluetoothManager =
                getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter: BluetoothAdapter = btManager.getAdapter() ?: return

            if (btScanner != null && scanCallback != null) {
                btScanner!!.stopScan(scanCallback)
                scanCallback = null
            }

            if (!btAdapter.isEnabled()) {
                return
            }

            btScanner = btAdapter.getBluetoothLeScanner()

            val scanSettings: ScanSettings =
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1) // DON'T USE 0
                    .build()

            scanCallback =
                object : PodsStatusScanCallback() {
                    override fun onStatus(newStatus: PodsStatus) {
                        setStatusChanged(status, newStatus)
                        status = newStatus
                        handlePlayPause(status, getApplicationContext())
                        updatePodsStatus(status, mCurrentDevice)
                    }
                }

            btScanner!!.startScan(scanCallback!!.scanFilters, scanSettings, scanCallback)
        } catch (t: Throwable) {}
    }

    private fun stopAirPodsScanner() {
        try {
            if (btScanner != null && scanCallback != null) {
                btScanner!!.stopScan(scanCallback)
                scanCallback = null
            }
            status = PodsStatus.DISCONNECTED
        } catch (t: Throwable) {}
    }

    // Set boolean value to true if device's status has changed
    private fun setStatusChanged(status: PodsStatus, newStatus: PodsStatus) {
        if (status != newStatus) {
            statusChanged = true
        }
    }

    // Handle Play/Pause media control event based on device wear status
    private fun handlePlayPause(status: PodsStatus, context: Context) {
        mSharedPrefs = getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE)
        val sp = mSharedPrefs

        val onePodMode: Boolean = sp!!.getBoolean(Constants.KEY_ONEPOD_MODE, false)
        val autoPlay: Boolean = sp!!.getBoolean(Constants.KEY_AUTO_PLAY, false)
        val autoPause: Boolean = sp!!.getBoolean(Constants.KEY_AUTO_PAUSE, false)
        val autoPlayPause = autoPlay && autoPause

        if (mediaControl == null) return

        val airpods: IPods? = status.pods
        if (airpods == null) return
        val single: Boolean = airpods.isSingle
        var currentWorn = false

        if (!single) {
            val regularPods: RegularPods = airpods as RegularPods
            currentWorn =
                if (onePodMode) {
                    (regularPods.isInEar(RegularPods.LEFT) ||
                        regularPods.isInEar(RegularPods.RIGHT))
                } else {
                    (regularPods.isInEar(RegularPods.LEFT) &&
                        regularPods.isInEar(RegularPods.RIGHT))
                }
        } else {
            val singlePods: SinglePods = airpods as SinglePods
            currentWorn = singlePods.isInEar
        }

        mediaControl?.let { media ->
            if (!previousWorn && currentWorn && !media.isPlaying) {
                if (autoPlayPause || autoPlay) {
                    media.sendPlay()
                }
            } else if (previousWorn && !currentWorn && media.isPlaying) {
                if (autoPlayPause || autoPause) {
                    media.sendPause()
                }
            }
        }

        previousWorn = currentWorn
    }

    // Convert internal content address combined with recieved path value to URI
    fun getUri(path: String?): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(Constants.AUTHORITY_BTHELPER)
            .appendPath(path)
            .build()
    }

    // Convert internal resource address to URI
    private fun resToUri(resId: Int): Uri? {
        try {
            val uri: Uri =
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(Constants.AUTHORITY_BTHELPER)
                    .appendPath(getApplicationContext().getResources().getResourceTypeName(resId))
                    .appendPath(getApplicationContext().getResources().getResourceEntryName(resId))
                    .build()
            return uri
        } catch (e: NotFoundException) {
            return null
        }
    }

    private fun setMetadata(device: BluetoothDevice, key: Int, value: ByteArray): Boolean {
        if (device.getMetadata(key) == null) {
            return device.setMetadata(key, value)
        }
        return true
    }

    // Set metadata (icon, battery, charging status, etc.) for current device
    // and send broadcast that device status has changed
    private fun updatePodsStatus(status: PodsStatus, device: BluetoothDevice?) {
        if (device == null) return

        val airpods: IPods? = status.pods
        if (airpods == null) return
        val single: Boolean = airpods.isSingle
        isSingleDevice = single
        var batteryUnified = 0
        var batteryUnifiedArg = 0
        var chargingMain = false

        if (!isMetaDataSet) {
            isSliceSet =
                setMetadata(
                    device,
                    BluetoothDevice.METADATA_COMPANION_APP,
                    Constants.AUTHORITY_BTHELPER.toByteArray(),
                )
            isSliceSet =
                setMetadata(
                    device,
                    BluetoothDevice.METADATA_SOFTWARE_VERSION,
                    COMPANION_TYPE_NONE.toByteArray(),
                )
            isSliceSet =
                setMetadata(
                    device,
                    BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI,
                    getUri(Constants.PATH_BTHELPER).toString().toByteArray(),
                )
        }

        if (!single) {
            val regularPods: RegularPods = airpods as RegularPods
            if (!isMetaDataSet) {
                isModelDataSet =
                    setMetadata(
                        device,
                        BluetoothDevice.METADATA_MANUFACTURER_NAME,
                        regularPods.menufacturer.toByteArray(),
                    ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_MODEL_NAME,
                            regularPods.model.toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_DEVICE_TYPE,
                            BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET,
                            true.toString().toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                            (regularPods.lowBattThreshold.toString() + "").toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
                            (regularPods.lowBattThreshold.toString() + "").toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
                            (regularPods.lowBattThreshold.toString() + "").toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
                            (regularPods.lowBattThreshold.toString() + "").toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_MAIN_ICON,
                            resToUri(regularPods.drawable).toString().toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON,
                            resToUri(regularPods.leftDrawable).toString().toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON,
                            resToUri(regularPods.rightDrawable).toString().toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_UNTETHERED_CASE_ICON,
                            resToUri(regularPods.caseDrawable).toString().toByteArray(),
                        )
            }

            if (statusChanged) {
                val leftCharging: Boolean = regularPods.isCharging(RegularPods.LEFT)
                val rightCharging: Boolean = regularPods.isCharging(RegularPods.RIGHT)
                val caseCharging: Boolean = regularPods.isCharging(RegularPods.CASE)
                val leftBattery: Int = regularPods.getParsedStatus(false, RegularPods.LEFT)
                val rightBattery: Int = regularPods.getParsedStatus(false, RegularPods.RIGHT)
                val leftBatteryArg: Int = regularPods.getParsedStatus(true, RegularPods.LEFT)
                val rightBatteryArg: Int = regularPods.getParsedStatus(true, RegularPods.RIGHT)
                val caseBattery: Int = regularPods.getParsedStatus(false, RegularPods.CASE)

                device.setMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING,
                    (leftCharging.toString() + "").uppercase(Locale.getDefault()).toByteArray(),
                )
                device.setMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING,
                    (rightCharging.toString() + "").uppercase(Locale.getDefault()).toByteArray(),
                )
                device.setMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING,
                    (caseCharging.toString() + "").uppercase(Locale.getDefault()).toByteArray(),
                )
                device.setMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY,
                    (leftBattery.toString() + "").toByteArray(),
                )
                device.setMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY,
                    (rightBattery.toString() + "").toByteArray(),
                )
                device.setMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY,
                    (caseBattery.toString() + "").toByteArray(),
                )

                chargingMain = leftCharging && rightCharging
                batteryUnified = min(leftBattery.toDouble(), rightBattery.toDouble()).toInt()
                batteryUnifiedArg =
                    min(leftBatteryArg.toDouble(), rightBatteryArg.toDouble()).toInt()
            }
        } else {
            val singlePods: SinglePods = airpods as SinglePods
            if (!isMetaDataSet) {
                isModelDataSet =
                    setMetadata(
                        device,
                        BluetoothDevice.METADATA_MANUFACTURER_NAME,
                        singlePods.menufacturer.toByteArray(),
                    ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_DEVICE_TYPE,
                            BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET,
                            true.toString().toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_MODEL_NAME,
                            singlePods.model.toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                            (singlePods.lowBattThreshold.toString() + "").toByteArray(),
                        ) &&
                        setMetadata(
                            device,
                            BluetoothDevice.METADATA_MAIN_ICON,
                            resToUri(singlePods.drawable).toString().toByteArray(),
                        )
            }
            chargingMain = singlePods.isCharging
            batteryUnified = singlePods.getParsedStatus(true)
            batteryUnifiedArg = singlePods.getParsedStatus(false)
        }

        if (!isMetaDataSet) {
            isMetaDataSet = isSliceSet && isModelDataSet
        }

        if (statusChanged) {
            device.setMetadata(
                BluetoothDevice.METADATA_MAIN_CHARGING,
                (chargingMain.toString() + "").uppercase(Locale.getDefault()).toByteArray(),
            )
            device.setMetadata(
                BluetoothDevice.METADATA_MAIN_BATTERY,
                (batteryUnified.toString() + "").toByteArray(),
            )

            broadcastVendorSpecificEventIntent(
                VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV,
                APPLE,
                BluetoothHeadset.AT_CMD_TYPE_SET,
                batteryUnified,
                batteryUnifiedArg,
                device,
            )

            statusChanged = false
        }
    }

    // Send broadcasts to Android Settings Intelligence, Bluetooth app, System Settings
    // to reflect current device status changes
    private fun broadcastVendorSpecificEventIntent(
        command: String,
        companyId: Int,
        commandType: Int,
        batteryUnified: Int,
        batteryUnifiedArg: Int,
        device: BluetoothDevice,
    ) {
        val arguments =
            arrayOf<Any>(
                1, // Number of key(IndicatorType)/value pairs
                VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL, // IndicatorType:
                // Battery Level
                batteryUnifiedArg, // Battery Level
            )

        // Update battery status for this device
        val intent: Intent = Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD, command)
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, commandType)
        // assert: all elements of args are Serializable
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, arguments)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        intent.putExtra(BluetoothDevice.EXTRA_NAME, device.getName())
        intent.addCategory(
            (BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY +
                "." +
                companyId.toString())
        )
        sendBroadcastAsUser(intent, UserHandle.ALL, Manifest.permission.BLUETOOTH_CONNECT)

        // Broadcast battery level changes
        val batteryIntent: Intent = Intent(ACTION_BATTERY_LEVEL_CHANGED)
        batteryIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        batteryIntent.putExtra(EXTRA_BATTERY_LEVEL, batteryUnified)
        sendBroadcastAsUser(batteryIntent, UserHandle.ALL, Manifest.permission.BLUETOOTH_CONNECT)

        // Update Android Settings Intelligence's battery widget
        val statusIntent: Intent = Intent(ACTION_ASI_UPDATE_BLUETOOTH_DATA).setPackage(PACKAGE_ASI)
        statusIntent.putExtra(ACTION_BATTERY_LEVEL_CHANGED, intent)
        sendBroadcastAsUser(statusIntent, UserHandle.ALL)
    }

    companion object {
        // Check whether current device is single model (e.g. AirPods Max)
        var isSingleDevice: Boolean = false
            private set

        /** A vendor-specific AT command */
        private const val VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV = "+IPHONEACCEV"

        /**
         * Battery level indicator associated with
         * {@link #VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV}
         */
        private const val VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL = 1

        /*
         * Apple, Inc.
         */
        private const val APPLE = 0x004C

        /**
         * Broadcast Action: Indicates the battery level of a remote device has been retrieved for
         * the first time, or changed since the last retrieval
         *
         * <p>Always contains the extra fields {@link BluetoothDevice#EXTRA_DEVICE} and {@link
         * BluetoothDevice#EXTRA_BATTERY_LEVEL}.
         */
        const val ACTION_BATTERY_LEVEL_CHANGED: String =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

        /**
         * Used as an Integer extra field in {@link #ACTION_BATTERY_LEVEL_CHANGED} intent. It
         * contains the most recently retrieved battery level information ranging from 0% to 100%
         * for a remote device, {@link #BATTERY_LEVEL_UNKNOWN} when the valid is unknown or there is
         * an error, {@link #BATTERY_LEVEL_BLUETOOTH_OFF} when the bluetooth is off
         */
        const val EXTRA_BATTERY_LEVEL: String = "android.bluetooth.device.extra.BATTERY_LEVEL"

        // Target Android Settings Intelligence package that have battery widget for data update
        private const val PACKAGE_ASI = "com.google.android.settings.intelligence"

        /**
         * Intent used to broadcast bluetooth data update for the Settings Intelligence package's
         * battery widget
         */
        private const val ACTION_ASI_UPDATE_BLUETOOTH_DATA =
            "batterywidget.impl.action.update_bluetooth_data"

        private const val COMPANION_TYPE_NONE = "COMPANION_NONE"

        /** A vendor-specific command for unsolicited result code. */
        const val VENDOR_RESULT_CODE_COMMAND_ANDROID: String = "+ANDROID"
    }
}
