package com.geely.ex2.tools.data.temperature

import android.app.Activity
import android.os.Bundle
import android.util.Log

class TemperatureLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemperatureAppStarter.startServiceIfEnabled(this, "TemperatureLaunchActivity")
        TemperatureAppStarter.notifyStatusIconIfEnabled(this, "TemperatureLaunchActivity")
        Log.i(TAG, "TemperatureLaunchActivity requested start and finished")
        finish()
    }

    companion object {
        private const val TAG = "GeelyToolsTemperature"
    }
}
