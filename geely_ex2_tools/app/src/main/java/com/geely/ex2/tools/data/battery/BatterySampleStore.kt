package com.geely.ex2.tools.data.battery

import com.geely.ex2.tools.data.kv.AppKv
import com.geely.ex2.tools.data.vhal.BatterySample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * Latest battery sample from [BatteryStatusService] / one-shot notify.
 * Backed by MULTI_PROCESS MMKV so the UI process can read samples written in `:core`.
 */
object BatterySampleStore {
    private const val KEY_PRESENT = "battery_sample.present"
    private const val KEY_SOC = "battery_sample.soc_percent"
    private const val KEY_AVAILABLE = "battery_sample.available"
    private const val KEY_SOURCE = "battery_sample.source"
    private const val KEY_DETAILS = "battery_sample.details"

    fun publish(sample: BatterySample) {
        val kv = AppKv.default()
        kv.encode(KEY_SOC, sample.socPercent)
        kv.encode(KEY_AVAILABLE, sample.isAvailable)
        kv.encode(KEY_SOURCE, sample.source)
        kv.encode(KEY_DETAILS, sample.details)
        kv.encode(KEY_PRESENT, true)
    }

    fun clear() {
        val kv = AppKv.default()
        kv.encode(KEY_PRESENT, false)
        kv.removeValueForKey(KEY_SOC)
        kv.removeValueForKey(KEY_AVAILABLE)
        kv.removeValueForKey(KEY_SOURCE)
        kv.removeValueForKey(KEY_DETAILS)
    }

    fun latest(): BatterySample? {
        val kv = AppKv.default()
        if (!kv.decodeBool(KEY_PRESENT, false)) return null
        return BatterySample(
            socPercent = kv.decodeFloat(KEY_SOC, 0f),
            isAvailable = kv.decodeBool(KEY_AVAILABLE, false),
            source = kv.decodeString(KEY_SOURCE, "") ?: "",
            details = kv.decodeString(KEY_DETAILS, "") ?: "",
        )
    }

    /** Polls MMKV so UI in the default process sees `:core` service updates. */
    fun observe(pollIntervalMs: Long = 1_000L): Flow<BatterySample?> =
        flow {
            while (true) {
                emit(latest())
                delay(pollIntervalMs.coerceAtLeast(250L))
            }
        }.distinctUntilChanged()
}
