package com.geely.ex2.tools.data.network

/**
 * Transport phía head unit: lắng nghe và sinh [CarLineChannel] cho mỗi phone nối vào.
 *
 * Mỗi link chạy trong coroutine riêng nên một transport chết không kéo theo transport kia.
 */
interface CarServerLink {
    val transport: CarTransport

    /**
     * Chạy vòng accept cho tới khi [stop] hoặc lỗi không phục hồi được.
     * **Không ném** — tự log rồi return, để [CarTcpServer] không phải bọc try/catch.
     *
     * [onClient] chạy tuần tự trong chính vòng accept của link này.
     */
    suspend fun accept(onClient: suspend (CarLineChannel) -> Unit)

    fun stop()
}
