package com.geely.ex2.tools.data.wifi

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

import com.geely.ex2.tools.data.driving.DrivingWifiWake
import com.geely.ex2.tools.data.ambient.AmbientLightWifiWake

class WifiStatusService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false
    private var pendingWifiReason = "debounced"
    private var pendingIconReason = "debounced"

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                WifiManager.RSSI_CHANGED_ACTION -> scheduleIconUpdate("service Wi-Fi event: $action")
                WifiManager.WIFI_STATE_CHANGED_ACTION -> scheduleWifiRefresh("service Wi-Fi event: $action")
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> scheduleWifiRefresh("service Wi-Fi event: $action")
            }
        }
    }

    private val wifiRefreshRunnable = Runnable {
        WifiAutoEnableController.enableWifiIfNeeded(this, pendingWifiReason)
        notifyDrivingOnWifiStatus(pendingWifiReason)
        updateIcon(pendingWifiReason)
    }

    private val iconRefreshRunnable = Runnable {
        notifyDrivingOnWifiStatus(pendingIconReason)
        updateIcon(pendingIconReason)
    }

    override fun onCreate() {
        super.onCreate()
        registerWifiReceiver()
        startForeground(
            WifiStatusIconHelper.SERVICE_NOTIFICATION_ID,
            WifiStatusIconHelper.buildServiceNotification(this),
        )
        Log.i(WifiAutoEnableController.TAG, "Wi-Fi foreground service notification started: service create")
        scheduleWifiRefresh("service create", delayMs = 0)
        Log.i(WifiAutoEnableController.TAG, "Wi-Fi status service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_START_REASON) ?: "service start"
        scheduleWifiRefresh(reason, delayMs = 0)
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(wifiRefreshRunnable)
        mainHandler.removeCallbacks(iconRefreshRunnable)
        unregisterWifiReceiver()
        Log.i(WifiAutoEnableController.TAG, "Wi-Fi status service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleWifiRefresh(reason: String, delayMs: Long = WIFI_DEBOUNCE_MS) {
        pendingWifiReason = reason
        mainHandler.removeCallbacks(wifiRefreshRunnable)
        if (delayMs <= 0) {
            mainHandler.post(wifiRefreshRunnable)
        } else {
            mainHandler.postDelayed(wifiRefreshRunnable, delayMs)
        }
    }

    private fun scheduleIconUpdate(reason: String) {
        pendingIconReason = reason
        mainHandler.removeCallbacks(iconRefreshRunnable)
        mainHandler.postDelayed(iconRefreshRunnable, ICON_DEBOUNCE_MS)
    }

    private fun registerWifiReceiver() {
        if (receiverRegistered) {
            return
        }

        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
        }

        registerReceiver(wifiReceiver, filter)
        receiverRegistered = true
        Log.i(WifiAutoEnableController.TAG, "Wi-Fi receiver registered in service")
    }

    private fun unregisterWifiReceiver() {
        if (!receiverRegistered) {
            return
        }

        try {
            unregisterReceiver(wifiReceiver)
        } catch (_: IllegalArgumentException) {
        }

        receiverRegistered = false
    }

    private fun updateIcon(reason: String) {
        WifiStatusIconHelper.notifyStatusIcon(this, reason)
    }

    private fun notifyDrivingOnWifiStatus(reason: String) {
        val connected = WifiStatusIconHelper.isWifiConnected(this)
        DrivingWifiWake.onWifiStatusUpdated(this, connected, reason)
        AmbientLightWifiWake.onWifiStatusUpdated(this, connected, reason)
    }

    companion object {
        const val EXTRA_START_REASON = "start_reason"
        private const val WIFI_DEBOUNCE_MS = 400L
        private const val ICON_DEBOUNCE_MS = 500L
    }
}
