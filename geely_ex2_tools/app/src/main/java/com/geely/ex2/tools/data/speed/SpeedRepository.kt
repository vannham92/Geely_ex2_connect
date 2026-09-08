package com.geely.ex2.tools.data.speed

import android.content.Context
import com.geely.ex2.tools.data.vhal.SpeedSample
import kotlinx.coroutines.flow.Flow

class SpeedRepository(private val context: Context) {
    fun isEnabled(): Boolean = SpeedSettings.isEnabled(context)

    fun setEnabled(enabled: Boolean) {
        SpeedSettings.setEnabled(context, enabled)
    }

    fun getStatusIconRank(): Int = SpeedSettings.getStatusIconRank(context)

    fun stepStatusIconRank(delta: Int): Int = SpeedSettings.stepStatusIconRank(context, delta)

    fun startStatusServiceIfEnabled(reason: String) {
        SpeedAppStarter.startServiceIfEnabled(context, reason)
    }

    fun stopStatusService(reason: String) {
        SpeedAppStarter.stopService(context, reason)
    }

    fun notifyStatusIconIfEnabled(reason: String, rank: Int? = null) {
        SpeedAppStarter.notifyStatusIconIfEnabled(context, reason, rank)
    }

    fun cancelStatusIcon() {
        SpeedStatusIconHelper.cancelStatusIcon(context)
        SpeedSampleStore.clear()
    }

    fun observeLatestSample(): Flow<SpeedSample?> = SpeedSampleStore.observe()

    fun latestSample(): SpeedSample? = SpeedSampleStore.latest()
}
