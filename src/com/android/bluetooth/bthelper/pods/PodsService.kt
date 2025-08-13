/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: The MoKee Open Source Project
 * SPDX-FileCopyrightText: Matthias Urhahn
 * SPDX-FileCopyrightText: LibrePods Contributors
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

@file:OptIn(ExperimentalEncodingApi::class)

package com.android.bluetooth.bthelper.pods

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.OnMetadataChangedListener
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.UserHandle
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.Constants.ACTION_ASI_UPDATE_BLUETOOTH_DATA
import com.android.bluetooth.bthelper.Constants.ACTION_BATTERY_LEVEL_CHANGED
import com.android.bluetooth.bthelper.Constants.ACTION_HF_INDICATORS_VALUE_CHANGED
import com.android.bluetooth.bthelper.Constants.COMPANION_TYPE_NONE
import com.android.bluetooth.bthelper.Constants.EXTRA_BATTERY_LEVEL
import com.android.bluetooth.bthelper.Constants.EXTRA_HF_INDICATORS_IND_ID
import com.android.bluetooth.bthelper.Constants.EXTRA_HF_INDICATORS_IND_VALUE
import com.android.bluetooth.bthelper.Constants.HF_INDICATOR_BATTERY_LEVEL_STATUS
import com.android.bluetooth.bthelper.Constants.PACKAGE_ASI
import com.android.bluetooth.bthelper.R
import com.android.bluetooth.bthelper.getSharedPreferences
import com.android.bluetooth.bthelper.getUriAllowlist
import com.android.bluetooth.bthelper.isLowLatencySupported
import com.android.bluetooth.bthelper.setSingleDevice
import com.android.bluetooth.bthelper.utils.A2dpReceiver
import com.android.bluetooth.bthelper.utils.AACPManager
import com.android.bluetooth.bthelper.utils.AACPManager.Companion.StemPressType
import com.android.bluetooth.bthelper.utils.AirPodsNotifications
import com.android.bluetooth.bthelper.utils.BLEManager
import com.android.bluetooth.bthelper.utils.BatteryComponent
import com.android.bluetooth.bthelper.utils.BatteryStatus
import com.android.bluetooth.bthelper.utils.BluetoothConnectionManager
import com.android.bluetooth.bthelper.utils.BluetoothSocketManager
import com.android.bluetooth.bthelper.utils.GestureDetector
import com.android.bluetooth.bthelper.utils.HeadTracking
import com.android.bluetooth.bthelper.utils.MediaController
import com.android.bluetooth.bthelper.utils.PodsA2dpListener
import com.android.bluetooth.bthelper.utils.PodsUuidListener
import com.android.bluetooth.bthelper.utils.StemAction
import com.android.bluetooth.bthelper.utils.UuidReceiver
import com.android.bluetooth.bthelper.utils.models.IPods
import com.android.bluetooth.bthelper.utils.models.RegularPods
import com.android.bluetooth.bthelper.utils.models.RegularPodsMetadata
import com.android.bluetooth.bthelper.utils.models.SinglePods
import com.android.bluetooth.bthelper.utils.models.SinglePodsMetadata
import com.android.bluetooth.bthelper.utils.setMetadataBoolean
import com.android.bluetooth.bthelper.utils.setMetadataInt
import com.android.bluetooth.bthelper.utils.setMetadataString
import com.android.bluetooth.bthelper.utils.setMetadataUri
import com.android.bluetooth.bthelper.utils.setMetadataValue
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them
 */
