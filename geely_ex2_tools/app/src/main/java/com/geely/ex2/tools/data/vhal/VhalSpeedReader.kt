package com.geely.ex2.tools.data.vhal

data class SpeedSample(
    val speedKmh: Float,
    val isAvailable: Boolean,
    val source: String = "",
    val details: String = "",
)

interface VhalSpeedReader {
    fun readSpeed(): SpeedSample

    /** @return true if VHAL callback was registered */
    fun subscribeSpeed(
        updateRateHz: Float,
        onSample: (SpeedSample) -> Unit,
        onError: (String) -> Unit,
    ): Boolean

    fun unsubscribeSpeed()

    fun close()
}
