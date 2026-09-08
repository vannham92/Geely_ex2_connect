# Giao thức Phone Companion — CarTcpServer

Tài liệu tích hợp cho **app điện thoại** kết nối tới head unit Geely EX2 (`geely_ex2_tools`)
để **nhận trạng thái xe** và **gửi lệnh điều khiển**.

Nguồn sự thật phía xe: `app/src/main/java/com/geely/ex2/tools/data/network/CarTcpServer.kt`
(business logic) + `TcpServerLink.kt` / `BtSppServerLink.kt` (transport).

---

## 1. Kết nối

Có **2 transport chạy song song**, nội dung giống hệt nhau — chọn cái nào cũng được, phone
nối qua transport nào thì nhận cùng bản tin đó.

| Thông số | WiFi | Bluetooth |
|----------|------|-----------|
| Transport | TCP raw socket | RFCOMM / SPP |
| Điểm cuối | port **47800** | SDP UUID **`6f1e2a00-47b8-4c1e-9d3a-c0ffee000001`**, service name `GeelyEX2-CarSync` |
| Host | IP head unit trên LAN/hotspot (§2) | thiết bị đã pair (bond từ handsfree/A2DP) |
| Discovery | NSD `_carsync._tcp.` (§2) | SDP theo UUID |
| Điều kiện | phone + xe cùng mạng | pair 1 lần, sau đó tự động |
| Code phía xe | `TcpServerLink.kt` | `BtSppServerLink.kt` |

Chung cho cả hai:

| Thông số | Giá trị |
|----------|---------|
| Framing | **NDJSON** — mỗi bản tin = 1 dòng JSON, kết thúc bằng `\n` (LF) |
| Encoding | UTF-8 |
| Chiều | Song công (full-duplex): xe đẩy status; phone gửi command bất cứ lúc nào |
| Số client | Nhiều client cùng lúc, kể cả trộn 2 transport (server broadcast cho tất cả) |

> **Bluetooth không có `soTimeout`.** Phone phải tự làm idle watchdog: >35s không nhận
> status/ping thì đóng socket để reconnect. Xem
> [../ex2-phone/docs/bluetooth-transport-plan.md](../../ex2-phone/docs/bluetooth-transport-plan.md) §5.4.

Ngay khi client nối, server **gửi 1 snapshot** trạng thái hiện tại (`type: "status"`),
rồi tiếp tục đẩy mỗi khi có thay đổi. Không có bắt tay (handshake), không auth (xem §7).

### Đọc/ghi
- **Đọc**: đọc từng dòng (`\n`), mỗi dòng parse thành 1 JSON object, phân loại theo field `type`.
- **Ghi**: serialize command thành JSON 1 dòng, nối `\n`, `flush()`.

---

## 2. Tìm địa chỉ head unit (discovery)

Head unit **đã đăng ký NSD/mDNS** — phone cùng mạng WiFi tự dò, khỏi hardcode IP.

| Thông số NSD | Giá trị |
|--------------|---------|
| Service type | `_carsync._tcp.` |
| Service name | `GeelyEX2-Car` (có thể bị đổi khi trùng, vd `GeelyEX2-Car (2)` — **dò theo type**, đừng lọc theo name) |
| Port | 47800 (lấy từ `resolvedInfo.port`) |
| Host | lấy từ `resolvedInfo.host` sau khi resolve |

**Phone dò (Android `NsdManager`):**

```kotlin
val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

val discovery = object : NsdManager.DiscoveryListener {
    override fun onServiceFound(info: NsdServiceInfo) {
        if (info.serviceType.contains("_carsync._tcp")) {
            nsd.resolveService(info, object : NsdManager.ResolveListener {
                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    val host = resolved.host.hostAddress   // IP head unit
                    val port = resolved.port               // 47800
                    // -> CarSocketClient(host, port).start()
                }
                override fun onResolveFailed(i: NsdServiceInfo, code: Int) {}
            })
        }
    }
    override fun onDiscoveryStarted(t: String) {}
    override fun onDiscoveryStopped(t: String) {}
    override fun onStartDiscoveryFailed(t: String, code: Int) {}
    override fun onStopDiscoveryFailed(t: String, code: Int) {}
    override fun onServiceLost(info: NsdServiceInfo) {}
}
nsd.discoverServices("_carsync._tcp.", NsdManager.PROTOCOL_DNS_SD, discovery)
```

