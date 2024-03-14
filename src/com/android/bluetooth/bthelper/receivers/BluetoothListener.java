/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.receivers;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

public abstract class BluetoothListener implements BluetoothProfile.ServiceListener {

    public abstract boolean onConnect (BluetoothDevice bluetoothDevice);

    public abstract void onDisconnect ();

    @Override
    public void onServiceConnected (int profile, BluetoothProfile bluetoothProfile) {
        if (profile == BluetoothProfile.HEADSET)
            for (BluetoothDevice device : bluetoothProfile.getConnectedDevices())
                if (onConnect(device))
                    break;
    }

    @Override
    public void onServiceDisconnected (int profile) {
        if (profile == BluetoothProfile.HEADSET)
            onDisconnect();
    }

}