package com.aliucord.manager.util

import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Globally disables the Android Hidden API restrictions.
 */
object HiddenAPI {
    private var disabled = AtomicBoolean(false)

    fun disable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !disabled.getAndSet(true))
            HiddenApiBypass.setHiddenApiExemptions("")
    }
}

