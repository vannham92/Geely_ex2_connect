# Plan — thêm Bluetooth SPP làm transport song song với TCP WiFi

> **Trạng thái: ✅ Phase 0–3 ĐÃ CODE XONG cả 2 phía (2026-08-02).** Cả 2 app build sạch.
> **Chưa chạy thử Bluetooth end-to-end trên xe thật** — mới verify tới mức POC ping/echo.
>
> - Phase 0 (POC): PASS trên xe thật. HU Geely EX2 cho app bên thứ 3 mở RFCOMM server socket.
> - Phase 1–3 (phone): xong — xem §5–§7. Phía xe: xong — xem §9. POC 2 bên đã xoá.
>
> Tài liệu này giờ là **mô tả thiết kế đã hiện thực**, không còn là kế hoạch. Đọc để hiểu
> vì sao code như vậy trước khi sửa.
>
> Đọc kèm: [phone-companion-protocol.md](phone-companion-protocol.md) (spec DTO — **không đổi**),
> [architecture.md](architecture.md), [coding-rules.md](coding-rules.md).

### ⚠️ 3 điểm quan trọng nhất — đọc trước khi làm gì

1. ~~**Phase 0 là cổng chặn.**~~ **✅ Đã PASS 2026-08-02** — HU cho mở RFCOMM server.
   Bỏ qua §4, vào thẳng Phase 1.
2. **Idle watchdog bắt buộc thêm.** `BluetoothSocket` không có `soTimeout` — code TCP
   hiện tại đang dựa vào `soTimeout = 35s` để bắt half-open. Thiếu watchdog thì BT rớt
   sẽ **treo im lặng** — `readLine()` block vĩnh viễn, coroutine cancel không cắt được.
   Xem §5.4.
3. **Giữ một `CarRepository` sống suốt đời ViewModel + `setLink()`.** Đừng tạo repo mới
   mỗi vòng reconnect — collector trong `CarViewModel.connect()` bám instance cũ, tạo mới
   là leak. Xem §6.2.

---

## 1. Mục tiêu

Hiện tại phone nối xe qua **TCP `<carIp>:47800`**. Nhược điểm: user phải bật WiFi trên xe và
đưa phone vào cùng mạng mỗi lần lên xe.

Thêm **Bluetooth SPP (RFCOMM)** làm transport thứ hai:

- Phone và head unit (HU) **đã pair sẵn** cho handsfree/A2DP → app mở thêm một kênh SPP riêng
  trên cùng bond đó. **User không phải thao tác gì**, không popup, không cần cùng mạng.
- **WiFi giữ nguyên**, không bỏ.
- User chọn được transport; mặc định **ưu tiên Bluetooth**, fallback WiFi.

### Không nằm trong scope

- **iOS.** SPP/RFCOMM không mở cho app bên thứ 3 (cần MFi). Chỉ Android.
- **BLE GATT.** Không làm — status JSON ~1.5KB, GATT phải chunk theo MTU, phức tạp vô ích.
- **Đổi DTO / protocol.** `CarStatusMessage` / `CarCommandMessage` / `CarCommandResponse` và
  framing NDJSON **giữ nguyên 1:1**. BT chỉ thay ống dẫn, không thay nội dung.

---

## 2. Bất biến — không được phá

| Bất biến | Lý do |
|---|---|
| DTO khớp 1:1 với `CarTcpServer.kt` bên xe | đổi field = breaking change 2 phía |
| Framing NDJSON: 1 JSON/dòng, `\n`, UTF-8 | chung cho cả TCP lẫn SPP |
| UUID SPP = `6f1e2a00-47b8-4c1e-9d3a-c0ffee000001` | phải khớp `BtSppPoc.SERVICE_UUID` bên xe |
| Layer: UI → `CarViewModel` → `CarRepository` → transport | UI không được biết đang chạy BT hay TCP |
| Chỉ 1 kết nối tại một thời điểm | HU phục vụ tuần tự |

---

## 3. Trạng thái hiện tại (đã có sẵn trong repo)

### Phía xe (`geely_ex2_tools`)

