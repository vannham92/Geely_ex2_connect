package com.example.ex2_phone.data.network

import android.content.Context

/**
 * Ống dẫn tới xe. Người dùng phải chọn hẳn một cái — không có chế độ tự dò.
 *
 * [BLUETOOTH] là mặc định: bond đã có sẵn từ pairing handsfree → nối được mà user không phải
 * bật WiFi trên xe.
 */
enum class TransportMode { BLUETOOTH, WIFI }

/**
 * Lưu lựa chọn transport + head unit Bluetooth đã chọn.
 *
 * Dùng chung `SharedPreferences("app_prefs")` với `ip_address` và `car_color` (xem `MainApp`).
 */
class TransportPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /** Giá trị `"AUTO"` cũ còn sót trong prefs sẽ không parse được → rơi về [TransportMode.BLUETOOTH]. */
    var mode: TransportMode
        get() = prefs.getString(KEY_MODE, null)
            ?.let { name -> runCatching { TransportMode.valueOf(name) }.getOrNull() }
            ?: TransportMode.BLUETOOTH
        set(value) {
            prefs.edit().putString(KEY_MODE, value.name).apply()
        }

    /** Head unit Bluetooth user đã chọn; `null` = chưa chọn → không dựng được link BT. */
    var btDevice: BondedDevice?
        get() {
            val address = prefs.getString(KEY_BT_ADDRESS, null) ?: return null
            return BondedDevice(prefs.getString(KEY_BT_NAME, null) ?: address, address)
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_BT_ADDRESS)
                    remove(KEY_BT_NAME)
                } else {
                    putString(KEY_BT_ADDRESS, value.address)
                    putString(KEY_BT_NAME, value.name)
                }
            }.apply()
        }

    var carIp: String
        get() = prefs.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) {
            prefs.edit().putString(KEY_IP, value).apply()
        }

    /**
     * Cho phép chạy nền: khi head unit ở [btDevice] kết nối Bluetooth, `CarBluetoothReceiver`
     * bật `CarConnectionService` để nối cả khi app đang đóng. Vô nghĩa khi [btDevice] là `null`.
     */
    var autoBackground: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKGROUND, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_BACKGROUND, value).apply()
        }

    /**
     * Đã tự động xin quyền thông báo lần đầu vào Cài đặt hay chưa. Chỉ hỏi một lần — từ chối rồi
     * thì để user tự bấm mục "Thông báo", không dội dialog mỗi lần mở Cài đặt.
     */
    var notificationAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ASKED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIFICATION_ASKED, value).apply()
        }

    companion object {
        private const val KEY_MODE = "transport_mode"
        private const val KEY_BT_ADDRESS = "bt_device_address"
        private const val KEY_BT_NAME = "bt_device_name"
        private const val KEY_IP = "ip_address"
        private const val KEY_AUTO_BACKGROUND = "auto_background"
        private const val KEY_NOTIFICATION_ASKED = "notification_asked"
        const val DEFAULT_IP = "10.77.86.7"
    }
}
