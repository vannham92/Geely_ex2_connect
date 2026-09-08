package com.geely.ex2.tools.data.avas

import android.content.Context
import android.content.Intent

object AvasWakeEvents {
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
        if (!AvasSettings.isMutedSaved(appContext)) {
            AvasAppStarter.stopRestoreService(appContext, reason)
            return
        }
        AvasAppStarter.startRestoreService(appContext, reason)
    }
}