**✅ Đã xong — xem §9.** Transport đã tách; SPP đẩy data thật, UUID
`6f1e2a00-47b8-4c1e-9d3a-c0ffee000001`. POC bên xe đã xóa.

### Phía phone (`ex2-phone`)

| File | Vai trò |
|---|---|
| `data/network/CarRepository.kt` | tự tạo `java.net.Socket`, đọc NDJSON, parse, gửi command — **transport dính vào repo** |
| `ui/CarViewModel.kt` | vòng reconnect 3s, mirror StateFlow, optimistic update |
| `ui/HomeScreen.kt` | tổng quan telemetry |
| `ui/DrivingScreen.kt` | thông tin lái xe (speed, gear, range) |
| `ui/ControlScreen.kt` | điều khiển driving mode, regen, AVAS, ambient light, WiFi |
| `ui/HvacScreen.kt` | điều hoà HVAC |
| `ui/DashboardScreen.kt` | dashboard tổng hợp |
| `ui/SettingsScreen.kt` | IP, màu xe, POC BT |
| `data/network/BtSppPocClient.kt` | **POC** client RFCOMM, gửi `{"ping":1}`, so echo |
| `ui/components/BtSppPocCard.kt` | UI POC |

---

## 4. Phase 0 — verify POC ✅ XONG (2026-08-02)

**Kết quả: PASS.** Phone log `connected` → `TX: {"ping":1}` → `RX: {"ping":1}` → `PASS`.
HU cho phép mở RFCOMM server, secure `createRfcommSocketToServiceRecord` chạy được
(không cần fallback insecure). Phần dưới giữ lại để tham khảo cách chạy lại.

Code đã xong. Chỉ cần build lại và cài.

> ⚠️ Hai flavor `user` và `system` **dùng chung `applicationId`**. HU đang chạy bản `system`
> (platform-signed, `android.uid.system`). **Đừng** cài `installUserDebug` đè lên — xung đột
> UID/chữ ký, phải `adb uninstall` trước và mất bản system đang chạy. Luôn build bản `system`.

Xe:

```bash
cd geely_ex2_tools && pwsh ./scripts/release-system-apk.ps1
adb push install/out/geely-ex2-tools-system-platform-signed.apk /data/local/tmp/geely-ex2-tools-system.apk
adb shell pm install -r -g /data/local/tmp/geely-ex2-tools-system.apk   # -g: grant sẵn BLUETOOTH_CONNECT
```

Phone:

```bash
cd ex2-phone && ./gradlew :app:installDebug
```

1. Xe: Cài đặt → **Bluetooth SPP (POC)** → Start.
2. Phone: Cài đặt → **Bluetooth SPP (POC)** → cấp quyền → bấm tên head unit.

### Bảng quyết định

| Log | Kết luận | Hành động |
|---|---|---|
| Phone `PASS ✅` | HU cho mở RFCOMM server | **làm tiếp Phase 1** |
| Xe `FAIL listen: SecurityException` | thiếu quyền | grant tay, thử lại |
| Xe `FAIL listen: IOException` | stack BT của HU chặn | **DỪNG** — BT không khả thi |
| Xe `OK listen`, phone `read failed, socket might closed` | SDP record không quảng bá được | thử `createInsecureRfcommSocketToServiceRecord` (§8); vẫn fail → **DỪNG** |
| Phone timeout 12s | HU không listen hoặc SDP không thấy UUID | kiểm tra UUID 2 phía khớp chưa |

**Không viết một dòng Phase 1 nào trước khi có `PASS ✅`.**

---

## 5. Phase 1 — tách transport khỏi `CarRepository`

Mục tiêu: `CarRepository` không còn biết `java.net.Socket` là gì. Nó chỉ nhận một cặp
`InputStream`/`OutputStream` đã mở.

### 5.1 File mới: `data/network/CarLink.kt`

```kotlin
enum class TransportKind { BLUETOOTH, WIFI }

/** Một cặp stream duplex đã mở. Framing NDJSON do CarRepository lo. */
class CarStreams(val input: InputStream, val output: OutputStream)

/**
 * Ống dẫn tới head unit. Chỉ lo mở/đóng kết nối vật lý — không biết gì về JSON.
 */
interface CarLink {
    val kind: TransportKind
    /** Nhãn hiển thị: "Bluetooth (Geely EX2)" / "WiFi 192.168.43.1". */
    val label: String
    /** Mở kết nối. Ném exception nếu fail — caller reconnect. */
    suspend fun open(): CarStreams
    /** Đóng — phải bung được readLine() đang block (đóng socket, không chỉ cancel coroutine). */
    fun close()
}
```

