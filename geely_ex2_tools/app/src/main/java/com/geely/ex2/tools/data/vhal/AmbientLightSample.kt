package com.geely.ex2.tools.data.vhal

data class AmbientLightSample(
    val isEnabled: Boolean,
    val isAvailable: Boolean,
    val source: String,
    val details: String,
)

data class AmbientLightWriteResult(
    val ok: Boolean,
    val requestedEnabled: Boolean,
    val error: String?,
)
