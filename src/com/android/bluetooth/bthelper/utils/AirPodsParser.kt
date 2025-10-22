/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothDevice
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.utils.models.*

object AirPodsParser {

    fun parseProximityMessage(address: String, data: ByteArray): BLEManager.AirPodsStatus {
        val paired = data[2].toInt() == 1

        val color = data[9].toInt()
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = getModel(modelId, color)

        val status = data[5].toInt() and 0xFF
        val podsBattery = data[6].toInt() and 0xFF
        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val conn = getConnectionState(data[10].toInt())

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftBatteryNibble =
            if (isFlipped) (podsBattery shr 4) and 0x0F else podsBattery and 0x0F
        val rightBatteryNibble =
            if (isFlipped) podsBattery and 0x0F else (podsBattery shr 4) and 0x0F

        val caseBattery = flagsCase and 0x0F
        val flags = (flagsCase shr 4) and 0x0F

        val isLeftCharging = if (isFlipped) (flags and 0x02) != 0 else (flags and 0x01) != 0
        val isRightCharging = if (isFlipped) (flags and 0x01) != 0 else (flags and 0x02) != 0
        val isCaseCharging = (flags and 0x04) != 0

        val lidOpen = ((lid shr 3) and 0x01) == 0

        fun decodeBattery(n: Int): Int =
            when (n) {
                in 0x0..0x9 -> n * 10
                in 0xA..0xE -> 100
                0xF -> BluetoothDevice.BATTERY_LEVEL_UNKNOWN
                else -> BluetoothDevice.BATTERY_LEVEL_UNKNOWN
            }

        return BLEManager.AirPodsStatus(
            address = address,
            lastSeen = System.currentTimeMillis(),
            paired = paired,
            model = model,
            leftBattery = decodeBattery(leftBatteryNibble),
            rightBattery = decodeBattery(rightBatteryNibble),
            caseBattery = decodeBattery(caseBattery),
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            lidOpen = lidOpen,
            connectionState = conn,
        )
    }

    fun parseProximityMessageWithDecryption(
        address: String,
        data: ByteArray,
        decrypted: ByteArray,
    ): BLEManager.AirPodsStatus {
        val paired = data[2].toInt() == 1

        val color = data[9].toInt()
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = getModel(modelId, color)

        val status = data[5].toInt() and 0xFF
        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val conn = getConnectionState(data[10].toInt())

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftByteIndex = if (isFlipped) 2 else 1
        val rightByteIndex = if (isFlipped) 1 else 2

        val rawLeftBatteryByte = decrypted[leftByteIndex].toInt() and 0xFF
        val (isLeftCharging, rawLeftBattery) = formatBattery(rawLeftBatteryByte)
        val leftBattery =
            if (rawLeftBatteryByte == 0xFF || (isLeftCharging && rawLeftBattery == 127)) {
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN
            } else {
                rawLeftBattery
            }

        val rawRightBatteryByte = decrypted[rightByteIndex].toInt() and 0xFF
        val (isRightCharging, rawRightBattery) = formatBattery(rawRightBatteryByte)
        val rightBattery =
            if (rawRightBatteryByte == 0xFF || (isRightCharging && rawRightBattery == 127)) {
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN
            } else {
                rawRightBattery
            }

        val rawCaseBatteryByte = decrypted[3].toInt() and 0xFF
        val (isCaseCharging, rawCaseBattery) = formatBattery(rawCaseBatteryByte)
        val caseBattery =
            if (rawCaseBatteryByte == 0xFF || (isCaseCharging && rawCaseBattery == 127)) {
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN
            } else {
                rawCaseBattery
            }

        val lidOpen = ((lid shr 3) and 0x01) == 0

        return BLEManager.AirPodsStatus(
            address = address,
            lastSeen = System.currentTimeMillis(),
            paired = paired,
            model = model,
            leftBattery = leftBattery,
            rightBattery = rightBattery,
            caseBattery = caseBattery,
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            lidOpen = lidOpen,
            connectionState = conn,
        )
    }

    private fun getModel(modelId: Int, color: Int): IPods? {
        return when (modelId) {
            0x0220 -> AirPods1(color)
            0x0F20 -> AirPods2(color)
            0x1320 -> AirPods3(color)
            0x1920 -> AirPods4(color)
            0x1B20 -> AirPods4Anc(color)
            0x0E20 -> AirPodsPro(color)
            0x1420 -> AirPodsPro2(color)
            0x2420 -> AirPodsPro2UsbC(color)
            0x2720 -> AirPodsPro3(color)
            0x0A20 -> AirPodsMax(color)
            0x1F20 -> AirPodsMaxUsbC(color)
            else -> null
        }
    }

    private fun getConnectionState(data: Int): String {
        return when (data) {
            0x00 -> Constants.STATE_DISCONNECTED
            0x04 -> Constants.STATE_IDLE
            0x05 -> Constants.STATE_MUSIC
            0x06 -> Constants.STATE_CALL
            0x07 -> Constants.STATE_RINGING
            0x09 -> Constants.STATE_HANGING_UP
            0xFF -> Constants.STATE_UNKNOWN
            else -> Constants.STATE_UNKNOWN
        }
    }

    private fun formatBattery(byteVal: Int): Pair<Boolean, Int> {
        val charging = (byteVal and 0x80) != 0
        val level = byteVal and 0x7F
        return Pair(charging, level)
    }
}
