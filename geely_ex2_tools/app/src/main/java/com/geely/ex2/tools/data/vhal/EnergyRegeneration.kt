package com.geely.ex2.tools.data.vhal

import com.geely.ex2.tools.R

object EnergyRegeneration {
    data class Option(
        val vhalValue: Int,
        val labelRes: Int,
    )

    /** Các mức EX2: Thấp, Trung bình, Cao. */
    val selectable: List<Option> = listOf(
        Option(VhalConstants.ENERGY_REGENERATION_LEVEL_LOW, R.string.driving_regen_low),
        Option(VhalConstants.ENERGY_REGENERATION_LEVEL_MID, R.string.driving_regen_mid),
        Option(VhalConstants.ENERGY_REGENERATION_LEVEL_HIGH, R.string.driving_regen_high),
    )

    fun isSelectableValue(vhalValue: Int): Boolean =
        selectable.any { it.vhalValue == vhalValue }

    fun labelResFor(vhalValue: Int): Int? =
        selectable.firstOrNull { it.vhalValue == vhalValue }?.labelRes

    fun selectableIndexFor(vhalValue: Int): Int =
        selectable.indexOfFirst { it.vhalValue == vhalValue }
}
