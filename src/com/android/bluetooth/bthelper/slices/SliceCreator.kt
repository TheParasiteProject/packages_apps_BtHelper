/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.slices

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.SliceAction
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.R
import com.android.bluetooth.bthelper.settings.MainSettingsActivity

class SliceCreator(
    private val icon: Int,
    private val title: String,
    private val summary: String?,
    private val enabled: Boolean,
    private val action: String,
    private val extra: Int,
    private val context: Context,
    private val type: Int,
) {
    fun getSettingRow(sliceUri: Uri): ListBuilder.RowBuilder {
        val settingRow: ListBuilder.RowBuilder = ListBuilder.RowBuilder(sliceUri)

        var ic: Int = R.drawable.ic_dummy
        if (icon != 0) ic = icon
        val iconCompat: IconCompat = IconCompat.createWithResource(context, ic)

        settingRow.setTitle(title)
        if (summary != null) {
            settingRow.setSubtitle(summary)
        }

        var sa: SliceAction? = null
        if (type == Constants.SLICE_TOGGLE) {
            sa = getToggleSlice(iconCompat)
            settingRow.addEndItem(sa)
        } else if (type == Constants.SLICE_MAIN) {
            sa = getMainSettingsSlice(iconCompat, sliceUri)
            settingRow.setPrimaryAction(sa)
        } else {
            sa = getMainSettingsSlice(iconCompat, sliceUri)
            settingRow.setPrimaryAction(sa)
        }

        return settingRow
    }

    private val broadcastIntent: PendingIntent
        get() {
            val intent: Intent =
                Intent(context, SliceBroadcastReceiver::class.java)
                    .setAction(action)
                    .putExtra(action, extra)
            return PendingIntent.getBroadcast(
                context,
                0, /* requestCode */
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        }

    private fun getToggleSlice(iconCompat: IconCompat): SliceAction {
        val actionTitle: CharSequence = title
        return SliceAction.createToggle(broadcastIntent, iconCompat, actionTitle, enabled)
    }

    private fun getMainSettingsSlice(iconCompat: IconCompat, sliceUri: Uri): SliceAction {
        val intent: Intent = Intent(context, MainSettingsActivity::class.java).setAction(action)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                sliceUri.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        val actionTitle: CharSequence = title
        return SliceAction(pendingIntent, iconCompat.toIcon(), actionTitle)
    }
}
