package com.geely.ex2.tools.data.battery

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log

class BatteryAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            BatteryAppWidgetHelper.update(context, appWidgetManager, appWidgetId)
        }
        Log.i(TAG, "onUpdate: ${appWidgetIds.size} widget(s)")
    }

    override fun onEnabled(context: Context) {
        BatteryAppStarter.startServiceIfEnabled(context, "BatteryAppWidget enabled")
        BatteryAppWidgetHelper.updateAll(context, "widget enabled")
    }

    override fun onDisabled(context: Context) {
        Log.i(TAG, "onDisabled")
    }

    companion object {
        private const val TAG = "GeelyToolsBattery"
    }
}
