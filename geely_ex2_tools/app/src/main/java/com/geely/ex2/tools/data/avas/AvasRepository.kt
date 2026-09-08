package com.geely.ex2.tools.data.avas

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo

class AvasRepository(context: Context) {
    private val appContext = context.applicationContext
    private val applier = AvasMuteApplier(appContext)
    private val volumeApplier = AvasVolumeApplier(appContext)

    fun isMutedSaved(): Boolean = AvasSettings.isMutedSaved(appContext)

    fun getLastActiveMode(): Int = AvasSettings.getLastActiveMode(appContext)

    fun readAvas(): AvasSample {
        val sample = applier.read()
        if (sample.isAvailable && sample.mode > AvasConstants.MODE_MUTED) {
            AvasSettings.setLastActiveMode(appContext, sample.mode)
        }
        return sample
    }

    fun setMuted(muted: Boolean): AvasWriteResult = CarPropertyIo.call {
        val lastActive = AvasSettings.getLastActiveMode(appContext)
        val result = applier.setMuted(muted, lastActive)
        if (result.ok) {
            AvasSettings.setMutedSaved(appContext, muted)
            Log.i(TAG, "AVAS mute=$muted ok; ${result.details.take(200)}")
        } else {
            Log.w(TAG, "AVAS mute=$muted failed: ${result.error}; ${result.details.take(200)}")
        }
        result
    }

    fun getLastVolume(): Int = AvasSettings.getLastVolume(appContext)

    fun readVolume(): AvasVolumeSample = CarPropertyIo.call {
        val sample = volumeApplier.read()
        if (sample.isAvailable) {
            AvasSettings.setLastVolume(appContext, sample.volume)
        }
        sample
    }

    fun setVolume(level: Int): AvasWriteResult = CarPropertyIo.call {
        val result = volumeApplier.setVolume(level)
        if (result.ok) {
            AvasSettings.setLastVolume(appContext, level)
            Log.i(TAG, "AVAS volume=$level ok; ${result.details.take(200)}")
        } else {
            Log.w(TAG, "AVAS volume=$level failed: ${result.error}; ${result.details.take(200)}")
        }
        result
    }

    fun restoreMuteIfNeeded(reason: String) {
        AvasController.restoreMuteIfNeeded(appContext, reason)
    }

    fun startRestoreService(reason: String) {
        AvasAppStarter.startRestoreServiceIfEnabled(appContext, reason)
    }

    fun stopRestoreService(reason: String) {
        AvasAppStarter.stopRestoreService(appContext, reason)
    }

    fun close() {
        // Stateless applier; nothing to close.
    }

    companion object {
        private const val TAG = "GeelyToolsAvas"
    }
}
