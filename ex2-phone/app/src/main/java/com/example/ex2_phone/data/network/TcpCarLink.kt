package com.example.ex2_phone.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Transport WiFi: TCP raw tới `TcpServerLink` của head unit (port [PORT]).
 *
 * Tách ra từ `CarRepository.connect()` — tham số socket giữ nguyên 1:1.
 */
class TcpCarLink(private val carIp: String) : CarLink {

    override val kind = TransportKind.WIFI
    override val label = "WiFi $carIp"

    @Volatile
    private var socket: Socket? = null

    override suspend fun open(): CarStreams = withContext(Dispatchers.IO) {
        val s = Socket().apply {
            tcpNoDelay = true
            keepAlive = true
            connect(InetSocketAddress(carIp, PORT), CONNECT_TIMEOUT_MS)
            // Lớp bảo vệ thứ nhất cho half-open TCP. CarRepository còn một idle watchdog
            // dùng chung cho cả 2 transport (Bluetooth không có soTimeout).
            soTimeout = SOCKET_READ_TIMEOUT_MS
        }
        socket = s
        CarStreams(s.getInputStream(), s.getOutputStream())
    }

    override fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    companion object {
        const val PORT = 47800
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val SOCKET_READ_TIMEOUT_MS = 35_000
    }
}
