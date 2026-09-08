package com.geely.ex2.tools.data.temperature

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TemperatureEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "null"
        Log.i(TAG, "Receiver action: $action")

        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == QUICKBOOT_POWERON
        ) {
            TemperatureAppStarter.startServiceIfEnabled(context, "receiver: $action")
            TemperatureAppStarter.notifyStatusIconIfEnabled(context, "receiver: $action")
        }
    }

    companion object {
        private const val TAG = "GeelyToolsTemperature"
        private const val QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