> Android 13+ (API 33+): discovery phía phone cần quyền `NEARBY_WIFI_DEVICES`
> (khai `android:usesPermissionFlags="neverForLocation"`) hoặc `ACCESS_FINE_LOCATION`.

**Fallback nếu NSD hỏng:** nhập IP tay (lấy `wifiIp` in ra sau khi nối 1 lần), hoặc quét subnet `/24` cổng 47800.

---

## 3. Bản tin XE → PHONE

### 3.1. Status (`type: "status"`)

Đẩy khi client vừa nối và **mỗi khi trạng thái đổi** (event-driven, không poll cố định).

```json
{
  "type": "status",
  "speedKmh": 42.0,
  "speedAvailable": true,
  "batteryPercent": 87.0,
  "batteryAvailable": true,
  "drivingMode": 2,
  "drivingModeAvailable": true,
  "avasMuted": false,
  "avasAvailable": true,
  "regenLevel": 3,
  "regenAvailable": true,
  "ambientLightOn": true,
  "ambientLightAvailable": true,
  "outsideTempC": 28.5,
  "tempAvailable": true,
  "wifiOn": true,
  "wifiIp": "192.168.43.1",
  "wifiAvailable": true,
  "rangeKm": 312.0,
  "rangeAvailable": true,
  "gear": "D",
  "gearAvailable": true,
  "odometerKm": 15230.0,
  "odometerAvailable": true,
  "hvacAvailable": true,
  "hvacAcOn": true,
  "hvacTempC": 24.5,
  "hvacFanSpeed": 3,
  "hvacRecircOn": false,
  "hvacEcoOn": true,
  "hvacDefrostOn": false,
  "tirePressureAvailable": true,
  "tireFlKpa": 240.0,
  "tireFrKpa": 242.0,
  "tireRlKpa": 238.0,
  "tireRrKpa": 205.0,
  "tireFlLow": false,
  "tireFrLow": false,
  "tireRlLow": false,
  "tireRrLow": true,
  "sceneAvailable": true,
  "sceneMode": 0,
  "doorLockAvailable": true,
  "doorsLocked": true,
  "windowAvailable": true,
  "windowFlPercent": 0,
  "windowFrPercent": 0,
  "windowRlPercent": 50,
  "windowRrPercent": 0
}
```

