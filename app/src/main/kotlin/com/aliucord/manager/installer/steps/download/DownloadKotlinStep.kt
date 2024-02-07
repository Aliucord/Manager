package com.aliucord.manager.installer.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.R
import com.aliucord.manager.installer.steps.base.DownloadStep
import com.aliucord.manager.manager.PathManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download the most recent available Kotlin stdlib build that is supported.
 */
@Stable
class DownloadKotlinStep : DownloadStep(), KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.install_step_dl_kotlin
    override val targetUrl = URL
    override val targetFile = paths.cachedKotlinDex()

    private companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"

        const val URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/main/installer/android/app/src/main/assets/kotlin/classes.dex"
    }
}
