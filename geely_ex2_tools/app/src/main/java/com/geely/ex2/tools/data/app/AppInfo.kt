package com.geely.ex2.tools.data.app

import android.content.Context
import android.content.Intent
import android.net.Uri

const val DEVELOPER_TELEGRAM_USERNAME = "i_am_artsiom"

fun getAppVersionName(context: Context): String =
    runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

fun openDeveloperTelegram(context: Context) {
    val uri = Uri.parse("https://t.me/$DEVELOPER_TELEGRAM_USERNAME")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    runCatching {
        context.startActivity(intent)
    }
}
