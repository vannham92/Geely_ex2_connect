package com.example.ex2_phone.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.ex2_phone.MainActivity
import com.example.ex2_phone.R
import com.example.ex2_phone.data.network.CarConnectionManager
import com.example.ex2_phone.data.network.TransportKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Foreground service giữ kết nối tới xe khi app đã đóng.
 *
 * Không tự dựng kết nối riêng — chỉ [CarConnectionManager.retain] một chỗ giữ, nên chạy song song
 * với UI cũng chỉ có đúng một socket. Vòng đời do [CarBluetoothReceiver] điều khiển: head unit
 * kết nối Bluetooth thì [start], rời tầm thì [stop].
 *
 * API 26+ bắt buộc foreground service phải có notification thường trực — không né được, nên
 * notification này luôn hiện và kiêm luôn phần hiển thị trạng thái + nút Dừng.
 */
class CarConnectionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // startForeground PHẢI chạy trong ~5s kể từ startForegroundService, trước mọi việc khác.
        startForegroundCompat(buildNotification(getString(R.string.car_link_connecting)))
        CarConnectionManager.init(this)
        CarConnectionManager.retain(CarConnectionManager.Owner.SERVICE)
        observeState()
        Log.i(TAG, "Service chạy nền bắt đầu")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.i(TAG, "Người dùng dừng service")
            stopSelf()
            return START_NOT_STICKY
        }
        // START_STICKY: hệ thống kill vì thiếu RAM thì dựng lại (intent null, không sao).
        return START_STICKY
    }

    override fun onDestroy() {
        CarConnectionManager.release(CarConnectionManager.Owner.SERVICE)
        scope.cancel()
        Log.i(TAG, "Service chạy nền dừng")
        super.onDestroy()
    }

    /** Gộp trạng thái thành một dòng cho notification; chỉ bắn lại khi dòng đó đổi. */
    private fun observeState() {
        scope.launch {
            combine(
                CarConnectionManager.connected,
                CarConnectionManager.carStatus,
                CarConnectionManager.activeLink,
            ) { connected, status, link ->
                if (!connected) {
                    getString(R.string.car_link_connecting)
                } else {
                    val transport = when (link?.kind) {
                        TransportKind.BLUETOOTH -> getString(R.string.car_link_bluetooth)
                        TransportKind.WIFI -> getString(R.string.car_link_wifi)
                        null -> ""
                    }
                    val battery = if (status.batteryAvailable) {
                        " · " + getString(R.string.car_link_battery, status.batteryPercent.toInt())
                    } else {
                        ""
                    }
                    getString(R.string.car_link_connected, transport) + battery
                }
            }
                .distinctUntilChanged()
                .collect { text -> notify(buildNotification(text)) }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.car_link_channel_name),
            NotificationManager.IMPORTANCE_LOW,   // LOW = không kêu, không heads-up
        ).apply {
            description = getString(R.string.car_link_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): android.app.Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, CarConnectionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_car_ex2)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, getString(R.string.car_link_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            // API 31+ chặn bật foreground service từ nền ngoài các trường hợp được miễn.
            Log.e(TAG, "startForeground thất bại", e)
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission") // thiếu POST_NOTIFICATIONS chỉ làm notification không hiện
    private fun notify(notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "notify thất bại", e)
        }
    }

    companion object {
        private const val TAG = "CarConnService"
        private const val NOTIF_ID = 1001

        /** Kênh của notification thường trực. `CarViewModel` đọc để biết user có chặn kênh không. */
        const val CHANNEL_ID = "car_link"

        const val ACTION_STOP = "com.example.ex2_phone.action.STOP_CAR_LINK"

        /**
         * Bật service. Gọi được từ nền khi đến từ broadcast Bluetooth (API 31+ miễn trừ hạn chế
         * "bật foreground service từ nền" cho broadcast cần `BLUETOOTH_CONNECT`).
         */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CarConnectionService::class.java),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Không bật được service từ nền", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CarConnectionService::class.java))
        }
    }
}
