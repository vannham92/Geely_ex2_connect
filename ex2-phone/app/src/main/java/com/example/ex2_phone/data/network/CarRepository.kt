package com.example.ex2_phone.data.network

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStream

@Serializable
data class CarStatusMessage(
    val type: String = "status",
    val speedKmh: Float = 0f,
    val speedAvailable: Boolean = false,
    val batteryPercent: Float = 0f,
    val batteryAvailable: Boolean = false,
    val drivingMode: Int = 0,               // 0=unknown 1=Eco 2=Comfort 3=Sport
    val drivingModeAvailable: Boolean = false,
    // AVAS (âm cảnh báo người đi bộ)
    val avasMuted: Boolean = false,
    val avasAvailable: Boolean = false,
    // Thu hồi năng lượng
    val regenLevel: Int = 0,                // 0=unknown 1=Thấp 2=Trung bình 3=Cao
    val regenAvailable: Boolean = false,
    // Đèn viền nội thất
    val ambientLightOn: Boolean = false,
    val ambientLightAvailable: Boolean = false,
    // Nhiệt độ ngoài trời (°C, chỉ đọc)
    val outsideTempC: Float = 0f,
    val tempAvailable: Boolean = false,
    // WiFi / hotspot
    val wifiOn: Boolean = false,
    val wifiIp: String? = null,
    val wifiAvailable: Boolean = false,
    val rangeKm: Float = 0f,
    val rangeAvailable: Boolean = false,
    val gear: String = "",
    val gearAvailable: Boolean = false,
    val odometerKm: Float = 0f,
    val odometerAvailable: Boolean = false,
    // HVAC
    val hvacAvailable: Boolean = false,
    val hvacAcOn: Boolean = false,
    val hvacTempC: Float = 0f,
    val hvacFanSpeed: Int = 0,
    val hvacRecircOn: Boolean = false,
    val hvacEcoOn: Boolean = false,
    val hvacDefrostOn: Boolean = false,
    // Tire Pressure
    val tirePressureAvailable: Boolean = false,
    val tireFlKpa: Float = 0f,
    val tireFrKpa: Float = 0f,
    val tireRlKpa: Float = 0f,
    val tireRrKpa: Float = 0f,
    val tireFlLow: Boolean = false,
    val tireFrLow: Boolean = false,
    val tireRlLow: Boolean = false,
    val tireRrLow: Boolean = false,
    // Scene Mode
    val sceneAvailable: Boolean = false,
    val sceneMode: Int = 0,                 // 0=none/default, 1=Rest, 2=Camping
    // Khóa cửa
    val doorLockAvailable: Boolean = false,
    val doorsLocked: Boolean = false,
    // Kính cửa (% mở: 0=đóng kín, 100=mở hết). UI hiện ngược lại (độ đóng) — xem `WindowTile`.
    val windowAvailable: Boolean = false,
    val windowFlPercent: Int = 0,
    val windowFrPercent: Int = 0,
    val windowRlPercent: Int = 0,
    val windowRrPercent: Int = 0,
    // Debug info từ xe: lý do từng reader fail
    val debugInfo: String = "",
)

@Serializable
data class CarCommandMessage(
    // "set_driving_mode", "set_regen_level", "set_avas_mute", "set_ambient_light", "set_wifi"
    val command: String,
    val modeValue: Int? = null,
    val enabled: Boolean? = null,
    val floatValue: Float? = null,
)

@Serializable
data class CarCommandResponse(
    val type: String = "command_response",
    val command: String = "",
    val ok: Boolean = false,
    val error: String? = null,
)

/**
 * Lớp mạng: client NDJSON tới head unit, transport bất kỳ.
 *
 * Không tự tạo socket — nhận một [CarLink] đã dựng sẵn qua [setLink] ([TcpCarLink] cho WiFi,
 * [BtSppCarLink] cho Bluetooth). Toàn bộ parse/gửi lệnh dùng chung cho cả hai.
 *
 * Giao thức NDJSON — mỗi bản tin là 1 dòng JSON kết thúc bằng '\n'.
 *  - Xe -> App: [CarStatusMessage] (type="status"), [CarCommandResponse] (type="command_response"),
 *    heartbeat {"type":"ping"} mỗi 15s.
 *  - App -> Xe: [CarCommandMessage].
 *
 * Chỉ mở 1 kết nối tại một thời điểm. Vòng lặp reconnect do `CarViewModel` điều phối; repository
 * sống suốt đời ViewModel (đổi transport = [setLink] rồi [connect] lại, KHÔNG tạo instance mới —
 * các collector đã bám vào flow của instance này).
 */
class CarRepository(initialState: CarStatusMessage = CarStatusMessage()) {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Volatile
    private var link: CarLink? = null

    @Volatile
    private var output: OutputStream? = null

