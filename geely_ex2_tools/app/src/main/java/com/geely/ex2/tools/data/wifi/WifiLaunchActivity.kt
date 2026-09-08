package com.geely.ex2.tools.data.wifi

import android.app.Activity
import android.os.Bundle
import android.util.Log

class WifiLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WifiAppStarter.startStatusService(this, "launch activity")
        Log.i(WifiAutoEnableController.TAG, "LaunchActivity started service and finished")
        finish()
    }
}
