package com.aliucord.manager.patcher.steps.prepare

import android.app.Application
import android.content.pm.PackageManager.NameNotFoundException
import com.aliucord.manager.R
import com.aliucord.manager.installers.InstallerResult
import com.aliucord.manager.manager.InstallerManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.base.StepState
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.aliucord.manager.util.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Prompt the user to uninstall a previous version of Aliucord if it has a larger version code.
 * (Prevent conflicts from downgrading)
 */
class DowngradeCheckStep(private val options: PatchOptions) : Step(), KoinComponent {
    private val context: Application by inject()
    private val installers: InstallerManager by inject()

    override val group = StepGroup.Prepare
    override val localizedName = R.string.patch_step_downgrade_check

    override suspend fun execute(container: StepRunner) {
        container.log("Fetching version of package ${options.packageName}")
        val (_, currentVersion) = try {
            context.getPackageVersion(options.packageName)
        }
        // Package is not installed
        catch (_: NameNotFoundException) {
            state = StepState.Skipped
            container.log("Package not uninstalled, skipping check")
            return
        }
        container.log("Version of installed Discord app: $currentVersion")

        val targetVersion = container
            .getStep<FetchInfoStep>()
            .data.discordVersionCode

        container.log("Target discord version: $targetVersion")

        if (currentVersion > targetVersion) {
            container.log("Current installed version is greater than target, forcing uninstallation")
            mainThread { context.showToast(R.string.installer_uninstall_new) }

            when (val result = installers.getActiveInstaller().waitUninstall(options.packageName)) {
                is InstallerResult.Error -> throw Error("Failed to uninstall Aliucord: ${result.getDebugReason()}")
                is InstallerResult.Cancelled -> {
                    mainThread { context.showToast(R.string.installer_uninstall_new) }
                    throw Error("Newer versions of Aliucord must be uninstalled prior to installing an older version")
                }

                else -> {}
            }
        }
    }
}
