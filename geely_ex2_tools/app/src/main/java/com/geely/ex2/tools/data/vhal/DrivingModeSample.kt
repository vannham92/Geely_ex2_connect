package com.geely.ex2.tools.data.vhal

data class DrivingModeSample(
    val modeValue: Int,
    val isAvailable: Boolean,
    val source: String,
    val details: String,
)

data class DrivingModeWriteResult(
    val ok: Boolean,
    val requestedValue: Int,
    val error: String?,
)
