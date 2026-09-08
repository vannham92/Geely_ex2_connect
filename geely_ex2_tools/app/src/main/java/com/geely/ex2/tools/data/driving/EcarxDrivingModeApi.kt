package com.geely.ex2.tools.data.driving

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.VhalConstants
import java.lang.reflect.Method

object EcarxDrivingModeApi {
    private const val TAG = "GeelyToolsDriving"
    private const val FUNC_DM_FUNC_DRIVE_MODE_SELECT = VhalConstants.PROP_DM_FUNC_DRIVE_MODE_SELECT
    private const val ZONE_GLOBAL = VhalConstants.GLOBAL_AREA_ID

    @Volatile
    private var ecarxAvailability: Boolean? = null

    @Volatile
    private var carFunction: Any? = null

    fun isAvailable(context: Context): Boolean {
        ecarxAvailability?.let { return it }
        return synchronized(this) {
            ecarxAvailability?.let { return it }
            val available = probeEcarxClasses()
            ecarxAvailability = available
            if (!available) {
                Log.i(TAG, "eCarX driving API unavailable on this head unit")
            }
            available
        }
    }

    fun readModeValue(context: Context): Int? {
        if (!isAvailable(context)) {
            return null
        }
        return try {
            val function = ensureCarFunction(context)
            readViaFunctionValue(function) ?: readViaCustomize(function)
        } catch (t: Throwable) {
            Log.w(TAG, "eCarX driving mode read failed", t)
            null
        }
    }

    fun writeModeValue(context: Context, modeValue: Int): Boolean {
        if (!isAvailable(context)) {
            return false
        }
        return try {
            val function = ensureCarFunction(context)
            for (candidate in DrivingModeValues.writeCandidates(modeValue)) {
                val methodName = writeViaFunctionValue(function, candidate)
                if (methodName != null) {
                    Log.i(TAG, "eCarX $methodName OK: 0x${candidate.toString(16)}")
                    return true
                }
                if (writeViaCustomize(function, candidate)) {
                    Log.i(TAG, "eCarX setCustomizeFunctionValue OK: 0x${candidate.toString(16)}")
                    return true
                }
            }
            Log.d(TAG, "eCarX write: no supported method for 0x${modeValue.toString(16)}")
            false
        } catch (t: Throwable) {
            Log.w(TAG, "eCarX driving mode write failed for 0x${modeValue.toString(16)}", t)
            false
        }
    }

