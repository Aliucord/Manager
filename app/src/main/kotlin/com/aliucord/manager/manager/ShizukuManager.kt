package com.aliucord.manager.manager

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.util.showToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * Handles setting up Shizuku and obtaining permissions.
 */
class ShizukuManager(private val context: Context) {
    private var shizukuPermissionLock = Mutex()
    private val shizukuAvailable: StateFlow<Boolean?>
        field = MutableStateFlow(null)

    init {
        Shizuku.addBinderReceivedListenerSticky {
            shizukuAvailable.value = true
        }
        Shizuku.addBinderDeadListener {
            shizukuAvailable.value = false

            if (shizukuPermissionLock.isLocked)
                shizukuPermissionLock.unlock()
        }

        // Required to change hidden SessionParams flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !hiddenApiDisabled.getAndSet(true))
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content", "Landroid/os")
    }

    /**
     * Determines whether Shizuku is available and the binder has been retrieved.
     */
    fun shizukuAvailable(): Boolean = when (shizukuAvailable.value) {
        true -> true
        false -> false
        null -> {
            Shizuku.pingBinder().also { shizukuAvailable.value = it }
        }
    }

    /**
     * Checks whether Shizuku permissions have been granted to this app.
     */
    fun checkPermissions(): Boolean {
        if (!shizukuAvailable()) return false

        // Old shizuku does not have permission checks
        if (Shizuku.isPreV11()) return true

        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests and waits for Shizuku permissions if they have not already been granted.
     */
    suspend fun requestPermissions(): Boolean {
        if (!shizukuAvailable()) return false

        // Lock and check if the previous holder already obtained permissions
        shizukuPermissionLock.lock()
        try {
            if (checkPermissions()) {
                shizukuPermissionLock.unlock()
                return true
            }
        } catch (_: Exception) {
            shizukuPermissionLock.unlock()
        }

        return suspendCancellableCoroutine { continuation ->
            val currentRequestCode = Random.Default.nextInt()
            val onPermissionRequestResult =
                Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                    if (requestCode != currentRequestCode)
                        return@OnRequestPermissionResultListener

                    if (grantResult == PackageManager.PERMISSION_DENIED)
                        context.showToast(R.string.permissions_shizuku_denied)

                    continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                }

            continuation.invokeOnCancellation {
                Shizuku.removeRequestPermissionResultListener(onPermissionRequestResult)
                shizukuPermissionLock.unlock()
            }

            Shizuku.addRequestPermissionResultListener(onPermissionRequestResult)
            Shizuku.requestPermission(currentRequestCode)
        }
    }

    private companion object {
        var hiddenApiDisabled = AtomicBoolean(false)
    }
}