### 5.2 File mới: `data/network/TcpCarLink.kt`

Bê nguyên phần tạo socket đang nằm trong `CarRepository.connect()`
([CarRepository.kt:145-157](../app/src/main/java/com/example/ex2_phone/data/network/CarRepository.kt)):

```kotlin
class TcpCarLink(private val carIp: String) : CarLink {
    override val kind = TransportKind.WIFI
    override val label = "WiFi $carIp"

    @Volatile private var socket: Socket? = null

    override suspend fun open(): CarStreams = withContext(Dispatchers.IO) {
        val s = Socket().apply {
            tcpNoDelay = true
            keepAlive = true
            connect(InetSocketAddress(carIp, PORT), CONNECT_TIMEOUT_MS)
            soTimeout = SOCKET_READ_TIMEOUT_MS   // idle watchdog, xem §5.4
        }
        socket = s
        CarStreams(s.getInputStream(), s.getOutputStream())
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    companion object {
        const val PORT = 47800
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val SOCKET_READ_TIMEOUT_MS = 35_000
    }
}
```

### 5.3 File mới: `data/network/BtSppCarLink.kt`

```kotlin
class BtSppCarLink(
    private val context: Context,
    private val deviceAddress: String,
    private val deviceName: String,
) : CarLink {
    override val kind = TransportKind.BLUETOOTH
    override val label = "Bluetooth $deviceName"

    @Volatile private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")   // caller đảm bảo đã có BLUETOOTH_CONNECT
    override suspend fun open(): CarStreams = withContext(Dispatchers.IO) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            ?: throw IOException("no BluetoothAdapter")
        if (!adapter.isEnabled) throw IOException("Bluetooth đang tắt")

        runCatching { adapter.cancelDiscovery() }   // discovery làm connect chậm/fail
        val s = adapter.getRemoteDevice(deviceAddress)
            .createRfcommSocketToServiceRecord(SERVICE_UUID)
        socket = s

        // connect() là blocking IO — coroutine cancel KHÔNG cắt được.
        // Watchdog đóng socket để bung nó ra bằng IOException.
        val watchdog = launch { delay(CONNECT_TIMEOUT_MS); runCatching { s.close() } }
        try { s.connect() } finally { watchdog.cancel() }

        CarStreams(s.inputStream, s.outputStream)
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    companion object {
        /** PHẢI khớp BtSppPoc.SERVICE_UUID bên xe. */
        val SERVICE_UUID: UUID = UUID.fromString("6f1e2a00-47b8-4c1e-9d3a-c0ffee000001")
        private const val CONNECT_TIMEOUT_MS = 12_000L
    }
}
```

### 5.4 Sửa `CarRepository.kt`

Đổi constructor: `CarRepository(carIp: String, ...)` → `CarRepository(link: CarLink, ...)`.

Giữ nguyên: `handleMessage()`, `sendCommand()`, toàn bộ `setXxx()`, `_carStatus`, `_connected`,
`_commandResponses`, `_lastRawJson`, logic retry `request_status` 500ms/3s/10s.

Đổi `connect()`:

```kotlin
suspend fun connect() = withContext(Dispatchers.IO) {
    val streams = link.open()           // ← thay cho Socket()
    output = streams.output
    _connected.value = true
    // ... requestJob giữ nguyên ...
    try {
        val reader = streams.input.bufferedReader()
        while (true) {
            val line = reader.readLine() ?: break
            lastRxAt = SystemClock.elapsedRealtime()    // ← mới, cho watchdog §5.4
            if (line.isBlank()) continue
            handleMessage(line)
        }
    } finally {
        requestJob.cancel()
        _connected.value = false
        output = null
        link.close()
    }
}
```

