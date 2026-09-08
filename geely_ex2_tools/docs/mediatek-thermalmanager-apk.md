# com.mediatek.thermalmanager — Hướng dẫn phân tích APK (MTK Thermal Manager)

Tài liệu này mô tả ứng dụng hệ thống **MTK Thermal Manager** (`com.mediatek.thermalmanager`) từ màn hình trung tâm Geely **IHU629G**: giao diện UI dành cho kỹ sư để điều chỉnh chính sách tản nhiệt (thermal policy), giám sát các vùng (zones) sysfs, bộ ghi log nhiệt độ (thermal logger) và xử lý các cảnh báo quá nhiệt (overheat) từ tiến trình ngầm (daemon) MediaTek thermal.

**Lưu ý quan trọng:** Đây **không phải** là API của Flyme/eCarX và **không phải** Car/VHAL. APK này chỉ là một lớp giao diện mỏng (wrapper) bao bọc xung quanh **ngăn xếp MediaTek thermal (MediaTek thermal stack)** (`/vendor/bin/thermal_manager`, `/proc/driver/thermal/*`, `/sys/class/thermal/*`). Trên các bản ROM user/release, màn hình chính của ứng dụng này sẽ **đóng lại ngay lập tức** (`Build.TYPE` bắt buộc phải là `eng`).

---

## 0. Tổng quan ứng dụng

| Thông số | Giá trị |
|----------|----------|
| Gói ứng dụng (Package) | `com.mediatek.thermalmanager` |
| Nhãn (Label) | **MTK Thermal Manager** |
| versionCode | `1` |
| versionName | `1.0` |
| minSdk / targetSdk | 15 / 28 |
| compileSdk | 28 (Android 9) |
| sharedUserId | `android.uid.system` |
| Application | Không được khai báo (mặc định là `Application`) |
| Launcher Activity | **Không có** (`MTKThermalManagerActivity` được đặt `exported=false`) |
| DEX | Chỉ có một tệp `classes.dex` (~27 KB, chứa **16** lớp) |
| Kích thước APK | ~65 KB |

**Mục đích:**

1. **Giao diện kỹ sư (Engineering UI)** — Dùng để chọn và áp dụng chính sách nhiệt độ (thermal policy) (các tệp `.conf` / `.mtc`), xem danh sách các vùng nhiệt (thermal zones) và thiết bị làm mát (cooling devices).
2. **Boot hook** — Khi nhận được thông báo `BOOT_COMPLETED`, nó sẽ đặt lại cờ `clsd_rst` bên trong trình điều khiển nhiệt (thermal driver).
3. **UX Cảnh báo nhiệt độ (Thermal warning UX)** — Dựa vào tín hiệu broadcast từ nền tảng (platform), nó hiển thị hộp thoại hệ thống với thông báo "thiết bị cần được làm mát" và sẽ lên lịch tắt máy (shutdown) sau 30 giây.
4. **Thermal logger** (đoạn mã nằm trong dex) — Ghi lại dữ liệu vào `/proc/driver/thermal_logger_config`, `storage_logger`, kết xuất (dump) file log vào `/data/` (Thành phần Switch trong giao diện layout đã **bị loại bỏ**, xem chi tiết tại §3.1).

**Cấu trúc ngăn xếp (theo dex/JADX):**

| Lớp (Layer) | Đường dẫn / thành phần (Component) |
|------|------------------|
| Tiến trình ngầm bản địa (Native daemon) | `/vendor/bin/thermal_manager <conf>` |
| Tệp chính sách (Policy files) | `/vendor/etc/.tp/thermal.conf`, `thermal.off.conf`, `.ht120.mtc` |
| Chính sách tùy biến (Custom policy) | `/data/*.mtc` |
| Linux thermal sysfs | `/sys/class/thermal/thermal_zone*`, `cooling_device*` |
| Các node proc của MTK (MTK proc nodes) | `/proc/driver/thermal/*`, `/proc/driver/thermal_logger_config`, `/proc/driver/storage_logger*` |
| Broadcast của nền tảng (Platform broadcasts) | `mediatek.intent.action.THERMAL_*` |