| Field | Kiểu | Ý nghĩa | Điều khiển được? |
|-------|------|---------|------------------|
| `speedKmh` | Float | Tốc độ km/h | Không (sensor) |
| `speedAvailable` | Bool | Có đọc được tốc độ không | |
| `batteryPercent` | Float | Pin % (0–100) | Không (sensor) |
| `batteryAvailable` | Bool | | |
| `drivingMode` | Int | `0`=unknown, `1`=ECO, `2`=COMFORT, `3`=SPORT/DYNAMIC | Có → `set_driving_mode` |
| `drivingModeAvailable` | Bool | | |
| `avasMuted` | Bool | AVAS (âm cảnh báo người đi bộ) đang tắt | Có → `set_avas_mute` |
| `avasAvailable` | Bool | | |
| `regenLevel` | Int | `0`=unknown, `1`=LOW, `2`=MID, `3`=HIGH | Có → `set_regen_level` |
| `regenAvailable` | Bool | | |
| `ambientLightOn` | Bool | Đèn viền nội thất đang bật | Có → `set_ambient_light` |
| `ambientLightAvailable` | Bool | | |
| `outsideTempC` | Float | Nhiệt độ ngoài trời (°C) | Không (sensor) |
| `tempAvailable` | Bool | | |
| `wifiOn` | Bool | **WiFi client (station)** đang bật/bật lên — theo `WifiManager`, **KHÔNG phải SoftAP/hotspot** | Có → `set_wifi` |
| `wifiIp` | String? | IPv4 head unit khi join 1 AP (null nếu tắt) | |
| `wifiAvailable` | Bool | Trạng thái WiFi biết được | |
| `rangeKm` | Float | Quãng đường còn lại (km) — `RANGE_REMAINING` | Không (sensor) |
| `rangeAvailable` | Bool | | |
| `gear` | String | Số hộp số `"P"`/`"R"`/`"N"`/`"D"` (`""`=unknown) — `GEAR_SELECTION` | Không (sensor) |
| `gearAvailable` | Bool | | |
| `odometerKm` | Float | Odometer tổng (km) — `PERF_ODOMETER` | Không (sensor) |
| `odometerAvailable` | Bool | | |
| `hvacAvailable` | Bool | Climate đọc được (build `system` + xe hỗ trợ). `false` → mọi field `hvac*` hiển thị `--` | |
| `hvacAcOn` | Bool | Máy nén A/C đang bật (HVAC_AC_ON 354419973 / area 117) | Có → `set_hvac_ac` |
| `hvacTempC` | Float | Nhiệt độ đặt ghế lái (°C) (HVAC_TEMPERATURE_SET 358614275 / area 1) | Có → `set_hvac_temp` |
| `hvacFanSpeed` | Int | Mức quạt gió (HVAC_FAN_SPEED 356517120 / area 0) | Có → `set_hvac_fan` |
| `hvacRecircOn` | Bool | Tuần hoàn gió trong đang bật (HVAC_RECIRC_ON 354419976 / area 117) | Có → `set_hvac_recirc` |
| `hvacEcoOn` | Bool | Chế độ ECO đang bật (HVAC_FUNC_ECO_SWITCH 268960000 / area 117, adapt-only) | Có → `set_hvac_eco` |
| `hvacDefrostOn` | Bool | Sấy kính trước (max defrost) đang bật (HVAC_MAX_DEFROST_ON 354419985 / area 1) | Có → `set_hvac_defrost` |
| `tirePressureAvailable` | Bool | Áp suất lốp đọc được (≥1 bánh). `false` → mọi field `tire*` hiển thị `--` | |
| `tireFlKpa` | Float | Áp suất lốp trước-trái (kPa) — `TIRE_PRESSURE` area FL | Không (sensor) |
| `tireFrKpa` | Float | Áp suất lốp trước-phải (kPa) — area FR | Không (sensor) |
| `tireRlKpa` | Float | Áp suất lốp sau-trái (kPa) — area RL | Không (sensor) |
| `tireRrKpa` | Float | Áp suất lốp sau-phải (kPa) — area RR | Không (sensor) |
| `tireFlLow` | Bool | Bánh trước-trái áp suất thấp (≤ `CRITICALLY_LOW_TIRE_PRESSURE`) | |
| `tireFrLow` | Bool | Bánh trước-phải áp suất thấp | |
| `tireRlLow` | Bool | Bánh sau-trái áp suất thấp | |
| `tireRrLow` | Bool | Bánh sau-phải áp suất thấp | |
| `sceneAvailable` | Bool | Đọc được scene hiện tại không (best-effort; schema provider SceneDirector ẩn). `false` → `sceneMode` không đáng tin, hiển thị `--` | |
| `sceneMode` | Int | Scene Mode đang chạy: `0`=none/default, `1`=Rest (Nghỉ ngơi), `2`=Camping (Cắm trại) | Có → `set_scene` |
| `doorLockAvailable` | Bool | Đọc được khóa cửa không. `false` → `doorsLocked` không đáng tin, hiển thị `--` | |
| `doorsLocked` | Bool | Mọi cửa đọc được đều **đã khóa** (một cửa mở khóa → `false`) | Có → `set_door_lock` |
| `windowAvailable` | Bool | Đọc được vị trí kính không (cần config max/min). `false` → mọi field `window*` hiển thị `--` | |
| `windowFlPercent` | Int | Kính trước-trái, % mở (`0`=đóng kín, `100`=mở hết) | Có → `set_window` |
| `windowFrPercent` | Int | Kính trước-phải, % mở | Có → `set_window` |
| `windowRlPercent` | Int | Kính sau-trái, % mở | Có → `set_window` |
| `windowRrPercent` | Int | Kính sau-phải, % mở | Có → `set_window` |

