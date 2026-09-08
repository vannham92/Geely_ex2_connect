package com.geely.ex2.tools.data.vhal

import android.content.Context
import android.util.Log
import java.lang.reflect.Method

class CarVhalBindings(context: Context) {
    private val appContext = context.applicationContext
    private var carClass: Class<*>? = null
    private var car: Any? = null
    private var propertyManager: Any? = null

    val isReady: Boolean
        get() = propertyManager != null

    fun ensureConnected(debug: StringBuilder? = null): Boolean {
        try {
            ensureInitialized(debug)
            return propertyManager != null
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize car property manager", t)
            debug?.append('\n')?.append(shortError(t))
            return false
        }
    }

    fun getPropertyManager(): Any? = propertyManager

    fun readFloatProperty(propertyId: Int, areaId: Int = VhalConstants.GLOBAL_AREA_ID): FloatProbe {
        val manager = propertyManager ?: return FloatProbe.error(propertyId, "CarPropertyManager is null")
        return readFloat(manager, propertyId, areaId)
    }

    fun readIntProperty(propertyId: Int, areaId: Int = VhalConstants.GLOBAL_AREA_ID): IntProbe {
        val manager = propertyManager ?: return IntProbe.error(propertyId, "CarPropertyManager is null")
        return readInt(manager, propertyId, areaId)
    }

    fun readBooleanProperty(propertyId: Int, areaId: Int = VhalConstants.GLOBAL_AREA_ID): BooleanProbe {
        val manager = propertyManager ?: return BooleanProbe.error(propertyId, "CarPropertyManager is null")
        return readBoolean(manager, propertyId, areaId)
    }

    fun readSwitchProperty(propertyId: Int, areaId: Int = VhalConstants.GLOBAL_AREA_ID): SwitchProbe {
        val manager = propertyManager ?: return SwitchProbe.error(propertyId, "CarPropertyManager is null")

        readBoolean(manager, propertyId, areaId).takeIf { it.ok }?.let {
            return SwitchProbe.ok(propertyId, it.value, "boolean")
        }

        readInt(manager, propertyId, areaId).takeIf { it.ok }?.let {
            return SwitchProbe.ok(propertyId, it.value != VhalConstants.COMMON_VALUE_OFF, "int")
        }

        return readPropertySwitch(manager, propertyId, areaId)
    }

    fun writeIntProperty(
        propertyId: Int,
        value: Int,
        areaId: Int = VhalConstants.GLOBAL_AREA_ID,
    ): WriteProbe {
        val manager = propertyManager ?: return WriteProbe.error(propertyId, "CarPropertyManager is null")
        return writeInt(manager, propertyId, areaId, value)
    }

    fun writeBooleanProperty(
        propertyId: Int,
        value: Boolean,
        areaId: Int = VhalConstants.GLOBAL_AREA_ID,
    ): WriteProbe {
        val manager = propertyManager ?: return WriteProbe.error(propertyId, "CarPropertyManager is null")
        return writeBoolean(manager, propertyId, areaId, value)
    }

    fun writeFloatProperty(
        propertyId: Int,
        value: Float,
        areaId: Int = VhalConstants.GLOBAL_AREA_ID,
    ): WriteProbe {
        val manager = propertyManager ?: return WriteProbe.error(propertyId, "CarPropertyManager is null")
        return writeFloat(manager, propertyId, areaId, value)
    }

    fun writeSwitchProperty(
        propertyId: Int,
        enabled: Boolean,
        areaId: Int = VhalConstants.GLOBAL_AREA_ID,
    ): WriteProbe {
        val manager = propertyManager ?: return WriteProbe.error(propertyId, "CarPropertyManager is null")

        writeBoolean(manager, propertyId, areaId, enabled).takeIf { it.ok }?.let { return it }

        val intValue = if (enabled) {
            VhalConstants.COMMON_VALUE_ON
        } else {
            VhalConstants.COMMON_VALUE_OFF
        }
        return writeInt(manager, propertyId, areaId, intValue)
    }

