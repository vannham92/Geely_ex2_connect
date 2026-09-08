package com.geely.ex2.tools.data.temperature

import android.content.Context
import com.geely.ex2.tools.data.kv.AppKv
import com.tencent.mmkv.MMKV

object TemperatureSettings {
    private const val PREFS = "geelytools_temperature"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_STATUS_ICON_RANK = "status_icon_rank"
    private const val KEY_WIDGET_POSITION = "widget_position"
    private const val LEGACY_DEGREE_SYMBOL_POSITION = "degree_symbol_position"

    const val ICON_SIZE_PERCENT = 160

    fun isEnabled(context: Context): Boolean =
        kv(context).decodeBool(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        kv(context).encode(KEY_ENABLED, enabled)
    }

    fun getStatusIconRank(context: Context): Int {
        val prefs = kv(context)
        if (prefs.containsKey(KEY_STATUS_ICON_RANK)) {
            return TemperatureWidgetRank.clamp(
                prefs.decodeInt(KEY_STATUS_ICON_RANK, TemperatureWidgetRank.DEFAULT),
            )
        }

        val legacyPosition = prefs.decodeString(KEY_WIDGET_POSITION, null)
            ?: prefs.decodeString(LEGACY_DEGREE_SYMBOL_POSITION, null)
        val legacyRank = when (legacyPosition) {
            "LEFT" -> 5
            "CENTER" -> 13
            "RIGHT" -> 22
            else -> TemperatureWidgetRank.DEFAULT
        }
        return TemperatureWidgetRank.clamp(legacyRank)
    }

    fun setStatusIconRank(context: Context, rank: Int) {
        kv(context).encode(KEY_STATUS_ICON_RANK, TemperatureWidgetRank.clamp(rank))
    }

    fun stepStatusIconRank(context: Context, delta: Int): Int {
        val newRank = TemperatureWidgetRank.clamp(getStatusIconRank(context) + delta)
        setStatusIconRank(context, newRank)
        return newRank
    }

    private fun kv(context: Context): MMKV {
        AppKv.init(context)
        return AppKv.of(PREFS)
    }
}
