/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothAssignedNumbers
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.getSharedPreferences
import com.android.bluetooth.bthelper.utils.models.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Manager for Bluetooth Low Energy scanning operations specifically for AirPods */
@OptIn(ExperimentalEncodingApi::class)
class BLEManager(private val context: Context, private val keyStorageManager: KeyStorageManager) {

    data class AirPodsStatus(
        val address: String = "",
        val lastSeen: Long = System.currentTimeMillis(),
        val paired: Boolean = false,
        val model: IPods? = null,
        val leftBattery: Int = BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
        val rightBattery: Int = BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
        val caseBattery: Int = BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
        val headsetBattery: Int = BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
        val isLeftInEar: Boolean = false,
        val isRightInEar: Boolean = false,
        val isLeftCharging: Boolean = false,
        val isRightCharging: Boolean = false,
        val isCaseCharging: Boolean = false,
        val isHeadsetCharging: Boolean = false,
        val lidOpen: Boolean = false,
        val connectionState: String = Constants.STATE_UNKNOWN,
    )

    fun getMostRecentStatus(): AirPodsStatus {
        return deviceStatusMap.values.maxByOrNull { it.lastSeen } ?: AirPodsStatus()
    }

    interface AirPodsStatusListener {
        fun onDeviceStatusChanged(currStatus: AirPodsStatus, prevStatus: AirPodsStatus?)

        fun onBroadcastFromNewAddress(status: AirPodsStatus)

        fun onLidStateChanged(status: AirPodsStatus?, lidOpen: Boolean)

        fun onEarStateChanged(status: AirPodsStatus, leftInEar: Boolean, rightInEar: Boolean)

        fun onBatteryChanged(status: AirPodsStatus)
    }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var airPodsStatusListener: AirPodsStatusListener? = null
    private val deviceStatusMap = mutableMapOf<String, AirPodsStatus>()
    private val verifiedAddresses = mutableSetOf<String>()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences()
    private var currentGlobalLidState: Boolean? = null
    private var lastBroadcastTime: Long = 0
    private val processedAddresses = mutableSetOf<String>()

    private val cleanupHandler = Handler(Looper.getMainLooper())
    private val cleanupRunnable =
        object : Runnable {
            override fun run() {
                cleanupStaleDevices()
                checkLidStateTimeout()
                cleanupHandler.postDelayed(this, CLEANUP_INTERVAL_MS)
            }
        }

    fun setAirPodsStatusListener(listener: AirPodsStatusListener) {
        airPodsStatusListener = listener
    }

    fun startScanning() {
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter

            if (btAdapter == null) {
                return
            }

            if (bluetoothLeScanner != null && scanCallback != null) {
                bluetoothLeScanner?.stopScan(scanCallback)
                scanCallback = null
            }

            if (!btAdapter.isEnabled) {
                return
            }

            bluetoothLeScanner = btAdapter.bluetoothLeScanner

            val scanSettings =
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1)
                    .build()

            val manufacturerData = ByteArray(27)
            val manufacturerDataMask = ByteArray(27)

            manufacturerData[0] = 7
            manufacturerData[1] = 25

            manufacturerDataMask[0] = -1
            manufacturerDataMask[1] = -1

            val scanFilter =
                ScanFilter.Builder()
                    .setManufacturerData(
                        BluetoothAssignedNumbers.APPLE,
                        manufacturerData,
                        manufacturerDataMask,
                    )
                    .build()

