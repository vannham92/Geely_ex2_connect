package com.geely.ex2.tools.data.vhal

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.driving.EcarxEnergyRegenerationApi
import com.geely.ex2.tools.data.driving.FlymeEnergyRegenerationApi
import java.util.Locale

class CarPropertyEnergyRegenerationReader(private val context: Context) {
    private val bindings = CarVhalBindings(context)

    fun readEnergyRegeneration(): EnergyRegenerationSample {
        val debug = StringBuilder()

        // CentralEXAuto đọc/ghi regen thông qua VHAL — để UI đồng bộ với giá trị ghi thực tế.
        val vhalSample = readViaVhal(debug)
        if (vhalSample.isAvailable) {
            return vhalSample
        }

        if (FlymeEnergyRegenerationApi.isAvailable(context)) {
            val flymeValue = FlymeEnergyRegenerationApi.readLevelValue(context)
            if (flymeValue != null) {
                debug.append('\n').append("Flyme mValue: 0x").append(flymeValue.toString(16))
                return EnergyRegenerationSample(
                    levelValue = flymeValue,
                    isAvailable = true,
                    source = "Flyme EnumFuncLiveData",
                    details = debug.toString(),
                )
            }
            debug.append('\n').append("Flyme read: unavailable")
        } else {
            debug.append('\n').append("Flyme read: skipped")
        }

        if (EcarxEnergyRegenerationApi.isAvailable(context)) {
            val ecarxValue = EcarxEnergyRegenerationApi.readLevelValue(context)
            if (ecarxValue != null) {
                debug.append('\n').append("eCarX read: 0x").append(ecarxValue.toString(16))
                return EnergyRegenerationSample(
                    levelValue = ecarxValue,
                    isAvailable = true,
                    source = "eCarX ICarFunction",
                    details = debug.toString(),
                )
            }
            debug.append('\n').append("eCarX read: unavailable")
        } else {
            debug.append('\n').append("eCarX read: skipped")
        }

        return vhalSample
    }

    fun writeEnergyRegeneration(levelValue: Int): EnergyRegenerationWriteResult {
        val debug = StringBuilder()

        // CentralEXAuto chỉ ghi regen thông qua VHAL setIntProperty(areas 0/1).
        // Flyme Force thường "thành công" nhưng không có hiệu lực → trước đây làm chặn fallback.
        if (writeViaVhal(levelValue, debug)) {
            invalidateFlymeCache()
            if (verifyLevel(levelValue, debug, preferVhal = true)) {
                return EnergyRegenerationWriteResult(
                    ok = true,
                    requestedValue = levelValue,
                    error = null,
                )
            }
            debug.append('\n').append("VHAL write: no verify yet, continue")
        } else {
            debug.append('\n').append("VHAL write: failed")
        }

        if (FlymeEnergyRegenerationApi.writeLevelValue(context, levelValue)) {
            debug.append('\n').append("Flyme write: invoked")
            invalidateFlymeCache()
            if (verifyLevel(levelValue, debug, preferVhal = false)) {
                return EnergyRegenerationWriteResult(
                    ok = true,
                    requestedValue = levelValue,
                    error = null,
                )
            }
            debug.append('\n').append("Flyme write: not verified")
        } else {
            debug.append('\n').append("Flyme write: failed")
        }

        if (EcarxEnergyRegenerationApi.writeLevelValue(context, levelValue)) {
            debug.append('\n').append("eCarX write: OK")
            invalidateFlymeCache()
            if (verifyLevel(levelValue, debug, preferVhal = true)) {
                return EnergyRegenerationWriteResult(
                    ok = true,
                    requestedValue = levelValue,
                    error = null,
                )
            }
            debug.append('\n').append("eCarX write: not verified")
        } else {
            debug.append('\n').append("eCarX write: failed")
        }

        // Nỗ lực cuối cùng: gọi lại VHAL + dừng ngắn để verify.
        if (writeViaVhal(levelValue, debug)) {
            invalidateFlymeCache()
            repeat(3) { attempt ->
                Thread.sleep(300L * (attempt + 1))
                if (verifyLevel(levelValue, debug, preferVhal = true)) {
                    return EnergyRegenerationWriteResult(
                        ok = true,
                        requestedValue = levelValue,
                        error = null,
                    )
                }
            }
        }

        Log.w(TAG, "Regen write failed for 0x${levelValue.toString(16)}:\n$debug")
        return EnergyRegenerationWriteResult(
            ok = false,
            requestedValue = levelValue,
            error = debug.toString().ifEmpty { "write failed" },
        )
    }

