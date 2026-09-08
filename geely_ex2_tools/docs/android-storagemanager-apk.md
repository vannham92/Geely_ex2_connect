# com.android.storagemanager — Hướng dẫn phân tích APK (Storage Manager)

Tài liệu này mô tả APK **Storage Manager** (`com.android.storagemanager`) — một ứng dụng hệ thống của AOSP **Android 9 (API 28)** có nhiệm vụ giải phóng không gian bộ nhớ lưu trữ: với chế độ dọn dẹp thủ công **Deletion Helper** và dọn dẹp chạy ngầm **Automatic Storage Management (ASM)**.

**Quan trọng:** ứng dụng này **không phải** là ứng dụng đặc thù của Geely/Flyme/eCarX. Nó là một package chuẩn của nền tảng nằm ở `packages/apps/StorageManager`. Trong phiên bản này (`versionName=9`), các overlay-provider cho Google Photos / tính năng dọn dẹp ngầm đã bị **vô hiệu hóa** (hàm `FeatureFactoryImpl` trả về `null`) — quá trình tự động xóa ảnh/video trong các tác vụ nền (Job) **không được thực thi**, chỉ còn giữ lại bộ khung của ASM và UI.

Bản cài đặt lấy từ Telegram: `…_com_android_storagemanager_v_9.apk`.

---

## 0. Tổng quan ứng dụng

| Tham số | Giá trị |
|----------|----------|
| Package | `com.android.storagemanager` |
| Label | **Storage Manager** / Tiếng Nga: **Менеджер хранилища** |
| versionCode / versionName | `28` / `9` |
| minSdk / targetSdk | 24 / 28 |
| compileSdk / platform | 28 / Android 9 |
| sharedUserId | **không có** |
| Application | `Application` mặc định |
| DEX | một tệp `classes.dex` (~1.8 MB) |
| Kích thước APK | ~4.6 MB |
| Launcher | **không có** MAIN/LAUNCHER riêng — khởi chạy thông qua Settings / intent `MANAGE_STORAGE` |

**Chức năng (toàn bộ tính năng):**

1. **Deletion Helper (dọn dẹp thủ công)** — màn hình "Free up space" / "Remove items": cho phép chọn và xóa các ứng dụng ít sử dụng, tệp tin trong thư mục Downloads, và (tùy chọn) các ảnh/video đã được sao lưu (backed-up).
2. **Automatic Storage Management (ASM)** — chạy định kỳ thông qua JobScheduler khi cắm sạc + thiết bị ở chế độ rảnh (idle): nếu bộ nhớ trống (private storage) < 15% và ASM đang tắt → hiển thị thông báo "hãy bật trình quản lý bộ nhớ"; nếu ASM đang bật → ủy quyền dọn dẹp cho overlay-provider (trong APK này nó là `null`).
3. **Opt-in / upsell** — thông báo và hộp thoại kêu gọi "bật tính năng tự động quản lý bộ nhớ lưu trữ".
4. **Tích hợp với Settings** — xử lý intent `android.os.storage.action.MANAGE_STORAGE` (kể cả khi một ứng dụng khác yêu cầu cấp dung lượng thông qua `StorageManager.allocateBytes`).

**Cấu trúc thực thi:**

| Lớp (Layer) | API / thành phần |
|------|-----------------|
| Storage | `StorageManager`, `PrivateStorageInfo`, `StorageStats` / `StorageStatsSource` |
| Usage | `UsageStatsManager` (`PACKAGE_USAGE_STATS`) |
| Packages | `PackageManager.deletePackageAsUser` (`DELETE_PACKAGES`) |
| Files | `Environment.DIRECTORY_DOWNLOADS`, `File.delete` |
| Jobs | `JobScheduler` + `JobService` (`BIND_JOB_SERVICE`) |
| Settings | Các key của `Settings.Secure` `automatic_storage_manager_*` |
| Overlay | `FeatureFactory` → Trình quản lý Photos / Job (từ OEM/GMS) |

---

## 1. Nguồn và artifact

| Tham số | Giá trị |
|----------|----------|
| APK gốc | Tải từ Telegram Desktop `…_com_android_storagemanager_v_9.apk` |
| Bản sao cục bộ | `.tmp/storagemanager/storagemanager.apk` |
| APK đã giải nén | `.tmp/storagemanager/apk/` |
| badging / manifest từ aapt | `.tmp/storagemanager/badging.txt`, `manifest.txt` |
| JADX | `.tmp/storagemanager/jadx/app/src/main/java/com/android/storagemanager/` |
| Danh sách class | `.tmp/storagemanager/classes.txt` |

