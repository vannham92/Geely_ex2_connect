package com.geely.ex2.tools.data.avas

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo

object AvasController {
    const val TAG = "GeelyToolsAvas"

    @Volatile
    private var restoreInFlight = false

    @Volatile
    private var restorePending = false

    fun restoreMuteIfNeeded(
        context: Context,
        reason: String,
        onComplete: (() -> Unit)? = null,
    ) {
        CarPropertyIo.execute {
            if (restoreInFlight) {
                restorePending = true
                onComplete?.invoke()
                return@execute
            }
            restoreInFlight = true
            try {
                do {
                    restorePending = false
                    restoreMuteIfNeededSync(context.applicationContext, reason)
                } while (restorePending)
            } finally {
                restoreInFlight = false
                onComplete?.invoke()
            }
        }
    }

    private fun restoreMuteIfNeededSync(context: Context, reason: String) {
        if (!AvasSettings.isMutedSaved(context)) {
            Log.i(TAG, "Skip AVAS restore, mute not saved: $reason")
            return
        }

        val repository = AvasRepository(context)
        try {
            repeat(AvasConstants.AVAS_RESTORE_WRITE_ATTEMPTS) { attempt ->
                val sample = repository.readAvas()
                if (!sample.isAvailable) {
                    Log.w(TAG, "AVAS read unavailable attempt ${attempt + 1} ($reason): ${sample.source}")
                    Thread.sleep(verifyDelayForAttempt(attempt))
                    return@repeat
                }

                if (sample.mode == AvasConstants.MODE_MUTED) {
                    Log.d(TAG, "AVAS already muted ($reason)")
                    return
                }

                Log.i(TAG, "AVAS restoring mute from mode=${sample.mode} ($reason)")
                val write = repository.setMuted(true)
                if (!write.ok) {
                    Log.w(
                        TAG,
                        "AVAS mute write failed attempt ${attempt + 1}/" +
                            "${AvasConstants.AVAS_RESTORE_WRITE_ATTEMPTS} ($reason): ${write.error}",
                    )
                    Thread.sleep(verifyDelayForAttempt(attempt))
                    return@repeat
                }

                Thread.sleep(verifyDelayForAttempt(attempt))
                val verify = repository.readAvas()
                if (verify.isAvailable && verify.mode == AvasConstants.MODE_MUTED) {
                    Log.i(TAG, "AVAS mute restored ($reason)")
                    return
                }
            }
            Log.w(TAG, "AVAS mute restore incomplete ($reason)")
        } finally {
            repository.close()
        }
    }

    private fun verifyDelayForAttempt(attempt: Int): Long {
        return AvasConstants.AVAS_RESTORE_VERIFY_BASE_MS +
            AvasConstants.AVAS_RESTORE_VERIFY_STEP_MS * attempt
    }
}
