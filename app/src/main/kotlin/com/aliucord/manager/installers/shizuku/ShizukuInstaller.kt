package com.aliucord.manager.installers.shizuku

import android.content.Context
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageInstallerHidden.SessionParamsHidden
import android.content.pm.PackageManagerHidden
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.installers.pm.PMUtils
import com.aliucord.manager.manager.ShizukuManager
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

// Based on https://github.com/Tobi823/ffupdater/blob/9830452fe1cb3b77b28175833c68118a63d5ca69/ffupdater/src/main/java/de/marmaro/krt/ffupdater/installer/impl/ShizukuInstaller.kt

class ShizukuInstaller(
    private val context: Context,
    private val shizuku: ShizukuManager,
) : Installer {
    /**
     * Creates the [PackageInstaller] session params to be used with Shizuku installs.
     */
    private fun createSessionParams(): SessionParams {
        val params = PMUtils.createInstallSessionParams(silent = true)

        // Add INSTALL_REPLACE_EXISTING flag to session params
        val paramsHidden = Refine.unsafeCast<SessionParamsHidden>(params)
        paramsHidden.installFlags = paramsHidden.installFlags or PackageManagerHidden.INSTALL_REPLACE_EXISTING

        return params
    }

    override suspend fun install(apks: List<File>, silent: Boolean) {
        if (!shizuku.requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        ShizukuSettingsWrapper.disableAdbVerify(context)

        // Construct install session and create it
        val params = createSessionParams()
        val packageInstaller = ShizukuPMWrapper.getPackageInstaller(context)
        val sessionId = packageInstaller.createSession(params)

        PMUtils.startInstall(
            context = context,
            session = ShizukuPMWrapper.openSession(sessionId),
            sessionId = sessionId,
            apks = apks,
            relay = false,
        )
    }

    override suspend fun waitInstall(apks: List<File>, silent: Boolean): InstallerResult {
        if (!shizuku.requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        ShizukuSettingsWrapper.disableAdbVerify(context)

        return suspendCancellableCoroutine { continuation ->
            // Construct install session and create it
            val params = createSessionParams()
            val packageInstaller = ShizukuPMWrapper.getPackageInstaller(context)
            val sessionId = packageInstaller.createSession(params)

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
                packageInstaller.abandonSession(sessionId)
            }

            PMUtils.startInstall(
                context = context,
                session = ShizukuPMWrapper.openSession(sessionId),
                sessionId = sessionId,
                apks = apks,
                relay = true,
            )
        }
    }

    override suspend fun waitUninstall(packageName: String): InstallerResult {
        if (!shizuku.requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        return suspendCancellableCoroutine { continuation ->
            val packageInstaller = ShizukuPMWrapper.getPackageInstaller(context)

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

            packageInstaller.uninstall(
                /* packageName = */ packageName,
                /* statusReceiver = */ PMUtils.createUninstallRelayingIntent(context).intentSender,
            )
        }
    }
}