### Quá trình giải nén

```powershell
$apk = "path\to\…_com_android_storagemanager_v_9.apk"
$base = ".tmp\storagemanager"
New-Item -ItemType Directory -Force -Path $base\apk | Out-Null
Copy-Item -LiteralPath $apk -Destination "$base\storagemanager.apk"
Copy-Item "$base\storagemanager.apk" "$base\storagemanager.zip"
Expand-Archive -Path "$base\storagemanager.zip" -DestinationPath "$base\apk" -Force

$aapt = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter "aapt.exe" | Select-Object -First 1).FullName
& $aapt dump badging $apk
& $aapt dump xmltree $apk AndroidManifest.xml
```

---

## 2. Kiến trúc

```mermaid
flowchart TB
    subgraph entry [Điểm vào (Entry points)]
        Intent[MANAGE_STORAGE]
        Boot[BOOT_COMPLETED]
        Settings[INTERNAL_STORAGE_SETTINGS]
    end

    subgraph ui [Giao diện Deletion Helper]
        DHA[DeletionHelperActivity]
        DHS[DeletionHelperSettings]
        Apps[AppDeletionType + AppsAsyncLoader]
        DL[DownloadsDeletionType + FetchDownloadsLoader]
        Photos[PhotosDeletionPreference + overlay DeletionType]
        Confirm[ConfirmDeletionDialog]
        Upsell[StorageManagerUpsellDialog]
    end

    subgraph auto [Automatic Storage Management]
        BootRx[AutomaticStorageBroadcastReceiver]
        Job[AutomaticStorageManagementJobService]
        Notif[NotificationController]
        Warn[WarningDialogActivity]
        OverlayJob[StorageManagementJobProvider overlay]
    end

    Intent --> DHA
    DHA --> DHS
    DHS --> Apps
    DHS --> DL
    DHS --> Photos
    DHS --> Confirm
    Confirm --> Upsell

    Boot --> BootRx
    BootRx -->|lên lịch JobInfo| Job
    Job -->|bộ nhớ trống < 15% và ASM tắt| Notif
    Job -->|ASM bật| OverlayJob
    Notif -->|ACTIVATE| Warn
    Notif -->|SHOW_SETTINGS| Settings
```

### Các package trong dex (internal)

| Package | Vai trò |
|-------|------|
| `…deletionhelper.*` | Giao diện và logic của dọn dẹp thủ công |
| `…automatic.*` | Job, quá trình khởi động (boot), notification, cảnh báo |
| `…overlay.*` | Hook kết nối với OEM/GMS (trong APK hiện tại là stub) |
| `…utils.*` | AsyncLoader, icons, trải nghiệm người dùng lúc đang load (loading UX) |
| `com.android.settingslib.*` | Thư viện nhúng StorageStats, PrivateStorageInfo, HelpUtils |

---

## 3. Manifest: Các thành phần (components)

### 3.1 Quyền (Permissions)

| Quyền | Mục đích |
|------------|--------|
| `PACKAGE_USAGE_STATS` | Theo dõi thời điểm ứng dụng được sử dụng lần cuối |
| `GET_PACKAGE_SIZE` | Lấy kích thước các ứng dụng (`StorageStats`) |
| `DELETE_PACKAGES` | Gỡ cài đặt các gói APK được chọn (`deletePackageAsUser`) |
| `READ/WRITE_EXTERNAL_STORAGE` | Quét và xóa các tập tin trong thư mục Downloads |
| `MANAGE_USERS` / `INTERACT_ACROSS_USERS` | Hỗ trợ đa người dùng (Multi-user, mẫu chung của nền tảng) |
| `WRITE_SECURE_SETTINGS` | Bật/tắt ASM trong `Settings.Secure` |
| `RECEIVE_BOOT_COMPLETED` | Thiết lập Job sau khi thiết bị khởi động |
| `USE_RESERVED_DISK` | Quyền sử dụng không gian ổ cứng dự phòng của nền tảng |

### 3.2 Hoạt động (Activities)

| Class | Mục đích |
|-------|------------|
| `.deletionhelper.DeletionHelperActivity` | UI chính của công cụ dọn dẹp. Cấu hình Intent-filter: `android.os.storage.action.MANAGE_STORAGE` + `DEFAULT`. Chế độ `launchMode=singleTask`. |
| `.automatic.WarningDialogActivity` | Hộp thoại cảnh báo sau khi kích hoạt ASM (nếu biến `ro.storage_manager.enabled=false`). Chạy với chế độ không lưu lịch sử, loại khỏi danh sách Recent (excludeFromRecents). |

