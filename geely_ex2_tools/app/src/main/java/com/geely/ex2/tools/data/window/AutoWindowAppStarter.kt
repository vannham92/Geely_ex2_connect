package com.geely.ex2.tools.data.window

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AutoWindowAppStarter {
    fun startServiceIfEnabled(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!AutoWindowSettings.isAnyCornerEnabled(appContext)) {
            Log.i(AutoWindowController.TAG, "Auto window service not started, disabled: $reason")
            stopService(appContext, reason)
            return
        }
        startService(appContext, reason)
    }

    fun startService(context: Context, reason: String) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, AutoWindowService::class.java).apply {
            putExtra(AutoWindowService.EXTRA_REASON, reason)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
        Log.i(AutoWindowController.TAG, "Auto window service start requested: $reason")
    }

    fun stopService(context: Context, reason: String) {
        Log.i(AutoWindowController.TAG, "Auto window service stop requested: $reason")
        context.applicationContext.stopService(
            Intent(context.applicationContext, AutoWindowService::class.java),
        )
    }
}
