package com.geely.ex2.tools.data.driving

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.VhalConstants

object FlymeDrivingModeApi {
    private const val TAG = "GeelyToolsDriving"

    @Volatile
    private var driveModeLiveData: Any? = null

    @Volatile
    private var flymeAvailability: Boolean? = null

    fun resetCache() {
        driveModeLiveData = null
    }

    fun isAvailable(context: Context): Boolean {
        flymeAvailability?.let { return it }
        return synchronized(this) {
            flymeAvailability?.let { return it }
            val available = probeFlymeClasses()
            flymeAvailability = available
            if (!available) {
                Log.i(TAG, "Flyme driving API unavailable on this head unit")
            } else {
                try {
                    ensureAutoFuncManager(context)
                } catch (t: Throwable) {
                    Log.w(TAG, "Flyme driving API init failed", t)
                    flymeAvailability = false
                    return@synchronized false
                }
            }
            available
        }
    }

    fun readModeValue(context: Context): Int? {
        if (!isAvailable(context)) {
            return null
        }
        return try {
            val liveData = getDriveModeLiveData(context)
            parseModeValue(readLiveDataFieldValue(liveData, "mValue"))
        } catch (t: Throwable) {
            Log.w(TAG, "Flyme driving mode read failed", t)
            null
        }
    }

    fun writeModeValue(context: Context, modeValue: Int): Boolean {
        if (!isAvailable(context)) {
            return false
        }
        return try {
            ensureAutoFuncManager(context)
            val liveData = getDriveModeLiveData(context)
            val update = liveData.javaClass.getMethod("updateFuncValueForce", Any::class.java)
            update.invoke(liveData, modeValue)
            Log.i(TAG, "Flyme updateFuncValueForce 0x${modeValue.toString(16)}")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Flyme driving mode write failed for 0x${modeValue.toString(16)}", t)
            false
        }
    }

    private fun probeFlymeClasses(): Boolean {
        return try {
            Class.forName("com.flyme.auto.api.AutoFuncManager")
            Class.forName("com.flyme.auto.api.data.EnumFuncLiveData")
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun ensureAutoFuncManager(context: Context) {
        val appContext = context.applicationContext
        val managerClass = Class.forName("com.flyme.auto.api.AutoFuncManager")
        val manager = managerClass.getMethod("getInstance", Context::class.java)
            .invoke(null, appContext)

        try {
            managerClass.getMethod("getAutoFuncInterface", Context::class.java)
                .invoke(manager, appContext)
        } catch (_: NoSuchMethodException) {
            managerClass.getMethod("getAutoFuncInterface")
                .invoke(manager)
        }
    }

    @Synchronized
    private fun getDriveModeLiveData(context: Context): Any {
        driveModeLiveData?.let { return it }

        ensureAutoFuncManager(context)

        val autoFuncIdClass = Class.forName("com.flyme.auto.api.AutoFuncId")
        val driveSelectId = autoFuncIdClass.getField("DM_FUNC_DRIVE_MODE_SELECT").get(null)

        val liveDataClass = Class.forName("com.flyme.auto.api.data.EnumFuncLiveData")
        val liveData = liveDataClass.getConstructor(autoFuncIdClass, Boolean::class.javaPrimitiveType)
            .newInstance(driveSelectId, false)
        liveDataClass.getMethod("init").invoke(liveData)

        driveModeLiveData = liveData
        return liveData
    }

    private fun readLiveDataFieldValue(liveData: Any, fieldName: String): Any? {
        val mutableLiveData = liveData.javaClass.getField(fieldName).get(liveData)
        return mutableLiveData.javaClass.getMethod("getValue").invoke(mutableLiveData)
    }

    private fun parseModeValue(value: Any?): Int? {
        return when (value) {
            null -> null
            is Int -> value
            is Number -> value.toInt()
            else -> {
                try {
                    val idField = value.javaClass.getField("id")
                    (idField.get(value) as? Number)?.toInt()
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }

    fun modeValueToAutoFuncName(modeValue: Int): String? = when (modeValue) {
        VhalConstants.DRIVE_MODE_ECO -> "VALUE_DRIVE_MODE_SELECTION_ECO"
        VhalConstants.DRIVE_MODE_COMFORT -> "VALUE_DRIVE_MODE_SELECTION_COMFORT"
        VhalConstants.DRIVE_MODE_DYNAMIC -> "VALUE_DRIVE_MODE_SELECTION_DYNAMIC"
        else -> null
    }
}
