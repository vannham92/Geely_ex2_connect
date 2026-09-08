package com.geely.ex2.tools.data.battery

import android.app.Activity
import android.os.Bundle
import android.util.Log

class BatteryLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BatteryAppStarter.startServiceIfEnabled(this, "BatteryLaunchActivity")
        BatteryAppStarter.notifyStatusIconIfEnabled(this, "BatteryLaunchActivity")
        Log.i(TAG, "BatteryLaunchActivity requested start and finished")
        finish()
    }

    companion object {
        private const val TAG = "GeelyToolsBattery"
    }
}
