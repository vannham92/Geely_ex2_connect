package com.geely.ex2.tools.data.vhal

import android.content.Context
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Snapshot khóa cửa + vị trí kính cho phone companion.
 * [doorLockAvailable]/[windowAvailable] gate từng cụm; false → phone hiển thị `--`.
 * Kính báo theo phần trăm mở: 0=đóng kín, 100=mở hết (quy đổi từ WINDOW_POS raw qua config max/min).
 */
data class DoorWindowSample(
    val doorLockAvailable: Boolean = false,
    val doorsLocked: Boolean = false,
    val windowAvailable: Boolean = false,
    val windowFlPercent: Int = 0,
    val windowFrPercent: Int = 0,
    val windowRlPercent: Int = 0,
    val windowRrPercent: Int = 0,
    val details: String = "",
)

/**
 * Đọc/ghi DOOR_LOCK (bool, VehicleAreaDoor) và WINDOW_POS (int, VehicleAreaWindow) qua [CarVhalBindings].
 * Ghi cần build `system` (quyền CONTROL_CAR_DOORS / CONTROL_CAR_WINDOWS); build `user` → available=false / ok=false.
 *
 * **An toàn**: caller (CarTcpServer) chặn ghi khi xe đang chạy (gate theo gear/tốc độ). Controller này
 * chỉ thực thi VHAL; không tự kiểm tra trạng thái lái.
 */
class CarPropertyDoorWindowController(context: Context) {
    private val bindings = CarVhalBindings(context)

    private val doorAreas = intArrayOf(
        VhalConstants.DOOR_ROW_1_LEFT,
        VhalConstants.DOOR_ROW_1_RIGHT,
        VhalConstants.DOOR_ROW_2_LEFT,
        VhalConstants.DOOR_ROW_2_RIGHT,
    )

    /** Id DOOR_POS đọc được trên xe này (AOSP hoặc vendor) — dò một lần rồi cache. */
    @Volatile
    private var doorPosPropId: Int? = null

    fun read(): DoorWindowSample {
        val debug = StringBuilder()
        if (!bindings.ensureConnected(debug)) {
            return DoorWindowSample(details = debug.toString().ifEmpty { "Car init error" })
        }

        // --- Door lock: coi là "đã khóa" khi mọi cửa đọc được đều báo locked ---
        var anyDoorOk = false
        var allLocked = true
        for (area in doorAreas) {
            val probe = bindings.readSwitchProperty(VhalConstants.PROP_DOOR_LOCK, area)
            if (probe.ok) {
                anyDoorOk = true
                if (!probe.value) allLocked = false
                debug.append(String.format(Locale.US, "DOOR_LOCK 0x%08X: %b\n", area, probe.value))
            } else {
                debug.append(String.format(Locale.US, "DOOR_LOCK 0x%08X: ERROR %s\n", area, probe.error ?: ""))
            }
        }
        val doorLockAvailable = anyDoorOk
        val doorsLocked = anyDoorOk && allLocked

        // --- Window position (%) từng kính ---
        val fl = readWindowPercent(VhalConstants.WINDOW_ROW_1_LEFT, "FL", debug)
        val fr = readWindowPercent(VhalConstants.WINDOW_ROW_1_RIGHT, "FR", debug)
        val rl = readWindowPercent(VhalConstants.WINDOW_ROW_2_LEFT, "RL", debug)
        val rr = readWindowPercent(VhalConstants.WINDOW_ROW_2_RIGHT, "RR", debug)
        val windowAvailable = fl != null || fr != null || rl != null || rr != null

        return DoorWindowSample(
            doorLockAvailable = doorLockAvailable,
            doorsLocked = doorsLocked,
            windowAvailable = windowAvailable,
            windowFlPercent = fl ?: 0,
            windowFrPercent = fr ?: 0,
            windowRlPercent = rl ?: 0,
            windowRrPercent = rr ?: 0,
            details = debug.toString(),
        )
    }

    /** Khóa/mở khóa toàn bộ 4 cửa. ok khi mọi cửa ghi thành công. */
    fun setDoorsLocked(locked: Boolean): CarVhalBindings.WriteProbe {
        if (!bindings.ensureConnected()) {
            return CarVhalBindings.WriteProbe.error(VhalConstants.PROP_DOOR_LOCK, "Car init error")
        }
        var firstError: String? = null
        for (area in doorAreas) {
            val probe = bindings.writeSwitchProperty(VhalConstants.PROP_DOOR_LOCK, locked, area)
            if (!probe.ok && firstError == null) {
                firstError = "door 0x${area.toString(16)}: ${probe.error}"
            }
        }
        return if (firstError == null) {
            CarVhalBindings.WriteProbe.ok(VhalConstants.PROP_DOOR_LOCK)
        } else {
            CarVhalBindings.WriteProbe.error(VhalConstants.PROP_DOOR_LOCK, firstError)
        }
    }

