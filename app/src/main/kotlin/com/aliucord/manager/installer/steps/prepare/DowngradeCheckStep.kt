package com.aliucord.manager.installer.steps.prepare

import android.content.Context
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.base.StepState
import com.aliucord.manager.installer.util.uninstallApk
import com.aliucord.manager.manager.PreferencesManager
import com.aliucord.manager.util.getPackageVersion
import com.aliucord.manager.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Prompt the user to uninstall a previous version of Aliucord if it has a larger version code.
 * (Prevent conflicts from downgrading)
 */
class DowngradeCheckStep : Step(), KoinComponent {
    private val context: Context by inject()
    private val prefs: PreferencesManager by inject()

    override val group = StepGroup.Prepare
    override val localizedName = R.string.install_step_downgrade_check

    override suspend fun execute(container: StepContainer) {
        val (_, currentVersion) = try {
            context.getPackageVersion(prefs.packageName)
        } catch (_: Throwable) {
            // Package is not installed
            return
        }

        val targetVersion = container
            .getCompletedStep<FetchInfoStep>()
            .data.versionCode.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid fetched Aliucord target Discord version")

        if (currentVersion > targetVersion) {
            context.uninstallApk(prefs.packageName)

            withContext(Dispatchers.Main) {
                context.showToast(R.string.installer_uninstall_new)
            }

            throw Error("Newer version of Aliucord must be uninstalled prior to installing an older version")
        } else {
            state = StepState.Skipped
        }
    }
}
