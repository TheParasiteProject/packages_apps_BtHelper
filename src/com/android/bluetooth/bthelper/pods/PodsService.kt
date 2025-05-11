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
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.IBinder
import android.os.UserHandle
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.getSharedPreferences
import com.android.bluetooth.bthelper.isLowLatencySupported
import com.android.bluetooth.bthelper.pods.models.IPods
import com.android.bluetooth.bthelper.pods.models.RegularPods
import com.android.bluetooth.bthelper.pods.models.RegularPodsMetadata
import com.android.bluetooth.bthelper.pods.models.SinglePods
import com.android.bluetooth.bthelper.pods.models.SinglePodsMetadata
import com.android.bluetooth.bthelper.setSingleDevice
import com.android.bluetooth.bthelper.utils.MediaControl
import com.android.bluetooth.bthelper.utils.setMetadataBoolean
import com.android.bluetooth.bthelper.utils.setMetadataInt
import com.android.bluetooth.bthelper.utils.setMetadataString
import com.android.bluetooth.bthelper.utils.setMetadataUri
import com.android.bluetooth.bthelper.utils.setMetadataValue
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
                if (this.isLowLatencySupported()) {
                    setLowLatencyAudio()
                }
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

    // Set Low Latency Audio mode to current device
    fun setLowLatencyAudio() {
        val sp = this.getSharedPreferences()

        mCurrentDevice?.setLowLatencyAudioAllowed(
            sp.getBoolean(Constants.KEY_LOW_LATENCY_AUDIO, false)
        )
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
                        handlePlayPause(status)
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
    private fun handlePlayPause(status: PodsStatus) {
        val sp = this.getSharedPreferences()

        val onePodMode: Boolean = sp.getBoolean(Constants.KEY_ONEPOD_MODE, false)
        val autoPlay: Boolean = sp.getBoolean(Constants.KEY_AUTO_PLAY, false)
        val autoPause: Boolean = sp.getBoolean(Constants.KEY_AUTO_PAUSE, false)
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
                    .appendPath(resources.getResourceTypeName(resId))
                    .appendPath(resources.getResourceEntryName(resId))
                    .build()
            return uri
        } catch (e: NotFoundException) {
            return null
        }
    }

    private fun setInitialMetadata(device: BluetoothDevice): Boolean {
        return device.setMetadataString(
            BluetoothDevice.METADATA_COMPANION_APP,
            Constants.AUTHORITY_BTHELPER,
        ) &&
            device.setMetadataString(
                BluetoothDevice.METADATA_SOFTWARE_VERSION,
                COMPANION_TYPE_NONE,
            ) &&
            device.setMetadataUri(
                BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI,
                getUri(Constants.PATH_BTHELPER),
            )
    }

    private fun setRegularPodsMetadata(
        device: BluetoothDevice,
        metadata: RegularPodsMetadata,
    ): Boolean {
        return device.setMetadataString(
            BluetoothDevice.METADATA_MANUFACTURER_NAME,
            metadata.manufacturer,
        ) &&
            device.setMetadataString(BluetoothDevice.METADATA_MODEL_NAME, metadata.model) &&
            device.setMetadataValue(
                BluetoothDevice.METADATA_DEVICE_TYPE,
                BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray(),
            ) &&
            device.setMetadataBoolean(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET, true) &&
            device.setMetadataInt(
                BluetoothDevice.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            ) &&
            device.setMetadataInt(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            ) &&
            device.setMetadataInt(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            ) &&
            device.setMetadataInt(
                BluetoothDevice.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            ) &&
            device.setMetadataUri(
                BluetoothDevice.METADATA_MAIN_ICON,
                resToUri(metadata.drawable),
            ) &&
            device.setMetadataUri(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON,
                resToUri(metadata.leftDrawable),
            ) &&
            device.setMetadataUri(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON,
                resToUri(metadata.rightDrawable),
            ) &&
            device.setMetadataUri(
                BluetoothDevice.METADATA_UNTETHERED_CASE_ICON,
                resToUri(metadata.caseDrawable),
            )
    }

    private fun setSinglePodsMetadata(
        device: BluetoothDevice,
        metadata: SinglePodsMetadata,
    ): Boolean {
        return device.setMetadataString(
            BluetoothDevice.METADATA_MANUFACTURER_NAME,
            metadata.manufacturer,
        ) &&
            device.setMetadataValue(
                BluetoothDevice.METADATA_DEVICE_TYPE,
                BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray(),
            ) &&
            device.setMetadataBoolean(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET, true) &&
            device.setMetadataString(BluetoothDevice.METADATA_MODEL_NAME, metadata.model) &&
            device.setMetadataInt(
                BluetoothDevice.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            ) &&
            device.setMetadataUri(BluetoothDevice.METADATA_MAIN_ICON, resToUri(metadata.drawable))
    }

    // Set metadata (icon, battery, charging status, etc.) for current device
    // and send broadcast that device status has changed
    private fun updatePodsStatus(status: PodsStatus, device: BluetoothDevice?) {
        if (device == null) return

        val sp = this.getSharedPreferences()
        val airpods: IPods? = status.pods
        if (airpods == null) return
        val single: Boolean = airpods.isSingle
        sp.setSingleDevice(single)
        var batteryUnified = 0
        var chargingMain = false

        if (!isMetaDataSet) {
            isSliceSet = setInitialMetadata(device)
        }

        if (!single) {
            val regularPods: RegularPods = airpods as RegularPods
            if (!isMetaDataSet) {
                val metadata =
                    RegularPodsMetadata(
                        manufacturer = regularPods.manufacturer,
                        model = regularPods.model,
                        lowBattThreshold = regularPods.lowBattThreshold,
                        drawable = regularPods.drawable,
                        leftDrawable = regularPods.leftDrawable,
                        rightDrawable = regularPods.rightDrawable,
                        caseDrawable = regularPods.caseDrawable,
                    )
                isModelDataSet = setRegularPodsMetadata(device, metadata)
            }

            if (statusChanged) {
                val leftCharging: Boolean = regularPods.isCharging(RegularPods.LEFT)
                val rightCharging: Boolean = regularPods.isCharging(RegularPods.RIGHT)
                val caseCharging: Boolean = regularPods.isCharging(RegularPods.CASE)
                val leftBattery: Int = regularPods.getParsedStatus(RegularPods.LEFT)
                val rightBattery: Int = regularPods.getParsedStatus(RegularPods.RIGHT)
                val caseBattery: Int = regularPods.getParsedStatus(RegularPods.CASE)

                device.setMetadataBoolean(
                    BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING,
                    leftCharging,
                )
                device.setMetadataBoolean(
                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING,
                    rightCharging,
                )
                device.setMetadataBoolean(
                    BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING,
                    caseCharging,
                )
                device.setMetadataInt(BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY, leftBattery)
                device.setMetadataInt(
                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY,
                    rightBattery,
                )
                device.setMetadataInt(BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY, caseBattery)

                chargingMain = leftCharging && rightCharging
                batteryUnified = min(leftBattery.toDouble(), rightBattery.toDouble()).toInt()
            }
        } else {
            val singlePods: SinglePods = airpods as SinglePods
            if (!isMetaDataSet) {
                val metadata =
                    SinglePodsMetadata(
                        manufacturer = singlePods.manufacturer,
                        model = singlePods.model,
                        lowBattThreshold = singlePods.lowBattThreshold,
                        drawable = singlePods.drawable,
                    )
                isModelDataSet = setSinglePodsMetadata(device, metadata)
            }
            chargingMain = singlePods.isCharging
            batteryUnified = singlePods.getParsedStatus()
        }

        if (!isMetaDataSet) {
            isMetaDataSet = isSliceSet && isModelDataSet
        }

        if (statusChanged) {
            device.setMetadataBoolean(BluetoothDevice.METADATA_MAIN_CHARGING, chargingMain)
            device.setMetadataInt(BluetoothDevice.METADATA_MAIN_BATTERY, batteryUnified)

            broadcastHfIndicatorEventIntent(batteryUnified, device)

            statusChanged = false
        }
    }

    // Send broadcasts to Android Settings Intelligence, Bluetooth app, System Settings
    // to reflect current device status changes
    private fun broadcastHfIndicatorEventIntent(batteryUnified: Int, device: BluetoothDevice) {
        // Update battery status for this device
        val intent: Intent =
            Intent(ACTION_HF_INDICATORS_VALUE_CHANGED).apply {
                putExtra(EXTRA_HF_INDICATORS_IND_ID, HF_INDICATOR_BATTERY_LEVEL_STATUS)
                putExtra(EXTRA_HF_INDICATORS_IND_VALUE, batteryUnified)
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            }
        sendBroadcastAsUser(intent, UserHandle.ALL, Manifest.permission.BLUETOOTH_CONNECT)

        // Broadcast battery level changes
        val batteryIntent: Intent =
            Intent(ACTION_BATTERY_LEVEL_CHANGED).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                putExtra(EXTRA_BATTERY_LEVEL, batteryUnified)
            }
        sendBroadcastAsUser(batteryIntent, UserHandle.ALL, Manifest.permission.BLUETOOTH_CONNECT)

        // Update Android Settings Intelligence's battery widget
        val statusIntent: Intent =
            Intent(ACTION_ASI_UPDATE_BLUETOOTH_DATA).apply {
                setPackage(PACKAGE_ASI)
                putExtra(ACTION_BATTERY_LEVEL_CHANGED, intent)
            }
        sendBroadcastAsUser(statusIntent, UserHandle.ALL)
    }

    companion object {
        /**
         * Intent used to broadcast the headset's indicator status
         *
         * <p>This intent will have 3 extras:
         * <ul>
         * <li> {@link #EXTRA_HF_INDICATORS_IND_ID} - The Assigned number of headset Indicator which
         *   is supported by the headset ( as indicated by AT+BIND command in the SLC sequence) or
         *   whose value is changed (indicated by AT+BIEV command) </li>
         * <li> {@link #EXTRA_HF_INDICATORS_IND_VALUE} - Updated value of headset indicator. </li>
         * <li> {@link BluetoothDevice#EXTRA_DEVICE} - Remote device. </li>
         * </ul>
         *
         * <p>{@link #EXTRA_HF_INDICATORS_IND_ID} is defined by Bluetooth SIG and each of the
         * indicators are given an assigned number. Below shows the assigned number of Indicator
         * added so far
         * - Enhanced Safety - 1, Valid Values: 0 - Disabled, 1 - Enabled
         * - Battery Level - 2, Valid Values: 0~100 - Remaining level of Battery
         */
        private const val ACTION_HF_INDICATORS_VALUE_CHANGED =
            "android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED"

        /**
         * A int extra field in {@link #ACTION_HF_INDICATORS_VALUE_CHANGED} intents that contains
         * the assigned number of the headset indicator as defined by Bluetooth SIG that is being
         * sent. Value range is 0-65535 as defined in HFP 1.7
         */
        private const val EXTRA_HF_INDICATORS_IND_ID =
            "android.bluetooth.headset.extra.HF_INDICATORS_IND_ID"

        /**
         * A int extra field in {@link #ACTION_HF_INDICATORS_VALUE_CHANGED} intents that contains
         * the value of the Headset indicator that is being sent.
         */
        private const val EXTRA_HF_INDICATORS_IND_VALUE =
            "android.bluetooth.headset.extra.HF_INDICATORS_IND_VALUE"

        // Match up with bthf_hf_ind_type_t of bt_hf.h
        private const val HF_INDICATOR_BATTERY_LEVEL_STATUS = 2

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
    }
}
