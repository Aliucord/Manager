package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.dto.Version
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a compiled dex file to be injected into the APK as the first `classes.dex` to override an entry point class.
 */
@Stable
class DownloadInjectorStep : DownloadStep(), KoinComponent {
    private val paths: PathManager by inject()

    /**
     * Populated from a dependency step ([FetchInfoStep]).
     * This is used as cache invalidation (ref: [Version.aliucordHash])
     */
    lateinit var aliucordHash: String
        private set

    override val localizedName = R.string.patch_step_dl_injector
    override val targetUrl = URL
    override val targetFile
        get() = paths.cachedInjectorDex(aliucordHash)

    override suspend fun execute(container: StepRunner) {
        aliucordHash = container
            .getStep<FetchInfoStep>()
            .data.aliucordHash

        super.execute(container)
    }

    private companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"

        const val URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/Injector.dex"
    }
}
