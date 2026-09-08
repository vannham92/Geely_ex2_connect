package com.geely.ex2.tools.data.window

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
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.DoorCorner
import com.geely.ex2.tools.data.vhal.VhalConstants

/**
 * Foreground service (process `:core`) theo dõi DOOR_POS suốt phiên xe để áp
 * [AutoWindowController]. START_STICKY vì tính năng phải sống khi app không mở.
 * Nếu VHAL từ chối callback on-change → chuyển sang poll [VhalConstants.AUTO_WINDOW_DOOR_POLL_INTERVAL_MS].
 */
class AutoWindowService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var controller: AutoWindowController? = null

    @Volatile
    private var isRunning = false

    @Volatile
    private var usePollingFallback = false

    private val pollRunnable = Runnable { pollDoors() }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.i(AutoWindowController.TAG, "Auto window service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: "service start"

        if (!AutoWindowSettings.isAnyCornerEnabled(this)) {
            Log.i(AutoWindowController.TAG, "Auto window service stopping, disabled: $reason")
            tearDown(stopSelf = true)
            return START_NOT_STICKY
        }

        isRunning = true
        handler.removeCallbacks(pollRunnable)
        startOrRefresh(reason)
        return START_STICKY
    }

    override fun onDestroy() {
        tearDown(stopSelf = false)
        Log.i(AutoWindowController.TAG, "Auto window service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOrRefresh(reason: String) {
        CarPropertyIo.execute {
            if (!isRunning || !AutoWindowSettings.isAnyCornerEnabled(this)) return@execute

            val activeController = controller ?: AutoWindowController(this).also { controller = it }
            activeController.syncBaseline(reason)

            if (!isRunning || !AutoWindowSettings.isAnyCornerEnabled(this)) {
                activeController.stopWatching()
                return@execute
            }

            val watching = activeController.startWatching { corner, isOpen ->
                handleDoorEvent(corner, isOpen)
            }
            usePollingFallback = !watching
            if (watching) {
                Log.i(AutoWindowController.TAG, "Door events via VHAL callback ($reason)")
            } else {
                Log.w(AutoWindowController.TAG, "Door callback unavailable, falling back to poll ($reason)")
                schedulePoll()
            }
        }
    }

    private fun handleDoorEvent(corner: DoorCorner, isOpen: Boolean) {
        CarPropertyIo.execute {
            if (!isRunning || !AutoWindowSettings.isAnyCornerEnabled(this)) return@execute
            controller?.onDoorState(corner, isOpen, "callback")
        }
    }

    private fun pollDoors() {
        CarPropertyIo.execute {
            if (!isRunning || !AutoWindowSettings.isAnyCornerEnabled(this) || !usePollingFallback) return@execute
            controller?.pollDoors("poll")
            schedulePoll()
        }
    }

    private fun schedulePoll() {
        if (!isRunning || !usePollingFallback) return
        handler.removeCallbacks(pollRunnable)
        handler.postDelayed(pollRunnable, VhalConstants.AUTO_WINDOW_DOOR_POLL_INTERVAL_MS)
    }

    private fun tearDown(stopSelf: Boolean) {
        isRunning = false
        usePollingFallback = false
        handler.removeCallbacks(pollRunnable)
        // Đóng đồng bộ để callback binder dừng trước khi service chết.
        CarPropertyIo.call {
            controller?.close()
            controller = null
        }
        if (stopSelf) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.auto_window_channel),
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
            .setContentTitle(getString(R.string.auto_window_notification_title))
            .setContentText(getString(R.string.auto_window_notification_text))
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_REASON = "reason"
        private const val CHANNEL_ID = "auto_window"
        private const val NOTIFICATION_ID = 4110
    }
}
