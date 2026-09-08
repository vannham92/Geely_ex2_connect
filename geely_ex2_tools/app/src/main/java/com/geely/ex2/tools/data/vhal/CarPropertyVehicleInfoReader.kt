package com.geely.ex2.tools.data.vhal

import android.content.Context
import java.util.Locale

/**
 * Snapshot telemetry read-only bổ sung cho phone companion (không ghi):
 *  - [rangeKm]    Quãng đường còn lại (km)  — RANGE_REMAINING 0x11600308
 *  - [gear]       Số hộp số "P"/"R"/"N"/"D" — GEAR_SELECTION 0x11400400 (android.car.VehicleGear)
 *  - [odometerKm] Odometer tổng (km)        — PERF_ODOMETER 0x11600204
 *
 * Mỗi field kèm cờ *Available: false = không đọc được → phone hiển thị `--`, đừng coi giá trị mặc định là thật.
 */
data class VehicleInfoSample(
    val rangeKm: Float = 0f,
    val rangeAvailable: Boolean = false,
    val gear: String = "",
    val gearAvailable: Boolean = false,
    val odometerKm: Float = 0f,
    val odometerAvailable: Boolean = false,
    val details: String = "",
)

/**
 * Đọc 3 thuộc tính telemetry ở trên qua một [CarVhalBindings] dùng chung (giữ kết nối
 * android.car.Car ấm giữa các lần đọc). Chỉ chạm trên luồng CarPropertyIo — giống các reader
 * khác được cache trong CarTcpServer.
 */
class CarPropertyVehicleInfoReader(context: Context) {
    private val bindings = CarVhalBindings(context)

    fun read(): VehicleInfoSample {
        val debug = StringBuilder()
        if (!bindings.ensureConnected(debug)) {
            return VehicleInfoSample(details = debug.toString().ifEmpty { "Car init error" })
        }

        // Range còn lại: AOSP RANGE_REMAINING trả mét; một số OEM trả thẳng km.
        // Heuristic: >= 5000 coi là mét → /1000 (range EV thực tế < ~1000 km). Log raw để verify trên xe.
        val rangeProbe = bindings.readFloatProperty(VhalConstants.PROP_RANGE_REMAINING)
        val rangeKm = if (rangeProbe.ok) normalizeRangeKm(rangeProbe.value) else 0f
        debug.append(floatLine("RANGE_REMAINING", rangeProbe.propertyId, rangeProbe.ok, rangeProbe.value, rangeProbe.error))

        // Gear selection: android.car.VehicleGear (P=4/R=2/N=1/D=8).
        val gearProbe = bindings.readIntProperty(VhalConstants.PROP_GEAR_SELECTION)
        val gear = if (gearProbe.ok) decodeGear(gearProbe.value) else ""
        debug.append('\n').append(intLine("GEAR_SELECTION", gearProbe.propertyId, gearProbe.ok, gearProbe.value, gearProbe.error))

        // Odometer tổng (km).
        val odoProbe = bindings.readFloatProperty(VhalConstants.PROP_PERF_ODOMETER)
        val odometerKm = if (odoProbe.ok) odoProbe.value else 0f
        debug.append('\n').append(floatLine("PERF_ODOMETER", odoProbe.propertyId, odoProbe.ok, odoProbe.value, odoProbe.error))

        return VehicleInfoSample(
            rangeKm = rangeKm,
            rangeAvailable = rangeProbe.ok,
            gear = gear,
            gearAvailable = gearProbe.ok,
            odometerKm = odometerKm,
            odometerAvailable = odoProbe.ok,
            details = debug.toString(),
        )
    }

    fun close() {
        bindings.close()
    }

    private fun normalizeRangeKm(raw: Float): Float =
        if (raw >= RANGE_METERS_THRESHOLD) raw / 1000f else raw

    private fun decodeGear(raw: Int): String = when {
        raw == VhalConstants.GEAR_NEUTRAL -> "N"
        raw == VhalConstants.GEAR_REVERSE -> "R"
        raw == VhalConstants.GEAR_PARK -> "P"
        raw >= VhalConstants.GEAR_DRIVE -> "D" // GEAR_DRIVE + các số tiến (GEAR_1..GEAR_8) đều hiển thị D
        else -> "" // GEAR_UNKNOWN (0) hoặc mã lạ
    }

    private fun floatLine(name: String, id: Int, ok: Boolean, value: Float, error: String?): String =
        if (ok) String.format(Locale.US, "%s 0x%08X: %.1f", name, id, value)
        else String.format(Locale.US, "%s 0x%08X: ERROR %s", name, id, error ?: "")

    private fun intLine(name: String, id: Int, ok: Boolean, value: Int, error: String?): String =
        if (ok) String.format(Locale.US, "%s 0x%08X: %d", name, id, value)
        else String.format(Locale.US, "%s 0x%08X: ERROR %s", name, id, error ?: "")

    companion object {
        private const val RANGE_METERS_THRESHOLD = 5000f
    }
}
