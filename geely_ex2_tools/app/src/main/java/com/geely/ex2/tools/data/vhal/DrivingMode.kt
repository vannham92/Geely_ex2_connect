package com.geely.ex2.tools.data.vhal

import com.geely.ex2.tools.R

object DrivingMode {
    data class Option(
        val vhalValue: Int,
        val labelRes: Int,
    )

    /** Các chế độ EX2: Eco, Comfort, Sport. */
    val selectable: List<Option> = listOf(
        Option(VhalConstants.DRIVE_MODE_ECO, R.string.driving_mode_eco),
        Option(VhalConstants.DRIVE_MODE_COMFORT, R.string.driving_mode_comfort),
        Option(VhalConstants.DRIVE_MODE_DYNAMIC, R.string.driving_mode_dynamic),
    )

    fun isSelectableValue(vhalValue: Int): Boolean =
        selectable.any { it.vhalValue == vhalValue }

    fun labelResFor(vhalValue: Int): Int? = when (vhalValue) {
        VhalConstants.DRIVE_MODE_ECO -> R.string.driving_mode_eco
        VhalConstants.DRIVE_MODE_COMFORT -> R.string.driving_mode_comfort
        VhalConstants.DRIVE_MODE_DYNAMIC -> R.string.driving_mode_dynamic
        VhalConstants.DRIVE_MODE_XC -> R.string.driving_mode_xc
        VhalConstants.DRIVE_MODE_PURE -> R.string.driving_mode_pure
        VhalConstants.DRIVE_MODE_HYBRID -> R.string.driving_mode_hybrid
        VhalConstants.DRIVE_MODE_SNOW -> R.string.driving_mode_snow
        VhalConstants.DRIVE_MODE_ADAPTIVE -> R.string.driving_mode_adaptive
        else -> null
    }

    fun selectableIndexFor(vhalValue: Int): Int =
        selectable.indexOfFirst { it.vhalValue == vhalValue }
}
