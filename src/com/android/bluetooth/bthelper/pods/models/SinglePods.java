/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.pods.models;

import com.android.bluetooth.bthelper.pods.Pod;

import java.util.Locale;

public abstract class SinglePods implements IPods {

    public abstract int getDrawable ();

    private final Pod pod;

    public SinglePods (Pod pod) {
        this.pod = pod;
    }

    public Pod getPod () {
        return pod;
    }

    public String getParsedStatus () {
        return pod.parseStatus();
    }

    public int getBatImgVisibility () {
        return pod.batImgVisibility();
    }

    public int getBatImgSrcId () {
        return pod.batImgSrcId();
    }

    @Override
    public boolean isSingle () {
        return true;
    }

    @Override
    public boolean isDisconnected () {
        return pod.isDisconnected();
    }

}