/*
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable

enum class Enums(val value: ByteArray) {
    NOISE_CANCELLATION(Capabilities.NOISE_CANCELLATION),
    PREFIX(byteArrayOf(0x04, 0x00, 0x04, 0x00)),
    SETTINGS(byteArrayOf(0x09, 0x00)),
    NOISE_CANCELLATION_PREFIX(PREFIX.value + SETTINGS.value + NOISE_CANCELLATION.value),
    CONVERSATION_AWARENESS_RECEIVE_PREFIX(PREFIX.value + byteArrayOf(0x4b, 0x00, 0x02, 0x00)),
}

object BatteryComponent {
    const val HEADSET = 1
    const val RIGHT = 2
    const val LEFT = 4
    const val CASE = 8
}

object BatteryStatus {
    const val CHARGING = 1
    const val NOT_CHARGING = 2
    const val DISCONNECTED = 4
}

data class Battery(val component: Int, val level: Int, val status: Int) : Parcelable {
    fun getComponentName(): String? {
        return when (component) {
            BatteryComponent.HEADSET -> "HEADSET"
            BatteryComponent.LEFT -> "LEFT"
            BatteryComponent.RIGHT -> "RIGHT"
            BatteryComponent.CASE -> "CASE"
            else -> null
        }
    }

    fun getStatusName(): String? {
        return when (status) {
            BatteryStatus.CHARGING -> "CHARGING"
            BatteryStatus.NOT_CHARGING -> "NOT_CHARGING"
            BatteryStatus.DISCONNECTED -> "DISCONNECTED"
            else -> null
        }
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeInt(component)
            writeInt(level)
            writeInt(status)
        }
    }
}

enum class NoiseControlMode {
    OFF,
    NOISE_CANCELLATION,
    TRANSPARENCY,
    ADAPTIVE,
}

class AirPodsNotifications {
    class EarDetection {
        private val notificationBit = Capabilities.EAR_DETECTION
        private val notificationPrefix = Enums.PREFIX.value + notificationBit

        var status: List<Byte> = listOf(0x01, 0x01)

        fun createEarDetectionData(leftInEar: Boolean, rightInEar: Boolean): ByteArray {
            val leftStatus = if (leftInEar) byteArrayOf(0x00) else byteArrayOf(0x01)
            val rightStatus = if (rightInEar) byteArrayOf(0x00) else byteArrayOf(0x01)
            return notificationPrefix + byteArrayOf(0x00) + leftStatus + rightStatus
        }

        fun setStatus(data: ByteArray) {
            status = listOf(data[6], data[7])
        }

        fun isEarDetectionData(data: ByteArray): Boolean {
            if (data.size != 8) {
                return false
            }
            val prefixHex = notificationPrefix.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }
    }

    class ANC {
        private val notificationPrefix = Enums.NOISE_CANCELLATION_PREFIX.value

        var status: Int = 1
            private set

        fun isANCData(data: ByteArray): Boolean {
            if (data.size != 11) {
                return false
            }
            val prefixHex = notificationPrefix.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }

        fun setStatus(data: ByteArray) {
            when (data.size) {
                // if the whole packet is given
                11 -> {
                    status = data[7].toInt()
                }
                // if only the data is given
                1 -> {
                    status = data[0].toInt()
                }
                // if the value of control command is given
                4 -> {
                    status = data[0].toInt()
                }
            }
        }

        val name: String =
            when (status) {
                1 -> "OFF"
                2 -> "ON"
                3 -> "TRANSPARENCY"
                4 -> "ADAPTIVE"
                else -> "UNKNOWN"
            }
    }

    class BatteryNotification {
        private var first: Battery =
            Battery(
                BatteryComponent.LEFT,
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
                BatteryStatus.DISCONNECTED,
            )
        private var second: Battery =
            Battery(
                BatteryComponent.RIGHT,
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
                BatteryStatus.DISCONNECTED,
            )
        private var case: Battery =
            Battery(
                BatteryComponent.CASE,
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
                BatteryStatus.DISCONNECTED,
            )
        private var headset: Battery =
            Battery(
                BatteryComponent.HEADSET,
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN,
                BatteryStatus.DISCONNECTED,
            )

        fun isBatteryData(data: ByteArray): Boolean {
            if (data.size != 12 && data.size != 22) {
                return false
            }
            return data.joinToString("") { "%02x".format(it) }.startsWith("040004000400")
        }

        fun setBatteryDirect(
            leftLevel: Int,
            leftCharging: Boolean,
            rightLevel: Int,
            rightCharging: Boolean,
            caseLevel: Int,
            caseCharging: Boolean,
        ) {
            first =
                Battery(
                    BatteryComponent.LEFT,
                    leftLevel,
                    if (leftCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING,
                )
            second =
                Battery(
                    BatteryComponent.RIGHT,
                    rightLevel,
                    if (rightCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING,
                )
            case =
                Battery(
                    BatteryComponent.CASE,
                    caseLevel,
                    if (caseCharging) BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING,
                )

            val unifiedLevel =
                when {
                    leftLevel != BluetoothDevice.BATTERY_LEVEL_UNKNOWN -> leftLevel
                    rightLevel != BluetoothDevice.BATTERY_LEVEL_UNKNOWN -> rightLevel
                    caseLevel != BluetoothDevice.BATTERY_LEVEL_UNKNOWN -> caseLevel
                    else -> BluetoothDevice.BATTERY_LEVEL_UNKNOWN
                }

            headset =
                Battery(
                    BatteryComponent.HEADSET,
                    unifiedLevel,
                    if (leftCharging || rightCharging || caseCharging) BatteryStatus.CHARGING
                    else BatteryStatus.NOT_CHARGING,
                )
        }

        fun setBattery(data: ByteArray) {
            if (data.size == 12) {
                headset =
                    if (data[10].toInt() == BatteryStatus.DISCONNECTED) {
                        Battery(headset.component, headset.level, BatteryStatus.DISCONNECTED)
                    } else {
                        Battery(headset.component, data[9].toInt(), data[10].toInt())
                    }
                return
            }

            if (data.size != 22) {
                return
            }
            first =
                if (data[10].toInt() == BatteryStatus.DISCONNECTED) {
                    Battery(first.component, first.level, BatteryStatus.DISCONNECTED)
                } else {
                    Battery(data[7].toInt(), data[9].toInt(), data[10].toInt())
                }
            second =
                if (data[15].toInt() == BatteryStatus.DISCONNECTED) {
                    Battery(second.component, second.level, BatteryStatus.DISCONNECTED)
                } else {
                    Battery(data[12].toInt(), data[14].toInt(), data[15].toInt())
                }
            case =
                if (
                    data[20].toInt() == BatteryStatus.DISCONNECTED &&
                        case.status != BatteryStatus.DISCONNECTED
                ) {
                    Battery(case.component, case.level, BatteryStatus.DISCONNECTED)
                } else {
                    Battery(data[17].toInt(), data[19].toInt(), data[20].toInt())
                }

            val unifiedLevel =
                when {
                    first.level != BluetoothDevice.BATTERY_LEVEL_UNKNOWN -> first.level
                    second.level != BluetoothDevice.BATTERY_LEVEL_UNKNOWN -> second.level
                    case.level != BluetoothDevice.BATTERY_LEVEL_UNKNOWN -> case.level
                    else -> BluetoothDevice.BATTERY_LEVEL_UNKNOWN
                }

            val unifiedStatus =
                when {
                    first.status != BatteryStatus.DISCONNECTED -> first.status
                    second.status != BatteryStatus.DISCONNECTED -> second.status
                    case.status != BatteryStatus.DISCONNECTED -> case.status
                    else -> BatteryStatus.DISCONNECTED
                }

            headset = Battery(headset.component, unifiedLevel, unifiedStatus)
        }

        fun getBattery(): List<Battery> {
            val left = if (first.component == BatteryComponent.LEFT) first else second
            val right = if (first.component == BatteryComponent.LEFT) second else first
            return listOf(left, right, case, headset)
        }
    }

    class ConversationalAwarenessNotification {
        @Suppress("PrivatePropertyName")
        private val NOTIFICATION_PREFIX = Enums.CONVERSATION_AWARENESS_RECEIVE_PREFIX.value

        var status: Byte = 0
            private set

        fun isConversationalAwarenessData(data: ByteArray): Boolean {
            if (data.size != 10) {
                return false
            }
            val prefixHex = NOTIFICATION_PREFIX.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }

        fun setData(data: ByteArray) {
            status = data[9]
        }
    }
}

class Capabilities {
    companion object {
        val NOISE_CANCELLATION = byteArrayOf(0x0d)
        val EAR_DETECTION = byteArrayOf(0x06)
    }
}

fun isHeadTrackingData(data: ByteArray): Boolean {
    if (data.size <= 60) return false

    val prefixPattern = byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x17, 0x00, 0x00, 0x00, 0x10, 0x00)

    for (i in prefixPattern.indices) {
        if (data[i] != prefixPattern[i].toByte()) return false
    }

    if (data[10] != 0x44.toByte() && data[10] != 0x45.toByte()) return false

    if (data[11] != 0x00.toByte()) return false

    return true
}
