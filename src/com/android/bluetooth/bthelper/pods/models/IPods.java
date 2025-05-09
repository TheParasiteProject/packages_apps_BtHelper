/*
 * SPDX-FileCopyrightText: Federico Dossena
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.pods.models;

public interface IPods {

    String getModel();

    boolean isSingle();

    boolean isDisconnected();

    int getLowBattThreshold();

    String getMenufacturer();
}
