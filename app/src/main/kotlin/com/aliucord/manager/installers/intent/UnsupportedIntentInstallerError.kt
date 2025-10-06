package com.aliucord.manager.installers.intent

import android.content.Context
import com.aliucord.manager.R
import com.aliucord.manager.installers.InstallerResult
import kotlinx.parcelize.Parcelize

@Parcelize
data class UnsupportedIntentInstallerError(private val action: String) : InstallerResult.Error() {
    override fun getDebugReason() = "This Android rom does not support $action!"

    override fun getLocalizedReason(context: Context) =
        context.getString(R.string.install_error_unhandled_intent, action)
}

