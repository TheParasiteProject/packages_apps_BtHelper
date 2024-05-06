/*
 * Copyright (C) 2021-2023 Matthias Urhahn
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package com.android.bluetooth.bthelper.utils;

import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;

public class MediaControl {
    private static AudioManager audioManager;
    private static MediaControl mInstance;

    public MediaControl(Context context) {
        audioManager = context.getSystemService(AudioManager.class);
    }

    public static synchronized MediaControl getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MediaControl(context);
        }
        return mInstance;
    }

    public static boolean isPlaying() {
        return audioManager.isMusicActive();
    }

    public static void sendPlay() {
        if (isPlaying()) {
            return;
        }
        sendKey(KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    public static void sendPause() {
        if (!isPlaying()) {
            return;
        }
        sendKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
    }

    public static void sendPlayPause() {
        if (isPlaying()) {
            sendPause();
        } else {
            sendPlay();
        }
    }

    private static void sendKey(int keyCode) {
        final long eventTime = SystemClock.uptimeMillis();
        audioManager.dispatchMediaKeyEvent(
                new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        audioManager.dispatchMediaKeyEvent(
                new KeyEvent(eventTime + 200, eventTime + 200, KeyEvent.ACTION_UP, keyCode, 0));
    }
}