### 3.3 Dịch vụ (Services)

| Class | Mục đích |
|-------|------------|
| `.automatic.AutomaticStorageManagementJobService` | Là một `JobService`, yêu cầu quyền `BIND_JOB_SERVICE`, không mở rộng `exported`. Chịu trách nhiệm kiểm tra storage theo định kỳ / kích hoạt ASM / hiển thị notification. |

### 3.4 Broadcast Receivers

| Class | Hành động (Actions) |
|-------|---------|
| `.automatic.AutomaticStorageBroadcastReceiver` | Đón tín hiệu `BOOT_COMPLETED` → lên lịch chạy Job |
| `.automatic.NotificationController` | Bắt các action `ACTIVATE`, `NO_THANKS`, `DISMISS`, `SHOW_NOTIFICATION` / `show_notification`, `DEBUG_SHOW_NOTIFICATION`, `SHOW_SETTINGS` |

---

## 4. Chi tiết chức năng

### 4.1 Deletion Helper — dọn dẹp không gian thủ công

**Màn hình:** Sử dụng PreferenceFragment với danh sách các danh mục cần dọn + nút Cancel / "Free up X".

**Các danh mục giao diện UI** (`res/xml/deletion_helper_list.xml`):

| Khóa (Key) | Thuộc tính (Preference) | Tác dụng |
|-----|------------|------------|
| `deletion_gauge` | `GaugePreference` | Nếu intent có chứa tham số `android.os.storage.extra.REQUESTED_BYTES` — hiển thị thanh "Ứng dụng X cần Y dung lượng" |
| `delete_photos` | `PhotosDeletionPreference` | Ảnh & video đã sao lưu (chỉ khi có provider overlay cung cấp tính năng này) |
| `delete_downloads` | `DownloadsDeletionPreferenceGroup` | File nằm trong `DIRECTORY_DOWNLOADS` |
| `apps_group` | `AppDeletionPreferenceGroup` | Các ứng dụng ít được sử dụng |

**Các chế độ của ngưỡng (thresholdType):**

| Loại | Menu | Hành vi |
|------|-------|-----------|
| `0` (mặc định) | "Hide recent items" | Đối với Apps: loại ra những app chưa dùng ≥ **90 ngày** (`debug.asm.app_unused_limit`, mặc định là 90). Đối với Downloads: Lọc theo thời gian bằng biến `debug.asm.file_age_limit` (mặc định 0 = toàn bộ file). |
| `1` | "Show all items" / liên kết ở giao diện empty-state | Đối với Apps: hiển thị tất cả ứng dụng non-system / không thuộc tiến trình nền dai dẳng (không xét tới usage time). Nhóm Downloads/apps có thể ẩn đi ở giao diện empty-state. |

Công tắc chuyển đổi (toggle) chế độ: tùy chọn ở menu góc trên, được quản lý bằng biến cờ `Settings.Global.enable_deletion_helper_no_threshold_toggle` (mặc định 1).

#### 4.1.1 Ứng dụng — những app ít dùng

**Các Class:** `AppsAsyncLoader` + `AppDeletionType` + `PackageDeletionTask`.

Thuật toán:

1. Chạy `UsageStatsManager.queryAndAggregateUsageStats` truy vấn lịch sử ứng dụng ~364 ngày + hoặc dùng phương thức dự phòng `queryUsageStats`.
2. Đối với mỗi app đã cài đặt (dựa trên UID): dùng `StorageStatsSource.getStatsForUid` → lấy kích thước app.
3. Các bộ lọc ngoại trừ (loại bỏ khỏi danh sách):
   - Ứng dụng hệ thống/đi kèm (`FLAG_SYSTEM`);
   - Tiến trình cố định (persistent process) (`FLAG_PERSISTENT`);
   - HOME / launcher mặc định của thiết bị;
   - Không lấy được dữ liệu hợp lệ về daysSinceInstall / daysSinceLastUse.
4. Sắp xếp các mục theo dung lượng giảm dần ↓.
5. Gỡ cài đặt: sử dụng `PackageManager.deletePackageAsUser` qua lớp `IPackageDeleteObserver` đối với các package **đã được tick chọn**.

**Lưu ý quan trọng cho thiết bị màn hình xe (ГУ):** thao tác sẽ xóa **toàn bộ ứng dụng** (uninstall), chứ không phải chức năng clear cache/data.

