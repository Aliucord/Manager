package com.aliucord.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aliucord.manager.utils.DownloadUtils
import com.aliucord.manager.utils.Github
import com.aliucord.manager.utils.aliucordDir
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InstallerScreen() {
    var log by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        launch {
            val supportedVersion = Github.versions.versionCode

            log += "Checking for cached APK...\n"

            val apk = File(context.cacheDir, "discord-${supportedVersion}").let { file ->
                val archiveInfo = context.packageManager.getPackageArchiveInfo(file.path, 0)

                if (file.exists() && archiveInfo?.versionCode.toString().startsWith(supportedVersion)) return@let file

                log += "Downloading APK...\n"

                DownloadUtils.downloadDiscordApk(context, supportedVersion)

                log += "Finished downloading APK\n"
            }

            val manifest = File(aliucordDir, "AndroidManifest.xml").takeIf(File::exists) ?: run {
                log += "Downloading manifest\n"

                DownloadUtils.download(context, Github.getDownloadUrl("builds", "AndroidManifest.xml"))
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(log)

        Spacer(Modifier.weight(1f, true))

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(8.dp),
            progress = 0.5f
        )
    }
}