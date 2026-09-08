package com.geely.ex2.tools.data.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Log
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.avas.AvasRepository
import com.geely.ex2.tools.data.scene.SceneRepository
import com.geely.ex2.tools.data.temperature.TemperatureRepository
import com.geely.ex2.tools.data.vhal.CarPropertyAmbientLightReader
import com.geely.ex2.tools.data.vhal.CarPropertyDoorWindowController
import com.geely.ex2.tools.data.vhal.CarPropertyDrivingModeReader
import com.geely.ex2.tools.data.vhal.CarPropertyEnergyRegenerationReader
import com.geely.ex2.tools.data.vhal.CarPropertyHvacController
import com.geely.ex2.tools.data.vhal.CarPropertyIo
import com.geely.ex2.tools.data.vhal.CarPropertyTirePressureReader
import com.geely.ex2.tools.data.vhal.CarPropertyVehicleInfoReader
import com.geely.ex2.tools.data.vhal.CarVhalBindings
import com.geely.ex2.tools.data.vhal.VhalBatteryReader
import com.geely.ex2.tools.data.vhal.VhalBatteryReaderFactory
import com.geely.ex2.tools.data.vhal.VhalConstants
import com.geely.ex2.tools.data.vhal.VhalSpeedReader
import com.geely.ex2.tools.data.vhal.VhalSpeedReaderFactory
import com.geely.ex2.tools.data.wifi.WifiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.roundToInt

@Serializable
data class CarStatusMessage(
    val type: String = "status",
    val speedKmh: Float = 0f,
    val speedAvailable: Boolean = false,
    val batteryPercent: Float = 0f,
    val batteryAvailable: Boolean = false,
    val drivingMode: Int = 0,
    val drivingModeAvailable: Boolean = false,
    // AVAS
    val avasMuted: Boolean = false,
    val avasAvailable: Boolean = false,
    // Energy Regeneration
    val regenLevel: Int = 0,
    val regenAvailable: Boolean = false,
    // Ambient Light
    val ambientLightOn: Boolean = false,
    val ambientLightAvailable: Boolean = false,
    // Temperature (ngoài trời, độ C — chỉ đọc)
    val outsideTempC: Float = 0f,
    val tempAvailable: Boolean = false,
    // WiFi client (station) — trạng thái radio WiFi client theo WifiManager, KHÔNG phải SoftAP/hotspot.
    // wifiIp = IPv4 head unit khi join 1 AP; null khi tắt.
    val wifiOn: Boolean = false,
    val wifiIp: String? = null,
    val wifiAvailable: Boolean = false,
    // Range còn lại (km, chỉ đọc)
    val rangeKm: Float = 0f,
    val rangeAvailable: Boolean = false,
    // Gear hộp số "P"/"R"/"N"/"D" ("" nếu unknown, chỉ đọc)
    val gear: String = "",
    val gearAvailable: Boolean = false,
    // Odometer tổng (km, chỉ đọc)
    val odometerKm: Float = 0f,
    val odometerAvailable: Boolean = false,
    // Điều hòa HVAC — hvacAvailable gate cả cụm; false → phone hiển thị `--` cho mọi field hvac.
    val hvacAvailable: Boolean = false,
    val hvacAcOn: Boolean = false,       // set_hvac_ac
    val hvacTempC: Float = 0f,           // set_hvac_temp (ghế lái)
    val hvacFanSpeed: Int = 0,           // set_hvac_fan
    val hvacRecircOn: Boolean = false,   // set_hvac_recirc
    val hvacEcoOn: Boolean = false,      // set_hvac_eco
    val hvacDefrostOn: Boolean = false,  // set_hvac_defrost
    // Áp suất lốp 4 bánh (kPa, chỉ đọc) + cảnh báo thấp/bánh; tirePressureAvailable gate cả cụm.
    val tirePressureAvailable: Boolean = false,
    val tireFlKpa: Float = 0f,
    val tireFrKpa: Float = 0f,
    val tireRlKpa: Float = 0f,
    val tireRrKpa: Float = 0f,
    val tireFlLow: Boolean = false,
    val tireFrLow: Boolean = false,
    val tireRlLow: Boolean = false,
    val tireRrLow: Boolean = false,
    // Scene Mode (情景空间): 0=none/default, 1=Rest(Nghỉ ngơi), 2=Camping(Cắm trại).
    // sceneAvailable=false -> đọc scene không chắc chắn (schema provider ẩn); phone hiển thị `--`.
    // Điều khiển: set_scene (kích hoạt, không có on-change callback → dựa periodic refresh + refresh sau command).
    val sceneAvailable: Boolean = false,
    val sceneMode: Int = 0,
    // Khóa cửa: doorLockAvailable gate; doorsLocked=true khi mọi cửa đọc được đều đã khóa.
    val doorLockAvailable: Boolean = false,
    val doorsLocked: Boolean = false,
    // Kính cửa (% mở: 0=đóng, 100=mở hết); windowAvailable gate cả cụm.
    val windowAvailable: Boolean = false,
    val windowFlPercent: Int = 0,
    val windowFrPercent: Int = 0,
    val windowRlPercent: Int = 0,
    val windowRrPercent: Int = 0,
    // Thông tin debug: lý do từng reader fail (chỉ gửi khi có lỗi)
    val debugInfo: String = "",
)

@Serializable
data class CarCommandMessage(
    // "set_driving_mode", "set_avas_mute", "set_regen_level", "set_ambient_light", "set_wifi",
    // "set_hvac_ac", "set_hvac_temp", "set_hvac_fan", "set_hvac_recirc", "set_hvac_eco", "set_hvac_defrost",
    // "set_scene" (modeValue 1=rest, 2=camping),
    // "set_door_lock" (enabled: true=khóa), "set_window" (modeValue=selector 0..4, floatValue=% hoặc enabled)
    val command: String,
    val modeValue: Int? = null,
    val enabled: Boolean? = null,
    val floatValue: Float? = null, // set_hvac_temp (°C) / set_window (% mở 0..100)
)

@Serializable
data class CarCommandResponse(
    val type: String = "command_response",
    val command: String,
    val ok: Boolean,
    val error: String? = null,
)

