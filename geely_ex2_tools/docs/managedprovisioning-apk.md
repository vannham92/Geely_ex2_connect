# com.android.managedprovisioning — Hướng dẫn phân tích APK (Work profile setup)

Tài liệu này mô tả ứng dụng hệ thống **Managed Provisioning** (`com.android.managedprovisioning`) từ màn hình trung tâm Geely **IHU629G**: bao gồm cấu trúc bên trong APK, cách khởi động thiết lập hồ sơ công việc (work profile) / chủ sở hữu thiết bị (device owner), và các intent/extra mà quá trình cung cấp (provisioning) Android Enterprise sử dụng.

**Lưu ý quan trọng:** Đây **không phải** là một ứng dụng của Flyme và **cũng không phải** ứng dụng dành riêng cho ô tô (automotive). APK này là một thành phần tiêu chuẩn của **AOSP Android 9** dùng để quản lý thiết bị doanh nghiệp (MDM / DPC). Trên màn hình trung tâm (GU/HU), nó tồn tại như một phần của bản rom hệ thống (system image); nó **không kết nối** với VHAL, `com.flyme.auto.api` hay logic của xe hơi. Trong tệp dex **không tìm thấy** bất kỳ chuỗi (string) nào chứa từ khóa `flyme`, `geely` hoặc `ecarx`.

---

## 0. Tổng quan ứng dụng

| Thông số | Giá trị |
|----------|----------|
| Gói ứng dụng (Package) | `com.android.managedprovisioning` |
| Nhãn (Label - EN) | **Work profile setup** |
| Nhãn (Label - RU/CN) | **Настройка рабочего профиля** (Thiết lập hồ sơ công việc) |
| versionCode | `28` |
| versionName | `9` |
| minSdk / targetSdk / compileSdk | 28 / 28 / 28 (Android 9) |
| sharedUserId | **không có** (là một priv-app thông thường) |
| Kích thước APK | ~2.9 MB |
| DEX | Chỉ có một file `classes.dex` (~1162 lớp, trong đó có **174** lớp thuộc `com.android.managedprovisioning.*`) |
| Điểm vào chính (Entry point) | `PreProvisioningActivity` (xử lý intent actions provisioning) |

**Mục đích:** Hướng dẫn người dùng (hoặc hệ thống MDM) thực hiện các kịch bản của Android Enterprise:

1. **Work profile** (profile owner - chủ sở hữu hồ sơ) — Tạo một hồ sơ công việc riêng biệt trên thiết bị cá nhân.
2. **Fully managed device** (device owner - chủ sở hữu thiết bị) — Thiết bị được quản lý toàn diện.
3. **Managed user** — Một người dùng riêng biệt được quản lý.
4. **Shareable device** — Chế độ doanh nghiệp trên thiết bị dùng chung.
5. **Silent device owner** — Cài đặt ngầm quyền chủ sở hữu thiết bị (cần quyền hệ thống).

Luồng xử lý điển hình: Ứng dụng MDM/DPC gửi một intent với action `android.app.action.PROVISION_*` và các extras `android.app.extra.PROVISIONING_*` → `PreProvisioningActivity` (đồng ý điều khoản, mã hóa, terms) → `ProvisioningActivity` (chuỗi nhiệm vụ - task) → `FinalizationActivity`.

**Cấu trúc ngăn xếp (theo dex):**

- UI: **SetupWizard/Glif** (`SetupGlifLayoutActivity`, các layout `suw_*`)
- Mô hình dữ liệu (Model): `ProvisioningParams`, `WifiInfo`, `CustomizationParams`
- Bộ điều phối (Orchestration): `ProvisioningManager` → `ProvisioningControllerFactory` → `ProfileOwnerProvisioningController` / `DeviceOwnerProvisioningController`
- Các bước thực thi (Tasks): `com.android.managedprovisioning.task.*` (Cài đặt Wi-Fi, tải/cài đặt DPC, tạo hồ sơ, chính sách thiết bị)
- Bộ phân tích cú pháp (Parsers): Phân tích NFC NDEF, QR/properties (`PropertiesProvisioningDataParser`, `ExtrasProvisioningDataParser`)

