package com.geely.ex2.tools.data.avas

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AvasAppStarter {
    fun startRestoreServiceIfEnabled(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!AvasSettings.isMutedSaved(appContext)) {
            Log.i(AvasController.TAG, "AVAS restore service not started, mute off: $reason")
            stopRestoreService(appContext, reason)
            return
        }
        startRestoreService(appContext, reason)
    }

    fun startRestoreService(context: Context, reason: String) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, AvasRestoreService::class.java).apply {
            putExtra(AvasRestoreService.EXTRA_REASON, reason)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
        Log.i(AvasController.TAG, "AVAS restore service start requested: $reason")
    }

    fun stopRestoreService(context: Context, reason: String) {
        Log.i(AvasController.TAG, "AVAS restore service stop requested: $reason")
        context.applicationContext.stopService(
            Intent(context.applicationContext, AvasRestoreService::class.java),
        )
    }
}
