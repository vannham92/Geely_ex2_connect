package com.geely.ex2.tools.data.avas

import android.content.Context
import android.util.Log

/**
 * Reflective access to OEM CarAudioManager AVAS APIs
 * (getAVASMode / setAVASMode / isAVASModeSupported) — as in CentralEXAuto.
 */
class CarAudioBindings(context: Context) {
    private val appContext = context.applicationContext
    private var carClass: Class<*>? = null
    private var car: Any? = null
    private var audioManager: Any? = null

    val isReady: Boolean
        get() = audioManager != null

    fun ensureConnected(debug: StringBuilder? = null): Boolean {
        return try {
            ensureInitialized(debug)
            audioManager != null
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize CarAudioManager", t)
            debug?.append('\n')?.append(shortError(t))
            false
        }
    }

    fun isAvasModeSupported(): Boolean {
        val manager = audioManager ?: return false
        return try {
            val method = manager.javaClass.methods.firstOrNull { candidate ->
                candidate.name == "isAVASModeSupported" && candidate.parameterCount == 0
            } ?: return false
            method.invoke(manager) as? Boolean == true
        } catch (t: Throwable) {
            Log.w(TAG, "isAVASModeSupported failed", t)
            false
        }
    }

    fun getAvasMode(): IntProbe {
        val manager = audioManager ?: return IntProbe.error("CarAudioManager is null")
        return try {
            val method = manager.javaClass.methods.firstOrNull { candidate ->
                candidate.name == "getAVASMode" && candidate.parameterCount == 0
            } ?: return IntProbe.error("getAVASMode not found")
            when (val value = method.invoke(manager)) {
                is Int -> IntProbe.ok(value)
                is Number -> IntProbe.ok(value.toInt())
                else -> IntProbe.error("not Int: $value")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "getAVASMode failed", t)
            IntProbe.error(shortError(t))
        }
    }

    fun setAvasMode(mode: Int): WriteProbe {
        val manager = audioManager ?: return WriteProbe.error("CarAudioManager is null")
        return try {
            val method = manager.javaClass.methods.firstOrNull { candidate ->
                candidate.name == "setAVASMode" &&
                    candidate.parameterCount == 1 &&
                    (
                        candidate.parameterTypes[0] == Int::class.javaPrimitiveType ||
                            candidate.parameterTypes[0] == Int::class.javaObjectType
                        )
            } ?: return WriteProbe.error("setAVASMode not found")
            method.invoke(manager, mode)
            WriteProbe.ok()
        } catch (t: Throwable) {
            val cause = t.cause ?: t
            Log.w(TAG, "setAVASMode($mode) failed", cause)
            WriteProbe.error(shortError(cause))
        }
    }

    fun close() {
        val carObject = car ?: return
        try {
            carObject.javaClass.getMethod("disconnect").invoke(carObject)
        } catch (_: Throwable) {
        }
        car = null
        audioManager = null
    }

    private fun ensureInitialized(debug: StringBuilder?) {
        if (audioManager != null) {
            debug?.append("CarAudioManager: OK cached")
            return
        }

        if (carClass == null) {
            carClass = Class.forName("android.car.Car")
            debug?.append("android.car.Car: OK")
        } else {
            debug?.append("android.car.Car: OK cached")
        }

        val classRef = carClass ?: return

        if (car == null) {
            val createCar = classRef.getMethod("createCar", Context::class.java)
            car = createCar.invoke(null, appContext)
            debug?.let { it.append("\ncreateCar: ").append(if (car == null) "NULL" else "OK") }
        } else {
            debug?.append("\ncreateCar: OK cached")
        }

        val carObject = car ?: return

        try {
            val isConnected = classRef.getMethod("isConnected")
            val connected = (isConnected.invoke(carObject) as? Boolean) == true
            debug?.let { it.append("\nisConnected: ").append(connected) }
            if (!connected) {
                classRef.getMethod("connect").invoke(carObject)
                debug?.append("\nconnect: called")
                for (i in 0 until 20) {
                    if ((isConnected.invoke(carObject) as? Boolean) == true) break
                    Thread.sleep(100)
                }
                debug?.let {
                    it.append("\nisConnected after wait: ")
                        .append((isConnected.invoke(carObject) as? Boolean) == true)
                }
            }
        } catch (e: IllegalStateException) {
            debug?.let { it.append("\nconnect: already connecting/connected: ").append(e.message) }
        } catch (_: NoSuchMethodException) {
            debug?.append("\nconnect: no method")
        }

        val getCarManager = classRef.getMethod("getCarManager", String::class.java)
        audioManager = getCarManager.invoke(carObject, AvasConstants.CAR_MANAGER_AUDIO)
        debug?.let {
            it.append("\ngetCarManager(audio): ")
                .append(if (audioManager == null) "NULL" else "OK")
        }
    }

    private fun shortError(t: Throwable): String {
        val message = t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
        return message.take(160)
    }

    data class IntProbe(
        val ok: Boolean,
        val value: Int,
        val error: String?,
    ) {
        companion object {
            fun ok(value: Int): IntProbe = IntProbe(true, value, null)
            fun error(error: String): IntProbe = IntProbe(false, -1, error)
        }
    }

    data class WriteProbe(
        val ok: Boolean,
        val error: String?,
    ) {
        companion object {
            fun ok(): WriteProbe = WriteProbe(true, null)
            fun error(error: String): WriteProbe = WriteProbe(false, error)
        }
    }

    companion object {
        private const val TAG = "GeelyToolsAvas"
    }
}