class PodsService :
    Service(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    PodsUuidListener,
    AACPManager.PacketCallback,
    BLEManager.AirPodsStatusListener,
    PodsA2dpListener,
    OnMetadataChangedListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val TAG: String = "PodsService"
    }

    data class ServiceConfig(
        var earDetectionEnabled: Boolean = true,
        var conversationalAwarenessPauseMusic: Boolean = false,
        var relativeConversationalAwarenessVolume: Boolean = true,
        var headGestures: Boolean = true,
        var disconnectWhenNotWearing: Boolean = false,
        var conversationalAwarenessVolume: Int = 43,
        var useAlternatePackets: Boolean = false,
        var leftSinglePressAction: StemAction =
            StemAction.defaultActions[StemPressType.SINGLE_PRESS]!!,
        var rightSinglePressAction: StemAction =
            StemAction.defaultActions[StemPressType.SINGLE_PRESS]!!,
        var leftDoublePressAction: StemAction =
            StemAction.defaultActions[StemPressType.DOUBLE_PRESS]!!,
        var rightDoublePressAction: StemAction =
            StemAction.defaultActions[StemPressType.DOUBLE_PRESS]!!,
        var leftTriplePressAction: StemAction =
            StemAction.defaultActions[StemPressType.TRIPLE_PRESS]!!,
        var rightTriplePressAction: StemAction =
            StemAction.defaultActions[StemPressType.TRIPLE_PRESS]!!,
        var leftLongPressAction: StemAction = StemAction.defaultActions[StemPressType.LONG_PRESS]!!,
        var rightLongPressAction: StemAction = StemAction.defaultActions[StemPressType.LONG_PRESS]!!,
    )

    private var config: ServiceConfig? = null
    private var bluetoothSocketManager: BluetoothSocketManager? = null

    private var currentDevice: BluetoothDevice? = null

    @Volatile private var isMetaDataSet = false
    @Volatile private var isSliceSet = false
    @Volatile private var isModelDataSet = false

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var aacpManager: AACPManager? = null
    private var sharedPreferences: SharedPreferences? = null

    private var inputManager: InputManager? = null

    private var telecomManager: TelecomManager? = null
    private var gestureDetector: GestureDetector? = null
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener =
        object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                val bleManager = bleManager ?: return
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        if (config?.headGestures == true) {
                            handleIncomingCall()
                        }
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        isInCall = true
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        isInCall = false
                        gestureDetector?.stopDetection()
                    }
                }
            }
        }

    private var isInCall = false
    var cameraActive = false

    val earDetectNotif = AirPodsNotifications.EarDetection()
    val ancNotif = AirPodsNotifications.ANC()
    val batteryNotif = AirPodsNotifications.BatteryNotification()
    val conversAwareNotif = AirPodsNotifications.ConversationalAwarenessNotification()
    val handler = Handler(Looper.getMainLooper())

    @Volatile var isHeadTrackingActive = false

    var uuidReceiver: UuidReceiver? = null
    var a2dpReceiver: A2dpReceiver? = null
    var bleManager: BLEManager? = null

    override fun onDeviceStatusChanged(
        device: BLEManager.AirPodsStatus,
        previousStatus: BLEManager.AirPodsStatus?,
    ) {
        if (device.connectionState == Constants.STATE_DISCONNECTED) {
            bluetoothSocketManager?.connectToSocket(currentDevice)
        }
        val bleManager = bleManager ?: return
        setRecentBatteryDirect(bleManager.getMostRecentStatus())
        updatePodsStatus(device, currentDevice)

        // if (
        //     batteryNotif.getBattery()[0].status == BatteryStatus.CHARGING &&
        //         batteryNotif.getBattery()[1].status == BatteryStatus.CHARGING
        // ) {
        //     disconnectAudio()
        // } else {
        //     connectAudio()
        // }
    }

    override fun onBroadcastFromNewAddress(device: BLEManager.AirPodsStatus) {}

    override fun onLidStateChanged(device: BLEManager.AirPodsStatus?, lidOpen: Boolean) {
        if (lidOpen) {
            val bleManager = bleManager ?: return
            setRecentBatteryDirect(bleManager.getMostRecentStatus())
            if (device != null) {
                updatePodsStatus(device, currentDevice)
            }
        } else {}
    }

    override fun onEarStateChanged(
        device: BLEManager.AirPodsStatus,
        leftInEar: Boolean,
        rightInEar: Boolean,
    ) {
        // processEarDetectionChange(earDetectNotif.createEarDetectionData(leftInEar, rightInEar))
    }

    override fun onBatteryChanged(device: BLEManager.AirPodsStatus) {
        val bleManager = bleManager ?: return
        setRecentBatteryDirect(bleManager.getMostRecentStatus())
        updatePodsStatus(device, currentDevice)
    }

    @Synchronized
    private fun setRecentBatteryDirect(recentStat: BLEManager.AirPodsStatus) {
        val leftLevel = recentStat.leftBattery
        val rightLevel = recentStat.rightBattery
        val caseLevel = recentStat.caseBattery
        val leftCharging = recentStat.isLeftCharging
        val rightCharging = recentStat.isRightCharging
        val caseCharging = recentStat.isCaseCharging

        batteryNotif.setBatteryDirect(
            leftLevel = leftLevel,
            leftCharging = leftCharging == true,
            rightLevel = rightLevel,
            rightCharging = rightCharging == true,
            caseLevel = caseLevel,
            caseCharging = caseCharging == true,
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @Synchronized
    fun handleIncomingCall() {
        if (isInCall) return
        if (config?.headGestures == true && !isInCall) {
            startHeadTracking()
            gestureDetector?.startDetection { accepted ->
                if (accepted) {
                    answerCall()
                } else {
                    rejectCall()
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun testHeadGestures(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            gestureDetector?.startDetection(doNotStop = true) { accepted ->
                if (continuation.isActive) {
                    continuation.resume(accepted) { gestureDetector?.stopDetection() }
                }
            }
        }
    }

    private fun answerCall() {
        try {
            if (
                checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) ==
                    PackageManager.PERMISSION_GRANTED
            ) {
                telecomManager?.acceptRingingCall()
            }

            sendToast(getString(R.string.head_gesture_call_answered_toast))
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call", e)
        }
    }

    private fun rejectCall() {
        try {
            if (
                checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) ==
                    PackageManager.PERMISSION_GRANTED
            ) {
                telecomManager?.endCall()
            }
            sendToast(getString(R.string.head_gesture_call_rejected_toast))
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting call", e)
        }
    }

    fun sendToast(message: String) {
        handler.post { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }

    override fun onBatteryInfoReceived(batteryInfo: ByteArray) {
        batteryNotif.setBattery(batteryInfo)
        updatePodsStatus(null, currentDevice)

        if (
            batteryNotif.getBattery()[0].status == BatteryStatus.CHARGING &&
                batteryNotif.getBattery()[1].status == BatteryStatus.CHARGING
        ) {
            disconnectAudio()
        } else {
            connectAudio()
        }
    }

    override fun onEarDetectionReceived(earDetection: ByteArray) {
        processEarDetectionChange(earDetection)
    }

    override fun onConversationAwarenessReceived(conversationAwareness: ByteArray) {
        conversAwareNotif.setData(conversationAwareness)
        if (conversAwareNotif.status == 1.toByte() || conversAwareNotif.status == 2.toByte()) {
            MediaController.startSpeaking()
        } else if (
            conversAwareNotif.status == 8.toByte() || conversAwareNotif.status == 9.toByte()
        ) {
            MediaController.stopSpeaking()
        }
    }

    override fun onControlCommandReceived(controlCommand: ByteArray) {
        val command = AACPManager.ControlCommand.fromByteArray(controlCommand) ?: return
        if (
            command.identifier ==
                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value
        ) {
            ancNotif.setStatus(
                byteArrayOf(command.value.takeIf { it.isNotEmpty() }?.get(0) ?: 0x00.toByte())
            )
        }
    }

    override fun onDeviceMetadataReceived(deviceMetadata: ByteArray) {}

    override fun onHeadTrackingReceived(headTracking: ByteArray) {
        if (isHeadTrackingActive) {
            HeadTracking.processPacket(headTracking)
            processHeadTrackingData(headTracking)
        }
    }

    override fun onProximityKeysReceived(proximityKeys: ByteArray) {
        val keys = aacpManager?.parseProximityKeysResponse(proximityKeys) ?: return
        sharedPreferences?.edit {
            for (key in keys) {
                putString(key.key.name, Base64.encode(key.value))
            }
        }
    }

    override fun onStemPressReceived(stemPress: ByteArray) {
        val (stemPressType, bud) = aacpManager?.parseStemPressResponse(stemPress) ?: return

        val action = getActionFor(bud, stemPressType)

        action?.let { executeStemAction(it) }
    }

    override fun onUnknownPacketReceived(packet: ByteArray) {}

    private fun getActionFor(
        bud: AACPManager.Companion.StemPressBudType,
        type: StemPressType,
    ): StemAction? {
        val config = config ?: return null
        return when (type) {
            StemPressType.SINGLE_PRESS ->
                if (bud == AACPManager.Companion.StemPressBudType.LEFT) config.leftSinglePressAction
                else config.rightSinglePressAction
            StemPressType.DOUBLE_PRESS ->
                if (bud == AACPManager.Companion.StemPressBudType.LEFT) config.leftDoublePressAction
                else config.rightDoublePressAction
            StemPressType.TRIPLE_PRESS ->
                if (bud == AACPManager.Companion.StemPressBudType.LEFT) config.leftTriplePressAction
                else config.rightTriplePressAction
            StemPressType.LONG_PRESS ->
                if (bud == AACPManager.Companion.StemPressBudType.LEFT) config.leftLongPressAction
                else config.rightLongPressAction
        }
    }

    private fun executeStemAction(action: StemAction) {
        when (action) {
            StemAction.defaultActions[StemPressType.SINGLE_PRESS] -> {
                // Default single press action: Play/Pause, not taking action.
            }
            StemAction.PLAY_PAUSE -> MediaController.sendPlayPause()
            StemAction.PREVIOUS_TRACK -> MediaController.sendPreviousTrack()
            StemAction.NEXT_TRACK -> MediaController.sendNextTrack()
            StemAction.CAMERA_SHUTTER -> triggerVirtualKeypress(KeyEvent.KEYCODE_CAMERA)
            StemAction.DIGITAL_ASSISTANT -> {
                val intent =
                    Intent(Intent.ACTION_VOICE_COMMAND).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                startActivity(intent)
            }
            StemAction.CYCLE_NOISE_CONTROL_MODES -> {
                sendBroadcast(Intent(Constants.ACTION_SET_ANC_MODE))
            }
        }
    }

    private fun triggerVirtualKeypress(keyCode: Int) {
        val now: Long = SystemClock.uptimeMillis()
        val downEvent: KeyEvent =
            KeyEvent(
                now,
                now,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_KEYBOARD,
            )
        val upEvent: KeyEvent = KeyEvent.changeAction(downEvent, KeyEvent.ACTION_UP)

        inputManager?.let {
            it.injectInputEvent(downEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
            it.injectInputEvent(upEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
        }
    }

    @Synchronized
    private fun processEarDetectionChange(earDetection: ByteArray) {
        var inEar = false
        var inEarData =
            listOf(
                earDetectNotif.status[0] == 0x00.toByte(),
                earDetectNotif.status[1] == 0x00.toByte(),
            )
        var justEnabledA2dp = false
        earDetectNotif.setStatus(earDetection)
        if (config?.earDetectionEnabled == true) {
            val data = earDetection.copyOfRange(earDetection.size - 2, earDetection.size)
            inEar = data[0] == 0x00.toByte() && data[1] == 0x00.toByte()

            val newInEarData = listOf(data[0] == 0x00.toByte(), data[1] == 0x00.toByte())

            if (newInEarData.contains(true) && inEarData == listOf(false, false)) {
                connectAudio()
                justEnabledA2dp = true
                registerA2dpConnectionReceiver()
                if (MediaController.getMusicActive() == true) {
                    MediaController.userPlayedTheMedia = true
                }
            } else if (newInEarData == listOf(false, false)) {
                MediaController.sendPause(force = true)
                if (config?.disconnectWhenNotWearing == true) {
                    disconnectAudio()
                }
            }

            if (inEarData.contains(false) && newInEarData == listOf(true, true)) {
                MediaController.userPlayedTheMedia = false
            }

            if (newInEarData.contains(false) && inEarData == listOf(true, true)) {
                MediaController.userPlayedTheMedia = false
            }

            if (newInEarData.sorted() != inEarData.sorted()) {
                inEarData = newInEarData
                if (inEar == true) {
                    if (!justEnabledA2dp) {
                        justEnabledA2dp = false
                        MediaController.sendPlay()
                        MediaController.userPausedTheMedia = false
                    }
                } else {
                    MediaController.sendPause()
                }
            }
        }
    }

    @Synchronized
    override fun onPodsA2dpDetected(device: BluetoothDevice?) {
        MediaController.sendPlay()
        MediaController.userPausedTheMedia = false
    }

    @Synchronized
    private fun registerA2dpConnectionReceiver() {
        currentDevice?.let { a2dpReceiver?.onCreate(this, it) }
    }

    @Synchronized
    fun processHeadTrackingData(data: ByteArray) {
        val horizontal = ByteBuffer.wrap(data, 51, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        val vertical = ByteBuffer.wrap(data, 53, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        gestureDetector?.processHeadOrientation(horizontal, vertical)
    }

    private fun initializeConfig() {
        val sharedPreferences = this.sharedPreferences ?: return
        config =
            ServiceConfig(
                earDetectionEnabled =
                    sharedPreferences.getBoolean(Constants.KEY_AUTOMATIC_EAR_DETECTION, true),
                conversationalAwarenessPauseMusic =
                    sharedPreferences.getBoolean(
                        Constants.KEY_CONVERSATIONAL_AWARENESS_PAUSE_MUSIC,
                        false,
                    ),
                relativeConversationalAwarenessVolume =
                    sharedPreferences.getBoolean(
                        Constants.KEY_RELATIVE_CONVERSATIONAL_AWARENESS_VOLUME,
                        true,
                    ),
                headGestures = sharedPreferences.getBoolean(Constants.KEY_HEAD_GESTURES, true),
                disconnectWhenNotWearing =
                    sharedPreferences.getBoolean(Constants.KEY_DISCONNECT_WHEN_NOT_WEARING, false),
                conversationalAwarenessVolume =
                    sharedPreferences.getInt(Constants.KEY_CONVERSATIONAL_AWARENESS_VOLUME, 43),
                useAlternatePackets =
                    sharedPreferences.getBoolean(
                        Constants.KEY_USE_ALTERNATE_HEAD_TRACKING_PACKETS,
                        false,
                    ),
                leftSinglePressAction =
                    StemAction.fromString(
                        sharedPreferences.getString(
                            Constants.KEY_LEFT_SINGLE_PRESS_ACTION,
                            StemAction.PLAY_PAUSE.name,
                        ) ?: StemAction.PLAY_PAUSE.name
                    )!!,
                rightSinglePressAction =
                    StemAction.fromString(
                        sharedPreferences.getString(
                            Constants.KEY_RIGHT_SINGLE_PRESS_ACTION,
                            StemAction.PLAY_PAUSE.name,
                        ) ?: StemAction.PLAY_PAUSE.name
                    )!!,
                leftDoublePressAction =
                    StemAction.fromString(
                        sharedPreferences.getString(
                            Constants.KEY_LEFT_DOUBLE_PRESS_ACTION,
                            StemAction.PREVIOUS_TRACK.name,
                        ) ?: StemAction.PREVIOUS_TRACK.name
                    )!!,
                rightDoublePressAction =
                    StemAction.fromString(
                        sharedPreferences.getString(
                            Constants.KEY_RIGHT_DOUBLE_PRESS_ACTION,
                            StemAction.NEXT_TRACK.name,
                        ) ?: StemAction.NEXT_TRACK.name
                    )!!,
                leftTriplePressAction =
                    StemAction.fromString(
                        sharedPreferences.getString(
                            Constants.KEY_LEFT_TRIPLE_PRESS_ACTION,
                            StemAction.PREVIOUS_TRACK.name,
                        ) ?: StemAction.PREVIOUS_TRACK.name
                    )!!,
                rightTriplePressAction =
                    StemAction.fromString(
                        sharedPreferences.getString(
                            Constants.KEY_RIGHT_TRIPLE_PRESS_ACTION,
                            StemAction.NEXT_TRACK.name,
                        ) ?: StemAction.NEXT_TRACK.name
                    )!!,
                leftLongPressAction =
                    StemAction.fromString(
                        sharedPreferences.getString(
                            Constants.KEY_LEFT_LONG_PRESS_ACTION,
                            StemAction.CYCLE_NOISE_CONTROL_MODES.name,
                        ) ?: StemAction.CYCLE_NOISE_CONTROL_MODES.name
                    )!!,
                rightLongPressAction =
                    StemAction.fromString(
                        sharedPreferences.getString(
                            Constants.KEY_RIGHT_LONG_PRESS_ACTION,
                            StemAction.DIGITAL_ASSISTANT.name,
                        ) ?: StemAction.DIGITAL_ASSISTANT.name
                    )!!,
            )
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (preferences == null || key == null) return

        val config = config ?: return
        when (key) {
            Constants.KEY_CONVERSATIONAL_AWARENESS ->
                aacpManager?.sendControlCommand(
                    AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG
                        .value,
                    preferences.getBoolean(key, true),
                )
            Constants.KEY_AUTOMATIC_EAR_DETECTION ->
                config.earDetectionEnabled = preferences.getBoolean(key, true)
            Constants.KEY_CONVERSATIONAL_AWARENESS_PAUSE_MUSIC ->
                config.conversationalAwarenessPauseMusic = preferences.getBoolean(key, false)
            Constants.KEY_RELATIVE_CONVERSATIONAL_AWARENESS_VOLUME ->
                config.relativeConversationalAwarenessVolume = preferences.getBoolean(key, true)
            Constants.KEY_HEAD_GESTURES -> config.headGestures = preferences.getBoolean(key, true)
            Constants.KEY_DISCONNECT_WHEN_NOT_WEARING ->
                config.disconnectWhenNotWearing = preferences.getBoolean(key, false)
            Constants.KEY_CONVERSATIONAL_AWARENESS_VOLUME ->
                config.conversationalAwarenessVolume = preferences.getInt(key, 43)
            Constants.KEY_LEFT_SINGLE_PRESS_ACTION -> {
                config.leftSinglePressAction =
                    StemAction.fromString(
                        preferences.getString(key, StemAction.PLAY_PAUSE.name)
                            ?: StemAction.PLAY_PAUSE.name
                    )!!
                setupStemActions()
            }
            Constants.KEY_RIGHT_SINGLE_PRESS_ACTION -> {
                config.rightSinglePressAction =
                    StemAction.fromString(
                        preferences.getString(key, StemAction.PLAY_PAUSE.name)
                            ?: StemAction.PLAY_PAUSE.name
                    )!!
                setupStemActions()
            }
            Constants.KEY_LEFT_DOUBLE_PRESS_ACTION -> {
                config.leftDoublePressAction =
                    StemAction.fromString(
                        preferences.getString(key, StemAction.PREVIOUS_TRACK.name)
                            ?: StemAction.PREVIOUS_TRACK.name
                    )!!
                setupStemActions()
            }
            Constants.KEY_RIGHT_DOUBLE_PRESS_ACTION -> {
                config.rightDoublePressAction =
                    StemAction.fromString(
                        preferences.getString(key, StemAction.NEXT_TRACK.name)
                            ?: StemAction.NEXT_TRACK.name
                    )!!
                setupStemActions()
            }
            Constants.KEY_LEFT_TRIPLE_PRESS_ACTION -> {
                config.leftTriplePressAction =
                    StemAction.fromString(
                        preferences.getString(key, StemAction.PREVIOUS_TRACK.name)
                            ?: StemAction.PREVIOUS_TRACK.name
                    )!!
                setupStemActions()
            }
            Constants.KEY_RIGHT_TRIPLE_PRESS_ACTION -> {
                config.rightTriplePressAction =
                    StemAction.fromString(
                        preferences.getString(key, StemAction.NEXT_TRACK.name)
                            ?: StemAction.NEXT_TRACK.name
                    )!!
                setupStemActions()
            }
            Constants.KEY_LEFT_LONG_PRESS_ACTION -> {
                config.leftLongPressAction =
                    StemAction.fromString(
                        preferences.getString(key, StemAction.CYCLE_NOISE_CONTROL_MODES.name)
                            ?: StemAction.CYCLE_NOISE_CONTROL_MODES.name
                    )!!
                setupStemActions()
            }
            Constants.KEY_RIGHT_LONG_PRESS_ACTION -> {
                config.rightLongPressAction =
                    StemAction.fromString(
                        preferences.getString(key, StemAction.DIGITAL_ASSISTANT.name)
                            ?: StemAction.DIGITAL_ASSISTANT.name
                    )!!
                setupStemActions()
            }
            Constants.KEY_LOW_LATENCY_AUDIO -> {
                if (isLowLatencySupported()) {
                    currentDevice?.setLowLatencyAudioAllowed(preferences.getBoolean(key, false))
                }
            }
        }
    }

    val ancModeFilter = IntentFilter(Constants.ACTION_SET_ANC_MODE)
    val ancModeReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Constants.ACTION_SET_ANC_MODE) return
                if (intent.hasExtra(Constants.EXTRA_MODE)) {
                    val mode = intent.getIntExtra(Constants.EXTRA_MODE, -1)
                    if (mode in 1..4) {
                        aacpManager?.sendControlCommand(
                            AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
                            mode,
                        )
                    }
                    return
                }
                val currentMode = ancNotif.status
                val allowOffModeValue =
                    aacpManager?.controlCommandStatusList?.find {
                        it.identifier ==
                            AACPManager.Companion.ControlCommandIdentifiers.ALLOW_OFF_OPTION
                    }
                val allowOffMode =
                    allowOffModeValue?.value?.takeIf { it.isNotEmpty() }?.get(0) == 0x01.toByte()

                val nextMode =
                    if (allowOffMode) {
                        when (currentMode) {
                            1 -> 2
                            2 -> 3
                            3 -> 4
                            4 -> 1
                            else -> 1
                        }
                    } else {
                        when (currentMode) {
                            1 -> 2
                            2 -> 3
                            3 -> 4
                            4 -> 2
                            else -> 2
                        }
                    }

                aacpManager?.sendControlCommand(
                    AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
                    nextMode,
                )
            }
        }

    override fun onCreate() {
        sharedPreferences = getSharedPreferences()
        initializeConfig()
        sharedPreferences?.registerOnSharedPreferenceChangeListener(this@PodsService)

        uuidReceiver = UuidReceiver(this)
        a2dpReceiver = A2dpReceiver(this)

        MediaController.onCreate(this)

        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        gestureDetector = GestureDetector(this) { stopHeadTracking() }

        telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        aacpManager = AACPManager()
        aacpManager?.setPacketCallback(this)

        bluetoothSocketManager =
            BluetoothSocketManager(
                serviceScope,
                aacpManager!!,
                { startHeadTracking() },
                { stopHeadTracking() },
                { setupStemActions() },
                { BluetoothConnectionManager.isConnected = false },
            )

        registerReceiver(ancModeReceiver, ancModeFilter, RECEIVER_NOT_EXPORTED)

        bleManager = BLEManager(this)
        bleManager?.setAirPodsStatusListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device: BluetoothDevice? =
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                ?: return START_STICKY

        if (currentDevice == null && intent?.action == Constants.ACTION_CONNECTED) {
            currentDevice = device
            updatePodsStatus(null, device)
            bluetoothAdapter?.addOnMetadataChangedListener(device!!, mainExecutor, this)
            serviceScope.launch { bluetoothSocketManager?.connectToSocket(device) }
            connect()
            serviceScope.launch { bleManager?.startScanning() }
            return START_STICKY
        }

        if (
            currentDevice != null &&
                currentDevice?.equals(device) == true &&
                intent?.action == Constants.ACTION_NAME_CHANGED &&
                BluetoothConnectionManager.isConnected
        ) {
            val name: String? = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
            setName(device, name)
            return START_STICKY
        }

        if (
            currentDevice != null &&
                currentDevice?.equals(device) == true &&
                intent?.action == Constants.ACTION_DISCONNECTED
        ) {
            BluetoothConnectionManager.isConnected = false
            stopSelf()
            return START_STICKY
        }

        if (currentDevice != null && BluetoothConnectionManager.isConnected) {
            manuallyCheckForAudioSource()
            return START_STICKY
        }

        return START_STICKY
    }

    @Synchronized
    fun manuallyCheckForAudioSource() {
        val shouldResume = MediaController.getMusicActive() ?: false
        if (earDetectNotif.status[0] != 0.toByte() && earDetectNotif.status[1] != 0.toByte()) {
            disconnectAudio(shouldResume = shouldResume)
        }
    }

    fun connect() {
        currentDevice?.let { uuidReceiver?.onCreate(this, it) }
    }

    override fun onPodsDetected(device: BluetoothDevice?) {
        if (device == null) return
        connectToA2dpProfile(device)
    }

    private fun connectToA2dpProfile(device: BluetoothDevice) {
        val bluetoothAdapter = bluetoothAdapter ?: return

        bluetoothAdapter.getProfileProxy(
            this,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        val connectedDevices = proxy.connectedDevices
                        if (connectedDevices.contains(device)) {
                            serviceScope.launch { bluetoothSocketManager?.connectToSocket(device) }
                        }
                    }
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            },
            BluetoothProfile.A2DP,
        )
    }

    fun disconnect() {
        val bluetoothAdapter = bluetoothAdapter ?: return
        val currentDevice = currentDevice ?: return

        bluetoothAdapter.getProfileProxy(
            this,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        val connectedDevices = proxy.connectedDevices
                        if (connectedDevices.contains(currentDevice)) {
                            MediaController.sendPause()
                        }
                    }
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            },
            BluetoothProfile.A2DP,
        )
    }

    fun setEarDetection(enabled: Boolean) {
        val config = config ?: return

        if (config.earDetectionEnabled == enabled) return
        config.earDetectionEnabled = enabled
        sharedPreferences?.edit { putBoolean(Constants.KEY_AUTOMATIC_EAR_DETECTION, enabled) }
    }

    fun cameraOpened() {
        val config = config ?: return
        val isCameraShutterUsed =
            listOf(
                    config.leftSinglePressAction,
                    config.rightSinglePressAction,
                    config.leftDoublePressAction,
                    config.rightDoublePressAction,
                    config.leftTriplePressAction,
                    config.rightTriplePressAction,
                    config.leftLongPressAction,
                    config.rightLongPressAction,
                )
                .any { it == StemAction.CAMERA_SHUTTER }

        if (isCameraShutterUsed) {
            cameraActive = true
            setupStemActions(isCameraActive = true)
        }
    }

    fun cameraClosed() {
        cameraActive = false
        setupStemActions()
    }

    fun isCustomAction(
        action: StemAction?,
        default: StemAction?,
        isCameraActive: Boolean = false,
    ): Boolean {
        return action != default && (action != StemAction.CAMERA_SHUTTER || isCameraActive)
    }

    fun setupStemActions(isCameraActive: Boolean = false) {
        val config = config ?: return

        val singlePressDefault = StemAction.defaultActions[StemPressType.SINGLE_PRESS]
        val doublePressDefault = StemAction.defaultActions[StemPressType.DOUBLE_PRESS]
        val triplePressDefault = StemAction.defaultActions[StemPressType.TRIPLE_PRESS]
        val longPressDefault = StemAction.defaultActions[StemPressType.LONG_PRESS]

        val singlePressCustomized =
            isCustomAction(config.leftSinglePressAction, singlePressDefault, isCameraActive) ||
                isCustomAction(config.rightSinglePressAction, singlePressDefault, isCameraActive)
        val doublePressCustomized =
            isCustomAction(config.leftDoublePressAction, doublePressDefault, isCameraActive) ||
                isCustomAction(config.rightDoublePressAction, doublePressDefault, isCameraActive)
        val triplePressCustomized =
            isCustomAction(config.leftTriplePressAction, triplePressDefault, isCameraActive) ||
                isCustomAction(config.rightTriplePressAction, triplePressDefault, isCameraActive)
        val longPressCustomized =
            isCustomAction(config.leftLongPressAction, longPressDefault, isCameraActive) ||
                isCustomAction(config.rightLongPressAction, longPressDefault, isCameraActive)
        aacpManager?.sendStemConfigPacket(
            singlePressCustomized,
            doublePressCustomized,
            triplePressCustomized,
            longPressCustomized,
        )
    }

    @Synchronized
    fun disconnectAudio(shouldResume: Boolean = false) {
        val device = currentDevice ?: return
        val bluetoothAdapter = bluetoothAdapter ?: return

        bluetoothAdapter?.getProfileProxy(
            this,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        try {
                            if (
                                proxy.getConnectionState(device) ==
                                    BluetoothProfile.STATE_DISCONNECTED
                            ) {
                                return
                            }
                            device?.disconnect()
                            if (shouldResume) {
                                handler.postDelayed({ MediaController.sendPlay() }, 150)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error disconnecting A2DP", e)
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Log.d(TAG, "A2DP service disconnected")
                }
            },
            BluetoothProfile.A2DP,
        )

        bluetoothAdapter?.getProfileProxy(
            this,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        try {
                            device?.disconnect()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error disconnecting HEADSET", e)
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Log.d(TAG, "HEADSET service disconnected")
                }
            },
            BluetoothProfile.HEADSET,
        )
    }

    @Synchronized
    fun connectAudio() {
        val device = currentDevice ?: return
        val bluetoothAdapter = bluetoothAdapter ?: return

        bluetoothAdapter?.getProfileProxy(
            this,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        try {
                            device?.connect()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error connecting A2DP", e)
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                            MediaController.sendPlay()
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Log.d(TAG, "A2DP service disconnected")
                }
            },
            BluetoothProfile.A2DP,
        )

        bluetoothAdapter?.getProfileProxy(
            this,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        try {
                            device?.connect()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error connecting HEADSET", e)
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Log.d(TAG, "HEADSET service disconnected")
                }
            },
            BluetoothProfile.HEADSET,
        )
    }

    @Synchronized
    fun setName(device: BluetoothDevice?, name: String?) {
        if (device == null || name == null) {
            return
        }

        if (!device.equals(currentDevice)) {
            return
        }

        aacpManager?.sendRename(name)
    }

    override fun onDestroy() {
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)

        unregisterReceiver(ancModeReceiver)

        bleManager?.stopScanning()

        disconnectAudio()

        bluetoothSocketManager?.disconnect()
        gestureDetector?.stopDetection()
        inputManager = null
        disconnect()

        config = null
        bluetoothSocketManager = null
        aacpManager = null
        sharedPreferences = null
        bleManager = null
        MediaController.onDestroy()
        gestureDetector = null
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        telephonyManager = null
        currentDevice?.let { bluetoothAdapter?.removeOnMetadataChangedListener(it, this) }
        currentDevice = null
        telecomManager = null
        bluetoothAdapter = null
        uuidReceiver?.onDestroy()
        uuidReceiver = null
        a2dpReceiver?.onDestroy()
        a2dpReceiver = null

        serviceScope.cancel()

        super.onDestroy()
    }

    @Synchronized
    fun startHeadTracking() {
        val config = config ?: return
        isHeadTrackingActive = true
        aacpManager?.sendStartHeadTracking(config.useAlternatePackets)
        HeadTracking.reset()
    }

    @Synchronized
    fun stopHeadTracking() {
        val config = config ?: return
        aacpManager?.sendStopHeadTracking(config.useAlternatePackets)
        isHeadTrackingActive = false
    }

    private fun resToFile(resId: Int): File? {
        val drawable = ContextCompat.getDrawable(this, resId) ?: return null

        val iconDir = File(filesDir, "icons")
        if (!iconDir.exists()) {
            iconDir.mkdirs()
        }

        val iconFile = File(iconDir, resources.getResourceEntryName(resId) + ".png")
        if (iconFile.exists()) {
            return iconFile
        }

        try {
            FileOutputStream(iconFile).use { out ->
                drawable.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error writing icon to file", e)
            return null
        }

        return iconFile
    }

    // Convert internal resource address to URI
    private fun getIconUri(resId: Int): Uri? {
        val iconFile = resToFile(resId) ?: return null

        val uri = FileProvider.getUriForFile(this, Constants.AUTHORITY_FILE, iconFile)

        getUriAllowlist().forEach { pkg ->
            grantUriPermission(
                pkg,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
        }

        return uri
    }

    private fun restoreInitialMetadata(device: BluetoothDevice, key: Int) {
        when (key) {
            BluetoothDevice.METADATA_COMPANION_APP -> {
                device.setMetadataString(
                    BluetoothDevice.METADATA_COMPANION_APP,
                    Constants.AUTHORITY_BTHELPER,
                )
            }
            BluetoothDevice.METADATA_SOFTWARE_VERSION -> {
                device.setMetadataString(
                    BluetoothDevice.METADATA_SOFTWARE_VERSION,
                    COMPANION_TYPE_NONE,
                )
            }
            BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI,
            Constants.METADATA_FAST_PAIR_CUSTOMIZED_FIELDS -> {
                sendSliceSetupIntent(device, key)
            }
        }
    }

    private fun setInitialMetadata(device: BluetoothDevice): Boolean {
        var ret =
            device.setMetadataString(
                BluetoothDevice.METADATA_COMPANION_APP,
                Constants.AUTHORITY_BTHELPER,
            )
        ret =
            device.setMetadataString(BluetoothDevice.METADATA_SOFTWARE_VERSION, COMPANION_TYPE_NONE)
        sendSliceSetupIntent(device, BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI)
        sendSliceSetupIntent(device, Constants.METADATA_FAST_PAIR_CUSTOMIZED_FIELDS)

        return ret
    }

    private fun sendSliceSetupIntent(device: BluetoothDevice, key: Int) {
        val sliceIntent: Intent =
            Intent(Constants.ACTION_SET_SLICE).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                putExtra(Constants.EXTRA_METADATA_KEY, key)
                setPackage(Constants.PACKAGE_BTHELPER_ADAPTER)
            }
        sendBroadcastAsUser(sliceIntent, UserHandle.ALL)
    }

    private fun setRegularPodsMetadata(
        device: BluetoothDevice,
        metadata: RegularPodsMetadata,
    ): Boolean {
        var ret =
            device.setMetadataString(
                BluetoothDevice.METADATA_MANUFACTURER_NAME,
                metadata.manufacturer,
            )
        ret = device.setMetadataString(BluetoothDevice.METADATA_MODEL_NAME, metadata.model)
        ret =
            device.setMetadataValue(
                BluetoothDevice.METADATA_DEVICE_TYPE,
                BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray(),
            )
        ret = device.setMetadataBoolean(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET, true)
        ret =
            device.setMetadataInt(
                BluetoothDevice.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            )
        ret =
            device.setMetadataInt(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            )
        ret =
            device.setMetadataInt(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            )
        ret =
            device.setMetadataInt(
                BluetoothDevice.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            )
        ret =
            device.setMetadataUri(BluetoothDevice.METADATA_MAIN_ICON, getIconUri(metadata.drawable))
        ret =
            device.setMetadataUri(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON,
                getIconUri(metadata.leftDrawable),
            )
        ret =
            device.setMetadataUri(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON,
                getIconUri(metadata.rightDrawable),
            )
        ret =
            device.setMetadataUri(
                BluetoothDevice.METADATA_UNTETHERED_CASE_ICON,
                getIconUri(metadata.caseDrawable),
            )
        return ret
    }

    private fun setSinglePodsMetadata(
        device: BluetoothDevice,
        metadata: SinglePodsMetadata,
    ): Boolean {
        var ret =
            device.setMetadataString(
                BluetoothDevice.METADATA_MANUFACTURER_NAME,
                metadata.manufacturer,
            )
        ret =
            device.setMetadataValue(
                BluetoothDevice.METADATA_DEVICE_TYPE,
                BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray(),
            )
        ret = device.setMetadataBoolean(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET, true)
        ret = device.setMetadataString(BluetoothDevice.METADATA_MODEL_NAME, metadata.model)
        ret =
            device.setMetadataInt(
                BluetoothDevice.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                metadata.lowBattThreshold,
            )
        ret =
            device.setMetadataUri(BluetoothDevice.METADATA_MAIN_ICON, getIconUri(metadata.drawable))
        return ret
    }

    override fun onMetadataChanged(device: BluetoothDevice, key: Int, value: ByteArray?) {
        if (currentDevice?.equals(device) != true) return
        if (isSliceSet) {
            restoreInitialMetadata(device, key)
        }
    }

    // Set metadata (icon, battery, charging status, etc.) for current device
    // and send broadcast that device status has changed
    @Synchronized
    private fun updatePodsStatus(status: BLEManager.AirPodsStatus?, device: BluetoothDevice?) {
        if (device == null) return

        val sp = this.getSharedPreferences()
        val airpods: IPods? = status?.model
        val single: Boolean? = airpods?.isSingle
        if (single != null) {
            sp.setSingleDevice(single)
        }
        var batteryUnified: Int? = null
        var chargingMain: Boolean? = null

        if (!isMetaDataSet) {
            isSliceSet = setInitialMetadata(device)
        }

        if (single == false) {
            val regularPods: RegularPods? = airpods as RegularPods?
            if (!isMetaDataSet && regularPods != null) {
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

            val leftCharging: Boolean =
                batteryNotif.getBattery().find { it.component == BatteryComponent.LEFT }?.status ==
                    BatteryStatus.CHARGING
            val rightCharging: Boolean =
                batteryNotif.getBattery().find { it.component == BatteryComponent.RIGHT }?.status ==
                    BatteryStatus.CHARGING
            val caseCharging: Boolean =
                batteryNotif.getBattery().find { it.component == BatteryComponent.CASE }?.status ==
                    BatteryStatus.CHARGING
            val leftBattery: Int =
                batteryNotif.getBattery().find { it.component == BatteryComponent.LEFT }?.level
                    ?: BluetoothDevice.BATTERY_LEVEL_UNKNOWN
            val rightBattery: Int =
                batteryNotif.getBattery().find { it.component == BatteryComponent.RIGHT }?.level
                    ?: BluetoothDevice.BATTERY_LEVEL_UNKNOWN
            val caseBattery: Int =
                batteryNotif.getBattery().find { it.component == BatteryComponent.CASE }?.level
                    ?: BluetoothDevice.BATTERY_LEVEL_UNKNOWN

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
            device.setMetadataInt(BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY, rightBattery)
            device.setMetadataInt(BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY, caseBattery)

            chargingMain = leftCharging && rightCharging
            batteryUnified = min(leftBattery.toDouble(), rightBattery.toDouble()).toInt()
        } else {
            val singlePods: SinglePods? = airpods as SinglePods?
            if (!isMetaDataSet && singlePods != null) {
                val metadata =
                    SinglePodsMetadata(
                        manufacturer = singlePods.manufacturer,
                        model = singlePods.model,
                        lowBattThreshold = singlePods.lowBattThreshold,
                        drawable = singlePods.drawable,
                    )
                isModelDataSet = setSinglePodsMetadata(device, metadata)
            }
            // chargingMain = singlePods.isCharging
            // batteryUnified = singlePods.getParsedStatus()
        }

        if (!isMetaDataSet) {
            isMetaDataSet = isSliceSet && isModelDataSet
        }

        if (batteryUnified == null || chargingMain == null) {
            return
        }

        device.setMetadataBoolean(BluetoothDevice.METADATA_MAIN_CHARGING, chargingMain)
        device.setMetadataInt(BluetoothDevice.METADATA_MAIN_BATTERY, batteryUnified)

        broadcastHfIndicatorEventIntent(batteryUnified, device)
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
}
