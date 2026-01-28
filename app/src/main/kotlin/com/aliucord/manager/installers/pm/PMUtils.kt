package com.aliucord.manager.installers.pm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.content.pm.PackageInstaller.SessionCallback
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageInstallerHidden.SessionParamsHidden
import android.os.*
import androidx.core.content.ContextCompat
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.util.*
import dev.rikka.tools.refine.Refine
import java.io.File

/**
 * Shared functionality between all types of installers that directly interface with the [PackageInstaller] API.
 */
object PMUtils {
    /**
     * Gets a binded [PackageInstaller] service wrapper.
     * This is used by remote installers such as Shizuku and Dhizuku.
     */
    fun getPackageInstaller(
        context: Context,
        iPackageInstaller: IPackageInstaller,
        installerPackageName: String,
    ): PackageInstaller {
        HiddenAPI.disable()

        val userId = context.getUserId() ?: 0

        val hiddenPackageInstaller = if (Build.VERSION.SDK_INT >= 31) {
            PackageInstallerHidden(
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ installerPackageName,
                /* installerAttributionTag = */ null,
                /* userId = */ userId,
            )
        } else if (Build.VERSION.SDK_INT >= 26) {
            PackageInstallerHidden(
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ installerPackageName,
                /* userId = */ userId,
            )
        } else {
            PackageInstallerHidden(
                /* context = */ context,
                /* pm = */ context.packageManager,
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ installerPackageName,
                /* userId = */ userId,
            )
        }
        return Refine.unsafeCast(hiddenPackageInstaller)
    }

    /**
     * Creates install sessions params for [PackageInstaller].
     * @param silent If this is an update, then the update will occur without user interaction.
     */
    @Suppress("DEPRECATION")
    fun createInstallSessionParams(silent: Boolean): SessionParams {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
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

        val hiddenParams = Refine.unsafeCast<SessionParamsHidden>(params)
        HiddenAPI.disable()
        hiddenParams.installFlags = hiddenParams.installFlags or
            PackageManagerHidden.INSTALL_REPLACE_EXISTING or
            PackageManagerHidden.INSTALL_ALLOW_TEST or
            (if (Build.VERSION.SDK_INT >= 34) PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK else 0)

        return Refine.unsafeCast<SessionParams>(hiddenParams)
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

    /**
     * Creates a session callback listener that is then registered to receive
     * installation progress updates from the system.
     */
    fun registerSessionCallback(
        sessionId: Int,
        packageInstaller: PackageInstaller,
        onProgressUpdate: Installer.ProgressListener,
    ): SessionCallback {
        val callback = object : SessionCallback() {
            override fun onActiveChanged(callbackSessionId: Int, active: Boolean) {}
            override fun onBadgingChanged(callbackSessionId: Int) {}
            override fun onCreated(callbackSessionId: Int) {}
            override fun onFinished(callbackSessionId: Int, success: Boolean) {}

            override fun onProgressChanged(callbackSessionId: Int, progress: Float) {
                if (sessionId != callbackSessionId) return

                onProgressUpdate.onUpdate(progress)
            }
        }

        // Register callback to receive invocations on main thread
        packageInstaller.registerSessionCallback(callback, Handler(Looper.getMainLooper()))

        return callback
    }

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

            for (apkIdx in 0..apks.lastIndex) {
                val apk = apks[apkIdx]
                val apkSize = apk.length()
                val filesProgress = (apkIdx + 1f) / apks.size

                session.openWrite(apk.name, 0, apkSize).use { out ->
                    apk.inputStream().use { input ->
                        val buffer = ByteArray(bufferSize)
                        var bytesCopied: Long = 0
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            out.write(buffer, 0, bytes)
                            bytesCopied += bytes

                            val apkProgress = bytes.toFloat() / apkSize
                            session.setStagingProgress(apkProgress * filesProgress)

                            bytes = input.read(buffer)
                        }
                    }
                    session.fsync(out)
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
