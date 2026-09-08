package com.geely.ex2.tools.data.avas

object AvasConstants {
    /** Car.getCarManager("audio") → CarAudioManager */
    const val CAR_MANAGER_AUDIO = "audio"

    /** setAVASMode(0) — mute / Desligado (CentralEXAuto) */
    const val MODE_MUTED = 0

    /** Chế độ hoạt động mặc định khi unmute (Kiểu 1) */
    const val MODE_DEFAULT_ACTIVE = 1

    /**
     * VHAL OEM (VehicleProperty) — fallback, nếu không có CAR_CONTROL_AUDIO_VOLUME.
     * CentralEXAuto đi qua CarAudioManager; đối với APK thông thường thường chỉ có VHAL.
     */
    const val PROP_AVAS_SWITCH = 557883720 // 0x2140A148
    const val PROP_AVAS_VOLUME = 557883721 // 0x2140A149
    const val PROP_AVAS_DISABLED_SET = 557887627 // 0x2140B08B

    /**
     * AVAS volume level range written to PROP_AVAS_VOLUME.
     * Firmware-dependent — Geely EX2 exposes discrete steps; adjust MAX if the HAL
     * reports a different config range. Writes are clamped to [MIN, MAX].
     */
    const val AVAS_VOLUME_MIN = 0
    const val AVAS_VOLUME_MAX = 7
    const val AVAS_VOLUME_STEP = 1
    const val AVAS_VOLUME_DEFAULT = 3

    /** Settings.Global keys из CarAudioService (Common.*) */
    const val SETTINGS_AVAS_MODE = "audio_avas_mode"
    const val SETTINGS_LAST_AVAS_MODE = "audio_last_avas_mode"

    const val AVAS_UI_POLL_INTERVAL_MS = 3_000L

    /** Độ trễ CentralEXAuto scheduleAvasRestore sau khi boot */
    const val AVAS_RESTORE_DELAY_MS = 12_000L

    const val AVAS_RESTORE_VERIFY_BASE_MS = 800L
    const val AVAS_RESTORE_VERIFY_STEP_MS = 800L
    const val AVAS_RESTORE_WRITE_ATTEMPTS = 3

    val AVAS_AREAS: IntArray = intArrayOf(0, 1)
}
