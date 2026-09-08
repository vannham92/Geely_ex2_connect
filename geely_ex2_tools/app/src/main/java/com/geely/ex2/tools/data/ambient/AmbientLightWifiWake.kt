package com.geely.ex2.tools.data.ambient

import android.content.Context
import android.util.Log

object AmbientLightWifiWake {
    @Volatile
    private var wasConnected = false

    fun onWifiStatusUpdated(context: Context, isConnected: Boolean, reason: String) {
        val reconnected = isConnected && !wasConnected
        wasConnected = isConnected

        if (!reconnected) {
            return
        }

        val appContext = context.applicationContext
        if (!AmbientLightSettings.shouldRestoreOnWake(appContext)) {
            return
        }

        Log.i(AmbientLightScheduleController.TAG, "Wi-Fi reconnected, restore ambient light: $reason")
        AmbientLightAppStarter.startRestoreService(appContext, "Wi-Fi reconnected: $reason")
        if (AmbientLightSettings.isScheduleEnabled(appContext)) {
            AmbientLightScheduleController.syncBackgroundWork(appContext, reason)
        }
    }
}
