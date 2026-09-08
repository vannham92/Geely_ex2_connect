package com.geely.ex2.tools

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import com.geely.ex2.tools.data.kv.AppKv
import com.geely.ex2.tools.data.settings.AppLocaleController
import com.geely.ex2.tools.data.statuswidget.StatusWidgetBootstrap

class GeelyEx2ToolsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Required in every process (UI + :core) before settings / sample stores.
        AppKv.init(this)
        AppLocaleController.applyStored(this)
        // Start widgets/restores only from the default process — :core hosts the services.
        if (isDefaultProcess()) {
            StatusWidgetBootstrap.startEnabledWidgets(this, "Application")
        }
    }

    private fun isDefaultProcess(): Boolean {
        val processName = currentProcessName() ?: return true
        return processName == packageName
    }

    private fun currentProcessName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return getProcessName()
        }
        val pid = Process.myPid()
        val am = getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return null
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
    }
}
