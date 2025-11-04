package com.aliucord.manager.util

import android.os.Build
import androidx.collection.ObjectList
import java.io.BufferedReader
import java.io.IOException
import java.lang.Long.signum
import java.text.StringCharacterIterator
import java.util.Locale
import kotlin.math.*

/**
 * Formats this number as bytes to a human readable short file size in terms of 1024b = 1KiB
 */
// https://stackoverflow.com/a/3758880/13964629
fun Long.formatShortFileSize(): String {
    val bytes = this

    val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
    if (absB < 1024) {
        return "$bytes B"
    }
    var value = absB
    val ci = StringCharacterIterator("KMGTPE")
    var i = 40
    while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
        value = value shr 10
        ci.next()
        i -= 10
    }
    value *= signum(bytes).toLong()
    return String.format(Locale.ROOT, "%.1f %ciB", value / 1024.0, ci.current())
}


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


