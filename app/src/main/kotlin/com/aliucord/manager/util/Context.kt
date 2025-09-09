package com.aliucord.manager.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.google.android.gms.safetynet.SafetyNet
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    clipboard.setPrimaryClip(
        ClipData.newPlainText(BuildConfig.APPLICATION_ID, text)
    )
}

suspend fun Context.saveFile(name: String, text: String): Boolean {
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloads, name)
    val base = downloads.parentFile!!

    return try {
        file.writeText(text)
        mainThread { showToast(R.string.installer_file_save_success, file.toRelativeString(base)) }
        true
    } catch (e: Throwable) {
        Log.e(BuildConfig.TAG, Log.getStackTraceString(e))
        mainThread { showToast(R.string.installer_file_save_failed, file.toRelativeString(base)) }
        false
    }
}

fun Context.showToast(@StringRes text: Int, vararg args: Any, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, this.getString(text, *args), length).show()
}

fun Context.selfHasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * @return (versionName, versionCode)
 */
fun Context.getPackageVersion(pkg: String): Pair<String?, Int> {
    @Suppress("DEPRECATION")
    return packageManager.getPackageInfo(pkg, 0)
        .let { it.versionName to it.versionCode }
}

fun Context.isPackageInstalled(packageName: String): Boolean {
    return try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

    val power = applicationContext.getSystemService(PowerManager::class.java)
    val name = applicationContext.packageName
    return power.isIgnoringBatteryOptimizations(name)
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

/**
 * Checks if the Play Protect/Verify Apps feature is enabled on this device.
 * @return `null` if failed to obtain, otherwise whether it's enabled.
 */
suspend fun Context.isPlayProtectEnabled(): Boolean? {
    return suspendCoroutine { continuation ->
        SafetyNet.getClient(this)
            .isVerifyAppsEnabled
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val enabled = task.result.isVerifyAppsEnabled
                    Log.d(BuildConfig.TAG, "Play Protect enabled: $enabled")
                    continuation.resume(enabled)
                } else {
                    Log.d(BuildConfig.TAG, "Failed to check Play Protect status", task.exception)
                    continuation.resume(null)
                }
            }
    }
}

/**
 * Check whether the device is connected on a metered WIFI connection or through any type of mobile data,
 * to avoid unknowingly downloading a lot of stuff through a potentially metered network.
 */
@Suppress("DEPRECATION")
fun Context.isNetworkDangerous(): Boolean {
    val connectivity = this.getSystemService<ConnectivityManager>()
        ?: error("Unable to get system connectivity service")

    if (connectivity.isActiveNetworkMetered) return true

    when (val info = connectivity.activeNetworkInfo) {
        null -> return false
        else -> {
            if (info.isRoaming) return true
            if (info.type == ConnectivityManager.TYPE_WIFI) return false
        }
    }

    val telephony = this.getSystemService<TelephonyManager>()
        ?: error("Unable to get system telephony service")

    val dangerousMobileDataStates = arrayOf(
        /* TelephonyManager.DATA_DISCONNECTING */ 4,
        TelephonyManager.DATA_CONNECTED,
        TelephonyManager.DATA_CONNECTING,
    )

    return dangerousMobileDataStates.contains(telephony.dataState)
}
