package com.aliucord.manager.manager

import android.content.Context
import android.content.pm.PackageManager
import com.aliucord.manager.R
import com.aliucord.manager.util.showToast
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Handles setting up Dhizuku and obtaining permissions.
 */
class DhizukuManager(private val context: Context) {
    private var dhizukuPermissionLock = Mutex()
    private val dhizukuAvailable = AtomicBoolean(false)

    /**
     * Determines whether Dhizuku is available and the binder has been retrieved.
     */
    fun dhizukuAvailable(): Boolean {
        if (!dhizukuAvailable.get()) {
            return Dhizuku.init(context)
                .also(dhizukuAvailable::set)
        }
        return true
    }

    /**
     * Checks whether Dhizuku permissions have been granted to this app.
     */
    fun checkPermissions(): Boolean {
        if (!dhizukuAvailable()) return false

        return Dhizuku.isPermissionGranted()
    }

    /**
     * Requests and waits for Dhizuku permissions if they have not already been granted.
     */
    suspend fun requestPermissions(): Boolean {
        if (!dhizukuAvailable()) return false

        // Lock and check if the previous holder already obtained permissions
        dhizukuPermissionLock.lock()
        try {
            if (checkPermissions()) {
                dhizukuPermissionLock.unlock()
                return true
            }
        } catch (_: Exception) {
            dhizukuPermissionLock.unlock()
        }

        return suspendCancellableCoroutine { continuation ->
            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                override fun onRequestPermission(grantResult: Int) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED)
                        context.showToast(R.string.permissions_dhizuku_denied)

                    continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                    dhizukuPermissionLock.unlock()
                }
            })
        }
    }
}
