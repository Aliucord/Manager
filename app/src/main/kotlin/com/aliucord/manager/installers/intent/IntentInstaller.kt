package com.aliucord.manager.installers.intent

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.di.ActivityProvider
import com.aliucord.manager.installers.*
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Launches an (un)installation intent to invoke the system's default app installer.
 * This defaults to the package installer in AOSP, however custom installers such as InstallerX
 * can intercept install intents if configured to do so.
 */
class IntentInstaller(
    private val context: Context,
    private val activities: ActivityProvider,
) : Installer {
    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun install(apks: List<File>, silent: Boolean) {
        coroutineScope.launch { waitInstall(apks, silent) }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("RequestInstallPackagesPolicy")
    override suspend fun waitInstall(apks: List<File>, silent: Boolean): InstallerResult {
        val file = apks.singleOrNull()
            ?: throw IllegalArgumentException("IntentInstaller only supports installing a single APK")
        val fileUri = if (Build.VERSION.SDK_INT >= 24) {
            FileProvider.getUriForFile(
                /* context = */ context,
                /* authority = */ "${BuildConfig.APPLICATION_ID}.provider",
                /* file = */ file,
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE, fileUri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            .putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, BuildConfig.APPLICATION_ID)

        val resultCode = try {
            launchForResultCode(intent)
        } catch (_: ActivityNotFoundException) {
            return UnsupportedIntentInstallerError(Intent.ACTION_INSTALL_PACKAGE)
        }

        return parseResultCode(resultCode)
    }

    @Suppress("DEPRECATION")
    override suspend fun waitUninstall(packageName: String): InstallerResult {
        // Ignore if the package does not exist
        try {
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return InstallerResult.Success
        }

        val uri = Uri.fromParts("package", packageName, null)
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)

        val resultCode = try {
            launchForResultCode(intent)
        } catch (_: ActivityNotFoundException) {
            return UnsupportedIntentInstallerError(Intent.ACTION_UNINSTALL_PACKAGE)
        }

        return parseResultCode(resultCode)
    }

    /**
     * Parses the result code of an intent that was launched with [Intent.EXTRA_RETURN_RESULT]
     * for package installer operations.
     */
    private fun parseResultCode(resultCode: Int): InstallerResult {
        return when (resultCode) {
            AppCompatActivity.RESULT_OK -> InstallerResult.Success
            AppCompatActivity.RESULT_CANCELED -> InstallerResult.Cancelled(systemTriggered = true)
            AppCompatActivity.RESULT_FIRST_USER -> // This is returned on errors
                UnknownInstallerError(IllegalStateException("External installer failed!"))

            else -> UnknownInstallerError(IllegalStateException("External installer returned unknown result code $resultCode"))
        }
    }

    /**
     * Launches an intent activity and captures it's result code.
     */
    private suspend fun launchForResultCode(intent: Intent): Int {
        val activity = activities.get<ComponentActivity>()

        return suspendCancellableCoroutine { continuation ->
            val launcher = activity.activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.StartActivityForResult(),
                callback = { continuation.resume(it.resultCode) },
            )

            continuation.invokeOnCancellation {
                launcher.unregister()
            }

            launcher.launch(intent)
        }
    }
}