---

## 1. Nguồn gốc và Artifacts

| Thông số | Giá trị |
|----------|----------|
| Nền tảng (Nguồn bản dump) | IHU629G |
| APK gốc (Từ ADBAppControl) | `downloads/250060 IHU629G/MTK Thermal Manager (com.mediatek.thermalmanager) [v.1.0].apk` |
| Bản sao cục bộ | `.tmp/mediatek-thermalmanager.apk` |
| APK đã giải nén | `.tmp/mediatek-thermalmanager-apk/` |
| JADX | `.tmp/mediatek-thermalmanager-jadx/` |

### Lấy APK từ thiết bị

```bash
adb shell pm path com.mediatek.thermalmanager
adb pull /system/app/.../MTKThermalManager.apk .tmp/mediatek-thermalmanager.apk
```

### Giải nén và tìm kiếm

```powershell
Copy-Item -LiteralPath ".tmp\mediatek-thermalmanager.apk" -Destination ".tmp\mediatek-thermalmanager.zip"
Expand-Archive -LiteralPath .tmp\mediatek-thermalmanager.zip -DestinationPath .tmp\mediatek-thermalmanager-apk -Force

$aapt = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter "aapt.exe" | Select-Object -First 1).FullName
& $aapt dump badging .tmp\mediatek-thermalmanager.apk
& $aapt dump xmltree .tmp\mediatek-thermalmanager.apk AndroidManifest.xml
```

**JADX** — Toàn bộ mã nguồn thu gọn lại chỉ còn vỏn vẹn trong 8 tệp Java (+ một vài lớp nội bộ - inner classes).

---

## 2. Kiến trúc

```mermaid
flowchart TB
    subgraph kernel [MediaTek thermal / Linux]
        TM["/vendor/bin/thermal_manager"]
        SYSFS["/sys/class/thermal"]
        PROC["/proc/driver/thermal/*"]
        DAEMON[Trình nền (daemon) / trình điều khiển (driver) nhiệt]
    end

    subgraph apk [com.mediatek.thermalmanager uid=system]
        SS[ServiceStarter BroadcastReceiver]
        MAIN[MTKThermalManagerActivity]
        SENS[ThermalSensorActivity]
        COOL[CoolersActivity]
        TZ[TzDeviceActivity]
        WARN[ShutDownAlertDialogActivity]
        SD[ShutDownAlarm]
    end

    DAEMON -->|"THERMAL_WARNING / THERMAL_SHUTDOWN"| SS
    SS -->|BOOT_COMPLETED| PROC
    SS -->|THERMAL_WARNING| WARN
    WARN -->|Lên lịch Alarm sau +30s| SD
    SD -->|Yêu cầu REQUEST_SHUTDOWN| SYS[Giao diện tắt nguồn Android (shutdown UI)]

    MAIN -->|Lệnh shell| TM
    MAIN --> PROC
    SENS --> SYSFS
    COOL --> SYSFS
    TZ --> SYSFS
    TM --> PROC
```

### 2.1 Các thành phần trong Manifest

| Thành phần | Lớp (Class) | exported | Mục đích |
|-----------|-------|----------|------------|
| Activity | `MTKThermalManagerActivity` | false | Màn hình chính (chỉ dành cho bản ROM eng) |
| Activity | `ThermalSensorActivity` | mặc định | Hiển thị danh sách `thermal_zone*` |
| Activity | `CoolersActivity` | mặc định | Hiển thị danh sách `cooling_device*` |
| Activity | `TzDeviceActivity` | mặc định | Xem chi tiết thông số vùng nhiệt: temp, trip points, coolers |
| Activity | `ShutDownAlertDialogActivity` | mặc định | Hộp thoại cảnh báo quá nhiệt (Sử dụng chủ đề `Theme`, kiểu cửa sổ window type 2003) |
| Receiver | `ServiceStarter` | mặc định | Lắng nghe `BOOT_COMPLETED`, `THERMAL_WARNING` |
| Receiver | `ShutDownAlarm` | false | Lắng nghe `THERMAL_SHUTDOWN` → ra lệnh tắt máy (shutdown) |

