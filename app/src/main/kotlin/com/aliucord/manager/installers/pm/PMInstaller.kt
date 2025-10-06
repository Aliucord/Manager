package com.aliucord.manager.installers.pm

import android.app.Application
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.util.Log
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.installers.Installer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Uses the [PackageInstaller] API from the system's [PackageManager] service.
 * This installer invokes the API directly from this app's context.
 */
class PMInstaller(
    private val context: Application,
) : Installer {
    private val _packageInstaller = context.packageManager.packageInstaller

    init {
        // Destroy all open sessions that may have not been previously cleaned up due to fatal errors
        for (session in _packageInstaller.mySessions) {
            Log.d(BuildConfig.TAG, "Deleting old PackageInstaller session ${session.sessionId}")
            _packageInstaller.abandonSession(session.sessionId)
        }
    }

    override suspend fun install(apks: List<File>, silent: Boolean) {
        val sessionId = createInstallSession(silent)

        PMUtils.startInstall(
            context = context,
            session = _packageInstaller.openSession(sessionId),
            sessionId = sessionId,
            apks = apks,
            relay = false,
        )
    }

    override suspend fun waitInstall(apks: List<File>, silent: Boolean) = suspendCancellableCoroutine { continuation ->
        // Create a new install session
        val sessionId = createInstallSession(silent)

        // Create and register a result receiver
        val relayReceiver = PMUtils.registerRelayReceiver(
            context = context,
            sessionId = sessionId,
            isUninstall = false,
            onResult = continuation::resume,
        )

        // Unregister PMResultReceiver when this coroutine finishes or errors
        // Explicitly cancel the install session if it did not finish.
        continuation.invokeOnCancellation {
            context.unregisterReceiver(relayReceiver)
            _packageInstaller.abandonSession(sessionId)
        }

        PMUtils.startInstall(
            context = context,
            session = _packageInstaller.openSession(sessionId),
            sessionId = sessionId,
            apks = apks,
            relay = true,
        )
    }

    override suspend fun waitUninstall(packageName: String) = suspendCancellableCoroutine { continuation ->
        // Create and register a result receiver
        val relayReceiver = PMUtils.registerRelayReceiver(
            context = context,
            sessionId = -1,
            isUninstall = true,
            onResult = continuation::resume,
        )

        // Unregister PMResultReceiver when this coroutine finishes or errors
        continuation.invokeOnCancellation {
            context.unregisterReceiver(relayReceiver)
        }

        _packageInstaller.uninstall(
            /* packageName = */ packageName,
            /* statusReceiver = */ PMUtils.createUninstallRelayingIntent(context).intentSender,
        )
    }

    /**
     * Starts a [PackageInstaller] session with the necessary params.
     * @param silent If this is an update, then the update will occur without user interaction.
     * @return The open install session id.
     */
    private fun createInstallSession(silent: Boolean): Int {
        return _packageInstaller.createSession(PMUtils.createInstallSessionParams(silent = silent))
    }
}