**⚠️ Idle watchdog — BẮT BUỘC thêm.** TCP đang dựa `soTimeout = 35s` để phát hiện half-open.
`BluetoothSocket` **không có `soTimeout`** → `readLine()` block vĩnh viễn khi BT rớt mà không có FIN.
Phải tự làm watchdog, dùng chung cho cả 2 transport:

```kotlin
// trong connect(), song song với vòng đọc
val idleWatchdog = launch {
    while (isActive) {
        delay(5_000)
        if (SystemClock.elapsedRealtime() - lastRxAt > IDLE_TIMEOUT_MS) {
            Log.w(TAG, "idle >${IDLE_TIMEOUT_MS}ms — đóng link để reconnect")
            link.close()      // bung readLine()
            break
        }
    }
}
```

`IDLE_TIMEOUT_MS = 35_000` (xe ping mỗi 15s). Sau khi có watchdog này, `soTimeout` bên
`TcpCarLink` là dư nhưng cứ giữ — hai lớp bảo vệ, không hại gì.

### 5.5 Sửa `CarViewModel.connect()`

Đổi chữ ký `connect(ipAddress: String)` → `connect(link: CarLink)`. Phần còn lại
(mirror flow, vòng reconnect 3s, `commandResponses`) **không đổi**.

### ✅ Nghiệm thu Phase 1

App chạy y hệt hiện tại qua `TcpCarLink`, không đổi hành vi, không đổi UI.
`BtSppCarLink` đã tồn tại nhưng chưa ai gọi. Commit riêng phase này.

---

## 6. Phase 2 — chọn transport + ưu tiên Bluetooth

### 6.1 Prefs

`SharedPreferences("app_prefs")` (đã có, chứa `ip_address`, `car_color`) thêm:

| Key | Kiểu | Ý nghĩa |
|---|---|---|
| `transport_mode` | String | `AUTO` (mặc định) / `BLUETOOTH` / `WIFI` |
| `bt_device_address` | String? | MAC head unit user đã chọn |
| `bt_device_name` | String? | tên hiển thị |

```kotlin
enum class TransportMode { AUTO, BLUETOOTH, WIFI }
```

### 6.2 Logic chọn link

Trong `CarViewModel`, mỗi vòng reconnect gọi lại hàm này (đừng cache — Bluetooth có thể vừa
được bật, hoặc user vừa đổi mạng):

```kotlin
private fun buildLinks(): List<CarLink> = when (transportMode) {
    TransportMode.BLUETOOTH -> listOfNotNull(btLinkOrNull())
    TransportMode.WIFI      -> listOfNotNull(tcpLinkOrNull())
    TransportMode.AUTO      -> listOfNotNull(btLinkOrNull(), tcpLinkOrNull())  // BT trước
}
```

`btLinkOrNull()` trả `null` khi: chưa có quyền `BLUETOOTH_CONNECT`, BT đang tắt, hoặc chưa chọn
thiết bị. `tcpLinkOrNull()` trả `null` khi `ip_address` rỗng.

Vòng reconnect:

```kotlin
connectJob = viewModelScope.launch {
    while (isActive) {
        val links = buildLinks()
        if (links.isEmpty()) { delay(3_000); continue }
        for (link in links) {
            try {
                _activeTransport.value = link.kind
                repo = CarRepository(link, _carStatus.value)   // rebind flow mirrors
                repo.connect()          // suspend tới khi đứt
            } catch (e: Exception) {
                Log.w(TAG, "${link.label} fail: ${e.message}")
                continue                // AUTO: rớt xuống link kế tiếp ngay, không chờ
            }
            break                       // đã nối rồi đứt → thoát for, chờ 3s rồi thử lại từ đầu
        }
        _connectionError.value = !connected.value
        delay(3_000)
    }
}
```

> ⚠️ **QUAN TRỌNG — single-repo pattern.**
> `CarRepository` hiện được tạo 1 lần trong `connect()` (line 90 `CarViewModel.kt`) và các
> `viewModelScope.launch { repo.xxx.collect {} }` (line 94–128) bám vào instance đó. Tạo repo
> mới mỗi vòng reconnect mà không cancel toàn bộ collector → **leak**: collector cũ vẫn chạy,
> bám instance cũ, ViewModel hold 2+ repo.
>
> **Giải pháp chọn:** giữ **một** `CarRepository` sống suốt đời ViewModel, thêm `repo.setLink(link)`
> trước mỗi lần `connect()`. Collector chỉ mount 1 lần khi ViewModel init. Không tạo repo mới.