**Protected broadcasts** (Các broadcast được bảo vệ, khai báo trong manifest):

| Hành động (Action) |
|--------|
| `mediatek.intent.action.THERMAL_DIAG` |
| `mediatek.intent.action.THERMAL_WARNING` |
| `mediatek.intent.action.THERMAL_SHUTDOWN` |

---

## 3. Các màn hình và Logic (Screens and logic)

### 3.1 MTKThermalManagerActivity

**Giới hạn bản build:**

```java
if (!Build.TYPE.equals("eng")) {
    Toast.makeText(this, "Only supported in eng build.", Toast.LENGTH_LONG).show();
    finish();
}
```

Trên các bản build **user** / **userdebug** (khi `Build.TYPE != "eng"`), activity này sẽ đóng ngay lập tức. Để khởi chạy thủ công, hãy dùng lệnh:

```bash
adb shell am start -n com.mediatek.thermalmanager/.MTKThermalManagerActivity
```

**Chính sách quản lý nhiệt - Thermal policy (Trình đơn thả xuống Spinner + Nút Apply):**

| Vị trí trên Spinner | Tệp policy tương ứng |
|-----------------|-------------|
| `default` (mặc định) | `/vendor/etc/.tp/thermal.conf` |
| `thermal protection only` (chỉ bảo vệ nhiệt) | `/vendor/etc/.tp/thermal.off.conf` |
| `high temp 120deg C` (nhiệt độ cao 120 độ C) | `/vendor/etc/.tp/.ht120.mtc` |
| động (dynamically) | Bất kỳ file `/data/*.mtc` nào (tải khi mở menu Spinner) |

Cách áp dụng:

```java
executeShellCommand("/vendor/bin/thermal_manager " + confFile);
```

Trước khi thay đổi policy, nếu bộ ghi log (thermal logger) đang hoạt động, mã sẽ cố gắng bật/tắt (toggle) công tắc `thermal_logger_switch` — **Tuy nhiên, Widget Switch (công tắc) trong tệp `res/layout/main.xml` đã bị gỡ bỏ**, biến tương ứng cũng không được khởi tạo (initialize) trong hàm `onCreate()`. Hàm lắng nghe sự kiện `onCheckedChangeListener` vẫn tồn tại bên trong dex, nhưng thành phần UI kết nối với nó trong bản build này đã bị **ngắt kết nối** (đây là mã rác - legacy/dead code).

**Bộ ghi log nhiệt - Thermal Logger (tác động lên proc, nếu kích hoạt mã lệnh):**

| Bước thực hiện | Tệp điều khiển | Giá trị |
|-----|------|----------|
| khởi tạo storage logger | `/proc/driver/storage_logger_config` | Ghi `0 0 0 0`, sau đó ghi `0 0 1` |
| bật thermal log | `/proc/driver/thermal_logger_config` | `5` |
| cấp phát bộ đệm (buffer) | `/proc/driver/storage_logger_bufsize_malloc` | `10485760` |
| khởi động | `/proc/driver/storage_logger` | `ENABLE 1` |
| theo dõi | `/proc/driver/thermal/mtm_monitor` | `1 <HHmmss>` |
| dừng theo dõi | `/proc/driver/thermal/mtm_monitor` | `0` |
| kết xuất log (dump) | `/proc/driver/storage_logger_display` → ghi ra `/data/storage_logger_dump_<timestamp>` | ghi nối (append) |

Kiểm tra "logger có đang bật không": Đọc tệp `/proc/driver/thermal_logger_config` (tìm các chuỗi `Enable logger`, `(Bit3)= 1`).

**Điều hướng UI (Danh sách ListView):**

| Mục chọn (Item) | Màn hình tương ứng (Activity) | Điều kiện kích hoạt |
|-------|----------|---------|
| Thermal Sensors (Cảm biến nhiệt) | `ThermalSensorActivity` | Bị khóa (blocked) nếu logger đang hoạt động |
| Coolers (Bộ làm mát) | `CoolersActivity` | Luôn luôn khả dụng |

