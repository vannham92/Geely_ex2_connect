# Kiến trúc tương tác VHAL

Tài liệu này giải thích cách ứng dụng giao tiếp với Vehicle Hardware Abstraction Layer (VHAL) của xe.

## 1. Thành phần chính
- **`VhalConstants.kt`**: Nơi lưu trữ tất cả các mã ID thuộc tính xe (Property IDs).
- **Trình đọc (Readers)**: Các file như `CarPropertyEnergyRegenerationReader.kt` chịu trách nhiệm đăng ký trực tiếp với hệ thống xe và nhận sự kiện thay đổi qua `CarPropertyManager.CarPropertyEventCallback`.
- **`VhalVehicleEventHub.kt`**: Trung tâm xử lý (Hub) tổng hợp mọi sự kiện, quản lý vòng đời và làm cầu nối cung cấp Flow cho tầng trên.

## 2. Luồng dữ liệu (Data Flow)
1. Xe thay đổi trạng thái (VD: Đổi chế độ tái tạo năng lượng).
2. Android `CarPropertyManager` gọi callback về ứng dụng.
3. `Reader` xử lý giá trị thô, map sang kiểu dữ liệu (Enums/Data classes).
4. `Reader` emit giá trị vào `StateFlow`.
5. `ViewModel` nhận giá trị từ `Flow` và cập nhật giao diện (Jetpack Compose) dạng State.

## 3. Quản lý kết nối
Cần đảm bảo việc kết nối tới `Car API` được giải phóng khi ứng dụng bị tắt để tránh rò rỉ bộ nhớ hoặc gọi callback khi app không còn trên foreground.
