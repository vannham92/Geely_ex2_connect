package com.geely.ex2.tools.data.vhal

import android.content.Context
import java.util.Locale

/**
 * Snapshot trạng thái điều hòa (Climate) cho phone companion.
 * [available] = true khi climate đọc được (build `system` + xe hỗ trợ); false → phone hiển thị cả cụm `--`.
 */
data class HvacSample(
    val available: Boolean = false,
    val acOn: Boolean = false,
    val tempC: Float = 0f,
    val fanSpeed: Int = 0,
    val recircOn: Boolean = false,
    val ecoOn: Boolean = false,
    val defrostOn: Boolean = false,
    val details: String = "",
)

/**
 * Đọc/ghi HVAC qua [CarVhalBindings] (CarPropertyManager trực tiếp) — cùng đường với các reader khác.
 * Property id + areaId: [VhalConstants] (nguồn docs/flyme-hvac-apk.md). Ghi cần build `system`
 * (quyền CONTROL_CAR_CLIMATE + CAR_VENDOR_EXTENSION); build `user` sẽ fail → available=false / write ok=false.
 *
 * Đồng bộ phone: on-change callback đăng ký trong CarTcpServer qua [CarVhalBindings.registerChangeCallback]
 * (type-agnostic — prop bool parse int/float trả null). Xe đổi điều hòa trên head unit → push phone tức thì.
 * Periodic refresh 30s + refresh sau command là lưới an toàn.
 */
class CarPropertyHvacController(context: Context) {
    private val bindings = CarVhalBindings(context)

    fun read(): HvacSample {
        val debug = StringBuilder()
        if (!bindings.ensureConnected(debug)) {
            return HvacSample(details = debug.toString().ifEmpty { "Car init error" })
        }

        val acProbe = bindings.readSwitchProperty(VhalConstants.PROP_HVAC_AC_ON, VhalConstants.HVAC_AREA_ALL)
        debug.append(switchLine("HVAC_AC_ON", acProbe))

        val tempProbe = bindings.readFloatProperty(VhalConstants.PROP_HVAC_TEMPERATURE_SET, VhalConstants.HVAC_AREA_DRIVER)
        debug.append('\n').append(floatLine("HVAC_TEMPERATURE_SET", tempProbe.propertyId, tempProbe.ok, tempProbe.value, tempProbe.error))

        val fanProbe = bindings.readIntProperty(VhalConstants.PROP_HVAC_FAN_SPEED, VhalConstants.HVAC_AREA_FAN)
        debug.append('\n').append(intLine("HVAC_FAN_SPEED", fanProbe.propertyId, fanProbe.ok, fanProbe.value, fanProbe.error))

        val recircProbe = bindings.readSwitchProperty(VhalConstants.PROP_HVAC_RECIRC_ON, VhalConstants.HVAC_AREA_ALL)
        debug.append('\n').append(switchLine("HVAC_RECIRC_ON", recircProbe))

        val ecoProbe = bindings.readSwitchProperty(VhalConstants.PROP_HVAC_FUNC_ECO_SWITCH, VhalConstants.HVAC_AREA_ALL)
        debug.append('\n').append(switchLine("HVAC_FUNC_ECO_SWITCH", ecoProbe))

        val defrostProbe = bindings.readSwitchProperty(VhalConstants.PROP_HVAC_MAX_DEFROST_ON, VhalConstants.HVAC_AREA_DRIVER)
        debug.append('\n').append(switchLine("HVAC_MAX_DEFROST_ON", defrostProbe))

        // available: climate coi như reachable nếu đọc được ít nhất AC hoặc nhiệt độ.
        val available = acProbe.ok || tempProbe.ok
        return HvacSample(
            available = available,
            acOn = acProbe.value,
            tempC = if (tempProbe.ok) tempProbe.value else 0f,
            fanSpeed = if (fanProbe.ok) fanProbe.value else 0,
            recircOn = recircProbe.value,
            ecoOn = ecoProbe.value,
            defrostOn = defrostProbe.value,
            details = debug.toString(),
        )
    }

    fun setAc(enabled: Boolean): CarVhalBindings.WriteProbe =
        write { bindings.writeSwitchProperty(VhalConstants.PROP_HVAC_AC_ON, enabled, VhalConstants.HVAC_AREA_ALL) }

    /** Ghi nhiệt độ ghế lái (float °C). */
    fun setTemperature(tempC: Float): CarVhalBindings.WriteProbe =
        write { bindings.writeFloatProperty(VhalConstants.PROP_HVAC_TEMPERATURE_SET, tempC, VhalConstants.HVAC_AREA_DRIVER) }

    fun setFanSpeed(level: Int): CarVhalBindings.WriteProbe =
        write { bindings.writeIntProperty(VhalConstants.PROP_HVAC_FAN_SPEED, level, VhalConstants.HVAC_AREA_FAN) }

    fun setRecirc(enabled: Boolean): CarVhalBindings.WriteProbe =
        write { bindings.writeSwitchProperty(VhalConstants.PROP_HVAC_RECIRC_ON, enabled, VhalConstants.HVAC_AREA_ALL) }

    fun setEco(enabled: Boolean): CarVhalBindings.WriteProbe =
        write { bindings.writeSwitchProperty(VhalConstants.PROP_HVAC_FUNC_ECO_SWITCH, enabled, VhalConstants.HVAC_AREA_ALL) }

    fun setDefrost(enabled: Boolean): CarVhalBindings.WriteProbe =
        write { bindings.writeSwitchProperty(VhalConstants.PROP_HVAC_MAX_DEFROST_ON, enabled, VhalConstants.HVAC_AREA_DRIVER) }

    fun close() {
        bindings.close()
    }

    private inline fun write(block: () -> CarVhalBindings.WriteProbe): CarVhalBindings.WriteProbe {
        if (!bindings.ensureConnected()) {
            return CarVhalBindings.WriteProbe.error(0, "Car init error")
        }
        return block()
    }

    private fun switchLine(name: String, probe: CarVhalBindings.SwitchProbe): String =
        if (probe.ok) String.format(Locale.US, "%s 0x%08X: %b (%s)", name, probe.propertyId, probe.value, probe.valueType)
        else String.format(Locale.US, "%s 0x%08X: ERROR %s", name, probe.propertyId, probe.error ?: "")

    private fun floatLine(name: String, id: Int, ok: Boolean, value: Float, error: String?): String =
        if (ok) String.format(Locale.US, "%s 0x%08X: %.1f", name, id, value)
        else String.format(Locale.US, "%s 0x%08X: ERROR %s", name, id, error ?: "")

    private fun intLine(name: String, id: Int, ok: Boolean, value: Int, error: String?): String =
        if (ok) String.format(Locale.US, "%s 0x%08X: %d", name, id, value)
        else String.format(Locale.US, "%s 0x%08X: ERROR %s", name, id, error ?: "")
}
