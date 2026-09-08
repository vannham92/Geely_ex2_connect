package com.geely.ex2.tools.data.vhal

object VhalConstants {
    const val GLOBAL_AREA_ID = 0

    /** android.car.VehiclePropertyIds.PERF_VEHICLE_SPEED */
    const val PROP_PERF_VEHICLE_SPEED = 0x11600207

    /** android.car.VehiclePropertyIds.GEAR_SELECTION */
    const val PROP_GEAR_SELECTION = 0x11400400

    /** android.car.VehiclePropertyIds.CURRENT_GEAR */
    const val PROP_CURRENT_GEAR = 0x11400401

    /** android.car.VehicleGear — giá trị của GEAR_SELECTION/CURRENT_GEAR. */
    const val GEAR_NEUTRAL = 1
    const val GEAR_REVERSE = 2
    const val GEAR_PARK = 4
    const val GEAR_DRIVE = 8

    /** android.car.VehiclePropertyIds.RANGE_REMAINING (float; AOSP = mét, một số OEM = km). */
    const val PROP_RANGE_REMAINING = 0x11600308

    /** android.car.VehiclePropertyIds.PERF_ODOMETER (float, km). */
    const val PROP_PERF_ODOMETER = 0x11600204

    /** android.car.VehiclePropertyIds.TIRE_PRESSURE (float, kPa; area = VehicleAreaWheel). */
    const val PROP_TIRE_PRESSURE = 0x17600309
    /** android.car.VehiclePropertyIds.CRITICALLY_LOW_TIRE_PRESSURE (float, kPa; ngưỡng cảnh báo/bánh). */
    const val PROP_CRITICALLY_LOW_TIRE_PRESSURE = 0x1760030A

    /** android.car.VehicleAreaWheel — areaId cho TIRE_PRESSURE. */
    const val WHEEL_LEFT_FRONT = 0x01
    const val WHEEL_RIGHT_FRONT = 0x02
    const val WHEEL_LEFT_REAR = 0x04
    const val WHEEL_RIGHT_REAR = 0x08

    // ------------------------------------------------------------------ Door lock / Window
    // AOSP VehiclePropertyIds (adapt layer của SceneDirector dùng cùng id: ASSIST_WINDOW_POS=322964416,
    // ASSIST_CAR_DOOR_STATE=373295872 — xem docs/flyme-scenedirector-apk.md §mapping).
    // Ghi cần quyền CONTROL_CAR_DOORS / CONTROL_CAR_WINDOWS → chỉ build `system`.

    /** android.car.VehiclePropertyIds.DOOR_LOCK (bool, area=VehicleAreaDoor) — true=khóa. */
    const val PROP_DOOR_LOCK = 0x16200B02          // 371198722
    /** android.car.VehiclePropertyIds.WINDOW_POS (int, area=VehicleAreaWindow) — 0=đóng, max=mở hết. */
    const val PROP_WINDOW_POS = 0x13400BC0          // 322964416
    /** android.car.VehiclePropertyIds.DOOR_POS (int, area=VehicleAreaDoor) — 0=đóng, >0=cửa mở. */
    const val PROP_DOOR_POS = 0x16400B00            // 373295872 (Flyme adapt: ASSIST_CAR_DOOR_STATE)

    /**
     * Prop vendor trên IHU629G (`dumpsys car_service`): id AOSP đi qua adapt layer nên có thể đọc
     * được, nhưng không có trong danh sách config → dùng làm fallback khi id AOSP trả ERROR.
     * DOOR_POS vendor: int 0..1 (0=đóng, 1=mở). WINDOW_POS vendor: int 0..100 (%).
     */
    const val PROP_VENDOR_DOOR_POS = 0x264020A9     // 643866793
    const val PROP_VENDOR_WINDOW_POS = 0x234020AB   // 590413995

    /** android.car.VehicleAreaDoor — areaId cho DOOR_LOCK. */
    const val DOOR_ROW_1_LEFT = 0x00000001
    const val DOOR_ROW_1_RIGHT = 0x00000004
    const val DOOR_ROW_2_LEFT = 0x00000010
    const val DOOR_ROW_2_RIGHT = 0x00000040

    /** android.car.VehicleAreaWindow — areaId cho WINDOW_POS (khác bit với Door!). */
    const val WINDOW_ROW_1_LEFT = 0x00000010
    const val WINDOW_ROW_1_RIGHT = 0x00000040
    const val WINDOW_ROW_2_LEFT = 0x00000100
    const val WINDOW_ROW_2_RIGHT = 0x00000400

