# Cài đặt AVAS / system

Bản `userDebug` / `adb install` thông thường **không thể** vô hiệu hóa AVAS: `CarAudioManager.setAVASMode` yêu cầu quyền `CAR_CONTROL_AUDIO_VOLUME`, và trong CentralEXAuto điều này chỉ hoạt động vì APK:

- Có `android:sharedUserId="android.uid.system"`
- Được ký bằng **AOSP platform testkey** (`android@android.com`)

## Build system APK (giống CentralEXAuto)

```powershell
.\scripts\release-system-apk.ps1
```

Mỗi lần chạy: `VERSION_CODE += 1` trong `app\version.properties`, sau đó build và ký platform-signature.

Kết quả:
- `install\out\geely-ex2-tools-v{VERSION_NAME}({VERSION_CODE}).apk` — ví dụ `geely-ex2-tools-v1.0(2).apk`
- `install\out\geely-ex2-tools-system-platform-signed.apk` — bản sao với tên cố định

Không tăng bản build: `.\scripts\release-system-apk.ps1 -NoBump`  
Đổi marketing-version: `.\scripts\release-system-apk.ps1 -VersionName 1.0`

## Cài đặt lên thiết bị (GU)

Đầu tiên hãy gỡ bản cài đặt user thông thường (nếu không sẽ bị xung đột UID/chữ ký):

```text
adb uninstall com.geely.ex2.tools
```

Trên thiết bị này, an toàn nhất là dùng lệnh push + `pm install` (lệnh `adb install install\out\...` tương đối trên Windows thường báo lỗi `filename doesn't end .apk`):

```text
adb push install/out/geely-ex2-tools-system-platform-signed.apk /data/local/tmp/geely-ex2-tools-system.apk
adb shell pm install -r -g /data/local/tmp/geely-ex2-tools-system.apk
```

Hoặc dùng đường dẫn tuyệt đối:

```text
adb install -r "C:\Users\hitma\AndroidStudioProjects\geely_ex2_tools\install\out\geely-ex2-tools-system-platform-signed.apk"
```

Khi cài đặt vào `/system/priv-app/`, cần đặt thêm file:

`install/privapp-permissions-com.geely.ex2.tools.xml` → `/system/etc/permissions/`

## Flavors (Phiên bản build)

| Flavor | sharedUserId | Chữ ký (Signature) | Mute AVAS |
|--------|--------------|---------|-----------|
| `user` (mặc định) | không | debug | thường là không |
| `system` | `android.uid.system` | platform testkey (script) | có, giống CentralEXAuto |