#### 4.1.2 Mục Downloads

**Các Class:** `FetchDownloadsLoader` + `DownloadsDeletionType`.

1. Quét sâu (đệ quy) thư mục `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)`.
2. Lấy danh sách các tệp có `lastModified <= now - debug.asm.file_age_limit * 1 day` (với mặc định age=0 → sẽ lấy tất cả các file).
3. Hình ảnh — tiến hành làm ảnh thu nhỏ qua `ThumbnailUtils`.
4. Xóa tệp: dùng `File.delete()` chạy ngầm (`AsyncTask`) cho các file đã tick chọn (mặc định tick tất cả).

Cần được cấp quyền cấp chạy nền `READ_EXTERNAL_STORAGE`.

#### 4.1.3 Ảnh & video (đã sao lưu)

**Interface:** `DeletionHelperFeatureProvider.createPhotoVideoDeletionType`.

Trong **APK hiện tại**, khi gọi hàm `FeatureFactoryImpl.getDeletionHelperFeatureProvider() == null` → mục chức năng (preference) sẽ **không hiển thị trên màn hình**. Trên các thiết bị thông thường có cài Google Services (Pixel/GMS), một hệ thống provider của Google Photos overlay thường được gắn vào để thực hiện xóa các bản sao cục bộ đã đồng bộ.

Dù vậy, giao diện vẫn chứa sẵn các chuỗi như: "Ảnh & video đã được sao lưu", "Cách đây hơn 30 ngày".

#### 4.1.4 Xác nhận + Upsell

1. **ConfirmDeletionDialog** — Hiển thị bảng xác nhận "Sẽ xóa %1$s dung lượng dữ liệu"; sau đó thực thi `clearData()` cho toàn bộ các category.
2. **StorageManagerUpsellDialog** — sau khi free bộ nhớ thành công: gợi ý thiết lập bật ASM (`Settings.Secure.automatic_storage_manager_enabled = 1`). Cơ chế chống làm phiền (spam): Lưu biến `StorageManagerUpsellDialog` trong SharedPreferences (nếu chọn no_thanks / dismiss sẽ được trì hoãn hiển thị lại từ 14 đến 90 ngày).

#### 4.1.5 Trạng thái trống (Empty state)

Khi mọi phân mục đều rỗng → chuyển sang giao diện báo "không có gì để dọn" kèm liên kết click được "Show all items" (thresholdType=1).

---

### 4.2 Tự động quản lý bộ nhớ (Công việc chạy ngầm - ASM)

#### 4.2.1 Cơ chế lên lịch

Lớp `AutomaticStorageBroadcastReceiver` bắt tín hiệu `BOOT_COMPLETED`:

```text
JobInfo:
  id = 0
  component = AutomaticStorageManagementJobService
  requiresCharging = true
  requiresDeviceIdle = true
  periodic = debug.asm.period (mặc định 86400000 ms = 24 tiếng)
```

Một kiểm tra bổ sung trong job: check xem có đang sạc `JobPreconditions.isCharging()` không qua (BatteryManager).

#### 4.2.2 Logic của `onStartJob`

| Bước | Điều kiện | Thao tác |
|-----|---------|----------|
| 1 | Không cắm sạc (Not charging) | Gọi `jobFinished(reschedule=true)` |
| 2 | Threshold policy bị vô hiệu | Tắt tính năng ASM (`enabled=0`, `turned_off_by_policy=1`) |
| 3 | Dung lượng trống `freeBytes >= 15% * totalBytes` | Bỏ qua, lưu thời gian cuối bằng `automatic_storage_manager_last_run` |
| 4 | ASM **đang tắt** | Broadcast → Kích hoạt `NotificationController` hiện thông báo |
| 5 | ASM **đang bật** | Tiến hành gọi `StorageManagementJobProvider.onStartJob(..., daysToRetain)` |

**Ngưỡng "gần hết dung lượng":** Khi `freeBytes < totalBytes * 15 / 100`.

**Ngày được giữ lại (Days to retain):** Cờ `Settings.Secure.automatic_storage_manager_days_to_retain` (lấy theo cấu hình mảng tài nguyên 30/60/90 ngày).

Riêng đối với **phiên bản build hiện tại**, quá trình ở bước 5 bị **bỏ qua (no-op)**: vì `getStorageManagementJobProvider() == null`.

#### 4.2.3 Biến Settings.Secure

