package com.geely.ex2.tools.data.vhal

sealed class VhalVehicleEvent {
    data class GearChanged(
        val gearSelection: Int,
    ) : VhalVehicleEvent()
}

fun interface VhalVehicleEventListener {
    fun onVehicleEvent(event: VhalVehicleEvent)
}
