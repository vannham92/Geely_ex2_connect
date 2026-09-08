package com.geely.ex2.tools.data.window

import android.content.Context
import com.geely.ex2.tools.data.vhal.CarPropertyDoorWindowController
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.DoorCorner

/** Trạng thái cửa + kính của một góc xe cho UI; null = không đọc được. */
data class DoorWindowCornerState(
    val corner: DoorCorner,
    val isDoorOpen: Boolean?,
    val windowPercent: Int?,
)

class AutoWindowRepository(context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var doorWindow: CarPropertyDoorWindowController? = null

    fun isCornerEnabled(corner: DoorCorner): Boolean =
        AutoWindowSettings.isCornerEnabled(appContext, corner)

    /** Lưu lựa chọn rồi bật/tắt service theo dõi cửa (chạy khi ≥ 1 cửa bật). */
    fun setCornerEnabled(corner: DoorCorner, enabled: Boolean, reason: String) {
        AutoWindowSettings.setCornerEnabled(appContext, corner, enabled)
        if (AutoWindowSettings.isAnyCornerEnabled(appContext)) {
            AutoWindowAppStarter.startService(appContext, reason)
        } else {
            AutoWindowAppStarter.stopService(appContext, reason)
        }
    }

    fun readCorners(): List<DoorWindowCornerState> = CarPropertyIo.call {
        val controller = doorWindow ?: CarPropertyDoorWindowController(appContext).also { doorWindow = it }
        DoorCorner.entries.map { corner ->
            DoorWindowCornerState(
                corner = corner,
                isDoorOpen = controller.readDoorOpen(corner),
                windowPercent = controller.readWindowPercent(corner),
            )
        }
    }

    fun close() {
        CarPropertyIo.call {
            doorWindow?.close()
            doorWindow = null
        }
    }
}
