package com.example.ex2_phone.data.network

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Chủ sở hữu kết nối ở mức **tiến trình** — repository + vòng reconnect + lựa chọn transport.
 *
 * Trước đây `CarViewModel` giữ những thứ này, nên đóng app là mất kết nối. Giờ cả UI lẫn
 * `CarConnectionService` cùng dùng MỘT manager, nhờ vậy service chạy nền giữ được socket sống
 * khi không còn Activity nào.
 *
 * Vòng nối chạy khi có ít nhất một [Owner] giữ chỗ qua [retain]; owner cuối cùng [release] thì
 * dừng hẳn và đóng socket. Nhờ đó mở app trong lúc service đang chạy **không** tạo kết nối thứ hai.
 *
 * Mọi hàm đổi trạng thái đều `@Synchronized` — receiver Bluetooth chạy trên main thread còn
 * service/UI có thể gọi từ thread khác.
 */
object CarConnectionManager {

    /** Ai đang cần kết nối. Vòng nối sống khi tập này khác rỗng. */
    enum class Owner { UI, SERVICE }

    private lateinit var appContext: Context
    private lateinit var prefs: TransportPrefs
    private lateinit var statePrefs: SharedPreferences

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val owners = mutableSetOf<Owner>()

    private var connectJob: Job? = null
    private var initialized = false

    /**
     * Repository duy nhất của tiến trình. `internal` vì chỉ ViewModel/Service được chạm —
     * composable đi qua `CarViewModel` (coding-rules §1).
     */
    internal lateinit var repository: CarRepository
        private set

    private val _connectionError = MutableStateFlow(false)
    val connectionError: StateFlow<Boolean> = _connectionError.asStateFlow()

    private val _transportMode = MutableStateFlow(TransportMode.BLUETOOTH)
    val transportMode: StateFlow<TransportMode> = _transportMode.asStateFlow()

    private val _btDevice = MutableStateFlow<BondedDevice?>(null)
    val btDevice: StateFlow<BondedDevice?> = _btDevice.asStateFlow()

    private val _autoBackground = MutableStateFlow(false)
    val autoBackground: StateFlow<Boolean> = _autoBackground.asStateFlow()

    val carStatus: StateFlow<CarStatusMessage> get() = repository.carStatus
    val connected: StateFlow<Boolean> get() = repository.connected
    val lastRawJson: StateFlow<String> get() = repository.lastRawJson
    val activeLink: StateFlow<CarLink?> get() = repository.activeLink

    /**
     * Kênh phản hồi lệnh. Là `Channel` → **một consumer duy nhất** (ViewModel).
     * Service không được collect, nếu không hai bên sẽ giành mất bản tin của nhau.
     */
    val commandResponses: Flow<CarCommandResponse> get() = repository.commandResponses

