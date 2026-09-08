package com.geely.ex2.tools.data.ambient

enum class AmbientLightControlMode {
    AUTO,
    OFF,
    ON,
    ;

    companion object {
        val selectable = listOf(AUTO, OFF, ON)

        fun fromIndex(index: Int): AmbientLightControlMode? = selectable.getOrNull(index)

        fun indexOf(mode: AmbientLightControlMode): Int = selectable.indexOf(mode)
    }
}
