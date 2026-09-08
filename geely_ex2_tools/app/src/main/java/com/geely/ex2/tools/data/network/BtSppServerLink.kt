package com.geely.ex2.tools.data.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Transport Bluetooth: RFCOMM (SPP) server trên SDP UUID [SERVICE_UUID].
 *
 * Vì sao cần: TCP đòi phone và head unit cùng mạng WiFi — user phải bật WiFi trên xe mỗi lần
 * lên xe. Bluetooth thì bond đã có sẵn từ pairing handsfree/A2DP, app chỉ mở thêm một kênh SPP
 * riêng trên bond đó → nối tự động, không thao tác.
 *
 * Đã verify trên xe thật (2026-08-02): HU Geely EX2 cho app bên thứ 3 mở RFCOMM server socket,
 * `listenUsingRfcommWithServiceRecord` secure chạy được, không cần bản insecure.
 *
 * Bluetooth có thể đang tắt lúc boot rồi bật sau → vòng ngoài retry mỗi [RETRY_DELAY_MS].
 * Server socket giữ mở qua nhiều lượt client; `accept()` gọi lại được nhiều lần.
 */
class BtSppServerLink(private val context: Context) : CarServerLink {

    override val transport = CarTransport.BLUETOOTH

    @Volatile
    private var serverSocket: BluetoothServerSocket? = null

    @Volatile
    private var stopped = false

    @SuppressLint("MissingPermission") // BLUETOOTH_CONNECT khai báo trong manifest; SecurityException bắt bên dưới
    override suspend fun accept(onClient: suspend (CarLineChannel) -> Unit) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null) {
            Log.w(TAG, "Không có BluetoothAdapter — bỏ transport Bluetooth")
            return
        }

        while (!stopped) {
            if (!adapter.isEnabled) {
                Log.i(TAG, "Bluetooth đang tắt — thử lại sau ${RETRY_DELAY_MS}ms")
                delay(RETRY_DELAY_MS)
                continue
            }

            val server = try {
                adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
            } catch (e: SecurityException) {
                // Quyền sẽ không tự xuất hiện — dừng hẳn thay vì spam log.
                Log.e(TAG, "Thiếu BLUETOOTH_CONNECT — bỏ transport Bluetooth", e)
                return
            } catch (e: Exception) {
                Log.w(TAG, "listenUsingRfcomm thất bại — thử lại sau ${RETRY_DELAY_MS}ms", e)
                delay(RETRY_DELAY_MS)
                continue
            }
            serverSocket = server
            Log.i(TAG, "SPP listening — name=$SERVICE_NAME uuid=$SERVICE_UUID")

            while (!stopped) {
                val socket = try {
                    server.accept()
                } catch (e: Exception) {
                    if (!stopped) Log.w(TAG, "accept() lỗi — mở lại server socket", e)
                    break
                }
                val peer = try {
                    "${socket.remoteDevice?.name} / ${socket.remoteDevice?.address}"
                } catch (_: Exception) {
                    "?"
                }
                val peerLabel = try {
                    socket.remoteDevice?.name ?: socket.remoteDevice?.address ?: "?"
                } catch (_: Exception) {
                    "?"
                }
                // Một client hỏng không được giết cả accept loop.
                try {
                    val channel = StreamLineChannel(
                        transport = CarTransport.BLUETOOTH,
                        peer = peer,
                        peerLabel = peerLabel,
                        input = socket.inputStream,
                        output = try { socket.outputStream } catch (_: Exception) { null },
                        closeSocket = { socket.close() },
                    )
                    onClient(channel)
                } catch (e: Exception) {
                    Log.w(TAG, "Bỏ client SPP lỗi: $peer", e)
                    try { socket.close() } catch (_: Exception) { }
                }
            }

            try {
                server.close()
            } catch (_: Exception) {
            }
            serverSocket = null
            if (!stopped) delay(RETRY_DELAY_MS)
        }
        Log.i(TAG, "SPP accept loop kết thúc")
    }

    override fun stop() {
        stopped = true
        // Đóng socket để bung accept() đang block — coroutine cancel không cắt được blocking IO.
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    companion object {
        private const val TAG = "BtSppServerLink"

        /** Tên quảng bá trong SDP record. */
        const val SERVICE_NAME = "GeelyEX2-CarSync"

        /** PHẢI khớp `BtSppCarLink.SERVICE_UUID` bên app phone. */
        val SERVICE_UUID: UUID = UUID.fromString("6f1e2a00-47b8-4c1e-9d3a-c0ffee000001")

        private const val RETRY_DELAY_MS = 30_000L
    }
}
