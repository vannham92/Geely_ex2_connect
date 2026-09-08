package com.geely.ex2.tools.data.avas

import android.content.Context
import com.geely.ex2.tools.data.kv.AppKv
import com.tencent.mmkv.MMKV

object AvasSettings {
    private const val PREFS = "geelytools_avas"
    private const val KEY_MUTED = "avas_muted_saved"
    private const val KEY_LAST_ACTIVE_MODE = "avas_last_active_mode"
    private const val KEY_LAST_VOLUME = "avas_last_volume"

    fun isMutedSaved(context: Context): Boolean =
        kv(context).decodeBool(KEY_MUTED, false)

    fun setMutedSaved(context: Context, muted: Boolean) {
        kv(context).encode(KEY_MUTED, muted)
    }

    fun getLastActiveMode(context: Context): Int {
        val mode = kv(context).decodeInt(KEY_LAST_ACTIVE_MODE, AvasConstants.MODE_DEFAULT_ACTIVE)
        return if (mode > AvasConstants.MODE_MUTED) mode else AvasConstants.MODE_DEFAULT_ACTIVE
    }

    fun setLastActiveMode(context: Context, mode: Int) {
        if (mode <= AvasConstants.MODE_MUTED) return
        kv(context).encode(KEY_LAST_ACTIVE_MODE, mode)
    }

    fun getLastVolume(context: Context): Int =
        kv(context).decodeInt(KEY_LAST_VOLUME, AvasConstants.AVAS_VOLUME_DEFAULT)
            .coerceIn(AvasConstants.AVAS_VOLUME_MIN, AvasConstants.AVAS_VOLUME_MAX)

    fun setLastVolume(context: Context, volume: Int) {
        kv(context).encode(
            KEY_LAST_VOLUME,
            volume.coerceIn(AvasConstants.AVAS_VOLUME_MIN, AvasConstants.AVAS_VOLUME_MAX),
        )
    }

    private fun kv(context: Context): MMKV {
        AppKv.init(context)
        return AppKv.of(PREFS)
    }
}
