# Cấu trúc dự án

Dự án tuân thủ theo nguyên lý Clean Architecture kết hợp với quy ước của Android Jetpack Compose.

## Sơ đồ thư mục (app/src/main/java/com/geely/ex2/tools)

- **`/data`**: Tầng xử lý dữ liệu.
  - `/vhal`: Các lớp xử lý giao tiếp phần cứng xe (Car Properties, Hub).
  - `/driving`: Dữ liệu và các giá trị enum liên quan đến trạng thái lái.
  - `/settings`: Lưu trữ và đọc cấu hình (Preferences, Locale settings).
  - `/avas`: Xử lý logic hệ thống AVAS.
- **`/ui`**: Tầng giao diện.
  - `/theme`: Material 3 themes, Typography, Colors.
  - Các thư mục màn hình (Screens), chứa các Compose UI functions.
- **`/di`** (nếu có): Dependency Injection.
- **`MainActivity.kt`**: Entry point duy nhất của ứng dụng.
