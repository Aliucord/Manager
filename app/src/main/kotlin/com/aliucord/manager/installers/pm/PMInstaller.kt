package com.aliucord.manager.installers.pm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import android.os.Process
import android.util.Log
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.InstallerResult
import kotlinx.coroutines.suspendCancellableCoroutine
import com.aliucord.manager.util.isMiui
import java.io.File

/**
 * APK installer using the [PackageInstaller] from the system's [PackageManager] service.
 */
class PMInstaller(
    val context: Context,
) : Installer {
    init {
        val pkgInstaller = context.packageManager.packageInstaller

        // Destroy all open sessions that may have not been previously cleaned up
        for (session in pkgInstaller.mySessions) {
            Log.d(BuildConfig.TAG, "Deleting PackageInstaller session ${session.sessionId}")
            pkgInstaller.abandonSession(session.sessionId)
        }
    }

    override fun install(apks: List<File>, silent: Boolean) {
        startInstall(createInstallSession(silent), apks, false)
    }

    override suspend fun waitInstall(apks: List<File>, silent: Boolean): InstallerResult {
        val sessionId = createInstallSession(silent)

        return suspendCancellableCoroutine { continuation ->
            // This will receive parsed data forwarded by PMIntentReceiver
            val relayReceiver = PMResultReceiver(sessionId, continuation)

            // Unregister PMResultReceiver when this coroutine finishes
            // additionally, cancel the install session entirely
            continuation.invokeOnCancellation {
                context.unregisterReceiver(relayReceiver)
                context.packageManager.packageInstaller.abandonSession(sessionId)
            }

            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(relayReceiver, relayReceiver.filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(relayReceiver, relayReceiver.filter)
            }

            startInstall(sessionId, apks, relay = true)
        }
    }

    /**
     * Starts a [PackageInstaller] session with the necessary params.
     * @param silent If this is an update, then the update will occur without user interaction.
     * @return The open install session id.
     */
    private fun createInstallSession(silent: Boolean): Int {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)

            if (Build.VERSION.SDK_INT >= 24) {
                setOriginatingUid(Process.myUid())
            }

            if (Build.VERSION.SDK_INT >= 26) {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
            }

            if (Build.VERSION.SDK_INT >= 31) {
                setInstallScenario(PackageManager.INSTALL_SCENARIO_FAST)

                // Allegedly MIUI is not happy with silent installs
                if (silent && !isMiui()) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            if (Build.VERSION.SDK_INT >= 34) {
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_OTHER)
            }
        }

        return context.packageManager.packageInstaller.createSession(params)
    }

    /**
     * Start a [PackageInstaller] session for installation.
     * @param apks The apks to install
     * @param relay Whether to use the [PMResultReceiver] flow.
     */
    private fun startInstall(sessionId: Int, apks: List<File>, relay: Boolean) {
        val callbackIntent = Intent(context, PMIntentReceiver::class.java)
            .putExtra(PMIntentReceiver.EXTRA_SESSION_ID, sessionId)
            .putExtra(PMIntentReceiver.EXTRA_RELAY_ENABLED, relay)

        val pendingIntent = PendingIntent.getBroadcast(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ callbackIntent,
            /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        context.packageManager.packageInstaller.openSession(sessionId).use { session ->
            val bufferSize = 1 * 1024 * 1024 // 1MiB

            for (apk in apks) {
                session.openWrite(apk.name, 0, apk.length()).use { outStream ->
                    apk.inputStream().use { it.copyTo(outStream, bufferSize) }
                    session.fsync(outStream)
                }
            }

            session.commit(pendingIntent.intentSender)
        }
    }
}
