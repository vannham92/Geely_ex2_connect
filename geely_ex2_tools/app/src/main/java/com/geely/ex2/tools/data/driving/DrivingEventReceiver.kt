package com.geely.ex2.tools.data.driving

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DrivingEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "null"
        Log.i(DrivingModeController.TAG, "Receiver action: $action")

        if (DrivingWakeEvents.isWakeAction(action)) {
            DrivingWakeEvents.onManifestWake(context, "receiver: $action")
        }
    }
}
