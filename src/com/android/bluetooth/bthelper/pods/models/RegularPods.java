/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.pods.models;

import com.android.bluetooth.bthelper.R;
import com.android.bluetooth.bthelper.pods.Pod;

import java.util.Locale;

public class RegularPods implements IPods {

    public static final int LEFT = 0, RIGHT = 1, CASE = 2;

    private final Pod[] pods;

    public RegularPods (Pod leftPod, Pod rightPod, Pod casePod) {
        this.pods = new Pod[] {leftPod, rightPod, casePod};
    }

    public Pod getPod (int pos) {
        return pods[pos];
    }

    public String getParsedStatus (int pos) {
        return pods[pos].parseStatus();
    }

    public int getLeftDrawable () {
        return getPod(LEFT).isConnected() ? R.drawable.pod : R.drawable.pod_disconnected;
    }

    public int getRightDrawable () {
        return getPod(RIGHT).isConnected() ? R.drawable.pod : R.drawable.pod_disconnected;
    }

    public int getCaseDrawable () {
        return getPod(CASE).isConnected() ? R.drawable.pod_case : R.drawable.pod_case_disconnected;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_UNKNOWN;
    }

    @Override
    public boolean isSingle () {
        return false;
    }

    @Override
    public boolean isDisconnected () {
        return pods[LEFT].isDisconnected() &&
                pods[RIGHT].isDisconnected() &&
                pods[CASE].isDisconnected();
    }

}