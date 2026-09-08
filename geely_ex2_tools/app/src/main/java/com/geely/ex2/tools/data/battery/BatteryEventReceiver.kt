package com.geely.ex2.tools.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo

class BatteryEventReceiver : BroadcastReceiver() {
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
                    BatteryAppStarter.startServiceIfEnabled(appContext, "receiver: $action")
                    // Nested execute runs inline; also updates the app widget.
                    BatteryAppStarter.notifyStatusIconIfEnabled(appContext, "receiver: $action")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "GeelyToolsBattery"
        private const val QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
