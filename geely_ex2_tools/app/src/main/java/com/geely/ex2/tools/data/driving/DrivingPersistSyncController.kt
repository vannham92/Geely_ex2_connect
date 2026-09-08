package com.geely.ex2.tools.data.driving

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.DrivingMode
import com.geely.ex2.tools.data.vhal.EnergyRegeneration

object DrivingPersistSyncController {
    const val TAG = DrivingModeController.TAG

    @Volatile
    private var syncInFlight = false

    fun syncFromCarIfNeeded(
        context: Context,
        reason: String,
        onComplete: (() -> Unit)? = null,
    ) {
        CarPropertyIo.execute {
            if (syncInFlight) {
                onComplete?.invoke()
                return@execute
            }
            syncInFlight = true
            try {
                syncFromCarIfNeededSync(context.applicationContext, reason)
            } finally {
                syncInFlight = false
                onComplete?.invoke()
            }
        }
    }

    private fun syncFromCarIfNeededSync(context: Context, reason: String) {
        val repository = DrivingModeRepository(context)

        if (DrivingSettings.isPersistEnabled(context)) {
            syncDrivingMode(context, repository, reason)
        }
        if (DrivingSettings.isRegenPersistEnabled(context)) {
            syncRegen(context, repository, reason)
        }
    }

    private fun syncDrivingMode(context: Context, repository: DrivingModeRepository, reason: String) {
        val savedMode = DrivingSettings.getSavedModeValue(context)
        val current = repository.readDrivingMode()
        if (!current.isAvailable) {
            Log.d(TAG, "Driving mode sync skipped, read unavailable ($reason): ${current.source}")
            return
        }
        if (!DrivingMode.isSelectableValue(current.modeValue)) {
            Log.d(
                TAG,
                "Driving mode sync skipped, non-selectable value 0x${current.modeValue.toString(16)} ($reason)",
            )
            return
        }
        if (current.modeValue == savedMode) {
            return
        }

        Log.i(
            TAG,
            "Driving mode sync save: current=0x${current.modeValue.toString(16)} " +
                "saved=0x${savedMode.toString(16)} ($reason)",
        )
        DrivingSettings.setSavedModeValue(context, current.modeValue)
    }

    private fun syncRegen(context: Context, repository: DrivingModeRepository, reason: String) {
        val savedLevel = DrivingSettings.getSavedRegenValue(context)
        val current = repository.readEnergyRegeneration()
        if (!current.isAvailable) {
            Log.d(TAG, "Regen sync skipped, read unavailable ($reason): ${current.source}")
            return
        }
        if (!EnergyRegeneration.isSelectableValue(current.levelValue)) {
            Log.d(
                TAG,
                "Regen sync skipped, non-selectable value 0x${current.levelValue.toString(16)} ($reason)",
            )
            return
        }
        if (current.levelValue == savedLevel) {
            return
        }

        Log.i(
            TAG,
            "Regen sync save: current=0x${current.levelValue.toString(16)} " +
                "saved=0x${savedLevel.toString(16)} ($reason)",
        )
        DrivingSettings.setSavedRegenValue(context, current.levelValue)
    }
}
