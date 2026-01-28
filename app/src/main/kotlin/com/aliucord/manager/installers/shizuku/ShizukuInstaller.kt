package com.aliucord.manager.installers.shizuku

import android.content.Context
import android.content.pm.*
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.installers.pm.PMUtils
import com.aliucord.manager.manager.ShizukuManager
import com.aliucord.manager.util.HiddenAPI
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.File
import kotlin.coroutines.resume

// Based on https://github.com/Tobi823/ffupdater/blob/9830452fe1cb3b77b28175833c68118a63d5ca69/ffupdater/src/main/java/de/marmaro/krt/ffupdater/installer/impl/ShizukuInstaller.kt

/**
 * The package name of Google Play Store.
 * We spoof our installer to this when installing through Shizuku to prevent
 * potentially unnecessary scans/checks.
 */
private const val PLAY_PACKAGE_NAME = "com.android.vending"

/**
 * Uses Shizuku to remotely invoke the [PackageInstaller] API from ADB.
 */
class ShizukuInstaller(
    private val context: Context,
    private val shizuku: ShizukuManager,
) : Installer {
    /**
     * Gets the Shizuku API binder for [IPackageInstaller].
     */
    private fun getPackageInstallerBinder(): IPackageInstaller {
        HiddenAPI.disable()

        val iPackageManager = IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        val iPackageInstaller = IPackageInstaller.Stub.asInterface(
            ShizukuBinderWrapper(iPackageManager.packageInstaller.asBinder())
        )

        return iPackageInstaller
    }

    /**
     * Opens and binds a [PackageInstaller.Session] wrapper through Shizuku.
     */
    fun openSession(sessionId: Int): PackageInstaller.Session {
        HiddenAPI.disable()

        val iPackageInstaller = getPackageInstallerBinder()
        val iSession = IPackageInstallerSession.Stub.asInterface(
            ShizukuBinderWrapper(iPackageInstaller.openSession(sessionId).asBinder())
        )
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }

    override suspend fun install(apks: List<File>, silent: Boolean) {
        if (!shizuku.requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        ShizukuSettingsWrapper.disableAdbVerify(context)

        // Construct install session and create it
        val params = PMUtils.createInstallSessionParams(silent = true)
        val packageInstaller = PMUtils.getPackageInstaller(
            context = context,
            iPackageInstaller = getPackageInstallerBinder(),
            installerPackageName = PLAY_PACKAGE_NAME,
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

    override suspend fun waitInstall(
        apks: List<File>,
        silent: Boolean,
        onProgressUpdate: Installer.ProgressListener?,
    ): InstallerResult {
        if (!shizuku.requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        ShizukuSettingsWrapper.disableAdbVerify(context)

        return suspendCancellableCoroutine { continuation ->
            // Construct install session and create it
            val params = PMUtils.createInstallSessionParams(silent = true)
            val packageInstaller = PMUtils.getPackageInstaller(
                context = context,
                iPackageInstaller = getPackageInstallerBinder(),
                installerPackageName = PLAY_PACKAGE_NAME,
            )
            val sessionId = packageInstaller.createSession(params)

            // Create and register a result receiver
            val relayReceiver = PMUtils.registerRelayReceiver(
                context = context,
                sessionId = sessionId,
                isUninstall = false,
                onResult = continuation::resume,
            )

            // Create and register a progress callback
            val sessionCallback = onProgressUpdate?.let { onProgressUpdate ->
                PMUtils.registerSessionCallback(
                    sessionId = sessionId,
                    packageInstaller = packageInstaller,
                    onProgressUpdate = onProgressUpdate,
                )
            }

            // Unregister PMResultReceiver when this coroutine finishes or errors
            // Explicitly cancel the install session if it did not finish.
            continuation.invokeOnCancellation {
                context.unregisterReceiver(relayReceiver)
                sessionCallback?.let { packageInstaller.unregisterSessionCallback(it) }
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
        if (!shizuku.requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        return suspendCancellableCoroutine { continuation ->
            val packageInstaller = PMUtils.getPackageInstaller(
                context = context,
                iPackageInstaller = getPackageInstallerBinder(),
                installerPackageName = PLAY_PACKAGE_NAME,
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
