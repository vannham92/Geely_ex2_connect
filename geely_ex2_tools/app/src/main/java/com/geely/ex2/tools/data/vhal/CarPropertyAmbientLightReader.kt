package com.geely.ex2.tools.data.vhal

import android.content.Context
import com.geely.ex2.tools.data.ambient.FlymeAmbientLightApi
import java.util.Locale

class CarPropertyAmbientLightReader(private val context: Context) {
    private val bindings = CarVhalBindings(context)

    fun readAmbientLight(): AmbientLightSample {
        val debug = StringBuilder()

        if (FlymeAmbientLightApi.isAvailable(context)) {
            val flymeValue = FlymeAmbientLightApi.readEnabled(context)
            if (flymeValue != null) {
                debug.append("Flyme mValue: ").append(flymeValue)
                return AmbientLightSample(
                    isEnabled = flymeValue,
                    isAvailable = true,
                    source = "Flyme BooleanFuncLiveData",
                    details = debug.toString(),
                )
            }
            debug.append("Flyme read: null mValue")
        } else {
            debug.append("Flyme read: skipped")
        }

        if (!bindings.ensureConnected(debug)) {
            return AmbientLightSample(
                isEnabled = false,
                isAvailable = false,
                source = "Car init error",
                details = debug.toString(),
            )
        }

        val probe = bindings.readSwitchProperty(VhalConstants.PROP_LIGHTINSIDE_ATMOSPHERE_LAMP_SWITCH)
        debug.append('\n').append(probe.line("LIGHTINSIDE_ATMOSPHERE_LAMP_SWITCH"))

        if (probe.ok) {
            return AmbientLightSample(
                isEnabled = probe.value,
                isAvailable = true,
                source = "LIGHTINSIDE_ATMOSPHERE_LAMP_SWITCH 0x2140a655 (${probe.valueType})",
                details = debug.toString(),
            )
        }

        return AmbientLightSample(
            isEnabled = false,
            isAvailable = false,
            source = probe.error ?: "ambient light unreadable",
            details = debug.toString(),
        )
    }

    fun writeAmbientLight(enabled: Boolean): AmbientLightWriteResult {
        val debug = StringBuilder()

        if (FlymeAmbientLightApi.writeEnabled(context, enabled)) {
            debug.append("Flyme updateFuncValue: OK")
            val readBack = FlymeAmbientLightApi.readEnabled(context)
            if (readBack != null) {
                debug.append('\n').append("Flyme read-back: ").append(readBack)
            }
            return AmbientLightWriteResult(
                ok = true,
                requestedEnabled = enabled,
                error = null,
            )
        }
        debug.append("Flyme write: failed")

        if (!bindings.ensureConnected(debug)) {
            return AmbientLightWriteResult(
                ok = false,
                requestedEnabled = enabled,
                error = debug.toString().ifEmpty { "Car init error" },
            )
        }

        val writeProbe = bindings.writeSwitchProperty(
            VhalConstants.PROP_LIGHTINSIDE_ATMOSPHERE_LAMP_SWITCH,
            enabled,
        )
        if (!writeProbe.ok) {
            return AmbientLightWriteResult(
                ok = false,
                requestedEnabled = enabled,
                error = writeProbe.error ?: "write failed",
            )
        }

        debug.append('\n').append("VHAL writeSwitchProperty: OK")
        return AmbientLightWriteResult(
            ok = true,
            requestedEnabled = enabled,
            error = null,
        )
    }

    fun close() {
        bindings.close()
    }

    private fun CarVhalBindings.SwitchProbe.line(name: String): String {
        return if (ok) {
            String.format(
                Locale.US,
                "%s 0x%08X: %s = %s",
                name,
                propertyId,
                valueType,
                value,
            )
        } else {
            String.format(Locale.US, "%s 0x%08X: ERROR %s", name, propertyId, error ?: "")
        }
    }
}
