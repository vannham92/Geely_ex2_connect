package com.geely.ex2.tools.data.temperature

import android.content.Context
import android.util.Log
import java.lang.reflect.Method
import java.util.Locale

class TemperatureReader(context: Context) {
    private val appContext = context.applicationContext
    private var carClass: Class<*>? = null
    private var car: Any? = null
    private var propertyManager: Any? = null

    fun readTemperature(): Result {
        val debug = StringBuilder()

        try {
            ensureInitialized(debug)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize car property manager", t)
            return Result.error("Car init error", debug.append('\n').append(shortError(t)).toString())
        }

        val manager = propertyManager ?: return Result.error(
            "CarPropertyManager is null, retrying",
            debug.toString(),
        )

        val ambientRaw = readInt(manager, "AC_AMBIENT_TEMP", AC_AMBIENT_TEMP)
        val insideRaw = readInt(manager, "AC_INSIDE_TEMP", AC_INSIDE_TEMP)
        val env = readFloat(manager, "ENV_OUTSIDE_TEMPERATURE", ENV_OUTSIDE_TEMPERATURE)

        debug.append('\n').append(ambientRaw.line())
        debug.append('\n').append(insideRaw.line())
        debug.append('\n').append(env.line())

        if (ambientRaw.ok) {
            val outside = decodeFlymeTemp(ambientRaw.value)
            debug.append('\n').append(
                String.format(
                    Locale.US,
                    "DriveMode decode: (%.0f - 80) / 2 = %.1f C",
                    ambientRaw.value,
                    outside,
                ),
            )
            if (insideRaw.ok) {
                debug.append('\n').append(
                    String.format(
                        Locale.US,
                        "Inside check: (%.0f - 80) / 2 = %.1f C",
                        insideRaw.value,
                        decodeFlymeTemp(insideRaw.value),
                    ),
                )
            }
            if (env.ok) {
                debug.append('\n').append(
                    String.format(Locale.US, "ENV_OUTSIDE_TEMPERATURE ignored: %.1f C", env.value),
                )
            }
            return Result.ok(outside, "AC_AMBIENT_TEMP 0x2140A377", debug.toString())
        }

        if (env.ok) {
            return Result.ok(env.value, "ENV_OUTSIDE_TEMPERATURE fallback 0x11600703", debug.toString())
        }

        return Result.error("No temperature property readable", debug.toString())
    }

    fun close() {
        val carObject = car ?: return
        try {
            val disconnect = carObject.javaClass.getMethod("disconnect")
            disconnect.invoke(carObject)
        } catch (_: Throwable) {
        }
        car = null
        propertyManager = null
    }

    private fun ensureInitialized(debug: StringBuilder) {
        if (propertyManager != null) {
            debug.append("CarPropertyManager: OK cached")
            return
        }

        if (carClass == null) {
            carClass = Class.forName("android.car.Car")
            debug.append("android.car.Car: OK")
        } else {
            debug.append("android.car.Car: OK cached")
        }

        val classRef = carClass ?: return

        if (car == null) {
            val createCar = classRef.getMethod("createCar", Context::class.java)
            car = createCar.invoke(null, appContext)
            debug.append("\ncreateCar: ").append(if (car == null) "NULL" else "OK")
        } else {
            debug.append("\ncreateCar: OK cached")
        }

        val carObject = car ?: return

        try {
            val isConnected = classRef.getMethod("isConnected")
            val connected = (isConnected.invoke(carObject) as? Boolean) == true
            debug.append("\nisConnected: ").append(connected)
            if (!connected) {
                val connect = classRef.getMethod("connect")
                connect.invoke(carObject)
                debug.append("\nconnect: called")
            }
        } catch (e: IllegalStateException) {
            debug.append("\nconnect: already connecting/connected: ").append(e.message)
        } catch (_: NoSuchMethodException) {
            debug.append("\nconnect: no method")
        }

        val getCarManager = classRef.getMethod("getCarManager", String::class.java)
        propertyManager = getCarManager.invoke(carObject, "property")
        debug.append("\ngetCarManager(property): ").append(if (propertyManager == null) "NULL" else "OK")
    }

    private fun readFloat(manager: Any, name: String, propertyId: Int): Probe {
        return try {
            val method: Method = manager.javaClass.getMethod("getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            val value = method.invoke(manager, propertyId, GLOBAL_AREA_ID)
            if (value is Float) {
                Probe.ok(name, propertyId, value, "float")
            } else {
                Probe.error(name, propertyId, "not Float: $value")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read float property $name", t)
            Probe.error(name, propertyId, shortError(t))
        }
    }

    private fun readInt(manager: Any, name: String, propertyId: Int): Probe {
        return try {
            val method: Method = manager.javaClass.getMethod("getIntProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            val value = method.invoke(manager, propertyId, GLOBAL_AREA_ID)
            if (value is Int) {
                Probe.ok(name, propertyId, value.toFloat(), "int")
            } else {
                Probe.error(name, propertyId, "not Integer: $value")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read int property $name", t)
            Probe.error(name, propertyId, shortError(t))
        }
    }

    data class Result(
        val ok: Boolean,
        val value: Float,
        val source: String,
        val details: String,
    ) {
        companion object {
            fun ok(value: Float, source: String, details: String): Result = Result(true, value, source, details)
            fun error(source: String, details: String): Result = Result(false, 0.0f, source, details)
        }
    }

    private data class Probe(
        val name: String,
        val propertyId: Int,
        val ok: Boolean,
        val value: Float,
        val type: String,
        val error: String?,
    ) {
        fun line(): String {
            return if (ok) {
                String.format(Locale.US, "%s 0x%08X: %s %.1f", name, propertyId, type, value)
            } else {
                String.format(Locale.US, "%s 0x%08X: ERROR %s", name, propertyId, error ?: "")
            }
        }

        companion object {
            fun ok(name: String, propertyId: Int, value: Float, type: String): Probe =
                Probe(name, propertyId, true, value, type, null)

            fun error(name: String, propertyId: Int, error: String): Probe =
                Probe(name, propertyId, false, 0.0f, "", error)
        }
    }

    companion object {
        private const val TAG = "GeelyToolsTemperature"
        private const val GLOBAL_AREA_ID = 0
        private const val AC_AMBIENT_TEMP = 0x2140a377
        private const val AC_INSIDE_TEMP = 0x2140a379
        private const val ENV_OUTSIDE_TEMPERATURE = 0x11600703

        private fun decodeFlymeTemp(raw: Float): Float = (raw - 80.0f) / 2.0f

        private fun shortError(error: Throwable): String {
            var cause = error
            while (cause.cause != null) {
                cause = cause.cause!!
            }
            val name = cause::class.java.simpleName
            val message = cause.message
            return if (message.isNullOrEmpty()) name else "$name: $message"
        }
    }
}
