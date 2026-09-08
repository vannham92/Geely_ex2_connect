package com.geely.ex2.tools.data.window

import android.content.Context
import com.geely.ex2.tools.data.kv.AppKv
import com.geely.ex2.tools.data.vhal.DoorCorner
import com.tencent.mmkv.MMKV

object AutoWindowSettings {
    private const val PREFS = "geelytools_auto_window"
    private const val KEY_PREFIX_CORNER = "auto_window_corner_"

    /** Mặc định TẮT cho từng cửa: tính năng tự động di chuyển kính nên phải do người dùng bật rõ ràng. */
    fun isCornerEnabled(context: Context, corner: DoorCorner): Boolean =
        kv(context).decodeBool(KEY_PREFIX_CORNER + corner.tag, false)

    fun setCornerEnabled(context: Context, corner: DoorCorner, enabled: Boolean) {
        kv(context).encode(KEY_PREFIX_CORNER + corner.tag, enabled)
    }

    /** true nếu ít nhất 1 cửa được bật — dùng để quyết định chạy/dừng service. */
    fun isAnyCornerEnabled(context: Context): Boolean =
        DoorCorner.entries.any { isCornerEnabled(context, it) }

    private fun kv(context: Context): MMKV {
        AppKv.init(context)
        return AppKv.of(PREFS)
    }
}