    /**
     * Đọc maxValue (Int) của [areaId] từ CarPropertyConfig. Dùng để quy đổi WINDOW_POS raw ↔ phần trăm.
     * null nếu prop/area không có config hoặc maxValue không phải Int.
     */
    fun readAreaConfigMaxInt(propertyId: Int, areaId: Int): Int? =
        readAreaConfigBound(propertyId, areaId, "getMaxValue")

    /** Đọc minValue (Int) của [areaId]; null nếu không có. */
    fun readAreaConfigMinInt(propertyId: Int, areaId: Int): Int? =
        readAreaConfigBound(propertyId, areaId, "getMinValue")

    private fun readAreaConfigBound(propertyId: Int, areaId: Int, methodName: String): Int? {
        val manager = propertyManager ?: return null
        return try {
            val getConfig = manager.javaClass.getMethod("getCarPropertyConfig", Int::class.javaPrimitiveType)
            val config = getConfig.invoke(manager, propertyId) ?: return null
            val getBound = config.javaClass.getMethod(methodName, Int::class.javaPrimitiveType)
            (getBound.invoke(config, areaId) as? Number)?.toInt()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read config $methodName 0x${propertyId.toString(16)} area 0x${areaId.toString(16)}", t)
            null
        }
    }

    fun registerPropertyCallback(
        propertyId: Int,
        updateRateHz: Float,
        onValue: (Float) -> Unit,
        onError: (String) -> Unit,
    ): Any? {
        val manager = propertyManager ?: return null

        return try {
            val callbackClass = Class.forName(
                "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
            )
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, method, args ->
                when (method.name) {
                    "onChangeEvent" -> {
                        val value = args?.getOrNull(0) ?: return@newProxyInstance null
                        parseFloatValue(value)?.let(onValue)
                    }
                    "onErrorEvent" -> {
                        val propId = args?.getOrNull(0) as? Int ?: propertyId
                        val zone = args?.getOrNull(1) as? Int ?: 0
                        onError("property 0x${propId.toString(16)} zone $zone")
                    }
                }
                null
            }

            val register = manager.javaClass.getMethod(
                "registerCallback",
                callbackClass,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            )
            val accepted = register.invoke(manager, callback, propertyId, updateRateHz) as? Boolean
            if (accepted == false) {
                Log.w(TAG, "registerCallback rejected for 0x${propertyId.toString(16)}")
                return null
            }
            callback
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register VHAL callback for 0x${propertyId.toString(16)}", t)
            null
        }
    }

    fun registerIntPropertyCallback(
        propertyId: Int,
        updateRateHz: Float,
        onValue: (Int) -> Unit,
        onError: (String) -> Unit,
    ): Any? {
        val manager = propertyManager ?: return null

        return try {
            val callbackClass = Class.forName(
                "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
            )
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, method, args ->
                when (method.name) {
                    "onChangeEvent" -> {
                        val value = args?.getOrNull(0) ?: return@newProxyInstance null
                        parseIntValue(value)?.let(onValue)
                    }
                    "onErrorEvent" -> {
                        val propId = args?.getOrNull(0) as? Int ?: propertyId
                        val zone = args?.getOrNull(1) as? Int ?: 0
                        onError("property 0x${propId.toString(16)} zone $zone")
                    }
                }
                null
            }

            val register = manager.javaClass.getMethod(
                "registerCallback",
                callbackClass,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            )
            val accepted = register.invoke(manager, callback, propertyId, updateRateHz) as? Boolean
            if (accepted == false) {
                Log.w(TAG, "registerCallback rejected int for 0x${propertyId.toString(16)}")
                return null
            }
            callback
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register int VHAL callback for 0x${propertyId.toString(16)}", t)
            null
        }
    }

    /**
     * Callback on-change kèm **areaId** — cần cho prop theo vùng, nơi một propertyId bắn sự kiện
     * cho nhiều vùng (DOOR_POS: mỗi cửa một areaId). Giá trị Boolean được quy đổi 1/0 vì adapt
     * layer có thể trả bool cho prop khai báo int 0..1.
     */
    fun registerAreaIntPropertyCallback(
        propertyId: Int,
        updateRateHz: Float,
        onValue: (Int, Int) -> Unit,
        onError: (String) -> Unit,
    ): Any? {
        val manager = propertyManager ?: return null

        return try {
            val callbackClass = Class.forName(
                "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
            )
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, method, args ->
                when (method.name) {
                    "onChangeEvent" -> {
                        val value = args?.getOrNull(0) ?: return@newProxyInstance null
                        val areaId = parseAreaId(value)
                        val intValue = parseIntOrBoolValue(value)
                        if (areaId != null && intValue != null) {
                            onValue(areaId, intValue)
                        }
                    }
                    "onErrorEvent" -> {
                        val propId = args?.getOrNull(0) as? Int ?: propertyId
                        val zone = args?.getOrNull(1) as? Int ?: 0
                        onError("property 0x${propId.toString(16)} zone $zone")
                    }
                }
                null
            }

            val register = manager.javaClass.getMethod(
                "registerCallback",
                callbackClass,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            )
            val accepted = register.invoke(manager, callback, propertyId, updateRateHz) as? Boolean
            if (accepted == false) {
                Log.w(TAG, "registerCallback rejected area int for 0x${propertyId.toString(16)}")
                return null
            }
            callback
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register area int VHAL callback for 0x${propertyId.toString(16)}", t)
            null
        }
    }

    /**
     * Callback không quan tâm kiểu giá trị: bắn [onChange] mỗi khi có onChangeEvent (bất kể
     * bool/int/float). Dùng cho prop chỉ cần biết "đã đổi" rồi tự đọc lại full — vd HVAC bool,
     * nơi [registerIntPropertyCallback]/[registerPropertyCallback] parse trả null với Boolean.
     */
    fun registerChangeCallback(
        propertyId: Int,
        updateRateHz: Float,
        onChange: () -> Unit,
        onError: (String) -> Unit,
    ): Any? {
        val manager = propertyManager ?: return null

        return try {
            val callbackClass = Class.forName(
                "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
            )
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, method, args ->
                when (method.name) {
                    "onChangeEvent" -> onChange()
                    "onErrorEvent" -> {
                        val propId = args?.getOrNull(0) as? Int ?: propertyId
                        val zone = args?.getOrNull(1) as? Int ?: 0
                        onError("property 0x${propId.toString(16)} zone $zone")
                    }
                }
                null
            }

            val register = manager.javaClass.getMethod(
                "registerCallback",
                callbackClass,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            )
            val accepted = register.invoke(manager, callback, propertyId, updateRateHz) as? Boolean
            if (accepted == false) {
                Log.w(TAG, "registerCallback rejected change for 0x${propertyId.toString(16)}")
                return null
            }
            callback
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register change VHAL callback for 0x${propertyId.toString(16)}", t)
            null
        }
    }

    fun unregisterPropertyCallback(callback: Any?) {
        val manager = propertyManager ?: return
        val callbackObject = callback ?: return

        try {
            val callbackClass = Class.forName(
                "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
            )
            val unregister = manager.javaClass.getMethod("unregisterCallback", callbackClass)
            unregister.invoke(manager, callbackObject)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to unregister VHAL callback", t)
        }
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

    private fun ensureInitialized(debug: StringBuilder?) {
        if (propertyManager != null) {
            debug?.append("CarPropertyManager: OK cached")
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
                val connect = classRef.getMethod("connect")
                connect.invoke(carObject)
                debug?.append("\nconnect: called")
            }
        } catch (e: IllegalStateException) {
            debug?.let { it.append("\nconnect: already connecting/connected: ").append(e.message) }
        } catch (_: NoSuchMethodException) {
            debug?.append("\nconnect: no method")
        }

        try {
            val getCarManager = classRef.getMethod("getCarManager", String::class.java)
            propertyManager = getCarManager.invoke(carObject, VhalConstants.CAR_MANAGER_PROPERTY)
            debug?.let {
                it.append("\ngetCarManager(property): ")
                    .append(if (propertyManager == null) "NULL" else "OK")
            }
        } catch (e: Exception) {
            debug?.let {
                it.append("\ngetCarManager(property) EXCEPTION: ").append(e.cause?.message ?: e.message)
            }
        } finally {
            // QUAN TRỌNG: Nếu lấy propertyManager thất bại (car service chưa sẵn sàng),
            // KHÔNG CACHE car object này. Phải vứt đi để lần sau gọi lại createCar().
            // Nếu không, carObject sẽ bị kẹt ở trạng thái failed vĩnh viễn đối với reader này.
            if (propertyManager == null) {
                car = null
            }
        }
    }

    private fun readBoolean(manager: Any, propertyId: Int, areaId: Int): BooleanProbe {
        return try {
            val method: Method = manager.javaClass.getMethod(
                "getBooleanProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            val value = method.invoke(manager, propertyId, areaId)
            if (value is Boolean) {
                BooleanProbe.ok(propertyId, value)
            } else {
                BooleanProbe.error(propertyId, "not Boolean: $value")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read boolean property 0x${propertyId.toString(16)}", t)
            BooleanProbe.error(propertyId, shortError(t))
        }
    }

    private fun readPropertySwitch(manager: Any, propertyId: Int, areaId: Int): SwitchProbe {
        return try {
            val method: Method = manager.javaClass.getMethod(
                "getProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            val carPropertyValue = method.invoke(manager, propertyId, areaId) ?: return SwitchProbe.error(
                propertyId,
                "getProperty returned null",
            )
            when (val rawValue = carPropertyValue.javaClass.getMethod("getValue").invoke(carPropertyValue)) {
                is Boolean -> SwitchProbe.ok(propertyId, rawValue, "getProperty boolean")
                is Int -> SwitchProbe.ok(
                    propertyId,
                    rawValue != VhalConstants.COMMON_VALUE_OFF,
                    "getProperty int",
                )
                is Number -> SwitchProbe.ok(
                    propertyId,
                    rawValue.toInt() != VhalConstants.COMMON_VALUE_OFF,
                    "getProperty number",
                )
                else -> SwitchProbe.error(propertyId, "unsupported value: $rawValue")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read switch property 0x${propertyId.toString(16)}", t)
            SwitchProbe.error(propertyId, shortError(t))
        }
    }

    private fun writeBoolean(manager: Any, propertyId: Int, areaId: Int, value: Boolean): WriteProbe {
        return try {
            val method: Method = manager.javaClass.getMethod(
                "setBooleanProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            )
            method.invoke(manager, propertyId, areaId, value)
            WriteProbe.ok(propertyId)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to write boolean property 0x${propertyId.toString(16)}", t)
            WriteProbe.error(propertyId, shortError(t))
        }
    }

    private fun readInt(manager: Any, propertyId: Int, areaId: Int): IntProbe {
        return try {
            val method: Method = manager.javaClass.getMethod(
                "getIntProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            val value = method.invoke(manager, propertyId, areaId)
            if (value is Int) {
                IntProbe.ok(propertyId, value)
            } else {
                IntProbe.error(propertyId, "not Int: $value")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read int property 0x${propertyId.toString(16)}", t)
            IntProbe.error(propertyId, shortError(t))
        }
    }

    private fun writeInt(manager: Any, propertyId: Int, areaId: Int, value: Int): WriteProbe {
        return try {
            val method: Method = manager.javaClass.getMethod(
                "setIntProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            method.invoke(manager, propertyId, areaId, value)
            WriteProbe.ok(propertyId)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to write int property 0x${propertyId.toString(16)}", t)
            WriteProbe.error(propertyId, shortError(t))
        }
    }

    private fun writeFloat(manager: Any, propertyId: Int, areaId: Int, value: Float): WriteProbe {
        return try {
            val method: Method = manager.javaClass.getMethod(
                "setFloatProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            )
            method.invoke(manager, propertyId, areaId, value)
            WriteProbe.ok(propertyId)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to write float property 0x${propertyId.toString(16)}", t)
            WriteProbe.error(propertyId, shortError(t))
        }
    }

    private fun readFloat(manager: Any, propertyId: Int, areaId: Int): FloatProbe {
        return try {
            val method: Method = manager.javaClass.getMethod(
                "getFloatProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            val value = method.invoke(manager, propertyId, areaId)
            if (value is Float) {
                FloatProbe.ok(propertyId, value)
            } else {
                FloatProbe.error(propertyId, "not Float: $value")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read float property 0x${propertyId.toString(16)}", t)
            FloatProbe.error(propertyId, shortError(t))
        }
    }

    private fun parseIntValue(carPropertyValue: Any): Int? {
        return try {
            val getValue = carPropertyValue.javaClass.getMethod("getValue")
            when (val value = getValue.invoke(carPropertyValue)) {
                is Int -> value
                is Byte -> value.toInt()
                is Short -> value.toInt()
                else -> null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to parse int CarPropertyValue", t)
            null
        }
    }

    private fun parseAreaId(carPropertyValue: Any): Int? {
        return try {
            carPropertyValue.javaClass.getMethod("getAreaId").invoke(carPropertyValue) as? Int
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to parse CarPropertyValue areaId", t)
            null
        }
    }

    private fun parseIntOrBoolValue(carPropertyValue: Any): Int? {
        return try {
            val getValue = carPropertyValue.javaClass.getMethod("getValue")
            when (val value = getValue.invoke(carPropertyValue)) {
                is Boolean -> if (value) VhalConstants.COMMON_VALUE_ON else VhalConstants.COMMON_VALUE_OFF
                is Number -> value.toInt()
                else -> null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to parse int/bool CarPropertyValue", t)
            null
        }
    }

    private fun parseFloatValue(carPropertyValue: Any): Float? {
        return try {
            val getValue = carPropertyValue.javaClass.getMethod("getValue")
            when (val value = getValue.invoke(carPropertyValue)) {
                is Float -> value
                is Double -> value.toFloat()
                is Int -> value.toFloat()
                else -> null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to parse CarPropertyValue", t)
            null
        }
    }

    data class FloatProbe(
        val propertyId: Int,
        val ok: Boolean,
        val value: Float,
        val error: String?,
    ) {
        companion object {
            fun ok(propertyId: Int, value: Float): FloatProbe =
                FloatProbe(propertyId, true, value, null)

            fun error(propertyId: Int, error: String): FloatProbe =
                FloatProbe(propertyId, false, 0f, error)
        }
    }

    data class IntProbe(
        val propertyId: Int,
        val ok: Boolean,
        val value: Int,
        val error: String?,
    ) {
        companion object {
            fun ok(propertyId: Int, value: Int): IntProbe =
                IntProbe(propertyId, true, value, null)

            fun error(propertyId: Int, error: String): IntProbe =
                IntProbe(propertyId, false, 0, error)
        }
    }

    data class BooleanProbe(
        val propertyId: Int,
        val ok: Boolean,
        val value: Boolean,
        val error: String?,
    ) {
        companion object {
            fun ok(propertyId: Int, value: Boolean): BooleanProbe =
                BooleanProbe(propertyId, true, value, null)

            fun error(propertyId: Int, error: String): BooleanProbe =
                BooleanProbe(propertyId, false, false, error)
        }
    }

    data class SwitchProbe(
        val propertyId: Int,
        val ok: Boolean,
        val value: Boolean,
        val valueType: String,
        val error: String?,
    ) {
        companion object {
            fun ok(propertyId: Int, value: Boolean, valueType: String): SwitchProbe =
                SwitchProbe(propertyId, true, value, valueType, null)

            fun error(propertyId: Int, error: String): SwitchProbe =
                SwitchProbe(propertyId, false, false, "", error)
        }
    }

    data class WriteProbe(
        val propertyId: Int,
        val ok: Boolean,
        val error: String?,
    ) {
        companion object {
            fun ok(propertyId: Int): WriteProbe =
                WriteProbe(propertyId, true, null)

            fun error(propertyId: Int, error: String): WriteProbe =
                WriteProbe(propertyId, false, error)
        }
    }

    companion object {
        private const val TAG = "GeelyToolsVhal"

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
