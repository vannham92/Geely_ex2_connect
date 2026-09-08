package com.geely.ex2.tools.data.avas

data class AvasSample(
    val mode: Int = AvasConstants.MODE_MUTED,
    val isMuted: Boolean = false,
    val isSupported: Boolean = false,
    val isAvailable: Boolean = false,
    val source: String = "",
    val details: String = "",
)

data class AvasVolumeSample(
    val volume: Int = AvasConstants.AVAS_VOLUME_DEFAULT,
    val areaId: Int = 0,
    val isAvailable: Boolean = false,
    val source: String = "",
    val details: String = "",
)

data class AvasWriteResult(
    val ok: Boolean,
    val error: String? = null,
    val details: String = "",
)
