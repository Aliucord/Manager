package com.aliucord.manager.installers.pm

import android.content.*
import android.content.pm.PackageInstaller
import com.aliucord.manager.R
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.installers.UnknownInstallerError
import com.aliucord.manager.util.showToast
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This receiver is meant to be registered dynamically in combination with [PMIntentReceiver] in order to
 * relay parsed [PackageInstaller] results back to the running application.
 * Additionally, it will handle restarting [PackageInstaller] user action intents infinitely until
 * we have received a result from the system.
 */
class PMResultReceiver(
    private val sessionId: Int,
    private val isUninstall: Boolean,
    private val onResult: (InstallerResult) -> Unit,
) : BroadcastReceiver() {
    /**
     * Keep re-launching the user action intent until we have received a result from the PM.
     * This is necessary because on some ROMs there is no callback intent sent when the user
     * clicks outside of the package installer dialog, forcing us to keep relaunching the dialog
     * until the user has clicked one of the buttons, after which the intent has been 'used up' and won't show anymore.
     */
    private val resultReceived = AtomicBoolean(false)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getIntExtra(EXTRA_SESSION_ID, -1) != sessionId) return

        try {
            when (intent.action) {
                ACTION_RECEIVE_INTENT -> handleIntent(context, intent)
                ACTION_RECEIVE_RESULT -> handleResult(context, intent)
            }
        } catch (t: Throwable) {
            resultReceived.set(true)
            context.unregisterReceiver(this)
            onResult(UnknownInstallerError(t))
        }
    }

    private fun handleIntent(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        val confirmationIntent = intent.getParcelableExtra<Intent>(EXTRA_USER_ACTION_INTENT)
            ?: return

        // No result yet, keep relaunching user action intent
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                context.startActivity(confirmationIntent)
                delay(1000)

                if (resultReceived.get()) break
            }
        }
    }

    private fun handleResult(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        val result = intent.getParcelableExtra<InstallerResult>(EXTRA_RESULT) ?: return

        resultReceived.set(true)

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
        const val EXTRA_USER_ACTION_INTENT = "userActionIntent"

        /**
         * The intent filter this receiver should be registered with to work properly.
         */
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_RECEIVE_INTENT)
            addAction(ACTION_RECEIVE_RESULT)
        }
    }
}
