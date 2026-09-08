# Phone Companion Protocol — CarTcpServer (nguồn sự thật phía xe)

Tài liệu tích hợp để **app điện thoại `ex2-phone`** kết nối tới head unit Geely EX2
(`geely_ex2_tools`), **nhận trạng thái xe** và **gửi lệnh điều khiển**.

> **Nguồn sự thật phía xe:** `geely_ex2_tools/app/src/main/java/com/geely/ex2/tools/data/network/CarTcpServer.kt`.
> Doc này mô tả đúng những gì server đó đang triển khai. Khi server đổi DTO/command → cập nhật doc.

---

## 0. ⚠️ Thay đổi lớn — xe đã bỏ Ktor WebSocket, chuyển sang TCP NDJSON

> ✅ **Đã migrate xong** (app `ex2-phone`): `data/network/CarRepository.kt` giờ là TCP `java.net.Socket`
> NDJSON, đúng giao thức bên dưới. Bảng dưới giữ lại để đối chiếu cũ/mới và cho ai port app khác.

| | Xe CŨ (Ktor — app phone đang theo) | Xe MỚI (`CarTcpServer` — hiện tại) |
|---|---|---|
| Transport | WebSocket `ws://<ip>:8080/ws/car` | **TCP raw `<ip>:47800`** |
| Framing | Ktor `Frame.Text` | **NDJSON** — 1 dòng JSON + `\n` |
| Status có thêm | `acOn`, `acTemp`, `fanSpeed`, `estimatedRange`, `totalDistance` | + `outsideTempC`, `wifi*`, `rangeKm`, `gear`, `odometerKm`, `hvac*` (đưa lại), `tire*` |
| Command có thêm | `toggle_ac`, `set_ac_temp`, `set_fan_speed` | + `set_wifi`, `set_hvac_*` (thay tên khí hậu cũ), `request_status` |
| Heartbeat | không | `{"type":"ping"}` mỗi 15s |

**Trạng thái hiện tại (migration + telemetry đã xong):**
- **DTO** — `CarStatusMessage` = giao thức TCP đầy đủ: telemetry lõi + `outsideTempC`, `wifi*`,
  `rangeKm`, `gear`, `odometerKm`, **`hvac*` (khí hậu đã dùng lại)**, `tire*`, `debugInfo`.
  `CarCommandMessage` = `command/modeValue/enabled/floatValue`.
- **Repository/ViewModel** — TCP socket + NDJSON (§6.2); đủ sender `set_driving_mode`, `set_regen_level`,
  `set_avas_mute`, `set_ambient_light`, `set_wifi`, `set_hvac_*`, `request_status`.
- **UI** — HVAC screen chạy trên `hvac*` (không còn dùng `toggle_ac/set_ac_temp/set_fan_speed` cũ);
  telemetry temp/wifi/range/gear/odometer/tire render theo `*Available`.

> Lịch sử: từng có giai đoạn bỏ hẳn khí hậu khi mới chuyển sang `CarTcpServer`. Nay head unit
> **expose lại** dưới tên chuẩn hoá `set_hvac_*` — xem §3.1/§4.

> Cleartext: TCP raw **không** dính luật `usesCleartextTraffic` của Android (luật đó chỉ chặn HTTP/WS
> qua `HttpClient`). Chuyển sang `java.net.Socket` là hết lỗi cleartext luôn.

---

## 1. Kết nối

| Thông số | Giá trị |
|----------|---------|
| Transport | TCP (raw socket) |
| Port | **47800** (`CarTcpServer.PORT`) |
| Host | IP head unit trên LAN/hotspot (xem §2) |
| Framing | **NDJSON** — mỗi bản tin = 1 dòng JSON, kết thúc `\n` (LF) |
| Encoding | UTF-8 |
| Chiều | Song công: xe đẩy status; phone gửi command bất cứ lúc nào |
| Số client | Nhiều client cùng lúc (server broadcast cho tất cả) |

Ngay khi client nối, server **gửi 1 snapshot** trạng thái hiện tại (`type:"status"`), rồi tiếp tục
đẩy mỗi khi trạng thái đổi. **Không** handshake, **không** auth (xem §7).

### Đọc/ghi
- **Đọc:** đọc từng dòng (`\n`) → parse mỗi dòng thành 1 JSON object → phân loại theo field `type`.
- **Ghi:** serialize command thành JSON 1 dòng, nối `\n`, `flush()`.

---

## 2. Tìm địa chỉ head unit (discovery)

Server **chưa** đăng ký NSD/mDNS. Hai cách lấy IP:

