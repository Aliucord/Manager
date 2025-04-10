package com.aliucord.manager.util

import android.os.Build
import androidx.collection.ObjectList
import com.aliucord.manager.BuildConfig
import java.io.BufferedReader
import java.io.IOException
import kotlin.math.pow
import kotlin.math.truncate

/**
 * Truncates this value to a specific number of [decimals] digits.
 */
fun Double.toPrecision(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return truncate(this * multiplier) / multiplier
}

inline fun <E> ObjectList<E>.find(block: (E) -> Boolean): E? {
    forEach { value ->
        if (block(value))
            return value
    }

    return null
}

/**
 * Whether this manager build is not an official release.
 */
@Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
val IS_CUSTOM_BUILD by lazy {
    BuildConfig.GIT_BRANCH != "release"
        || BuildConfig.GIT_LOCAL_CHANGES
        || BuildConfig.GIT_LOCAL_COMMITS
}

/**
 * Check whether this device is most likely an emulator.
 * src: https://stackoverflow.com/a/21505193/13964629
 */
val IS_PROBABLY_EMULATOR by lazy {
    // Android SDK emulator
    return@lazy ((Build.MANUFACTURER == "Google" && Build.BRAND == "google" &&
        ((Build.FINGERPRINT.startsWith("google/sdk_gphone_")
            && Build.FINGERPRINT.endsWith(":user/release-keys")
            && Build.PRODUCT.startsWith("sdk_gphone_")
            && Build.MODEL.startsWith("sdk_gphone_"))
            // Alternative
            || (Build.FINGERPRINT.startsWith("google/sdk_gphone64_")
            && (Build.FINGERPRINT.endsWith(":userdebug/dev-keys") || Build.FINGERPRINT.endsWith(":user/release-keys"))
            && Build.PRODUCT.startsWith("sdk_gphone64_")
            && Build.MODEL.startsWith("sdk_gphone64_"))))
        || Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        // Bluestacks
        || "QC_Reference_Phone" == Build.BOARD && !"Xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)
        // Bluestacks
        || Build.MANUFACTURER.contains("Genymotion")
        || Build.HOST.startsWith("Build")
        // MSI App Player
        || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
        || Build.PRODUCT == "google_sdk"
        // Another Android SDK emulator check
        || getSystemProp("ro.kernel.qemu") == "1")
}

/**
 * Checks whether this device is running MIUI
 */
fun isMiui(): Boolean {
    return getSystemProp("ro.miui.ui.version.name")
        ?.isNotEmpty() ?: false
}

/**
 * Gets a system property from build.prop
 */
private fun getSystemProp(name: String): String? {
    var reader: BufferedReader? = null

    return try {
        val process = Runtime.getRuntime().exec("getprop $name")
        reader = process.inputStream.bufferedReader()
        reader.readLine()
    } catch (_: Exception) {
        null
    } finally {
        try {
            reader?.close()
        } catch (_: IOException) {
            // ignore
        }
    }
}