| Khóa (Key) | Ý nghĩa |
|-----|--------|
| `automatic_storage_manager_enabled` | Giá trị 0/1 — ASM bật hay tắt |
| `automatic_storage_manager_days_to_retain` | Giữ file media trong bao nhiêu ngày trước khi xóa đi |
| `automatic_storage_manager_last_run` | Timestamp thời gian chạy gần nhất |
| `automatic_storage_manager_turned_off_by_policy` | ASM bị tắt bởi hệ thống policy / vượt quá kỳ hạn deadline |

Các thuộc tính Property liên quan:

| Thuộc tính (Property) | Giá trị gốc | Ý nghĩa |
|----------|---------|--------|
| `ro.storage_manager.enabled` | false | Trường hợp true — có nghĩa ASM được "chính thức" hỗ trợ mặc định trên bản build; cảnh báo văn bản thay đổi |
| `debug.asm.period` | 86400000 | Chu kỳ Job tính bằng ms |
| `debug.asm.app_unused_limit` | 90 | Giới hạn ngày cho tiêu chí "không dùng ứng dụng" |
| `debug.asm.file_age_limit` | 0 | Giới hạn tối thiểu ngày cho tệp cũ của thư mục Downloads |
| `STORAGE_MANAGER_SHOW_OPT_IN_PROPERTY` | (chuỗi string trong dex) | Xử lý hành vi đăng ký (opt-in) |

---

### 4.3 Quản lý thông báo (Notifications - opt-in ASM)

`NotificationController` — đóng vai trò là một BroadcastReceiver + Channel `"storage"`.

| Action (Hành động) | Phản hồi ứng xử |
|--------|-----------|
| `…show_notification` / `SHOW_NOTIFICATION` | Trình thông báo, với điều kiện số lần cảnh báo không vượt giới hạn |
| `DEBUG_SHOW_NOTIFICATION` | Chế độ ép buộc phải hiển thị |
| `ACTIVATE` | Đặt `automatic_storage_manager_enabled=1`; trong tình huống `!ro.storage_manager.enabled` → hiện hộp thoại WarningDialog |
| `NO_THANKS` | Tạm ngưng và hẹn sau **90 ngày** hiển thị lại |
| `DISMISS` | Từ chối lần này (lùi lại **14 ngày**), tăng +1 counter |
| `SHOW_SETTINGS` | Mở `android.settings.INTERNAL_STORAGE_SETTINGS` |

Giới hạn (Limits) được theo dõi thông qua `NotificationController` trong SharedPreferences:

- Số lần hiển thị tối đa (max shown): **4**
- Số lần đóng bỏ tối đa (max dismissed): **9**
- Các lưu khóa (keys): `notification_shown_count`, `notification_dismiss_count`, `notification_next_show_time`

Các nút hành động trên notification: **No thanks** / **Turn on** (+ Khi chạm vào bảng thông báo → nhảy tới mục Settings).

---

### 4.4 Hộp thoại cảnh báo (Warning dialog)

Các lớp `WarningDialogActivity` + `WarningDialogFragment` — Chứa hộp thoại AlertDialog kèm theo tin nhắn thông báo kích hoạt cảnh báo, khi hệ thống kích hoạt ASM mà thiếu `ro.storage_manager.enabled`. Cửa sổ chỉ có mỗi nút OK → sau đó gọi lệnh finish để đóng lại.

---

## 5. Sơ đồ các class (hoàn chỉnh cho bộ tính năng)

### deletionhelper

| Class | Vai trò |
|-------|------|
| `DeletionHelperActivity` | Màn hình Host (Host Activity), thanh button bar, empty state, và ngưỡng (menu threshold) |
| `DeletionHelperSettings` | Chứa thẻ PreferenceFragment, đồng bộ danh mục, và chạy nút dọn (Free button) |
| `DeletionType` | Interface để tạo chuẩn loại nhóm xóa |
| `AppDeletionType` | Code backend xử lý apps + class cho phép LoaderCallbacks |
| `AppsAsyncLoader` | Scanner các apps + đọc usage + tổng sizes |
| `AppDeletionPreference` / `AppDeletionPreferenceGroup` | Quản lý khung UI để tick check các app |
| `DownloadsDeletionType` | Backend phân tích file trong Downloads |
| `FetchDownloadsLoader` | Quét nội dung danh bạ thư mục một cách đệ quy |
| `DownloadsDeletionPreferenceGroup` / `DownloadsFilePreference` | Nhóm xử lý UI mục Downloads |
| `PhotosDeletionPreference` | Khung điều khiển UI mục photos (nhờ cậy sang overlay layer) |
| `PackageDeletionTask` | Gỡ (Uninstall) app |
| `ConfirmDeletionDialog` | Bật lời xác nhận đồng ý thao tác dọn dẹp |
| `StorageManagerUpsellDialog` | Hiển thị ASM Upsell nhắc nhở |
| `GaugePreference` | Công cụ đồng hồ (Gauge) để thể hiện "app cần dung lượng X" |
| `CollapsibleCheckboxPreferenceGroup` | Thanh nhóm với nút bấm check tổng master-checkbox đóng/mở được |
| `NestedDeletionPreference` / `DeletionPreference` | Mục base item cơ bản |
| `LoadingSpinnerController` | Dùng Spinner chèn vào lúc đợi hiển thị xong dữ liệu |

