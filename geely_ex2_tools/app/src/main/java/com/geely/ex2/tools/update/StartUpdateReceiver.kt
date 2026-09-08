package com.geely.ex2.tools.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Small receiver to start UpdateService after boot or when the package is replaced.
 */
class StartUpdateReceiver : BroadcastReceiver() {
    private val TAG = "StartUpdateReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive ${intent.action}")
        try {
            val svc = Intent(context, UpdateService::class.java)
            context.startService(svc)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start UpdateService", e)
        }
    }
}
