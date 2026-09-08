# Tài liệu Geely EX2 Tools

| Tài liệu | Mô tả |
|----------|----------|
| [android-car-apk.md](./android-car-apk.md) | Phân tích APK `com.android.car` (Car service): Vehicle HAL, Car API, `VehicleProperty`, eCarX |
| [flyme-settings-apk.md](./flyme-settings-apk.md) | Phân tích APK `com.flyme.auto.settings`: Flyme API, VHAL id, các class và pattern từ dex |
| [flyme-hvac-apk.md](./flyme-hvac-apk.md) | Phân tích APK `com.flyme.auto.hvac` (Climate): UI, HvacService AIDL, VHAL/Adapt property id |
| [flyme-auto-service-apk.md](./flyme-auto-service-apk.md) | Phân tích APK `com.flyme.auto` (FlymeAutoService): CoreService, driving restrictions, SDK |
| [flyme-customize-apk.md](./flyme-customize-apk.md) | Phân tích APK `com.flyme.auto.customize` (Themes): themes/hình nền, ThemeManager, WallpaperProvider, AI |
| [flyme-energy-apk.md](./flyme-energy-apk.md) | Phân tích APK `com.flyme.auto.energy` (Energy/Năng lượng): sạc/xả, trip, super endurance, AIDL |
| [ecarx-parking-apk.md](./ecarx-parking-apk.md) | Phân tích APK `com.ecarx.parking` (AVM): VHAL commands, EAS API, điểm truy cập (entry points), ArcSoft |
| [managedprovisioning-apk.md](./managedprovisioning-apk.md) | Phân tích APK `com.android.managedprovisioning`: work profile / device owner provisioning (AOSP) |
| [mediatek-thermalmanager-apk.md](./mediatek-thermalmanager-apk.md) | Phân tích APK `com.mediatek.thermalmanager`: thermal policy, sysfs, cảnh báo/tắt nguồn (warning/shutdown) |
| [flyme-scenedirector-apk.md](./flyme-scenedirector-apk.md) | Phân tích APK `com.flyme.auto.scenedirector` (Scene Mode): Rest/Camping, HAL, VR, SceneProvider |
| [android-shell-apk.md](./android-shell-apk.md) | Phân tích APK `com.android.shell` (Shell): UID shell, bugreport UX, dumpstate |
| [flyme-wallpaperlauncher-apk.md](./flyme-wallpaperlauncher-apk.md) | Phân tích APK `com.flyme.auto.wallpaperlauncher`: live wallpaper, Customize Provider, MIPC/Car3D |
| [flyme-launcher-apk.md](./flyme-launcher-apk.md) | Phân tích APK `com.flyme.auto.launcher` (Lib): HOME, Aicy Widget, applist, plugins, recent apps |
| [centralexauto-apk.md](./centralexauto-apk.md) | Phân tích APK `com.ex.auto` (CentralEXAuto): watchdog, AA/CarPlay, floaters, VHAL, GitHub updates |
| [ca-fix-apk.md](./ca-fix-apk.md) | Phân tích APK `com.njda.adapter` (`ca_fix.apk`): ConnAdaptor — Android Auto / CarPlay adapter |
| [android-storagemanager-apk.md](./android-storagemanager-apk.md) | Phân tích APK `com.android.storagemanager` (Storage Manager v9): Deletion Helper, ASM job, notifications |
| [carassistant-ets-apk.md](./carassistant-ets-apk.md) | Phân tích APK `com.baidu.che.codriver` (CarAssistant-ETS): multi-display thiết bị đầu cuối, DPI, model profiles, giọng nói, Flyme/ECARX |
| [auto-window-door.md](./auto-window-door.md) | Tính năng cửa mở → hé kính 20%, cửa đóng → đóng kính (≤30%): property DOOR_POS/WINDOW_POS, service, công tắc |
| [phone-companion-protocol.md](./phone-companion-protocol.md) | Giao thức TCP `:47800` (CarTcpServer) cho app điện thoại: NDJSON, push status + gửi command, reference client Kotlin, discovery, bảo mật |

## Các artifact cục bộ (Local artifacts)

