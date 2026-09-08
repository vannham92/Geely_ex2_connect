package com.geely.ex2.tools.data.avas

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarVhalBindings

/**
 * Read/write AVAS speaker volume via VHAL PROP_AVAS_VOLUME (0x2140A149).
 *
 * CarAudioManager only exposes AVAS *mode* (on/off — see [CarAudioBindings]); the volume
 * level is VHAL-only on this platform. Writing it needs the same system privilege as mute
 * (CAR_CONTROL_AUDIO_VOLUME + platform signature via the `systemRelease` build).
 */
class AvasVolumeApplier(context: Context) {
    private val appContext = context.applicationContext

    fun read(): AvasVolumeSample {
        val debug = StringBuilder()
        val bindings = CarVhalBindings(appContext)
        return try {
            if (!bindings.ensureConnected(debug)) {
                return AvasVolumeSample(
                    isAvailable = false,
                    source = "CarPropertyManager null",
                    details = debug.toString(),
                )
            }
            for (area in AvasConstants.AVAS_AREAS) {
                val probe = bindings.readIntProperty(AvasConstants.PROP_AVAS_VOLUME, area)
                debug.append("\nread AVAS_VOLUME area=").append(area).append(": ")
                    .append(if (probe.ok) probe.value.toString() else probe.error)
                if (probe.ok) {
                    return AvasVolumeSample(
                        volume = probe.value.coerceIn(
                            AvasConstants.AVAS_VOLUME_MIN,
                            AvasConstants.AVAS_VOLUME_MAX,
                        ),
                        areaId = area,
                        isAvailable = true,
                        source = "VHAL AVAS_VOLUME 0x2140A149 area=$area",
                        details = debug.toString(),
                    )
                }
            }
            AvasVolumeSample(
                isAvailable = false,
                source = "AVAS_VOLUME unreadable",
                details = debug.toString(),
            )
        } finally {
            bindings.close()
        }
    }

    fun setVolume(level: Int): AvasWriteResult {
        val target = level.coerceIn(AvasConstants.AVAS_VOLUME_MIN, AvasConstants.AVAS_VOLUME_MAX)
        val debug = StringBuilder()
        val bindings = CarVhalBindings(appContext)
        return try {
            if (!bindings.ensureConnected(debug)) {
                return AvasWriteResult(ok = false, error = "CarPropertyManager null", details = debug.toString())
            }

            val errors = mutableListOf<String>()
            for (area in AvasConstants.AVAS_AREAS) {
                val write = bindings.writeIntProperty(AvasConstants.PROP_AVAS_VOLUME, target, area)
                debug.append("\nwrite AVAS_VOLUME area=").append(area)
                    .append(" value=").append(target).append(": ")
                    .append(if (write.ok) "OK" else write.error)
                if (!write.ok) {
                    write.error?.let { errors.add("AVAS_VOLUME@$area: $it") }
                    continue
                }

                val verify = bindings.readIntProperty(AvasConstants.PROP_AVAS_VOLUME, area)
                debug.append("\nverify AVAS_VOLUME area=").append(area).append(": ")
                    .append(if (verify.ok) verify.value.toString() else verify.error)
                // Accept when read-back matches, or when read is unavailable after a successful write.
                if (!verify.ok || verify.value == target) {
                    return AvasWriteResult(ok = true, details = debug.toString())
                }
                Log.w(TAG, "AVAS volume verify mismatch@$area wrote=$target read=${verify.value}")
                errors.add("verify mismatch@$area wrote=$target read=${verify.value}")
            }

            AvasWriteResult(
                ok = false,
                error = errors.joinToString("; ").ifEmpty { "AVAS_VOLUME write failed" },
                details = debug.toString(),
            )
        } finally {
            bindings.close()
        }
    }

    companion object {
        private const val TAG = "GeelyToolsAvas"
    }
}
