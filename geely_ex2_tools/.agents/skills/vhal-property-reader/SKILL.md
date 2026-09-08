---
name: vhal-property-reader
description: Hướng dẫn cách thêm mới một trình đọc dữ liệu VHAL từ hệ thống xe.
---

# VHAL Property Reader

Khi cần đọc thêm một thuộc tính (Vehicle Property) từ xe:
1. **Định nghĩa ID**: Thêm thuộc tính vào `com.geely.ex2.tools.data.vhal.VhalConstants`.
2. **Tạo lớp Reader**: Khởi tạo một lớp `CarProperty[Name]Reader` kế thừa giao diện đọc (hoặc lấy pattern từ `CarPropertyEnergyRegenerationReader.kt`).
3. **Luồng dữ liệu**: Đăng ký callback qua `CarPropertyManager`, lắng nghe thay đổi và phát (emit) dữ liệu ra `StateFlow` hoặc `SharedFlow`.
4. **Tích hợp**: Khai báo và sử dụng trong `VhalVehicleEventHub.kt` để quản lý tập trung và dễ dàng kết nối tới ViewModel/UI.
