package com.geely.ex2.tools.data.battery

import android.content.Context
import com.geely.ex2.tools.data.kv.AppKv
import com.tencent.mmkv.MMKV

object BatterySettings {
    private const val PREFS = "geelytools_battery"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_STATUS_ICON_RANK = "status_icon_rank"

    const val ICON_SIZE_PERCENT = 160

    fun isEnabled(context: Context): Boolean =
        kv(context).decodeBool(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        kv(context).encode(KEY_ENABLED, enabled)
    }

    fun getStatusIconRank(context: Context): Int {
        return BatteryWidgetRank.clamp(
            kv(context).decodeInt(KEY_STATUS_ICON_RANK, BatteryWidgetRank.DEFAULT),
        )
    }

    fun setStatusIconRank(context: Context, rank: Int) {
        kv(context).encode(KEY_STATUS_ICON_RANK, BatteryWidgetRank.clamp(rank))
    }

    fun stepStatusIconRank(context: Context, delta: Int): Int {
        val newRank = BatteryWidgetRank.clamp(getStatusIconRank(context) + delta)
        setStatusIconRank(context, newRank)
        return newRank
    }

    private fun kv(context: Context): MMKV {
        AppKv.init(context)
        return AppKv.of(PREFS)
    }
}
