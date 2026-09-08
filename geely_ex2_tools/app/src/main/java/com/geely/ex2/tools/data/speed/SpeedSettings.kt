package com.geely.ex2.tools.data.speed

import android.content.Context
import com.geely.ex2.tools.data.kv.AppKv
import com.tencent.mmkv.MMKV

object SpeedSettings {
    private const val PREFS = "geelytools_speed"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_STATUS_ICON_RANK = "status_icon_rank"

    const val ICON_SIZE_PERCENT = 160

    fun isEnabled(context: Context): Boolean =
        kv(context).decodeBool(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        kv(context).encode(KEY_ENABLED, enabled)
    }

    fun getStatusIconRank(context: Context): Int {
        return SpeedWidgetRank.clamp(
            kv(context).decodeInt(KEY_STATUS_ICON_RANK, SpeedWidgetRank.DEFAULT),
        )
    }

    fun setStatusIconRank(context: Context, rank: Int) {
        kv(context).encode(KEY_STATUS_ICON_RANK, SpeedWidgetRank.clamp(rank))
    }

    fun stepStatusIconRank(context: Context, delta: Int): Int {
        val newRank = SpeedWidgetRank.clamp(getStatusIconRank(context) + delta)
        setStatusIconRank(context, newRank)
        return newRank
    }

    private fun kv(context: Context): MMKV {
        AppKv.init(context)
        return AppKv.of(PREFS)
    }
}
