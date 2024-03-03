package com.aliucord.manager.util

import android.content.*
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.annotation.StringRes
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import java.io.File
import java.io.InputStream

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

/**
 * Get the raw bytes for a resource.
 * @param id The resource identifier
 * @return The resource's raw bytes as stored inside the APK
 */
fun Context.getResBytes(@AnyRes id: Int): ByteArray {
    val tValue = TypedValue()
    this.resources.getValue(
        /* id = */ id,
        /* outValue = */ tValue,
        /* resolveRefs = */ true,
    )

    val resPath = tValue.string.toString()

    return this.javaClass.classLoader
        ?.getResourceAsStream(resPath)
        ?.use(InputStream::readBytes)
        ?: error("Failed to get resource file $resPath from APK")
}