**Mối liên kết với các APK khác trong dự án:**

| APK | Mối liên kết |
|-----|-------|
| Flyme Settings / HVAC / Car service | **Không có liên kết trực tiếp** |
| Geely EX2 Tools | Chỉ có thể gọi intent provisioning **nếu** có kịch bản MDM và cấp quyền hệ thống |
| Bất kỳ DPC (Device Policy Controller) nào | Đây là **ứng dụng đích (client)** — được APK này cài đặt để hoạt động như một gói quản trị viên (admin package) |

---

## 1. Nguồn gốc và Artifacts

| Thông số | Giá trị |
|----------|----------|
| Nền tảng (Nguồn bản dump) | IHU629G |
| APK gốc (Từ ADBAppControl) | `downloads/250060 IHU629G/Настройка рабочего профиля (com.android.managedprovisioning) [v.9].apk` |
| Bản sao cục bộ | `.tmp/managedprovisioning.apk` |
| APK đã giải nén | `.tmp/managedprovisioning-apk/` |

### Lấy APK từ thiết bị

```bash
adb shell pm path com.android.managedprovisioning
adb pull /system/priv-app/ManagedProvisioning/ManagedProvisioning.apk .tmp/managedprovisioning.apk
```

> Lưu ý: Đường dẫn chính xác trên ROM có thể khác nhau (có thể là `ManagedProvisioning`, `Provision`, v.v.) — cần kiểm tra kết quả đầu ra của lệnh `pm path`.

### Giải nén và tìm kiếm

```powershell
Copy-Item -LiteralPath ".tmp\managedprovisioning.apk" -Destination ".tmp\managedprovisioning.zip"
Expand-Archive -LiteralPath .tmp\managedprovisioning.zip -DestinationPath .tmp\managedprovisioning-apk -Force

$aapt = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter "aapt.exe" | Select-Object -First 1).FullName
& $aapt dump badging .tmp\managedprovisioning.apk
& $aapt dump xmltree .tmp\managedprovisioning.apk AndroidManifest.xml

$dexdump = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter "dexdump.exe" | Select-Object -First 1).FullName
& $dexdump -f .tmp\managedprovisioning-apk\classes.dex | Select-String "managedprovisioning"
& $dexdump -d .tmp\managedprovisioning-apk\classes.dex | Select-String "PROVISIONING_"
```

**JADX** — Dùng để đọc `PreProvisioningController`, `ProvisioningManager`, và các lớp task (không có mã dịch ngược sẵn trong bản dump).

---

## 2. Kiến trúc

```mermaid
flowchart TB
    subgraph triggers [Các bộ kích hoạt provisioning (Triggers)]
        MDM[Ứng dụng DPC / MDM]
        NFC[Chạm NFC NDEF]
        TRUST[Ứng dụng nguồn tin cậy]
        SILENT[Gửi Broadcast Silent DO]
    end

    subgraph pre [Tiền xử lý - preprovisioning]
        PPA[PreProvisioningActivity]
        TERMS[TermsActivity]
        ENC[EncryptDeviceActivity]
        WEB[WebActivity :webview]
    end

    subgraph core [Cốt lõi - provisioning]
        PA[ProvisioningActivity]
        PM[ProvisioningManager]
        PCF[ProvisioningControllerFactory]
        POC[ProfileOwnerProvisioningController]
        DOC[DeviceOwnerProvisioningController]
        TASKS[Chuỗi task.*]
    end

    subgraph post [Hoàn thiện - finalization]
        FA[FinalizationActivity]
        DPC[Ứng dụng DPC đã cài đặt]
    end

    MDM -->|Intent PROVISION_*| PPA
    NFC -->|NDEF application/com.android.managedprovisioning| PPA
    TRUST -->|PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE| PPA
    SILENT -->|PROVISION_MANAGED_DEVICE_SILENTLY| SilentDO[SilentDeviceOwnerProvisioningReceiver]

    PPA --> TERMS
    PPA --> ENC
    PPA --> WEB
    PPA --> PA
    PA --> PM --> PCF
    PCF -->|Nếu là isDeviceOwnerAction| DOC
    PCF -->|Ngược lại profile/user| POC
    DOC --> TASKS
    POC --> TASKS
    TASKS --> FA
    FA --> DPC
```

