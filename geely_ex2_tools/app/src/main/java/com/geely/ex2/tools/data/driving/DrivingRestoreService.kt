package com.geely.ex2.tools.data.driving

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
import com.geely.ex2.tools.data.vhal.VhalConstants
import com.geely.ex2.tools.data.vhal.VhalVehicleEvent
import com.geely.ex2.tools.data.vhal.VhalVehicleEventHub
import com.geely.ex2.tools.data.vhal.VhalVehicleEventListener
import java.util.concurrent.atomic.AtomicInteger

class DrivingRestoreService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingRestores = AtomicInteger(0)
    private var tickReason = "service start"

    @Volatile
    private var eventHub: VhalVehicleEventHub? = null

    private var lastHandledGearSelection: Int? = null

    private val vehicleEventListener = VhalVehicleEventListener { event ->
        when (event) {
            is VhalVehicleEvent.GearChanged -> onGearChanged(event)
        }
    }

    private val syncRunnable = object : Runnable {
        override fun run() {
            if (!DrivingSettings.hasAnyPersistEnabled(this@DrivingRestoreService)) {
                Log.i(TAG, "Driving sync stopping, persist disabled")
                finishService()
                return
            }
            ensureEventHub()
            DrivingPersistSyncController.syncFromCarIfNeeded(this@DrivingRestoreService, tickReason)
            mainHandler.postDelayed(this, VhalConstants.DRIVING_PERSIST_SYNC_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.i(TAG, "Driving restore service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tickReason = intent?.getStringExtra(EXTRA_REASON) ?: "service start"
        val restoreMode = DrivingSettings.isPersistEnabled(this)
        val restoreRegen = DrivingSettings.isRegenPersistEnabled(this)

        if (!restoreMode && !restoreRegen) {
            Log.i(TAG, "Driving restore skipped, persist disabled: $tickReason")
            finishService()
            return START_NOT_STICKY
        }

        var scheduled = 0
        if (restoreMode) scheduled++
        if (restoreRegen) scheduled++
        pendingRestores.addAndGet(scheduled)

        if (restoreMode) {
            DrivingModeController.restoreDrivingModeIfNeeded(this, tickReason) {
                onRestoreFinished()
            }
        }
        if (restoreRegen) {
            EnergyRegenController.restoreEnergyRegenerationIfNeeded(this, tickReason) {
                onRestoreFinished()
            }
        }

        ensureEventHub()

        mainHandler.removeCallbacks(syncRunnable)
        mainHandler.postDelayed(syncRunnable, VhalConstants.DRIVING_PERSIST_SYNC_INTERVAL_MS)
        return START_STICKY
    }

    private fun onGearChanged(event: VhalVehicleEvent.GearChanged) {
        if (!DrivingSettings.hasAnyPersistEnabled(this)) {
            return
        }

        val selection = event.gearSelection
        if (selection == lastHandledGearSelection) {
            return
        }
        lastHandledGearSelection = selection

        val reason = "gear changed selection=$selection"
        Log.i(TAG, reason)

        if (DrivingSettings.isPersistEnabled(this)) {
            DrivingModeController.restoreDrivingModeIfNeeded(this, reason)
        }
        if (DrivingSettings.isRegenPersistEnabled(this)) {
            EnergyRegenController.restoreEnergyRegenerationIfNeeded(this, reason)
        }
    }

    private fun ensureEventHub() {
        if (eventHub != null) return

        val hub = VhalVehicleEventHub(this)
        hub.addListener(vehicleEventListener)
        eventHub = hub
        CarPropertyIo.execute {
            if (eventHub !== hub) {
                hub.removeListener(vehicleEventListener)
                hub.stop()
                return@execute
            }
            if (!hub.start()) {
                if (eventHub === hub) {
                    eventHub = null
                }
                hub.removeListener(vehicleEventListener)
                Log.w(TAG, "Gear VHAL subscription unavailable")
            }
        }
    }

    private fun tearDownEventHub() {
        val hub = eventHub ?: return
        eventHub = null
        lastHandledGearSelection = null
        hub.removeListener(vehicleEventListener)
        CarPropertyIo.execute {
            hub.stop()
        }
    }

    private fun onRestoreFinished() {
        pendingRestores.decrementAndGet()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(syncRunnable)
        tearDownEventHub()
        Log.i(TAG, "Driving restore service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun finishService() {
        mainHandler.removeCallbacks(syncRunnable)
        tearDownEventHub()
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
            getString(R.string.driving_restore_channel),
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
            .setContentTitle(getString(R.string.driving_restore_notification_title))
            .setContentText(getString(R.string.driving_restore_notification_text))
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_REASON = "reason"
        private const val TAG = "GeelyToolsDriving"
        private const val CHANNEL_ID = "driving_restore"
        private const val NOTIFICATION_ID = 4106
    }
}
