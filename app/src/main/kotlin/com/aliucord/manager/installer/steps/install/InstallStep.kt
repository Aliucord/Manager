package com.aliucord.manager.installer.steps.install

import android.app.Application
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepContainer
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.patch.CopyDependenciesStep
import com.aliucord.manager.installer.util.installApks
import com.aliucord.manager.manager.PreferencesManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Install the final APK with the system's PackageManager.
 */
class InstallStep : Step(), KoinComponent {
    private val application: Application by inject()
    private val prefs: PreferencesManager by inject()

    override val group = StepGroup.Install
    override val localizedName = R.string.install_step_installing

    override suspend fun execute(container: StepContainer) {
        val apk = container.getCompletedStep<CopyDependenciesStep>().patchedApk

        application.installApks(
            silent = !prefs.devMode,
            apks = arrayOf(apk),
        )
    }
}
