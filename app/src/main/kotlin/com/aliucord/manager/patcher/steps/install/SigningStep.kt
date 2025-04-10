package com.aliucord.manager.patcher.steps.install

import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.util.Signer
import org.koin.core.component.KoinComponent

/**
 * Sign the APK with a keystore generated on-device.
 */
class SigningStep : Step(), KoinComponent {
    override val group = StepGroup.Install
    override val localizedName = R.string.patch_step_signing

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk

        container.log("Signing apk at ${apk.absolutePath}")
        Signer.signApk(apk)
    }
}