            scanCallback =
                object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        processScanResult(result)
                    }

                    override fun onBatchScanResults(results: List<ScanResult>) {
                        processedAddresses.clear()
                        for (result in results) {
                            processScanResult(result)
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {}
                }

            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)

            cleanupHandler.postDelayed(cleanupRunnable, CLEANUP_INTERVAL_MS)
        } catch (t: Throwable) {}
    }

    fun stopScanning() {
        try {
            if (bluetoothLeScanner != null && scanCallback != null) {
                bluetoothLeScanner?.stopScan(scanCallback)
                scanCallback = null
            }

            cleanupHandler.removeCallbacksAndMessages(null)
        } catch (t: Throwable) {}
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getKeyFromPreferences(type: AACPManager.Companion.ProximityKeyType): ByteArray? {
        val encryptedKeyBase64 = sharedPreferences.getString(type.name + "_encrypted", null)
        val ivBase64 = sharedPreferences.getString(type.name + "_iv", null)

        if (encryptedKeyBase64 != null && ivBase64 != null) {
            return try {
                val encryptedKey = Base64.decode(encryptedKeyBase64)
                val iv = Base64.decode(ivBase64)
                keyStorageManager.decrypt(iv, encryptedKey)
            } catch (e: Exception) {
                null // Decryption failed
            }
        }

        return null
    }

    private fun decryptLastBytes(data: ByteArray, key: ByteArray): ByteArray? {
        return try {
            if (data.size < 16) {
                return null
            }

            val block = data.copyOfRange(data.size - 16, data.size)
            val cipher = Cipher.getInstance(Constants.CRYPTO_AES_ECB_NO_PADDING)
            val secretKey = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            cipher.doFinal(block)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatBattery(byteVal: Int): Pair<Boolean, Int> {
        val charging = (byteVal and 0x80) != 0
        val level = byteVal and 0x7F
        return Pair(charging, level)
    }

    private fun processScanResult(result: ScanResult) {
        try {
            val scanRecord = result.scanRecord ?: return
            val address = result.device.address

            if (processedAddresses.contains(address)) {
                return
            }

            val manufacturerData = scanRecord.getManufacturerSpecificData(76) ?: return
            if (manufacturerData.size <= 20) return

            if (!verifiedAddresses.contains(address)) {
                val irk = getKeyFromPreferences(AACPManager.Companion.ProximityKeyType.IRK)
                if (irk == null || !BluetoothCryptography.verifyRPA(address, irk)) {
                    return
                }
                verifiedAddresses.add(address)
            }

            processedAddresses.add(address)
            lastBroadcastTime = System.currentTimeMillis()

            val encryptionKey =
                getKeyFromPreferences(AACPManager.Companion.ProximityKeyType.ENC_KEY)
            val decryptedData =
                if (encryptionKey != null) decryptLastBytes(manufacturerData, encryptionKey)
                else null
            val parsedStatus =
                if (decryptedData != null && decryptedData.size == 16) {
                    AirPodsParser.parseProximityMessageWithDecryption(
                        address,
                        manufacturerData,
                        decryptedData,
                    )
                } else {
                    AirPodsParser.parseProximityMessage(address, manufacturerData)
                }

            val previousStatus = deviceStatusMap[address]
            deviceStatusMap[address] = parsedStatus

            airPodsStatusListener?.let { listener ->
                if (previousStatus == null) {
                    listener.onBroadcastFromNewAddress(parsedStatus)

                    if (
                        currentGlobalLidState == null ||
                            currentGlobalLidState != parsedStatus.lidOpen
                    ) {
                        currentGlobalLidState = parsedStatus.lidOpen
                        listener.onLidStateChanged(parsedStatus, parsedStatus.lidOpen)
                    }
                } else {
                    if (parsedStatus != previousStatus) {
                        listener.onDeviceStatusChanged(parsedStatus, previousStatus)
                    }

                    if (parsedStatus.lidOpen != previousStatus.lidOpen) {
                        val previousGlobalState = currentGlobalLidState
                        currentGlobalLidState = parsedStatus.lidOpen

                        if (previousGlobalState != parsedStatus.lidOpen) {
                            listener.onLidStateChanged(parsedStatus, parsedStatus.lidOpen)
                        }
                    }

                    if (
                        parsedStatus.isLeftInEar != previousStatus.isLeftInEar ||
                            parsedStatus.isRightInEar != previousStatus.isRightInEar
                    ) {
                        listener.onEarStateChanged(
                            parsedStatus,
                            parsedStatus.isLeftInEar,
                            parsedStatus.isRightInEar,
                        )
                    }

                    if (
                        parsedStatus.leftBattery != previousStatus.leftBattery ||
                            parsedStatus.rightBattery != previousStatus.rightBattery ||
                            parsedStatus.caseBattery != previousStatus.caseBattery
                    ) {
                        listener.onBatteryChanged(parsedStatus)
                    }
                }
            }
        } catch (t: Throwable) {}
    }

    private fun cleanupStaleDevices() {
        val now = System.currentTimeMillis()
        val staleCutoff = now - STALE_DEVICE_TIMEOUT_MS

        val staleDevices = deviceStatusMap.filter { it.value.lastSeen < staleCutoff }

        for (device in staleDevices) {
            deviceStatusMap.remove(device.key)
        }
    }

    private fun checkLidStateTimeout() {
        val currentTime = System.currentTimeMillis()
        if (
            currentTime - lastBroadcastTime > LID_CLOSE_TIMEOUT_MS && currentGlobalLidState == true
        ) {
            currentGlobalLidState = false
            airPodsStatusListener?.onLidStateChanged(null, false)
        }
    }

    companion object {
        private const val TAG = "AirPodsBLE"
        private const val CLEANUP_INTERVAL_MS = 30000L
        private const val STALE_DEVICE_TIMEOUT_MS = 60000L
        private const val LID_CLOSE_TIMEOUT_MS = 2000L
    }
}