    // ------------------------------------------------------------------ Auto window (cửa mở → hé kính)

    /** % hé kính khi cửa tương ứng được mở. */
    const val AUTO_WINDOW_DROP_PERCENT = 20
    /** Cửa đóng lại: chỉ tự đóng kính khi kính đang hé ở mức <= ngưỡng này. */
    const val AUTO_WINDOW_CLOSE_MAX_PERCENT = 30
    /** Chặn thao tác kính khi xe đang chạy (đọc được PERF_VEHICLE_SPEED và vượt ngưỡng). */
    const val AUTO_WINDOW_MAX_SPEED_KMH = 5f
    /** Poll DOOR_POS khi callback on-change bị VHAL từ chối. */
    const val AUTO_WINDOW_DOOR_POLL_INTERVAL_MS = 2_000L
    /** UI màn hình Cửa & Kính */
    const val AUTO_WINDOW_UI_POLL_INTERVAL_MS = 3_000L

    // ------------------------------------------------------------------ HVAC (Climate)
    // Property id + areaId lấy từ docs/flyme-hvac-apk.md §2.2/§5/§6 (Flyme IHU629G, build 26012721).
    // Ghi cần quyền android.car.permission.CONTROL_CAR_CLIMATE + CAR_VENDOR_EXTENSION → chỉ build `system`.

    /** HVAC_AC_ON (bool) — bật/tắt máy nén A/C. */
    const val PROP_HVAC_AC_ON = 354419973
    /** HVAC_POWER_ON (bool) — cầu dao tổng climate (chưa expose, để tham chiếu). */
    const val PROP_HVAC_POWER_ON = 354419984
    /** HVAC_TEMPERATURE_SET (float °C) — nhiệt độ đặt theo vùng. */
    const val PROP_HVAC_TEMPERATURE_SET = 358614275
    /** HVAC_TEMPERATURE_CURRENT (float °C) — nhiệt độ hiện tại (read-only). */
    const val PROP_HVAC_TEMPERATURE_CURRENT = 358614274
    /** HVAC_FAN_SPEED (int) — mức quạt gió. */
    const val PROP_HVAC_FAN_SPEED = 356517120
    /** HVAC_RECIRC_ON (bool) — tuần hoàn gió trong/ngoài. */
    const val PROP_HVAC_RECIRC_ON = 354419976
    /** HVAC_MAX_DEFROST_ON (bool) — sấy tan sương kính trước max. */
    const val PROP_HVAC_MAX_DEFROST_ON = 354419985
    /** HVAC_FUNC_ECO_SWITCH (bool; adapt-only funType=2) — chế độ ECO. */
    const val PROP_HVAC_FUNC_ECO_SWITCH = 268960000

    /** HVAC areaId (com.flyme.auto.hvac.car_api.Zone). */
    const val HVAC_AREA_DRIVER = 1        // SEAT_ROW_1_LEFT (nhiệt độ ghế lái, max defrost)
    const val HVAC_AREA_PASSENGER = 4     // SEAT_ROW_1_RIGHT
    const val HVAC_AREA_ROW2_LEFT = 16    // ZONE_ROW_2_LEFT (nhiệt độ hàng sau)
    const val HVAC_AREA_ROW1_ALL = 5      // ZONE_ROW_1_ALL (power tổng)
    const val HVAC_AREA_ALL = 117         // ZONE_ALL (AC / recirc / ECO)
    const val HVAC_AREA_FAN = 0           // HVAC_FAN_SPEED — doc để trống, thử GLOBAL 0 (verify trên xe)

    /** Geely OEM SOC % (Flyme) — ưu tiên trên IHU629G */
    const val PROP_ED_EV_BATTERY_PERCENTAGE = 0x2140a6ed

    /** android.car.VehiclePropertyIds.EV_BATTERY_LEVEL */
    const val PROP_EV_BATTERY_LEVEL = 0x11600309

    /** android.car.VehiclePropertyIds.EV_CURRENT_BATTERY_CAPACITY */
    const val PROP_EV_CURRENT_BATTERY_CAPACITY = 0x1160030d

    /** android.car.VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY */
    const val PROP_INFO_EV_BATTERY_CAPACITY = 0x11600106

    /** Flyme OEM: BCM_FUNC_LIGHT_ATMOSPHERE_LAMPS */
    const val PROP_BCM_FUNC_LIGHT_ATMOSPHERE_LAMPS = 0x21051000

    /** android.car LIGHTINSIDE_ATMOSPHERE_LAMP_SWITCH */
    const val PROP_LIGHTINSIDE_ATMOSPHERE_LAMP_SWITCH = 557885013