    private fun probeEcarxClasses(): Boolean {
        val carClassNames = listOf(
            "com.ecarx.xui.adaptapi.car.Car",
            "com.ecarx.xui.adaptapi.binder.Car",
        )
        val functionClassNames = listOf(
            "com.ecarx.xui.adaptapi.car.base.ICarFunction",
            "com.ecarx.xui.adaptapi.car.ICarFunction",
            "com.ecarx.xui.adaptapi.car.base.ICarFunctionImpl",
        )
        return carClassNames.any { name ->
            try {
                Class.forName(name)
                true
            } catch (_: Throwable) {
                false
            }
        } && functionClassNames.any { name ->
            try {
                Class.forName(name)
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    private fun createEcarxCar(context: Context): Any {
        val appContext = context.applicationContext
        val classNames = listOf(
            "com.ecarx.xui.adaptapi.car.Car",
            "com.ecarx.xui.adaptapi.binder.Car",
        )
        for (className in classNames) {
            try {
                val carClass = Class.forName(className)
                return carClass.getMethod("create", Context::class.java).invoke(null, appContext)
                    ?: continue
            } catch (_: Throwable) {
            }
        }
        throw ClassNotFoundException("eCarX Car class not found")
    }

    private fun ensureCarFunction(context: Context): Any {
        carFunction?.let { return it }

        val appContext = context.applicationContext
        val car = createEcarxCar(appContext)

        waitForCarConnection(car)

        val function = car.javaClass.getMethod("getICarFunction").invoke(car)
            ?: throw IllegalStateException("eCarX getICarFunction returned null")

        carFunction = function
        return function
    }

    private fun waitForCarConnection(car: Any) {
        try {
            val isConnected = car.javaClass.getMethod("isConnected")
            repeat(10) {
                if ((isConnected.invoke(car) as? Boolean) == true) {
                    return
                }
                Thread.sleep(100)
            }
            val connect = car.javaClass.getMethod("connect")
            connect.invoke(car)
            repeat(20) {
                if ((isConnected.invoke(car) as? Boolean) == true) {
                    return
                }
                Thread.sleep(100)
            }
        } catch (_: Throwable) {
        }
    }

    private fun readViaCustomize(function: Any): Int? {
        return invokeIntMethod(
            function,
            "getCustomizeFunctionValue",
            FUNC_DM_FUNC_DRIVE_MODE_SELECT,
            ZONE_GLOBAL,
        )?.let(DrivingModeValues::normalizeReadValue)
    }

    private fun readViaFunctionValue(function: Any): Int? {
        val raw = invokeNumberMethod(
            function,
            "getFunctionValue",
            FUNC_DM_FUNC_DRIVE_MODE_SELECT,
            ZONE_GLOBAL,
        ) ?: return null
        return DrivingModeValues.normalizeReadValue(raw)
    }

    /** @return method name if write succeeded */
    private fun writeViaFunctionValue(function: Any, value: Int): String? {
        if (invokeWriteResult(
                function,
                "setFunctionValue",
                FUNC_DM_FUNC_DRIVE_MODE_SELECT,
                ZONE_GLOBAL,
                value,
            )
        ) {
            return "setFunctionValue(int)"
        }
        if (invokeWriteResult(
                function,
                "setFunctionValue",
                FUNC_DM_FUNC_DRIVE_MODE_SELECT,
                ZONE_GLOBAL,
                value.toFloat(),
            )
        ) {
            return "setFunctionValue(float)"
        }
        return null
    }

    private fun writeViaCustomize(function: Any, value: Int): Boolean {
        return invokeWriteResult(
            function,
            "setCustomizeFunctionValue",
            FUNC_DM_FUNC_DRIVE_MODE_SELECT,
            ZONE_GLOBAL,
            value,
        )
    }

    private fun invokeIntMethod(function: Any, name: String, vararg args: Any): Int? {
        return invokeNumberMethod(function, name, *args)
    }

    private fun invokeNumberMethod(function: Any, name: String, vararg args: Any): Int? {
        return try {
            val method = findMethod(function.javaClass, name, args) ?: return null
            when (val result = method.invoke(function, *args)) {
                is Int -> result
                is Float -> result.toInt()
                is Double -> result.toInt()
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun invokeWriteResult(function: Any, name: String, vararg args: Any): Boolean {
        return try {
            val method = findMethod(function.javaClass, name, args) ?: return false
            when (val result = method.invoke(function, *args)) {
                is Boolean -> result
                is Int -> result == 1
                else -> true
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun findMethod(type: Class<*>, name: String, args: Array<out Any>): Method? {
        return type.methods.firstOrNull { method ->
            if (method.name != name || method.parameterCount != args.size) {
                return@firstOrNull false
            }
            method.parameterTypes.withIndex().all { (index, parameterType) ->
                matchesParameterType(parameterType, args[index])
            }
        }
    }

    private fun matchesParameterType(parameterType: Class<*>, arg: Any): Boolean {
        return when (arg) {
            is Int -> parameterType == Int::class.javaPrimitiveType ||
                parameterType == Int::class.javaObjectType ||
                parameterType == Float::class.javaPrimitiveType ||
                parameterType == Float::class.javaObjectType ||
                parameterType == Double::class.javaPrimitiveType

            is Float -> parameterType == Float::class.javaPrimitiveType ||
                parameterType == Float::class.javaObjectType ||
                parameterType == Double::class.javaPrimitiveType

            else -> parameterType.isInstance(arg)
        }
    }
}
