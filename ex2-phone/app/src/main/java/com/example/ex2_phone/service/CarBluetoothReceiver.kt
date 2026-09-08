package com.example.ex2_phone.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ex2_phone.data.network.TransportPrefs

/**
 * Đánh thức app khi head unit đã chọn kết nối Bluetooth.
 *
 * `ACL_CONNECTED` bắn ra khi bond handsfree/A2DP của xe bắt tay với điện thoại — tức là user vừa
 * lên xe. Đây là tín hiệu rẻ nhất để biết "xe tới": không phải quét, không phải poll, gần như
 * không tốn pin.
 *
 * Khai báo trong manifest nên vẫn nhận được khi app đang đóng, và sống lại sau khi khởi động máy
 * mà không cần receiver `BOOT_COMPLETED` — miễn là app đã từng được mở và không bị force-stop.
 *
 * Hai broadcast này nằm trong danh sách miễn trừ lệnh cấm implicit broadcast của API 26+, và vì
 * chúng đòi `BLUETOOTH_CONNECT` nên cũng miễn luôn hạn chế "bật foreground service từ nền" của
 * API 31+ — nhờ vậy [CarConnectionService.start] chạy được từ đây.
 */
class CarBluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = TransportPrefs(context)
        if (!prefs.autoBackground) return

        val target = prefs.btDevice?.address ?: return   // chưa chọn head unit → không làm gì
        val address = deviceAddress(intent) ?: return
        if (!address.equals(target, ignoreCase = true)) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.i(TAG, "Head unit $address kết nối → bật service")
                CarConnectionService.start(context)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.i(TAG, "Head unit $address rời tầm → dừng service")
                CarConnectionService.stop(context)
            }
        }
    }

    /** Chỉ đọc địa chỉ MAC — `device.name` mới là thứ đòi `BLUETOOTH_CONNECT`. */
    @Suppress("DEPRECATION") // getParcelableExtra(String, Class) chỉ có từ API 33.
    private fun deviceAddress(intent: Intent): String? =
        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.address

    private companion object {
        const val TAG = "CarBtReceiver"
    }
}