### 6.3 Expose trạng thái cho UI

```kotlin
val activeTransport: StateFlow<TransportKind?>   // null = chưa nối
val transportMode: StateFlow<TransportMode>
fun setTransportMode(mode: TransportMode)
fun setBtDevice(address: String, name: String)
fun bondedDevices(): List<BondedDevice>          // tái dùng từ BtSppPocClient
```

### ✅ Nghiệm thu Phase 2

- `AUTO` + BT bật + đã chọn HU → nối qua BT, badge hiện "Bluetooth".
- Tắt BT giữa chừng → tự rớt sang WiFi trong ≤6s (không cần user bấm gì).
- `WIFI` → không đụng Bluetooth stack, hành vi y như trước.

---

## 7. Phase 3 — UI

Sửa [SettingsScreen.kt](../app/src/main/java/com/example/ex2_phone/ui/SettingsScreen.kt),
section **"Kết nối"**. Theo [ui-conventions.md](ui-conventions.md): `GlassCard`, accent từ
`carColor.accent`, chuỗi tiếng Việt.

1. **Segmented 3 lựa chọn**: `Tự động` / `Bluetooth` / `WiFi`. Mặc định `Tự động`,
   phụ đề: *"Ưu tiên Bluetooth, tự chuyển WiFi khi không có"*.
2. **Chọn head unit** (hiện khi mode ≠ WiFi): danh sách thiết bị đã pair; chưa có quyền thì
   hiện nút *"Cấp quyền Bluetooth"* → `BLUETOOTH_CONNECT`.
3. **`ConnectCard` (nhập IP)**: chỉ hiện khi mode ≠ Bluetooth.
4. **`StatusPill`**: thêm nhãn transport đang chạy — `● Bluetooth` / `● WiFi 192.168.43.1`.

Manifest đã có sẵn quyền BT (thêm ở Phase 0), không cần sửa thêm.

### ✅ Nghiệm thu Phase 3

Đổi mode trong UI → reconnect ngay theo mode mới, không cần restart app.

---

## 8. Gotcha — đọc trước khi debug

| Vấn đề | Xử lý |
|---|---|
| `connect()` / `readLine()` của `BluetoothSocket` là **blocking IO** | coroutine `cancel()` không cắt được. Phải **đóng socket** để bung ra. Mọi timeout đều làm bằng watchdog đóng socket. |
| `createRfcommSocketToServiceRecord` fail `IOException: read failed, socket might closed` | thử `createInsecureRfcommSocketToServiceRecord` (không mã hoá, vẫn cần bond). Một số HU chỉ chấp nhận insecure. Thử secure trước, fail thì fallback insecure. |
| BT discovery đang chạy | làm connect chậm/fail. **Luôn** `adapter.cancelDiscovery()` trước connect. |
| `BluetoothSocket` không có `soTimeout` | dùng idle watchdog §5.4. |
| Mở song song BT + TCP | đừng. Vòng reconnect thử **tuần tự**, link nào mở được thì giữ. |
| Chế độ `AUTO` mà xe tắt máy | mỗi vòng phí tối đa 12s chờ BT trước khi rơi xuống WiFi (watchdog cắt). Chấp nhận được; muốn nhanh thì chọn `WIFI` thủ công. |
| Status JSON ~1.5KB > MTU RFCOMM (~990B) | không sao — stream API tự ghép, `readLine()` xử lý đúng. |
| `adapter.bondedDevices` cần `BLUETOOTH_CONNECT` (API 31+) | check quyền trước, đừng để `SecurityException`. |
| App bị kill khi tắt màn hình | ngoài scope plan này. Auto-connect nền cần foreground service + receiver `ACTION_ACL_CONNECTED` — làm sau nếu cần. |

---

## 9. Phía xe — ✅ ĐÃ XONG (2026-08-02)

Không còn chặn Phase 2. Xe đã đẩy data thật qua SPP.

