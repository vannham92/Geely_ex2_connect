package com.geely.ex2.tools.data.driving

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object DrivingAppStarter {
    fun startRestoreServiceIfEnabled(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!DrivingSettings.hasAnyPersistEnabled(appContext)) {
            Log.i(DrivingModeController.TAG, "Driving restore service not started, persist disabled: $reason")
            stopRestoreService(appContext, reason)
            return
        }
        startRestoreService(appContext, reason)
    }

    fun startRestoreService(context: Context, reason: String) {
        val appContext = context.applicationContext

        val intent = Intent(appContext, DrivingRestoreService::class.java).apply {
            putExtra(DrivingRestoreService.EXTRA_REASON, reason)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
        Log.i(DrivingModeController.TAG, "Driving restore service start requested: $reason")
    }

    fun stopRestoreServiceIfIdle(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (DrivingSettings.hasAnyPersistEnabled(appContext)) {
            Log.i(DrivingModeController.TAG, "Driving restore service kept, other persist still on: $reason")
            return
        }
        stopRestoreService(appContext, reason)
    }

    fun stopRestoreService(context: Context, reason: String) {
        Log.i(DrivingModeController.TAG, "Driving restore service stop requested: $reason")
        val appContext = context.applicationContext
        appContext.stopService(Intent(appContext, DrivingRestoreService::class.java))
    }
}
