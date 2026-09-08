package com.geely.ex2.tools.data.scene

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * Điều khiển **Scene Mode / 情景空间** (`com.flyme.auto.scenedirector`) — chế độ kịch bản không gian
 * của Flyme Auto HU: Nghỉ ngơi (Rest/Nap) và Cắm trại (Camping).
 *
 * Kích hoạt = start hai Activity **exported** của SceneDirector qua Intent (giống `am start`):
 *  - Rest:    action `android.intent.action.START_REST`    -> `.activities.nap.RestActivity`
 *  - Camping: action `android.intent.action.START_CAMPING` -> `.activities.camping.CampingActivity`
 *
 * Đọc scene hiện tại = **best-effort**: SceneDirector công bố trạng thái qua
 * `content://com.flyme.auto.scenedirector/scene` và `Settings.Secure sysui_alive_launcher_settings`.
 * Schema cột không được tài liệu hoá chắc chắn nên đọc phòng thủ; lỗi/không rõ -> [SceneSample.available] = false.
 *
 * → xem docs/flyme-scenedirector-apk.md và docs/phone-companion-protocol.md.
 */
class SceneRepository(context: Context) {
    private val appContext = context.applicationContext

    /** Kết quả kích hoạt scene (giống pattern *WriteResult của các reader VHAL). */
    data class SceneResult(val ok: Boolean, val error: String? = null)

    /** Trạng thái scene đọc được, map sang phone domain [phoneMode]. */
    data class SceneSample(val available: Boolean, val phoneMode: Int)

    /** Kích hoạt Rest (Nghỉ ngơi). */
    fun startRest(): SceneResult =
        startScene(ACTION_START_REST, ACTIVITY_REST)

    /** Kích hoạt Camping (Cắm trại). */
    fun startCamping(): SceneResult =
        startScene(ACTION_START_CAMPING, ACTIVITY_CAMPING)

    /**
     * Map phone modeValue -> scene: `1`=Rest, `2`=Camping.
     * (Không có cơ chế thoát scene bằng Intent trong tài liệu → `0`/khác trả lỗi.)
     */
    fun setScene(phoneMode: Int): SceneResult = when (phoneMode) {
        PHONE_SCENE_REST -> startRest()
        PHONE_SCENE_CAMPING -> startCamping()
        else -> SceneResult(ok = false, error = "scene modeValue không hỗ trợ: $phoneMode (1=rest, 2=camping)")
    }

    private fun startScene(action: String, activityClass: String): SceneResult {
        return try {
            val intent = Intent(action).apply {
                setClassName(SCENE_PACKAGE, activityClass)
                // startActivity từ context không phải Activity -> bắt buộc NEW_TASK.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            Log.i(TAG, "startScene ok: $action -> $activityClass")
            SceneResult(ok = true)
        } catch (e: Exception) {
            // ActivityNotFoundException: thiếu <queries> hoặc SceneDirector không có trên máy.
            Log.w(TAG, "startScene failed: $action", e)
            SceneResult(ok = false, error = "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Đọc scene hiện tại (best-effort). Thứ tự: ContentProvider -> Settings.Secure.
     * Không xác định được -> available=false (phone hiển thị `--`).
     */
    fun readCurrentScene(): SceneSample {
        readFromProvider()?.let { return SceneSample(available = true, phoneMode = it) }
        readFromSecureSettings()?.let { return SceneSample(available = true, phoneMode = it) }
        return SceneSample(available = false, phoneMode = PHONE_SCENE_NONE)
    }

    /** Query provider, quét mọi cột hàng đầu tìm scene id (int) hoặc keyword (string). */
    private fun readFromProvider(): Int? {
        return try {
            appContext.contentResolver.query(Uri.parse(PROVIDER_URI), null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return null
                for (i in 0 until c.columnCount) {
                    val fromInt = runCatching { c.getInt(i) }.getOrNull()?.let { sceneIdToPhone(it) }
                    if (fromInt != null && fromInt != PHONE_SCENE_NONE) return fromInt
                    val fromStr = runCatching { c.getString(i) }.getOrNull()?.let { parseSceneString(it) }
                    if (fromStr != null && fromStr != PHONE_SCENE_NONE) return fromStr
                }
                // Có hàng nhưng không khớp scene nào -> coi như DEFAULT (đang chạy provider = biết trạng thái).
                PHONE_SCENE_NONE
            }
        } catch (e: Exception) {
            Log.d(TAG, "readFromProvider failed: ${e.message}")
            null
        }
    }

    private fun readFromSecureSettings(): Int? {
        return try {
            val raw = Settings.Secure.getString(appContext.contentResolver, SECURE_KEY) ?: return null
            parseSceneString(raw)
        } catch (e: Exception) {
            Log.d(TAG, "readFromSecureSettings failed: ${e.message}")
            null
        }
    }

    /** Map scene id thô (int SceneStateSpec) -> phone domain. */
    private fun sceneIdToPhone(sceneId: Int): Int = when (sceneId) {
        SCENE_ID_NAP -> PHONE_SCENE_REST
        SCENE_ID_CAMPING -> PHONE_SCENE_CAMPING
        else -> PHONE_SCENE_NONE
    }

    /** Parse chuỗi bất kỳ (JSON/số/keyword) -> phone domain. Best-effort, không crash. */
    private fun parseSceneString(raw: String): Int {
        val s = raw.lowercase()
        return when {
            s.contains(SCENE_ID_CAMPING.toString()) || s.contains("camping") -> PHONE_SCENE_CAMPING
            s.contains(SCENE_ID_NAP.toString()) || s.contains("nap") || s.contains("rest") -> PHONE_SCENE_REST
            else -> PHONE_SCENE_NONE
        }
    }

    companion object {
        private const val TAG = "SceneRepository"

        const val SCENE_PACKAGE = "com.flyme.auto.scenedirector"
        private const val ACTIVITY_REST = "$SCENE_PACKAGE.activities.nap.RestActivity"
        private const val ACTIVITY_CAMPING = "$SCENE_PACKAGE.activities.camping.CampingActivity"
        private const val ACTION_START_REST = "android.intent.action.START_REST"
        private const val ACTION_START_CAMPING = "android.intent.action.START_CAMPING"

        private const val PROVIDER_URI = "content://com.flyme.auto.scenedirector/scene"
        private const val SECURE_KEY = "sysui_alive_launcher_settings"

        // SceneStateSpec (thô) — xem docs/flyme-scenedirector-apk.md §0.
        private const val SCENE_ID_NAP = 67108864       // 0x04000000 (Rest)
        private const val SCENE_ID_CAMPING = 201326592  // 0x0C000000

        // Phone domain (khớp docs/phone-companion-protocol.md).
        const val PHONE_SCENE_NONE = 0
        const val PHONE_SCENE_REST = 1
        const val PHONE_SCENE_CAMPING = 2
    }
}