    fun close() {
        bindings.close()
    }

    private fun invalidateFlymeCache() {
        FlymeEnergyRegenerationApi.resetCache()
    }

    private fun verifyLevel(
        expected: Int,
        debug: StringBuilder,
        preferVhal: Boolean,
    ): Boolean {
        if (preferVhal) {
            val vhal = readVhalLevel()
            if (vhal != null) {
                debug.append('\n').append(
                    String.format(Locale.US, "verify VHAL: 0x%08X", vhal),
                )
                if (vhal == expected) return true
            }
        }

        val flyme = FlymeEnergyRegenerationApi.readLevelValue(context)
        if (flyme != null) {
            debug.append('\n').append(
                String.format(Locale.US, "verify Flyme: 0x%08X", flyme),
            )
            if (flyme == expected) return true
        }

        val ecarx = EcarxEnergyRegenerationApi.readLevelValue(context)
        if (ecarx != null) {
            debug.append('\n').append(
                String.format(Locale.US, "verify eCarX: 0x%08X", ecarx),
            )
            if (ecarx == expected) return true
        }

        if (!preferVhal) {
            val vhal = readVhalLevel()
            if (vhal != null) {
                debug.append('\n').append(
                    String.format(Locale.US, "verify VHAL: 0x%08X", vhal),
                )
                if (vhal == expected) return true
            }
        }

        return false
    }

    private fun readVhalLevel(): Int? {
        if (!bindings.ensureConnected(StringBuilder())) return null
        for (areaId in VhalConstants.ENERGY_REGENERATION_AREAS) {
            val probe = bindings.readIntProperty(
                VhalConstants.PROP_SETTING_FUNC_ENERGY_REGENERATION,
                areaId,
            )
            if (probe.ok) return probe.value
        }
        return null
    }

    private fun writeViaVhal(levelValue: Int, debug: StringBuilder): Boolean {
        if (!bindings.ensureConnected(debug)) {
            return false
        }
        for (areaId in VhalConstants.ENERGY_REGENERATION_AREAS) {
            val writeProbe = bindings.writeIntProperty(
                VhalConstants.PROP_SETTING_FUNC_ENERGY_REGENERATION,
                levelValue,
                areaId,
            )
            if (writeProbe.ok) {
                debug.append('\n').append("VHAL setIntProperty area=").append(areaId).append(": OK")
                return true
            }
            debug.append('\n').append("VHAL write area=").append(areaId).append(": ")
                .append(writeProbe.error ?: "failed")
        }
        return false
    }

    private fun readViaVhal(debug: StringBuilder): EnergyRegenerationSample {
        if (!bindings.ensureConnected(debug)) {
            return EnergyRegenerationSample(
                levelValue = 0,
                isAvailable = false,
                source = "Car init error",
                details = debug.toString(),
            )
        }

        var lastError: String? = null
        for (areaId in VhalConstants.ENERGY_REGENERATION_AREAS) {
            val probe = bindings.readIntProperty(
                VhalConstants.PROP_SETTING_FUNC_ENERGY_REGENERATION,
                areaId,
            )
            debug.append('\n').append(probe.line("SETTING_FUNC_ENERGY_REGENERATION", areaId))
            if (probe.ok) {
                return EnergyRegenerationSample(
                    levelValue = probe.value,
                    isAvailable = true,
                    source = String.format(
                        Locale.US,
                        "SETTING_FUNC_ENERGY_REGENERATION 0x%08X area=%d",
                        VhalConstants.PROP_SETTING_FUNC_ENERGY_REGENERATION,
                        areaId,
                    ),
                    details = debug.toString(),
                )
            }
            lastError = probe.error
        }

        return EnergyRegenerationSample(
            levelValue = 0,
            isAvailable = false,
            source = lastError ?: "energy regeneration unreadable",
            details = debug.toString(),
        )
    }

    private fun CarVhalBindings.IntProbe.line(name: String, areaId: Int): String {
        return if (ok) {
            String.format(
                Locale.US,
                "%s 0x%08X area=%d: int 0x%08X (%d)",
                name,
                propertyId,
                areaId,
                value,
                value,
            )
        } else {
            String.format(
                Locale.US,
                "%s 0x%08X area=%d: ERROR %s",
                name,
                propertyId,
                areaId,
                error ?: "",
            )
        }
    }

    companion object {
        private const val TAG = "GeelyToolsDriving"
    }
}