**Quy tắc UI**: khi `*Available == false` → hiển thị `--`/disabled; đừng coi giá trị mặc định
(`0`, `false`) là thật. Lưu ý `fan` area 0 và `ECO` adapt-only là chỗ dễ fail, cần xem log khi debug.

### 3.2. Command response (`type: "command_response"`)

Trả lời cho mỗi command phone gửi lên.

```json
{ "type": "command_response", "command": "set_avas_mute", "ok": true, "error": null }
```

| Field | Kiểu | Ý nghĩa |
|-------|------|---------|
| `command` | String | Tên lệnh gốc (hoặc `"unknown"` nếu JSON lỗi) |
| `ok` | Bool | Ghi thành công? |
| `error` | String? | Thông báo lỗi nếu `ok=false` |

Sau khi command `ok=true`, server đọc lại + broadcast 1 `status` mới → phone tự nhận trạng thái cập nhật.

> **`set_wifi` đặc biệt:** `ok` = kết quả thật của `WifiManager.setWifiEnabled()`. Trên build **không có
> quyền hệ thống** (Android 10+) lệnh no-op → `ok=false`, `error="setWifiEnabled(...) bị từ chối (cần
> build system?)"`. Chỉ build `system` (`android.uid.system`) mới đổi được WiFi. Dù `ok` thế nào,
> trạng thái WiFi thật vẫn đến qua `status` kế tiếp (do `WIFI_STATE_CHANGED`) — phone nên tin `status`,
> không suy từ `ok`.

### 3.3. Heartbeat (`type: "ping"`)

```json
{ "type": "ping" }
```

Server gửi mỗi **15 giây**. Dùng để phát hiện kết nối chết. Phone **không cần trả lời**; chỉ cần
coi như "còn sống". Nếu quá lâu (ví dụ > 35s) không nhận status lẫn ping → coi như rớt, reconnect.

---

## 4. Bản tin PHONE → XE (command)

```json
{ "command": "<tên>", "modeValue": <Int?>, "enabled": <Bool?>, "floatValue": <Float?> }
```

| command | Field dùng | Giá trị | Tác dụng |
|---------|-----------|---------|----------|
| `set_driving_mode` | `modeValue` | `1`=ECO, `2`=COMFORT, `3`=SPORT | Đổi chế độ lái |
| `set_regen_level` | `modeValue` | `1`=LOW, `2`=MID, `3`=HIGH | Đổi mức hồi năng lượng |
| `set_avas_mute` | `enabled` | `true`=tắt tiếng AVAS | Bật/tắt mute AVAS |
| `set_ambient_light` | `enabled` | `true`=bật đèn viền | Bật/tắt đèn viền |
| `set_wifi` | `enabled` | `true`=bật WiFi | Bật/tắt **WiFi client (station)**, không phải hotspot |
| `set_hvac_ac` | `enabled` | `true`=bật A/C | Bật/tắt máy nén điều hòa |
| `set_hvac_temp` | `floatValue` | °C (vd `24.5`) | Đặt nhiệt độ ghế lái |
| `set_hvac_fan` | `modeValue` | mức quạt (int) | Đặt tốc độ quạt gió |
| `set_hvac_recirc` | `enabled` | `true`=tuần hoàn trong | Bật/tắt recirc |
| `set_hvac_eco` | `enabled` | `true`=bật ECO | Bật/tắt ECO |
| `set_hvac_defrost` | `enabled` | `true`=bật sấy kính | Bật/tắt max defrost kính trước |
| `set_scene` | `modeValue` | `1`=Rest (Nghỉ ngơi), `2`=Camping (Cắm trại) | Kích hoạt Scene Mode |
| `set_door_lock` | `enabled` | `true`=khóa tất cả cửa, `false`=mở khóa | Khóa/mở khóa 4 cửa |
| `set_window` | `modeValue` + (`floatValue` \| `enabled`) | `modeValue`=selector `0`=all/`1`=FL/`2`=FR/`3`=RL/`4`=RR; `floatValue`=% mở `0..100` (hoặc `enabled` `true`=100/`false`=0) | Hạ/nâng kính |

