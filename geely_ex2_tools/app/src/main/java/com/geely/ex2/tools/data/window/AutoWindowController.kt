package com.geely.ex2.tools.data.window

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyDoorWindowController
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.CarVhalBindings
import com.geely.ex2.tools.data.vhal.DoorCorner
import com.geely.ex2.tools.data.vhal.VhalConstants
import kotlin.math.abs

/**
 * "Cửa mở → hé kính cửa đó, cửa đóng → đóng lại kính nếu đang hé".
 *
 * - Cửa chuyển đóng → mở: nếu kính đang thấp hơn [VhalConstants.AUTO_WINDOW_DROP_PERCENT]% thì
 *   hạ tới đúng mức đó; kính đang mở nhiều hơn thì giữ nguyên (không bao giờ đóng bớt lại).
 * - Cửa chuyển mở → đóng: chỉ đóng kính khi đang hé trong khoảng
 *   1..[VhalConstants.AUTO_WINDOW_CLOSE_MAX_PERCENT]%; mở nhiều hơn coi là người dùng chủ động
 *   hạ kính nên không can thiệp.
 * - Chỉ nghe DOOR_POS, không nghe WINDOW_POS → không có vòng lặp phản hồi với chính lệnh ghi.
 *
 * Ghi WINDOW_POS cần quyền `CONTROL_CAR_WINDOWS` → chỉ có tác dụng trên build `system`.
 *
 * **Threading**: mọi hàm public phải chạy trên luồng [CarPropertyIo] (binder VHAL + map trạng
 * thái không thread-safe). Callback on-change đến từ luồng binder nên caller phải chuyển tiếp.
 */
class AutoWindowController(context: Context) {
    private val appContext = context.applicationContext
    private val doorWindow = CarPropertyDoorWindowController(appContext)

    // Bindings riêng cho on-change callback, tách khỏi bindings đọc/ghi của doorWindow.
    private val eventBindings = CarVhalBindings(appContext)

    @Volatile
    private var doorCallback: Any? = null

    private val lastDoorOpen = mutableMapOf<DoorCorner, Boolean>()

    /** Đọc trạng thái cửa hiện tại làm mốc — KHÔNG thao tác kính. */
    fun syncBaseline(reason: String) {
        for (corner in DoorCorner.entries) {
            val open = doorWindow.readDoorOpen(corner)
            if (open == null) {
                Log.w(TAG, "Baseline ${corner.tag}: không đọc được DOOR_POS ($reason)")
                continue
            }
            lastDoorOpen[corner] = open
            Log.i(TAG, "Baseline ${corner.tag}: ${if (open) "mở" else "đóng"} ($reason)")
        }
    }

    /** Đăng ký on-change DOOR_POS. false → caller phải tự poll bằng [pollDoors]. */
    fun startWatching(onDoorEvent: (DoorCorner, Boolean) -> Unit): Boolean {
        if (doorCallback != null) return true

        val propId = doorWindow.resolveDoorPosPropertyId()
        if (propId == null) {
            Log.w(TAG, "DOOR_POS không đọc được (AOSP 0x16400B00 / vendor 0x264020A9) — cần build system?")
            return false
        }
        if (!eventBindings.ensureConnected()) {
            Log.w(TAG, "Không kết nối được CarPropertyManager cho callback DOOR_POS")
            return false
        }

        val callback = eventBindings.registerAreaIntPropertyCallback(
            propertyId = propId,
            updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
            onValue = { areaId, value ->
                val corner = DoorCorner.fromDoorAreaId(areaId)
                if (corner == null) {
                    // Cốp (0x20000000) / ca-pô (0x10000000) không có kính → bỏ qua.
                    Log.d(TAG, "DOOR_POS area 0x${areaId.toString(16)} không phải cửa hành khách")
                } else {
                    onDoorEvent(corner, value != VhalConstants.COMMON_VALUE_OFF)
                }
            },
            onError = { error -> Log.w(TAG, "DOOR_POS callback error: $error") },
        ) ?: return false

        doorCallback = callback
        Log.i(TAG, "Theo dõi DOOR_POS 0x${propId.toString(16)} qua callback on-change")
        return true
    }

