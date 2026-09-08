package com.geely.ex2.tools.data.temperature

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo

object TemperatureAppStarter {
    fun startServiceIfEnabled(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!TemperatureSettings.isEnabled(appContext)) {
            Log.i(TAG, "Temperature service not started, disabled: $reason")
            stopService(appContext, reason)
            TemperatureStatusIconHelper.cancelStatusIcon(appContext)
            return
        }
        startService(appContext, reason)
    }

    fun notifyStatusIconIfEnabled(context: Context, reason: String, rank: Int? = null) {
        val appContext = context.applicationContext
        if (!TemperatureSettings.isEnabled(appContext)) {
            TemperatureStatusIconHelper.cancelStatusIcon(appContext)
            return
        }

        val iconRank = rank ?: TemperatureSettings.getStatusIconRank(appContext)
        CarPropertyIo.execute {
            val reader = TemperatureReader(appContext)
            val result = try {
                reader.readTemperature()
            } finally {
                reader.close()
            }
            TemperatureStatusIconHelper.notifyTemperature(
                context = appContext,
                result = result,
                reason = reason,
                rank = iconRank,
            )
        }
    }

    fun stopService(context: Context, reason: String) {
        Log.i(TAG, "Temperature service stop requested: $reason")
        context.applicationContext.stopService(Intent(context, TemperatureStatusService::class.java))
    }

    fun cancelStatusIcon(context: Context) {
        TemperatureStatusIconHelper.cancelStatusIcon(context.applicationContext)
    }

    private fun startService(context: Context, reason: String) {
        val intent = Intent(context, TemperatureStatusService::class.java).apply {
            putExtra(TemperatureStatusService.EXTRA_REASON, reason)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        Log.i(TAG, "Temperature service start requested: $reason")
    }

    private const val TAG = "GeelyToolsTemperature"
}
