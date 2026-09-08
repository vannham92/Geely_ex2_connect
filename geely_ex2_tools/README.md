# Geely EX2 Tools

Ứng dụng Android viết bằng Kotlin với Jetpack Compose.

## Yêu cầu

- Android Studio Ladybug (2024.2+) hoặc mới hơn
- JDK 17–21 (Android Studio sử dụng JBR tích hợp sẵn)
- Android SDK 35

> **Lưu ý:** JDK 25 trên hệ thống không được hỗ trợ bởi Kotlin Gradle Plugin. `gradlew.bat` sẽ tự động lấy JBR từ Android Studio trên Windows.

## Build

```bash
./gradlew assembleDebug
```

Cài đặt lên thiết bị đã kết nối:

```bash
./gradlew installDebug
```

## Phát hành (system APK)

Bản build System có chữ ký nền tảng (platform-signature) (dùng để cài đặt lên thiết bị):

```powershell
.\scripts\release-system-apk.ps1
```

Mỗi lần chạy:

1. Tăng số bản build (`VERSION_CODE += 1` trong `app\version.properties`)
2. Build bản `systemRelease`
3. Lưu file APK vào `install\out\`

| Lệnh | Hành động |
|---------|----------|
| `.\scripts\release-system-apk.ps1` | tăng bản build + phát hành |
| `.\scripts\release-system-apk.ps1 -NoBump` | phát hành không tăng bản build |
| `.\scripts\release-system-apk.ps1 -VersionName 0.0.4` | phiên bản mới + tăng bản build |

Phiên bản và bản build được lưu trong `app\version.properties`.

Chi tiết: [docs/system-install.md](docs/system-install.md)

## Cấu trúc dự án

```text
app/
└── src/main/
    ├── java/com/geely/ex2/tools/
    │   ├── GeelyEx2ToolsApp.kt    # Application
    │   ├── MainActivity.kt        # Điểm bắt đầu (Entry point), Compose UI
    │   └── ui/theme/              # Material 3 theme
    ├── res/                       # Resource
    └── AndroidManifest.xml
```

## Thông số

| Tham số    | Giá trị              |
|-------------|-----------------------|
| Package     | `com.geely.ex2.tools` |
| minSdk      | 26                    |
| targetSdk   | 35                    |
| UI          | Jetpack Compose       |
