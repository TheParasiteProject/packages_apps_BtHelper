/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.pods.models;

import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.pods.Pod;

public class AirPodsMax extends SinglePods {

    public AirPodsMax (Pod singlePod) {
        super(singlePod);
    }

    @Override
    public int getDrawable () {
        return getPod().isConnected() ? R.drawable.podmax : R.drawable.podmax_disconnected;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_AIRPODS_MAX;
    }

}