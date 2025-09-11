package com.aliucord.manager.installers.shizuku

import android.content.Context
import android.content.pm.*
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageInstallerHidden.SessionParamsHidden
import android.os.Build
import com.aliucord.manager.R
import com.aliucord.manager.installers.Installer
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.installers.pm.PMUtils
import com.aliucord.manager.util.showToast
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.random.Random

// Based on https://github.com/Tobi823/ffupdater/blob/9830452fe1cb3b77b28175833c68118a63d5ca69/ffupdater/src/main/java/de/marmaro/krt/ffupdater/installer/impl/ShizukuInstaller.kt

class ShizukuInstaller(private val context: Context) : Installer {
    private var shizukuPermissionLock = Mutex()
    private val shizukuAvailable: StateFlow<Boolean?>
        field = MutableStateFlow(null)

    // Listeners
    private val onBinderReceived = Shizuku.OnBinderReceivedListener {
        shizukuAvailable.value = true
    }
    private val onBinderDead = Shizuku.OnBinderDeadListener {
        shizukuAvailable.value = false

        if (shizukuPermissionLock.isLocked)
            shizukuPermissionLock.unlock()
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
        Shizuku.addBinderDeadListener(onBinderDead)
    }

    fun stop() {
        Shizuku.removeBinderReceivedListener(onBinderReceived)
        Shizuku.removeBinderDeadListener(onBinderDead)
    }

    /**
     * Determines whether Shizuku is available and the binder has been retrieved.
     */
    fun shizukuAvailable(): Boolean = when (shizukuAvailable.value) {
        true -> true
        false -> false
        null -> {
            Shizuku.pingBinder().also { shizukuAvailable.value = it }
        }
    }

    /**
     * Checks whether Shizuku permissions have been granted to this app.
     */
    fun checkPermissions(): Boolean {
        if (!shizukuAvailable()) return false

        // Old shizuku does not have permission checks
        if (Shizuku.isPreV11()) return true

        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests and waits for Shizuku permissions if they have not already been granted.
     */
    suspend fun requestPermissions(): Boolean {
        if (!shizukuAvailable()) return false

        // Lock and check if the previous holder already obtained permissions
        shizukuPermissionLock.lock()
        try {
            if (checkPermissions()) {
                shizukuPermissionLock.unlock()
                return true
            }
        } catch (_: Exception) {
            shizukuPermissionLock.unlock()
        }

        return suspendCancellableCoroutine { continuation ->
            val currentRequestCode = Random.nextInt()
            val onPermissionRequestResult = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode != currentRequestCode)
                    return@OnRequestPermissionResultListener

                if (grantResult == PackageManager.PERMISSION_DENIED)
                    context.showToast(R.string.permissions_shizuku_denied)

                continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
            }

            continuation.invokeOnCancellation {
                Shizuku.removeRequestPermissionResultListener(onPermissionRequestResult)
                shizukuPermissionLock.unlock()
            }

            Shizuku.addRequestPermissionResultListener(onPermissionRequestResult)
            Shizuku.requestPermission(currentRequestCode)
        }
    }

    /**
     * Gets the Shizuku binder for [IPackageInstaller].
     */
    private fun getPackageInstallerBinder(): IPackageInstaller {
        val iPackageManager = IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        val iPackageInstaller = IPackageInstaller.Stub.asInterface(
            ShizukuBinderWrapper(iPackageManager.packageInstaller.asBinder())
        )

        return iPackageInstaller
    }

    /**
     * Gets a binded [PackageInstaller] service wrapper through Shizuku.
     */
    private fun getPackageInstaller(): PackageInstaller {
        val iPackageInstaller = getPackageInstallerBinder()

        val hiddenPackageInstaller = if (Build.VERSION.SDK_INT >= 31) {
            PackageInstallerHidden(
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* installerAttributionTag = */ null,
                /* userId = */ 0,
            )
        } else if (Build.VERSION.SDK_INT >= 26) {
            PackageInstallerHidden(
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* userId = */ 0,
            )
        } else {
            PackageInstallerHidden(
                /* context = */ context,
                /* pm = */ context.packageManager,
                /* installer = */ iPackageInstaller,
                /* installerPackageName = */ PLAY_PACKAGE_NAME,
                /* userId = */ 0,
            )
        }
        return Refine.unsafeCast(hiddenPackageInstaller)
    }

    /**
     * Opens and binds a [PackageInstaller.Session] wrapper through Shizuku.
     */
    private fun openSession(sessionId: Int): PackageInstaller.Session {
        val iPackageInstaller = getPackageInstallerBinder()
        val iSession = IPackageInstallerSession.Stub.asInterface(
            ShizukuBinderWrapper(iPackageInstaller.openSession(sessionId).asBinder())
        )
        return Refine.unsafeCast(PackageInstallerHidden.SessionHidden(iSession))
    }

    /**
     * Creates the [PackageInstaller] session params to be used with Shizuku installs.
     */
    private fun createSessionParams(): SessionParams {
        // Required to change hidden SessionParams flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !hiddenApiDisabled.getAndSet(true))
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content", "Landroid/os")

        val params = PMUtils.createInstallSessionParams(silent = true)

        // Add INSTALL_REPLACE_EXISTING flag to session params
        val paramsHidden = Refine.unsafeCast<SessionParamsHidden>(params)
        paramsHidden.installFlags = paramsHidden.installFlags or PackageManagerHidden.INSTALL_REPLACE_EXISTING

        return params
    }

    override suspend fun install(apks: List<File>, silent: Boolean) {
        if (!requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        // Construct install session and create it
        val params = createSessionParams()
        val packageInstaller = getPackageInstaller()
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
        if (!requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        return suspendCancellableCoroutine { continuation ->
            // Construct install session and create it
            val params = createSessionParams()
            val packageInstaller = getPackageInstaller()
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
        if (!requestPermissions())
            throw IllegalStateException("Shizuku is not available!")

        return suspendCancellableCoroutine { continuation ->
            val packageInstaller = getPackageInstaller()

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

    private companion object {
        // We spoof Google Play Store to prevent unnecessary checks
        const val PLAY_PACKAGE_NAME = "com.android.vending"

        /**
         * Whether the hidden API exemptions have already been added for
         * use in modifying hidden install session flags.
         */
        var hiddenApiDisabled = AtomicBoolean(false)
    }
}
