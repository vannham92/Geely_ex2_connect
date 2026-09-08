package com.geely.ex2.tools.data.avas

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

class AvasRestoreService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var restoreScheduled = false

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.i(AvasController.TAG, "AVAS restore service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: "service start"

        if (!AvasSettings.isMutedSaved(this)) {
            Log.i(AvasController.TAG, "AVAS restore skipped, mute off: $reason")
            finishService()
            return START_NOT_STICKY
        }

        if (restoreScheduled) {
            Log.d(AvasController.TAG, "AVAS restore already scheduled: $reason")
            return START_NOT_STICKY
        }
        restoreScheduled = true

        // Đối với boot/wake — tạm dừng giống CentralEXAuto (~12s). Đối với UI/Application — chạy ngay.
        val delayMs = if (reason.startsWith("receiver:") || reason.contains("BOOT", ignoreCase = true)) {
            AvasConstants.AVAS_RESTORE_DELAY_MS
        } else {
            0L
        }

        mainHandler.postDelayed({
            AvasController.restoreMuteIfNeeded(this, reason) {
                mainHandler.post { finishService() }
            }
        }, delayMs)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        Log.i(AvasController.TAG, "AVAS restore service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun finishService() {
        restoreScheduled = false
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
            getString(R.string.avas_restore_channel),
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
            .setContentTitle(getString(R.string.avas_restore_notification_title))
            .setContentText(getString(R.string.avas_restore_notification_text))
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_REASON = "reason"
        private const val CHANNEL_ID = "avas_restore"
        private const val NOTIFICATION_ID = 4109
    }
}
