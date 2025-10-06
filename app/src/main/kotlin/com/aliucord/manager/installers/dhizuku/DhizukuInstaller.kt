package com.aliucord.manager.installers.dhizuku

import android.content.Context
import android.content.pm.*
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.installers.pm.PMUtils
import com.aliucord.manager.manager.DhizukuManager
import com.aliucord.manager.util.HiddenAPI
import com.rosan.dhizuku.api.Dhizuku
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.SystemServiceHelper
import java.io.File
import kotlin.coroutines.resume

/**
 * Uses Dhizuku to remotely invoke the [PackageInstaller] API using device owner.
 */
class DhizukuInstaller(
    private val context: Context,
    private val dhizuku: DhizukuManager,
) : Installer {
    /**
     * Gets the Dhizuku API binder for [IPackageInstaller].
     */
    private fun getPackageInstallerBinder(): IPackageInstaller {
        HiddenAPI.disable()

        val iPackageManager = IPackageManager.Stub.asInterface(
            Dhizuku.binderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        val iPackageInstaller = IPackageInstaller.Stub.asInterface(
            Dhizuku.binderWrapper(iPackageManager.packageInstaller.asBinder())
        )

        return iPackageInstaller
    }

    /**
     * Opens and binds a [PackageInstaller.Session] wrapper through Dhizuku.
     */
    fun openSession(sessionId: Int): PackageInstaller.Session {
        HiddenAPI.disable()

        val iPackageInstaller = getPackageInstallerBinder()
        val iSession = IPackageInstallerSession.Stub.asInterface(
            Dhizuku.binderWrapper(iPackageInstaller.openSession(sessionId).asBinder())
        )
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }

    override suspend fun install(apks: List<File>, silent: Boolean) {
        if (!dhizuku.requestPermissions())
            throw IllegalStateException("Dhizuku is not available!")

        // Construct install session and create it
        val params = PMUtils.createInstallSessionParams(silent = true)
        val packageInstaller = PMUtils.getPackageInstaller(
            context = context,
            iPackageInstaller = getPackageInstallerBinder(),
            installerPackageName = Dhizuku.getOwnerPackageName(),
        )
        val sessionId = packageInstaller.createSession(params)

        PMUtils.startInstall(
            context = context,
            session = openSession(sessionId),
            sessionId = sessionId,
            apks = apks,
            relay = false,
        )
    }

    override suspend fun waitInstall(apks: List<File>, silent: Boolean): InstallerResult {
        if (!dhizuku.requestPermissions())
            throw IllegalStateException("Dhizuku is not available!")

        return suspendCancellableCoroutine { continuation ->
            // Construct install session and create it
            val params = PMUtils.createInstallSessionParams(silent = true)
            val packageInstaller = PMUtils.getPackageInstaller(
                context = context,
                iPackageInstaller = getPackageInstallerBinder(),
                installerPackageName = Dhizuku.getOwnerPackageName(),
            )
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
                session = openSession(sessionId),
                sessionId = sessionId,
                apks = apks,
                relay = true,
            )
        }
    }

    override suspend fun waitUninstall(packageName: String): InstallerResult {
        if (!dhizuku.requestPermissions())
            throw IllegalStateException("Dhizuku is not available!")

        return suspendCancellableCoroutine { continuation ->
            val packageInstaller = PMUtils.getPackageInstaller(
                context = context,
                iPackageInstaller = getPackageInstallerBinder(),
                installerPackageName = Dhizuku.getOwnerPackageName(),
            )

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
