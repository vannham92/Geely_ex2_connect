package com.geely.ex2.tools.data.speed

import com.geely.ex2.tools.data.kv.AppKv
import com.geely.ex2.tools.data.vhal.SpeedSample
import com.geely.ex2.tools.data.vhal.VhalConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * Latest speed sample from [SpeedStatusService] / one-shot notify.
 * Backed by MULTI_PROCESS MMKV so the UI process can read samples written in `:core`.
 */
object SpeedSampleStore {
    private const val KEY_PRESENT = "speed_sample.present"
    private const val KEY_SPEED_KMH = "speed_sample.speed_kmh"
    private const val KEY_AVAILABLE = "speed_sample.available"
    private const val KEY_SOURCE = "speed_sample.source"
    private const val KEY_DETAILS = "speed_sample.details"

    fun publish(sample: SpeedSample) {
        val kv = AppKv.default()
        kv.encode(KEY_SPEED_KMH, sample.speedKmh)
        kv.encode(KEY_AVAILABLE, sample.isAvailable)
        kv.encode(KEY_SOURCE, sample.source)
        kv.encode(KEY_DETAILS, sample.details)
        kv.encode(KEY_PRESENT, true)
    }

    fun clear() {
        val kv = AppKv.default()
        kv.encode(KEY_PRESENT, false)
        kv.removeValueForKey(KEY_SPEED_KMH)
        kv.removeValueForKey(KEY_AVAILABLE)
        kv.removeValueForKey(KEY_SOURCE)
        kv.removeValueForKey(KEY_DETAILS)
    }

    fun latest(): SpeedSample? {
        val kv = AppKv.default()
        if (!kv.decodeBool(KEY_PRESENT, false)) return null
        return SpeedSample(
            speedKmh = kv.decodeFloat(KEY_SPEED_KMH, 0f),
            isAvailable = kv.decodeBool(KEY_AVAILABLE, false),
            source = kv.decodeString(KEY_SOURCE, "") ?: "",
            details = kv.decodeString(KEY_DETAILS, "") ?: "",
        )
    }

    /** Polls MMKV so UI in the default process sees `:core` service updates. */
    fun observe(pollIntervalMs: Long = VhalConstants.SPEED_POLL_INTERVAL_MS / 2): Flow<SpeedSample?> =
        flow {
            while (true) {
                emit(latest())
                delay(pollIntervalMs.coerceAtLeast(250L))
            }
        }.distinctUntilChanged()
}