### 3.2 ThermalSensorActivity

- Quét thư mục `/sys/class/thermal/`, lọc tìm các tên tệp dạng `thermal_zone*`.
- Đọc thông tin `type` (loại) và `temp` (nhiệt độ) của mỗi vùng (đơn vị hiển thị trên UI là millidegree C).
- Bấm vào mục chọn (Click) → Sẽ mở `TzDeviceActivity` kèm theo dữ liệu bổ sung (extra) là `tz_sysfs_path`.

### 3.3 CoolersActivity

- Quét thư mục `/sys/class/thermal/`, lọc tìm `cooling_device*`.
- Đọc các thông số: `type`, `cur_state` (trạng thái hiện tại), `max_state` (trạng thái tối đa).
- Bấm vào mục chọn (Click) — Gọi tới một trình xử lý rỗng (empty handler).

### 3.4 TzDeviceActivity

Hiển thị thông tin chi tiết cho khu vực được chọn (`tz_sysfs_path`):

| Đường dẫn sysfs | Cách hiển thị (Otoбражение) |
|-------|-------------|
| `type` | tiêu đề (header) |
| `temp` | nhiệt độ hiện tại |
| `mode` | chế độ chạy (ví dụ `kernel`) |
| `trip_point_N_temp` / `trip_point_N_type` | các điểm kích hoạt nhiệt độ giới hạn (ngưỡng - thresholds, tối đa 12) |
| `cdevN_trip_point`, `cdevN/type` | liên kết (binding) bộ làm mát cooler → gắn với trip point |

### 3.5 ServiceStarter (Khởi động lúc boot + Xử lý cảnh báo)

**`android.intent.action.BOOT_COMPLETED`** (+ kèm category `HOME`):

```java
write("/proc/driver/thermal/clsd_rst", "1");
```

**`mediatek.intent.action.THERMAL_WARNING`:**

- Kích hoạt mở `ShutDownAlertDialogActivity` với các cờ (flags) `NEW_TASK | CLEAR_TOP`.

### 3.6 ShutDownAlertDialogActivity + ShutDownAlarm

**Khung thoại (Dialog):**

| ID chuỗi (R.string) | Bản Tiếng Anh (EN) |
|----------|-----|
| `alert_dialog_two_buttons_title` | Thermal Warning! (Cảnh Báo Quá Nhiệt!) |
| `alert_dialog_two_buttons_message` | Phone needs to cool down soon! (Thiết bị cần được làm mát ngay!) |
| `alert_dialog_ok` | Ok |
| `alert_dialog_cancel` | Cancel (Hủy) |

Loại cửa sổ (Window): Gán thông qua lệnh `dialog.getWindow().setType(2003)` — Tương đương với hằng số `TYPE_SYSTEM_ALERT` (Đây là hộp thoại overlay đè lên hệ thống).

**Bộ đếm thời gian tắt nguồn (Tái hẹn tắt máy - shutdown timer):** Sử dụng `AlarmManager` + Hẹn sau **30 giây** → Bắn broadcast gọi `ShutDownAlarm` cùng với action `mediatek.intent.action.THERMAL_SHUTDOWN`.

**Shutdown intent (Ra lệnh tắt nguồn)** (Khi người dùng bấm nút Ok hoặc chuông báo alarm hết hạn):

```java
Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
context.startActivity(intent);
```

Thao tác này bắt buộc phải có quyền `android.permission.SHUTDOWN` (đã được khai báo trong manifest và ứng dụng chạy bằng UID hệ thống - system UID).

---

## 4. Quyền hạn (Permissions)

