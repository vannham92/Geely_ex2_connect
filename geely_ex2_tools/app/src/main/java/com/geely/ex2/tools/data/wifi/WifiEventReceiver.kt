package com.geely.ex2.tools.data.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log

class WifiEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "null"
        Log.i(WifiAutoEnableController.TAG, "Receiver action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED == action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == action ||
            QUICKBOOT_POWERON == action ||
            WifiManager.WIFI_STATE_CHANGED_ACTION == action
        ) {
            WifiAutoEnableController.enableWifiIfNeeded(context, "receiver: $action")
            WifiStatusIconHelper.notifyStatusIcon(context, "receiver: $action")
            WifiAppStarter.startStatusService(context, "receiver: $action")
        }
    }

    companion object {
        private const val QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