/**
 * Máy chủ đẩy trạng thái xe cho app điện thoại theo **sự kiện**, chạy song song trên 2 transport:
 *  - **WiFi** — TCP port 47800 + NSD ([TcpServerLink]).
 *  - **Bluetooth** — RFCOMM/SPP ([BtSppServerLink]); phone nối được mà không cần cùng mạng.
 *
 * Hai transport dùng chung y hệt business logic — chỉ khác ống dẫn. Client nào cũng là một
 * [CarLineChannel] trong [clients], nhận cùng bản tin broadcast.
 *
 * Khác bản Ktor cũ: không poll mỗi 1s. Thay vào đó đăng ký on-change callback của VHAL
 * (drive mode / regen / ambient / battery) + speed (rate thấp, throttle theo km/h nguyên).
 * Mỗi lần có thay đổi → đọc lại qua reader sẵn có rồi broadcast NDJSON tới mọi client.
 *
 * Giao thức: mỗi bản tin = 1 dòng JSON kết thúc bằng '\n' (NDJSON).
 *  - Car -> Phone: [CarStatusMessage] (type="status"), gửi snapshot khi client vừa nối và
 *    mỗi khi trạng thái đổi. Heartbeat {"type":"ping"} mỗi 15s.
 *  - Phone -> Car: [CarCommandMessage]; Car trả [CarCommandResponse] (type="command_response").
 */
object CarTcpServer {
    private const val TAG = "CarTcpServer"

    // Ngưỡng tốc độ coi là "đứng yên" cho gate điều khiển cửa/kính (km/h).
    private const val SAFE_CONTROL_SPEED_KMH = 3f

    // AC_AMBIENT_TEMP — trùng id trong TemperatureReader (0x2140A377).
    private const val TEMP_AC_AMBIENT_PROP = 0x2140a377

    // Khoảng cách tối thiểu giữa 2 thông báo cùng loại trên head unit (chống spam khi reconnect).
    private const val TOAST_DEBOUNCE_MS = 10_000L

    private val json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Các transport đang chạy (WiFi + Bluetooth). Mỗi link một coroutine accept riêng. */
    private val links = CopyOnWriteArrayList<CarServerLink>()

    @Volatile
    var running = false
        private set

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var wifiReceiver: BroadcastReceiver? = null

    private val clients = CopyOnWriteArraySet<CarLineChannel>()

    /** Lần thông báo gần nhất theo khoá "transport-connected" (elapsedRealtime) — chống spam. */
    private val lastToastAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    val clientCount: Int get() = clients.size

    /** Số client đang nối theo từng transport — để hiển thị/log. */
    fun clientCount(transport: CarTransport): Int = clients.count { it.transport == transport }

    private val _connectedPeers = MutableStateFlow<List<CarLinkPeer>>(emptyList())

    /** Phone đang nối vào head unit, theo từng transport — cho màn Cài đặt quan sát. */
    val connectedPeers: StateFlow<List<CarLinkPeer>> = _connectedPeers.asStateFlow()

    private fun publishPeers() {
        _connectedPeers.value = clients.map { CarLinkPeer(it.transport, it.peerLabel) }
    }

    // Trạng thái xe hiện tại giữ trong bộ nhớ; cập nhật trên luồng CarPropertyIo, broadcast trên IO.
    @Volatile
    private var current = CarStatusMessage()

    // Latch đợi snapshot VHAL đầu tiên xong trước khi accept loop gửi cho phone.
    @Volatile
    private var snapshotReady = CountDownLatch(1)

    // Chỉ broadcast speed khi km/h (làm tròn) đổi, tránh flood với giá trị liên tục.
    @Volatile
    private var lastSpeedRounded = Int.MIN_VALUE

    // Cached readers giữ kết nối android.car.Car ấm giữa các lần đọc. Chỉ chạm trên luồng CarPropertyIo.
    @Volatile
    private var speedReader: VhalSpeedReader? = null
    @Volatile
    private var batteryReader: VhalBatteryReader? = null
    @Volatile
    private var drivingReader: CarPropertyDrivingModeReader? = null
    @Volatile
    private var regenReader: CarPropertyEnergyRegenerationReader? = null
    @Volatile
    private var ambientReader: CarPropertyAmbientLightReader? = null
    @Volatile
    private var vehicleInfoReader: CarPropertyVehicleInfoReader? = null
    @Volatile
    private var hvacController: CarPropertyHvacController? = null
    @Volatile
    private var tirePressureReader: CarPropertyTirePressureReader? = null
    // Scene Mode: Intent/provider-based (không giữ android.car connection), tạo 1 lần dùng lại.
    @Volatile
    private var sceneRepository: SceneRepository? = null
    @Volatile
    private var doorWindowController: CarPropertyDoorWindowController? = null

    // Bindings riêng cho on-change callback (tách khỏi bindings nội bộ của reader).
    @Volatile
    private var eventBindings: CarVhalBindings? = null
    private val eventCallbacks = mutableListOf<Any>()

    // Phone button domain <-> Flyme drive-mode codes.
    // Phone gửi/so 1/2/3; xe đọc/ghi mã Flyme thô.
    private val phoneToFlymeMode = mapOf(
        1 to VhalConstants.DRIVE_MODE_ECO,      // 0x22010101
        2 to VhalConstants.DRIVE_MODE_COMFORT,  // 0x22010102
        3 to VhalConstants.DRIVE_MODE_DYNAMIC,  // 0x22010103
    )
    private val flymeToPhoneMode = phoneToFlymeMode.entries.associate { (k, v) -> v to k }

    // Phone regen button domain <-> Flyme regen codes.
    private val phoneToFlymeRegen = mapOf(
        1 to VhalConstants.ENERGY_REGENERATION_LEVEL_LOW,
        2 to VhalConstants.ENERGY_REGENERATION_LEVEL_MID,
        3 to VhalConstants.ENERGY_REGENERATION_LEVEL_HIGH,
    )
    private val flymeToPhoneRegen = phoneToFlymeRegen.entries.associate { (k, v) -> v to k }