| Quyền hạn | Mục đích sử dụng |
|------------|-------|
| `RECEIVE_BOOT_COMPLETED` | Dùng cho receiver `ServiceStarter` |
| `SHUTDOWN` | Cho phép tắt máy tính cưỡng bức |
| `SYSTEM_ALERT_WINDOW` | Mở hộp thoại thông báo nổi (overlay) trên các giao diện khác |
| `MODIFY_AUDIO_SETTINGS` | Quyền này không được gọi trong dex (thuộc mã cũ - legacy / platform hook) |
| `com.android.alarm.permission.SET_ALARM` | Cấp phép gọi `AlarmManager` để trì hoãn việc tắt máy (delayed shutdown) |
| `com.android.alarm.permission.WRITE_SETTINGS` | Sử dụng cho API alarm (dạng cũ - legacy) |

---

## 5. Tài nguyên chuỗi (String resources - cài đặt theo ngôn ngữ mặc định)

| Tên ID | Tiếng Anh (EN) |
|----|-----|
| `app_name` | MTK Thermal Manager |
| `thermal_sensors` | Thermal Sensors (Cảm biến nhiệt) |
| `coolers` | Coolers (Bộ làm mát) |
| `thermal_protection` | Thermal Protection (Bảo vệ nhiệt) |
| `thermal_policy_file_colon` | Thermal Policy File: (Tệp tin Chính sách Tản nhiệt:) |
| `apply_new_thermal_policy` | Apply New Thermal Policy (Áp dụng Chính sách Tản nhiệt Mới) |
| `tz_device_info` | TZ Device Info (Thông tin Thiết bị TZ) |
| `thermal_logger` | Thermal Logger (Bộ ghi log nhiệt) |

Hỗ trợ bản địa hóa (Localization): ~40 ngôn ngữ (bao gồm ar, ru, zh-CN, ja, …).

---

## 6. Các lớp có trong dex

| Lớp | Vai trò |
|-------|------|
| `MTKThermalManagerActivity` | Màn hình chính, cấu hình policy, bộ ghi log logger |
| `ThermalSensorActivity` | Xem danh sách các thermal zones |
| `CoolersActivity` | Xem danh sách các cooling devices |
| `TzDeviceActivity` | Xem chi tiết một thermal zone |
| `ServiceStarter` | Bộ thu nhận (receiver) cho Boot và báo động nhiệt |
| `ShutDownAlertDialogActivity` | Giao diện thông báo cảnh báo |
| `ShutDownAlarm` | Xử lý lệnh Shutdown thông qua báo thức alarm |
| `ExtensionFilter` | Lọc các file đuôi `*.mtc` có trong thư mục `/data` |

Các nhãn Log (Log tags): `@M_MTKThermalManagerActivity`, `@M_ThermalSensorActivity`, `@M_TzDeviceActivity`, `thermalmanager.ServiceStarter`, `@M_thermalmanager.ServiceStarter`.

---

## 7. Mối liên kết với các thành phần khác trên IHU629G

```mermaid
flowchart LR
    subgraph mtk [Nền tảng MediaTek platform]
        DRV[Trình điều khiển nhân nhiệt (thermal kernel driver)]
        TM["Tệp thực thi thermal_manager (binary)"]
    end

    subgraph apk [com.mediatek.thermalmanager]
        UI[Giao diện Kỹ sư (Engineering UI)]
        RX[ServiceStarter]
    end

    subgraph flyme [Ngăn xếp Flyme / Geely stack]
        SETTINGS[com.flyme.auto.settings]
        TOOLS[Geely EX2 Tools]
    end

    DRV -->|"THERMAL_WARNING v.v."| RX
    UI --> TM
    TM --> DRV
    SETTINGS -.->|không có API kết nối trực tiếp| apk
    TOOLS -.->|không có API kết nối trực tiếp| apk
```

| Thành phần | Liên kết |
|-----------|-------|
| `com.flyme.auto.*` | **Không có** sự tích hợp nào với trình quản lý nhiệt (thermal manager) |
| `com.android.car` / VHAL | **Không liên quan** — Ứng dụng chỉ giao tiếp qua sysfs/proc |
| Geely EX2 Tools | **Không sử dụng** APK này; tuy nhiên công cụ này rất hữu ích trong việc **phân tích lỗi quá nhiệt trên màn hình trung tâm (ГУ - HU)** |

