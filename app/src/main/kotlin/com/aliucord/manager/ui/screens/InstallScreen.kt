/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.aliucord.libzip.Zip
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.ui.screens.destinations.HomeScreenDestination
import com.aliucord.manager.utils.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

@OptIn(ExperimentalFoundationApi::class)
@Destination
@Composable
fun InstallerScreen(navigator: DestinationsNavigator, apk: File?) {
    var log by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val supportedVersion = Github.version.versionCode

            val discordApk = apk ?: run {
                log += "Checking for cached APK...\n"

                File(context.externalCacheDir, "discord-${supportedVersion}.apk").also { file ->
                    val archiveInfo = context.packageManager.getPackageArchiveInfo(file.path, 0)

                    if (archiveInfo?.versionCode.toString().startsWith(supportedVersion)) return@also

                    log += "Downloading Discord APK...\n"

                    DownloadUtils.downloadDiscordApk(context, supportedVersion)

                    log += "Done\n"
                }
            }

            val injector = File(context.externalCacheDir, "Injector.dex").also { file ->
                if (file.exists()) return@also

                log += "Downloading injector...\n"

                DownloadUtils.downloadInjector(context)

                log += "Done\n"
            }

            val outputDir = aliucordDir.also {
                if (!it.exists() && !it.mkdirs()) throw FileNotFoundException()
            }

            val outputApk = File(outputDir, "Aliucord.apk")
            discordApk.copyTo(outputApk, true)

            log += "Repacking APK\n"

            var patched = false
            var manifestBytes: ByteArray?

            with(Zip(outputApk.absolutePath, 6, 'r')) {
                for (index in 0 until totalEntries) {
                    openEntryByIndex(index)
                    val name = entryName
                    closeEntry()
                    if (name == "classes5.dex") {
                        patched = true
                        break
                    }
                }

                val cacheDir = context.cacheDir.absolutePath

                if (!patched) (1..3).forEach { i ->
                    openEntry("classes" + (if (i == 1) "" else i) + ".dex")
                    extractEntry(cacheDir + "/classes" + (i + 1) + ".dex")
                    closeEntry()
                }

                openEntry("AndroidManifest.xml")
                manifestBytes = readEntry()
                closeEntry()

                close()
            }

            val assetManager = context.assets

            with(Zip(outputApk.absolutePath, 6, 'a')) {

                if (!patched) {
                    (1..3).forEach { i -> deleteEntry("classes${if (i == 1) "" else i}.dex") }
                } else {
                    deleteEntry("classes.dex")
                    deleteEntry("classes5.dex")
                    deleteEntry("classes6.dex")
                }

                deleteEntry("AndroidManifest.xml")
                deleteEntry("lib/arm64-v8a/libpine.so")
                deleteEntry("lib/armeabi-v7a/libpine.so")

                if (!patched) for (i in 2..4) {
                    val name = "classes$i.dex"
                    val cacheFile = File(context.cacheDir, name)

                    openEntry(name)
                    compressFile(cacheFile.absolutePath)
                    closeEntry()
                    cacheFile.delete()
                }

                openEntry("classes.dex")
                compressFile(injector.absolutePath)
                closeEntry()

                writeEntry("classes5.dex", assetManager.open("pine/classes.dex"))

                writeEntry("lib/arm64-v8a/libpine.so", assetManager.open("pine/arm64-v8a/libpine.so"))
                writeEntry("lib/armeabi-v7a/libpine.so", assetManager.open("pine/armeabi-v7a/libpine.so"))

                writeEntry("classes6.dex", assetManager.open("kotlin/classes.dex"))

                manifestBytes?.let { bytes ->
                    log += "Patching manifest\n"
                    val newManifestBytes = patchManifest(bytes)
                    openEntry("AndroidManifest.xml")
                    writeEntry(newManifestBytes, newManifestBytes.size.toLong())
                    closeEntry()
                }

                close()
            }

            if (Prefs.replaceBg.get()) {
                log += "Replacing icon background\n"

                val icon1Bytes = assetManager.open("icon1.png").readBytes()
                val icon2Bytes = assetManager.open("icon2.png").readBytes()

                // use androguard to figure out entries
                // androguard arsc resources.arsc --id 0x7f0f0000 (icon1)
                // androguard arsc resources.arsc --id 0x7f0f0002 and androguard arsc resources.arsc --id 0x7f0f0006 (icon2)
                val icon1Entries = listOf("MbV.png", "kbF.png", "_eu.png", "EtS.png")
                val icon2Entries =
                    listOf("_h_.png", "9MB.png", "Dy7.png", "kC0.png", "oEH.png", "RG0.png", "ud_.png", "W_3.png")

                with(Zip(outputApk.absolutePath, 0, 'a')) {
                    icon1Entries.forEach { entry -> deleteEntry("res/$entry") }
                    icon2Entries.forEach { entry -> deleteEntry("res/$entry") }

                    icon1Entries.forEach { entry -> writeEntry("res/$entry", icon1Bytes) }
                    icon2Entries.forEach { entry -> writeEntry("res/$entry", icon2Bytes) }

                    close()
                }
            }

            log += "Signing APK...\n"

            Signer.signApk(outputApk)

            log += "Signed APK\n"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", outputApk),
                    "application/vnd.android.package-archive"
                )

                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)

            launch(Dispatchers.Main) {
                navigator.navigate(HomeScreenDestination)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(log)

        Spacer(Modifier.weight(1f, true))

        //        LinearProgressIndicator(
        //            modifier = Modifier
        //                .fillMaxWidth()
        //                .height(8.dp),
        //            progress = 0.5f
        //        )
    }
}
