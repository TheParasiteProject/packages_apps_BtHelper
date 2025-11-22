/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils

import android.app.ActivityTaskManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log

class CameraStateMonitor(private val context: Context) {

    interface Listener {
        fun onCameraOpened()

        fun onCameraClosed()
    }

    private val handler = Handler(Looper.getMainLooper())

    private val cameraManager: CameraManager? by lazy {
        context.getSystemService(CameraManager::class.java)
    }

    private var listeners: MutableList<Listener> = emptyList()
    private var listenersLock: Any = Any()

    fun onCreate() {
        cameraManager?.registerAvailabilityCallback(availabilityCallback, handler)
    }

    fun registerListener(listener: Listener) {
        synchronized(listenersLock) { listeners.add(listener) }
    }

    fun unregisterListener(listener: Listener) {
        synchronized(listenersLock) { listeners.remove(listener) }
    }

    fun onDestroy() {
        cameraManager?.unregisterAvailabilityCallback(availabilityCallback)
        cameraManager = null
        listeners.clear()
    }

    private val availabilityCallback =
        object : CameraManager.AvailabilityCallback() {
            override fun onCameraUnavailable(cameraId: String) {
                handleCameraStateChange(isCameraOpen = true)
            }

            override fun onCameraAvailable(cameraId: String) {
                val listenersCopy: List<Listener>
                synchronized(listenersLock) { listenersCopy = List(listeners) }

                listenersCopy.forEach { listener -> listener.onCameraClosed() }
            }
        }

    private fun handleCameraStateChange(isCameraOpen: Boolean) {
        if (!isCameraOpen || listeners.isEmpty()) return

        val foregroundPackage = foregroundPackageName

        if (foregroundPackage != null && CAMERA_PACKAGE_WHITELIST.contains(foregroundPackage)) {
            val listenersCopy: List<Listener>
            synchronized(listenersLock) { listenersCopy = List(listeners) }

            listenersCopy.forEach { listener -> listener.onCameraOpened() }
        }
    }

    private val foregroundPackageName: String?
        get() {
            return try {
                val info = ActivityTaskManager.getService().focusedRootTaskInfo
                info?.topActivity?.packageName
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to query ActivityTaskManager", e)
                null
            }
        }

    companion object {
        private const val TAG = "CameraStateMonitor"

        private val CAMERA_PACKAGE_WHITELIST =
            setOf(
                "com.android.camera2",
                "com.android.camera",
                "com.google.android.apps.cameralite",
                "com.google.android.GoogleCamera",
                "com.motorola.camera2",
                "com.oppo.camera",
                "com.sec.android.app.camera",
                "org.codeaurora.snapcam",
                "org.lineageos.aperture",
            )
    }
}
