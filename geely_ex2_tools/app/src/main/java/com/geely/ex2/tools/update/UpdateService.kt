package com.geely.ex2.tools.update

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.geely.ex2.tools.BuildConfig
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

/**
 * Background service that periodically checks a remote JSON endpoint for a newer APK and
 * installs it. When running as a system/priv-app (flavor `system`) this will attempt a
 * silent install using PackageInstaller APIs or pm command. On normal (user) installs it
 * falls back to interactive install via FileProvider (not implemented here).
 */
class UpdateService : Service() {
    private val TAG = "UpdateService"
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var scheduledFuture: ScheduledFuture<*>? = null

    companion object {
        // TODO: replace with your real update JSON endpoint
        const val UPDATE_JSON_URL = "https://updates.example.com/geely_ex2_tools/version.json"
        // check interval: 3 hours
        const val CHECK_INTERVAL_MIN = 60 * 3
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: starting scheduled updater")
        // start immediately, then periodically
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
        if (remoteVersion <= BuildConfig.VERSION_CODE) {
            Log.i(TAG, "No update: remote=$remoteVersion local=${BuildConfig.VERSION_CODE}")
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

        // If running as system flavor, attempt silent install
        if (BuildConfig.FLAVOR == "system") {
            Log.i(TAG, "Attempting silent install (system)")
            val installed = trySilentInstall(apkFile)
            Log.i(TAG, "Silent install result: $installed")
        } else {
            Log.i(TAG, "Non-system build: interactive install required (not implemented)")
            // We intentionally avoid prompting user here; interactive flow could be added.
        }
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
        // First try PackageInstaller session API (requires privileged INSTALL_PACKAGES)
        try {
            val pm = packageManager
            val installer: PackageInstaller = pm.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            // optional: set app package name / install flags
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            session.openWrite("package", 0, -1).use { out ->
                FileInputStream(apkFile).use { input ->
                    input.copyTo(out)
                    out.flush()
                }
            }
            val intentSender = null
            session.commit(intentSender)
            session.close()
            return true
        } catch (e: Exception) {
            Log.w(TAG, "PackageInstaller session failed", e)
        }

        // Fallback: try invoking pm install -r
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
