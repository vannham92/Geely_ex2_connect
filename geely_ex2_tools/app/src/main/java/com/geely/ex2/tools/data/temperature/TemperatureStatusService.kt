package com.geely.ex2.tools.data.temperature

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.VhalConstants

class TemperatureStatusService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var reader: TemperatureReader? = null

    @Volatile
    private var isRunning = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning || !TemperatureSettings.isEnabled(this@TemperatureStatusService)) {
                return
            }
            updateTemperature("periodic")
            if (isRunning && TemperatureSettings.isEnabled(this@TemperatureStatusService)) {
                handler.postDelayed(this, VhalConstants.TEMPERATURE_POLL_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(
            TemperatureStatusIconHelper.SERVICE_NOTIFICATION_ID,
            TemperatureStatusIconHelper.buildServiceNotification(this),
        )
        Log.i(TAG, "Temperature service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: "service start"

        if (!TemperatureSettings.isEnabled(this)) {
            Log.i(TAG, "Temperature service stopping, disabled: $reason")
            isRunning = false
            handler.removeCallbacks(updateRunnable)
            TemperatureStatusIconHelper.cancelStatusIcon(this)
            closeReaderAsync()
            stopSelf()
            return START_NOT_STICKY
        }

        updateTemperature(reason)
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, VhalConstants.TEMPERATURE_POLL_INTERVAL_MS)

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        TemperatureStatusIconHelper.cancelStatusIcon(this)
        closeReaderAsync()
        Log.i(TAG, "Temperature service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateTemperature(reason: String) {
        CarPropertyIo.execute {
            if (!isRunning || !TemperatureSettings.isEnabled(this@TemperatureStatusService)) {
                return@execute
            }

            val temperatureReader = reader ?: TemperatureReader(this@TemperatureStatusService).also {
                reader = it
            }
            val result = temperatureReader.readTemperature()
            if (!isRunning || !TemperatureSettings.isEnabled(this@TemperatureStatusService)) {
                return@execute
            }

            if (result.ok) {
                Log.i(TAG, "Outside temperature: ${result.value} C, source: ${result.source}")
            } else {
                Log.w(TAG, "Temperature read failed: ${result.source}, details: ${result.details}")
            }
            TemperatureStatusIconHelper.notifyTemperature(
                this@TemperatureStatusService,
                result,
                reason,
                force = reason != "periodic",
            )
        }
    }

    private fun closeReaderAsync() {
        CarPropertyIo.execute {
            reader?.close()
            reader = null
        }
    }

    companion object {
        const val EXTRA_REASON = "reason"
        private const val TAG = "GeelyToolsTemperature"
    }
}
