package com.geely.ex2.tools.data.network

import java.io.InputStream
import java.io.OutputStream

/** Ống dẫn vật lý tới app điện thoại. Nội dung NDJSON giống hệt nhau ở mọi transport. */
enum class CarTransport { WIFI, BLUETOOTH }

/** Một phone đang nối vào head unit — snapshot bất biến cho UI. */
data class CarLinkPeer(val transport: CarTransport, val label: String)

/**
 * Một kết nối phone đang mở, đã đóng khung theo dòng.
 *
 * Framing NDJSON (1 JSON/dòng + `\n`) và toàn bộ nội dung do [CarTcpServer] lo — channel chỉ
 * đọc/ghi dòng thô. Nhờ vậy TCP và Bluetooth SPP dùng chung y hệt business logic.
 */
interface CarLineChannel {
    val transport: CarTransport

    /** Mô tả đầu kia để log: địa chỉ IP hoặc tên/MAC thiết bị Bluetooth. */
    val peer: String

    /**
     * Tên ngắn để hiển thị cho người dùng (toast): tên thiết bị Bluetooth hoặc IP phone.
     * Khác [peer] ở chỗ bỏ port / MAC — [peer] vẫn là thứ dùng để log.
     */
    val peerLabel: String get() = peer

    /** Đọc 1 dòng, block tới khi có. `null` = đầu kia đóng hoặc lỗi. */
    fun readLine(): String?

    /** Ghi 1 dòng. Trả `false` nếu kết nối hỏng (caller loại channel khỏi danh sách). */
    fun trySend(line: String): Boolean

    /** Đóng — phải bung được [readLine] đang block. */
    fun close()
}

/**
 * [CarLineChannel] chạy trên cặp stream bất kỳ (socket TCP hoặc [android.bluetooth.BluetoothSocket]).
 *
 * [closeSocket] phải đóng socket gốc chứ không chỉ stream — đóng socket là cách duy nhất bung
 * `readLine()` đang block, vì blocking IO không phản ứng với coroutine cancel.
 */
class StreamLineChannel(
    override val transport: CarTransport,
    override val peer: String,
    input: InputStream,
    output: OutputStream?,
    private val closeSocket: () -> Unit,
    peerLabel: String? = null,
) : CarLineChannel {

    override val peerLabel: String = peerLabel ?: peer

    private val reader = input.bufferedReader()
    private val out = output
    private val writeLock = Any()

    override fun readLine(): String? = try {
        reader.readLine()
    } catch (_: Exception) {
        null
    }

    override fun trySend(line: String): Boolean {
        val stream = out ?: return false
        return try {
            synchronized(writeLock) {
                stream.write((line + "\n").toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun close() {
        try {
            closeSocket()
        } catch (_: Exception) {
        }
    }
}
