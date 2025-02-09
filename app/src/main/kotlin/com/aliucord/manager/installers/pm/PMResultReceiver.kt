package com.aliucord.manager.installers.pm

import android.content.*
import android.content.pm.PackageInstaller
import com.aliucord.manager.R
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.installers.UnknownInstallerError
import com.aliucord.manager.util.showToast

/**
 * This receiver is meant to be registered dynamically in combination with [PMIntentReceiver] in order to
 * relay parsed [PackageInstaller] results back to the running application.
 */
class PMResultReceiver(
    private val sessionId: Int,
    private val isUninstall: Boolean,
    private val onResult: (InstallerResult) -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getIntExtra(EXTRA_SESSION_ID, -1) != sessionId) return

        try {
            when (intent.action) {
                ACTION_RECEIVE_RESULT -> handleResult(context, intent)
            }
        } catch (t: Throwable) {
            context.unregisterReceiver(this)
            onResult(UnknownInstallerError(t))
        }
    }

    private fun handleResult(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        val result = intent.getParcelableExtra<InstallerResult>(EXTRA_RESULT) ?: return

        // Show toast for successful and aborted sessions
        when (result) {
            InstallerResult.Success -> {
                context.showToast(if (!isUninstall) R.string.installer_install_success else R.string.installer_uninstall_success)
            }

            // The reason we don't do this in PMIntentReceiver is we can't tell whether it was
            // an old session that for which `abandonSession(...)` was called
            is InstallerResult.Cancelled -> {
                context.showToast(if (!isUninstall) R.string.installer_install_aborted else R.string.installer_uninstall_aborted)
            }

            else -> {}
        }

        context.unregisterReceiver(this)
        onResult(result)
    }

    companion object {
        const val ACTION_RECEIVE_INTENT = "com.aliucord.manager.RELAY_PM_INTENT"
        const val ACTION_RECEIVE_RESULT = "com.aliucord.manager.RELAY_PM_RESULT"
        const val EXTRA_RESULT = "installerResult"
        const val EXTRA_SESSION_ID = "sessionId"

        /**
         * The intent filter this receiver should be registered with to work properly.
         */
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_RECEIVE_INTENT)
            addAction(ACTION_RECEIVE_RESULT)
        }
    }
}
