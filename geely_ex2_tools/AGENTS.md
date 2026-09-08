# Workspace Rules for Antigravity

This file contains custom rules and instructions for the Antigravity agent specific to this workspace.
You can add your project-scoped rules below.

---

## 🧭 Giao thức tự động tra cứu (Auto-Discovery Protocol)

> **BẮT BUỘC — chạy TRƯỚC khi bắt đầu bất kỳ task nào.**
>
> 1. Đọc yêu cầu của user, rút ra **từ khóa** (VHAL, AVAS, HVAC, build system, energy, theme, launcher…).
> 2. Quét **Bảng định tuyến Docs** và **Bảng Agents / Rules** bên dưới theo từ khóa đó.
> 3. **Đọc** (Read) mọi doc / rule / agent playbook khớp **trước khi viết hoặc sửa code**. Không đoán khi đã có tài liệu.
> 4. Nếu không có dòng nào khớp → tra `docs/README.md` (mục lục đầy đủ) và `docs/project-structure.md`.
> 5. Sau khi code xong → tự review theo `code-reviewer` (xem Bảng Agents).

### Bảng định tuyến Docs (từ khóa yêu cầu → tài liệu phải đọc)

| Từ khóa trong yêu cầu | Đọc tài liệu |
|-----------------------|--------------|
| VHAL, vehicle property, đọc/ghi trạng thái xe, CarProperty, property id | [../docs/vhal-architecture.md](../docs/vhal-architecture.md) |
| cửa, kính, door, window, DOOR_POS, WINDOW_POS, hé kính khi mở cửa, auto window | [../docs/auto-window-door.md](../docs/auto-window-door.md) |
| AVAS, mute, âm thanh cảnh báo xe, `android.uid.system` cho AVAS | [../docs/avas-mute-logic.md](../docs/avas-mute-logic.md) |
| build system APK, ký APK, systemRelease, quyền hệ thống, cài đặt system app | [../docs/system-install.md](../docs/system-install.md) |
| cấu trúc dự án, đặt file ở đâu, package layout, Clean Architecture | [../docs/project-structure.md](../docs/project-structure.md) |
| Car service, Car API, `VehicleProperty`, eCarX HAL | [../docs/android-car-apk.md](../docs/android-car-apk.md) |
| HVAC, climate, điều hòa, HvacService AIDL | [../docs/flyme-hvac-apk.md](../docs/flyme-hvac-apk.md) |
| energy, charge/discharge, năng lượng, regeneration, trip, super endurance | [../docs/flyme-energy-apk.md](../docs/flyme-energy-apk.md) |
| settings, Flyme settings, VHAL id trong settings | [../docs/flyme-settings-apk.md](../docs/flyme-settings-apk.md) |
| FlymeAutoService, CoreService, driving restriction, SDK | [../docs/flyme-auto-service-apk.md](../docs/flyme-auto-service-apk.md) |
| theme, wallpaper, hình nền, ThemeManager, Customize | [../docs/flyme-customize-apk.md](../docs/flyme-customize-apk.md) · [../docs/flyme-wallpaperlauncher-apk.md](../docs/flyme-wallpaperlauncher-apk.md) |
| launcher, HOME, applist, recent apps, Aicy widget | [../docs/flyme-launcher-apk.md](../docs/flyme-launcher-apk.md) |
| scene mode, Rest/Camping, SceneDirector, SceneProvider | [../docs/flyme-scenedirector-apk.md](../docs/flyme-scenedirector-apk.md) |
| parking, AVM, camera 360, ArcSoft | [../docs/ecarx-parking-apk.md](../docs/ecarx-parking-apk.md) |
| thermal, nhiệt độ, sysfs, warning/shutdown | [../docs/mediatek-thermalmanager-apk.md](../docs/mediatek-thermalmanager-apk.md) |
| Android Auto, CarPlay, ConnAdaptor, ca_fix | [../docs/ca-fix-apk.md](../docs/ca-fix-apk.md) · [../docs/centralexauto-apk.md](../docs/centralexauto-apk.md) · [../docs/carassistant-ets-apk.md](../docs/carassistant-ets-apk.md) |
| storage, dọn dẹp bộ nhớ, Deletion Helper | [../docs/android-storagemanager-apk.md](../docs/android-storagemanager-apk.md) |
| provisioning, device owner, work profile | [../docs/managedprovisioning-apk.md](../docs/managedprovisioning-apk.md) |
| shell, bugreport, dumpstate, UID shell | [../docs/android-shell-apk.md](../docs/android-shell-apk.md) |
| CarAssistant, ETS, trợ lý giọng nói, multi-display | [../docs/carassistant-ets-apk.md](../docs/carassistant-ets-apk.md) |
| phone companion, app điện thoại, TCP 47800, CarTcpServer, socket, NDJSON, NSD, mDNS, discovery, đồng bộ status/command, push/nhận data | [../docs/phone-companion-protocol.md](../docs/phone-companion-protocol.md) |
| bluetooth, BT, SPP, RFCOMM, transport song song, kết nối phone không cần WiFi | [../ex2-phone/docs/bluetooth-transport-plan.md](../ex2-phone/docs/bluetooth-transport-plan.md) — plan theo phase; §9 là phần việc phía xe |
| *(không khớp)* | [../docs/README.md](../docs/README.md) — mục lục đầy đủ toàn bộ docs |

