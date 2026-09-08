package com.geely.ex2.tools.data.vhal

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Đăng ký nhận các sự kiện VHAL on-change hiếm gặp (sang số v.v...).
 * Có thể thêm các thuộc tính và loại [VhalVehicleEvent] mới vào đây mà không làm phình to các service.
 */
class VhalVehicleEventHub(context: Context) {
    private val appContext = context.applicationContext
    private val bindings = CarVhalBindings(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<VhalVehicleEventListener>()
    private val registeredCallbacks = mutableListOf<Any>()

    @Volatile
    private var isStarted = false

    @Volatile
    private var lastGearSelection: Int? = null

    @Volatile
    private var hasGearBaseline = false

    private val gearDebounceRunnable = Runnable {
        if (!isStarted) return@Runnable
        val selection = lastGearSelection ?: return@Runnable
        dispatch(VhalVehicleEvent.GearChanged(gearSelection = selection))
    }

    fun addListener(listener: VhalVehicleEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: VhalVehicleEventListener) {
        listeners.remove(listener)
    }

    /** Gọi từ luồng [CarPropertyIo]. */
    fun start(): Boolean {
        if (isStarted) return true

        if (!bindings.ensureConnected()) {
            Log.w(TAG, "Vehicle event hub start skipped: CarPropertyManager unavailable")
            return false
        }

        val hasGearSelection = subscribeInt(VhalConstants.PROP_GEAR_SELECTION) { value ->
            onGearSelectionChanged(value)
        }
        if (!hasGearSelection) {
            Log.w(TAG, "Vehicle event hub start failed: GEAR_SELECTION callback rejected")
            unregisterAll()
            bindings.close()
            return false
        }

        isStarted = true
        Log.i(TAG, "Vehicle event hub started (GEAR_SELECTION on-change)")
        return true
    }

    /** Gọi từ luồng [CarPropertyIo]. */
    fun stop() {
        if (!isStarted && registeredCallbacks.isEmpty()) {
            listeners.clear()
            return
        }

        isStarted = false
        mainHandler.removeCallbacks(gearDebounceRunnable)
        unregisterAll()
        bindings.close()
        lastGearSelection = null
        hasGearBaseline = false
        Log.i(TAG, "Vehicle event hub stopped")
    }

    private fun onGearSelectionChanged(value: Int) {
        // Callback đầu tiên sau khi register — là giá trị hiện tại, không phải là chuyển số.
        if (!hasGearBaseline) {
            lastGearSelection = value
            hasGearBaseline = true
            Log.d(TAG, "Gear baseline set to $value")
            return
        }

        if (lastGearSelection == value) {
            return
        }

        lastGearSelection = value
        scheduleGearDispatch()
    }

    private fun unregisterAll() {
        registeredCallbacks.forEach { callback ->
            bindings.unregisterPropertyCallback(callback)
        }
        registeredCallbacks.clear()
    }

    private fun subscribeInt(propertyId: Int, onValue: (Int) -> Unit): Boolean {
        val callback = bindings.registerIntPropertyCallback(
            propertyId = propertyId,
            updateRateHz = VhalConstants.CALLBACK_RATE_ONCHANGE_HZ,
            onValue = onValue,
            onError = { error ->
                Log.w(TAG, "VHAL callback error 0x${propertyId.toString(16)}: $error")
            },
        ) ?: return false
        registeredCallbacks.add(callback)
        return true
    }

    private fun scheduleGearDispatch() {
        mainHandler.removeCallbacks(gearDebounceRunnable)
        mainHandler.postDelayed(gearDebounceRunnable, VhalConstants.GEAR_EVENT_DEBOUNCE_MS)
    }

    private fun dispatch(event: VhalVehicleEvent) {
        for (listener in listeners) {
            try {
                listener.onVehicleEvent(event)
            } catch (t: Throwable) {
                Log.w(TAG, "Vehicle event listener failed for $event", t)
            }
        }
    }

    companion object {
        private const val TAG = "GeelyToolsVhalEvents"
    }
}