    /**
     * Đặt vị trí kính theo phần trăm (0=đóng, 100=mở hết).
     * [selector]: 0=tất cả, 1=FL, 2=FR, 3=RL, 4=RR.
     */
    fun setWindow(selector: Int, percent: Int): CarVhalBindings.WriteProbe {
        if (!bindings.ensureConnected()) {
            return CarVhalBindings.WriteProbe.error(VhalConstants.PROP_WINDOW_POS, "Car init error")
        }
        val areas = when (selector) {
            SELECTOR_ALL -> intArrayOf(
                VhalConstants.WINDOW_ROW_1_LEFT, VhalConstants.WINDOW_ROW_1_RIGHT,
                VhalConstants.WINDOW_ROW_2_LEFT, VhalConstants.WINDOW_ROW_2_RIGHT,
            )
            SELECTOR_FL -> intArrayOf(VhalConstants.WINDOW_ROW_1_LEFT)
            SELECTOR_FR -> intArrayOf(VhalConstants.WINDOW_ROW_1_RIGHT)
            SELECTOR_RL -> intArrayOf(VhalConstants.WINDOW_ROW_2_LEFT)
            SELECTOR_RR -> intArrayOf(VhalConstants.WINDOW_ROW_2_RIGHT)
            else -> return CarVhalBindings.WriteProbe.error(
                VhalConstants.PROP_WINDOW_POS,
                "window selector không hợp lệ: $selector (0=all,1=FL,2=FR,3=RL,4=RR)",
            )
        }
        val pct = percent.coerceIn(0, 100)
        var firstError: String? = null
        var anyOk = false
        for (area in areas) {
            val raw = windowRawFromPercent(area, pct)
            if (raw == null) {
                if (firstError == null) firstError = "window 0x${area.toString(16)}: thiếu config max"
                continue
            }
            val probe = bindings.writeIntProperty(VhalConstants.PROP_WINDOW_POS, raw, area)
            if (probe.ok) anyOk = true
            else if (firstError == null) firstError = "window 0x${area.toString(16)}: ${probe.error}"
        }
        return if (anyOk && firstError == null) {
            CarVhalBindings.WriteProbe.ok(VhalConstants.PROP_WINDOW_POS)
        } else {
            CarVhalBindings.WriteProbe.error(
                VhalConstants.PROP_WINDOW_POS,
                firstError ?: "no window written",
            )
        }
    }

    /**
     * Id DOOR_POS dùng được trên xe này, dò theo thứ tự AOSP → vendor bằng một lần đọc thử
     * cửa trước trái. null khi cả hai id đều không đọc được (xe không hỗ trợ / thiếu quyền).
     */
    fun resolveDoorPosPropertyId(): Int? {
        doorPosPropId?.let { return it }
        if (!bindings.ensureConnected()) return null
        for (propId in intArrayOf(VhalConstants.PROP_DOOR_POS, VhalConstants.PROP_VENDOR_DOOR_POS)) {
            val probe = bindings.readSwitchProperty(propId, VhalConstants.DOOR_ROW_1_LEFT)
            if (probe.ok) {
                doorPosPropId = propId
                return propId
            }
        }
        return null
    }

    /** true=cửa đang mở (DOOR_POS != 0); null khi không đọc được. */
    fun readDoorOpen(corner: DoorCorner): Boolean? {
        val propId = resolveDoorPosPropertyId() ?: return null
        val probe = bindings.readSwitchProperty(propId, corner.doorAreaId)
        return if (probe.ok) probe.value else null
    }

    /** % mở kính của cửa [corner] (0=đóng kín, 100=mở hết); null khi không đọc được. */
    fun readWindowPercent(corner: DoorCorner): Int? {
        if (!bindings.ensureConnected()) return null
        return readWindowPercent(corner.windowAreaId, corner.tag, StringBuilder())
    }

    /** Đặt % mở kính cho đúng cửa [corner]. */
    fun setWindowPercent(corner: DoorCorner, percent: Int): CarVhalBindings.WriteProbe {
        if (!bindings.ensureConnected()) {
            return CarVhalBindings.WriteProbe.error(VhalConstants.PROP_WINDOW_POS, "Car init error")
        }
        val raw = windowRawFromPercent(corner.windowAreaId, percent.coerceIn(0, 100))
            ?: return CarVhalBindings.WriteProbe.error(
                VhalConstants.PROP_WINDOW_POS,
                "window ${corner.tag}: thiếu config max",
            )
        return bindings.writeIntProperty(VhalConstants.PROP_WINDOW_POS, raw, corner.windowAreaId)
    }

    fun close() {
        bindings.close()
    }

    /** Đọc % mở của 1 kính; null nếu không đọc được hoặc thiếu config max. */
    private fun readWindowPercent(area: Int, tag: String, debug: StringBuilder): Int? {
        val probe = bindings.readIntProperty(VhalConstants.PROP_WINDOW_POS, area)
        if (!probe.ok) {
            debug.append(String.format(Locale.US, "WINDOW_POS[%s] 0x%08X: ERROR %s\n", tag, area, probe.error ?: ""))
            return null
        }
        val min = bindings.readAreaConfigMinInt(VhalConstants.PROP_WINDOW_POS, area) ?: 0
        val max = bindings.readAreaConfigMaxInt(VhalConstants.PROP_WINDOW_POS, area)
        if (max == null || max <= min) {
            debug.append(String.format(Locale.US, "WINDOW_POS[%s] 0x%08X: raw=%d (thiếu max config)\n", tag, area, probe.value))
            return null
        }
        val percent = ((probe.value - min) * 100f / (max - min)).roundToInt().coerceIn(0, 100)
        debug.append(String.format(Locale.US, "WINDOW_POS[%s] 0x%08X: raw=%d min=%d max=%d -> %d%%\n", tag, area, probe.value, min, max, percent))
        return percent
    }

    /** Quy đổi % → WINDOW_POS raw theo config; null nếu thiếu max. */
    private fun windowRawFromPercent(area: Int, percent: Int): Int? {
        val min = bindings.readAreaConfigMinInt(VhalConstants.PROP_WINDOW_POS, area) ?: 0
        val max = bindings.readAreaConfigMaxInt(VhalConstants.PROP_WINDOW_POS, area) ?: return null
        if (max <= min) return null
        return min + (percent / 100f * (max - min)).roundToInt()
    }

    companion object {
        const val SELECTOR_ALL = 0
        const val SELECTOR_FL = 1
        const val SELECTOR_FR = 2
        const val SELECTOR_RL = 3
        const val SELECTOR_RR = 4
    }
}
