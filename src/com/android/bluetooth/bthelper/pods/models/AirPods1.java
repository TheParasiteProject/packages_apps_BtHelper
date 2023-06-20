/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.pods.models;

import com.android.bluetooth.bthelper.pods.Pod;
import com.android.bluetooth.bthelper.R;

public class AirPods1 extends RegularPods {

    public AirPods1 (String color, Pod leftPod, Pod rightPod, Pod casePod) {
        super(color, leftPod, rightPod, casePod);
    }

    @Override
    public int getDrawable () {
        return R.drawable.AirPods;
    };

    @Override
    public int getLeftDrawable () {
        return R.drawable.AirPods_Left;
    }

    @Override
    public int getRightDrawable () {
        return R.drawable.AirPods_Right;
    }

    @Override
    public int getCaseDrawable () {
        return R.drawable.AirPods_Case;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_AIRPODS_GEN1;
    }

    @Override
    public String getMenufacturer () {
        return Constants.MANUFACTURER_APPLE;
    }

}