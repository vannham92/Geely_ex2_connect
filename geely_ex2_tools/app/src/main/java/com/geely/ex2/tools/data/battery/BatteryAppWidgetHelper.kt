package com.geely.ex2.tools.data.battery

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.geely.ex2.tools.MainActivity
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.vhal.BatterySample
import com.geely.ex2.tools.data.vhal.VhalBatteryReaderFactory
import com.geely.ex2.tools.navigation.AppRoutes

object BatteryAppWidgetHelper {
    fun updateAll(context: Context, reason: String, sample: BatterySample? = null) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext) ?: return
        val componentName = ComponentName(appContext, BatteryAppWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) {
            return
        }

        val batterySample = sample
            ?: BatterySampleStore.latest()
            ?: readBatterySoc(appContext)
        val views = buildRemoteViews(appContext, batterySample)
        manager.updateAppWidget(componentName, views)
        Log.i(TAG, "Battery app widget updated ($reason): ${formatPercent(appContext, batterySample)}")
    }

    fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val appContext = context.applicationContext
        val sample = readBatterySoc(appContext)
        val views = buildRemoteViews(appContext, sample)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun readBatterySoc(context: Context): BatterySample {
        val reader = VhalBatteryReaderFactory.create(context)
        return try {
            reader.readBatterySoc()
        } finally {
            reader.close()
        }
    }

    private fun buildRemoteViews(context: Context, sample: BatterySample): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery)
        val percentText = formatPercent(context, sample)
        val progress = if (sample.isAvailable) {
            sample.socPercent.toInt().coerceIn(0, 100)
        } else {
            0
        }

        val socColor = batteryColor(sample)
        views.setTextViewText(R.id.widget_battery_percent, percentText)
        views.setTextColor(R.id.widget_battery_percent, socColor)
        views.setInt(R.id.widget_battery_icon, "setColorFilter", socColor)
        views.setProgressBar(R.id.widget_battery_progress, 100, progress, false)
        views.setViewVisibility(
            R.id.widget_battery_progress,
            if (sample.isAvailable) View.VISIBLE else View.INVISIBLE,
        )
        views.setOnClickPendingIntent(R.id.widget_battery_root, buildOpenBatteryPendingIntent(context))
        return views
    }

    // >= 50% green, 21..49% orange, <= 20% red; default dark when unavailable.
    private fun batteryColor(sample: BatterySample): Int {
        if (!sample.isAvailable) {
            return WIDGET_DEFAULT_COLOR
        }
        return when {
            sample.socPercent.toInt() >= 50 -> 0xFF34C759.toInt()
            sample.socPercent.toInt() >= 21 -> 0xFFFF9500.toInt()
            else -> 0xFFFF3B30.toInt()
        }
    }

    private fun formatPercent(context: Context, sample: BatterySample): String {
        if (!sample.isAvailable) {
            return context.getString(R.string.battery_app_widget_unavailable)
        }
        return context.getString(R.string.battery_latest_value, sample.socPercent.toInt())
    }

    private fun buildOpenBatteryPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_START_ROUTE, AppRoutes.BATTERY)
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(context, REQUEST_OPEN_BATTERY, intent, flags)
    }

    const val EXTRA_START_ROUTE = "start_route"
    private const val REQUEST_OPEN_BATTERY = 15043
    private const val WIDGET_DEFAULT_COLOR = 0xFF111318.toInt()
    private const val TAG = "GeelyToolsBattery"
}
