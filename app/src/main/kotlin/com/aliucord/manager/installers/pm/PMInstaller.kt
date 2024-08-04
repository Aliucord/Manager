package com.aliucord.manager.installers.pm

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.*
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.util.isMiui
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * APK installer using the [PackageInstaller] from the system's [PackageManager] service.
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

    override fun install(apks: List<File>, silent: Boolean) {
        startInstall(createInstallSession(silent), apks, false)
    }

    override suspend fun waitInstall(apks: List<File>, silent: Boolean) = suspendCancellableCoroutine { continuation ->
        // Create a new install session
        val sessionId = createInstallSession(silent)

        // This will receive parsed data forwarded by PMIntentReceiver
        val relayReceiver = PMResultReceiver(
            sessionId = sessionId,
            isUninstall = false,
            onResult = { continuation.resume(it) },
        )

        ContextCompat.registerReceiver(
            /* context = */ context,
            /* receiver = */ relayReceiver,
            /* filter = */ PMResultReceiver.intentFilter,
            /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Unregister PMResultReceiver when this coroutine finishes or error occurs
        // Additionally, cancel the install session entirely
        continuation.invokeOnCancellation { error ->
            context.unregisterReceiver(relayReceiver)
            context.packageManager.packageInstaller.abandonSession(sessionId)
        }

        startInstall(sessionId, apks, relay = true)
    }

    override suspend fun waitUninstall(packageName: String) = suspendCancellableCoroutine { continuation ->
        val callbackIntent = Intent(context, PMIntentReceiver::class.java)
            .putExtra(PMIntentReceiver.EXTRA_SESSION_ID, -1)
            .putExtra(PMIntentReceiver.EXTRA_RELAY_ENABLED, true)

        val pendingIntent = PendingIntent.getBroadcast(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ callbackIntent,
            /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // This will receive parsed data forwarded by PMIntentReceiver
        val relayReceiver = PMResultReceiver(
            sessionId = -1,
            isUninstall = true,
            onResult = { continuation.resume(it) },
        )

        ContextCompat.registerReceiver(
            /* context = */ context,
            /* receiver = */ relayReceiver,
            /* filter = */ PMResultReceiver.intentFilter,
            /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Unregister PMResultReceiver when this coroutine is cancelled early
        continuation.invokeOnCancellation { error ->
            context.unregisterReceiver(relayReceiver)
        }

        _packageInstaller.uninstall(packageName, pendingIntent.intentSender)
    }

    /**
     * Starts a [PackageInstaller] session with the necessary params.
     * @param silent If this is an update, then the update will occur without user interaction.
     * @return The open install session id.
     */
    private fun createInstallSession(silent: Boolean): Int {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)

            if (Build.VERSION.SDK_INT >= 24) setOriginatingUid(Process.myUid())
            if (Build.VERSION.SDK_INT >= 26) setInstallReason(PackageManager.INSTALL_REASON_USER)

            if (Build.VERSION.SDK_INT >= 31) {
                setInstallScenario(PackageManager.INSTALL_SCENARIO_FAST)

                // Allegedly MIUI is not happy with silent installs
                if (silent && !isMiui()) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            if (Build.VERSION.SDK_INT >= 34) setPackageSource(PackageInstaller.PACKAGE_SOURCE_OTHER)
        }

        return context.packageManager.packageInstaller.createSession(params)
    }

    /**
     * Start a [PackageInstaller] session for installation.
     * @param apks The apks to install
     * @param relay Whether to use the [PMResultReceiver] flow.
     * @return The [PendingIntent] that was registered to the [PackageInstaller.Session]
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

        _packageInstaller.openSession(sessionId).use { session ->
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
