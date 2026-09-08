package com.example.ex2_phone.data.network

import java.io.InputStream
import java.io.OutputStream

/** Ống dẫn vật lý tới head unit. Nội dung NDJSON giống hệt nhau ở mọi transport. */
enum class TransportKind { BLUETOOTH, WIFI }

/** Cặp stream duplex đã mở. Framing NDJSON do [CarRepository] lo. */
class CarStreams(val input: InputStream, val output: OutputStream)

/**
 * Ống dẫn tới head unit — chỉ lo mở/đóng kết nối vật lý, không biết gì về JSON.
 *
 * Nhờ lớp này mà `CarRepository` dùng chung y hệt logic cho TCP WiFi
 * ([TcpCarLink]) và Bluetooth SPP ([BtSppCarLink]).
 */
interface CarLink {
    val kind: TransportKind

    /** Nhãn hiển thị cho UI: "Bluetooth Geely EX2" / "WiFi 192.168.43.1". */
    val label: String

    /** Mở kết nối. Ném exception nếu thất bại — caller reconnect. */
    suspend fun open(): CarStreams

    /**
     * Đóng kết nối.
     *
     * Phải **đóng socket gốc**, không chỉ cancel coroutine: `readLine()` trên socket là
     * blocking IO, cancel không cắt được — chỉ đóng socket mới bung ra.
     */
    fun close()
}