| File | Vai trò |
|---|---|
| `data/network/CarLineChannel.kt` | `CarTransport` enum, interface `CarLineChannel`, `StreamLineChannel` (chạy trên cặp stream bất kỳ) |
| `data/network/CarServerLink.kt` | interface transport phía server |
| `data/network/TcpServerLink.kt` | `ServerSocket` 47800 + NSD — tách nguyên từ `CarTcpServer.runAcceptLoop()` |
| `data/network/BtSppServerLink.kt` | `BluetoothServerSocket`, UUID `6f1e2a00-…`, service name `GeelyEX2-CarSync` |
| `data/network/CarTcpServer.kt` | giữ nguyên toàn bộ VHAL logic; `clients` giờ là `Set<CarLineChannel>` |

Hành vi:

- 2 link chạy 2 coroutine riêng — Bluetooth chết không kéo theo WiFi và ngược lại.
- Client nào nối (BT hay TCP) cũng nhận snapshot ngay, rồi vào chung `clients` → chung
  broadcast, chung heartbeat 15s, chung periodic refresh 5s.
- BT tắt lúc boot rồi bật sau: `BtSppServerLink` retry mỗi 30s. Thiếu `BLUETOOTH_CONNECT`
  thì log lỗi rồi bỏ hẳn transport BT (không spam).
- Server SPP nằm trong `CarTcpServer`, khởi động cùng `StatusWidgetBootstrap` như TCP —
  chưa tách foreground service riêng, đủ dùng vì app đã chạy nền sẵn.

**POC phía xe đã xóa** (`BtSppPoc.kt`, `BtSppPocSection.kt`) — trùng UUID với server thật,
để cả hai sẽ tranh SDP record.

---

## 10. Dọn dẹp POC — ✅ ĐÃ XONG

Cả 4 file POC + 2 block UI đã xoá ở cả 2 repo. Quyền BT trong 2 manifest giữ lại.
UI chọn thiết bị trong `TransportCard` thay thế hoàn toàn card POC.

---

## 11. Checklist theo file — ✅ HOÀN THÀNH

### Phase 1 — tách transport

- [x] `+ data/network/CarLink.kt` — `TransportKind`, `CarStreams`, `CarLink`
- [x] `+ data/network/TcpCarLink.kt`
- [x] `+ data/network/BtSppCarLink.kt` — kèm `BondedDevice`, `bondedDevices()`, `adapterOrNull()`
- [x] `~ data/network/CarRepository.kt` — `setLink()` + `activeLink` flow + idle watchdog 35s
- [x] `~ ui/CarViewModel.kt` — một repository sống suốt đời VM, collector mount trong `init {}`

### Phase 2 — chọn transport

- [x] `+ data/network/TransportPrefs.kt` — `TransportMode` + `bt_device_*` + `ip_address`
- [x] `~ ui/CarViewModel.kt` — `buildLinks()`, `restartConnectLoop()` có fallback,
      `activeTransport` / `activeTransportLabel` / `transportMode` / `btDevice`

### Phase 3 — UI

- [x] `+ ui/components/TransportCard.kt` — segmented Tự động/Bluetooth/WiFi + picker thiết bị
      + xin quyền + dòng trạng thái
- [x] `~ ui/SettingsScreen.kt` — dùng `TransportCard`; `ConnectCard` ẩn khi mode = Bluetooth
- [x] `~ ui/HomeScreen.kt` — `StatusPill` theo transport; ẩn `ConnectCard` khi mode = Bluetooth
- [x] `~ ui/components/DashboardComponents.kt` — `StatusPill(transport = …)`

### Còn phải test trên xe thật

- [ ] `AUTO` + đã chọn head unit → nối qua Bluetooth, pill hiện `Bluetooth`
- [ ] Tắt Bluetooth giữa chừng → tự rơi xuống WiFi trong ≤6s
- [ ] Bật lại Bluetooth → vòng sau tự leo lại BT
- [ ] Mode `WIFI` → hành vi y hệt trước khi có thay đổi này
- [ ] Ra khỏi tầm BT không tắt máy → idle watchdog cắt sau 35s rồi reconnect
