package com.geely.ex2.tools.data.battery

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.VhalBatteryReader
import com.geely.ex2.tools.data.vhal.VhalBatteryReaderFactory
import com.geely.ex2.tools.data.vhal.VhalConstants

class BatteryStatusService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var reader: VhalBatteryReader? = null

    @Volatile
    private var isRunning = false

    private val updateRunnable = Runnable {
        if (!isRunning || !BatterySettings.isEnabled(this)) {
            return@Runnable
        }
        updateBattery("periodic")
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(
            BatteryStatusIconHelper.SERVICE_NOTIFICATION_ID,
            BatteryStatusIconHelper.buildServiceNotification(this),
        )
        Log.i(TAG, "Battery service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: "service start"

        if (!BatterySettings.isEnabled(this)) {
            Log.i(TAG, "Battery service stopping, disabled: $reason")
            isRunning = false
            handler.removeCallbacks(updateRunnable)
            BatteryStatusIconHelper.cancelStatusIcon(this)
            closeReaderAsync()
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        handler.removeCallbacks(updateRunnable)
        updateBattery(reason)

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        BatteryStatusIconHelper.cancelStatusIcon(this)
        closeReaderAsync()
        Log.i(TAG, "Battery service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateBattery(reason: String) {
        CarPropertyIo.execute {
            if (!isRunning || !BatterySettings.isEnabled(this@BatteryStatusService)) {
                return@execute
            }

            val batteryReader = reader ?: VhalBatteryReaderFactory.create(this@BatteryStatusService).also {
                reader = it
            }
            val sample = batteryReader.readBatterySoc()
            if (!isRunning || !BatterySettings.isEnabled(this@BatteryStatusService)) {
                return@execute
            }

            if (sample.isAvailable) {
                Log.d(TAG, "Battery SOC: ${sample.socPercent}%, source: ${sample.source}")
            } else {
                Log.w(TAG, "Battery read failed: ${sample.source}")
            }
            BatterySampleStore.publish(sample)
            BatteryStatusIconHelper.notifyBattery(
                this@BatteryStatusService,
                sample,
                reason,
                force = reason != "periodic",
            )
            BatteryAppWidgetHelper.updateAll(this@BatteryStatusService, reason, sample)
            scheduleNextPoll()
        }
    }

    private fun scheduleNextPoll() {
        if (!isRunning || !BatterySettings.isEnabled(this)) {
            return
        }
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, VhalConstants.BATTERY_POLL_INTERVAL_MS)
    }

    private fun closeReaderAsync() {
        CarPropertyIo.execute {
            reader?.close()
            reader = null
        }
    }

    companion object {
        const val EXTRA_REASON = "reason"
        private const val TAG = "GeelyToolsBattery"
    }
}
