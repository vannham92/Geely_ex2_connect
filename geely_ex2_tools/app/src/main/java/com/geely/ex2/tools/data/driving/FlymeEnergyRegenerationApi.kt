package com.geely.ex2.tools.data.driving

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.VhalConstants

object FlymeEnergyRegenerationApi {
    private const val TAG = "GeelyToolsDriving"

    @Volatile
    private var regenLiveData: Any? = null

    @Volatile
    private var flymeAvailability: Boolean? = null

    fun resetCache() {
        regenLiveData = null
    }

    fun isAvailable(context: Context): Boolean {
        flymeAvailability?.let { return it }
        return synchronized(this) {
            flymeAvailability?.let { return it }
            val available = probeFlymeClasses()
            flymeAvailability = available
            if (!available) {
                Log.i(TAG, "Flyme regen API unavailable on this head unit")
            } else {
                try {
                    ensureAutoFuncManager(context)
                } catch (t: Throwable) {
                    Log.w(TAG, "Flyme regen API init failed", t)
                    flymeAvailability = false
                    return@synchronized false
                }
            }
            available
        }
    }

    fun readLevelValue(context: Context): Int? {
        if (!isAvailable(context)) {
            return null
        }
        return try {
            val liveData = getRegenLiveData(context)
            parseLevelValue(readLiveDataFieldValue(liveData, "mValue"))
        } catch (t: Throwable) {
            Log.w(TAG, "Flyme regen read failed", t)
            null
        }
    }

    /**
     * Giống Flyme Settings: `updateValueDelayWriter(AutoFuncId)`.
     * Force — chỉ với int id (như drive mode), không dùng với object AutoFuncId.
     */
    fun writeLevelValue(context: Context, levelValue: Int): Boolean {
        if (!isAvailable(context)) {
            return false
        }
        return try {
            ensureAutoFuncManager(context)
            val liveData = getRegenLiveData(context)
            val autoFuncValue = resolveLevelAutoFuncId(levelValue)
            var wrote = false

            // Đường dẫn Settings — là chính để regen.
            if (autoFuncValue != null &&
                invokeUpdate(liveData, "updateValueDelayWriter", autoFuncValue)
            ) {
                Log.i(TAG, "Flyme regen updateValueDelayWriter AutoFuncId 0x${levelValue.toString(16)}")
                wrote = true
            }

            // Giống drive mode: Force(Integer), không phải AutoFuncId.
            if (invokeUpdate(liveData, "updateFuncValueForce", Integer.valueOf(levelValue))) {
                Log.i(TAG, "Flyme regen updateFuncValueForce int 0x${levelValue.toString(16)}")
                wrote = true
            }

            if (!wrote && invokeUpdate(liveData, "updateFuncValue", Integer.valueOf(levelValue))) {
                Log.i(TAG, "Flyme regen updateFuncValue int 0x${levelValue.toString(16)}")
                wrote = true
            }

            wrote
        } catch (t: Throwable) {
            Log.w(TAG, "Flyme regen write failed for 0x${levelValue.toString(16)}", t)
            false
        }
    }

    private fun invokeUpdate(liveData: Any, methodName: String, value: Any): Boolean {
        return try {
            liveData.javaClass.getMethod(methodName, Any::class.java).invoke(liveData, value)
            true
        } catch (_: Throwable) {
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
    private fun getRegenLiveData(context: Context): Any {
        regenLiveData?.let { return it }

        ensureAutoFuncManager(context)

        val autoFuncIdClass = Class.forName("com.flyme.auto.api.AutoFuncId")
        val regenId = autoFuncIdClass.getField("SETTING_FUNC_ENERGY_REGENERATION").get(null)
        val liveDataClass = Class.forName("com.flyme.auto.api.data.EnumFuncLiveData")

        val liveData = createRegenLiveData(liveDataClass, autoFuncIdClass, regenId)
        regenLiveData = liveData
        return liveData
    }

    private fun createRegenLiveData(
        liveDataClass: Class<*>,
        autoFuncIdClass: Class<*>,
        regenId: Any?,
    ): Any {
        try {
            val ctor = liveDataClass.getConstructor(
                autoFuncIdClass,
                Long::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            )
            val liveData = ctor.newInstance(regenId, 3000L, false)
            invokeInit(liveDataClass, liveData)
            return liveData
        } catch (_: NoSuchMethodException) {
        } catch (t: Throwable) {
            Log.w(TAG, "Flyme regen 3-arg LiveData init failed, trying 2-arg", t)
        }

        val ctor = liveDataClass.getConstructor(autoFuncIdClass, Boolean::class.javaPrimitiveType)
        val liveData = ctor.newInstance(regenId, false)
        invokeInit(liveDataClass, liveData)
        return liveData
    }

    private fun invokeInit(liveDataClass: Class<*>, liveData: Any) {
        try {
            liveDataClass.getMethod("init2").invoke(liveData)
        } catch (_: NoSuchMethodException) {
            liveDataClass.getMethod("init").invoke(liveData)
        }
    }

    private fun resolveLevelAutoFuncId(levelValue: Int): Any? {
        val fieldName = levelValueToAutoFuncName(levelValue) ?: return null
        return try {
            Class.forName("com.flyme.auto.api.AutoFuncId").getField(fieldName).get(null)
        } catch (t: Throwable) {
            Log.w(TAG, "Flyme regen AutoFuncId.$fieldName missing", t)
            null
        }
    }

    private fun readLiveDataFieldValue(liveData: Any, fieldName: String): Any? {
        val mutableLiveData = liveData.javaClass.getField(fieldName).get(liveData)
        return mutableLiveData.javaClass.getMethod("getValue").invoke(mutableLiveData)
    }

    private fun parseLevelValue(value: Any?): Int? {
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

    fun levelValueToAutoFuncName(levelValue: Int): String? = when (levelValue) {
        VhalConstants.ENERGY_REGENERATION_LEVEL_LOW -> "VALUE_ENERGY_REGENERATION_LEVEL_LOW"
        VhalConstants.ENERGY_REGENERATION_LEVEL_MID -> "VALUE_ENERGY_REGENERATION_LEVEL_MID"
        VhalConstants.ENERGY_REGENERATION_LEVEL_HIGH -> "VALUE_ENERGY_REGENERATION_LEVEL_HIGH"
        else -> null
    }
}
