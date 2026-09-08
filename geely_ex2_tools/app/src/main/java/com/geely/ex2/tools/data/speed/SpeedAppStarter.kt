package com.geely.ex2.tools.data.speed

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.SpeedSample
import com.geely.ex2.tools.data.vhal.VhalSpeedReaderFactory

object SpeedAppStarter {
    fun startServiceIfEnabled(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!SpeedSettings.isEnabled(appContext)) {
            Log.i(TAG, "Speed service not started, disabled: $reason")
            stopService(appContext, reason)
            SpeedStatusIconHelper.cancelStatusIcon(appContext)
            SpeedSampleStore.clear()
            return
        }
        startService(appContext, reason)
    }

    fun notifyStatusIconIfEnabled(context: Context, reason: String, rank: Int? = null) {
        val appContext = context.applicationContext
        if (!SpeedSettings.isEnabled(appContext)) {
            SpeedStatusIconHelper.cancelStatusIcon(appContext)
            SpeedSampleStore.clear()
            return
        }

        val iconRank = rank ?: SpeedSettings.getStatusIconRank(appContext)
        CarPropertyIo.execute {
            // Rank-only: reuse store. Start/resume: always re-read so UI is not stale.
            val sample = if (rank != null) {
                SpeedSampleStore.latest() ?: readAndPublish(appContext)
            } else {
                readAndPublish(appContext)
            }
            SpeedStatusIconHelper.notifySpeed(
                context = appContext,
                sample = sample,
                reason = reason,
                rank = iconRank,
            )
        }
    }

    fun stopService(context: Context, reason: String) {
        Log.i(TAG, "Speed service stop requested: $reason")
        val appContext = context.applicationContext
        val intent = Intent(appContext, SpeedStatusService::class.java).apply {
            putExtra(SpeedStatusService.EXTRA_REASON, reason)
        }
        // Wake running service so onStartCommand unsubscribes and stops immediately.
        appContext.startService(intent)
        appContext.stopService(intent)
    }

    private fun readAndPublish(appContext: Context): SpeedSample {
        val reader = VhalSpeedReaderFactory.create(appContext)
        return try {
            reader.readSpeed()
        } finally {
            reader.close()
        }.also { SpeedSampleStore.publish(it) }
    }

    private fun startService(context: Context, reason: String) {
        val intent = Intent(context, SpeedStatusService::class.java).apply {
            putExtra(SpeedStatusService.EXTRA_REASON, reason)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        Log.i(TAG, "Speed service start requested: $reason")
    }

    private const val TAG = "GeelyToolsSpeed"
}
