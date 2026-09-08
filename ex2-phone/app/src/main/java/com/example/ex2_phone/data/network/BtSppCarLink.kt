package com.example.ex2_phone.data.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/** Thiết bị Bluetooth đã pair — dùng cho danh sách chọn head unit trong Cài đặt. */
data class BondedDevice(val name: String, val address: String)

/**
 * Transport Bluetooth: RFCOMM (SPP) tới `BtSppServerLink` của head unit.
 *
 * Vì sao dùng: TCP đòi phone và xe cùng mạng WiFi — phải bật WiFi trên xe mỗi lần lên xe.
 * Bluetooth thì bond đã có sẵn từ pairing handsfree/A2DP, app chỉ mở thêm một kênh SPP riêng
 * trên bond đó → nối tự động, không thao tác.
 */
class BtSppCarLink(
    private val context: Context,
    private val deviceAddress: String,
    deviceName: String,
) : CarLink {

    override val kind = TransportKind.BLUETOOTH
    override val label = "Bluetooth $deviceName"

    @Volatile
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission") // caller đảm bảo đã có BLUETOOTH_CONNECT
    override suspend fun open(): CarStreams = withContext(Dispatchers.IO) {
        val adapter = adapterOrNull(context) ?: throw IOException("Không có BluetoothAdapter")
        if (!adapter.isEnabled) throw IOException("Bluetooth đang tắt")

        // Discovery đang chạy làm connect chậm hoặc fail.
        runCatching { adapter.cancelDiscovery() }

        val device = adapter.getRemoteDevice(deviceAddress)
        val s = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
        socket = s

        // connect() là blocking IO — coroutine cancel KHÔNG cắt được.
        // Watchdog đóng socket để bung nó ra bằng IOException.
        val watchdog = launch {
            delay(CONNECT_TIMEOUT_MS)
            Log.w(TAG, "connect() quá ${CONNECT_TIMEOUT_MS}ms — đóng socket")
            runCatching { s.close() }
        }
        try {
            s.connect()
        } finally {
            watchdog.cancel()
        }

        CarStreams(s.inputStream, s.outputStream)
    }

    override fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    companion object {
        private const val TAG = "BtSppCarLink"

        /** PHẢI khớp `BtSppServerLink.SERVICE_UUID` bên head unit. */
        val SERVICE_UUID: UUID = UUID.fromString("6f1e2a00-47b8-4c1e-9d3a-c0ffee000001")

        private const val CONNECT_TIMEOUT_MS = 12_000L

        fun adapterOrNull(context: Context): BluetoothAdapter? =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

        /** Danh sách thiết bị đã pair. Rỗng nếu thiếu quyền hoặc không có adapter. */
        @SuppressLint("MissingPermission") // caller kiểm tra BLUETOOTH_CONNECT trước
        fun bondedDevices(context: Context): List<BondedDevice> {
            val adapter = adapterOrNull(context) ?: return emptyList()
            return runCatching {
                adapter.bondedDevices.orEmpty()
                    .map { BondedDevice(it.name ?: "(không tên)", it.address) }
                    .sortedBy { it.name }
            }.getOrElse {
                Log.w(TAG, "Đọc bondedDevices thất bại", it)
                emptyList()
            }
        }
    }
}
