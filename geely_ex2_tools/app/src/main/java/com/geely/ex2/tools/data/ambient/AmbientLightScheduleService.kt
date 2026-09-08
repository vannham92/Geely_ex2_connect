package com.geely.ex2.tools.data.ambient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.geely.ex2.tools.R

class AmbientLightScheduleService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tickReason = "schedule tick"

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!AmbientLightSettings.isScheduleEnabled(this@AmbientLightScheduleService)) {
                Log.i(TAG, "Ambient schedule service stopping, auto disabled")
                finishService()
                return
            }
            AmbientLightScheduleController.applyIfNeeded(this@AmbientLightScheduleService, tickReason)
            mainHandler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.i(TAG, "Ambient schedule service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tickReason = intent?.getStringExtra(EXTRA_REASON) ?: "service start"
        if (!AmbientLightSettings.isScheduleEnabled(this)) {
            Log.i(TAG, "Ambient schedule service skipped, auto disabled: $tickReason")
            finishService()
            return START_NOT_STICKY
        }

        mainHandler.removeCallbacks(tickRunnable)
        AmbientLightScheduleController.applyIfNeeded(this, tickReason)
        AmbientLightScheduleAlarm.scheduleNext(this, "service start")
        mainHandler.postDelayed(tickRunnable, CHECK_INTERVAL_MS)
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(tickRunnable)
        if (AmbientLightSettings.isScheduleEnabled(this)) {
            AmbientLightScheduleAlarm.scheduleNext(this, "service destroyed")
        }
        Log.i(TAG, "Ambient schedule service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun finishService() {
        mainHandler.removeCallbacks(tickRunnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.ambient_light_schedule_channel),
            NotificationManager.IMPORTANCE_MIN,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setSmallIcon(R.drawable.ic_notification_speed)
            .setContentTitle(getString(R.string.ambient_light_schedule_notification_title))
            .setContentText(getString(R.string.ambient_light_schedule_notification_text))
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_REASON = "reason"
        private const val TAG = "GeelyToolsAmbient"
        private const val CHANNEL_ID = "ambient_light_schedule"
        private const val NOTIFICATION_ID = 4107
        private const val CHECK_INTERVAL_MS = 60_000L
    }
}
