package com.aliucord.manager.installer.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.StepRunner
import com.aliucord.manager.installer.steps.base.DownloadStep
import com.aliucord.manager.installer.steps.prepare.FetchInfoStep
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
    private lateinit var aliucordHash: String

    override val localizedName = R.string.install_step_dl_injector
    override val targetUrl = URL
    override val targetFile
        get() = paths.cachedInjectorDex(aliucordHash).resolve("discord.apk")

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