### 2.1 Chọn bộ điều khiển (Controller)

Trong `ProvisioningControllerFactory.createProvisioningController()` (dựa theo mã dex):

- Nếu `Utils.isDeviceOwnerAction(action)` → Chọn `DeviceOwnerProvisioningController`
- Ngược lại → Chọn `ProfileOwnerProvisioningController`

Dựa vào các chuỗi bên trong `Utils` (dùng dexdump):

| Phương thức | Actions (trả về true) |
|-------|----------------|
| `isDeviceOwnerAction` | `android.app.action.PROVISION_MANAGED_DEVICE`, `android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE` |
| `isProfileOwnerAction` | `android.app.action.PROVISION_MANAGED_PROFILE`, `android.app.action.PROVISION_MANAGED_USER` |

---

## 3. Các thành phần trong Manifest

### 3.1 Activities

| Lớp (Class) | Mục đích |
|-------|------------|
| `PreProvisioningActivity` | Bộ xử lý chính cho các intent provisioning; chứa hoạt ảnh intro, thực hiện các kiểm tra, định tuyến (routing) |
| `PreProvisioningActivityViaNfc` (alias) | Xử lý NFC NDEF `application/com.android.managedprovisioning` |
| `PreProvisioningActivityViaTrustedApp` (alias) | Provisioning từ một ứng dụng nguồn tin cậy |
| `PreProvisioningActivityAfterEncryption` (alias) | Khôi phục lại quá trình sau khi mã hóa (`RESUME_PROVISIONING`) |
| `TermsActivity` | Màn hình hiển thị điều khoản / từ chối trách nhiệm (disclaimer) |
| `EncryptDeviceActivity` | Yêu cầu mã hóa thiết bị trước khi thực hiện provisioning |
| `PostEncryptionActivity` | HOME-alias dùng để tiếp tục luồng sau khi khởi động lại máy (mặc định bị vô hiệu hóa - disabled) |
| `WebActivity` (`:webview`) | Chạy trình duyệt WebView cho các liên kết URL (hỗ trợ, disclaimer) |
| `ProvisioningActivity` | Hiển thị tiến trình chính của việc provisioning (chuỗi task) |
| `FinalizationActivity` | Hoàn thiện quy trình (`PROVISION_FINALIZATION`) |
| `TrampolineActivity` | Chuyển tiếp intent (startActivityForResult + finish) |

### 3.2 Services

| Lớp (Class) | Mục đích |
|-------|------------|
| `ProvisioningService` | Service ảo (Stub service) (`onBind` → trả về null) |
| `SilentDeviceOwnerProvisioningService` | Thực hiện quy trình cài đặt ngầm device owner |
| `OtaService` | Các hàm hỗ trợ OTA cho trạng thái provisioning |

### 3.3 Receivers

| Lớp (Class) | Hành động (Action) |
|-------|--------|
| `SilentDeviceOwnerProvisioningReceiver` | `android.app.action.PROVISION_MANAGED_DEVICE_SILENTLY` |
| `BootReminder` | `BOOT_COMPLETED` — Nhắc nhở người dùng về quá trình provisioning chưa hoàn tất |
| `PreBootListener` | `PRE_BOOT_COMPLETED` |
| `ManagedUserCreationListener` | `android.app.action.MANAGED_USER_CREATED` |
| `CrossProfileIntentFiltersSetter$RestrictionChangedReceiver` | `DATA_SHARING_RESTRICTION_CHANGED` |
| `DpcReceivedSuccessReceiver` | (Dùng trong bước hoàn thiện - finalization) |

### 3.4 Quyền tùy chỉnh riêng (Custom permission)

| Quyền hạn (Permission) | Cấp độ bảo vệ (protectionLevel) | Thành phần sử dụng |
|------------|-----------------|----------------|
| `android.permission.PROVISION_MANAGED_DEVICE_SILENTLY` | signature\|privileged | `SilentDeviceOwnerProvisioningReceiver` |

---

## 4. Intent actions và điểm vào (entry points)

### 4.1 Các action của Provisioning (Vào PreProvisioningActivity)

