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
    public static boolean isPlaying (Context context) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isMusicActive();
    }

    public static void sendPlay (Context context) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isMusicActive()) {
            return;
        }
        sendKey(context, KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    public static void sendPause (Context context) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (!audioManager.isMusicActive()) {
            return;
        }
        sendKey(context, KeyEvent.KEYCODE_MEDIA_PAUSE);
    }

    public static void sendPlayPause (Context context) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isMusicActive()) {
            sendPause(context);
        } else {
            sendPlay(context);
        }
    }

    private static void sendKey (Context context, int keyCode) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final long eventTime = SystemClock.uptimeMillis();
        audioManager.dispatchMediaKeyEvent(new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0));
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        audioManager.dispatchMediaKeyEvent(new KeyEvent(eventTime + 200, eventTime + 200, KeyEvent.ACTION_UP, keyCode, 0));
    }
}
