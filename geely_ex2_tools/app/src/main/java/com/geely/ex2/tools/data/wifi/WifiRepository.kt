package com.geely.ex2.tools.data.wifi

import android.content.Context
import android.net.wifi.WifiManager

class WifiRepository(private val context: Context) {
    fun isAutoEnableEnabled(): Boolean = WifiAutoEnableController.isAutoEnableEnabled(context)

    fun setAutoEnableEnabled(enabled: Boolean) {
        WifiAutoEnableController.setAutoEnableEnabled(context, enabled)
    }

    fun getWifiState(): Int = WifiAutoEnableController.getWifiState(context)

    fun isWifiEnabledOrEnabling(): Boolean = WifiAutoEnableController.isWifiEnabledOrEnabling(context)

    fun getWifiIpAddress(): String? = WifiAutoEnableController.getWifiIpAddress(context)

    fun setWifiEnabledFromUi(enabled: Boolean) {
        if (enabled) {
            WifiAutoEnableController.enableWifiFromUi(context)
        } else {
            WifiAutoEnableController.disableWifiFromUi(context)
        }
    }

    fun toggleWifiFromUi() {
        setWifiEnabledFromUi(!WifiAutoEnableController.isWifiEnabledOrEnabling(context))
    }

    /** Bật/tắt WiFi cho app phone; trả về kết quả thật của setWifiEnabled (false nếu bị từ chối). */
    fun setWifiEnabledReturning(enabled: Boolean): Boolean =
        WifiAutoEnableController.setWifiEnabledReturning(context, enabled)

    fun toggleAutoEnableFromUi() {
        val enabled = WifiAutoEnableController.isAutoEnableEnabled(context)
        WifiAutoEnableController.setAutoEnableEnabled(context, !enabled)
    }

    fun startStatusService(reason: String) {
        WifiAppStarter.startStatusService(context, reason)
    }

    fun stopStatusService(reason: String) {
        WifiAppStarter.stopStatusService(context, reason)
    }

    fun notifyStatusIcon(reason: String, rank: Int? = null) {
        WifiStatusIconHelper.notifyStatusIcon(context, reason, rank)
    }

    fun cancelStatusIcon() {
        WifiStatusIconHelper.cancelStatusIcon(context)
    }

    fun getStatusIconRank(): Int = WifiSettings.getStatusIconRank(context)

    fun stepStatusIconRank(delta: Int): Int = WifiSettings.stepStatusIconRank(context, delta)

    fun wifiStateToLabel(state: Int): String {
        return when (state) {
            WifiManager.WIFI_STATE_DISABLED -> context.getString(com.geely.ex2.tools.R.string.wifi_state_disabled)
            WifiManager.WIFI_STATE_DISABLING -> context.getString(com.geely.ex2.tools.R.string.wifi_state_disabling)
            WifiManager.WIFI_STATE_ENABLED -> context.getString(com.geely.ex2.tools.R.string.wifi_state_enabled)
            WifiManager.WIFI_STATE_ENABLING -> context.getString(com.geely.ex2.tools.R.string.wifi_state_enabling)
            else -> context.getString(com.geely.ex2.tools.R.string.wifi_state_unknown)
        }
    }
}
