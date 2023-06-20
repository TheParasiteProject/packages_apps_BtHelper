/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.pods.models;

import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.pods.Pod;

public class AirPodsPro extends RegularPods {

    public AirPodsPro (Pod leftPod, Pod rightPod, Pod casePod) {
        super(leftPod, rightPod, casePod);
    }

    @Override
    public int getLeftDrawable () {
        return getPod(LEFT).isConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected;
    }

    @Override
    public int getRightDrawable () {
        return getPod(RIGHT).isConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected;
    }

    @Override
    public int getCaseDrawable () {
        return getPod(CASE).isConnected() ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_AIRPODS_PRO;
    }

}