| Hành động (Action) | Kịch bản |
|--------|----------|
| `android.app.action.PROVISION_MANAGED_PROFILE` | **Hồ sơ công việc** (chủ sở hữu hồ sơ - profile owner) |
| `android.app.action.PROVISION_MANAGED_USER` | Người dùng phụ được quản lý (Managed secondary user) |
| `android.app.action.PROVISION_MANAGED_DEVICE` | Thiết bị được quản lý toàn diện (chủ sở hữu thiết bị - device owner) |
| `android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE` | Chế độ dùng chung / Thuộc sở hữu công ty |
| `android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE` | Kích hoạt từ ứng dụng tin cậy |
| `android.app.action.PROVISION_MANAGED_DEVICE_SILENTLY` | Cài đặt ngầm thiết bị (qua receiver) |
| `com.android.managedprovisioning.action.RESUME_PROVISIONING` | Tiếp tục quy trình sau khi khởi động lại để mã hóa |

### 4.2 Các action khác (broadcast / finalization)

| Hành động (Action) | Mục đích |
|--------|------------|
| `android.app.action.PROVISION_FINALIZATION` | Gọi `FinalizationActivity` |
| `android.app.action.PROVISIONING_SUCCESSFUL` | (Chứa trong dex, dùng cho phân tích/callbacks) |
| `android.app.action.MANAGED_PROFILE_PROVISIONED` | Hồ sơ đã được tạo thành công |
| `android.app.action.PROFILE_PROVISIONING_COMPLETE` | Hoàn tất việc provisioning cấu hình |
| `android.app.action.MANAGED_USER_CREATED` | Người dùng quản lý đã được tạo |
| `android.app.action.START_ENCRYPTION` | Bắt đầu mã hóa thiết bị |
| `android.app.action.DATA_SHARING_RESTRICTION_CHANGED` | Thay đổi các giới hạn chia sẻ dữ liệu giữa các cấu hình (cross-profile) |

### 4.3 NFC

| Kiểu MIME | Alias liên kết |
|------|-------|
| `application/com.android.managedprovisioning` | `PreProvisioningActivityViaNfc` |

Yêu cầu quyền hạn `android.permission.DISPATCH_NFC_MESSAGE`.

---

## 5. Dữ liệu bổ sung Intent (`android.app.extra.PROVISIONING_*`)

Các khóa (keys) được tìm thấy bên trong `classes.dex` (thuộc API tiêu chuẩn của Android 9 provisioning):

| Extra | Mục đích |
|-------|------------|
| `PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME` | ComponentName của ứng dụng DPC (admin receiver) |
| `PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME` | Tên gói (package) của ứng dụng DPC |
| `PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION` | Link URL tải file APK DPC |
| `PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM` | Mã kiểm tra SHA-256 của file APK |
| `PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM` | Mã kiểm tra chữ ký (signature) của DPC |
| `PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER` | Header Cookie cần dùng để tải xuống |
| `PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE` | Phiên bản (versionCode) tối thiểu của DPC |
| `PROVISIONING_DEVICE_ADMIN_PACKAGE_ICON_URI` | Biểu tượng (Icon) của DPC |
| `PROVISIONING_DEVICE_ADMIN_PACKAGE_LABEL` | Tên (Label) của DPC |
| `PROVISIONING_ADMIN_EXTRAS_BUNDLE` | Gói dữ liệu Bundle truyền thẳng tới DPC |
| `PROVISIONING_ACCOUNT_TO_MIGRATE` | Tài khoản cần di chuyển (migrate) sang cấu hình công việc |
| `PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION` | Giữ nguyên tài khoản bên cấu hình cá nhân (personal side) |
| `PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED` | Không gỡ bỏ các ứng dụng hệ thống (system apps) |
| `PROVISIONING_SKIP_ENCRYPTION` | Bỏ qua bước mã hóa thiết bị |
| `PROVISIONING_SKIP_USER_CONSENT` | Bỏ qua giao diện hỏi ý kiến người dùng |
| `PROVISIONING_SKIP_USER_SETUP` | Bỏ qua phần thiết lập người dùng |
| `PROVISIONING_LOCAL_TIME` | Đặt giờ địa phương trong quá trình provisioning |
| `PROVISIONING_TIME_ZONE` | Đặt múi giờ (Timezone) |
| `PROVISIONING_LOCALE` | Đặt ngôn ngữ (Locale) |
| `PROVISIONING_WIFI_SSID` | Tên Wi-Fi (SSID) để tự động kết nối tải DPC |
| `PROVISIONING_WIFI_PASSWORD` | Mật khẩu Wi-Fi |
| `PROVISIONING_WIFI_SECURITY_TYPE` | Phương thức bảo mật Wi-Fi |
| `PROVISIONING_WIFI_HIDDEN` | Mạng Wi-Fi bị ẩn (Hidden SSID) |
| `PROVISIONING_WIFI_PROXY_HOST` / `_PORT` / `_BYPASS` | Thông số cài đặt Proxy Wi-Fi |
| `PROVISIONING_WIFI_PAC_URL` | Đường dẫn PAC URL |
| `PROVISIONING_USE_MOBILE_DATA` | Cho phép sử dụng dữ liệu di động (Mobile data) |
| `PROVISIONING_LOGO_URI` | Đường dẫn Logo của tổ chức |
| `PROVISIONING_MAIN_COLOR` | Màu chủ đạo (Accent color) cho giao diện |
| `PROVISIONING_ORGANIZATION_NAME` | Tên của tổ chức doanh nghiệp |
| `PROVISIONING_SUPPORT_URL` | Đường link hỗ trợ |
| `PROVISIONING_DISCLAIMERS` / `_HEADER` / `_CONTENT` | Các tài liệu giới hạn trách nhiệm (Disclaimer docs) |

