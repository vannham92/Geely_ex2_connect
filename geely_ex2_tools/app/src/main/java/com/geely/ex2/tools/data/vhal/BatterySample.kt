package com.geely.ex2.tools.data.vhal

data class BatterySample(
    val socPercent: Float,
    val isAvailable: Boolean,
    val source: String = "",
    val details: String = "",
)

interface VhalBatteryReader {
    fun readBatterySoc(): BatterySample

    fun close()
}
