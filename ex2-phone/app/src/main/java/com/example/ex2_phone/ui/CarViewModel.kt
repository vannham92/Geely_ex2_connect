package com.example.ex2_phone.ui

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ex2_phone.data.network.BondedDevice
import com.example.ex2_phone.data.network.CarConnectionManager
import com.example.ex2_phone.data.network.CarStatusMessage
import com.example.ex2_phone.data.network.TransportKind
import com.example.ex2_phone.data.network.TransportMode
import com.example.ex2_phone.service.CarConnectionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lớp trình bày. **Không** sở hữu kết nối nữa — socket + vòng reconnect nằm ở
 * [CarConnectionManager] mức tiến trình, để `CarConnectionService` giữ được kết nối khi app đóng.
 * ViewModel chỉ giữ chỗ ([CarConnectionManager.Owner.UI]), mirror flow và lo phần UI-only
 * (optimistic update, latch cửa kính, snackbar lỗi lệnh).
 */
class CarViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = CarConnectionManager.also { it.init(application) }

    /** Đường gửi lệnh. Cùng một instance với service đang chạy nền — chỉ có một socket. */
    private val repository get() = manager.repository

    private val _carStatus = MutableStateFlow(manager.carStatus.value)
    val carStatus: StateFlow<CarStatusMessage> = _carStatus.asStateFlow()

    val connected: StateFlow<Boolean> get() = manager.connected

    val connectionError: StateFlow<Boolean> get() = manager.connectionError

    val lastRawJson: StateFlow<String> get() = manager.lastRawJson

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Thông báo lệnh thất bại (one-shot) → UI hiện Snackbar. extraBufferCapacity để emit không suspend.
    private val _commandError = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val commandError: SharedFlow<String> = _commandError.asSharedFlow()

    val transportMode: StateFlow<TransportMode> get() = manager.transportMode

    val btDevice: StateFlow<BondedDevice?> get() = manager.btDevice

    /** Có tự chạy nền khi head unit Bluetooth kết nối hay không. */
    val autoBackground: StateFlow<Boolean> get() = manager.autoBackground

    /** Transport đang chạy; `null` khi chưa nối. UI hiện "Bluetooth" / "WiFi …". */
    val activeTransport: StateFlow<TransportKind?> = manager.activeLink
        .map { it?.kind }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeTransportLabel: StateFlow<String?> = manager.activeLink
        .map { it?.label }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Đang thực sự chạy trên Bluetooth. Cờ riêng để UI không phải import `TransportKind`
     * (coding-rules §1) — chỉ tuỳ chọn chạy nền dùng tới, vì nó chỉ có nghĩa với Bluetooth.
     */
    val bluetoothActive: StateFlow<Boolean> = manager.activeLink
        .map { it?.kind == TransportKind.BLUETOOTH }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Latch đích của từng cửa kính (index 0..3 = FL, FR, RL, RR); `null` = không có lệnh đang chạy.
     *
     * Kính chạy mất vài giây và xe broadcast status liên tục trong lúc đó. Nếu UI bám theo
     * từng frame trung gian thì thanh trượt sẽ nhảy loạn (ra lệnh mở 90%, đang ở 40% → UI tụt về 40%).
     * Nên khi có lệnh, UI giữ nguyên đích cho tới khi kính tới nơi (hoặc quá [WINDOW_MOVE_TIMEOUT_MS]).
     *
     * Phải khai báo TRƯỚC `init` — `viewModelScope` chạy trên `Dispatchers.Main.immediate` nên
     * collector `manager.carStatus` phát giá trị đầu ngay trong `init`, lúc đó property khai báo
     * sau vẫn còn null.
     */
    private val windowTargets = arrayOfNulls<Int>(4)
    private val windowDeadlines = LongArray(4)

    init {
        manager.retain(CarConnectionManager.Owner.UI)

        viewModelScope.launch {
            manager.carStatus.collect { status ->
                _carStatus.value = applyWindowTargets(status)
            }
        }
        // Nối được bằng Bluetooth trong lúc app mở → bật service để kết nối sống tiếp khi
        // app đóng, không phải chờ lần ACL_CONNECTED sau. Chỉ với Bluetooth, vì đó là transport
        // duy nhất có sự kiện ngắt (ACL_DISCONNECTED) để dừng service lại.
        viewModelScope.launch {
            combine(manager.connected, activeTransport, manager.autoBackground) { on, kind, auto ->
                on && kind == TransportKind.BLUETOOTH && auto
            }
                .distinctUntilChanged()
                .collect { shouldRunInBackground ->
                    if (shouldRunInBackground) CarConnectionService.start(getApplication())
                }
        }
        // Lệnh thất bại → Snackbar + xin lại status để rollback optimistic UI.
        viewModelScope.launch {
            manager.commandResponses.collect { response ->
                if (!response.ok) {
                    Log.w(TAG, "Command ${response.command} failed: ${response.error}. Requesting status to rollback...")
                    // Lệnh hỏng → bỏ latch kính để status thật ghi đè lại UI optimistic.
                    if (response.command == "set_window") clearWindowTargets()
                    _commandError.tryEmit(commandErrorMessage(response.command, response.error))
                    repository.requestStatus()
                }
            }
        }
    }

    fun requestStatus() {
        if (!connected.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            repository.requestStatus()
            delay(500) // Giữ refresh indicator tối thiểu 500ms cho mượt
            _isRefreshing.value = false
        }
    }

    /** Đổi IP head unit rồi nối lại. Giữ tên cũ để `MainApp` không phải đổi. */
    fun connect(ipAddress: String) = manager.setCarIp(ipAddress)

    fun setTransportMode(mode: TransportMode) = manager.setTransportMode(mode)

    fun setBtDevice(device: BondedDevice?) = manager.setBtDevice(device)

    /**
     * Bật/tắt chạy nền. Tắt thì dừng luôn service đang chạy — nếu không nó sẽ sống tới lần
     * ACL_DISCONNECTED kế tiếp dù người dùng vừa tắt tính năng.
     */
    fun setAutoBackground(enabled: Boolean) {
        manager.setAutoBackground(enabled)
        if (!enabled) CarConnectionService.stop(getApplication())
    }

    /**
     * Thông báo đang bị chặn → notification thường trực của service sẽ không hiện ra, user mất
     * chỗ duy nhất thấy trạng thái khi app đóng. Kiểm cả hai mức: toàn app và riêng kênh
     * [CarConnectionService.CHANNEL_ID] (user tắt được từng kênh trong Cài đặt).
     */
    /** Quyền runtime `POST_NOTIFICATIONS`. Trước API 33 không có quyền này nên luôn coi là có. */
    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            getApplication<Application>().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Lần đầu vào Cài đặt thì tự xin quyền; từ chối rồi thì thôi, để user tự bấm. */
    fun shouldAutoAskNotification(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !manager.notificationAsked &&
            !hasNotificationPermission()

    /** Đã từng bắn dialog xin quyền thông báo hay chưa — dùng để đoán còn hỏi được nữa không. */
    fun notificationAsked(): Boolean = manager.notificationAsked

    fun markNotificationAsked() {
        manager.notificationAsked = true
    }

    fun notificationsBlocked(): Boolean {
        val notifications = NotificationManagerCompat.from(getApplication())
        if (!notifications.areNotificationsEnabled()) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val channel = notifications.getNotificationChannel(CarConnectionService.CHANNEL_ID)
        return channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE
    }

    /** Danh sách thiết bị đã pair. Rỗng nếu chưa cấp `BLUETOOTH_CONNECT`. */
    fun bondedDevices(): List<BondedDevice> = manager.bondedDevices()

    fun hasBluetoothConnect(): Boolean = manager.hasBluetoothConnect()

    /** Bluetooth của máy đang bật hay không — UI hỏi trước khi mở danh sách thiết bị. */
    fun bluetoothEnabled(): Boolean = manager.bluetoothEnabled()

    fun setDrivingMode(modeValue: Int) {
        _carStatus.value = _carStatus.value.copy(drivingMode = modeValue)
        viewModelScope.launch { repository.setDrivingMode(modeValue) }
    }

    fun setWifi(enabled: Boolean) {
        _carStatus.value = _carStatus.value.copy(wifiOn = enabled)
        viewModelScope.launch { repository.setWifi(enabled) }
    }

    fun setAvasMute(muted: Boolean) {
        _carStatus.value = _carStatus.value.copy(avasMuted = muted)
        viewModelScope.launch { repository.setAvasMute(muted) }
    }

    fun setRegenLevel(level: Int) {
        _carStatus.value = _carStatus.value.copy(regenLevel = level)
        viewModelScope.launch { repository.setRegenLevel(level) }
    }

    fun setAmbientLight(enabled: Boolean) {
        _carStatus.value = _carStatus.value.copy(ambientLightOn = enabled)
        viewModelScope.launch { repository.setAmbientLight(enabled) }
    }

    fun setHvacAc(on: Boolean) {
        _carStatus.value = _carStatus.value.copy(hvacAcOn = on)
        viewModelScope.launch { repository.setHvacAc(on) }
    }

    fun setHvacTemp(tempC: Float) {
        _carStatus.value = _carStatus.value.copy(hvacTempC = tempC)
        viewModelScope.launch { repository.setHvacTemp(tempC) }
    }

    fun setHvacFan(level: Int) {
        _carStatus.value = _carStatus.value.copy(hvacFanSpeed = level)
        viewModelScope.launch { repository.setHvacFan(level) }
    }

    fun setHvacRecirc(on: Boolean) {
        _carStatus.value = _carStatus.value.copy(hvacRecircOn = on)
        viewModelScope.launch { repository.setHvacRecirc(on) }
    }

    fun setHvacEco(on: Boolean) {
        _carStatus.value = _carStatus.value.copy(hvacEcoOn = on)
        viewModelScope.launch { repository.setHvacEco(on) }
    }

    fun setHvacDefrost(on: Boolean) {
        _carStatus.value = _carStatus.value.copy(hvacDefrostOn = on)
        viewModelScope.launch { repository.setHvacDefrost(on) }
    }

    /** Làm mát nhanh: hạ nhiệt độ về [QUICK_COOL_TEMP_C] và đẩy quạt lên [QUICK_FAN_SPEED]. */
    fun quickCool() = applyHvacPreset(QUICK_COOL_TEMP_C, QUICK_FAN_SPEED)

    /** Làm ấm nhanh: nâng nhiệt độ lên [QUICK_WARM_TEMP_C] và đẩy quạt lên [QUICK_FAN_SPEED]. */
    fun quickWarm() = applyHvacPreset(QUICK_WARM_TEMP_C, QUICK_FAN_SPEED)

    /** Gửi liên tiếp set_hvac_temp + set_hvac_fan. Hành động một lần, không có trạng thái bật/tắt. */
    private fun applyHvacPreset(tempC: Float, fanSpeed: Int) {
        _carStatus.value = _carStatus.value.copy(hvacTempC = tempC, hvacFanSpeed = fanSpeed)
        viewModelScope.launch {
            repository.setHvacTemp(tempC)
            repository.setHvacFan(fanSpeed)
        }
    }

    fun setScene(mode: Int) {
        // optimistic update
        _carStatus.value = _carStatus.value.copy(sceneMode = mode)
        viewModelScope.launch { repository.setScene(mode) }
    }

    fun setDoorLock(locked: Boolean) {
        _carStatus.value = _carStatus.value.copy(doorsLocked = locked)
        viewModelScope.launch { repository.setDoorLock(locked) }
    }

    private fun clearWindowTargets() {
        for (i in windowTargets.indices) windowTargets[i] = null
    }

    /** Ghi đè phần trăm kính bằng đích đang chạy tới; nhả latch khi tới nơi hoặc hết hạn. */
    private fun applyWindowTargets(status: CarStatusMessage): CarStatusMessage {
        if (windowTargets.all { it == null }) return status
        val now = SystemClock.elapsedRealtime()
        val actual = intArrayOf(
            status.windowFlPercent, status.windowFrPercent,
            status.windowRlPercent, status.windowRrPercent,
        )
        val shown = IntArray(4)
        for (i in 0..3) {
            val target = windowTargets[i]
            if (target == null) {
                shown[i] = actual[i]
                continue
            }
            val reached = kotlin.math.abs(actual[i] - target) <= WINDOW_REACHED_TOLERANCE
            if (reached || now > windowDeadlines[i]) {
                windowTargets[i] = null
                shown[i] = actual[i]
            } else {
                shown[i] = target
            }
        }
        return status.copy(
            windowFlPercent = shown[0], windowFrPercent = shown[1],
            windowRlPercent = shown[2], windowRrPercent = shown[3],
        )
    }

    /** selector: 0=all, 1=FL, 2=FR, 3=RL, 4=RR; percent = độ mở: 0=đóng, 100=mở hết. */
    fun setWindow(selector: Int, percent: Int) {
        // Latch đích để status trung gian trong lúc kính chạy không kéo UI ngược lại
        val deadline = SystemClock.elapsedRealtime() + WINDOW_MOVE_TIMEOUT_MS
        val targeted = if (selector == 0) 0..3 else (selector - 1)..(selector - 1)
        for (i in targeted) {
            if (i !in 0..3) continue
            windowTargets[i] = percent
            windowDeadlines[i] = deadline
        }

        // optimistic update theo selector
        val s = _carStatus.value
        _carStatus.value = when (selector) {
            0 -> s.copy(windowFlPercent = percent, windowFrPercent = percent, windowRlPercent = percent, windowRrPercent = percent)
            1 -> s.copy(windowFlPercent = percent)
            2 -> s.copy(windowFrPercent = percent)
            3 -> s.copy(windowRlPercent = percent)
            4 -> s.copy(windowRrPercent = percent)
            else -> s
        }
        viewModelScope.launch { repository.setWindow(selector, percent) }
    }

    /**
     * Dò IP head unit = gateway của mạng WiFi đang nối (đúng cách 1 trong protocol §2:
     * khi phone nối hotspot do xe phát, IP xe = địa chỉ gateway). Trả `null` nếu không có gateway
     * (chưa nối WiFi / gateway = 0). Không cần quyền mới — đã có ACCESS_WIFI_STATE.
     */
    fun detectGatewayIp(): String? = manager.detectGatewayIp()

    /**
     * Nhả chỗ giữ. Kết nối chỉ thật sự đóng khi service cũng đã nhả — đóng app trong lúc service
     * chạy nền thì socket vẫn sống.
     */
    override fun onCleared() {
        super.onCleared()
        manager.release(CarConnectionManager.Owner.UI)
        Log.i(TAG, "ViewModel cleared")
    }

    private fun commandErrorMessage(command: String, error: String?): String {
        val name = when (command) {
            "set_driving_mode" -> "chế độ lái"
            "set_regen_level" -> "mức thu hồi năng lượng"
            "set_avas_mute" -> "AVAS"
            "set_ambient_light" -> "đèn viền"
            "set_wifi" -> "WiFi"
            "set_hvac_ac" -> "điều hoà"
            "set_hvac_temp" -> "nhiệt độ"
            "set_hvac_fan" -> "quạt gió"
            "set_hvac_recirc" -> "lấy gió trong"
            "set_hvac_eco" -> "ECO điều hoà"
            "set_hvac_defrost" -> "sấy kính"
            "set_door_lock" -> "khoá cửa"
            "set_window" -> "cửa kính"
            else -> command
        }
        return if (error.isNullOrBlank()) "Đổi $name thất bại" else "Đổi $name thất bại: $error"
    }

    companion object {
        private const val TAG = "CarViewModel"

        /** Hạn tối đa giữ latch kính. Kính chạy hết hành trình ~5s; hết hạn thì trả UI về số thật. */
        private const val WINDOW_MOVE_TIMEOUT_MS = 20_000L

        /** Sai số coi như kính đã tới đích (xe hiếm khi dừng đúng số tròn). */
        private const val WINDOW_REACHED_TOLERANCE = 2

        /** Cấu hình cho hai nút làm mát/làm ấm nhanh ở màn hình Điều hoà. */
        private const val QUICK_COOL_TEMP_C = 18f
        private const val QUICK_WARM_TEMP_C = 31f
        private const val QUICK_FAN_SPEED = 7
    }
}
