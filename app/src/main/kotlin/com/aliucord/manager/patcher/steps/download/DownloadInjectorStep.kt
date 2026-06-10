package com.aliucord.manager.patcher.steps.download

import android.content.Context
import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.*
import com.aliucord.manager.patcher.steps.patch.ReorganizeDexStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import com.aliucord.manager.ui.screens.componentopts.PatchComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.FileNotFoundException

/**
 * Download a compiled dex file to be injected into the APK as the first `classes.dex` to override an entry point class.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadInjectorStep(
    private val custom: PatchComponent?,
) : DownloadStep<SemVer>(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()
    private val context: Context by inject()

    override val localizedName = R.string.patch_step_dl_injector

    override fun getRemoteUrl(container: StepRunner) = URL

    // Set when the Sunflower injector bundled in Manager assets is present
    private val bundledVersion: SemVer? by lazy {
        try {
            context.assets.open(BUNDLED_ASSET).use { }
            SemVer.parse(BUNDLED_VERSION)
        } catch (_: Exception) {
            null
        }
    }

    override fun getVersion(container: StepRunner) =
        custom?.version ?: bundledVersion ?: container.getStep<FetchInfoStep>().data.injectorVersion

    override fun getStoredFile(container: StepRunner) =
        custom?.getFile(paths) ?: paths.cachedInjector(getVersion(container))

    override val dexCount = 1
    override val dexPriority = 3
    override fun getDexFiles(container: StepRunner) =
        listOf(getStoredFile(container).readBytes())

    override suspend fun execute(container: StepRunner) {
        if (custom != null) {
            container.log("Using custom injector with version ${custom.version} built ${custom.timestamp}")

            if (!custom.getFile(paths).exists()) {
                throw FileNotFoundException(
                    "Selected custom component does not exist on disk! If this is an update, " +
                        "updates cannot occur when the originally selected custom component has been deleted."
                )
            }

            state = StepState.Skipped
            return
        }

        // Prefer the Sunflower injector bundled in Manager assets over a remote download
        if (bundledVersion != null) {
            val bundled = context.assets.open(BUNDLED_ASSET).use { it.readBytes() }
            container.log("Using Sunflower injector $bundledVersion bundled in Manager assets (${bundled.size} bytes)")
            getStoredFile(container).apply {
                parentFile?.mkdirs()
                writeBytes(bundled)
            }
            state = StepState.Skipped
            return
        }

        super.execute(container)
    }

    private companion object {
        const val URL = "https://builds.aliucord.com/Injector.dex"
        const val BUNDLED_ASSET = "sunflower/Injector.dex"
        const val BUNDLED_VERSION = "2.4.11"
    }
}
