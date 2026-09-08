package com.geely.ex2.tools.data.vhal

/**
 * Ghép cặp areaId của cửa (`VehicleAreaDoor`) với kính cùng vị trí (`VehicleAreaWindow`).
 * Hai enum bit KHÁC nhau — cửa sau trái = 0x10 nhưng kính sau trái = 0x100 — nên mọi logic
 * "cửa nào → kính nào" phải đi qua đây thay vì tự dịch bit.
 */
enum class DoorCorner(
    val doorAreaId: Int,
    val windowAreaId: Int,
    val tag: String,
) {
    FRONT_LEFT(VhalConstants.DOOR_ROW_1_LEFT, VhalConstants.WINDOW_ROW_1_LEFT, "FL"),
    FRONT_RIGHT(VhalConstants.DOOR_ROW_1_RIGHT, VhalConstants.WINDOW_ROW_1_RIGHT, "FR"),
    REAR_LEFT(VhalConstants.DOOR_ROW_2_LEFT, VhalConstants.WINDOW_ROW_2_LEFT, "RL"),
    REAR_RIGHT(VhalConstants.DOOR_ROW_2_RIGHT, VhalConstants.WINDOW_ROW_2_RIGHT, "RR"),
    ;

    companion object {
        /** null với areaId không phải cửa hành khách (vd cốp 0x20000000, nắp ca-pô 0x10000000). */
        fun fromDoorAreaId(areaId: Int): DoorCorner? = entries.firstOrNull { it.doorAreaId == areaId }
    }
}