Toàn bộ danh sách extras dùng cho phân tích (analytics) được lọc bằng tiền tố `android.app.extra.PROVISIONING_` (`AnalyticsUtils.isValidProvisioningExtra`).

---

## 6. Chuỗi tác vụ (Task chain - Các bước provisioning)

Các lớp đảm nhiệm tác vụ (task) nằm trong thư mục `com.android.managedprovisioning.task` (dựa trên dex):

| Tác vụ (Task) | Mục đích |
|------|------------|
| `AddWifiNetworkTask` | Tự động kết nối Wi-Fi từ thông số extras |
| `ConnectMobileNetworkTask` | Bật dữ liệu di động (mobile data) để tải tệp |
| `DownloadPackageTask` | Tải file APK DPC thông qua liên kết URL |
| `VerifyPackageTask` | Kiểm tra tính toàn vẹn qua Checksum / Chữ ký |
| `InstallPackageTask` | Cài đặt ứng dụng DPC |
| `InstallExistingPackageTask` | Nếu DPC đã có trong system — bật (enable) ứng dụng lên |
| `CreateManagedProfileTask` | Tạo hồ sơ (managed profile) / người dùng quản lý |
| `StartManagedProfileTask` | Khởi động cấu hình sau khi thiết bị được mở khóa |
| `CopyAccountToUserTask` | Di chuyển (Migrate) tài khoản Google/người dùng |
| `DeleteNonRequiredAppsTask` | Gỡ cài đặt các ứng dụng không cần thiết khỏi cấu hình/DO |
| `ManagedProfileSettingsTask` | Cài đặt cấu hình (biểu tượng, tên gọi, màu sắc) |
| `CrossProfileIntentFiltersSetter` | Thiết lập bộ lọc Intent liên cấu hình (Cross-profile intent filters) |
| `DisableInstallShortcutListenersTask` | Vô hiệu hóa tính năng cài đặt phím tắt (shortcut listeners) |
| `SetDevicePolicyTask` | Kích hoạt device/profile owner |
| `DeviceOwnerInitializeProvisioningTask` | Khởi tạo thông số cho device owner |
| `DisallowAddUserTask` | Chặn tính năng thêm người dùng (áp dụng cho DO) |
| `MigrateSystemAppsSnapshotTask` | Lưu ảnh chụp (Snapshot) các ứng dụng hệ thống (áp dụng cho DO) |

Thứ tự thực thi chính xác được quyết định bởi `ProfileOwnerProvisioningController` / `DeviceOwnerProvisioningController` (phụ thuộc vào các extras: ví dụ như có cần tải xuống không, Wi-Fi, di chuyển tài khoản, v.v.).