---

## 8. Hướng dẫn Gỡ lỗi trên thiết bị (Debugging)

### Kiểm tra sự tồn tại của gói phần mềm

```bash
adb shell pm list packages | findstr thermalmanager
adb shell dumpsys package com.mediatek.thermalmanager
```

### Kiểm tra thông số Thermal zones / coolers

```bash
adb shell ls /sys/class/thermal/
adb shell cat /sys/class/thermal/thermal_zone0/temp
adb shell cat /sys/class/thermal/cooling_device0/cur_state
```

### Kiểm tra nút bấm MTK proc (yêu cầu máy đã được cấp quyền root / cài bản ROM eng)

```bash
adb shell cat /proc/driver/thermal/clsd_rst
adb shell cat /proc/driver/thermal_logger_config
adb shell ls /vendor/etc/.tp/
```

### Mô phỏng cảnh báo nhiệt (hãy cẩn trọng — hành động này có thể hiển thị UI tắt nguồn - shutdown UI)

```bash
adb shell am broadcast -a mediatek.intent.action.THERMAL_WARNING
```

### Thu thập Nhật ký Logs

```bash
adb logcat | findstr /i "MTKThermalManagerActivity thermalmanager.ServiceStarter ThermalSensorActivity TzDeviceActivity"
```

### Các sự cố thường gặp (Troubleshooting)

| Triệu chứng | Lý do khả thi nhất |
|---------|-------------------|
| Activity đóng lại ngay sau khi vừa bật | Trạng thái máy `Build.TYPE != eng` |
| Thông báo lỗi "No thermal sensors found" | Cấu trúc tệp `/sys/class/thermal` không tồn tại hoặc thư mục trống |
| Không thể áp dụng Policy | Không tìm thấy tệp nhị phân `/vendor/bin/thermal_manager` hoặc tệp cấu hình conf |
| Đột nhiên hiện Hộp thoại Shutdown | Do nhiệt độ tăng cao thực sự hoặc hệ thống nhận được lệnh broadcast `THERMAL_WARNING` / `THERMAL_SHUTDOWN` |
| Giao diện Logger UI bị ẩn, không mở được | Công tắc (Switch) trong layout đã bị xóa từ phiên bản v1.0 |

---

## 9. Khả năng tương tác với Geely EX2 Tools

**Không yêu cầu sự tích hợp trực tiếp** — bên trong dự án hoàn toàn không có bất kỳ dòng lệnh nào gọi tới `com.mediatek.thermalmanager`.

Giá trị ứng dụng thực tiễn của tài liệu này:

- Giúp hiểu rõ, **lệnh tự động tắt máy khi quá nhiệt (thermal shutdown) xuất phát từ đâu** (nhấn mạnh: không phải từ ứng dụng cài đặt Flyme Settings);
- Khả năng chuẩn đoán tình trạng quá nhiệt (overheating) trên chip SoC/màn hình trung tâm HU thông qua các lệnh phân tích sysfs và proc;
- Trên các bản ROM dạng **eng**, cho phép thay đổi cấu hình tản nhiệt (thermal policy) phục vụ cho quá trình kiểm thử (`thermal_manager`).

Hãy tuyệt đối lưu ý: Không tự ý kích hoạt các lệnh `REQUEST_SHUTDOWN` và `THERMAL_SHUTDOWN` trên hệ thống xe đang vận hành bình thường (trừ khi gặp trường hợp cực kỳ cấp thiết).

---

*Tài liệu này được biên soạn dựa trên quá trình dịch ngược và phân tích (reverse engineering) file APK `com.mediatek.thermalmanager` phiên bản v1.0 (trên nền máy IHU629G). Nếu thay đổi bản phân phối phần mềm (firmware), vui lòng lưu ý rằng các đường dẫn như `/vendor/etc/.tp/*`, cấu trúc nút thư mục lệnh proc và nội dung phát sóng broadcast có thể sẽ bị thay đổi hoặc không tương thích.*
