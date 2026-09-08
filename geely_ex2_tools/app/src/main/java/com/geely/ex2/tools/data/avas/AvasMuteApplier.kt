package com.geely.ex2.tools.data.avas

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarVhalBindings

/**
 * Mute AVAS the way CentralEXAuto does (CarAudioManager.setAVASMode),
 * with VHAL + Settings.Global fallbacks for apps without CAR_CONTROL_AUDIO_VOLUME.
 */
class AvasMuteApplier(context: Context) {
    private val appContext = context.applicationContext

    fun read(): AvasSample {
        val debug = StringBuilder()

        val audio = tryCarAudioRead(debug)
        if (audio != null) return audio

        val settings = trySettingsRead(debug)
        if (settings != null) return settings

        val vhal = tryVhalRead(debug)
        if (vhal != null) return vhal

        return AvasSample(
            isAvailable = false,
            source = "AVAS unreadable",
            details = debug.toString(),
        )
    }

    fun setMuted(muted: Boolean, lastActiveMode: Int): AvasWriteResult {
        val debug = StringBuilder()
        val errors = mutableListOf<String>()

        val audio = tryCarAudioWrite(muted, lastActiveMode, debug)
        if (audio.ok) {
            return AvasWriteResult(ok = true, details = debug.toString())
        }
        audio.error?.let { errors.add("CarAudio: $it") }

        val settings = trySettingsWrite(muted, lastActiveMode, debug)
        if (settings.ok) {
            // Settings.Global alone may not drive HAL — require VHAL (or verified read).
            val vhalAfter = tryVhalWrite(muted, debug)
            if (vhalAfter.ok) {
                return AvasWriteResult(ok = true, details = debug.toString())
            }
            vhalAfter.error?.let { errors.add("VHAL after Settings: $it") }
            if (verifyTargetState(muted, debug)) {
                return AvasWriteResult(ok = true, details = debug.toString())
            }
            errors.add("Settings wrote but state not confirmed")
        } else {
            settings.error?.let { errors.add("Settings: $it") }
        }

        val vhal = tryVhalWrite(muted, debug)
        if (vhal.ok) {
            return AvasWriteResult(ok = true, details = debug.toString())
        }
        vhal.error?.let { errors.add("VHAL: $it") }

        return AvasWriteResult(
            ok = false,
            error = errors.joinToString("; ").ifEmpty { "all AVAS write paths failed" },
            details = debug.toString(),
        )
    }

    private fun verifyTargetState(muted: Boolean, debug: StringBuilder): Boolean {
        val sample = read()
        debug.append("\nverify read: available=").append(sample.isAvailable)
            .append(" muted=").append(sample.isMuted)
            .append(" source=").append(sample.source)
        return sample.isAvailable && sample.isMuted == muted
    }

    private fun tryCarAudioRead(debug: StringBuilder): AvasSample? {
        val bindings = CarAudioBindings(appContext)
        return try {
            if (!bindings.ensureConnected(debug)) {
                debug.append("\nCarAudio: not connected")
                return null
            }
            val supported = bindings.isAvasModeSupported()
            debug.append("\nisAVASModeSupported: ").append(supported)
            val modeProbe = bindings.getAvasMode()
            if (!modeProbe.ok) {
                debug.append("\ngetAVASMode: ERROR ").append(modeProbe.error)
                return null
            }
            val mode = modeProbe.value
            debug.append("\ngetAVASMode: ").append(mode)
            AvasSample(
                mode = mode,
                isMuted = mode == AvasConstants.MODE_MUTED,
                isSupported = supported,
                isAvailable = true,
                source = "CarAudioManager.getAVASMode",
                details = debug.toString(),
            )
        } finally {
            bindings.close()
        }
    }