Ví dụ:
```json
{ "command": "set_driving_mode", "modeValue": 3 }
{ "command": "set_wifi", "enabled": true }
{ "command": "set_scene", "modeValue": 2 }
{ "command": "set_door_lock", "enabled": true }
{ "command": "set_window", "modeValue": 0, "floatValue": 100 }
{ "command": "set_window", "modeValue": 1, "enabled": false }
```

> **⚠️ An toàn — cửa/kính chỉ đổi khi xe đỗ:** `set_window` và `set_door_lock` (khi **mở khóa**) bị
> **chặn nếu xe đang chạy** — chỉ chạy khi `gear=="P"` HOẶC tốc độ `< 3 km/h`. Không đọc được gear
> lẫn tốc độ → chặn (fail-safe). Bị chặn → `ok=false`, `error` nêu lý do. **Khóa cửa** (`enabled=true`)
> luôn cho phép (an toàn khi lái). Ghi cần build `system` (quyền `CONTROL_CAR_DOORS`/`CONTROL_CAR_WINDOWS`);
> build `user` → `ok=false`. Kênh **chưa auth** (§7): điều khiển cửa/kính qua LAN là vector rủi ro —
> chỉ dùng trên hotspot riêng của xe, cân nhắc thêm token trước khi bật trên mạng chung.

> **Scene Mode (`set_scene`):** xe khởi chạy Activity **exported** của `com.flyme.auto.scenedirector`
> (`START_REST` → RestActivity, `START_CAMPING` → CampingActivity). Chỉ `modeValue` `1`/`2` hợp lệ;
> giá trị khác → `ok=false`. **Không có lệnh thoát scene** qua companion (tài liệu SceneDirector không
> mô tả Intent thoát) — người dùng thoát trên màn xe. `ok=true` nghĩa là đã bắn Intent thành công
> (không đảm bảo scene đã hiển thị nếu SceneDirector từ chối precondition, vd đang chạy xe). `sceneMode`
> trong `status` kế tiếp phản ánh trạng thái thật (đọc best-effort — có thể `sceneAvailable=false`).

Field không dùng có thể bỏ qua (null). Server bỏ qua field lạ (`ignoreUnknownKeys`).

> **HVAC cần build `system`:** các lệnh `set_hvac_*` ghi VHAL climate (quyền `CONTROL_CAR_CLIMATE` +
> `CAR_VENDOR_EXTENSION`). Build `user` → `ok=false`. Sau `ok=true` server refresh + broadcast `status`
> mới. `set_hvac_temp` dùng `floatValue` (°C). `set_hvac_eco` là adapt-only prop → có thể `ok=false`
> tùy ECU dù climate reachable; phone nên tin `status` kế tiếp, không suy từ `ok`.

---

## 5. Vòng đời kết nối phía phone (khuyến nghị)

1. **Connect**: mở `Socket(host, 47800)`, đặt `tcpNoDelay=true`, `keepAlive=true`.
2. **Read loop** (thread/coroutine riêng): `readLine()` liên tục → parse `type` → cập nhật state.
3. **Nhận snapshot đầu** ngay sau connect → render trạng thái ban đầu.
4. **Gửi command** khi user thao tác → chờ `command_response` (timeout ~3s) để báo ok/lỗi.
5. **Reconnect**: `readLine()` trả `null` hoặc ném exception → đóng socket, đợi theo
   **exponential backoff** (1s → 2s → 4s … tối đa 30s), nối lại. Reset backoff khi connect thành công.