    fun start(context: Context) {
        if (running) return
        running = true
        val appContext = context.applicationContext
        this.appContext = appContext
        snapshotReady = CountDownLatch(1)

        // Seed snapshot + đăng ký on-change callback trên luồng CarPropertyIo.
        // Retry với backoff nếu VHAL chưa sẵn sàng (xe boot chậm).
        CarPropertyIo.execute {
            var retryCount = 0
            val maxRetries = 5
            val retryDelays = longArrayOf(0, 2_000, 4_000, 8_000, 15_000)
            while (retryCount <= maxRetries) {
                try {
                    val snapshot = readCarStatus(appContext)
                    current = snapshot
                    val anyAvailable = snapshot.speedAvailable || snapshot.batteryAvailable ||
                        snapshot.drivingModeAvailable || snapshot.avasAvailable ||
                        snapshot.regenAvailable || snapshot.ambientLightAvailable ||
                        snapshot.tempAvailable || snapshot.wifiAvailable
                    if (anyAvailable) {
                        Log.i(TAG, "Initial snapshot OK (retry=$retryCount): available data found")
                        break
                    } else {
                        Log.w(TAG, "Snapshot retry $retryCount: all *Available=false, VHAL may not be ready")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Initial snapshot read failed (retry=$retryCount)", e)
                }
                retryCount++
                if (retryCount <= maxRetries) {
                    val delayMs = retryDelays.getOrElse(retryCount) { 15_000 }
                    Log.i(TAG, "Retrying initial snapshot in ${delayMs}ms...")
                    Thread.sleep(delayMs)
                }
            }
            snapshotReady.countDown()
            registerEventCallbacks(appContext)
        }

        registerWifiReceiver(appContext)

        // Mỗi transport một coroutine riêng: Bluetooth chết không kéo theo WiFi và ngược lại.
        links += TcpServerLink(appContext)
        links += BtSppServerLink(appContext)
        links.forEach { link ->
            serverScope.launch { link.accept { channel -> onPhoneConnected(appContext, channel) } }
        }

        serverScope.launch { runHeartbeat() }
        serverScope.launch { runPeriodicRefresh(appContext) }
    }

    fun stop() {
        running = false
        links.forEach { it.stop() }
        links.clear()
        wifiReceiver?.let { receiver ->
            try {
                appContext?.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
        wifiReceiver = null
        eventCallbacks.toList().let { callbacks ->
            CarPropertyIo.execute {
                callbacks.forEach { eventBindings?.unregisterPropertyCallback(it) }
                eventCallbacks.clear()
                eventBindings?.close()
                eventBindings = null
                speedReader?.close(); speedReader = null
                batteryReader?.close(); batteryReader = null
                drivingReader?.close(); drivingReader = null
                regenReader?.close(); regenReader = null
                ambientReader?.close(); ambientReader = null
                vehicleInfoReader?.close(); vehicleInfoReader = null
                hvacController?.close(); hvacController = null
                tirePressureReader?.close(); tirePressureReader = null
                doorWindowController?.close(); doorWindowController = null
                sceneRepository = null
            }
        }
        clients.forEach { it.close() }
        clients.clear()
        publishPeers()
    }

    // -------------------------------------------------------------- Clients

    /**
     * Một phone vừa nối vào (qua transport bất kỳ): gửi snapshot tươi rồi giao cho [handleClient].
     *
     * Chạy tuần tự trong vòng accept của link → snapshot đọc xong mới nhận client kế tiếp.
     */
    private suspend fun onPhoneConnected(context: Context, client: CarLineChannel) {
        // Toast trước khi add — cần biết transport này trước đó có client nào chưa.
        val firstOfTransport = clients.none { it.transport == client.transport }
        clients.add(client)
        publishPeers()
        Log.i(TAG, "Phone connected via ${client.transport}: ${client.peer}")
        if (firstOfTransport) showLinkToast(context, client, connected = true)

        // Đọc trực tiếp snapshot mới nhất (KHÔNG qua CarPropertyIo — tránh deadlock
        // khi CarPropertyIo thread đang bận registerEventCallbacks lúc boot).
        val freshSnapshot = try {
            readCarStatus(context)
        } catch (e: Exception) {
            Log.w(TAG, "Fresh snapshot for new client failed", e)
            current // fallback: dùng snapshot cũ
        }
        current = freshSnapshot

        Log.i(TAG, "Sending snapshot to phone: speed=${freshSnapshot.speedAvailable}(${freshSnapshot.speedKmh}) bat=${freshSnapshot.batteryAvailable}(${freshSnapshot.batteryPercent}) mode=${freshSnapshot.drivingModeAvailable}(${freshSnapshot.drivingMode}) avas=${freshSnapshot.avasAvailable} regen=${freshSnapshot.regenAvailable}(${freshSnapshot.regenLevel}) ambient=${freshSnapshot.ambientLightAvailable}(${freshSnapshot.ambientLightOn}) temp=${freshSnapshot.tempAvailable}(${freshSnapshot.outsideTempC}) wifi=${freshSnapshot.wifiAvailable}(${freshSnapshot.wifiOn})")
        if (!client.trySend(encodeStatus(freshSnapshot))) {
            removeClient(client)
            return
        }
        serverScope.launch { handleClient(context, client) }
    }

    /**
     * Thông báo nổi trên head unit khi phone nối vào / rời đi, báo rõ đường nào và thiết bị nào.
     *
     * Chống spam: chỉ báo cho client **đầu tiên** của mỗi transport (và client **cuối cùng** khi
     * ngắt), thêm tối thiểu [TOAST_DEBOUNCE_MS] giữa 2 lần cùng loại — phone reconnect mỗi 3s khi
     * sóng chập chờn sẽ không làm ngập màn hình.
     */
    private fun showLinkToast(context: Context, client: CarLineChannel, connected: Boolean) {
        val key = "${client.transport}-$connected"
        val now = SystemClock.elapsedRealtime()
        if (now - (lastToastAt[key] ?: 0L) < TOAST_DEBOUNCE_MS) return
        lastToastAt[key] = now

        val resId = when (client.transport) {
            CarTransport.BLUETOOTH ->
                if (connected) R.string.car_link_connected_bluetooth
                else R.string.car_link_disconnected_bluetooth
            CarTransport.WIFI ->
                if (connected) R.string.car_link_connected_wifi
                else R.string.car_link_disconnected_wifi
        }
        CarLinkToast.show(context, context.getString(resId, client.peerLabel))
    }

    private suspend fun handleClient(context: Context, client: CarLineChannel) {
        try {
            while (running) {
                val line = client.readLine() ?: break
                if (line.isBlank()) continue
                val cmd = try {
                    json.decodeFromString(CarCommandMessage.serializer(), line)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid command from phone: $line", e)
                    client.trySend(
                        encodeResponse(CarCommandResponse(command = "unknown", ok = false, error = e.message)),
                    )
                    continue
                }

                // request_status: đọc VHAL trực tiếp rồi push snapshot cho client ngay
                // KHÔNG dùng CarPropertyIo.call — tránh deadlock.
                if (cmd.command == "request_status") {
                    Log.i(TAG, "Phone requested fresh status — reading VHAL now (direct)")
                    val freshSnapshot = try {
                        readCarStatus(context)
                    } catch (e: Exception) {
                        Log.w(TAG, "request_status: readCarStatus failed", e)
                        current
                    }
                    current = freshSnapshot
                    Log.i(TAG, "request_status: Sending snapshot: speed=${freshSnapshot.speedAvailable}(${freshSnapshot.speedKmh}) bat=${freshSnapshot.batteryAvailable}(${freshSnapshot.batteryPercent}) mode=${freshSnapshot.drivingModeAvailable}(${freshSnapshot.drivingMode}) wifi=${freshSnapshot.wifiAvailable}(${freshSnapshot.wifiOn})")
                    client.trySend(encodeStatus(freshSnapshot))
                    continue
                }

                val response = handleCommand(context, cmd)
                client.trySend(encodeResponse(response))
                if (response.ok) {
                    // Command có thể đổi trạng thái (nhất là AVAS và HVAC — không có on-change callback).
                    // Đọc lại + broadcast để mọi client đồng bộ. Đợi 1000ms để ECU kịp áp dụng.
                    serverScope.launch(Dispatchers.IO) {
                        delay(1000)
                        refreshAndBroadcast(context)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client loop error", e)
        } finally {
            removeClient(client)
            Log.i(TAG, "Phone disconnected (${client.transport}): ${client.peer}")
        }
    }

    private suspend fun runHeartbeat() {
        val ping = """{"type":"ping"}""" + "\n"
        while (running) {
            delay(15_000)
            if (clients.isEmpty()) continue
            clients.forEach { if (!it.trySend(ping)) removeClient(it) }
        }
    }

    /**
     * Refresh định kỳ mỗi 5s khi có client kết nối.
     * VHAL trên một số xe (đặc biệt là Geely/Flyme) không bắn onChangeEvent khi điều hoà
     * được thay đổi từ màn hình vật lý, nên phải poll liên tục để phone nhận được tức thời.
     */
    private suspend fun runPeriodicRefresh(context: Context) {
        while (running) {
            delay(5_000)
            if (clients.isEmpty()) continue
            refreshAndBroadcast(context)
        }
    }

    /**
     * Gỡ 1 client. Đặt toast "đã ngắt" ở đây (không ở [handleClient]) để mọi đường rơi đều báo:
     * heartbeat gửi hỏng, broadcast gửi hỏng, hay vòng đọc kết thúc.
     */
    private fun removeClient(client: CarLineChannel) {
        if (!clients.remove(client)) return
        client.close()
        publishPeers()
        // Chỉ báo khi transport này không còn client nào — nối 2 phone cùng đường thì im.
        if (clients.none { it.transport == client.transport }) {
            appContext?.let { showLinkToast(it, client, connected = false) }
        }
    }

    private fun broadcast(status: CarStatusMessage) {
        if (clients.isEmpty()) return
        val line = encodeStatus(status)
        clients.forEach { if (!it.trySend(line)) removeClient(it) }
    }

    private fun encodeStatus(status: CarStatusMessage): String =
        json.encodeToString(CarStatusMessage.serializer(), status) + "\n"

    private fun encodeResponse(response: CarCommandResponse): String =
        json.encodeToString(CarCommandResponse.serializer(), response) + "\n"

    // -------------------------------------------------------------- Events

    /** Gọi trên luồng [CarPropertyIo]. */
    private fun registerEventCallbacks(context: Context) {
        val bindings = CarVhalBindings(context)
        if (!bindings.ensureConnected()) {
            Log.w(TAG, "Event callbacks skipped: CarPropertyManager unavailable")
            return
        }
        eventBindings = bindings

        // Speed: float, rate thấp; onSpeedRaw throttle theo km/h nguyên.
        bindings.registerPropertyCallback(
            propertyId = VhalConstants.PROP_PERF_VEHICLE_SPEED,
            updateRateHz = VhalConstants.SPEED_CALLBACK_RATE_HZ,
            onValue = { raw -> onSpeedRaw(context, raw) },
            onError = { Log.w(TAG, "speed callback error: $it") },
        )?.let { eventCallbacks.add(it) }

        // Battery %: float on-change -> đọc lại full (logic decode phức tạp, tái dùng reader).
        bindings.registerPropertyCallback(
            propertyId = VhalConstants.PROP_ED_EV_BATTERY_PERCENTAGE,
            updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
            onValue = { onDomainChanged(context) },
            onError = { Log.w(TAG, "battery callback error: $it") },
        )?.let { eventCallbacks.add(it) }

        // Drive mode: int on-change.
        bindings.registerIntPropertyCallback(
            propertyId = VhalConstants.PROP_DM_FUNC_DRIVE_MODE_SELECT,
            updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
            onValue = { onDomainChanged(context) },
            onError = { Log.w(TAG, "drive-mode callback error: $it") },
        )?.let { eventCallbacks.add(it) }

        // Energy regeneration: int on-change.
        bindings.registerIntPropertyCallback(
            propertyId = VhalConstants.PROP_SETTING_FUNC_ENERGY_REGENERATION,
            updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
            onValue = { onDomainChanged(context) },
            onError = { Log.w(TAG, "regen callback error: $it") },
        )?.let { eventCallbacks.add(it) }

        // Ambient light switch: int on-change.
        bindings.registerIntPropertyCallback(
            propertyId = VhalConstants.PROP_LIGHTINSIDE_ATMOSPHERE_LAMP_SWITCH,
            updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
            onValue = { onDomainChanged(context) },
            onError = { Log.w(TAG, "ambient callback error: $it") },
        )?.let { eventCallbacks.add(it) }

        // Temperature: AC_AMBIENT_TEMP int on-change (id trùng TemperatureReader).
        bindings.registerIntPropertyCallback(
            propertyId = TEMP_AC_AMBIENT_PROP,
            updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
            onValue = { onDomainChanged(context) },
            onError = { Log.w(TAG, "temperature callback error: $it") },
        )?.let { eventCallbacks.add(it) }

        // Gear selection: int on-change (P/R/N/D đổi rời rạc → phản hồi tức thì).
        // (Range/odometer đổi liên tục → dựa vào periodic refresh 30s, không đăng ký callback.)
        bindings.registerIntPropertyCallback(
            propertyId = VhalConstants.PROP_GEAR_SELECTION,
            updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
            onValue = { onDomainChanged(context) },
            onError = { Log.w(TAG, "gear callback error: $it") },
        )?.let { eventCallbacks.add(it) }

        // HVAC: on-change type-agnostic (prop bool → parse int/float trả null, phải dùng registerChangeCallback).
        // Bất kỳ prop nào đổi trên head unit → đọc lại full + broadcast (đồng bộ phone tức thì, khỏi đợi 30s).
        intArrayOf(
            VhalConstants.PROP_HVAC_AC_ON,
            VhalConstants.PROP_HVAC_TEMPERATURE_SET,
            VhalConstants.PROP_HVAC_FAN_SPEED,
            VhalConstants.PROP_HVAC_RECIRC_ON,
            VhalConstants.PROP_HVAC_FUNC_ECO_SWITCH,
            VhalConstants.PROP_HVAC_MAX_DEFROST_ON,
        ).forEach { propId ->
            bindings.registerChangeCallback(
                propertyId = propId,
                updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
                onChange = { onDomainChanged(context) },
                onError = { Log.w(TAG, "hvac callback error (0x${propId.toString(16)}): $it") },
            )?.let { eventCallbacks.add(it) }
        }

        Log.i(TAG, "Registered ${eventCallbacks.size} VHAL on-change callbacks")
    }

    /** WiFi không phải VHAL prop -> lắng nghe broadcast WIFI_STATE_CHANGED để push khi đổi. */
    private fun registerWifiReceiver(context: Context) {
        if (wifiReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    onDomainChanged(context)
                }
            }
        }
        try {
            context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
            wifiReceiver = receiver
        } catch (e: Exception) {
            Log.w(TAG, "WiFi receiver register failed", e)
        }
    }

    /** Sự kiện speed thô (chạy trên luồng callback của VHAL). */
    private fun onSpeedRaw(context: Context, rawKmh: Float) {
        val rounded = rawKmh.roundToInt()
        if (rounded == lastSpeedRounded) return
        lastSpeedRounded = rounded
        serverScope.launch(Dispatchers.IO) {
            val sample = try {
                val reader = speedReader
                    ?: VhalSpeedReaderFactory.create(context).also { speedReader = it }
                reader.readSpeed()
            } catch (e: Exception) {
                Log.w(TAG, "Speed read failed", e)
                null
            }
            if (sample != null) {
                current = current.copy(speedKmh = sample.speedKmh, speedAvailable = sample.isAvailable)
                val snapshot = current
                broadcast(snapshot)
            }
        }
    }

    /** Bất kỳ thuộc tính on-change nào đổi -> đọc lại full snapshot rồi broadcast. */
    private fun onDomainChanged(context: Context) {
        refreshAndBroadcast(context)
    }

    private fun refreshAndBroadcast(context: Context) {
        serverScope.launch(Dispatchers.IO) {
            current = try {
                readCarStatus(context)
            } catch (e: Exception) {
                Log.w(TAG, "Snapshot refresh failed", e)
                return@launch
            }
            val snapshot = current
            broadcast(snapshot)
        }
    }

    // -------------------------------------------------------------- VHAL IO

    private fun readCarStatus(context: Context): CarStatusMessage {
        val debug = StringBuilder()
        var speedKmh = 0f
        var speedAvailable = false
        var batteryPercent = 0f
        var batteryAvailable = false
        var drivingMode = 0
        var drivingModeAvailable = false

        try {
            val speedSample = run {
                val reader = speedReader
                    ?: VhalSpeedReaderFactory.create(context).also { speedReader = it }
                reader.readSpeed()
            }
            speedKmh = speedSample.speedKmh
            speedAvailable = speedSample.isAvailable
            Log.d(TAG, "readCarStatus speed: available=${speedSample.isAvailable} kmh=${speedSample.speedKmh} src=${speedSample.source}")
            debug.append("[Speed] ").append(if (speedSample.isAvailable) "OK: ${speedSample.speedKmh} km/h (${speedSample.source})" else "FAIL: ${speedSample.source}").append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "Speed read failed", e)
            debug.append("[Speed] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        try {
            val batterySample = run {
                val reader = batteryReader
                    ?: VhalBatteryReaderFactory.create(context).also { batteryReader = it }
                reader.readBatterySoc()
            }
            batteryPercent = batterySample.socPercent
            batteryAvailable = batterySample.isAvailable
            Log.d(TAG, "readCarStatus battery: available=${batterySample.isAvailable} soc=${batterySample.socPercent} src=${batterySample.source}")
            debug.append("[Battery] ").append(if (batterySample.isAvailable) "OK: ${batterySample.socPercent}% (${batterySample.source})" else "FAIL: ${batterySample.source}").append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "Battery read failed", e)
            debug.append("[Battery] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        try {
            val drivingSample = run {
                val reader = drivingReader
                    ?: CarPropertyDrivingModeReader(context).also { drivingReader = it }
                reader.readDrivingMode()
            }
            // Map mã Flyme thô -> phone button domain (1/2/3); 0 = không khớp/unknown.
            drivingMode = flymeToPhoneMode[drivingSample.modeValue] ?: 0
            drivingModeAvailable = drivingSample.isAvailable
            Log.d(TAG, "readCarStatus driving: available=${drivingSample.isAvailable} raw=0x${drivingSample.modeValue.toString(16)} mapped=$drivingMode src=${drivingSample.source}")
            debug.append("[Driving] ").append(if (drivingSample.isAvailable) "OK: mode=$drivingMode (${drivingSample.source})" else "FAIL: ${drivingSample.source}").append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "Driving mode read failed", e)
            debug.append("[Driving] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // AVAS
        var avasMuted = false
        var avasAvailable = false
        try {
            val avasRepo = AvasRepository(context)
            try {
                val avasSample = avasRepo.readAvas()
                avasMuted = avasSample.isMuted
                avasAvailable = avasSample.isAvailable
                Log.d(TAG, "readCarStatus avas: available=${avasSample.isAvailable} muted=${avasSample.isMuted}")
                debug.append("[AVAS] ").append(if (avasSample.isAvailable) "OK: muted=$avasMuted" else "FAIL").append('\n')
            } finally {
                avasRepo.close()
            }
        } catch (e: Exception) {
            Log.w(TAG, "AVAS read failed", e)
            debug.append("[AVAS] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // Energy Regeneration
        var regenLevel = 0
        var regenAvailable = false
        try {
            val regenSample = run {
                val reader = regenReader
                    ?: CarPropertyEnergyRegenerationReader(context).also { regenReader = it }
                reader.readEnergyRegeneration()
            }
            regenLevel = flymeToPhoneRegen[regenSample.levelValue] ?: 0
            regenAvailable = regenSample.isAvailable
            Log.d(TAG, "readCarStatus regen: available=${regenSample.isAvailable} raw=0x${regenSample.levelValue.toString(16)} mapped=$regenLevel")
            debug.append("[Regen] ").append(if (regenSample.isAvailable) "OK: level=$regenLevel (${regenSample.source})" else "FAIL: ${regenSample.source}").append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "Regen read failed", e)
            debug.append("[Regen] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // Ambient Light
        var ambientLightOn = false
        var ambientLightAvailable = false
        try {
            val ambientSample = run {
                val reader = ambientReader
                    ?: CarPropertyAmbientLightReader(context).also { ambientReader = it }
                reader.readAmbientLight()
            }
            ambientLightOn = ambientSample.isEnabled
            ambientLightAvailable = ambientSample.isAvailable
            Log.d(TAG, "readCarStatus ambient: available=${ambientSample.isAvailable} on=${ambientSample.isEnabled}")
            debug.append("[Ambient] ").append(if (ambientSample.isAvailable) "OK: on=$ambientLightOn (${ambientSample.source})" else "FAIL: ${ambientSample.source}").append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "Ambient light read failed", e)
            debug.append("[Ambient] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // Temperature (sensor ngoài trời, chỉ đọc)
        var outsideTempC = 0f
        var tempAvailable = false
        try {
            val tempResult = TemperatureRepository(context).readTemperature()
            outsideTempC = tempResult.value
            tempAvailable = tempResult.ok
            Log.d(TAG, "readCarStatus temp: available=${tempResult.ok} value=${tempResult.value}")
            debug.append("[Temp] ").append(if (tempResult.ok) "OK: ${tempResult.value}°C (${tempResult.source})" else "FAIL: ${tempResult.source} ${tempResult.details}").append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "Temperature read failed", e)
            debug.append("[Temp] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // WiFi / hotspot (WifiManager — không cần CarPropertyIo)
        var wifiOn = false
        var wifiIp: String? = null
        var wifiAvailable = false
        try {
            val wifiRepo = WifiRepository(context)
            wifiOn = wifiRepo.isWifiEnabledOrEnabling()
            wifiIp = if (wifiOn) wifiRepo.getWifiIpAddress() else null
            wifiAvailable = wifiRepo.getWifiState() != WifiManager.WIFI_STATE_UNKNOWN
            Log.d(TAG, "readCarStatus wifi: available=$wifiAvailable on=$wifiOn ip=$wifiIp")
            debug.append("[WiFi] ").append(if (wifiAvailable) "OK: on=$wifiOn ip=$wifiIp" else "FAIL: state unknown").append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "WiFi read failed", e)
            debug.append("[WiFi] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // Vehicle info bổ sung (range / gear / odometer) — chỉ đọc, gộp một reader.
        var rangeKm = 0f
        var rangeAvailable = false
        var gear = ""
        var gearAvailable = false
        var odometerKm = 0f
        var odometerAvailable = false
        try {
            val info = run {
                val reader = vehicleInfoReader
                    ?: CarPropertyVehicleInfoReader(context).also { vehicleInfoReader = it }
                reader.read()
            }
            rangeKm = info.rangeKm
            rangeAvailable = info.rangeAvailable
            gear = info.gear
            gearAvailable = info.gearAvailable
            odometerKm = info.odometerKm
            odometerAvailable = info.odometerAvailable
            Log.d(TAG, "readCarStatus vehicleInfo: range=$rangeAvailable($rangeKm) gear=$gearAvailable($gear) odo=$odometerAvailable($odometerKm)")
            debug.append("[VehicleInfo]\n").append(info.details).append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle info read failed", e)
            debug.append("[VehicleInfo] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // Điều hòa HVAC (đọc; chỉ đọc được trên build `system` + xe hỗ trợ)
        var hvacAvailable = false
        var hvacAcOn = false
        var hvacTempC = 0f
        var hvacFanSpeed = 0
        var hvacRecircOn = false
        var hvacEcoOn = false
        var hvacDefrostOn = false
        try {
            val hvac = run {
                val ctrl = hvacController
                    ?: CarPropertyHvacController(context).also { hvacController = it }
                ctrl.read()
            }
            hvacAvailable = hvac.available
            hvacAcOn = hvac.acOn
            hvacTempC = hvac.tempC
            hvacFanSpeed = hvac.fanSpeed
            hvacRecircOn = hvac.recircOn
            hvacEcoOn = hvac.ecoOn
            hvacDefrostOn = hvac.defrostOn
            Log.d(TAG, "readCarStatus hvac: available=$hvacAvailable ac=$hvacAcOn temp=$hvacTempC fan=$hvacFanSpeed recirc=$hvacRecircOn eco=$hvacEcoOn defrost=$hvacDefrostOn")
            debug.append("[HVAC] ").append(if (hvac.available) "OK" else "FAIL").append('\n').append(hvac.details).append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "HVAC read failed", e)
            debug.append("[HVAC] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // Áp suất lốp 4 bánh (đọc; AOSP TIRE_PRESSURE, kPa)
        var tirePressureAvailable = false
        var tireFlKpa = 0f
        var tireFrKpa = 0f
        var tireRlKpa = 0f
        var tireRrKpa = 0f
        var tireFlLow = false
        var tireFrLow = false
        var tireRlLow = false
        var tireRrLow = false
        try {
            val tp = run {
                val reader = tirePressureReader
                    ?: CarPropertyTirePressureReader(context).also { tirePressureReader = it }
                reader.read()
            }
            tirePressureAvailable = tp.available
            tireFlKpa = tp.flKpa; tireFrKpa = tp.frKpa; tireRlKpa = tp.rlKpa; tireRrKpa = tp.rrKpa
            tireFlLow = tp.flLow; tireFrLow = tp.frLow; tireRlLow = tp.rlLow; tireRrLow = tp.rrLow
            Log.d(TAG, "readCarStatus tire: available=$tirePressureAvailable FL=$tireFlKpa/$tireFlLow FR=$tireFrKpa/$tireFrLow RL=$tireRlKpa/$tireRlLow RR=$tireRrKpa/$tireRrLow")
            debug.append("[Tire] ").append(if (tp.available) "OK" else "FAIL").append('\n').append(tp.details)
        } catch (e: Exception) {
            Log.w(TAG, "Tire pressure read failed", e)
            debug.append("[Tire] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // Scene Mode (Rest/Camping) — best-effort đọc; kích hoạt qua set_scene.
        var sceneAvailable = false
        var sceneMode = 0
        try {
            val sample = run {
                val repo = sceneRepository
                    ?: SceneRepository(context).also { sceneRepository = it }
                repo.readCurrentScene()
            }
            sceneAvailable = sample.available
            sceneMode = sample.phoneMode
            Log.d(TAG, "readCarStatus scene: available=$sceneAvailable mode=$sceneMode")
            debug.append("[Scene] ").append(if (sample.available) "OK: mode=$sceneMode" else "FAIL/unknown").append('\n')
        } catch (e: Exception) {
            Log.w(TAG, "Scene read failed", e)
            debug.append("[Scene] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        // Khóa cửa + vị trí kính (đọc; ghi chỉ trên build `system` + xe hỗ trợ)
        var doorLockAvailable = false
        var doorsLocked = false
        var windowAvailable = false
        var windowFlPercent = 0
        var windowFrPercent = 0
        var windowRlPercent = 0
        var windowRrPercent = 0
        try {
            val dw = run {
                val ctrl = doorWindowController
                    ?: CarPropertyDoorWindowController(context).also { doorWindowController = it }
                ctrl.read()
            }
            doorLockAvailable = dw.doorLockAvailable
            doorsLocked = dw.doorsLocked
            windowAvailable = dw.windowAvailable
            windowFlPercent = dw.windowFlPercent
            windowFrPercent = dw.windowFrPercent
            windowRlPercent = dw.windowRlPercent
            windowRrPercent = dw.windowRrPercent
            Log.d(TAG, "readCarStatus doorWindow: doorLock=$doorLockAvailable(locked=$doorsLocked) window=$windowAvailable FL=$windowFlPercent FR=$windowFrPercent RL=$windowRlPercent RR=$windowRrPercent")
            debug.append("[DoorWindow] door=").append(if (dw.doorLockAvailable) "OK" else "FAIL")
                .append(" window=").append(if (dw.windowAvailable) "OK" else "FAIL").append('\n').append(dw.details)
        } catch (e: Exception) {
            Log.w(TAG, "Door/window read failed", e)
            debug.append("[DoorWindow] EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        val result = CarStatusMessage(
            type = "status",
            speedKmh = speedKmh,
            speedAvailable = speedAvailable,
            batteryPercent = batteryPercent,
            batteryAvailable = batteryAvailable,
            drivingMode = drivingMode,
            drivingModeAvailable = drivingModeAvailable,
            avasMuted = avasMuted,
            avasAvailable = avasAvailable,
            regenLevel = regenLevel,
            regenAvailable = regenAvailable,
            ambientLightOn = ambientLightOn,
            ambientLightAvailable = ambientLightAvailable,
            outsideTempC = outsideTempC,
            tempAvailable = tempAvailable,
            wifiOn = wifiOn,
            wifiIp = wifiIp,
            wifiAvailable = wifiAvailable,
            rangeKm = rangeKm,
            rangeAvailable = rangeAvailable,
            gear = gear,
            gearAvailable = gearAvailable,
            odometerKm = odometerKm,
            odometerAvailable = odometerAvailable,
            hvacAvailable = hvacAvailable,
            hvacAcOn = hvacAcOn,
            hvacTempC = hvacTempC,
            hvacFanSpeed = hvacFanSpeed,
            hvacRecircOn = hvacRecircOn,
            hvacEcoOn = hvacEcoOn,
            hvacDefrostOn = hvacDefrostOn,
            tirePressureAvailable = tirePressureAvailable,
            tireFlKpa = tireFlKpa,
            tireFrKpa = tireFrKpa,
            tireRlKpa = tireRlKpa,
            tireRrKpa = tireRrKpa,
            tireFlLow = tireFlLow,
            tireFrLow = tireFrLow,
            tireRlLow = tireRlLow,
            tireRrLow = tireRrLow,
            sceneAvailable = sceneAvailable,
            sceneMode = sceneMode,
            doorLockAvailable = doorLockAvailable,
            doorsLocked = doorsLocked,
            windowAvailable = windowAvailable,
            windowFlPercent = windowFlPercent,
            windowFrPercent = windowFrPercent,
            windowRlPercent = windowRlPercent,
            windowRrPercent = windowRrPercent,
            debugInfo = debug.toString().trim(),
        )
        val availableFlags = listOf(
            speedAvailable, batteryAvailable, drivingModeAvailable, avasAvailable,
            regenAvailable, ambientLightAvailable, tempAvailable, wifiAvailable,
            rangeAvailable, gearAvailable, odometerAvailable, hvacAvailable,
            tirePressureAvailable
        )
        val availableCount = availableFlags.count { it }
        Log.i(TAG, "readCarStatus complete: $availableCount/${availableFlags.size} readers available\n$debug")
        return result
    }

    private fun handleCommand(context: Context, cmd: CarCommandMessage): CarCommandResponse {
        return when (cmd.command) {
            "set_driving_mode" -> {
                val modeValue = cmd.modeValue
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "modeValue missing")
                // Phone gửi 1/2/3 -> đổi sang mã Flyme thô trước khi ghi.
                // Fallback dùng giá trị thô nếu mã Flyme được gửi trực tiếp.
                val flymeMode = phoneToFlymeMode[modeValue] ?: modeValue
                run {
                    val reader = drivingReader
                        ?: CarPropertyDrivingModeReader(context).also { drivingReader = it }
                    val result = reader.writeDrivingMode(flymeMode)
                    CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
                }
            }
            "set_avas_mute" -> {
                val muted = cmd.enabled
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "enabled missing")
                val avasRepo = AvasRepository(context)
                try {
                    val result = avasRepo.setMuted(muted)
                    CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
                } finally {
                    avasRepo.close()
                }
            }
            "set_regen_level" -> {
                val level = cmd.modeValue
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "modeValue missing")
                val flymeLevel = phoneToFlymeRegen[level] ?: level
                run {
                    val reader = regenReader
                        ?: CarPropertyEnergyRegenerationReader(context).also { regenReader = it }
                    val result = reader.writeEnergyRegeneration(flymeLevel)
                    CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
                }
            }
            "set_ambient_light" -> {
                val enabled = cmd.enabled
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "enabled missing")
                run {
                    val reader = ambientReader
                        ?: CarPropertyAmbientLightReader(context).also { ambientReader = it }
                    val result = reader.writeAmbientLight(enabled)
                    CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
                }
            }
            "set_wifi" -> {
                val enabled = cmd.enabled
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "enabled missing")
                try {
                    // ok phản ánh kết quả thật của setWifiEnabled (false = bị từ chối, vd build không
                    // có quyền hệ thống). WifiManager áp bất đồng bộ; WIFI_STATE_CHANGED receiver
                    // sẽ push status mới khi trạng thái ổn định.
                    val ok = WifiRepository(context).setWifiEnabledReturning(enabled)
                    CarCommandResponse(
                        command = cmd.command,
                        ok = ok,
                        error = if (ok) null else "setWifiEnabled($enabled) bị từ chối (cần build system?)",
                    )
                } catch (e: Exception) {
                    CarCommandResponse(command = cmd.command, ok = false, error = e.message)
                }
            }
            "set_hvac_ac" -> hvacBoolCommand(context, cmd) { it.setAc(requireNotNull(cmd.enabled)) }
            "set_hvac_recirc" -> hvacBoolCommand(context, cmd) { it.setRecirc(requireNotNull(cmd.enabled)) }
            "set_hvac_eco" -> hvacBoolCommand(context, cmd) { it.setEco(requireNotNull(cmd.enabled)) }
            "set_hvac_defrost" -> hvacBoolCommand(context, cmd) { it.setDefrost(requireNotNull(cmd.enabled)) }
            "set_hvac_temp" -> {
                val temp = cmd.floatValue
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "floatValue missing")
                val ctrl = hvacController
                    ?: CarPropertyHvacController(context).also { hvacController = it }
                val result = ctrl.setTemperature(temp)
                CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
            }
            "set_hvac_fan" -> {
                val level = cmd.modeValue
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "modeValue missing")
                val ctrl = hvacController
                    ?: CarPropertyHvacController(context).also { hvacController = it }
                val result = ctrl.setFanSpeed(level)
                CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
            }
            "set_scene" -> {
                val mode = cmd.modeValue
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "modeValue missing")
                val repo = sceneRepository
                    ?: SceneRepository(context).also { sceneRepository = it }
                val result = repo.setScene(mode)
                CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
            }
            "set_door_lock" -> {
                val locked = cmd.enabled
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "enabled missing")
                // Khóa: an toàn khi đang chạy. Mở khóa: chỉ khi đỗ (chống mở trộm khi xe lăn bánh).
                if (!locked) {
                    parkedGateError(context)?.let {
                        return CarCommandResponse(command = cmd.command, ok = false, error = it)
                    }
                }
                val ctrl = doorWindowController
                    ?: CarPropertyDoorWindowController(context).also { doorWindowController = it }
                val result = ctrl.setDoorsLocked(locked)
                CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
            }
            "set_window" -> {
                val selector = cmd.modeValue
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "modeValue (selector) missing")
                // percent: floatValue (0..100) hoặc enabled (true=mở 100 / false=đóng 0).
                val percent = cmd.floatValue?.roundToInt()
                    ?: cmd.enabled?.let { if (it) 100 else 0 }
                    ?: return CarCommandResponse(command = cmd.command, ok = false, error = "floatValue hoặc enabled missing")
                // Kính: luôn chặn khi xe đang chạy.
                parkedGateError(context)?.let {
                    return CarCommandResponse(command = cmd.command, ok = false, error = it)
                }
                val ctrl = doorWindowController
                    ?: CarPropertyDoorWindowController(context).also { doorWindowController = it }
                val result = ctrl.setWindow(selector, percent)
                CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
            }
            "request_status" -> {
                // Phone yêu cầu push lại snapshot mới nhất
                Log.i(TAG, "Phone requested fresh status")
                val freshSnapshot = try {
                    readCarStatus(context)
                } catch (e: Exception) {
                    Log.w(TAG, "request_status: readCarStatus failed", e)
                    current
                }
                current = freshSnapshot
                // Gửi trực tiếp cho client yêu cầu thay vì chỉ trả command_response
                // (client sẽ nhận qua handleMessage -> "status" branch)
                CarCommandResponse(command = cmd.command, ok = true)
            }
            else -> CarCommandResponse(command = cmd.command, ok = false, error = "unknown command: ${cmd.command}")
        }
    }

    /**
     * Chặn điều khiển vật lý (cửa/kính) khi xe không chắc chắn đang đỗ.
     * An toàn khi: gear == "P" HOẶC tốc độ < [SAFE_CONTROL_SPEED_KMH]. Không xác định được → chặn (fail-safe).
     * Trả null nếu cho phép; ngược lại trả chuỗi lý do.
     */
    private fun parkedGateError(context: Context): String? {
        var gear = ""
        var gearOk = false
        try {
            val info = run {
                val reader = vehicleInfoReader
                    ?: CarPropertyVehicleInfoReader(context).also { vehicleInfoReader = it }
                reader.read()
            }
            gear = info.gear
            gearOk = info.gearAvailable
        } catch (e: Exception) {
            Log.w(TAG, "parkedGate gear read failed", e)
        }
        if (gearOk && gear == "P") return null

        var speedKmh = Float.NaN
        var speedOk = false
        try {
            val s = run {
                val reader = speedReader
                    ?: VhalSpeedReaderFactory.create(context).also { speedReader = it }
                reader.readSpeed()
            }
            speedKmh = s.speedKmh
            speedOk = s.isAvailable
        } catch (e: Exception) {
            Log.w(TAG, "parkedGate speed read failed", e)
        }
        if (speedOk && speedKmh < SAFE_CONTROL_SPEED_KMH) return null

        return when {
            speedOk -> "xe đang chạy (${speedKmh.roundToInt()} km/h) — chặn điều khiển cửa/kính khi lái"
            else -> "không xác định được trạng thái đỗ (gear/tốc độ) — chặn để an toàn"
        }
    }

    /** Command HVAC dạng bool (ac/recirc/eco/defrost): validate `enabled`, lấy controller cached, ghi. */
    private inline fun hvacBoolCommand(
        context: Context,
        cmd: CarCommandMessage,
        write: (CarPropertyHvacController) -> CarVhalBindings.WriteProbe,
    ): CarCommandResponse {
        if (cmd.enabled == null) {
            return CarCommandResponse(command = cmd.command, ok = false, error = "enabled missing")
        }
        val ctrl = hvacController
            ?: CarPropertyHvacController(context).also { hvacController = it }
        val result = write(ctrl)
        return CarCommandResponse(command = cmd.command, ok = result.ok, error = result.error)
    }

}
