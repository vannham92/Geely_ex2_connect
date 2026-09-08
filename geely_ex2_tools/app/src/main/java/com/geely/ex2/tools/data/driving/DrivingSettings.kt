package com.geely.ex2.tools.data.driving

import android.content.Context
import com.geely.ex2.tools.data.kv.AppKv
import com.geely.ex2.tools.data.vhal.VhalConstants
import com.tencent.mmkv.MMKV

object DrivingSettings {
    private const val PREFS = "geelytools_driving"
    private const val KEY_PERSIST_ENABLED = "persist_enabled"
    private const val KEY_SAVED_MODE_VALUE = "saved_mode_value"
    private const val KEY_REGEN_PERSIST_ENABLED = "regen_persist_enabled"
    private const val KEY_SAVED_REGEN_VALUE = "saved_regen_value"

    fun isPersistEnabled(context: Context): Boolean =
        kv(context).decodeBool(KEY_PERSIST_ENABLED, true)

    fun setPersistEnabled(context: Context, enabled: Boolean) {
        kv(context).encode(KEY_PERSIST_ENABLED, enabled)
    }

    fun getSavedModeValue(context: Context): Int =
        kv(context).decodeInt(KEY_SAVED_MODE_VALUE, VhalConstants.DRIVE_MODE_COMFORT)

    fun setSavedModeValue(context: Context, modeValue: Int) {
        kv(context).encode(KEY_SAVED_MODE_VALUE, modeValue)
    }

    fun hasSavedMode(context: Context): Boolean =
        kv(context).containsKey(KEY_SAVED_MODE_VALUE)

    fun isRegenPersistEnabled(context: Context): Boolean =
        kv(context).decodeBool(KEY_REGEN_PERSIST_ENABLED, false)

    fun setRegenPersistEnabled(context: Context, enabled: Boolean) {
        kv(context).encode(KEY_REGEN_PERSIST_ENABLED, enabled)
    }

    fun getSavedRegenValue(context: Context): Int {
        val raw = kv(context).decodeInt(
            KEY_SAVED_REGEN_VALUE,
            VhalConstants.ENERGY_REGENERATION_LEVEL_MID,
        )
        val normalized = EnergyRegenerationValues.normalizeStoredValue(raw)
        if (normalized != raw) {
            setSavedRegenValue(context, normalized)
        }
        return normalized
    }

    fun setSavedRegenValue(context: Context, levelValue: Int) {
        kv(context).encode(
            KEY_SAVED_REGEN_VALUE,
            EnergyRegenerationValues.normalizeStoredValue(levelValue),
        )
    }

    fun hasAnyPersistEnabled(context: Context): Boolean =
        isPersistEnabled(context) || isRegenPersistEnabled(context)

    private fun kv(context: Context): MMKV {
        AppKv.init(context)
        return AppKv.of(PREFS)
    }
}
