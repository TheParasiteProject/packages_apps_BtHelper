/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass

/* This is a very basic ATT (Attribute Protocol) implementation. I have only implemented
 * what is necessary for LibrePods to function, i.e. reading and writing characteristics,
 * and receiving notifications. It is not a complete implementation of the ATT protocol.
 */

enum class ATTHandles(val value: Int) {
    TRANSPARENCY(0x18),
    LOUD_SOUND_REDUCTION(0x1B),
    HEARING_AID(0x2A),
}

enum class ATTCCCDHandles(val value: Int) {
    TRANSPARENCY(ATTHandles.TRANSPARENCY.value + 1),
    LOUD_SOUND_REDUCTION(ATTHandles.LOUD_SOUND_REDUCTION.value + 1),
    HEARING_AID(ATTHandles.HEARING_AID.value + 1),
}

class ATTManager(private val serviceScope: CoroutineScope) {
    companion object {
        private const val TAG = "ATTManager"

        private const val OPCODE_READ_REQUEST: Byte = 0x0A
        private const val OPCODE_WRITE_REQUEST: Byte = 0x12
        private const val OPCODE_HANDLE_VALUE_NTF: Byte = 0x1B
    }

    private var currentSocket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private val listeners = mutableMapOf<Int, MutableList<(ByteArray) -> Unit>>()
    private var notificationJob: Job? = null

    // queue for non-notification PDUs (responses to requests)
    private val responses = LinkedBlockingQueue<ByteArray>()

    init {
        HiddenApiBypass.addHiddenApiExemptions(Constants.HIDDEN_API_BLUETOOTH_SOCKET)
    }

    @Synchronized
    fun connectToSocket(device: BluetoothDevice?) {
        if (device == null) return
        if (BluetoothConnectionManager.isAttConnected) return

        val uuid = ParcelUuid.fromString("00000000-0000-0000-0000-000000000000")

        currentSocket =
            try {
                createBluetoothSocket(device, uuid) ?: return
            } catch (e: Exception) {
                Log.e(TAG, "Error creating bluetooth socket", e)
                return
            }

        val currentSocket = this.currentSocket ?: return

        runBlocking {
            withTimeout(5000L) {
                currentSocket.connect()
                input = currentSocket.inputStream
                output = currentSocket.outputStream
                BluetoothConnectionManager.isAttConnected = true
                BluetoothConnectionManager.currentAttSocket = currentSocket
            }
        }

        notificationJob =
            serviceScope.launch {
                while (currentSocket.isConnected == true) {
                    try {
                        val pdu = readPDU() ?: break
                        if (pdu.isNotEmpty() && pdu[0] == OPCODE_HANDLE_VALUE_NTF) {
                            // notification -> dispatch to listeners
                            val handle =
                                (pdu[1].toInt() and 0xFF) or ((pdu[2].toInt() and 0xFF) shl 8)
                            val value = pdu.copyOfRange(3, pdu.size)
                            listeners[handle]?.forEach { listener ->
                                try {
                                    listener(value)
                                } catch (e: Exception) {}
                            }
                        } else {
                            // not a notification -> treat as a response for pending request(s)
                            responses.put(pdu)
                        }
                    } catch (e: Exception) {
                        if (currentSocket.isConnected != true) break
                    }
                }
            }
    }

    fun disconnect() {
        try {
            notificationJob?.cancel()
            currentSocket?.close()
        } catch (e: Exception) {}
    }

    fun registerListener(handle: ATTHandles, listener: (ByteArray) -> Unit) {
        listeners.getOrPut(handle.value) { mutableListOf() }.add(listener)
    }

    fun unregisterListener(handle: ATTHandles, listener: (ByteArray) -> Unit) {
        listeners[handle.value]?.remove(listener)
    }

    fun enableNotifications(handle: ATTHandles) {
        write(ATTCCCDHandles.valueOf(handle.name), byteArrayOf(0x01, 0x00))
    }

    fun read(handle: ATTHandles): ByteArray? {
        val lsb = (handle.value and 0xFF).toByte()
        val msb = ((handle.value shr 8) and 0xFF).toByte()
        val pdu = byteArrayOf(OPCODE_READ_REQUEST, lsb, msb)
        writeRaw(pdu)
        // wait for response placed into responses queue by the reader coroutine
        return readResponse()
    }

    fun write(handle: ATTHandles, value: ByteArray) {
        val lsb = (handle.value and 0xFF).toByte()
        val msb = ((handle.value shr 8) and 0xFF).toByte()
        val pdu = byteArrayOf(OPCODE_WRITE_REQUEST, lsb, msb) + value
        writeRaw(pdu)
        // usually a Write Response (0x13) will arrive; wait for it (but discard return)
        readResponse()
    }

    fun write(handle: ATTCCCDHandles, value: ByteArray) {
        val lsb = (handle.value and 0xFF).toByte()
        val msb = ((handle.value shr 8) and 0xFF).toByte()
        val pdu = byteArrayOf(OPCODE_WRITE_REQUEST, lsb, msb) + value
        writeRaw(pdu)
        // usually a Write Response (0x13) will arrive; wait for it (but discard return)
        readResponse()
    }

    private fun writeRaw(pdu: ByteArray) {
        output?.write(pdu)
        output?.flush()
        Log.d(TAG, "writeRaw: ${pdu.joinToString(" ") { String.format("%02X", it) }}")
    }

    // rename / specialize: read raw PDU directly from input stream (blocking)
    private fun readPDU(): ByteArray? {
        val inp = input ?: return null
        val buffer = ByteArray(512)
        val len = inp.read(buffer)
        if (len == -1) {
            disconnect()
            return null
        }
        val data = buffer.copyOfRange(0, len)
        return data
    }

    // wait for a response PDU produced by the background reader
    private fun readResponse(timeoutMs: Long = 2000): ByteArray? {
        try {
            val resp = responses.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: return null
            return resp.copyOfRange(1, resp.size)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
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
}
