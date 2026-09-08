package com.geely.ex2.tools.data.wifi

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object WifiAppStarter {
    fun startStatusService(context: Context, reason: String) {
        val intent = Intent(context, WifiStatusService::class.java).apply {
            putExtra(WifiStatusService.EXTRA_START_REASON, reason)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        Log.i(WifiAutoEnableController.TAG, "Wi-Fi status service start requested: $reason")
    }

    fun stopStatusService(context: Context, reason: String) {
        Log.i(WifiAutoEnableController.TAG, "Wi-Fi status service stop requested: $reason")
        context.applicationContext.stopService(Intent(context, WifiStatusService::class.java))
    }
}
