package com.geely.ex2.tools.data.vhal

import kotlin.math.abs

object SpeedNormalizer {
    const val MAX_SPEED_KMH = 140f

    internal const val SPEED_OFFSET_MIN_KMH = 10f

    fun driveModeSpeedKmh(rawKmh: Float): Float {
        val normalized = abs(rawKmh).coerceIn(0f, MAX_SPEED_KMH)
        val corrected = if (normalized >= SPEED_OFFSET_MIN_KMH) {
            normalized + 1f
        } else {
            normalized
        }
        return corrected.coerceIn(0f, MAX_SPEED_KMH)
    }
}
