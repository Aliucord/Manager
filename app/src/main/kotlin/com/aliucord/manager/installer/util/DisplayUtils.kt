package com.aliucord.manager.installer.util

import android.content.Context
import android.util.DisplayMetrics

object DisplayUtils {
    fun getDpiName(context: Context): String {
        val dpi = context.resources.displayMetrics.densityDpi

        return when {
            dpi <= DisplayMetrics.DENSITY_LOW -> "ldpi"
            dpi <= DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
            dpi <= DisplayMetrics.DENSITY_HIGH -> "hdpi"
            dpi <= DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
            dpi <= DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
            else -> "xxhdpi"
        }
    }
}
