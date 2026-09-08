# Auto window — cửa mở hé kính, cửa đóng đóng kính

Tính năng: **mở cửa nào → hạ kính cửa đó 20%; đóng cửa lại → đóng kính nếu kính đang hé ≤ 30%.**
Có công tắc bật/tắt **riêng từng cửa** (mặc định **tắt**) ở màn hình *Cửa & kính* (`AppRoutes.AUTO_WINDOW`).

## 1. Property VHAL

Số liệu lấy từ `adb shell dumpsys car_service` trên IHU629G (EX2 thật):

| Ý nghĩa | Id AOSP | Id vendor | Area | Giá trị |
|---------|---------|-----------|------|---------|
| Trạng thái cửa (DOOR_POS) | `0x16400B00` | `0x264020A9` | VehicleAreaDoor `0x1/0x4/0x10/0x40` (+ cốp `0x20000000`) | int 0=đóng, 1=mở |
| Vị trí kính (WINDOW_POS) | `0x13400BC0` | `0x234020AB` | VehicleAreaWindow `0x10/0x40/0x100/0x400` (+ roof `0x10000/0x20000`) | int 0..100 (%) |
| Khóa cửa (DOOR_LOCK) | `0x16200B02` | `0x264020AA` | VehicleAreaDoor | bool |

Lưu ý:

- **Bit area của cửa và kính KHÁC nhau** (cửa sau trái `0x10`, kính sau trái `0x100`) → luôn ghép cặp
  qua [`DoorCorner`](../app/src/main/java/com/geely/ex2/tools/data/vhal/DoorCorner.kt).
- Id AOSP đọc được qua adapt layer của Flyme dù `dumpsys` chỉ liệt kê id vendor
  (đã xác nhận: `WINDOW_POS 0x13400BC0` trả `min=0 max=100`). `CarPropertyDoorWindowController`
  dò DOOR_POS theo thứ tự **AOSP → vendor** rồi cache id đọc được.
- WINDOW_POS trên xe này đã là phần trăm (`min=0, max=100`), nhưng code vẫn quy đổi qua
  `CarPropertyConfig` min/max để không phụ thuộc firmware.
- Ghi kính cần `android.car.permission.CONTROL_CAR_WINDOWS` → **chỉ hoạt động trên build `system`**;
  build `user` chỉ đọc được trạng thái.

## 2. Luồng xử lý

```text
DOOR_POS on-change (per-area, binder thread)
  └─ AutoWindowService.handleDoorEvent → CarPropertyIo
       └─ AutoWindowController.onDoorState(corner, isOpen)
            ├─ cửa đó chưa bật → bỏ qua
            ├─ chưa có baseline / trạng thái không đổi → bỏ qua
            ├─ PERF_VEHICLE_SPEED > 5 km/h → bỏ qua (xe đang chạy)
            ├─ cửa MỞ  : kính < 20%  → WINDOW_POS = 20
            └─ cửa ĐÓNG: kính 1..30% → WINDOW_POS = 0
```

- Chỉ nghe **DOOR_POS**, không nghe WINDOW_POS → lệnh ghi của chính app không tạo vòng lặp.
- Cửa mở mà kính đã mở ≥ 20% → giữ nguyên (không bao giờ đóng bớt kính đang mở).
- Cửa đóng mà kính mở > 30% → coi là người dùng chủ động hạ kính, không can thiệp.
- Callback bị VHAL từ chối → service tự chuyển sang poll 4 cửa mỗi
  `AUTO_WINDOW_DOOR_POLL_INTERVAL_MS` (2 s).

## 3. File liên quan

| File | Vai trò |
|------|---------|
| `data/vhal/DoorCorner.kt` | Ghép areaId cửa ↔ kính cùng vị trí |
| `data/vhal/CarPropertyDoorWindowController.kt` | Đọc DOOR_POS / đọc–ghi WINDOW_POS theo góc xe |
| `data/vhal/CarVhalBindings.registerAreaIntPropertyCallback` | Callback on-change **kèm areaId** |
| `data/window/AutoWindowController.kt` | Toàn bộ logic hé/đóng kính + gate tốc độ |
| `data/window/AutoWindowService.kt` | Foreground service `:core`, theo dõi DOOR_POS (START_STICKY) |
| `data/window/AutoWindowSettings.kt` | Công tắc bật/tắt riêng từng cửa (MMKV, mặc định false) |
| `data/window/AutoWindowAppStarter.kt`, `AutoWindowEventReceiver.kt` | Khởi động sau boot / MY_PACKAGE_REPLACED |
| `feature/window/ui/AutoWindowScreen.kt` | 4 toggle riêng từng cửa + trạng thái cửa/kính |

Ngưỡng nằm trong `VhalConstants`: `AUTO_WINDOW_DROP_PERCENT`, `AUTO_WINDOW_CLOSE_MAX_PERCENT`,
`AUTO_WINDOW_MAX_SPEED_KMH`.

## 4. Kiểm thử trên xe

```bash
adb logcat -s GeelyToolsAutoWindow
```

Bật công tắc cửa trước trái trong app → mở cửa lái → log phải có `Kính FL → 20%`; đóng cửa → `Kính FL → 0%`.
Nếu thấy `DOOR_POS không đọc được` thì xe/bản build không cấp quyền đọc trạng thái cửa.