---

## 7. Giao diện (UI layouts - riêng cho provisioning)

Ngoài giao diện AppCompat/SetupWizard (như `abc_*`, `suw_*`), bên trong APK còn chứa các layout màn hình sau:

| Tên Layout | Màn hình |
|--------|-------|
| `intro_profile_owner.xml` | Màn hình giới thiệu (Intro) work profile |
| `intro_device_owner.xml` | Màn hình giới thiệu device owner |
| `intro_profile_owner_info_buttons.xml` | Các nút chức năng info trên màn hình intro |
| `intro_animation.xml` / `intro_animation_captions.xml` | Hình động trình bày các lợi ích (benefits) |
| `accept_and_continue_footer.xml` | Khối Footer "chấp nhận và tiếp tục" |
| `encrypt_device.xml` | Giao diện thông báo yêu cầu Mã hóa (Encryption) |
| `terms_screen.xml` | Danh sách các điều khoản (Terms list) |
| `terms_disclaimer_header.xml` / `terms_disclaimer_content.xml` | Thông báo từ chối trách nhiệm (Disclaimer) |
| `delete_managed_profile_dialog.xml` | Khung thoại cảnh báo xóa hồ sơ quản lý |
| `progress.xml` | Màn hình hiển thị thanh tiến trình (Progress) |
| `device_manager_icon_label.xml` | Layout chứa biểu tượng (Icon) + tên DPC |

Các file ảnh động (Animations): `enterprise_wp_*` (work profile), `enterprise_do_*` (device owner).

---

## 8. Quyền hạn (Permissions - uses-permission)

Vì là ứng dụng hệ thống (priv-app), APK này yêu cầu nhiều quyền **hệ thống** mạnh:

| Quyền (Permission) | Lý do sử dụng |
|------------|-------|
| `MANAGE_USERS` | Tạo cấu hình / người dùng quản lý |
| `MANAGE_PROFILE_AND_DEVICE_OWNERS` | Giao quyền PO/DO |
| `MANAGE_DEVICE_ADMINS` / `BIND_DEVICE_ADMIN` | Kích hoạt thiết lập DPC |
| `INSTALL_PACKAGES` / `DELETE_PACKAGES` | Cài đặt/gỡ cài đặt DPC và các ứng dụng |
| `INTERACT_ACROSS_USERS` / `_FULL` | Định cấu hình liên người dùng (Cross-user provisioning) |
| `MANAGE_ACCOUNTS` | Di chuyển tài khoản giữa các người dùng |
| `CRYPT_KEEPER` / `MASTER_CLEAR` | Điều khiển tiến trình mã hóa thiết bị |
| `WRITE_SECURE_SETTINGS` / `WRITE_SETTINGS` | Sửa đổi System settings trong quá trình thiết lập |
| `CHANGE_COMPONENT_ENABLED_STATE` | Bật/tắt trạng thái hoạt động của các components |
| `CONNECTIVITY_INTERNAL` / Các quyền Wi-Fi | Thiết lập mạng Internet |
| `NFC` | Khởi chạy Provisioning thông qua NFC |
| `FOREGROUND_SERVICE` | Chạy dịch vụ provisioning trong thời gian dài |
| `RECEIVE_BOOT_COMPLETED` | Khôi phục tiến trình sau khi khởi động lại |
| `SHUTDOWN` | Khởi động lại máy để áp dụng mã hóa |
| `SET_TIME` / `SET_TIME_ZONE` | Đồng bộ thời gian từ thông số extras |
| `PEERS_MAC_ADDRESS` | Các lệnh hỗ trợ thiết lập Wi-Fi |

---

## 9. Các thuộc tính hệ thống và Quá trình Mã hóa

Lớp `Utils.isEncryptionRequired()` (từ dex):

- Kiểm tra xem thiết bị đã bị mã hóa phần cứng vật lý hay chưa;
- Đọc biến môi trường **`persist.sys.no_req_encrypt`** — Nếu biến này là `true`, bước yêu cầu mã hóa có thể bị bỏ qua.