1. **Hotspot gateway (đơn giản nhất)** — nếu phone nối vào hotspot do head unit phát, IP head unit =
   địa chỉ gateway (thường cố định, ví dụ `192.168.43.1`). Lấy gateway từ
   `WifiManager`/`ConnectivityManager` rồi nối `gateway:47800`.
2. **Nhập tay / cấu hình** — cho user nhập IP head unit một lần, lưu vào
   `SharedPreferences("app_prefs")` key `ip_address` (đúng convention app hiện có).

> TODO tương lai: head unit đăng ký NSD (`_carsync._tcp`, port 47800) → phone dùng
> `NsdManager.discoverServices()` thay vì hardcode IP.

---

## 3. Bản tin XE → PHONE

### 3.1. Status (`type: "status"`)

Đẩy khi client vừa nối và **mỗi khi trạng thái đổi** (event-driven qua VHAL on-change callback +
WiFi broadcast; speed throttle theo km/h nguyên). Không poll cố định.

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
  "odometerKm": 15420.0,
  "odometerAvailable": true,
  "hvacAvailable": true,
  "hvacAcOn": true,
  "hvacTempC": 22.0,
  "hvacFanSpeed": 4,
  "hvacRecircOn": false,
  "hvacEcoOn": false,
  "hvacDefrostOn": false,
  "tirePressureAvailable": true,
  "tireFlKpa": 240.0,
  "tireFrKpa": 240.0,
  "tireRlKpa": 235.0,
  "tireRrKpa": 235.0,
  "tireFlLow": false,
  "tireFrLow": false,
  "tireRlLow": false,
  "tireRrLow": false,
  "debugInfo": ""
}
```

| Field | Kiểu | Ý nghĩa | Điều khiển được? |
|-------|------|---------|------------------|
| `speedKmh` | Float | Tốc độ km/h | Không (sensor) |
| `speedAvailable` | Bool | Đọc được tốc độ không | |
| `batteryPercent` | Float | Pin % (0–100) | Không (sensor) |
| `batteryAvailable` | Bool | | |
| `drivingMode` | Int | `0`=unknown, `1`=ECO, `2`=COMFORT, `3`=SPORT/DYNAMIC | Có → `set_driving_mode` |
| `drivingModeAvailable` | Bool | | |
| `avasMuted` | Bool | AVAS (âm cảnh báo người đi bộ) đang tắt tiếng | Có → `set_avas_mute` |
| `avasAvailable` | Bool | | |
| `regenLevel` | Int | `0`=unknown, `1`=LOW, `2`=MID, `3`=HIGH | Có → `set_regen_level` |
| `regenAvailable` | Bool | | |
| `ambientLightOn` | Bool | Đèn viền nội thất đang bật | Có → `set_ambient_light` |
| `ambientLightAvailable` | Bool | | |
| `outsideTempC` | Float | Nhiệt độ ngoài trời (°C) | Không (sensor) |
| `tempAvailable` | Bool | | |
| `wifiOn` | Bool | WiFi/hotspot đang bật (hoặc đang bật lên) | Có → `set_wifi` |
| `wifiIp` | String? | IP WiFi (`null` nếu tắt) | |
| `wifiAvailable` | Bool | Trạng thái WiFi biết được | |
| `rangeKm` | Float | Quãng đường ước tính còn lại (km) | Không (sensor) |
| `rangeAvailable` | Bool | | |
| `gear` | String | Số hiện tại (`P`/`R`/`N`/`D`, `""` nếu chưa biết) | Không (sensor) |
| `gearAvailable` | Bool | | |
| `odometerKm` | Float | Số km tổng (odometer) | Không (sensor) |
| `odometerAvailable` | Bool | | |
| `hvacAcOn` | Bool | Điều hoà (A/C) đang bật | Có → `set_hvac_ac` |
| `hvacTempC` | Float | Nhiệt độ đặt HVAC (°C) | Có → `set_hvac_temp` |
| `hvacFanSpeed` | Int | Tốc độ quạt (`1`–`8`) | Có → `set_hvac_fan` |
| `hvacRecircOn` | Bool | Lấy gió trong (recirculate) | Có → `set_hvac_recirc` |
| `hvacEcoOn` | Bool | Chế độ ECO của HVAC | Có → `set_hvac_eco` |
| `hvacDefrostOn` | Bool | Sấy kính (defrost) | Có → `set_hvac_defrost` |
| `hvacAvailable` | Bool | HVAC đọc/điều khiển được | |
| `tireFlKpa` | Float | Áp suất lốp trước-trái (kPa) | Không (sensor) |
| `tireFrKpa` | Float | Áp suất lốp trước-phải (kPa) | Không (sensor) |
| `tireRlKpa` | Float | Áp suất lốp sau-trái (kPa) | Không (sensor) |
| `tireRrKpa` | Float | Áp suất lốp sau-phải (kPa) | Không (sensor) |
| `tireFlLow`/`tireFrLow`/`tireRlLow`/`tireRrLow` | Bool | Cảnh báo lốp non tương ứng | |
| `tirePressureAvailable` | Bool | Đọc được áp suất lốp | |
| `debugInfo` | String | Chuỗi debug từ xe (lý do reader fail); `""` nếu không có | |

**Quy tắc UI (`*Available`):** khi `xxxAvailable == false` → hiển thị `--`/disabled; đừng coi giá
trị mặc định (`0`, `false`) là thật. (Đúng như luật §7 trong `AGENTS.md`.)

> **Server-authoritative:** render từ `status` xe đẩy về, **không** từ command vừa gửi. Sau command
> `ok=true`, xe tự đọc lại + broadcast `status` mới → UI tự cập nhật. (Slider đang kéo có thể preview
> cục bộ.)

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

Sau `ok=true`, server đọc lại + broadcast 1 `status` mới → phone tự nhận trạng thái cập nhật.

### 3.3. Heartbeat (`type: "ping"`)

```json
{ "type": "ping" }
```

Server gửi mỗi **15 giây**. Phone **không cần trả lời**; chỉ coi như "còn sống". Nếu > 35s không
nhận cả status lẫn ping → coi như rớt, reconnect.

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
| `set_wifi` | `enabled` | `true`=bật WiFi | Bật/tắt WiFi/hotspot |
| `set_hvac_ac` | `enabled` | `true`=bật A/C | Bật/tắt điều hoà |
| `set_hvac_temp` | `floatValue` | °C (bước 1°) | Đặt nhiệt độ HVAC |
| `set_hvac_fan` | `modeValue` | `1`–`8` | Đặt tốc độ quạt |
| `set_hvac_recirc` | `enabled` | `true`=lấy gió trong | Bật/tắt recirculate |
| `set_hvac_eco` | `enabled` | `true`=bật ECO | Bật/tắt HVAC ECO |
| `set_hvac_defrost` | `enabled` | `true`=bật sấy kính | Bật/tắt defrost |
| `request_status` | *(không)* | | Yêu cầu xe push lại snapshot mới nhất |

Ví dụ:
```json
{ "command": "set_driving_mode", "modeValue": 3 }
{ "command": "set_wifi", "enabled": true }
{ "command": "set_hvac_temp", "floatValue": 22.0 }
{ "command": "set_hvac_fan", "modeValue": 4 }
{ "command": "request_status" }
```

Field không dùng bỏ qua (`null`). Server bỏ qua field lạ (`ignoreUnknownKeys`). Phone gửi domain
`1/2/3`; server tự map sang mã Flyme thô trước khi ghi VHAL.

> **Lịch sử:** bản Ktor cũ dùng `toggle_ac`/`set_ac_temp`/`set_fan_speed`. Head unit `CarTcpServer`
> có giai đoạn bỏ hẳn khí hậu, sau **đưa lại** dưới tên chuẩn hoá `set_hvac_*` (bảng trên). Các tên
> lệnh cũ đã ngừng — gửi lên nhận `command_response` `ok=false, error="unknown command: ..."`.

---

## 5. Vòng đời kết nối phía phone (khuyến nghị)

1. **Connect:** `Socket(host, 47800)`, đặt `tcpNoDelay=true`, `keepAlive=true`, timeout connect ~5s.
2. **Read loop** (coroutine riêng, `Dispatchers.IO`): `readLine()` liên tục → parse `type` → cập nhật state.
3. **Snapshot đầu** đến ngay sau connect → render trạng thái ban đầu.
4. **Gửi command** khi user thao tác → chờ `command_response` (timeout ~3s) để báo ok/lỗi.
5. **Reconnect:** `readLine()` trả `null` hoặc ném exception → đóng socket, đợi **exponential backoff**
   (1s → 2s → 4s … tối đa 30s), nối lại. Reset backoff khi connect thành công.
6. **Watchdog** (tuỳ chọn): > 35s không nhận gì (kể cả ping) → chủ động đóng để trigger reconnect.

---

## 6. Reference client (Kotlin — copy thẳng sang app phone)

Phụ thuộc: `org.jetbrains.kotlinx:kotlinx-serialization-json` + plugin `kotlin("plugin.serialization")`,
`kotlinx-coroutines`. Quyền: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`.
Đúng layering repo: đặt DTO + `CarSocketClient` trong `data/network/`; ViewModel gọi qua đây, UI không chạm socket.

