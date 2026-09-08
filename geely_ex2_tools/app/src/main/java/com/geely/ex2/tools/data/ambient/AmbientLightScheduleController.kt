package com.geely.ex2.tools.data.ambient

import android.content.Context
import android.util.Log
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.VhalConstants

object AmbientLightScheduleController {
    const val TAG = "GeelyToolsAmbient"

    @Volatile
    private var restoreInFlight = false

    @Volatile
    private var restorePending = false

    fun desiredEnabled(context: Context): Boolean? {
        return when (AmbientLightSettings.getControlMode(context)) {
            AmbientLightControlMode.AUTO -> AmbientLightSchedule.shouldBeEnabledNow(context)
            AmbientLightControlMode.OFF -> false
            AmbientLightControlMode.ON -> true
        }
    }

    fun restoreAmbientLightIfNeeded(
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
                    restoreAmbientLightIfNeededSync(context.applicationContext, reason)
                } while (restorePending)
            } finally {
                restoreInFlight = false
                onComplete?.invoke()
            }
        }
    }

    /** Fire-and-forget; safe from main. */
    fun applyIfNeeded(context: Context, reason: String) {
        val appContext = context.applicationContext
        CarPropertyIo.execute {
            applyIfNeededOnIoThread(appContext, reason)
        }
    }

    /** Blocks until apply finishes. Call only from a background thread. */
    fun applyIfNeededBlocking(context: Context, reason: String): Boolean {
        val appContext = context.applicationContext
        return CarPropertyIo.call {
            applyIfNeededOnIoThread(appContext, reason)
        }
    }

    private fun applyIfNeededOnIoThread(context: Context, reason: String): Boolean {
        val desired = desiredEnabled(context) ?: return false
        return applyDesiredState(context, desired, reason)
    }

    fun syncBackgroundWork(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (AmbientLightSettings.isScheduleEnabled(appContext)) {
            AmbientLightAppStarter.startScheduleService(appContext, reason)
            AmbientLightScheduleAlarm.start(appContext, reason)
        } else {
            AmbientLightScheduleAlarm.cancel(appContext, reason)
            AmbientLightAppStarter.stopScheduleService(appContext, reason)
            applyIfNeeded(appContext, reason)
        }
    }

    private fun restoreAmbientLightIfNeededSync(context: Context, reason: String) {
        if (!AmbientLightSettings.shouldRestoreOnWake(context)) {
            Log.i(TAG, "Skip ambient light restore, restore disabled: $reason")
            return
        }

        val desired = desiredEnabled(context)
        if (desired == null) {
            Log.w(TAG, "Skip ambient light restore, desired state unknown: $reason")
            return
        }

        Log.i(
            TAG,
            "Ambient light restore desired=$desired ($reason), " +
                "mode=${AmbientLightSettings.getControlMode(context)}, " +
                "window ${formatScheduleWindow(context)}",
        )

        val repository = AmbientLightRepository(context)
        val current = repository.readAmbientLight()
        if (!current.isAvailable) {
            Log.w(TAG, "Ambient light read unavailable ($reason): ${current.source}")
        } else if (current.isEnabled == desired) {
            Log.d(TAG, "Ambient light already desired=$desired ($reason)")
            return
        }

        repeat(WRITE_ATTEMPTS) { attempt ->
            val sample = if (attempt == 0 && current.isAvailable) {
                current
            } else {
                repository.readAmbientLight()
            }

            if (!sample.isAvailable) {
                Log.w(
                    TAG,
                    "Ambient light read unavailable attempt ${attempt + 1}/$WRITE_ATTEMPTS ($reason): " +
                        sample.source,
                )
                Thread.sleep(verifyDelayForAttempt(attempt))
                return@repeat
            }

            if (sample.isEnabled == desired) {
                Log.i(TAG, "Ambient light restored to desired=$desired ($reason)")
                return
            }

            val writeResult = repository.setAmbientLightEnabled(desired)
            if (!writeResult.ok) {
                Log.w(
                    TAG,
                    "Ambient light write failed attempt ${attempt + 1}/$WRITE_ATTEMPTS ($reason): " +
                        writeResult.error,
                )
                Thread.sleep(verifyDelayForAttempt(attempt))
                return@repeat
            }

            val verifyDelayMs = verifyDelayForAttempt(attempt)
            Thread.sleep(verifyDelayMs)

            val verify = repository.readAmbientLight()
            if (verify.isAvailable && verify.isEnabled == desired) {
                Log.i(
                    TAG,
                    "Ambient light restored to desired=$desired ($reason) after ${verifyDelayMs}ms",
                )
                return
            }

            Log.d(
                TAG,
                "Ambient light verify attempt ${attempt + 1}/$WRITE_ATTEMPTS ($reason): " +
                    "read enabled=${verify.isEnabled}, want=$desired after ${verifyDelayMs}ms",
            )
        }

        Log.w(TAG, "Ambient light restore incomplete ($reason)")
    }

    private fun applyDesiredState(context: Context, desired: Boolean, reason: String): Boolean {
        val repository = AmbientLightRepository(context)
        val sample = repository.readAmbientLight()
        if (!sample.isAvailable) {
            Log.w(TAG, "Ambient schedule skipped, light unavailable: $reason")
            return false
        }
        if (sample.isEnabled == desired) {
            Log.d(TAG, "Ambient schedule unchanged enabled=$desired: $reason")
            return true
        }
        val result = repository.setAmbientLightEnabled(desired)
        if (result.ok) {
            Log.i(TAG, "Ambient schedule set enabled=$desired: $reason")
        } else {
            Log.w(TAG, "Ambient schedule write failed: ${result.error}, $reason")
        }
        return result.ok
    }

    private fun verifyDelayForAttempt(attempt: Int): Long {
        return VhalConstants.AMBIENT_LIGHT_WAKE_VERIFY_BASE_MS +
            VhalConstants.AMBIENT_LIGHT_WAKE_VERIFY_STEP_MS * attempt
    }

    private fun formatScheduleWindow(context: Context): String {
        return AmbientLightSchedule.formatTime(
            AmbientLightSettings.getStartHour(context),
            AmbientLightSettings.getStartMinute(context),
        ) + " - " + AmbientLightSchedule.formatTime(
            AmbientLightSettings.getEndHour(context),
            AmbientLightSettings.getEndMinute(context),
        )
    }

    private const val WRITE_ATTEMPTS = 6
}