6. **Watchdog** (tuỳ chọn): nếu > 35s không nhận gì (kể cả ping) → chủ động đóng để trigger reconnect.

---

## 6. Reference client (Kotlin, copy sang app phone)

Phụ thuộc: `org.jetbrains.kotlinx:kotlinx-serialization-json` + plugin `kotlin("plugin.serialization")`,
`kotlinx-coroutines`. Quyền `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`.

### 6.1. DTO (giống hệt phía xe)

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class CarStatusMessage(
    val type: String = "status",
    val speedKmh: Float = 0f,
    val speedAvailable: Boolean = false,
    val batteryPercent: Float = 0f,
    val batteryAvailable: Boolean = false,
    val drivingMode: Int = 0,
    val drivingModeAvailable: Boolean = false,
    val avasMuted: Boolean = false,
    val avasAvailable: Boolean = false,
    val regenLevel: Int = 0,
    val regenAvailable: Boolean = false,
    val ambientLightOn: Boolean = false,
    val ambientLightAvailable: Boolean = false,
    val outsideTempC: Float = 0f,
    val tempAvailable: Boolean = false,
    val wifiOn: Boolean = false,
    val wifiIp: String? = null,
    val wifiAvailable: Boolean = false,
    val rangeKm: Float = 0f,
    val rangeAvailable: Boolean = false,
    val gear: String = "",
    val gearAvailable: Boolean = false,
    val odometerKm: Float = 0f,
    val odometerAvailable: Boolean = false,
    val hvacAvailable: Boolean = false,
    val hvacAcOn: Boolean = false,
    val hvacTempC: Float = 0f,
    val hvacFanSpeed: Int = 0,
    val hvacRecircOn: Boolean = false,
    val hvacEcoOn: Boolean = false,
    val hvacDefrostOn: Boolean = false,
    val tirePressureAvailable: Boolean = false,
    val tireFlKpa: Float = 0f,
    val tireFrKpa: Float = 0f,
    val tireRlKpa: Float = 0f,
    val tireRrKpa: Float = 0f,
    val tireFlLow: Boolean = false,
    val tireFrLow: Boolean = false,
    val tireRlLow: Boolean = false,
    val tireRrLow: Boolean = false,
    val sceneAvailable: Boolean = false,
    val sceneMode: Int = 0,   // 0=none, 1=Rest, 2=Camping
    val doorLockAvailable: Boolean = false,
    val doorsLocked: Boolean = false,
    val windowAvailable: Boolean = false,
    val windowFlPercent: Int = 0,  // 0=đóng, 100=mở hết
    val windowFrPercent: Int = 0,
    val windowRlPercent: Int = 0,
    val windowRrPercent: Int = 0,
)

@Serializable
data class CarCommandMessage(
    val command: String,
    val modeValue: Int? = null,
    val enabled: Boolean? = null,
    val floatValue: Float? = null,
)

