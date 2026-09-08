---
name: build-system-apk
description: Hướng dẫn cách tạo bản build hệ thống (System APK) sử dụng script release-system-apk.ps1
---

# Build System APK

Khi người dùng yêu cầu build bản system release, bạn phải thực hiện thông qua terminal:

1. Có thể sử dụng script PowerShell được cung cấp: `.\scripts\release-system-apk.ps1` (nếu dùng Windows) hoặc dùng script bash tương ứng (nếu có).
2. Lệnh này sẽ tự động:
   - Tăng `VERSION_CODE` trong `app/version.properties`
   - Build biến thể (flavor) `systemRelease`
   - Ký bằng AOSP platform testkey
   - Đặt file thành phẩm vào `install/out/geely-ex2-tools-system-platform-signed.apk`
3. Các tham số hỗ trợ:
   - `-NoBump`: Không tăng version code.
   - `-VersionName <version>`: Cập nhật version name.
