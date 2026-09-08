package com.geely.ex2.tools.data.speed

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.SpeedSample
import com.geely.ex2.tools.data.vhal.VhalConstants
import com.geely.ex2.tools.data.vhal.VhalSpeedReader
import com.geely.ex2.tools.data.vhal.VhalSpeedReaderFactory

class SpeedStatusService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var reader: VhalSpeedReader? = null

    @Volatile
    private var isRunning = false

    @Volatile
    private var usePollingFallback = false

    private val updateRunnable = Runnable {
        if (!isRunning || !SpeedSettings.isEnabled(this) || !usePollingFallback) {
            return@Runnable
        }
        updateSpeed("periodic")
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(
            SpeedStatusIconHelper.SERVICE_NOTIFICATION_ID,
            SpeedStatusIconHelper.buildServiceNotification(this),
        )
        Log.i(TAG, "Speed service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: "service start"

        if (!SpeedSettings.isEnabled(this)) {
            Log.i(TAG, "Speed service stopping, disabled: $reason")
            tearDown(cancelIcon = true, stopSelf = true)
            return START_NOT_STICKY
        }

        isRunning = true
        handler.removeCallbacks(updateRunnable)
        startOrRefresh(reason)

        return START_STICKY
    }

    override fun onDestroy() {
        tearDown(cancelIcon = true, stopSelf = false)
        Log.i(TAG, "Speed service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOrRefresh(reason: String) {
        CarPropertyIo.execute {
            if (!isRunning || !SpeedSettings.isEnabled(this@SpeedStatusService)) {
                return@execute
            }

            val speedReader = reader ?: VhalSpeedReaderFactory.create(this@SpeedStatusService).also {
                reader = it
            }

            val subscribed = speedReader.subscribeSpeed(
                updateRateHz = VhalConstants.SPEED_CALLBACK_RATE_HZ,
                onSample = { sample -> handleCallbackSample(sample) },
                onError = { error -> handleCallbackError(error) },
            )

            if (!isRunning || !SpeedSettings.isEnabled(this@SpeedStatusService)) {
                speedReader.unsubscribeSpeed()
                return@execute
            }

            usePollingFallback = !subscribed
            if (subscribed) {
                Log.i(TAG, "Speed updates via VHAL callback")
            } else {
                Log.w(TAG, "Speed callback unavailable, falling back to poll")
            }

            if (!isRunning || !SpeedSettings.isEnabled(this@SpeedStatusService)) {
                speedReader.unsubscribeSpeed()
                return@execute
            }

            publishAndNotify(speedReader.readSpeed(), reason, force = true)

            if (usePollingFallback) {
                scheduleNextPoll()
            }
        }
    }

    private fun handleCallbackSample(sample: SpeedSample) {
        CarPropertyIo.execute {
            if (!isRunning || !SpeedSettings.isEnabled(this@SpeedStatusService)) {
                return@execute
            }
            publishAndNotify(sample, "callback", force = false)
        }
    }

    private fun handleCallbackError(error: String) {
        CarPropertyIo.execute {
            if (!isRunning || !SpeedSettings.isEnabled(this@SpeedStatusService)) {
                return@execute
            }
            Log.w(TAG, "Speed callback error: $error")
            publishAndNotify(
                SpeedSample(
                    speedKmh = 0f,
                    isAvailable = false,
                    source = error,
                ),
                "callback",
                force = false,
            )
        }
    }

    private fun updateSpeed(reason: String) {
        CarPropertyIo.execute {
            if (!isRunning || !SpeedSettings.isEnabled(this@SpeedStatusService) || !usePollingFallback) {
                return@execute
            }

            val speedReader = reader ?: VhalSpeedReaderFactory.create(this@SpeedStatusService).also {
                reader = it
            }
            val sample = speedReader.readSpeed()
            if (!isRunning || !SpeedSettings.isEnabled(this@SpeedStatusService)) {
                return@execute
            }

            publishAndNotify(sample, reason, force = reason != "periodic")
            scheduleNextPoll()
        }
    }

    private fun publishAndNotify(sample: SpeedSample, reason: String, force: Boolean) {
        if (sample.isAvailable) {
            Log.d(TAG, "Vehicle speed: ${sample.speedKmh} km/h, source: ${sample.source}")
        } else {
            Log.w(TAG, "Speed read failed: ${sample.source}")
        }
        SpeedSampleStore.publish(sample)
        SpeedStatusIconHelper.notifySpeed(
            this@SpeedStatusService,
            sample,
            reason,
            force = force,
        )
    }

    private fun scheduleNextPoll() {
        if (!isRunning || !SpeedSettings.isEnabled(this) || !usePollingFallback) {
            return
        }
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, VhalConstants.SPEED_POLL_INTERVAL_MS)
    }

    private fun tearDown(cancelIcon: Boolean, stopSelf: Boolean) {
        isRunning = false
        usePollingFallback = false
        handler.removeCallbacks(updateRunnable)
        if (cancelIcon) {
            SpeedStatusIconHelper.cancelStatusIcon(this)
        }
        // Unsubscribe synchronously so binder callbacks stop before destroy/stopSelf.
        CarPropertyIo.call {
            reader?.close()
            reader = null
        }
        if (stopSelf) {
            stopSelf()
        }
    }

    companion object {
        const val EXTRA_REASON = "reason"
        private const val TAG = "GeelyToolsSpeed"
    }
}
