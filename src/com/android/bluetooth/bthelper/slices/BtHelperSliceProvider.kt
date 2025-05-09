/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package com.android.bluetooth.bthelper.slices

import android.annotation.ColorInt
import com.android.bluetooth.bthelper.Constants

class BtHelperSliceProvider : SliceProvider() {
    private var mContext: Context? = null

    override fun onCreateSliceProvider(): Boolean {
        try {
            mContext = getContext()
            mSharedPrefs =
                mContext.getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE)
        } catch (e: NullPointerException) {}
        return true
    }

    override fun onBindSlice(sliceUri: Uri): Slice? {
        if (mContext == null) {
            return null
        }
        val path: String = sliceUri.getPath()
        when (path) {
            Constants.SLICE_BTHELPER -> return createBtHelperSlice(sliceUri)
        }
        return null
    }

    private fun createBtHelperSlice(sliceUri: Uri): Slice? {
        try {
            if (mContext == null) return null
        } catch (e: NullPointerException) {}

        /*
                final String ONEPOD_TITLE = mContext.getString(R.string.onepod_mode_title);
                final boolean onePodModeEnabled = mSharedPrefs.getBoolean(Constants.KEY_ONEPOD_MODE, false);

                final String AUTO_PLAY_TITLE = mContext.getString(R.string.auto_play_title);
                final boolean autoPlayEnabled = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PLAY, false);

                final String AUTO_PAUSE_TITLE = mContext.getString(R.string.auto_pause_title);
                final boolean autoPauseEnabled = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PAUSE, false);
        */
        val MORE_SETTINGS_TITLE: String = mContext.getString(R.string.more_settings_title)
        val MORE_SETTINGS_SUBTITLE: String = mContext.getString(R.string.more_settings_subtitle)

        val listBuilder: ListBuilder = ListBuilder(mContext, sliceUri, INFINITY)

        /*
                listBuilder.addRow(new SliceCreator(
                        0,
                        ONEPOD_TITLE,
                        null,
                        onePodModeEnabled,
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_ONEPOD_CHANGED,
                        mContext,
                        Constants.SLICE_TOGGLE
                    ).getSettingRow(sliceUri));

                listBuilder.addRow(new SliceCreator(
                        0,
                        AUTO_PLAY_TITLE,
                        null,
                        autoPlayEnabled,
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_AUTO_PLAY_CHANGED,
                        mContext,
                        Constants.SLICE_TOGGLE
                    ).getSettingRow(sliceUri));

                listBuilder.addRow(new SliceCreator(
                        0,
                        AUTO_PAUSE_TITLE,
                        null,
                        autoPauseEnabled,
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_AUTO_PAUSE_CHANGED,
                        mContext,
                        Constants.SLICE_TOGGLE
                    ).getSettingRow(sliceUri));
        */
        listBuilder.addRow(
            SliceCreator(
                    R.drawable.ic_chevron_right,
                    MORE_SETTINGS_TITLE,
                    MORE_SETTINGS_SUBTITLE,
                    false,
                    Constants.ACTION_PENDING_INTENT,
                    Constants.EXTRA_NONE,
                    mContext,
                    Constants.SLICE_MAIN,
                )
                .getSettingRow(sliceUri)
        )

        listBuilder.setAccentColor(getColorAccentDefaultColor(mContext))
        return listBuilder.build()
    }

    companion object {
        private var mSharedPrefs: SharedPreferences? = null

        /** Constant representing infinity. */
        private const val INFINITY: Long = -1

        @ColorInt
        private fun getColorAccentDefaultColor(context: Context): Int {
            return getColorAttrDefaultColor(context, android.R.attr.colorAccent, 0)
        }

        private fun getColorAttrDefaultColor(
            context: Context,
            attr: Int,
            @ColorInt defValue: Int,
        ): Int {
            val ta: TypedArray = context.obtainStyledAttributes(intArrayOf(attr))
            @ColorInt val colorAccent: Int = ta.getColor(0, defValue)
            ta.recycle()
            return colorAccent
        }
    }
}