### 6.1. DTO (khớp 1:1 với `CarTcpServer.kt`)

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class CarStatusMessage(
    val type: String = "status",
    val speedKmh: Float = 0f,
    val speedAvailable: Boolean = false,
    val batteryPercent: Float = 0f,
    val batteryAvailable: Boolean = false,
    val drivingMode: Int = 0,            // 0=unknown 1=ECO 2=COMFORT 3=SPORT
    val drivingModeAvailable: Boolean = false,
    val avasMuted: Boolean = false,
    val avasAvailable: Boolean = false,
    val regenLevel: Int = 0,             // 0=unknown 1=LOW 2=MID 3=HIGH
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
    val gear: String = "",               // P/R/N/D, "" = chưa biết
    val gearAvailable: Boolean = false,
    val odometerKm: Float = 0f,
    val odometerAvailable: Boolean = false,
    // HVAC
    val hvacAvailable: Boolean = false,
    val hvacAcOn: Boolean = false,
    val hvacTempC: Float = 0f,
    val hvacFanSpeed: Int = 0,           // 1..8
    val hvacRecircOn: Boolean = false,
    val hvacEcoOn: Boolean = false,
    val hvacDefrostOn: Boolean = false,
    // Áp suất lốp (kPa)
    val tirePressureAvailable: Boolean = false,
    val tireFlKpa: Float = 0f,
    val tireFrKpa: Float = 0f,
    val tireRlKpa: Float = 0f,
    val tireRrKpa: Float = 0f,
    val tireFlLow: Boolean = false,
    val tireFrLow: Boolean = false,
    val tireRlLow: Boolean = false,
    val tireRrLow: Boolean = false,
    // Debug từ xe: lý do từng reader fail
    val debugInfo: String = "",
)

