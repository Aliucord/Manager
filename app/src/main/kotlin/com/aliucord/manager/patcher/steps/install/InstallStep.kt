package com.aliucord.manager.patcher.steps.install

import android.content.Context
import androidx.lifecycle.*
import com.aliucord.manager.R
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.manager.*
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.ui.components.dialogs.PlayProtectDialog
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.ui.util.InstallNotifications
import com.aliucord.manager.util.isPackageInstalled
import com.aliucord.manager.util.isPlayProtectEnabled
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ID used for showing ready notifications if the activity is currently minimized when having reached this step.
 */
private const val READY_NOTIF_ID = 200001

/**
 * Install the final APK with the system's PackageManager.
 */
class InstallStep(private val options: PatchOptions) : Step(), KoinComponent {
    private val context: Context by inject()
    private val installers: InstallerManager by inject()
    private val prefs: PreferencesManager by inject()
    private val overlays: OverlayManager by inject()

    override val group = StepGroup.Install
    override val localizedName = R.string.patch_step_install

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        // If app backgrounded, show notification
        if (ProcessLifecycleOwner.get().lifecycle.currentState == Lifecycle.State.CREATED) {
            InstallNotifications.createNotification(
                context = context,
                id = READY_NOTIF_ID,
                title = R.string.notif_install_ready_title,
                description = R.string.notif_install_ready_desc,
            )

            container.log("Waiting until manager is resumed to continue installation")
        }

        // Wait until app resumed
        ProcessLifecycleOwner.get().lifecycle.withResumed {}

        // Show [PlayProtectDialog] and wait until it gets dismissed
        if (!prefs.devMode && !context.isPackageInstalled(options.packageName) && context.isPlayProtectEnabled() == true) {
            container.log("Showing play protect warning dialog")
            overlays.startComposableForResult { callback ->
                PlayProtectDialog(onDismiss = { callback(Unit) })
            }
        }

        container.log("Installing ${apk.absolutePath}, silent: ${!prefs.devMode}")
        val result = installers.getActiveInstaller().waitInstall(
            apks = listOf(apk),
            silent = !prefs.devMode,
        )

        when (result) {
            is InstallerResult.Error -> {
                container.log("Installation failed")
                throw Error("Failed to install APKs: ${result.getDebugReason()}")
            }

            is InstallerResult.Cancelled -> {
                // The install screen is automatically closed immediately once cleanup finishes
                state = StepState.Skipped
                container.log("Installation was cancelled by user")
            }

            InstallerResult.Success ->
                container.log("Installation successful")
        }
    }
}
