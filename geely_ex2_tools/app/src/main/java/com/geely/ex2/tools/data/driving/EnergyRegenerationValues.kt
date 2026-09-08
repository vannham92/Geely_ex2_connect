package com.geely.ex2.tools.data.driving

import com.geely.ex2.tools.data.vhal.VhalConstants

object EnergyRegenerationValues {
    /** Trước đây đã sử dụng nhầm 0x2202050x thay vì AutoFuncId 0x2002050x. */
    fun normalizeStoredValue(raw: Int): Int = when (raw) {
        0x22020501 -> VhalConstants.ENERGY_REGENERATION_LEVEL_LOW
        0x22020502 -> VhalConstants.ENERGY_REGENERATION_LEVEL_MID
        0x22020503 -> VhalConstants.ENERGY_REGENERATION_LEVEL_HIGH
        0x22020500 -> VhalConstants.PROP_SETTING_FUNC_ENERGY_REGENERATION
        else -> raw
    }
}