    /** Thời điểm nhận dòng gần nhất (elapsedRealtime) — cho idle watchdog. */
    @Volatile
    private var lastRxAt = 0L

    private val _carStatus = MutableStateFlow(initialState)
    val carStatus: StateFlow<CarStatusMessage> = _carStatus.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _commandResponses = Channel<CarCommandResponse>(Channel.BUFFERED)
    val commandResponses: Flow<CarCommandResponse> = _commandResponses.receiveAsFlow()

    private val _lastRawJson = MutableStateFlow("")
    val lastRawJson: StateFlow<String> = _lastRawJson.asStateFlow()

    /** Link đang kết nối, `null` khi rớt. UI đọc để hiện "Bluetooth" hay "WiFi". */
    private val _activeLink = MutableStateFlow<CarLink?>(null)
    val activeLink: StateFlow<CarLink?> = _activeLink.asStateFlow()

    /** Đặt transport cho lần [connect] kế tiếp. Không tự mở kết nối. */
    fun setLink(link: CarLink) {
        this.link = link
    }

    /**
     * Mở kết nối qua link hiện tại và đọc dữ liệu real-time cho tới khi đứt.
     * Suspend cho tới khi kết nối kết thúc; ném exception để caller reconnect.
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        val currentLink = link ?: throw IllegalStateException("Chưa setLink()")
        Log.i(TAG, "Connecting via ${currentLink.label}...")
        val streams = currentLink.open()
        output = streams.output
        lastRxAt = SystemClock.elapsedRealtime()
        _connected.value = true
        _activeLink.value = currentLink
        Log.i(TAG, "Connected via ${currentLink.label}")

        // Gửi request_status NGAY khi kết nối → xe đọc VHAL rồi push data về
        val requestScope = CoroutineScope(Dispatchers.IO)
        val requestJob = requestScope.launch {
            // Lần 1: gửi ngay sau 500ms (đợi xe setup handleClient)
            delay(500)
            Log.i(TAG, "Sending request_status #1 (immediate)")
            sendRequestStatus()

            // Lần 2: sau 3s nếu vẫn chưa có data
            delay(2_500)
            if (!hasAnyAvailableData()) {
                Log.i(TAG, "Sending request_status #2 (3s retry)")
                sendRequestStatus()
            }

            // Lần 3: sau 10s nếu vẫn chưa có data
            delay(7_000)
            if (!hasAnyAvailableData()) {
                Log.i(TAG, "Sending request_status #3 (10s retry)")
                sendRequestStatus()
            }
        }

        // Idle watchdog dùng chung cho mọi transport: xe ping mỗi 15s, quá [IDLE_TIMEOUT_MS]
        // không nhận gì = link chết (xe ngủ / ra khỏi tầm BT / half-open TCP). Đóng link để bung
        // readLine() — blocking IO không phản ứng với coroutine cancel.
        // BluetoothSocket KHÔNG có soTimeout nên đây là cơ chế duy nhất bắt được BT rớt câm.
        val idleWatchdog = launch {
            while (isActive) {
                delay(IDLE_CHECK_INTERVAL_MS)
                if (SystemClock.elapsedRealtime() - lastRxAt > IDLE_TIMEOUT_MS) {
                    Log.w(TAG, "Idle > ${IDLE_TIMEOUT_MS}ms — đóng ${currentLink.label} để reconnect")
                    currentLink.close()
                    break
                }
            }
        }

        try {
            val reader = streams.input.bufferedReader()
            var lineCount = 0
            while (true) {
                val line = reader.readLine() ?: break   // null = server đóng
                lastRxAt = SystemClock.elapsedRealtime()
                if (line.isBlank()) continue
                lineCount++
                if (lineCount <= 5) {
                    Log.i(TAG, "Received line #$lineCount (${line.length} chars): ${line.take(300)}")
                }
                handleMessage(line)
            }
            Log.i(TAG, "Stream ended (server closed), total lines received: $lineCount")
        } finally {
            idleWatchdog.cancel()
            requestJob.cancel()
            _connected.value = false
            _activeLink.value = null
            output = null
            currentLink.close()
            Log.i(TAG, "Disconnected from ${currentLink.label}")
        }
    }

    private fun hasAnyAvailableData(): Boolean {
        val s = _carStatus.value
        return s.speedAvailable || s.batteryAvailable || s.drivingModeAvailable ||
            s.avasAvailable || s.regenAvailable || s.ambientLightAvailable ||
            s.tempAvailable || s.wifiAvailable
    }

    /** Gửi command request_status trực tiếp (không suspend). */
    private fun sendRequestStatus() {
        val out = output ?: return
        val line = """{"command":"request_status"}""" + "\n"
        try {
            synchronized(out) {
                out.write(line.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            Log.i(TAG, "request_status sent OK")
        } catch (e: Exception) {
            Log.w(TAG, "request_status send failed", e)
        }
    }

    private suspend fun handleMessage(line: String) {
        val type = try {
            (json.parseToJsonElement(line) as? JsonObject)?.get("type")?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse message type: ${line.take(200)}", e)
            null
        } ?: "status"

        when (type) {
            "status" -> {
                try {
                    val status = json.decodeFromString(CarStatusMessage.serializer(), line)
                    _carStatus.value = status
                    _lastRawJson.value = line
                    Log.d(TAG, "Status received: speed=${status.speedAvailable}(${status.speedKmh}) bat=${status.batteryAvailable}(${status.batteryPercent}) mode=${status.drivingModeAvailable}(${status.drivingMode}) avas=${status.avasAvailable} regen=${status.regenAvailable} ambient=${status.ambientLightAvailable} temp=${status.tempAvailable} wifi=${status.wifiAvailable}")
                } catch (e: Exception) {
                    _lastRawJson.value = "PARSE ERROR: ${e.message}\nRAW: ${line.take(500)}"
                    Log.e(TAG, "Failed to decode CarStatusMessage: ${line.take(200)}", e)
                }
            }
            "command_response" -> {
                try {
                    _commandResponses.send(json.decodeFromString(CarCommandResponse.serializer(), line))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode CarCommandResponse", e)
                }
            }
            "ping" -> { /* heartbeat — bỏ qua */ }
            else -> Log.d(TAG, "Unknown message type: $type")
        }
    }

    /** Gửi 1 command NDJSON. Không làm gì nếu chưa kết nối. */
    private suspend fun sendCommand(command: CarCommandMessage) = withContext(Dispatchers.IO) {
        val out = output ?: return@withContext
        val line = json.encodeToString(CarCommandMessage.serializer(), command) + "\n"
        try {
            synchronized(out) {
                out.write(line.toByteArray(Charsets.UTF_8))
                out.flush()
            }
        } catch (_: Exception) { }
    }

    suspend fun setDrivingMode(modeValue: Int) =
        sendCommand(CarCommandMessage(command = "set_driving_mode", modeValue = modeValue))

    suspend fun setRegenLevel(level: Int) =
        sendCommand(CarCommandMessage(command = "set_regen_level", modeValue = level))

    suspend fun setAvasMute(muted: Boolean) =
        sendCommand(CarCommandMessage(command = "set_avas_mute", enabled = muted))

    suspend fun setAmbientLight(enabled: Boolean) =
        sendCommand(CarCommandMessage(command = "set_ambient_light", enabled = enabled))

    suspend fun setWifi(enabled: Boolean) =
        sendCommand(CarCommandMessage(command = "set_wifi", enabled = enabled))

    suspend fun setHvacAc(on: Boolean) =
        sendCommand(CarCommandMessage(command = "set_hvac_ac", enabled = on))

    suspend fun setHvacTemp(tempC: Float) =
        sendCommand(CarCommandMessage(command = "set_hvac_temp", floatValue = tempC))

    suspend fun setHvacFan(level: Int) =
        sendCommand(CarCommandMessage(command = "set_hvac_fan", modeValue = level))

    suspend fun setHvacRecirc(on: Boolean) =
        sendCommand(CarCommandMessage(command = "set_hvac_recirc", enabled = on))

    suspend fun setHvacEco(on: Boolean) =
        sendCommand(CarCommandMessage(command = "set_hvac_eco", enabled = on))

    suspend fun setHvacDefrost(on: Boolean) =
        sendCommand(CarCommandMessage(command = "set_hvac_defrost", enabled = on))

    suspend fun setScene(mode: Int) =
        sendCommand(CarCommandMessage(command = "set_scene", modeValue = mode))

    suspend fun setDoorLock(locked: Boolean) =
        sendCommand(CarCommandMessage(command = "set_door_lock", enabled = locked))

    /** selector: 0=all, 1=FL, 2=FR, 3=RL, 4=RR; percent = độ mở: 0=đóng, 100=mở hết. Chỉ chạy khi xe đỗ. */
    suspend fun setWindow(selector: Int, percent: Int) =
        sendCommand(CarCommandMessage(command = "set_window", modeValue = selector, floatValue = percent.toFloat()))

    /** Yêu cầu xe push lại snapshot trạng thái mới nhất. */
    suspend fun requestStatus() =
        sendCommand(CarCommandMessage(command = "request_status"))

    fun disconnect() {
        _connected.value = false
        _activeLink.value = null
        output = null
        link?.close()
    }

    companion object {
        private const val TAG = "CarRepository"

        // >35s không nhận status/ping (ping xe = 15s) → coi như rớt, để caller reconnect.
        private const val IDLE_TIMEOUT_MS = 35_000L
        private const val IDLE_CHECK_INTERVAL_MS = 5_000L
    }
}
