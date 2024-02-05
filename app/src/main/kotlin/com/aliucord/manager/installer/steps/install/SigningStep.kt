package com.aliucord.manager.installer.steps.install

import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepGroup
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.base.Step
import com.aliucord.manager.installer.steps.patch.CopyDependenciesStep
import com.aliucord.manager.installer.util.Signer
import org.koin.core.component.KoinComponent

/**
 * Sign the APK with a keystore generated on-device.
 */
class SigningStep : Step(), KoinComponent {
    override val group = StepGroup.Install
    override val localizedName = R.string.install_step_signing

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        Signer.signApk(apk)
    }
}
