package com.geely.ex2.tools.data.driving

import android.content.Context
import android.content.Intent

object DrivingWakeEvents {
    private const val QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"

    fun isWakeAction(action: String?): Boolean {
        if (action == null) return false
        return action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == QUICKBOOT_POWERON
    }

    fun onManifestWake(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!DrivingSettings.hasAnyPersistEnabled(appContext)) {
            DrivingAppStarter.stopRestoreService(appContext, reason)
            return
        }
        DrivingAppStarter.startRestoreService(appContext, reason)
    }
}