### automatic

| Class | Vai trò |
|-------|------|
| `AutomaticStorageBroadcastReceiver` | Bắt Boot → và đặt schedule lên job |
| `AutomaticStorageManagementJobService` | Viết logic chu kỳ dọn ASM |
| `JobPreconditions` | Yêu cầu cắm sạc (isCharging) |
| `NotificationController` | Bộ định tuyến dạng máy trạng thái hữu hạn xử lý tính năng Opt-in notification |
| `WarningDialogActivity` / `WarningDialogFragment` | Xử lý hiển thị thông tin khi hoàn thành kích hoạt |

### overlay

| Class | Vai trò |
|-------|------|
| `FeatureFactory` / `FeatureFactoryImpl` | Cấu trúc mẫu Factory; **Ở file này Impl đóng vai stub (nghĩa là gán null providers)** |
| `DeletionHelperFeatureProvider` | Plugin gọi để xóa phim/ảnh (Photos/video) |
| `StorageManagementJobProvider` | Background xóa ngầm tự động qua plugin |

### utils

| Class | Vai trò |
|-------|------|
| `AsyncLoader` | Chứa Base Loader cơ sở |
| `IconProvider` | Hệ thống quản lý MIME/icon cho tệp |
| `PreferenceListCache` | Chứa file tái tạo cho thẻ preference items |
| `Utils` | Xử lý tạo hoạt cảnh làm mờ Loading container |
| `ButtonBarProvider` | Lớp interface chuẩn chứa Cancel/Free kẹp chung ở cuối ứng dụng |

---

## 6. Luồng Intent / giao tiếp với hệ thống

| Intent / extra (tham số) | Kẻ phát sinh (Sender) | Tác dụng |
|----------------|----------|--------|
| `android.os.storage.action.MANAGE_STORAGE` | Truyền đi từ Settings, hoặc của một số app | Để gọi lên màn hình quản lý (Deletion Helper) |
| `android.os.storage.extra.REQUESTED_BYTES` | Hệ caller phát intent yêu cầu bộ nhớ | Vẽ lên thanh hiển thị (Gauge "App needs X") |
| `android.settings.INTERNAL_STORAGE_SETTINGS` | Notification click (khi người dùng chạm) | Chuyển tới giao diện Settings của máy về Bộ Nhớ |
| `com.android.storagemanager.automatic.*` | Chạy Job / lệnh PendingIntents | Gọi quản lý các tiến trình (notification / ASM) |

Phương pháp (Mẫu gọi) điển hình để dùng nó (chuẩn AOSP):

```java
StorageManager sm = context.getSystemService(StorageManager.class);
// Trong quá trình hoạt động không đủ chỗ chứa (nếu có báo cạn kiệt), thiết bị hay mở bằng lệnh:
Intent i = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
i.putExtra(StorageManager.EXTRA_REQUESTED_BYTES, neededBytes);
context.startActivity(i);
```

---

## 7. Các tính năng đang hoạt động bình thường trên version v9 này

| Thuộc tính (Function) | Tình trạng (trên file APK version 9) |
|---------|---------------------|
| Giao diện người dùng Deletion Helper | ✅ |
| Khả năng Liệt kê danh sách + Xóa tự uninstall những app "bỏ hoang" | ✅ (Tất nhiên, bắt buộc có permission như DELETE_PACKAGES + USAGE_STATS) |
| Tự quét list/Xóa tệp Downloads | ✅ (yêu cầu cấp cho quyền READ/WRITE của storage) |
| Mục gỡ ảnh đồng bộ/ video clip (Photos & videos cleanup) | ❌ bị gán stub overlay |
| Khả năng tự động Job chạy dọn ẩn ngầm định kỳ file media | ❌ bị gán stub overlay |
| Hệ cảnh báo notification thông minh gọi nhắc vì cạn bộ nhớ | ✅ |
| Tính năng đánh cờ cấp quyền bật tắt ASM của mục Secure settings | ✅ |
| Xóa và auto clean thành công trên máy bằng app gốc sau cắm quyền ASM | ❌ Không, thiếu framework của GMS/OEM làm overlay |

