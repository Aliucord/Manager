package com.aliucord.manager.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
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

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val power = applicationContext.getSystemService(PowerManager::class.java)
    val name = applicationContext.packageName
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return power.isIgnoringBatteryOptimizations(name)
    }
    return true
}

/**
 * Launch a system dialog to enable unrestricted battery usage.
 */
@SuppressLint("BatteryLife")
fun Context.requestNoBatteryOptimizations() {
    val intent = Intent(
        /* action = */ Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        /* uri = */ Uri.fromParts("package", this.packageName, null)
    )

    with(intent) {
        addCategory(Intent.CATEGORY_DEFAULT)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }

    startActivity(intent)
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
