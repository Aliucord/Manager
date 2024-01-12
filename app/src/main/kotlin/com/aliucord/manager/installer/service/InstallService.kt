package com.aliucord.manager.installer.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.IBinder
import android.util.Log
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.util.showToast

class InstallService : Service() {
    private val errorMessages = mapOf(
        PackageInstaller.STATUS_FAILURE to R.string.install_error_unknown,
        PackageInstaller.STATUS_FAILURE_BLOCKED to R.string.install_error_blocked,
        PackageInstaller.STATUS_FAILURE_INVALID to R.string.install_error_invalid,
        PackageInstaller.STATUS_FAILURE_CONFLICT to R.string.install_error_conflict,
        PackageInstaller.STATUS_FAILURE_STORAGE to R.string.install_error_storage,
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE to R.string.install_error_incompatible,
        PackageInstaller.STATUS_FAILURE_TIMEOUT to R.string.install_error_timeout
    )

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (val statusCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION") // No.
                val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                startActivity(confirmationIntent)
            }

            PackageInstaller.STATUS_SUCCESS -> showToast(R.string.installer_success)
            PackageInstaller.STATUS_FAILURE_ABORTED -> showToast(R.string.installer_aborted)

            else -> {
                Log.i(BuildConfig.TAG, "Install failed with error code $statusCode")

                if (errorMessages[statusCode] != null) {
                    showToast(errorMessages[statusCode]!!)
                } else {
                    showToast(R.string.install_error_code, statusCode)
                }
            }
        }

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null
}