@Serializable
data class CarCommandResponse(
    val type: String = "command_response",
    val command: String,
    val ok: Boolean = false,
    val error: String? = null,
)
```

### 6.2. Socket client + auto-reconnect

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

class CarSocketClient(
    private val host: String,
    private val port: Int = 47800,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _status = MutableStateFlow<CarStatusMessage?>(null)
    val status: StateFlow<CarStatusMessage?> = _status

    private val _connection = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connection: StateFlow<ConnectionState> = _connection

    private val _responses = MutableStateFlow<CarCommandResponse?>(null)
    val responses: StateFlow<CarCommandResponse?> = _responses

    @Volatile private var socket: Socket? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true
        scope.launch { connectLoop() }
    }

    fun stop() {
        running = false
        closeSocket()
        _connection.value = ConnectionState.DISCONNECTED
    }

    /** Trả false nếu chưa kết nối. Kết quả thật đến qua [responses]. */
    fun send(cmd: CarCommandMessage): Boolean {
        val out = output ?: return false
        return try {
            val line = json.encodeToString(CarCommandMessage.serializer(), cmd) + "\n"
            synchronized(out) { out.write(line.toByteArray(Charsets.UTF_8)); out.flush() }
            true
        } catch (_: Exception) { false }
    }

    // Tiện ích
    fun setDrivingMode(mode: Int) = send(CarCommandMessage("set_driving_mode", modeValue = mode))
    fun setRegenLevel(level: Int) = send(CarCommandMessage("set_regen_level", modeValue = level))
    fun setAvasMute(muted: Boolean) = send(CarCommandMessage("set_avas_mute", enabled = muted))
    fun setAmbientLight(on: Boolean) = send(CarCommandMessage("set_ambient_light", enabled = on))
    fun setWifi(on: Boolean) = send(CarCommandMessage("set_wifi", enabled = on))
    fun setHvacAc(on: Boolean) = send(CarCommandMessage("set_hvac_ac", enabled = on))
    fun setHvacTemp(tempC: Float) = send(CarCommandMessage("set_hvac_temp", floatValue = tempC))
    fun setHvacFan(level: Int) = send(CarCommandMessage("set_hvac_fan", modeValue = level))
    fun setHvacRecirc(on: Boolean) = send(CarCommandMessage("set_hvac_recirc", enabled = on))
    fun setHvacEco(on: Boolean) = send(CarCommandMessage("set_hvac_eco", enabled = on))
    fun setHvacDefrost(on: Boolean) = send(CarCommandMessage("set_hvac_defrost", enabled = on))
    fun setScene(mode: Int) = send(CarCommandMessage("set_scene", modeValue = mode)) // 1=Rest, 2=Camping
    fun setDoorLock(locked: Boolean) = send(CarCommandMessage("set_door_lock", enabled = locked))
    /** selector: 0=all,1=FL,2=FR,3=RL,4=RR; percent 0..100 (0=đóng). Chỉ chạy khi xe đỗ. */
    fun setWindow(selector: Int, percent: Int) =
        send(CarCommandMessage("set_window", modeValue = selector, floatValue = percent.toFloat()))

    private suspend fun connectLoop() {
        var attempt = 0
        while (running) {
            _connection.value = ConnectionState.CONNECTING
            try {
                val s = Socket().apply {
                    tcpNoDelay = true
                    keepAlive = true
                    connect(InetSocketAddress(host, port), 5_000)
                }
                socket = s
                output = s.getOutputStream()
                _connection.value = ConnectionState.CONNECTED
                attempt = 0

                val reader = s.getInputStream().bufferedReader()
                while (running) {
                    val line = reader.readLine() ?: break   // null = server đóng
                    if (line.isBlank()) continue
                    handleLine(line)
                }
            } catch (e: Exception) {
                // nuốt để reconnect
            } finally {
                closeSocket()
                _connection.value = ConnectionState.DISCONNECTED
            }
            if (!running) break
            // exponential backoff: 1s, 2s, 4s ... tối đa 30s
            val delayMs = min(30_000L, 1_000L * (1L shl min(attempt, 5)))
            attempt++
            delay(delayMs)
        }
    }

    private fun handleLine(line: String) {
        val obj = runCatching { json.parseToJsonElement(line) as? JsonObject }.getOrNull() ?: return
        when (obj["type"]?.jsonPrimitive?.content) {
            "status" -> runCatching {
                _status.value = json.decodeFromString(CarStatusMessage.serializer(), line)
            }
            "command_response" -> runCatching {
                _responses.value = json.decodeFromString(CarCommandResponse.serializer(), line)
            }
            "ping" -> { /* heartbeat, bỏ qua */ }
        }
    }

    private fun closeSocket() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        output = null
    }
}
```

### 6.3. Dùng trong ViewModel

