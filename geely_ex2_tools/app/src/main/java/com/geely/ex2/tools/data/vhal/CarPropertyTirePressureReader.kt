package com.geely.ex2.tools.data.vhal

import android.content.Context
import java.util.Locale

/**
 * Snapshot áp suất lốp 4 bánh (read-only) cho phone companion.
 *  - Áp suất từng bánh (kPa) — TIRE_PRESSURE 0x17600309, area = VehicleAreaWheel (FL/FR/RL/RR).
 *  - Cảnh báo thấp từng bánh — pressure <= CRITICALLY_LOW_TIRE_PRESSURE (0x1760030A) cùng area.
 *
 * [available] = true khi đọc được ít nhất 1 bánh; false → phone hiển thị cả cụm `--`.
 * `*Low` chỉ true khi cả áp suất và ngưỡng đọc được và áp suất <= ngưỡng.
 */
data class TirePressureSample(
    val available: Boolean = false,
    val flKpa: Float = 0f,
    val frKpa: Float = 0f,
    val rlKpa: Float = 0f,
    val rrKpa: Float = 0f,
    val flLow: Boolean = false,
    val frLow: Boolean = false,
    val rlLow: Boolean = false,
    val rrLow: Boolean = false,
    val details: String = "",
)

/**
 * Đọc TIRE_PRESSURE + CRITICALLY_LOW_TIRE_PRESSURE cho 4 area bánh qua [CarVhalBindings] dùng chung.
 * Đơn vị AOSP = kPa. Không đăng ký callback (áp suất đổi chậm → dựa periodic refresh 30s).
 */
class CarPropertyTirePressureReader(context: Context) {
    private val bindings = CarVhalBindings(context)

    fun read(): TirePressureSample {
        val debug = StringBuilder()
        if (!bindings.ensureConnected(debug)) {
            return TirePressureSample(details = debug.toString().ifEmpty { "Car init error" })
        }

        val fl = readWheel("FL", VhalConstants.WHEEL_LEFT_FRONT, debug)
        val fr = readWheel("FR", VhalConstants.WHEEL_RIGHT_FRONT, debug)
        val rl = readWheel("RL", VhalConstants.WHEEL_LEFT_REAR, debug)
        val rr = readWheel("RR", VhalConstants.WHEEL_RIGHT_REAR, debug)

        return TirePressureSample(
            available = fl.ok || fr.ok || rl.ok || rr.ok,
            flKpa = fl.kpa, frKpa = fr.kpa, rlKpa = rl.kpa, rrKpa = rr.kpa,
            flLow = fl.low, frLow = fr.low, rlLow = rl.low, rrLow = rr.low,
            details = debug.toString(),
        )
    }

    fun close() {
        bindings.close()
    }

    private fun readWheel(name: String, areaId: Int, debug: StringBuilder): Wheel {
        val pressure = bindings.readFloatProperty(VhalConstants.PROP_TIRE_PRESSURE, areaId)
        val critical = bindings.readFloatProperty(VhalConstants.PROP_CRITICALLY_LOW_TIRE_PRESSURE, areaId)
        val low = pressure.ok && critical.ok && pressure.value <= critical.value
        debug.append(
            String.format(
                Locale.US,
                "%s TIRE_PRESSURE 0x%08X@%d: %s | crit: %s | low=%b\n",
                name, pressure.propertyId, areaId,
                if (pressure.ok) String.format(Locale.US, "%.0f kPa", pressure.value) else "ERR ${pressure.error}",
                if (critical.ok) String.format(Locale.US, "%.0f kPa", critical.value) else "ERR ${critical.error}",
                low,
            ),
        )
        return Wheel(pressure.ok, if (pressure.ok) pressure.value else 0f, low)
    }

    private data class Wheel(val ok: Boolean, val kpa: Float, val low: Boolean)
}
