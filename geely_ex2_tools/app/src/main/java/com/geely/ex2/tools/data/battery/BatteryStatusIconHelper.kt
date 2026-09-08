package com.geely.ex2.tools.data.battery

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import com.geely.ex2.tools.MainActivity
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.temperature.TemperatureStatusFont
import com.geely.ex2.tools.data.vhal.BatterySample
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object BatteryStatusIconHelper {
    const val STATUS_NOTIFICATION_ID = 15041
    const val SERVICE_NOTIFICATION_ID = 15042

    @Volatile
    private var lastNotifiedRank: Int? = null

    @Volatile
    private var lastDisplayKey: String? = null

    @Volatile
    private var lastIconAsset: BatteryIconAsset? = null

    private const val STATUS_CHANNEL_ID = "geely_battery_status_icon"
    private const val SERVICE_CHANNEL_ID = "geely_battery_service"

    private const val FLAG_STATUS_ICON = "flag_status_icon_notification"
    private const val FLAG_STATUS_ICON_ID = "flag_status_icon_id"
    private const val FLAG_STATUS_ICON_DESCRIBE = "flag_status_icon_describe"
    private const val FLAG_STATUS_ICON_ICON = "flag_status_icon_icon"
    private const val FLAG_STATUS_ICON_PRESSED_ICON = "flag_status_icon_pressed_icon"
    private const val FLAG_STATUS_ICON_HIDE = "flag_status_icon_hide"
    private const val FLAG_STATUS_ICON_RANK = "flag_status_icon_rank"
    private const val FLAG_STATUS_ICON_SPACE_X = "flag_status_icon_space_x"
    private const val FLAG_STATUS_ICON_IS_PICK_ON = "flag_status_icon_is_pick_on"
    private const val FLAG_STATUS_ICON_SPECIFIC_WIDTH = "flag_status_icon_specific_width"

    fun buildServiceNotification(context: Context): Notification {
        ensureChannel(
            context = context,
            channelId = SERVICE_CHANNEL_ID,
            name = context.getString(R.string.battery_service_channel),
            importance = NotificationManager.IMPORTANCE_MIN,
        )

        val pendingIntent = buildMainActivityIntent(context, requestCode = 6)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, SERVICE_CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }

        return builder
            .setSmallIcon(R.drawable.ic_notification_battery)
            .setContentTitle(context.getString(R.string.battery_service_notification_title))
            .setContentText(context.getString(R.string.battery_service_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(Notification.PRIORITY_MIN)
            .build()
    }

    fun notifyBattery(
        context: Context,
        sample: BatterySample?,
        reason: String,
        rank: Int = BatterySettings.getStatusIconRank(context),
        force: Boolean = false,
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null, reason: $reason")
            return
        }

        val previousRank = lastNotifiedRank
        if (previousRank != null && previousRank != rank) {
            notificationManager.cancel(STATUS_NOTIFICATION_ID)
            Log.i(TAG, "Battery status icon reposition: rank $previousRank -> $rank ($reason)")
        }

        val key = displayKey(sample)
        val cachedAsset = lastIconAsset
        if (!force && key == lastDisplayKey && rank == lastNotifiedRank && cachedAsset != null) {
            Log.d(TAG, "Battery status icon unchanged, skip: $reason")
            return
        }

        ensureChannel(
            context = context,
            channelId = STATUS_CHANNEL_ID,
            name = context.getString(R.string.battery_status_icon_channel),
            importance = NotificationManager.IMPORTANCE_LOW,
        )

        val iconAsset = if (!force && key == lastDisplayKey && cachedAsset != null) {
            cachedAsset
        } else {
            createBatteryIcon(context, sample).also {
                lastIconAsset = it
                lastDisplayKey = key
            }
        }
        val icon = iconAsset.icon
        val text = if (sample?.isAvailable == true) {
            context.getString(R.string.battery_status_icon_text_available, sample.socPercent.toInt())
        } else {
            context.getString(R.string.battery_status_icon_text_unavailable)
        }

        val pendingIntent = buildMainActivityIntent(context, requestCode = 7)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, STATUS_CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(icon)
            .setContentTitle(context.getString(R.string.battery_status_icon_title))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(Notification.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        notification.extras.putBoolean(FLAG_STATUS_ICON, true)
        notification.extras.putInt(FLAG_STATUS_ICON_ID, 17)
        notification.extras.putString(FLAG_STATUS_ICON_DESCRIBE, "StatusIcon_BatterySoc")
        notification.extras.putParcelable(FLAG_STATUS_ICON_ICON, icon)
        notification.extras.putParcelable(FLAG_STATUS_ICON_PRESSED_ICON, icon)
        notification.extras.putBoolean(FLAG_STATUS_ICON_HIDE, false)
        notification.extras.putInt(FLAG_STATUS_ICON_RANK, rank)
        notification.extras.putInt(FLAG_STATUS_ICON_SPACE_X, 1)
        notification.extras.putBoolean(FLAG_STATUS_ICON_IS_PICK_ON, false)
        notification.extras.putInt(FLAG_STATUS_ICON_SPECIFIC_WIDTH, iconAsset.widthPx)

        notificationManager.notify(STATUS_NOTIFICATION_ID, notification)
        lastNotifiedRank = rank
        Log.i(TAG, "Battery status icon notified: $reason, rank=$rank, $text")
    }

    fun cancelStatusIcon(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(STATUS_NOTIFICATION_ID)
        lastNotifiedRank = null
        lastDisplayKey = null
        lastIconAsset = null
    }

    private data class BatteryIconAsset(
        val icon: Icon,
        val widthPx: Int,
    )

    private fun displayKey(sample: BatterySample?): String {
        return if (sample?.isAvailable == true) {
            "${sample.socPercent.toInt()}%"
        } else {
            "?%"
        }
    }

    private fun createBatteryIcon(context: Context, sample: BatterySample?): BatteryIconAsset {
        val numberText = if (sample?.isAvailable == true) {
            "${sample.socPercent.toInt()}%"
        } else {
            "?%"
        }

        val heightPx = dp(context, BASE_ICON_HEIGHT_DP)
        val sizeScale = BatterySettings.ICON_SIZE_PERCENT / 100.0f
        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.LEFT
            typeface = TemperatureStatusFont.getStatusIconTypeface(context)
            textSize = min(
                heightPx * BASE_TEXT_SIZE_RATIO * sizeScale,
                heightPx * MAX_TEXT_HEIGHT_RATIO,
            )
        }

        val contentWidth = numberPaint.measureText(numberText)
        val horizontalPaddingPx = max(
            dp(context, MIN_HORIZONTAL_PADDING_DP).toFloat(),
            heightPx * HORIZONTAL_PADDING_RATIO,
        )
        val widthPx = max(
            heightPx,
            ceil(contentWidth + horizontalPaddingPx * 2).toInt(),
        )

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fontMetrics = numberPaint.fontMetrics
        val baselineY = heightPx / 2.0f - (fontMetrics.ascent + fontMetrics.descent) / 2.0f
        val startX = (widthPx - contentWidth) / 2.0f
        canvas.drawText(numberText, startX, baselineY, numberPaint)

        return BatteryIconAsset(
            icon = Icon.createWithBitmap(bitmap),
            widthPx = widthPx,
        )
    }

    private fun ensureChannel(context: Context, channelId: String, name: String, importance: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null")
            return
        }

        val channel = NotificationChannel(channelId, name, importance).apply {
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildMainActivityIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    private const val TAG = "GeelyToolsBattery"
    private const val BASE_ICON_HEIGHT_DP = 28
    private const val MIN_HORIZONTAL_PADDING_DP = 2
    private const val BASE_TEXT_SIZE_RATIO = 0.72f
    private const val MAX_TEXT_HEIGHT_RATIO = 0.96f
    private const val HORIZONTAL_PADDING_RATIO = 0.10f
}