    /** Flyme OEM: DM_FUNC_DRIVE_MODE_SELECT */
    const val PROP_DM_FUNC_DRIVE_MODE_SELECT = 0x22010100

    /** Flyme OEM drive mode values (VALUE_DRIVE_MODE_SELECTION_*) */
    const val DRIVE_MODE_ECO = 0x22010101
    const val DRIVE_MODE_COMFORT = 0x22010102
    const val DRIVE_MODE_DYNAMIC = 0x22010103
    const val DRIVE_MODE_XC = 0x22010104
    const val DRIVE_MODE_PURE = 0x22010106
    const val DRIVE_MODE_HYBRID = 0x22010107
    const val DRIVE_MODE_SNOW = 0x22010109
    const val DRIVE_MODE_ADAPTIVE = 0x22010116

    /**
     * Flyme OEM: SETTING_FUNC_ENERGY_REGENERATION.
     * AutoFuncId / CentralEXAuto: 537003264 = 0x20020500 (не 0x22020500).
     */
    const val PROP_SETTING_FUNC_ENERGY_REGENERATION = 0x20020500

    /** Flyme OEM regen levels (VALUE_ENERGY_REGENERATION_LEVEL_*) */
    const val ENERGY_REGENERATION_LEVEL_LOW = 0x20020501
    const val ENERGY_REGENERATION_LEVEL_MID = 0x20020502
    const val ENERGY_REGENERATION_LEVEL_HIGH = 0x20020503

    /** Areas для VHAL regen (CentralEXAuto REGEN_AREAS / REGEN_PROP_AREAS). */
    val ENERGY_REGENERATION_AREAS: IntArray = intArrayOf(0, 1)

    /** ICarFunction.COMMON_VALUE_ON / COMMON_VALUE_OFF */
    const val COMMON_VALUE_ON = 1
    const val COMMON_VALUE_OFF = 0

    const val CAR_MANAGER_PROPERTY = "property"

    /** UI màn hình Wi‑Fi — làm mới nhẹ trạng thái (icon được cập nhật bởi WifiStatusService) */
    const val WIFI_UI_POLL_INTERVAL_MS = 5_000L

    /** UI màn hình Driving — poll ít hơn, ECU không kịp trả lời trong 1 giây sau khi write */
    const val DRIVING_UI_POLL_INTERVAL_MS = 6_000L

    /** UI màn hình đèn viền nội thất (ambient light) */
    const val AMBIENT_LIGHT_UI_POLL_INTERVAL_MS = 3_000L

    /** Đánh thức màn hình — VHAL/Flyme API có thể khởi động chậm */
    const val AMBIENT_LIGHT_WAKE_VERIFY_BASE_MS = 1_000L
    const val AMBIENT_LIGHT_WAKE_VERIFY_STEP_MS = 1_500L
    const val AMBIENT_LIGHT_WAKE_APPLY_ATTEMPTS = 6

    /** Tạm dừng trước khi read-back sau khi write (ECU áp dụng chế độ có độ trễ) */
    const val DRIVING_RESTORE_VERIFY_BASE_MS = 800L
    const val DRIVING_RESTORE_VERIFY_STEP_MS = 800L

    /** Đồng bộ ngầm các chế độ lái/tái tạo năng lượng đã lưu với xe */
    const val DRIVING_PERSIST_SYNC_INTERVAL_MS = 60_000L

    /** Debounce thay đổi GEAR_SELECTION (một lần sang số = một lần restore). */
    const val GEAR_EVENT_DEBOUNCE_MS = 500L

    /** Chu kỳ truy vấn widget trên thanh trạng thái (pin, nhiệt độ) */
    const val STATUS_WIDGET_POLL_INTERVAL_MS = 100_000L

    const val BATTERY_POLL_INTERVAL_MS = STATUS_WIDGET_POLL_INTERVAL_MS
    const val TEMPERATURE_POLL_INTERVAL_MS = STATUS_WIDGET_POLL_INTERVAL_MS

    /** Đồng hồ tốc độ trên thanh trạng thái Flyme — cập nhật mỗi 3 giây */
    const val SPEED_POLL_INTERVAL_MS = 3_000L

    /** VHAL callback rate for PERF_VEHICLE_SPEED (~раз в 3 с). */
    const val SPEED_CALLBACK_RATE_HZ = 1f / 3f

    /** CarPropertyManager.SENSOR_RATE_ONCHANGE — sự kiện chỉ xảy ra khi đổi giá trị. */
    const val CALLBACK_RATE_ONCHANGE_HZ = 0f
}
