package com.geely.ex2.tools.data.ambient

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AmbientLightAppStarter {
    fun startRestoreServiceIfEnabled(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!AmbientLightSettings.shouldRestoreOnWake(appContext)) {
            Log.i(AmbientLightScheduleController.TAG, "Ambient restore service not started: $reason")
            stopRestoreService(appContext, reason)
            return
        }
        startRestoreService(appContext, reason)
    }

    fun startRestoreService(context: Context, reason: String) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, AmbientLightRestoreService::class.java).apply {
            putExtra(AmbientLightRestoreService.EXTRA_REASON, reason)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
        Log.i(AmbientLightScheduleController.TAG, "Ambient restore service start requested: $reason")
    }

    fun stopRestoreService(context: Context, reason: String) {
        Log.i(AmbientLightScheduleController.TAG, "Ambient restore service stop requested: $reason")
        val appContext = context.applicationContext
        appContext.stopService(Intent(appContext, AmbientLightRestoreService::class.java))
    }

    fun startScheduleServiceIfEnabled(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!AmbientLightSettings.isScheduleEnabled(appContext)) {
            Log.i(AmbientLightScheduleController.TAG, "Ambient schedule service not started, auto disabled: $reason")
            stopScheduleService(appContext, reason)
            return
        }
        startScheduleService(appContext, reason)
    }

    fun startScheduleService(context: Context, reason: String) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, AmbientLightScheduleService::class.java).apply {
            putExtra(AmbientLightScheduleService.EXTRA_REASON, reason)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
        Log.i(AmbientLightScheduleController.TAG, "Ambient schedule service start requested: $reason")
    }

    fun stopScheduleService(context: Context, reason: String) {
        Log.i(AmbientLightScheduleController.TAG, "Ambient schedule service stop requested: $reason")
        val appContext = context.applicationContext
        appContext.stopService(Intent(appContext, AmbientLightScheduleService::class.java))
    }
}
