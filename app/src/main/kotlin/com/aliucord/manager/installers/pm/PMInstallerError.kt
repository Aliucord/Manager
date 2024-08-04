package com.aliucord.manager.installers.pm

import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Parcelable
import com.aliucord.manager.R
import com.aliucord.manager.installers.InstallerResult
import kotlinx.parcelize.Parcelize

/**
 * Translates the errors returned by PackageInstaller's [PackageInstaller.EXTRA_STATUS]
 * that is captured by a receiver into something human readable.
 */
@Parcelize
data class PMInstallerError(val status: Int) : InstallerResult.Error(), Parcelable {
    override fun getDebugReason() = when (status) {
        PackageInstaller.STATUS_FAILURE -> "Unknown failure"
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "Blocked"
        PackageInstaller.STATUS_FAILURE_INVALID -> "Invalid package"
        PackageInstaller.STATUS_FAILURE_CONFLICT -> "Package conflict"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "Storage error"
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "Device incompatibility"
        /* PackageInstaller.STATUS_FAILURE_TIMEOUT */ 8 -> "Operation timeout"
        else -> "Unknown code ($status)"
    }

    override fun getLocalizedReason(context: Context): String {
        val string = when (status) {
            PackageInstaller.STATUS_FAILURE -> R.string.install_error_unknown
            PackageInstaller.STATUS_FAILURE_BLOCKED -> R.string.install_error_blocked
            PackageInstaller.STATUS_FAILURE_INVALID -> R.string.install_error_invalid
            PackageInstaller.STATUS_FAILURE_CONFLICT -> R.string.install_error_conflict
            PackageInstaller.STATUS_FAILURE_STORAGE -> R.string.install_error_storage
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> R.string.install_error_incompatible
            /* PackageInstaller.STATUS_FAILURE_TIMEOUT */ 8 -> R.string.install_error_timeout
            else -> R.string.install_error_unknown
        }

        return context.getString(string)
    }
}
