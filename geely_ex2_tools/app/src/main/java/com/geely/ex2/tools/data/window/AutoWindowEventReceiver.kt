package com.geely.ex2.tools.data.window

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AutoWindowEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "null"
        Log.i(AutoWindowController.TAG, "Receiver action: $action")
        AutoWindowAppStarter.startServiceIfEnabled(context, "receiver: $action")
    }
}
