/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.android.bluetooth.bthelper.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.lsposed.hiddenapibypass.HiddenApiBypass

class BluetoothSocketManager(
    private val serviceScope: CoroutineScope,
    private val onIncomingCall: () -> Unit,
    private val onCallReceived: () -> Unit,
    private val onDeviceConnected: () -> Unit,
    private val onSocketClosed: () -> Unit,
) {

    private var currentSocket: BluetoothSocket? = null
    private var socketJob: Job? = null

    init {
        HiddenApiBypass.addHiddenApiExemptions(Constants.HIDDEN_API_BLUETOOTH_SOCKET)
    }

    @Synchronized
    fun connectToSocket(device: BluetoothDevice?, aacpManager: AACPManager) {
        if (device == null) return
        if (BluetoothConnectionManager.isConnected) return

        val uuid: ParcelUuid = Constants.PODS_UUID

        currentSocket =
            try {
                createBluetoothSocket(device, uuid) ?: return
            } catch (e: Exception) {
                Log.e(TAG, "Error creating bluetooth socket", e)
                return
            }

        val currentSocket = this.currentSocket ?: return

        try {
            runBlocking {
                withTimeout(5000L) {
                    currentSocket.connect()
                    BluetoothConnectionManager.isConnected = true
                    BluetoothConnectionManager.currentSocket = currentSocket
                }
            }

            aacpManager.sendPacket(aacpManager.createHandshakePacket())
            aacpManager.sendSetFeatureFlagsPacket()
            aacpManager.sendNotificationRequest()
            aacpManager.sendRequestProximityKeys(
                (AACPManager.Companion.ProximityKeyType.IRK.value +
                        AACPManager.Companion.ProximityKeyType.ENC_KEY.value)
                    .toByte()
            )

            socketJob =
                serviceScope.launch {
                    aacpManager.sendPacket(aacpManager.createHandshakePacket())
                    delay(200)
                    aacpManager.sendSetFeatureFlagsPacket()
                    delay(200)
                    aacpManager.sendNotificationRequest()
                    delay(200)
                    aacpManager.sendRequestProximityKeys(
                        (AACPManager.Companion.ProximityKeyType.IRK.value +
                                AACPManager.Companion.ProximityKeyType.ENC_KEY.value)
                            .toByte()
                    )
                    onIncomingCall()

                    Handler(Looper.getMainLooper())
                        .postDelayed(
                            {
                                aacpManager.sendPacket(aacpManager.createHandshakePacket())
                                aacpManager.sendSetFeatureFlagsPacket()
                                aacpManager.sendNotificationRequest()
                                aacpManager.sendRequestProximityKeys(
                                    AACPManager.Companion.ProximityKeyType.IRK.value
                                )
                                onCallReceived()
                            },
                            5000,
                        )

                    onDeviceConnected()

                    while (currentSocket.isConnected) {
                        try {
                            val buffer = ByteArray(1024)
                            val bytesRead = currentSocket.inputStream.read(buffer)
                            if (bytesRead > 0) {
                                val data = buffer.copyOfRange(0, bytesRead)
                                aacpManager.receivePacket(data)
                            } else if (bytesRead == -1) {
                                currentSocket.close()
                                break
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading from socket", e)
                            currentSocket.close()
                            break
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to socket", e)
            try {
                currentSocket.close()
            } catch (closeException: Exception) {
                Log.e(TAG, "Error closing socket", closeException)
            }
        } finally {
            onSocketClosed()
        }
    }

    @Synchronized
    private fun createBluetoothSocket(device: BluetoothDevice, uuid: ParcelUuid): BluetoothSocket? {
        val type = 3 // L2CAP
        val constructorSpecs =
            listOf(
                arrayOf(device, type, true, true, 0x1001, uuid),
                arrayOf(device, type, 1, true, true, 0x1001, uuid),
                arrayOf(type, 1, true, true, device, 0x1001, uuid),
                arrayOf(type, true, true, device, 0x1001, uuid),
            )

        for ((index, params) in constructorSpecs.withIndex()) {
            try {
                return HiddenApiBypass.newInstance(BluetoothSocket::class.java, *params)
                    as BluetoothSocket
            } catch (e: Exception) {
                Log.e(TAG, "Error creating bluetooth socket with constructor $index", e)
            }
        }
        return null
    }

    @Synchronized
    fun disconnect() {
        try {
            socketJob?.cancel()
            currentSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket", e)
        }
    }

    companion object {
        const val TAG: String = "BluetoothSocketManager"
    }
}
