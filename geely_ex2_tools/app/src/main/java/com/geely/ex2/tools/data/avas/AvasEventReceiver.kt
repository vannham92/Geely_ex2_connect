package com.geely.ex2.tools.data.avas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AvasEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "null"
        Log.i(AvasController.TAG, "Receiver action: $action")

        if (AvasWakeEvents.isWakeAction(action)) {
            AvasWakeEvents.onManifestWake(context, "receiver: $action")
        }
    }
}
