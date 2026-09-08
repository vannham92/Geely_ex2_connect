package com.geely.ex2.tools.data.kv

import android.app.Application
import android.content.Context
import android.os.Build
import com.tencent.mmkv.MMKV

/**
 * Thin MULTI_PROCESS MMKV wrapper. Root dir is device-protected so Direct Boot
 * receivers/services can read settings before user unlock.
 */
object AppKv {
    @Volatile
    private var initialized = false

    @Volatile
    private var storageContext: Context? = null

    @Volatile
    private var packageName: String? = null

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            val dpContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appContext.createDeviceProtectedStorageContext()
            } else {
                appContext
            }
            MMKV.initialize(dpContext)
            storageContext = dpContext
            packageName = appContext.packageName
            initialized = true
        }
    }

    /** Default mmap used for cross-process sample snapshots and misc keys. */
    fun default(): MMKV {
        ensureInit()
        return MMKV.defaultMMKV(MMKV.MULTI_PROCESS_MODE, null).also {
            it.checkContentChangedByOuterProcess()
        }
    }

    /**
     * Named mmap (one per legacy SharedPreferences file name).
     * Migrates old prefs into MMKV once on first access (default process only).
     */
    fun of(prefsName: String): MMKV {
        ensureInit()
        val mmkv = MMKV.mmkvWithID(prefsName, MMKV.MULTI_PROCESS_MODE)
        if (isDefaultProcess()) {
            migrateSharedPreferencesOnce(prefsName, mmkv)
        }
        mmkv.checkContentChangedByOuterProcess()
        return mmkv
    }

    private fun ensureInit() {
        check(initialized) {
            "AppKv.init() must be called from Application.onCreate before use"
        }
    }

    private fun isDefaultProcess(): Boolean {
        val pkg = packageName ?: return false
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            return true
        }
        return processName == pkg
    }

    private fun migrateSharedPreferencesOnce(prefsName: String, mmkv: MMKV) {
        val flag = "__sp_migrated"
        if (mmkv.decodeBool(flag, false)) return

        synchronized(this) {
            if (mmkv.decodeBool(flag, false)) return
            val context = storageContext ?: return
            val sp = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            if (sp.all.isNotEmpty()) {
                mmkv.importFromSharedPreferences(sp)
                sp.edit().clear().commit()
            }
            mmkv.encode(flag, true)
        }
    }
}
