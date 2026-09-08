package com.geely.ex2.tools.data.speed

import android.app.Activity
import android.os.Bundle
import android.util.Log

class SpeedLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeedAppStarter.startServiceIfEnabled(this, "SpeedLaunchActivity")
        SpeedAppStarter.notifyStatusIconIfEnabled(this, "SpeedLaunchActivity")
        Log.i(TAG, "SpeedLaunchActivity requested start and finished")
        finish()
    }

    companion object {
        private const val TAG = "GeelyToolsSpeed"
    }
}
