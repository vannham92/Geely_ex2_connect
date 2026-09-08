package com.geely.ex2.tools.data.driving

import android.content.Context
import android.util.Log

object DrivingWifiWake {
    @Volatile
    private var wasConnected = false

    fun onWifiStatusUpdated(context: Context, isConnected: Boolean, reason: String) {
        val reconnected = isConnected && !wasConnected
        wasConnected = isConnected

        if (!reconnected) {
            return
        }

        val appContext = context.applicationContext
        val restoreMode = DrivingSettings.isPersistEnabled(appContext)
        val restoreRegen = DrivingSettings.isRegenPersistEnabled(appContext)
        if (!restoreMode && !restoreRegen) {
            return
        }

        if (restoreMode) {
            Log.i(DrivingModeController.TAG, "Wi-Fi reconnected, restore driving mode: $reason")
            DrivingModeController.restoreDrivingModeIfNeeded(appContext, "Wi-Fi reconnected: $reason")
        }
        if (restoreRegen) {
            Log.i(DrivingModeController.TAG, "Wi-Fi reconnected, restore regen: $reason")
            EnergyRegenController.restoreEnergyRegenerationIfNeeded(
                appContext,
                "Wi-Fi reconnected: $reason",
            )
        }
    }
}