@Serializable
data class CarCommandMessage(
    val command: String,
    val modeValue: Int? = null,
    val enabled: Boolean? = null,
    val floatValue: Float? = null,       // dùng cho set_hvac_temp
)

@Serializable
data class CarCommandResponse(
    val type: String = "command_response",
    val command: String = "",
    val ok: Boolean = false,
    val error: String? = null,
)
```

### 6.2. Socket client + auto-reconnect

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val status: StateFlow<CarStatusMessage?> = _status.asStateFlow()

    private val _connection = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _responses = MutableStateFlow<CarCommandResponse?>(null)
    val responses: StateFlow<CarCommandResponse?> = _responses.asStateFlow()

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

    // Helper — 1 command 1 method, tên khớp protocol string.
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
    fun requestStatus() = send(CarCommandMessage("request_status"))

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
            } catch (_: Exception) {
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
val client = CarSocketClient(host = "192.168.43.1") // hoặc IP từ SharedPreferences
client.start()

// Quan sát trạng thái xe
viewModelScope.launch {
    client.status.collect { s ->
        s ?: return@collect
        if (s.batteryAvailable) updateBattery(s.batteryPercent)
        if (s.speedAvailable) updateSpeed(s.speedKmh)
        if (s.tempAvailable) updateTemp(s.outsideTempC)
        // ... honor *Available cho mỗi field
    }
}

// Gửi lệnh khi user bấm (fire-and-forget)
client.setDrivingMode(3)   // SPORT
client.setWifi(true)
```

---

## 7. Bảo mật (quan trọng)

- **Không có auth.** Bất kỳ thiết bị nào cùng LAN nối được `:47800` đều điều khiển được xe.
- Chấp nhận được nếu chỉ chạy trên **hotspot riêng của xe** (client tin cậy).
- Nếu head unit dùng chung WiFi công cộng → **rủi ro**. Nên bổ sung cả 2 phía:
  - Token/handshake: bản tin đầu phone gửi `{"command":"auth","token":"..."}`; server từ chối nếu sai.
  - Hoặc bind loopback + tunnel, hoặc allowlist IP.
- Chưa mã hoá (plaintext). Dữ liệu xe không nhạy cảm nhưng **lệnh điều khiển** thì có.

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

- **AVAS đổi từ UI xe** không có on-change callback → chỉ push lại khi có sự kiện khác hoặc sau
  command AVAS.
- **Chưa có discovery (NSD)** — phải biết IP head unit (§2).
- **Chưa có auth** (§7).
- Chế độ lái chỉ 3 mode (ECO/COMFORT/SPORT) — theo `DrivingMode.selectable` của app EX2.
- **HVAC/range/gear/odometer/áp suất lốp đã được đưa lại** (khác giai đoạn giữa từng bỏ). Vẫn honor
  `hvacAvailable`/`rangeAvailable`/… trước khi render.
- Cấu hình chỉ-của-head-unit (widget rank, lịch đèn viền, auto-enable WiFi) **không** đồng bộ.
