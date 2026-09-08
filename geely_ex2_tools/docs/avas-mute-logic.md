# AVAS Mute Logic

AVAS (Acoustic Vehicle Alerting System) là hệ thống cảnh báo âm thanh của xe điện. Ứng dụng này có tính năng cho phép can thiệp để làm câm (mute) AVAS, dựa trên `AvasMuteApplier.kt`.

## 1. Yêu cầu quyền
Thay đổi âm thanh AVAS (`CarAudioManager.setAVASMode` hoặc các thuộc tính tương đương) yêu cầu quyền hệ thống: `android.permission.CAR_CONTROL_AUDIO_VOLUME`.
Vì đây là quyền hệ thống/đặc quyền, ứng dụng không thể cấp thông qua màn hình xin quyền thông thường.

## 2. Giải pháp
Để quyền này hoạt động, ứng dụng phải:
- Khai báo `android:sharedUserId="android.uid.system"` trong `AndroidManifest.xml`.
- Được ký (sign) bằng **platform testkey** tương tự như firmware của xe. (Dùng biến thể build `systemRelease` và script).

## 3. Hoạt động
Tính năng Mute sẽ can thiệp vào cấu hình âm thanh của xe, ghi giá trị trạng thái mới xuống VHAL hoặc CarAudioManager. Trạng thái này có thể bị reset khi khởi động lại xe, tuỳ thuộc vào firmware của xe Geely.
