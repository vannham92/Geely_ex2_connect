package com.geely.ex2.tools.data.battery

import android.content.Context
import com.geely.ex2.tools.data.vhal.BatterySample
import com.geely.ex2.tools.data.vhal.VhalBatteryReaderFactory
import kotlinx.coroutines.flow.Flow

class BatteryRepository(private val context: Context) {
    fun isEnabled(): Boolean = BatterySettings.isEnabled(context)

    fun setEnabled(enabled: Boolean) {
        BatterySettings.setEnabled(context, enabled)
    }

    fun getStatusIconRank(): Int = BatterySettings.getStatusIconRank(context)

    fun stepStatusIconRank(delta: Int): Int = BatterySettings.stepStatusIconRank(context, delta)

    fun startStatusServiceIfEnabled(reason: String) {
        BatteryAppStarter.startServiceIfEnabled(context, reason)
    }

    fun stopStatusService(reason: String) {
        BatteryAppStarter.stopService(context, reason)
    }

    fun notifyStatusIconIfEnabled(reason: String, rank: Int? = null) {
        BatteryAppStarter.notifyStatusIconIfEnabled(context, reason, rank)
    }

    fun cancelStatusIcon() {
        BatteryStatusIconHelper.cancelStatusIcon(context)
        BatterySampleStore.clear()
    }

    fun observeLatestSample(): Flow<BatterySample?> = BatterySampleStore.observe()

    fun latestSample(): BatterySample? = BatterySampleStore.latest()

    /** Direct VHAL read for the in-app value (independent of the status-icon service). */
    fun readBatterySocDirect(): BatterySample {
        val reader = VhalBatteryReaderFactory.create(context)
        return try {
            reader.readBatterySoc()
        } finally {
            reader.close()
        }
    }
}
