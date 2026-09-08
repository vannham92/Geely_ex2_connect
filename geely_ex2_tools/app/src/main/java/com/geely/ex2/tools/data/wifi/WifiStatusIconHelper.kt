package com.geely.ex2.tools.data.wifi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.geely.ex2.tools.MainActivity
import com.geely.ex2.tools.R

object WifiStatusIconHelper {
    const val NOTIFICATION_ID = 14021
    const val SERVICE_NOTIFICATION_ID = 14022

    @Volatile
    private var lastNotifiedRank: Int? = null

    private const val CHANNEL_ID = "wifi_status_icon"
    private const val SERVICE_CHANNEL_ID = "wifi_status_service"

    private const val FLAG_STATUS_ICON = "flag_status_icon_notification"
    private const val FLAG_STATUS_ICON_ID = "flag_status_icon_id"
    private const val FLAG_STATUS_ICON_DESCRIBE = "flag_status_icon_describe"
    private const val FLAG_STATUS_ICON_ICON = "flag_status_icon_icon"
    private const val FLAG_STATUS_ICON_PRESSED_ICON = "flag_status_icon_pressed_icon"
    private const val FLAG_STATUS_ICON_HIDE = "flag_status_icon_hide"
    private const val FLAG_STATUS_ICON_RANK = "flag_status_icon_rank"
    private const val FLAG_STATUS_ICON_SPACE_X = "flag_status_icon_space_x"
    private const val FLAG_STATUS_ICON_IS_PICK_ON = "flag_status_icon_is_pick_on"
    private const val FLAG_STATUS_ICON_SPECIFIC_WIDTH = "flag_status_icon_specific_width"

    fun isWifiConnected(context: Context): Boolean = isWifiConnectedToNetwork(context)

    fun notifyStatusIcon(context: Context, reason: String, rank: Int? = null) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (notificationManager == null) {
            Log.e(WifiAutoEnableController.TAG, "NotificationManager is null, reason: $reason")
            return
        }

        val iconRank = rank ?: WifiSettings.getStatusIconRank(context)
        val previousRank = lastNotifiedRank
        if (previousRank != null && previousRank != iconRank) {
            notificationManager.cancel(NOTIFICATION_ID)
            Log.i(WifiAutoEnableController.TAG, "Wi-Fi status icon reposition: rank $previousRank -> $iconRank ($reason)")
        }

        val isConnected = isWifiConnectedToNetwork(context)
        val notification = buildStatusIconNotification(context, hide = false, rank = iconRank)
        notificationManager.notify(NOTIFICATION_ID, notification)
        lastNotifiedRank = iconRank
        Log.i(WifiAutoEnableController.TAG, "Wi-Fi status icon shown (connected=$isConnected, rank=$iconRank): $reason")
    }

    fun cancelStatusIcon(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
        lastNotifiedRank = null
        Log.i(WifiAutoEnableController.TAG, "Wi-Fi status icon cancelled")
    }

    fun buildServiceNotification(context: Context): Notification {
        ensureServiceChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }

        val pendingIntent = PendingIntent.getActivity(context, 1, intent, flags)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, SERVICE_CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }

        return builder
            .setSmallIcon(R.drawable.ic_notification_wifi)
            .setContentTitle(context.getString(R.string.wifi_service_notification_title))
            .setContentText(context.getString(R.string.wifi_service_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(Notification.PRIORITY_MIN)
            .build()
    }

    private fun buildStatusIconNotification(context: Context, hide: Boolean, rank: Int): Notification {
        ensureStatusIconChannel(context)

        val iconResId = getWifiIconResId(context)
        val wifiIcon = Icon.createWithResource(context, iconResId)

        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(iconResId)
            .setContentTitle(context.getString(R.string.wifi_status_icon_title))
            .setContentText(context.getString(R.string.wifi_status_icon_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(Notification.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        notification.extras.putBoolean(FLAG_STATUS_ICON, true)
        notification.extras.putInt(FLAG_STATUS_ICON_ID, 14)
        notification.extras.putString(FLAG_STATUS_ICON_DESCRIBE, "StatusIcon_Wifi")
        notification.extras.putParcelable(FLAG_STATUS_ICON_ICON, wifiIcon)
        notification.extras.putParcelable(FLAG_STATUS_ICON_PRESSED_ICON, wifiIcon)
        notification.extras.putBoolean(FLAG_STATUS_ICON_HIDE, hide)
        notification.extras.putInt(FLAG_STATUS_ICON_RANK, rank)
        notification.extras.putInt(FLAG_STATUS_ICON_SPACE_X, 1)
        notification.extras.putBoolean(FLAG_STATUS_ICON_IS_PICK_ON, false)
        notification.extras.putInt(FLAG_STATUS_ICON_SPECIFIC_WIDTH, 0)

        return notification
    }

    private fun ensureStatusIconChannel(context: Context) {
        ensureChannel(
            context,
            CHANNEL_ID,
            context.getString(R.string.wifi_status_icon_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
    }

    private fun ensureServiceChannel(context: Context) {
        ensureChannel(
            context,
            SERVICE_CHANNEL_ID,
            context.getString(R.string.wifi_service_channel),
            NotificationManager.IMPORTANCE_MIN,
        )
    }

    private fun ensureChannel(context: Context, channelId: String, name: String, importance: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (notificationManager == null) {
            Log.e(WifiAutoEnableController.TAG, "NotificationManager is null")
            return
        }

        val channel = NotificationChannel(channelId, name, importance).apply {
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun isWifiConnectedToNetwork(context: Context): Boolean {
        val appContext = context.applicationContext
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (connectivityManager != null) {
            val network = connectivityManager.activeNetwork
            val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }

            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                return true
            }
        }

        val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return false

        if (wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            return false
        }

        val wifiInfo = wifiManager.connectionInfo ?: return false
        if (wifiInfo.networkId == -1) {
            return false
        }

        return wifiInfo.supplicantState == SupplicantState.COMPLETED
    }

    private fun getWifiIconResId(context: Context): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        if (wifiManager == null) {
            Log.w(WifiAutoEnableController.TAG, "WifiManager is null, use Wi-Fi off icon")
            return R.drawable.ic_wifi_off
        }

        if (wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            Log.i(WifiAutoEnableController.TAG, "Wi-Fi is not enabled, use Wi-Fi off icon")
            return R.drawable.ic_wifi_off
        }

        if (!isWifiConnectedToNetwork(context)) {
            Log.i(WifiAutoEnableController.TAG, "Wi-Fi enabled but not connected, use dimmed icon")
            return R.drawable.ic_wifi_no_connection
        }

        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo == null) {
            Log.i(WifiAutoEnableController.TAG, "WifiInfo is null, use Wi-Fi level 4 icon")
            return R.drawable.ic_wifi_4
        }

        val rssi = wifiInfo.rssi
        if (rssi <= -100 || rssi >= 0) {
            Log.i(WifiAutoEnableController.TAG, "Wi-Fi RSSI invalid: $rssi, use dimmed icon")
            return R.drawable.ic_wifi_no_connection
        }

        val level = WifiManager.calculateSignalLevel(rssi, 5)
        Log.i(WifiAutoEnableController.TAG, "Wi-Fi connected, RSSI: $rssi, level: $level")

        return when (level) {
            0 -> R.drawable.ic_wifi_0
            1 -> R.drawable.ic_wifi_1
            2 -> R.drawable.ic_wifi_2
            3 -> R.drawable.ic_wifi_3
            else -> R.drawable.ic_wifi_4
        }
    }
}
