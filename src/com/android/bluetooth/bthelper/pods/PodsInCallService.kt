/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.pods

import android.content.Intent
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService

class PodsInCallService : InCallService() {

    companion object {
        const val ACTION_ANSWER_CALL = "com.android.bluetooth.bthelper.ANSWER_CALL"
        const val ACTION_REJECT_CALL = "com.android.bluetooth.bthelper.REJECT_CALL"
        private var callInstance: Call? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ANSWER_CALL -> answerCall()
            ACTION_REJECT_CALL -> rejectCall()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        callInstance = call
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        callInstance = null
    }

    private fun answerCall() {
        callInstance?.answer(0)
    }

    private fun rejectCall() {
        callInstance?.reject(false, "")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}
