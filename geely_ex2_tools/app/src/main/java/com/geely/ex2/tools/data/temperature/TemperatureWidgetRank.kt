package com.geely.ex2.tools.data.temperature

object TemperatureWidgetRank {
    const val MIN = 1
    const val MAX = 30
    const val DEFAULT = 1
    const val STEP = 1

    fun clamp(rank: Int): Int = rank.coerceIn(MIN, MAX)

    fun canStepLeft(rank: Int): Boolean = rank > MIN

    fun canStepRight(rank: Int): Boolean = rank < MAX
}