    fun stopWatching() {
        val callback = doorCallback ?: return
        doorCallback = null
        eventBindings.unregisterPropertyCallback(callback)
        Log.i(TAG, "Dừng theo dõi DOOR_POS")
    }

    /** Fallback khi callback bị từ chối: đọc lại 4 cửa và xử lý như sự kiện. */
    fun pollDoors(reason: String) {
        for (corner in DoorCorner.entries) {
            val open = doorWindow.readDoorOpen(corner) ?: continue
            onDoorState(corner, open, reason)
        }
    }

    /** Xử lý một trạng thái cửa; chỉ thao tác kính khi trạng thái thực sự đổi so với lần trước. */
    fun onDoorState(corner: DoorCorner, isOpen: Boolean, reason: String) {
        if (!AutoWindowSettings.isCornerEnabled(appContext, corner)) {
            Log.d(TAG, "Bỏ qua ${corner.tag}: cửa này đang tắt ($reason)")
            return
        }

        val previous = lastDoorOpen.put(corner, isOpen)
        if (previous == null) {
            Log.i(TAG, "Baseline muộn ${corner.tag}: ${if (isOpen) "mở" else "đóng"} ($reason)")
            return
        }
        if (previous == isOpen) return

        if (isDriving()) {
            Log.w(TAG, "Bỏ qua ${corner.tag}: xe đang chạy ($reason)")
            return
        }

        if (isOpen) {
            dropWindowOnDoorOpen(corner, reason)
        } else {
            closeWindowOnDoorClose(corner, reason)
        }
    }

    fun close() {
        stopWatching()
        eventBindings.close()
        doorWindow.close()
        lastDoorOpen.clear()
    }

    private fun dropWindowOnDoorOpen(corner: DoorCorner, reason: String) {
        val percent = doorWindow.readWindowPercent(corner)
        if (percent == null) {
            Log.w(TAG, "Cửa ${corner.tag} mở nhưng không đọc được WINDOW_POS ($reason)")
            return
        }
        val target = VhalConstants.AUTO_WINDOW_DROP_PERCENT
        if (percent >= target) {
            Log.i(TAG, "Cửa ${corner.tag} mở: kính đã ở $percent% (>= $target%) → giữ nguyên")
            return
        }
        writeWindow(corner, target, "cửa mở, $reason")
    }

    private fun closeWindowOnDoorClose(corner: DoorCorner, reason: String) {
        val percent = doorWindow.readWindowPercent(corner)
        if (percent == null) {
            Log.w(TAG, "Cửa ${corner.tag} đóng nhưng không đọc được WINDOW_POS ($reason)")
            return
        }
        if (percent <= 0) return
        val maxPercent = VhalConstants.AUTO_WINDOW_CLOSE_MAX_PERCENT
        if (percent > maxPercent) {
            Log.i(TAG, "Cửa ${corner.tag} đóng: kính mở $percent% (> $maxPercent%) → giữ nguyên")
            return
        }
        writeWindow(corner, 0, "cửa đóng, $reason")
    }

    private fun writeWindow(corner: DoorCorner, percent: Int, reason: String) {
        val probe = doorWindow.setWindowPercent(corner, percent)
        if (probe.ok) {
            Log.i(TAG, "Kính ${corner.tag} → $percent% ($reason)")
        } else {
            Log.w(TAG, "Ghi kính ${corner.tag} → $percent% lỗi: ${probe.error} ($reason)")
        }
    }

    /**
     * Chặn thao tác kính khi xe đang chạy. Không đọc được tốc độ thì cho phép — cửa mở lúc xe
     * chạy là tình huống bất thường và mức hé kính rất nhỏ.
     */
    private fun isDriving(): Boolean {
        if (!eventBindings.ensureConnected()) return false
        val probe = eventBindings.readFloatProperty(VhalConstants.PROP_PERF_VEHICLE_SPEED)
        if (!probe.ok) {
            Log.d(TAG, "Không đọc được PERF_VEHICLE_SPEED: ${probe.error}")
            return false
        }
        return abs(probe.value) > VhalConstants.AUTO_WINDOW_MAX_SPEED_KMH
    }

    companion object {
        const val TAG = "GeelyToolsAutoWindow"
    }
}
