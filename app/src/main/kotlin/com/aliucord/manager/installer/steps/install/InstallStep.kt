package com.aliucord.manager.installer.steps.install

import android.content.Context
import androidx.lifecycle.*
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.StepRunner
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
import com.aliucord.manager.installer.steps.download.CopyDependenciesStep
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.manager.InstallerManager
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.ui.components.dialogs.PlayProtectDialog
import com.aliucord.manager.ui.screens.installopts.InstallOptions
import com.aliucord.manager.ui.util.InstallNotifications
import com.aliucord.manager.util.isPackageInstalled
import com.aliucord.manager.util.isPlayProtectEnabled
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val READY_NOTIF_ID = 200001

/**
 * Install the final APK with the system's PackageManager.
 */
class InstallStep(private val options: InstallOptions) : Step(), KoinComponent {
    private val context: Context by inject()
    private val installers: InstallerManager by inject()
    private val prefs: PreferencesManager by inject()

    /**
     * Locks and unlocks the step's execution to start and wait until [PlayProtectDialog] dismissed.
     */
    private val _gppWarningLock = MutableSharedFlow<Boolean>()
    val gppWarningLock: Flow<Boolean>
        get() = _gppWarningLock

    override val group = StepGroup.Install
    override val localizedName = R.string.install_step_installing

    suspend fun dismissGPPWarning() {
        _gppWarningLock.emit(false)
    }

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
        }

        // Wait until app resumed
        ProcessLifecycleOwner.get().lifecycle.withResumed {}

        // Show [PlayProtectDialog] and wait until it gets dismissed
        if (!context.isPackageInstalled(options.packageName) && context.isPlayProtectEnabled() == true) {
            _gppWarningLock.emit(true)
            _gppWarningLock
                .filter { show -> !show }
                .take(1)
                .collect()
        }

        val result = installers.getActiveInstaller().waitInstall(
            apks = listOf(apk),
            silent = !prefs.devMode,
        )

        when (result) {
            is InstallerResult.Error -> throw Error("Failed to install APKs: ${result.debugReason}")
            is InstallerResult.Cancelled -> {
                // The install screen is automatically closed immediately once cleanup finishes
                state = StepState.Skipped
            }

            else -> {}
        }
    }
}