---

## 8. Ứng dụng tái sử dụng vào công cụ `geely_ex2_tools`

Về cốt lõi **không** khuyến nghị việc sao y toàn vẹn cấu trúc giao diện "AOSP PreferenceFragment UI", thay vào đó hãy vay mượn lại những module thiết kế chuẩn:

### 8.1 Mô hình đáng sao chép

1. **Trình Scanner quét app "tử ngỏ"** — Áp dụng các thư viện `UsageStatsManager` + `StorageStatsManager` kết hợp hệ thống chặn lọc các ứng dụng có số ngày ít truy cập (N days) + bỏ chừa các ứng dụng quan trọng cho (hệ thống/launcher ra).
2. **Loại bỏ file tạp khỏi mục Downloads / thư mục cache** — Duyệt thư mục dạng cây đệ quy + kèm thông tin cảnh báo tuổi tệp (age threshold) + hỏi hộp thoại đồng ý.
3. **Màn theo dõi Watchdog khi không gian tụt** — Bằng cách lập 1 lệnh báo hằng ngày qua `Job/WorkManager`: Cụ thể khi `free/total < ngưỡng (threshold)` → tự báo push notification / gọi màn dọn tệp rác ra màn hình.
4. **Các dạng cờ giống với mô hình Settings.Secure** — Với dữ liệu hiện tại đang sẵn dùng trên thư viện nội (như `AppKv` / DataStore); sẽ cho tương thích thay cờ kiểu như `*_enabled`, `*_last_run`, `*_days_to_retain`.
5. **Giới hạn tốc độ/thúc (Upsell / rate-limit) hiển thị notification** — Biến theo dõi chống spam (bằng SharedPreferences) để delay trì hoãn các lượt hiển thị lần nữa thành 14/90 ngày.

### 8.2 Các chi tiết cần thay đổi để thích ứng cho màn xe Geely EX2 (ГУ)

| Mẫu AOSP | Giải pháp tại ГУ Geely (cho ứng dụng riêng) |
|------|-------------------|
| `deletePackageAsUser` | Quyền này chỉ dùng cho ứng dụng được ủy nhiệm khóa system (system-signed / privileged); Còn nếu thông thường — bạn đành gọi clear cache / dẫn dắt mở Setting mặc định ra |
| Overlay Photos Google | Xóa bỏ vì hệ không cần; nên thay qua dọn cho các cổng media ở khe cắm USB, ứng dụng dẫn đường (map cache), các file logs, những file APK tải dở ở khu Download |
| Cấu trúc Job yêu cầu 2 trạng thái cắm sạc+nghỉ (charging+idle) | Vì tính năng cắm điện (charging) là phụ thuộc nguồn điện ô tô (nguồn liên tục) nên giá trị từ cảm biến (BatteryManager) có thể báo trạng thái ảo; máy ô tô (always-on HU) cũng hay nhận tình trạng nghỉ (idle) luôn luôn |
| `MANAGE_STORAGE` | Chú ý ta có quyền thêm intent activity mang action tương đồng nhưng **chỉ khi** ta được cài đè lên thành chức năng StorageManager hệ thống chính; nếu cài với ứng dụng thứ ba không root thì xài theo chuẩn đường dẫn (deep-link) nội bộ |
| Thiết lập bộ giới hạn ngưỡng 15% | Thiết bị ГУ thường chỉ cho bộ nhớ thấp (small userdata), cần đổi ra dạng tính tuyệt đối dễ nhìn (ví dụ thông báo: còn < 500 MB) |

### 8.3 Kịch bản khung (feature-skeleton) tinh giản cho lập trình viên

```text
feature/storage/
  ui/StorageCleanupScreen.kt          # Hàm Compose: dọn dẹp apps / downloads / mục caches
  StorageCleanupViewModel.kt
data/storage/
  StorageStatsRepository.kt           # Tính free/total, dung lượng của mỗi app (per-app sizes)
  UnusedAppsScanner.kt                # Ứng dụng UsageStats + lọc tuổi (age filter)
  DownloadsCleaner.kt                 # Khảo sát danh sách walk + delete file
  StorageWatchdog.kt                  # Job check định kỳ
  StorageSettings.kt                  # Set biến enabled, threshold, lastRun (Sử dụng AppKv)
```

