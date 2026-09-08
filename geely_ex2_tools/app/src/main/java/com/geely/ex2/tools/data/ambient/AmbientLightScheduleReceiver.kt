package com.geely.ex2.tools.data.ambient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AmbientLightScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "null"
        if (action != AmbientLightScheduleAlarm.ACTION_TICK) {
            return
        }

        Log.i(AmbientLightScheduleController.TAG, "Ambient schedule receiver action: $action")
        handleScheduleTick(context)
    }

    private fun handleScheduleTick(context: Context) {
        val appContext = context.applicationContext
        if (!AmbientLightSettings.isScheduleEnabled(appContext)) {
            AmbientLightScheduleAlarm.cancel(appContext, "tick while auto disabled")
            AmbientLightAppStarter.stopScheduleService(appContext, "tick while auto disabled")
            return
        }

        val pendingResult = goAsync()
        Thread {
            try {
                AmbientLightScheduleController.applyIfNeededBlocking(appContext, "alarm tick")
                AmbientLightScheduleAlarm.scheduleNext(appContext, "alarm chain")
                AmbientLightAppStarter.startScheduleService(appContext, "alarm tick keepalive")
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
