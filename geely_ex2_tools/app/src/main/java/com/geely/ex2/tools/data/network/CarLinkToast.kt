package com.geely.ex2.tools.data.network

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

/**
 * Thông báo nổi trên head unit khi phone nối/ngắt — chữ to gấp đôi Toast thường.
 *
 * Vì sao không dùng `Toast.setView()`: từ Android 11 (API 30) custom view của Toast **bị bỏ qua**
 * khi app đang ở background, mà [CarTcpServer] chạy nền là chính. Thay bằng overlay window
 * (`TYPE_APPLICATION_OVERLAY`) — app ký platform + `android.uid.system` nên `SYSTEM_ALERT_WINDOW`
 * được cấp sẵn theo chữ ký. Không có quyền thì tự rơi về Toast thường (chữ nhỏ, vẫn hiện).
 *
 * Hiển thị [DURATION_MS] rồi tự gỡ. Thông báo mới đè thông báo cũ.
 */
object CarLinkToast {

    private const val TAG = "CarLinkToast"

    /** Thời gian hiện — Toast hệ thống chỉ có ~2s/~3.5s nên overlay tự hẹn giờ cho đúng 3s. */
    private const val DURATION_MS = 3_000L

    /** Toast mặc định ~14sp; yêu cầu "to gấp đôi". */
    private const val TEXT_SIZE_SP = 28f

    private val main = Handler(Looper.getMainLooper())

    private var currentView: View? = null
    private var currentWm: WindowManager? = null

    fun show(context: Context, text: String) {
        val appContext = context.applicationContext
        main.post { showOnMain(appContext, text) }
    }

    private fun showOnMain(context: Context, text: String) {
        dismiss()

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (wm == null || !Settings.canDrawOverlays(context)) {
            Log.i(TAG, "Không có quyền overlay — rơi về Toast thường")
            fallbackToast(context, text)
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(context, 96)
        }

        val view = buildView(context, text)
        try {
            wm.addView(view, params)
        } catch (t: Throwable) {
            Log.w(TAG, "addView overlay thất bại — rơi về Toast thường", t)
            fallbackToast(context, text)
            return
        }

        currentView = view
        currentWm = wm
        main.postDelayed(::dismiss, DURATION_MS)
    }

    private fun buildView(context: Context, text: String): View = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP)
        setTextColor(Color.WHITE)
        val padH = dp(context, 32)
        val padV = dp(context, 20)
        setPadding(padH, padV, padH, padV)
        background = GradientDrawable().apply {
            cornerRadius = dp(context, 28).toFloat()
            setColor(0xF0202225.toInt())
        }
        elevation = dp(context, 8).toFloat()
    }

    private fun fallbackToast(context: Context, text: String) {
        val toast = Toast.makeText(context, text, Toast.LENGTH_LONG)
        toast.show()
        main.postDelayed({ toast.cancel() }, DURATION_MS)
    }

    /** Gỡ thông báo đang hiện (nếu có). Chỉ gọi trên main thread. */
    private fun dismiss() {
        main.removeCallbacks(::dismiss)
        val view = currentView ?: return
        try {
            currentWm?.removeView(view)
        } catch (_: Throwable) {
            // view đã bị gỡ (window leak / process restart) — bỏ qua
        }
        currentView = null
        currentWm = null
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
