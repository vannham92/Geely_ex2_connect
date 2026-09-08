package com.geely.ex2.tools.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * Transport WiFi: `ServerSocket` trên [PORT] + quảng bá NSD/mDNS để phone cùng mạng tự dò
 * head unit (khỏi hardcode IP).
 *
 * Tách ra từ `CarTcpServer.runAcceptLoop()` — hành vi giữ nguyên 1:1.
 */
class TcpServerLink(private val context: Context) : CarServerLink {

    override val transport = CarTransport.WIFI

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var nsdManager: NsdManager? = null

    @Volatile
    private var nsdListener: NsdManager.RegistrationListener? = null

    override suspend fun accept(onClient: suspend (CarLineChannel) -> Unit) {
        val ss = try {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(PORT))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bind port $PORT thất bại", e)
            return
        }
        serverSocket = ss
        registerNsd()
        Log.i(TAG, "TCP listening on port $PORT")

        while (!ss.isClosed) {
            val socket = try {
                ss.accept()
            } catch (e: Exception) {
                if (!ss.isClosed) Log.w(TAG, "accept() lỗi", e)
                break
            }
            try {
                socket.tcpNoDelay = true
                socket.keepAlive = true
            } catch (_: Exception) {
            }
            // Một client hỏng không được giết cả accept loop.
            try {
                val channel = StreamLineChannel(
                    transport = CarTransport.WIFI,
                    peer = socket.remoteSocketAddress?.toString() ?: "?",
                    peerLabel = socket.inetAddress?.hostAddress ?: "?",
                    input = socket.getInputStream(),
                    output = try { socket.getOutputStream() } catch (_: Exception) { null },
                    closeSocket = { socket.close() },
                )
                onClient(channel)
            } catch (e: Exception) {
                Log.w(TAG, "Bỏ client TCP lỗi", e)
                try { socket.close() } catch (_: Exception) { }
            }
        }
        Log.i(TAG, "TCP accept loop kết thúc")
    }

    override fun stop() {
        unregisterNsd()
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    private fun registerNsd() {
        if (nsdListener != null) return
        val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: run {
            Log.w(TAG, "NsdManager unavailable — bỏ qua NSD")
            return
        }
        val info = NsdServiceInfo().apply {
            serviceName = NSD_SERVICE_NAME
            serviceType = NSD_SERVICE_TYPE
            port = PORT
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdInfo: NsdServiceInfo) {
                // Tên có thể bị đổi khi trùng (vd "GeelyEX2-Car (2)"); phone nên dò theo serviceType.
                Log.i(TAG, "NSD registered: ${nsdInfo.serviceName}")
            }

            override fun onRegistrationFailed(nsdInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD registration failed: $errorCode")
            }

            override fun onServiceUnregistered(nsdInfo: NsdServiceInfo) {
                Log.i(TAG, "NSD unregistered")
            }

            override fun onUnregistrationFailed(nsdInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD unregistration failed: $errorCode")
            }
        }
        try {
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
            nsdManager = manager
            nsdListener = listener
        } catch (e: Exception) {
            Log.w(TAG, "NSD register error", e)
        }
    }

    private fun unregisterNsd() {
        nsdListener?.let { listener ->
            try {
                nsdManager?.unregisterService(listener)
            } catch (_: Exception) {
            }
        }
        nsdListener = null
        nsdManager = null
    }

    companion object {
        private const val TAG = "TcpServerLink"
        const val PORT = 47800
        const val NSD_SERVICE_TYPE = "_carsync._tcp."
        const val NSD_SERVICE_NAME = "GeelyEX2-Car"
    }
}
