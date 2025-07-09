/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.slices

import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.net.Uri
import android.util.Log
import androidx.slice.Slice
import androidx.slice.SliceProvider
import androidx.slice.builders.ListBuilder
import com.android.bluetooth.bthelper.Constants
import com.android.bluetooth.bthelper.R

class BtHelperSliceProvider : SliceProvider() {

    private var mContext: Context? = null
    private var mSharedPrefs: SharedPreferences? = null

    override fun onCreateSliceProvider(): Boolean {
        try {
            mContext = context
            mContext?.let { ctx ->
                mSharedPrefs =
                    ctx.getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE)
            }
        } catch (e: NullPointerException) {
            Log.e(TAG, "Error in onCreateSliceProvider", e)
        }
        return true
    }

    override fun onBindSlice(sliceUri: Uri): Slice? {
        if (mContext == null) {
            return null
        }
        val path: String? = sliceUri.getPath()
        when (path) {
            Constants.SLICE_BTHELPER -> return createBtHelperSlice(sliceUri)
        }
        return null
    }

    private fun createBtHelperSlice(sliceUri: Uri): Slice? {
        val context = mContext

        if (context == null) return null

        val MORE_SETTINGS_TITLE: String = context.getString(R.string.more_settings_title)
        val MORE_SETTINGS_SUBTITLE: String = context.getString(R.string.more_settings_subtitle)

        val listBuilder: ListBuilder = ListBuilder(context, sliceUri, INFINITY)

        listBuilder.addRow(
            SliceCreator(
                    R.drawable.ic_chevron_right,
                    MORE_SETTINGS_TITLE,
                    MORE_SETTINGS_SUBTITLE,
                    false,
                    Constants.ACTION_PENDING_INTENT,
                    Constants.EXTRA_NONE,
                    context,
                    Constants.SLICE_MAIN,
                )
                .getSettingRow(sliceUri)
        )

        listBuilder.setAccentColor(getColorAccentDefaultColor(context))
        return listBuilder.build()
    }

    companion object {
        const val TAG: String = "BtHelperSliceProvider"

        /** Constant representing infinity. */
        private const val INFINITY: Long = -1

        private fun getColorAccentDefaultColor(context: Context): Int {
            return getColorAttrDefaultColor(context, android.R.attr.colorAccent, 0)
        }

        private fun getColorAttrDefaultColor(context: Context, attr: Int, defValue: Int): Int {
            val ta: TypedArray = context.obtainStyledAttributes(intArrayOf(attr))
            val colorAccent: Int = ta.getColor(0, defValue)
            ta.recycle()
            return colorAccent
        }
    }
}
