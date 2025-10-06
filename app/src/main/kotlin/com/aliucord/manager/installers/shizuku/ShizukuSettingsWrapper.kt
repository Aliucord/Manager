package com.aliucord.manager.installers.shizuku

import android.content.*
import android.os.*
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.aliucord.manager.util.HiddenAPI
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

// Based on https://github.com/vvb2060/PackageInstaller/blob/3d113a5e000c62a712e6165cb75cbca63fb912aa/app/src/main/java/io/github/vvb2060/packageinstaller/model/Hook.kt

/**
 * Wraps writing to Android's global settings through Shizuku.
 * This allows accessing Android secure settings.
 */
object ShizukuSettingsWrapper {
    /**
     * Disable ADB install verification (bypass useless Play Protect).
     */
    fun disableAdbVerify(context: Context) {
        val settingName = "verifier_verify_adb_installs"
        val enabled = Settings.Global.getInt(context.contentResolver, settingName, 1) != 0
        if (enabled) {
            wrapGlobalSettings {
                val contextWrapper = ShizukuContext(context)
                val cr = object : ContentResolver(contextWrapper) {}
                Settings.Global.putInt(cr, settingName, 0)
            }
        }
    }

    private fun wrapGlobalSettings(callback: () -> Unit) {
        HiddenAPI.disable()

        val holder = Settings.Global::class.java.getDeclaredField("sProviderHolder")
            .apply { isAccessible = true }
            .get(null)
        val provider = holder::class.java.getDeclaredField("mContentProvider")
            .apply { isAccessible = true }
            .get(holder)

        val remoteField = provider::class.java.getDeclaredField("mRemote")
            .apply { isAccessible = true }

        val originalBinder = remoteField.get(provider) as IBinder
        remoteField.set(provider, ShizukuBinderWrapper(originalBinder))
        callback()
        remoteField.set(provider, originalBinder)
    }

    private class ShizukuContext(context: Context) : ContextWrapper(context) {
        override fun getOpPackageName(): String = "com.android.shell"

        @RequiresApi(Build.VERSION_CODES.S)
        override fun getAttributionSource(): AttributionSource {
            val builder = AttributionSource.Builder(Shizuku.getUid())
                .setPackageName("com.android.shell")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                builder.setPid(Process.INVALID_PID)
            }
            return builder.build()
        }
    }
}
