package com.aliucord.manager.installers.pm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.util.isMiui
import java.io.File

/**
 * Shared functionality between all types of installers that directly interface with the [PackageInstaller] API.
 */
object PMUtils {
    /**
     * Creates install sessions params for [PackageInstaller].
     * @param silent If this is an update, then the update will occur without user interaction.
     */
    @Suppress("DEPRECATION")
    fun createInstallSessionParams(silent: Boolean): SessionParams {
        return SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            if (Build.VERSION.SDK_INT >= 24) setOriginatingUid(Process.myUid())
            if (Build.VERSION.SDK_INT >= 26) setInstallReason(PackageManager.INSTALL_REASON_USER)
            if (Build.VERSION.SDK_INT >= 30) setAutoRevokePermissionsMode(false)

            if (Build.VERSION.SDK_INT >= 31) {
                setInstallScenario(PackageManager.INSTALL_SCENARIO_FAST)

                // Allegedly MIUI is not happy with silent installs
                if (silent && !isMiui()) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            if (Build.VERSION.SDK_INT >= 34) setPackageSource(PackageInstaller.PACKAGE_SOURCE_OTHER)
        }
    }

    /**
     * Registers a [PMResultReceiver] to receive results relayed by [PMIntentReceiver], to return the state
     * back to the application.
     *
     * @param context Android context.
     * @param sessionId The [PackageInstaller] install session ID to filter for.
     * @param isUninstall Whether this operation is an uninstallation.
     * @param onResult A callback lambda providing the parsed installation result.
     * @return The receiver that was registered. This should be unregistered manually, such as
     * upon the cancellation of the registering coroutine.
     */
    fun registerRelayReceiver(
        context: Context,
        sessionId: Int,
        isUninstall: Boolean,
        onResult: (InstallerResult) -> Unit,
    ): PMResultReceiver {
        // This will receive parsed data forwarded by PMIntentReceiver
        val relayReceiver = PMResultReceiver(
            sessionId = sessionId,
            isUninstall = isUninstall,
            onResult = onResult,
        )

        ContextCompat.registerReceiver(
            /* context = */ context,
            /* receiver = */ relayReceiver,
            /* filter = */ PMResultReceiver.intentFilter,
            /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        return relayReceiver
    }

    // TODO: Install progress?
    //       PackageInstaller.Session#setStagingProgress
    //       PackageInstaller.SessionCallback#onProgressChanged(int, float)

    /**
     * Start a [PackageInstaller] session for installation.
     * @param context Android context
     * @param session A newly opened install session to be written to. Ones binded through Shizuku also work.
     * @param sessionId The install session ID of [session].
     * @param apks The apks to install
     * @param relay Whether to use the [PMResultReceiver] flow.
     */
    fun startInstall(
        context: Context,
        session: PackageInstaller.Session,
        sessionId: Int,
        apks: List<File>,
        relay: Boolean,
    ) {
        val callbackIntent = Intent(context, PMIntentReceiver::class.java)
            .putExtra(PMIntentReceiver.EXTRA_SESSION_ID, sessionId)
            .putExtra(PMIntentReceiver.EXTRA_RELAY_ENABLED, relay)

        val pendingIntent = PendingIntent.getBroadcast(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ callbackIntent,
            /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session.use { session ->
            val bufferSize = 1 * 1024 * 1024 // 1MiB

            for (apk in apks) {
                session.openWrite(apk.name, 0, apk.length()).use { outStream ->
                    apk.inputStream().use { it.copyTo(outStream, bufferSize) }
                    session.fsync(outStream)
                }
            }

            @SuppressLint("RequestInstallPackagesPolicy")
            session.commit(pendingIntent.intentSender)
        }
    }

    /**
     * Creates an uninstallation callback [PendingIntent] that will forward events
     * to the relaying [PMIntentReceiver]. These events can be captured by registering [PMResultReceiver]
     * through [PMUtils.registerRelayReceiver].
     */
    fun createUninstallRelayingIntent(context: Context): PendingIntent {
        // FIXME: Conflicting pending intents when multiple simultaneous uninstalls are happening.
        //        The extras will end up being merged into one pending intent, with only the newest one working.
        val callbackIntent = Intent(context, PMIntentReceiver::class.java)
            .putExtra(PMIntentReceiver.EXTRA_SESSION_ID, -1)
            .putExtra(PMIntentReceiver.EXTRA_RELAY_ENABLED, true)

        return PendingIntent.getBroadcast(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ callbackIntent,
            /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
