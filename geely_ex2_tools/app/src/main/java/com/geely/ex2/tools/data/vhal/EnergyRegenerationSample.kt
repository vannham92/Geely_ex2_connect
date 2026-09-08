package com.geely.ex2.tools.data.vhal

data class EnergyRegenerationSample(
    val levelValue: Int,
    val isAvailable: Boolean,
    val source: String,
    val details: String,
)

data class EnergyRegenerationWriteResult(
    val ok: Boolean,
    val requestedValue: Int,
    val error: String?,
)
