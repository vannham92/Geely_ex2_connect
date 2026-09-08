package com.example.ex2_phone.update

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class UpdateService : Service() {
    private val TAG = "PhoneUpdateService"
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var scheduledFuture: ScheduledFuture<*>? = null

    companion object {
        const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/vannham92/Geely_ex2_connect/main/updates/ex2_phone/version.json"
        const val CHECK_INTERVAL_MIN = 60 * 3
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: starting scheduled updater")
        scheduledFuture = scheduler.scheduleWithFixedDelay({
            try {
                checkOnce()
            } catch (e: Throwable) {
                Log.e(TAG, "update check failed", e)
            }
        }, 0, CHECK_INTERVAL_MIN.toLong(), TimeUnit.MINUTES)
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledFuture?.cancel(true)
        scheduler.shutdownNow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getLocalVersionCode(): Int {
        return try {
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            try {
                val longField = pkgInfo::class.java.getDeclaredField("longVersionCode")
                longField.isAccessible = true
                val lv = longField.getLong(pkgInfo)
                lv.toInt()
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                try {
                    val vcField = pkgInfo::class.java.getDeclaredField("versionCode")
                    vcField.isAccessible = true
                    vcField.getInt(pkgInfo)
                } catch (e2: Exception) {
                    0
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun checkOnce() {
        Log.i(TAG, "Checking update endpoint: $UPDATE_JSON_URL")
        val json = fetchUrl(UPDATE_JSON_URL) ?: run {
            Log.w(TAG, "No response from update endpoint")
            return
        }
        val obj = try { JSONObject(json) } catch (e: Exception) {
            Log.e(TAG, "Invalid JSON", e); return
        }
        val remoteVersion = obj.optInt("versionCode", -1)
        val apkUrl = obj.optString("apkUrl", "")
        val sha256 = obj.optString("sha256", "")
        val localVersion = getLocalVersionCode()
        if (remoteVersion <= localVersion) {
            Log.i(TAG, "No update: remote=$remoteVersion local=$localVersion")
            return
        }
        if (apkUrl.isBlank()) {
            Log.w(TAG, "No apkUrl in update JSON")
            return
        }
        Log.i(TAG, "New version available: $remoteVersion, downloading $apkUrl")
        val apkFile = File(cacheDir, "update-${remoteVersion}.apk")
        if (!downloadFile(apkUrl, apkFile)) {
            Log.e(TAG, "Download failed")
            return
        }
        if (sha256.isNotBlank()) {
            val ok = verifySha256(apkFile, sha256)
            if (!ok) {
                Log.e(TAG, "SHA256 mismatch, aborting")
                apkFile.delete()
                return
            }
        }

        // For phone builds we will not perform silent install; interactive install required.
        Log.i(TAG, "Downloaded update for phone: ${apkFile.absolutePath}")
    }

    private fun fetchUrl(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 30_000
                requestMethod = "GET"
            }
            if (conn.responseCode != 200) {
                Log.w(TAG, "HTTP ${conn.responseCode} from $urlStr")
                null
            } else {
                conn.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchUrl error", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun downloadFile(urlStr: String, outFile: File): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 60_000
                requestMethod = "GET"
                instanceFollowRedirects = true
            }
            if (conn.responseCode != 200) {
                Log.w(TAG, "Download HTTP ${conn.responseCode}")
                return false
            }
            conn.inputStream.use { input ->
                outFile.outputStream().use { fos ->
                    input.copyTo(fos)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "downloadFile error", e)
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun verifySha256(file: File, expectedHex: String): Boolean {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buf = ByteArray(8192)
                var read: Int
                while (fis.read(buf).also { read = it } > 0) {
                    md.update(buf, 0, read)
                }
            }
            val actual = md.digest().joinToString("") { "%02x".format(it) }
            actual.equals(expectedHex, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "verifySha256 error", e)
            false
        }
    }

    private fun trySilentInstall(apkFile: File): Boolean {
        try {
            val pm = packageManager
            val installer: PackageInstaller = pm.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            session.openWrite("package", 0, -1).use { out ->
                FileInputStream(apkFile).use { input ->
                    input.copyTo(out)
                    out.flush()
                }
            }
            // Create a PendingIntent so we can pass a non-null IntentSender to commit()
            val piIntent = Intent("com.example.ex2_phone.UPDATE_INSTALL")
            val pending = PendingIntent.getBroadcast(applicationContext, 0, piIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
            val intentSender: IntentSender = pending.intentSender
            session.commit(intentSender)
            session.close()
            return true
        } catch (e: Exception) {
            Log.w(TAG, "PackageInstaller session failed", e)
        }
        try {
            val cmd = arrayOf("pm", "install", "-r", apkFile.absolutePath)
            val proc = Runtime.getRuntime().exec(cmd)
            val exit = proc.waitFor()
            Log.i(TAG, "pm exit code=$exit")
            return exit == 0
        } catch (e: Exception) {
            Log.e(TAG, "pm install failed", e)
        }
        return false
    }
}
