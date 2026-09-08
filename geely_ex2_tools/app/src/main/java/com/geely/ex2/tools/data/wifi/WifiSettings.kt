package com.geely.ex2.tools.data.wifi

import android.content.Context
import com.geely.ex2.tools.data.kv.AppKv
import com.tencent.mmkv.MMKV

object WifiSettings {
    private const val PREFS = "wifi_status_auto_enable_prefs"
    private const val KEY_STATUS_ICON_RANK = "status_icon_rank"

    fun getStatusIconRank(context: Context): Int {
        return WifiWidgetRank.clamp(
            kv(context).decodeInt(KEY_STATUS_ICON_RANK, WifiWidgetRank.DEFAULT),
        )
    }

    fun setStatusIconRank(context: Context, rank: Int) {
        kv(context).encode(KEY_STATUS_ICON_RANK, WifiWidgetRank.clamp(rank))
    }

    fun stepStatusIconRank(context: Context, delta: Int): Int {
        val newRank = WifiWidgetRank.clamp(getStatusIconRank(context) + delta)
        setStatusIconRank(context, newRank)
        return newRank
    }

    private fun kv(context: Context): MMKV {
        AppKv.init(context)
        return AppKv.of(PREFS)
    }
}