Trên màn hình ô tô (automotive HU), thuộc tính này do nhà sản xuất (OEM) quy định; đối với dòng IHU629G, không tìm thấy kiểm tra bổ sung nào dành riêng cho thiết bị này.

---

## 10. Liên hệ với bối cảnh IHU629G / Geely EX2 Tools

| Câu hỏi | Trả lời |
|--------|-------|
| Có phải là phiên bản tùy chỉnh của Geely/Flyme không? | **Không** — Đây là mã nguồn thuần (stock) AOSP Android 9, với versionName `9` |
| Có cần thiết cho dự án EX2 Tools không? | **Không**, trừ khi bạn đang triển khai tính năng MDM/enterprise |
| Khi nào ứng dụng này mới cần thiết? | Dùng cho quản lý đội xe doanh nghiệp (fleet MDM), chế độ ki-ốt (kiosk), hoặc chạy ứng dụng công việc trong một profile riêng biệt |
| Chạy nó theo cách thủ công như thế nào? | Gửi Intent từ ứng dụng DPC hoặc qua lệnh `adb shell am start` kết hợp `PROVISION_*` + extras (yêu cầu máy userdebug/quyền hệ thống) |
| Có xung đột với tính năng khóa màn hình khi lái xe (driving restrictions) không? | Hồ sơ công việc (Work profile) hoạt động bên trong `UserManager`; còn Flyme restrictions (`com.flyme.auto`) thuộc một lớp quản lý (layer) hoàn toàn riêng biệt |

### Ví dụ: Kiểm tra xem gói đã được cài đặt chưa

```bash
adb shell pm list packages com.android.managedprovisioning
adb shell dumpsys package com.android.managedprovisioning | head -40
```

### Ví dụ: Kích hoạt intent work profile (ở mức cơ bản, yêu cầu phải có một DPC hợp lệ)

```bash
adb shell am start -a android.app.action.PROVISION_MANAGED_PROFILE \
  -e android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME \
     com.example.dpc/.AdminReceiver
```

> Lưu ý: Nếu chạy lệnh này trên màn hình HU (production) mà không có hệ thống MDM phù hợp, ứng dụng có thể gặp lỗi và đóng lại, hoặc hiển thị giao diện UI yêu cầu xác nhận.

---

## 11. Cấu trúc các gói mã nguồn (trong dex)

| Gói thư mục (Package) | Số lượng Lớp (≈) | Chức năng (Role) |
|-------|-------------|------|
| `com.android.managedprovisioning.provisioning` | ~15 | Các Activity, Manager, và Controllers |
| `com.android.managedprovisioning.preprovisioning` | ~20 | Quá trình tiền thiết lập (Pre-flow), mã hóa thiết bị, điều khoản, web |
| `com.android.managedprovisioning.task` | ~25 | Các lớp chứa logic Provisioning tasks |
| `com.android.managedprovisioning.model` | ~10 | Mô hình cấu trúc dữ liệu (Data model) |
| `com.android.managedprovisioning.parser` | ~6 | Bộ phân tích Intent và NFC |
| `com.android.managedprovisioning.finalization` | ~5 | Giao diện hiển thị sau khi hoàn tất thiết lập (Post-provision UI) |
| `com.android.managedprovisioning.analytics` | ~5 | Các chức năng đo lường (MetricsLogger, timing) |
| `com.android.managedprovisioning.common` | ~20 | Lớp tiện ích (Utils), hộp thoại (dialogs), các hàm cơ sở cho SetupGlif |
| `com.android.managedprovisioning.ota` | ~4 | OTA / khởi động pre-boot |
| `com.android.managedprovisioning.manageduser` | ~3 | Quản lý việc tạo cấu hình người dùng |
| `android.support.*` / `android.arch.*` | Hơn 900+ | Các thư viện hỗ trợ truyền thống (Legacy support libraries) |

---

## 12. Tài liệu tham khảo

- [Android Enterprise — Work profile](https://developer.android.com/work/managed-profiles)
- [Device admin / Device owner provisioning](https://developer.android.com/work/dpc/build-device-owner)
- Mã nguồn AOSP (Android 9): Thư mục `packages/apps/ManagedProvisioning/`
