package com.geely.ex2.tools.data.speed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo

class SpeedEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "null"
        Log.i(TAG, "Receiver action: $action")

        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == QUICKBOOT_POWERON
        ) {
            val appContext = context.applicationContext
            val pendingResult = goAsync()
            CarPropertyIo.execute {
                try {
                    SpeedAppStarter.startServiceIfEnabled(appContext, "receiver: $action")
                    SpeedAppStarter.notifyStatusIconIfEnabled(appContext, "receiver: $action")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "GeelyToolsSpeed"
        private const val QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
