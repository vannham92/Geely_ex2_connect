---
name: adb-system-deploy
description: Hướng dẫn cách cài đặt ứng dụng có quyền hệ thống qua ADB.
---

# ADB System Deploy

Ứng dụng Geely EX2 Tools (bản System) yêu cầu UID hệ thống và chữ ký hệ thống. Để cài đặt qua ADB, cần làm theo quy trình:

1. **Gỡ bản cũ (Nếu là bản user)**: `adb uninstall com.geely.ex2.tools` (Tránh xung đột UID)
2. **Copy file APK vào TMP**: Do lệnh install trực tiếp thường gặp lỗi `filename doesn't end .apk` trên Windows -> `adb push install/out/geely-ex2-tools-system-platform-signed.apk /data/local/tmp/app.apk`
3. **Cài đặt qua PM**: `adb shell pm install -r -g /data/local/tmp/app.apk`
4. **Lưu ý đặc quyền**: Bản cài này phải đi kèm file `privapp-permissions` ở `/system/etc/permissions/` nếu được đẩy vào `/system/priv-app/`.