    private fun tryCarAudioWrite(
        muted: Boolean,
        lastActiveMode: Int,
        debug: StringBuilder,
    ): AvasWriteResult {
        // Giống QuickAccess: gọi Car.createCar mới mỗi lần write, sau đó disconnect
        val bindings = CarAudioBindings(appContext)
        return try {
            if (!bindings.ensureConnected(debug)) {
                return AvasWriteResult(ok = false, error = "CarAudioManager null")
            }
            val target = if (muted) {
                AvasConstants.MODE_MUTED
            } else {
                lastActiveMode.coerceAtLeast(AvasConstants.MODE_DEFAULT_ACTIVE)
            }
            val write = bindings.setAvasMode(target)
            debug.append("\nsetAVASMode(").append(target).append("): ")
                .append(if (write.ok) "OK" else write.error)
            if (!write.ok) {
                return AvasWriteResult(ok = false, error = write.error)
            }
            val verify = bindings.getAvasMode()
            debug.append("\nverify getAVASMode: ")
                .append(if (verify.ok) verify.value.toString() else verify.error)
            if (verify.ok) {
                val expectMuted = target == AvasConstants.MODE_MUTED
                val actuallyMuted = verify.value == AvasConstants.MODE_MUTED
                if (actuallyMuted == expectMuted) {
                    return AvasWriteResult(ok = true)
                }
                Log.w(TAG, "CarAudio verify mismatch: wrote $target read ${verify.value}")
                return AvasWriteResult(
                    ok = false,
                    error = "verify mismatch wrote=$target read=${verify.value}",
                )
            }
            // Read unavailable after successful invoke — accept write, fallthrough handles worse cases
            AvasWriteResult(ok = true)
        } catch (t: Throwable) {
            Log.w(TAG, "CarAudio write failed", t)
            AvasWriteResult(ok = false, error = t.message ?: t.javaClass.simpleName)
        } finally {
            bindings.close()
        }
    }

    private fun trySettingsRead(debug: StringBuilder): AvasSample? {
        return try {
            val mode = Settings.Global.getInt(
                appContext.contentResolver,
                AvasConstants.SETTINGS_AVAS_MODE,
                -1,
            )
            debug.append("\nSettings.Global.").append(AvasConstants.SETTINGS_AVAS_MODE)
                .append("=").append(mode)
            if (mode < 0) return null
            AvasSample(
                mode = mode,
                isMuted = mode == AvasConstants.MODE_MUTED,
                isSupported = true,
                isAvailable = true,
                source = "Settings.Global.audio_avas_mode",
                details = debug.toString(),
            )
        } catch (t: Throwable) {
            debug.append("\nSettings read ERROR: ").append(t.message)
            null
        }
    }

    private fun trySettingsWrite(
        muted: Boolean,
        lastActiveMode: Int,
        debug: StringBuilder,
    ): AvasWriteResult {
        return try {
            val cr = appContext.contentResolver
            if (muted) {
                val current = Settings.Global.getInt(cr, AvasConstants.SETTINGS_AVAS_MODE, lastActiveMode)
                if (current > AvasConstants.MODE_MUTED) {
                    Settings.Global.putInt(cr, AvasConstants.SETTINGS_LAST_AVAS_MODE, current)
                } else {
                    Settings.Global.putInt(
                        cr,
                        AvasConstants.SETTINGS_LAST_AVAS_MODE,
                        lastActiveMode.coerceAtLeast(AvasConstants.MODE_DEFAULT_ACTIVE),
                    )
                }
                val ok = Settings.Global.putInt(cr, AvasConstants.SETTINGS_AVAS_MODE, 0)
                debug.append("\nSettings put audio_avas_mode=0: ").append(ok)
                if (!ok) return AvasWriteResult(ok = false, error = "Settings.putInt returned false")
            } else {
                val target = lastActiveMode.coerceAtLeast(AvasConstants.MODE_DEFAULT_ACTIVE)
                val ok = Settings.Global.putInt(cr, AvasConstants.SETTINGS_AVAS_MODE, target)
                debug.append("\nSettings put audio_avas_mode=").append(target).append(": ").append(ok)
                if (!ok) return AvasWriteResult(ok = false, error = "Settings.putInt returned false")
            }
            AvasWriteResult(ok = true)
        } catch (t: Throwable) {
            debug.append("\nSettings write ERROR: ").append(t.message)
            AvasWriteResult(ok = false, error = t.message ?: t.javaClass.simpleName)
        }
    }

