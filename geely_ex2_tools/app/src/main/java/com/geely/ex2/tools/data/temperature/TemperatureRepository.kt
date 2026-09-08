package com.geely.ex2.tools.data.temperature

import android.content.Context

class TemperatureRepository(private val context: Context) {
    fun isEnabled(): Boolean = TemperatureSettings.isEnabled(context)

    fun setEnabled(enabled: Boolean) {
        TemperatureSettings.setEnabled(context, enabled)
    }

    fun getStatusIconRank(): Int = TemperatureSettings.getStatusIconRank(context)

    fun stepStatusIconRank(delta: Int): Int = TemperatureSettings.stepStatusIconRank(context, delta)

    fun startStatusServiceIfEnabled(reason: String) {
        TemperatureAppStarter.startServiceIfEnabled(context, reason)
    }

    fun stopStatusService(reason: String) {
        TemperatureAppStarter.stopService(context, reason)
    }

    fun cancelStatusIcon() {
        TemperatureAppStarter.cancelStatusIcon(context)
    }

    fun notifyStatusIconIfEnabled(reason: String, rank: Int? = null) {
        TemperatureAppStarter.notifyStatusIconIfEnabled(context, reason, rank)
    }

    fun readTemperature(): TemperatureReader.Result {
        val reader = TemperatureReader(context)
        return try {
            reader.readTemperature()
        } finally {
            reader.close()
        }
    }
}
