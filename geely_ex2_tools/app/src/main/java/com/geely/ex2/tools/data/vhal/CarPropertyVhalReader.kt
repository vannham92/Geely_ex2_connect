package com.geely.ex2.tools.data.vhal

import android.content.Context
import android.util.Log
import java.util.Locale

class CarPropertyVhalReader(context: Context) : VhalSpeedReader {
    private val bindings = CarVhalBindings(context)
    private var propertyCallback: Any? = null

    override fun readSpeed(): SpeedSample {
        val debug = StringBuilder()
        if (!bindings.ensureConnected(debug)) {
            return SpeedSample(
                speedKmh = 0f,
                isAvailable = false,
                source = "Car init error",
                details = debug.toString(),
            )
        }

        val probe = bindings.readFloatProperty(VhalConstants.PROP_PERF_VEHICLE_SPEED)
        debug.append('\n').append(probe.line())

        if (probe.ok) {
            return toAvailableSample(probe.value, debug)
        }

        return SpeedSample(
            speedKmh = 0f,
            isAvailable = false,
            source = probe.error ?: "PERF_VEHICLE_SPEED unreadable",
            details = debug.toString(),
        )
    }

    override fun subscribeSpeed(
        updateRateHz: Float,
        onSample: (SpeedSample) -> Unit,
        onError: (String) -> Unit,
    ): Boolean {
        if (propertyCallback != null) {
            return true
        }

        val debug = StringBuilder()
        if (!bindings.ensureConnected(debug)) {
            Log.w(TAG, "Subscribe skipped: ${debug.toString().ifBlank { "Car init error" }}")
            return false
        }

        // onError is only for live onErrorEvent; register failures return null → poll fallback.
        val callback = bindings.registerPropertyCallback(
            propertyId = VhalConstants.PROP_PERF_VEHICLE_SPEED,
            updateRateHz = updateRateHz,
            onValue = { raw ->
                onSample(toAvailableSample(raw, StringBuilder()))
            },
            onError = onError,
        )
        if (callback == null) {
            return false
        }

        propertyCallback = callback
        Log.i(TAG, "Subscribed to PERF_VEHICLE_SPEED at ${updateRateHz} Hz")
        return true
    }

    override fun unsubscribeSpeed() {
        val callback = propertyCallback ?: return
        propertyCallback = null
        bindings.unregisterPropertyCallback(callback)
        Log.i(TAG, "Unsubscribed from PERF_VEHICLE_SPEED")
    }

    override fun close() {
        unsubscribeSpeed()
        bindings.close()
    }

    private fun toAvailableSample(raw: Float, debug: StringBuilder): SpeedSample {
        val normalized = SpeedNormalizer.driveModeSpeedKmh(raw)
        debug.append('\n').append(
            String.format(
                Locale.US,
                "driveModeSpeedKmh: raw=%.1f -> %.0f km/h (+1 if >= %.0f)",
                raw,
                normalized,
                SpeedNormalizer.SPEED_OFFSET_MIN_KMH,
            ),
        )
        return SpeedSample(
            speedKmh = normalized,
            isAvailable = true,
            source = "PERF_VEHICLE_SPEED 0x11600207",
            details = debug.toString(),
        )
    }

    private fun CarVhalBindings.FloatProbe.line(): String {
        return if (ok) {
            String.format(Locale.US, "PERF_VEHICLE_SPEED 0x%08X: float %.1f", propertyId, value)
        } else {
            String.format(Locale.US, "PERF_VEHICLE_SPEED 0x%08X: ERROR %s", propertyId, error ?: "")
        }
    }

    companion object {
        private const val TAG = "GeelyToolsSpeed"
    }
}
