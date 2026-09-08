package com.geely.ex2.tools.data.driving

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.EnergyRegeneration
import com.geely.ex2.tools.data.vhal.VhalConstants

object EnergyRegenController {
    const val TAG = DrivingModeController.TAG

    @Volatile
    private var restoreInFlight = false

    @Volatile
    private var restorePending = false

    fun restoreEnergyRegenerationIfNeeded(
        context: Context,
        reason: String,
        onComplete: (() -> Unit)? = null,
    ) {
        CarPropertyIo.execute {
            if (restoreInFlight) {
                restorePending = true
                return@execute
            }
            restoreInFlight = true
            try {
                do {
                    restorePending = false
                    restoreEnergyRegenerationIfNeededSync(context.applicationContext, reason)
                } while (restorePending)
            } finally {
                restoreInFlight = false
                onComplete?.invoke()
            }
        }
    }

    private fun restoreEnergyRegenerationIfNeededSync(context: Context, reason: String) {
        if (!DrivingSettings.isRegenPersistEnabled(context)) {
            Log.i(TAG, "Skip regen restore, persist disabled: $reason")
            return
        }

        val savedLevel = DrivingSettings.getSavedRegenValue(context)
        if (!EnergyRegeneration.isSelectableValue(savedLevel)) {
            Log.w(TAG, "Skip regen restore, invalid saved level: 0x${savedLevel.toString(16)} ($reason)")
            return
        }

        val repository = DrivingModeRepository(context)
        val current = repository.readEnergyRegeneration()
        if (!current.isAvailable) {
            Log.w(TAG, "Regen read unavailable ($reason): ${current.source}")
            return
        }

        if (current.levelValue == savedLevel) {
            Log.d(TAG, "Regen already saved value 0x${savedLevel.toString(16)} ($reason)")
            return
        }

        Log.i(
            TAG,
            "Regen mismatch: current=0x${current.levelValue.toString(16)} " +
                "saved=0x${savedLevel.toString(16)}, restoring ($reason)",
        )

        repeat(WRITE_ATTEMPTS) { attempt ->
            val writeResult = repository.setEnergyRegeneration(savedLevel)
            if (!writeResult.ok) {
                Log.w(
                    TAG,
                    "Regen write failed attempt ${attempt + 1}/$WRITE_ATTEMPTS ($reason): ${writeResult.error}",
                )
                return@repeat
            }

            val verifyDelayMs = verifyDelayForAttempt(attempt)
            Thread.sleep(verifyDelayMs)

            val verify = repository.readEnergyRegeneration()
            if (verify.isAvailable && verify.levelValue == savedLevel) {
                Log.i(
                    TAG,
                    "Regen restored to 0x${savedLevel.toString(16)} ($reason) " +
                        "after ${verifyDelayMs}ms",
                )
                return
            }

            Log.d(
                TAG,
                "Regen verify attempt ${attempt + 1}/$WRITE_ATTEMPTS ($reason): " +
                    "read 0x${verify.levelValue.toString(16)} after ${verifyDelayMs}ms",
            )
        }

        Log.w(TAG, "Regen restore incomplete ($reason)")
    }

    private fun verifyDelayForAttempt(attempt: Int): Long {
        return VhalConstants.DRIVING_RESTORE_VERIFY_BASE_MS +
            VhalConstants.DRIVING_RESTORE_VERIFY_STEP_MS * attempt
    }

    private const val WRITE_ATTEMPTS = 3
}