| Đường dẫn | Nội dung |
|------|------------|
| `.tmp/android-car-apk/` | APK `com.android.car` đã giải nén |
| `.tmp/android-car-jadx/` | Car service được dịch ngược bởi JADX |
| `.tmp/android-car.apk` | APK gốc (tùy chọn) |
| `.tmp/android-shell.apk` | APK gốc (tùy chọn) |
| `.tmp/android-shell-apk/` | APK `com.android.shell` đã giải nén |
| `.tmp/android-shell-jadx/` | Shell APK được dịch ngược bởi JADX |
| `.tmp/flyme-settings-apk/` | APK `com.flyme.auto.settings` đã giải nén |
| `.tmp/flyme-settings.apk` | APK gốc (tùy chọn) |
| `.tmp/flyme-hvac-apk/` | APK `com.flyme.auto.hvac` đã giải nén |
| `.tmp/flyme-hvac-jadx/` | Climate APK được dịch ngược bởi JADX |
| `.tmp/flyme-hvac.apk` | APK gốc (tùy chọn) |
| `.tmp/flyme-auto-service-apk/` | APK `com.flyme.auto` đã giải nén |
| `.tmp/flyme-auto-service-jadx/` | FlymeAutoService được dịch ngược bởi JADX |
| `.tmp/flyme-auto-service.apk` | APK gốc (tùy chọn) |
| `.tmp/flyme-customize.apk` | APK gốc (tùy chọn) |
| `.tmp/flyme-customize-apk/` | APK `com.flyme.auto.customize` đã giải nén |
| `.tmp/flyme-customize-jadx/` | Themes / CustomizeCenter được dịch ngược bởi JADX |
| `.tmp/flyme-customize-dexdump.txt` | dexdump `classes.dex` |
| `.tmp/flyme-energy-apk/` | APK `com.flyme.auto.energy` đã giải nén |
| `.tmp/flyme-energy-dexdump.txt` | dexdump Energy APK |
| `.tmp/flyme-energy-strings.txt` | Chuỗi lọc từ dex (CHARGE/HYBRID/…) |
| `.tmp/flyme-energy.apk` | APK gốc (tùy chọn) |
| `.tmp/ecarx-parking-apk/` | APK `com.ecarx.parking` đã giải nén |
| `.tmp/ecarx-parking-src/` | AVM APK được dịch ngược bởi JADX |
| `.tmp/managedprovisioning.apk` | APK gốc (tùy chọn) |
| `.tmp/managedprovisioning-apk/` | APK `com.android.managedprovisioning` đã giải nén |
| `.tmp/mediatek-thermalmanager.apk` | APK gốc (tùy chọn) |
| `.tmp/mediatek-thermalmanager-apk/` | APK `com.mediatek.thermalmanager` đã giải nén |
| `.tmp/mediatek-thermalmanager-jadx/` | MTK Thermal Manager được dịch ngược bởi JADX |
| `.tmp/flyme-scenedirector.apk` | APK gốc (tùy chọn) |
| `.tmp/flyme-scenedirector-apk/` | APK `com.flyme.auto.scenedirector` đã giải nén |
| `.tmp/flyme-scenedirector-jadx/` | Scene Mode APK được dịch ngược bởi JADX |
| `.tmp/flyme-wallpaperlauncher.apk` | APK gốc (tùy chọn) |
| `.tmp/flyme-wallpaperlauncher-apk/` | APK `com.flyme.auto.wallpaperlauncher` đã giải nén |
| `.tmp/flyme-wallpaperlauncher-jadx/` | AutoWallpaperLauncher được dịch ngược bởi JADX |
| `.tmp/flyme-launcher.apk` | APK gốc (tùy chọn) |
| `.tmp/flyme-launcher-apk/` | APK `com.flyme.auto.launcher` đã giải nén |
| `.tmp/flyme-launcher-badging.txt` | `aapt dump badging` AutoLauncher |
| `.tmp/flyme-launcher-manifest.txt` | `aapt dump xmltree` manifest của launcher |
| `.tmp/flyme-launcher-analysis.txt` | Phân tích các class/package trong dex của launcher |
| `.tmp/centralexauto.apk` | APK gốc CentralEXAuto (tùy chọn) |
| `.tmp/centralexauto/` | APK đã giải nén + badging/manifest |
| `.tmp/centralexauto-jadx/` | CentralEXAuto được dịch ngược bởi JADX |
| `.tmp/ca_fix.apk` | APK gốc ConnAdaptor fix (tùy chọn) |
| `.tmp/ca-fix/` | APK đã giải nén + badging/manifest |
| `.tmp/ca-fix-jadx/` | ConnAdaptor / ca_fix được dịch ngược bởi JADX |
| `.tmp/storagemanager/storagemanager.apk` | APK gốc Storage Manager v9 |
| `.tmp/storagemanager/apk/` | APK `com.android.storagemanager` đã giải nén |
| `.tmp/storagemanager/jadx/` | Storage Manager được dịch ngược bởi JADX |
| `.tmp/storagemanager/badging.txt` | `aapt dump badging` |
| `.tmp/storagemanager/manifest.txt` | `aapt dump xmltree` manifest |
| `.tmp/carassistant-ets/CarAssistantETS.apk` | APK gốc CarAssistant-ETS (tùy chọn) |
| `.tmp/carassistant-ets/apk/` | APK `com.baidu.che.codriver` đã giải nén |
| `.tmp/carassistant-ets/badging.txt` | `aapt dump badging` |
| `.tmp/carassistant-ets/manifest.txt` | `aapt dump xmltree` manifest |
| `.tmp/carassistant-ets/resources-ids.txt` | Lọc dump resource IDs |
| `.tmp/carassistant-ets/configs-list.txt` | Danh sách `assets/configs` + kích thước |
| `.tmp/carassistant-ets/ex2-header.hex` | Tiêu đề của file encrypted `ex2.json` |

Thư mục `.tmp/` được khai báo trong `.gitignore`.