**Cấu hình manifest System flavor:** Cho các quyền như `PACKAGE_USAGE_STATS`, `DELETE_PACKAGES` hoặc tùy chỉnh `CLEAR_APP_CACHE`, kèm `MANAGE_EXTERNAL_STORAGE` (hoặc legacy storage), riêng khi chạy app với `system UID` — thì quyền hệ thống `WRITE_SECURE_SETTINGS` không thiết yếu nữa.

### 8.4 Những thành phần loại bỏ (không thể bê y chang qua)

- Dạng giao diện cấu trúc thuộc Support Library Preference UI + kèm luôn các mảnh settingslib blobs dư thừa được khai bên trong file dex.
- Chế độ tích hợp dành cho Google Photos (overlay).
- Cần chú ý giả định thiết bị "điện thoại (phone)" trên dòng tin nhắn thông báo (vd: "điện thoại sắp hết chỗ chứa") — chúng ta phải thay các câu text (strings) này thành từ phù hợp màn ô tô (HU).

---

## 9. Mã hành vi (MetricsLogger actions)

Ghi nhận các action trả lại cho mã sự kiện Confirm / Free / Cancel:

| Mã Action (id) | Lúc nào thì xảy ra |
|-----------|--------|
| 467 | Nhấn lệnh làm rảnh bộ nhớ (Free) (Hiển thị form xác minh Confirm) |
| 468 | Hủy lệnh (Cancel) |
| 469 | Đồng ý thao tác xóa hoàn toàn (Confirm delete OK) |
| 470 | Từ chối trên lệnh confirm |
| 471 | Xảy ra lỗi ngoài mong muốn khi dỡ app (uninstall packages) |
| 472 | Hỏng việc trong khi delete danh bạ downloads |

---

## 10. Bảng liệt kê các chức năng cơ bản (Cần Check)

- [x] Sắp đặt UI cơ bản cho thao tác xóa tay (Deletion Helper) bắt nguồn bằng cờ `MANAGE_STORAGE`
- [x] Hiện chỉ số thiết kế Gauge tính dung lượng "ứng dụng tốn thêm N Bytes"
- [x] Lọc ra chuyên mục app hãn hữu được truy cập (từ 90 days / show all)
- [x] Tháo gỡ các apps được gắn đánh dấu tick uninstall
- [x] Chỉ mục hạng (Downloads) + dọn đi tệp dữ liệu đã tìm
- [x] Chuyên mục "ảnh đã backed-up" (Chỉ làm dáng UI without core backend stub)
- [x] Cửa sổ xác minh (Confirm dialog) + kêu gọi mở auto upsell tự dọn
- [x] Hiển thị Empty state trường hợp trống tệp + Nút ấn toggle chuyển qua hiển thị No-threshold
- [x] Gọi chạy Job ẩn periodic từ lệnh bật máy (khi cắm nguồn sạc + nhàn hạ idle)
- [x] Theo dõi Job báo lỗi khi dung lượng (free < 15%)
- [x] Theo dõi Job thông qua notification (gửi request opt-in nếu module ASM tắt)
- [x] Theo dõi Job qua Overlay ngầm nếu cơ chế đã bật ASM on (Stub ẩn)
- [x] Thực thi hệ notification actions có kèm các phím: Activate / No thanks / Dismiss / Mở truy xuất Settings
- [x] Chạy Warning dialog (Hộp màn hình nhắc nhỏ) vừa lúc Active xong
- [x] Bật lưu dạng cấp cờ Secure settings
- [x] Thông số Properties về threshold chu kỳ (Chuẩn cho gỡ lỗi Debug)
- [x] Xây các nút chặn cắm FeatureFactory (hook overlay - đã gán stub)

---

## 11. Các hồ sơ/tài liệu tham chiếu liên quan (Reference links)

| Tài liệu | Gắn kết |
|----------|--------|
| [android-shell-apk.md](./android-shell-apk.md) | Thêm 1 ứng dụng app hệ thống AOSP chạy song song (để chạy shell / bugreport) |
| [managedprovisioning-apk.md](./managedprovisioning-apk.md) | Kiến thức nền về AOSP provisioning |
| [system-install.md](./system-install.md) | Mô tả chi tiết cách nhét apps dưới dạng app có độ ưu tiên privileged trên xe (ГУ) |