```kotlin
val client = CarSocketClient(host = "192.168.43.1")
client.start()

// Quan sát trạng thái xe
lifecycleScope.launch {
    client.status.collect { s ->
        s ?: return@collect
        if (s.batteryAvailable) updateBattery(s.batteryPercent)
        if (s.speedAvailable) updateSpeed(s.speedKmh)
        // ...
    }
}

// Gửi lệnh khi user bấm
client.setDrivingMode(3)   // SPORT
client.setWifi(true)
```

---

## 7. Bảo mật (quan trọng)

- **Không có auth**. Bất kỳ thiết bị nào cùng LAN nối được `:47800` đều điều khiển được xe.
- Chấp nhận được nếu chỉ chạy trên **hotspot riêng của xe** (client tin cậy).
- Nếu head unit dùng chung WiFi công cộng → **rủi ro**. Nên bổ sung (cả 2 phía):
  - Token/handshake: bản tin đầu phone gửi `{"command":"auth","token":"..."}`; server từ chối nếu sai.
  - Hoặc chỉ bind loopback + tunnel, hoặc allowlist IP.
- Chưa có mã hoá (plaintext). Dữ liệu xe không nhạy cảm nhưng lệnh điều khiển thì có.

---

## 8. Test nhanh không cần app (netcat)

Cùng mạng với head unit:

```bash
# Xem status chảy ra + ping mỗi 15s
nc <ip-head-unit> 47800

# Gửi lệnh (giữ kết nối để thấy response)
echo '{"command":"set_avas_mute","enabled":true}' | nc <ip-head-unit> 47800
```

Nối được + thấy `{"type":"status",...}` và `{"type":"command_response",...}` → server OK.

---

## 9. Giới hạn hiện tại

- **AVAS đổi từ UI xe** không có on-change callback → chỉ push lại khi có sự kiện khác hoặc
  sau command AVAS. (Muốn live tuyệt đối: subscribe prop AVAS trong `AvasConstants`.)
- **Discovery: đã có NSD** (`_carsync._tcp:47800`) — phone cùng mạng tự dò (§2).
- **Chưa có auth** (§7) — chấp nhận trên mạng tin cậy.
- Chế độ lái chỉ 3 mode (ECO/COMFORT/SPORT) — đúng theo `DrivingMode.selectable` của app EX2.
- **WiFi = client (station)**, không phải SoftAP/hotspot. `wifiIp` là IP head unit khi join AP ngoài.
  Nếu phone nối qua hotspot của xe thì `wifiIp` ≠ địa chỉ dùng để reach server, và `set_wifi=false` chỉ
  tắt WiFi client. Đồng bộ với topology mạng trước khi dựa vào field này.
- **`set_wifi` chỉ đổi được trên build `system`** (Android 10+ chặn `setWifiEnabled` cho app thường);
  build `user` trả `ok=false`.
- Các cấu hình chỉ-của-head-unit (widget rank, lịch đèn viền, auto-enable WiFi) **không** đồng bộ
  vì không phải trạng thái xe.
- **Cửa/kính cần build `system` + xe hỗ trợ**: `set_door_lock`/`set_window` ghi VHAL `DOOR_LOCK`/`WINDOW_POS`
  (quyền `CONTROL_CAR_DOORS`/`CONTROL_CAR_WINDOWS`). `window*Percent` quy đổi từ config max/min — xe không
  khai config → `windowAvailable=false`. Ghi cửa/kính **bị chặn khi xe đang chạy** (gate parked, xem §4).
  Không có on-change callback cho cửa/kính → dựa periodic refresh 5s + refresh sau command.
- **Scene Mode đọc best-effort**: `sceneMode` lấy từ ContentProvider/`Settings.Secure` của SceneDirector,
  schema cột không tài liệu hoá chắc → có thể `sceneAvailable=false` dù scene đang chạy. Kích hoạt
  (`set_scene`) thì tin cậy (start Activity exported). **Không thoát scene** được qua companion. Trên
  build `user` vẫn kích hoạt được (Intent công khai), không như HVAC/WiFi cần build `system`.
