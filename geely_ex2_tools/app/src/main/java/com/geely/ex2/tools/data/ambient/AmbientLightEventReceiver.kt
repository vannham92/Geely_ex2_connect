package com.geely.ex2.tools.data.ambient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AmbientLightEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "null"
        Log.i(AmbientLightScheduleController.TAG, "Ambient event receiver action: $action")

        if (AmbientLightWakeEvents.isWakeAction(action)) {
            AmbientLightWakeEvents.onManifestWake(context, "receiver: $action")
        }
    }
}
