package com.geely.ex2.tools.data.temperature

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicReference

object TemperatureStatusFont {
    private val cachedTypeface = AtomicReference<Typeface?>(null)

    private val SYSTEM_FONT_PATHS = arrayOf(
        "/system/fonts/fontenw5.ttf",
        "/system/fonts/fontw5.ttf",
        "/system/fonts/fontenw4.ttf",
        "/system/fonts/fontw4.ttf",
        "/product/fonts/fontenw5.ttf",
        "/product/fonts/fontw5.ttf",
        "/product/fonts/fontenw4.ttf",
        "/product/fonts/fontw4.ttf",
    )

    fun getStatusIconTypeface(context: Context): Typeface {
        cachedTypeface.get()?.let { return it }

        loadFromSystemPaths()?.let { typeface ->
            cachedTypeface.set(typeface)
            return typeface
        }

        val resolved = resolveSystemAliasTypeface()
        cachedTypeface.set(resolved)
        return resolved
    }

    /** Khởi động trước (warm up) font chữ trên IO khi ứng dụng bắt đầu — tránh disk I/O ở lần thông báo (notify) đầu tiên. */
    fun warmUp(context: Context) {
        getStatusIconTypeface(context.applicationContext)
    }

    fun invalidateCache() {
        cachedTypeface.set(null)
    }

    private fun loadFromSystemPaths(): Typeface? {
        for (path in SYSTEM_FONT_PATHS) {
            val file = File(path)
            if (!file.isFile || !file.canRead()) {
                continue
            }

            try {
                return Typeface.createFromFile(file)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to load Flyme system font: $path", e)
            }
        }
        return null
    }

    private fun resolveSystemAliasTypeface(): Typeface {
        for (alias in SYSTEM_FONT_ALIASES) {
            val typeface = Typeface.create(alias, Typeface.NORMAL)
            if (typeface != null) {
                return typeface
            }
        }

        return Typeface.DEFAULT
    }

    private val SYSTEM_FONT_ALIASES = arrayOf(
        "sans-serif-medium",
        "sans-serif",
    )

    private const val TAG = "GeelyToolsTemperature"
}
