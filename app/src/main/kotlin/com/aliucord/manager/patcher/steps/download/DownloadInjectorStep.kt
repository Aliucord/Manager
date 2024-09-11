package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.base.IDexProvider
import com.aliucord.manager.patcher.steps.patch.ReorganizeDexStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a compiled dex file to be injected into the APK as the first `classes.dex` to override an entry point class.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadInjectorStep : DownloadStep(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()

    /**
     * This is populated right before the download starts (ref: [execute])
     */
    lateinit var targetVersion: String
        private set

    override val localizedName = R.string.patch_step_dl_injector
    override val targetUrl = URL
    override val targetFile
        get() = paths.cachedInjectorDex(targetVersion)

    override suspend fun execute(container: StepRunner) {
        targetVersion = container.getStep<FetchInfoStep>()
            .data.injectorVersion.toString()

        super.execute(container)
    }

    private companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"

        const val URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/Injector.dex"
    }

    override val dexCount = 1
    override val dexPriority = 3
    override fun getDexFiles() = listOf(targetFile.readBytes())
}