    /** Gọi được nhiều lần; chỉ lần đầu có tác dụng. */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        prefs = TransportPrefs(appContext)
        statePrefs = appContext.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        repository = CarRepository(loadLastState())
        _transportMode.value = prefs.mode
        _btDevice.value = prefs.btDevice
        _autoBackground.value = prefs.autoBackground
        scope.launch { repository.carStatus.collect { saveLastState(it) } }
        initialized = true
        Log.i(TAG, "Khởi tạo xong")
    }

    @Synchronized
    fun retain(owner: Owner) {
        if (!owners.add(owner)) return
        Log.i(TAG, "retain($owner) → owners=$owners")
        if (connectJob == null) startLoop()
    }

    @Synchronized
    fun release(owner: Owner) {
        if (!owners.remove(owner)) return
        Log.i(TAG, "release($owner) → owners=$owners")
        if (owners.isEmpty()) stopLoop()
    }

    /**
     * Đổi IP head unit rồi nối lại.
     *
     * `MainApp` gọi hàm này mỗi lần mở app, nên IP không đổi mà đang nối ngon thì phải bỏ qua —
     * nếu không, mở app sẽ cắt đứt kết nối mà service đang giữ. Chưa nối thì cứ thử lại ngay
     * (đúng ý nút "Kết nối").
     */
    @Synchronized
    fun setCarIp(ipAddress: String) {
        val changed = prefs.carIp != ipAddress
        prefs.carIp = ipAddress
        if (changed || !repository.connected.value) restart()
    }

    @Synchronized
    fun setTransportMode(mode: TransportMode) {
        if (_transportMode.value == mode) return
        prefs.mode = mode
        _transportMode.value = mode
        restart()
    }

    @Synchronized
    fun setBtDevice(device: BondedDevice?) {
        prefs.btDevice = device
        _btDevice.value = device
        restart()
    }

    /** Đã tự xin quyền thông báo lần đầu chưa. Chỉ là chỗ chứa cờ, không liên quan kết nối. */
    var notificationAsked: Boolean
        get() = prefs.notificationAsked
        set(value) {
            prefs.notificationAsked = value
        }

    /** Bật/tắt việc tự chạy nền khi head unit Bluetooth kết nối. Không tự nối lại. */
    @Synchronized
    fun setAutoBackground(enabled: Boolean) {
        prefs.autoBackground = enabled
        _autoBackground.value = enabled
    }

    /** Danh sách thiết bị đã pair. Rỗng nếu chưa cấp `BLUETOOTH_CONNECT`. */
    fun bondedDevices(): List<BondedDevice> {
        if (!hasBluetoothConnect()) return emptyList()
        return BtSppCarLink.bondedDevices(appContext)
    }

    /** Adapter Bluetooth của máy đang bật. `false` cả khi máy không có Bluetooth. */
    fun bluetoothEnabled(): Boolean = BtSppCarLink.adapterOrNull(appContext)?.isEnabled == true

    fun hasBluetoothConnect(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Dò IP head unit = gateway của mạng WiFi đang nối (protocol §2: khi phone nối hotspot do xe
     * phát, IP xe = địa chỉ gateway). Trả `null` nếu không có gateway.
     */
    @Suppress("DEPRECATION") // dhcpInfo đủ dùng cho minSdk 26, IPv4 hotspot.
    fun detectGatewayIp(): String? {
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val gateway = wifi.dhcpInfo?.gateway ?: 0
        if (gateway == 0) return null
        // dhcpInfo trả int little-endian (byte thấp = octet đầu).
        return "%d.%d.%d.%d".format(
            gateway and 0xff,
            gateway shr 8 and 0xff,
            gateway shr 16 and 0xff,
            gateway shr 24 and 0xff,
        )
    }

    // ---------------------------------------------------------------- vòng nối

    private fun restart() {
        stopLoop()
        if (owners.isNotEmpty()) startLoop()
    }

    private fun stopLoop() {
        connectJob?.cancel()
        connectJob = null
        // Cancel coroutine KHÔNG bung được readLine() đang block — phải đóng socket.
        if (initialized) repository.disconnect()
    }

    /** Mở link của transport đang chọn, bám tới khi đứt, chờ 3s rồi thử lại. */
    private fun startLoop() {
        connectJob = scope.launch {
            while (isActive) {
                val links = buildLinks()
                if (links.isEmpty()) {
                    _connectionError.value = true
                    delay(RETRY_DELAY_MS)
                    continue
                }
                var connectedAny = false
                for (link in links) {
                    try {
                        _connectionError.value = false
                        repository.setLink(link)
                        repository.connect()   // suspend tới khi đứt
                        connectedAny = true
                        break                  // đã nối được rồi rớt → chờ rồi thử lại từ đầu
                    } catch (e: Exception) {
                        Log.w(TAG, "${link.label} thất bại: ${e.message}")
                    }
                }
                if (!connectedAny) _connectionError.value = true
                delay(RETRY_DELAY_MS)
            }
        }
    }

    /** Gọi lại mỗi vòng (không cache): Bluetooth có thể vừa được bật, IP có thể vừa đổi. */
    private fun buildLinks(): List<CarLink> = when (_transportMode.value) {
        TransportMode.BLUETOOTH -> listOfNotNull(btLinkOrNull())
        TransportMode.WIFI -> listOfNotNull(tcpLinkOrNull())
    }

    private fun btLinkOrNull(): CarLink? {
        if (!hasBluetoothConnect()) return null
        val device = _btDevice.value ?: return null
        val adapter = BtSppCarLink.adapterOrNull(appContext) ?: return null
        if (!adapter.isEnabled) return null
        return BtSppCarLink(appContext, device.address, device.name)
    }

    private fun tcpLinkOrNull(): CarLink? {
        val ip = prefs.carIp
        return if (ip.isBlank()) null else TcpCarLink(ip)
    }

    // ------------------------------------------------------- trạng thái lần cuối

    private fun loadLastState(): CarStatusMessage {
        val saved = statePrefs.getString(KEY_LAST_STATUS, null) ?: return CarStatusMessage()
        return try {
            json.decodeFromString(CarStatusMessage.serializer(), saved)
        } catch (e: Exception) {
            Log.e(TAG, "Đọc trạng thái lần cuối thất bại", e)
            CarStatusMessage()
        }
    }

    private fun saveLastState(status: CarStatusMessage) {
        try {
            statePrefs.edit()
                .putString(KEY_LAST_STATUS, json.encodeToString(CarStatusMessage.serializer(), status))
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Lưu trạng thái lần cuối thất bại", e)
        }
    }

    private const val TAG = "CarConnManager"
    private const val RETRY_DELAY_MS = 3_000L
    private const val STATE_PREFS = "car_prefs"
    private const val KEY_LAST_STATUS = "last_car_status"
}
