package com.geely.ex2.tools.data.ambient

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object AmbientLightScheduleAlarm {
    const val ACTION_TICK = "com.geely.ex2.tools.action.AMBIENT_LIGHT_SCHEDULE_TICK"

    private const val REQUEST_CODE = 4107

    fun start(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!AmbientLightSettings.isScheduleEnabled(appContext)) {
            cancel(appContext, reason)
            return
        }
        AmbientLightScheduleController.applyIfNeeded(appContext, "alarm start: $reason")
        scheduleNext(appContext, reason)
    }

    fun cancel(context: Context, reason: String) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(buildPendingIntent(appContext))
        Log.i(AmbientLightScheduleController.TAG, "Ambient schedule alarm cancelled: $reason")
    }

    fun scheduleNext(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!AmbientLightSettings.isScheduleEnabled(appContext)) {
            cancel(appContext, reason)
            return
        }

        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        if (alarmManager == null) {
            Log.w(AmbientLightScheduleController.TAG, "AlarmManager unavailable: $reason")
            return
        }

        val triggerAt = nextMinuteBoundaryMillis()
        val pendingIntent = buildPendingIntent(appContext)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (securityError: SecurityException) {
            Log.w(AmbientLightScheduleController.TAG, "Exact alarm denied, using inexact: $reason", securityError)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        Log.i(
            AmbientLightScheduleController.TAG,
            "Ambient schedule alarm planned at $triggerAt: $reason",
        )
    }

    private fun nextMinuteBoundaryMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, 1)
        }
        return calendar.timeInMillis
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AmbientLightScheduleReceiver::class.java).apply {
            action = ACTION_TICK
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
