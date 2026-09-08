package com.geely.ex2.tools.data.driving

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.DrivingMode
import com.geely.ex2.tools.data.vhal.VhalConstants

object DrivingModeController {
    const val TAG = "GeelyToolsDriving"

    @Volatile
    private var restoreInFlight = false

    @Volatile
    private var restorePending = false

    fun restoreDrivingModeIfNeeded(
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
                    restoreDrivingModeIfNeededSync(context.applicationContext, reason)
                } while (restorePending)
            } finally {
                restoreInFlight = false
                onComplete?.invoke()
            }
        }
    }

    private fun restoreDrivingModeIfNeededSync(context: Context, reason: String) {
        if (!DrivingSettings.isPersistEnabled(context)) {
            Log.i(TAG, "Skip driving mode restore, persist disabled: $reason")
            return
        }

        val savedMode = DrivingSettings.getSavedModeValue(context)
        if (!DrivingMode.isSelectableValue(savedMode)) {
            Log.w(TAG, "Skip driving mode restore, invalid saved mode: 0x${savedMode.toString(16)} ($reason)")
            return
        }

        val repository = DrivingModeRepository(context)
        val current = repository.readDrivingMode()
        if (!current.isAvailable) {
            Log.w(TAG, "Driving mode read unavailable ($reason): ${current.source}")
            return
        }

        if (current.modeValue == savedMode) {
            Log.d(TAG, "Driving mode already saved value 0x${savedMode.toString(16)} ($reason)")
            return
        }

        Log.i(
            TAG,
            "Driving mode mismatch: current=0x${current.modeValue.toString(16)} " +
                "saved=0x${savedMode.toString(16)}, restoring ($reason)",
        )

        repeat(WRITE_ATTEMPTS) { attempt ->
            val writeResult = repository.setDrivingMode(savedMode)
            if (!writeResult.ok) {
                Log.w(
                    TAG,
                    "Driving mode write failed attempt ${attempt + 1}/$WRITE_ATTEMPTS ($reason): ${writeResult.error}",
                )
                return@repeat
            }

            val verifyDelayMs = verifyDelayForAttempt(attempt)
            Thread.sleep(verifyDelayMs)

            val verify = repository.readDrivingMode()
            if (verify.isAvailable && verify.modeValue == savedMode) {
                Log.i(
                    TAG,
                    "Driving mode restored to 0x${savedMode.toString(16)} ($reason) " +
                        "after ${verifyDelayMs}ms",
                )
                return
            }

            Log.d(
                TAG,
                "Driving mode verify attempt ${attempt + 1}/$WRITE_ATTEMPTS ($reason): " +
                    "read 0x${verify.modeValue.toString(16)} after ${verifyDelayMs}ms",
            )
        }

        Log.w(TAG, "Driving mode restore incomplete ($reason)")
    }

    private fun verifyDelayForAttempt(attempt: Int): Long {
        return VhalConstants.DRIVING_RESTORE_VERIFY_BASE_MS +
            VhalConstants.DRIVING_RESTORE_VERIFY_STEP_MS * attempt
    }

    private const val WRITE_ATTEMPTS = 3
}
