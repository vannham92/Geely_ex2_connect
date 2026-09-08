package com.geely.ex2.tools.data.ambient

import android.content.Context
import com.geely.ex2.tools.data.kv.AppKv
import com.tencent.mmkv.MMKV

object AmbientLightSettings {
    private const val PREFS = "geelytools_ambient_light"
    private const val KEY_AUTO_ENABLED = "auto_enabled"
    private const val KEY_SESSION_MODE = "session_mode"
    private const val KEY_START_HOUR = "start_hour"
    private const val KEY_START_MINUTE = "start_minute"
    private const val KEY_END_HOUR = "end_hour"
    private const val KEY_END_MINUTE = "end_minute"

    private const val DEFAULT_START_HOUR = 22
    private const val DEFAULT_START_MINUTE = 0
    private const val DEFAULT_END_HOUR = 7
    private const val DEFAULT_END_MINUTE = 0

    fun isAutoModeSaved(context: Context): Boolean {
        clearLegacyControlMode(context)
        return kv(context).decodeBool(KEY_AUTO_ENABLED, true)
    }

    fun getControlMode(context: Context): AmbientLightControlMode {
        if (isAutoModeSaved(context)) {
            return AmbientLightControlMode.AUTO
        }
        return decodeSessionMode(kv(context))
    }

    fun setControlMode(context: Context, mode: AmbientLightControlMode) {
        clearLegacyControlMode(context)
        val prefs = kv(context)
        when (mode) {
            AmbientLightControlMode.AUTO -> {
                prefs.encode(KEY_AUTO_ENABLED, true)
                prefs.removeValueForKey(KEY_SESSION_MODE)
            }
            AmbientLightControlMode.OFF,
            AmbientLightControlMode.ON,
            -> {
                prefs.encode(KEY_AUTO_ENABLED, false)
                prefs.encode(KEY_SESSION_MODE, mode.name)
            }
        }
    }

    fun syncSessionModeFromVehicle(mode: AmbientLightControlMode) {
        if (mode == AmbientLightControlMode.AUTO) {
            return
        }
        // Caller must have AppKv.init; use default context path via of(PREFS) needs Context.
        // Persist via last-known storage from AppKv — sync is always after settings access.
        // Keep signature; encode into named mmap if already initialized.
        AppKv.of(PREFS).encode(KEY_SESSION_MODE, mode.name)
    }

    fun isScheduleEnabled(context: Context): Boolean =
        isAutoModeSaved(context)

    fun shouldRestoreOnWake(context: Context): Boolean = isScheduleEnabled(context)

    fun getStartHour(context: Context): Int =
        kv(context).decodeInt(KEY_START_HOUR, DEFAULT_START_HOUR)

    fun getStartMinute(context: Context): Int =
        kv(context).decodeInt(KEY_START_MINUTE, DEFAULT_START_MINUTE)

    fun getEndHour(context: Context): Int =
        kv(context).decodeInt(KEY_END_HOUR, DEFAULT_END_HOUR)

    fun getEndMinute(context: Context): Int =
        kv(context).decodeInt(KEY_END_MINUTE, DEFAULT_END_MINUTE)

    fun setStartTime(context: Context, hour: Int, minute: Int) {
        val prefs = kv(context)
        prefs.encode(KEY_START_HOUR, hour.coerceIn(0, 23))
        prefs.encode(KEY_START_MINUTE, minute.coerceIn(0, 59))
    }

    fun setEndTime(context: Context, hour: Int, minute: Int) {
        val prefs = kv(context)
        prefs.encode(KEY_END_HOUR, hour.coerceIn(0, 23))
        prefs.encode(KEY_END_MINUTE, minute.coerceIn(0, 59))
    }

    private fun decodeSessionMode(prefs: MMKV): AmbientLightControlMode {
        val raw = prefs.decodeString(KEY_SESSION_MODE, null) ?: return AmbientLightControlMode.OFF
        return runCatching { AmbientLightControlMode.valueOf(raw) }.getOrDefault(AmbientLightControlMode.OFF)
            .takeUnless { it == AmbientLightControlMode.AUTO }
            ?: AmbientLightControlMode.OFF
    }

    private fun clearLegacyControlMode(context: Context) {
        val preferences = kv(context)
        if (!preferences.containsKey("control_mode")) {
            return
        }
        val legacyMode = preferences.decodeString("control_mode", null)
        preferences.removeValueForKey("control_mode")
        if (legacyMode != null && legacyMode != AmbientLightControlMode.AUTO.name) {
            preferences.encode(KEY_AUTO_ENABLED, false)
            if (legacyMode == AmbientLightControlMode.ON.name ||
                legacyMode == AmbientLightControlMode.OFF.name
            ) {
                preferences.encode(KEY_SESSION_MODE, legacyMode)
            }
        }
    }

    private fun kv(context: Context): MMKV {
        AppKv.init(context)
        return AppKv.of(PREFS)
    }
}
