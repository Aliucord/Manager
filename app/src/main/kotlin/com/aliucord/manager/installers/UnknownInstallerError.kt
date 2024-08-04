package com.aliucord.manager.installers

import android.content.Context
import com.aliucord.manager.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class UnknownInstallerError(val error: Throwable) : InstallerResult.Error() {
    override fun getDebugReason() = error.stackTraceToString()

    // No localizations for exceptions, use short message anyway
    override fun getLocalizedReason(context: Context) =
        error.message ?: context.getString(R.string.install_error_unknown)
}
