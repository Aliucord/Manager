package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.steps.base.DownloadStep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * If not already cached, then download the raw unmodified v126.21 (Kotlin) Discord APK
 * from a redirect to an APK mirror site provided by the Aliucord backend.
 */
@Stable
class DownloadDiscordStep : DownloadStep(), KoinComponent {
    private val paths: PathManager by inject()

    override val localizedName = R.string.patch_step_dl_kt_apk
    override val targetUrl = getDiscordApkUrl(DISCORD_KT_VERSION)
    override val targetFile = paths.discordApkVersionCache(DISCORD_KT_VERSION)
        .resolve("discord.apk")

    override suspend fun verify() {
        super.verify()

        // TODO: verify signature
    }

    private companion object {
        /**
         * Last version of Discord before the RN transition.
         */
        const val DISCORD_KT_VERSION = 126021

        fun getDiscordApkUrl(version: Int) =
            "${BuildConfig.BACKEND_URL}/download/discord?v=$version"
    }
}
