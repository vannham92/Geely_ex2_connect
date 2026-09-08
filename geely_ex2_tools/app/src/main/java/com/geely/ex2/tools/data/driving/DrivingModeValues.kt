package com.geely.ex2.tools.data.driving

import com.geely.ex2.tools.data.vhal.VhalConstants

object DrivingModeValues {
    /** PHEV_DRV_MODE_SET properValue → Flyme adapter value (IHU629G). */
    private val properToFlyme = mapOf(
        1 to VhalConstants.DRIVE_MODE_COMFORT,
        2 to VhalConstants.DRIVE_MODE_ECO,
        3 to VhalConstants.DRIVE_MODE_DYNAMIC,
    )

    fun normalizeReadValue(raw: Int): Int {
        if (raw in 0x22010100..0x220101FF) {
            return raw
        }
        return properToFlyme[raw] ?: raw
    }

    fun writeCandidates(flymeMode: Int): List<Int> {
        val proper = properToFlyme.entries.firstOrNull { it.value == flymeMode }?.key
        return buildList {
            add(flymeMode)
            if (proper != null) {
                add(proper)
            }
        }
    }
}