    private fun tryVhalRead(debug: StringBuilder): AvasSample? {
        val bindings = CarVhalBindings(appContext)
        return try {
            if (!bindings.ensureConnected(debug)) return null

            for (area in AvasConstants.AVAS_AREAS) {
                val disabled = bindings.readIntProperty(AvasConstants.PROP_AVAS_DISABLED_SET, area)
                debug.append("\nAVAS_DISABLED_SET area=").append(area).append(": ")
                    .append(if (disabled.ok) disabled.value.toString() else disabled.error)
                if (disabled.ok) {
                    val muted = disabled.value != 0
                    return AvasSample(
                        mode = if (muted) AvasConstants.MODE_MUTED else AvasConstants.MODE_DEFAULT_ACTIVE,
                        isMuted = muted,
                        isSupported = true,
                        isAvailable = true,
                        source = "VHAL AVAS_DISABLED_SET 0x2140B08B",
                        details = debug.toString(),
                    )
                }
            }

            for (area in AvasConstants.AVAS_AREAS) {
                val sw = bindings.readSwitchProperty(AvasConstants.PROP_AVAS_SWITCH, area)
                debug.append("\nAVASSwitch area=").append(area).append(": ")
                    .append(if (sw.ok) "${sw.value} (${sw.valueType})" else sw.error)
                if (sw.ok) {
                    return AvasSample(
                        mode = if (sw.value) AvasConstants.MODE_DEFAULT_ACTIVE else AvasConstants.MODE_MUTED,
                        isMuted = !sw.value,
                        isSupported = true,
                        isAvailable = true,
                        source = "VHAL AVASSwitch 0x2140A148",
                        details = debug.toString(),
                    )
                }
            }
            null
        } finally {
            bindings.close()
        }
    }

    private fun tryVhalWrite(muted: Boolean, debug: StringBuilder): AvasWriteResult {
        val bindings = CarVhalBindings(appContext)
        return try {
            if (!bindings.ensureConnected(debug)) {
                return AvasWriteResult(ok = false, error = "CarPropertyManager null")
            }

            val errors = mutableListOf<String>()
            var primaryOk = false

            for (area in AvasConstants.AVAS_AREAS) {
                val disabledValue = if (muted) 1 else 0
                val disabled = bindings.writeIntProperty(
                    AvasConstants.PROP_AVAS_DISABLED_SET,
                    disabledValue,
                    area,
                )
                debug.append("\nwrite AVAS_DISABLED_SET area=").append(area)
                    .append(" value=").append(disabledValue).append(": ")
                    .append(if (disabled.ok) "OK" else disabled.error)
                if (disabled.ok) {
                    primaryOk = true
                    break
                }
                disabled.error?.let { errors.add("DISABLED_SET@$area: $it") }
            }

            if (!primaryOk) {
                for (area in AvasConstants.AVAS_AREAS) {
                    val sw = bindings.writeSwitchProperty(
                        AvasConstants.PROP_AVAS_SWITCH,
                        enabled = !muted,
                        areaId = area,
                    )
                    debug.append("\nwrite AVASSwitch area=").append(area)
                        .append(" enabled=").append(!muted).append(": ")
                        .append(if (sw.ok) "OK" else sw.error)
                    if (sw.ok) {
                        primaryOk = true
                        break
                    }
                    sw.error?.let { errors.add("AVASSwitch@$area: $it") }
                }
            }

            if (primaryOk) {
                AvasWriteResult(ok = true)
            } else {
                AvasWriteResult(ok = false, error = errors.joinToString("; ").ifEmpty { "VHAL write failed" })
            }
        } finally {
            bindings.close()
        }
    }

    companion object {
        private const val TAG = "GeelyToolsAvas"
    }
}