### Bảng Agents / Skills (loại công việc → playbook phải đọc)

| Loại công việc | Đọc / Delegate |
|----------------|----------------|
| Viết / sửa feature Android, UI, Gradle, Bluetooth/OBD/ADB | [.agents/android-senior.md](./.agents/android-senior.md) |
| Review diff trước commit / PR | [.agents/code-reviewer.md](./.agents/code-reviewer.md) |
| Cài / deploy system APK qua ADB | [.agents/skills/adb-system-deploy/SKILL.md](./.agents/skills/adb-system-deploy/SKILL.md) |
| Build & ký system APK | [.agents/skills/build-system-apk/SKILL.md](./.agents/skills/build-system-apk/SKILL.md) |
| Đọc / khai báo VHAL property | [.agents/skills/vhal-property-reader/SKILL.md](./.agents/skills/vhal-property-reader/SKILL.md) |

### Quy ước bảo trì bảng định tuyến

- Thêm doc mới vào `docs/` → **thêm ngay một dòng** vào Bảng định tuyến Docs kèm từ khóa.
- Thêm agent / skill mới → thêm dòng vào Bảng Agents / Skills.
- Từ khóa để **tiếng gốc của tính năng** (VHAL, AVAS, HVAC…) để dễ khớp; không dịch tên riêng.

---

## Rules
- **Auto-discovery**: Trước khi code, **luôn** chạy Giao thức tự động tra cứu ở trên. Không bịa convention khi đã có doc/agent mô tả.
- **Kiến trúc UI**: Sử dụng 100% Jetpack Compose và Material 3 cho giao diện. Tuyệt đối không dùng XML layout.
- **Xử lý bất đồng bộ**: Ưu tiên sử dụng Kotlin Coroutines và `StateFlow` / `SharedFlow` cho mọi tương tác dữ liệu liên tục (đặc biệt khi theo dõi trạng thái xe từ VHAL).
- **Quản lý VHAL**: Mọi thay đổi liên quan đến việc đọc/ghi thuộc tính của xe (Vehicle Properties) phải được thực hiện thông qua `VhalVehicleEventHub.kt` và khai báo ID ở `VhalConstants.kt`. → xem [../docs/vhal-architecture.md](../docs/vhal-architecture.md).
- **Hệ thống Build**: Cần lưu ý ứng dụng có 2 flavors: `user` (debug thông thường) và `system` (cần quyền `android.uid.system` để điều khiển AVAS). Code không được làm hỏng tiến trình build `systemRelease`. → xem [../docs/system-install.md](../docs/system-install.md).
- **Quyền hệ thống**: Không tự ý xóa bỏ các thuộc tính cấp hệ thống trong `AndroidManifest.xml` (như `sharedUserId="android.uid.system"`).
- **Phone companion (đồng bộ với app điện thoại)**: Business logic ở `CarTcpServer.kt`, transport tách ra 2 link chạy **song song** — `TcpServerLink.kt` (**TCP raw 47800** + NSD) và `BtSppServerLink.kt` (**Bluetooth RFCOMM/SPP**, UUID `6f1e2a00-47b8-4c1e-9d3a-c0ffee000001`). Cả hai dùng chung **NDJSON (1 JSON/dòng + `\n`), event-driven** (KHÔNG phải Ktor/WebSocket/HTTP; đã gỡ Ktor). Bất biến bắt buộc giữ:
  - **DTO `CarStatusMessage` / `CarCommandMessage` / `CarCommandResponse` phải khớp 1:1 với app phone** (`ex2-phone`). Đổi/thêm/xóa field = breaking change → phải cập nhật cả 2 phía + doc.
  - Thêm dữ liệu xe mới: thêm field vào `CarStatusMessage`, đọc trong `readCarStatus()`, và nếu ghi được thì thêm `case` trong `handleCommand()`. Ưu tiên **on-change callback qua `CarVhalBindings`** (như `registerEventCallbacks()`), không poll.
  - **Không viết code riêng cho từng transport.** Mọi client là một `CarLineChannel` trong `clients` — thêm transport mới = thêm một `CarServerLink`, không đụng `readCarStatus()` / `handleCommand()` / `broadcast()`.
  - Discovery: WiFi = **NSD `_carsync._tcp` port 47800** (trong `TcpServerLink.registerNsd()`); Bluetooth = SDP theo UUID (trong `BtSppServerLink`).
  - `set_wifi` = WiFi **client/station** (không phải hotspot); chỉ đổi thật trên build `system`.
  - → xem [../docs/phone-companion-protocol.md](../docs/phone-companion-protocol.md) (spec đầy đủ + reference client).
