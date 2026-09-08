package com.example.ex2_phone.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StartUpdateReceiver : BroadcastReceiver() {
    private val TAG = "PhoneStartUpdateReceiver"
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
