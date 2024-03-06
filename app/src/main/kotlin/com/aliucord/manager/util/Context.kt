package com.aliucord.manager.util

import android.app.Activity
import android.content.*
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import java.io.File

fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    clipboard.setPrimaryClip(
        ClipData.newPlainText(BuildConfig.APPLICATION_ID, text)
    )
}

fun Context.saveFile(name: String, text: String): File? {
    val basePath = Environment.getExternalStorageDirectory()
    val file = File(basePath, "/Download/$name")

    return try {
        file.writeText(text)
        showToast(R.string.installer_file_save_success, file.relativeTo(basePath))
        file
    } catch (e: Throwable) {
        Log.e(BuildConfig.TAG, Log.getStackTraceString(e))
        showToast(R.string.installer_file_save_failed, file.relativeTo(basePath))
        null
    }
}

fun Context.showToast(@StringRes text: Int, vararg args: Any, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, this.getString(text, *args), length).show()
}

/**
 * @return (versionName, versionCode)
 */
fun Context.getPackageVersion(pkg: String): Pair<String, Int> {
    @Suppress("DEPRECATION")
    return packageManager.getPackageInfo(pkg, 0)
        .let { it.versionName to it.versionCode }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
