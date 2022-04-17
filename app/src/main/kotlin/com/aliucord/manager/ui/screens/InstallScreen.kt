/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.ui.screens.destinations.HomeScreenDestination
import com.aliucord.manager.utils.*
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
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

                context.externalCacheDir!!.resolve("discord-${supportedVersion}.apk").also { file ->
                    val archiveInfo = context.packageManager.getPackageArchiveInfo(file.path, 0)

                    if (archiveInfo?.versionCode.toString().startsWith(supportedVersion)) return@also

                    log += "Downloading Discord APK...\n"

                    DownloadUtils.downloadDiscordApk(context, supportedVersion)

                    log += "Done\n"
                }
            }

            val injector = context.externalCacheDir!!.resolve("Injector.dex").also { file ->
                if (file.exists()) return@also

                log += "Downloading injector...\n"

                DownloadUtils.downloadInjector(context)

                log += "Done\n"
            }

            val outputDir = aliucordDir.also {
                if (!it.exists() && !it.mkdirs()) throw FileNotFoundException()
            }

            val outputApk = outputDir.resolve("Aliucord.apk")
            discordApk.copyTo(outputApk, true)

            log += "Repacking APK\n"

            var patched = false
            var manifestBytes: ByteArray?

            ZipReader(outputApk).use { zip ->
                patched = zip.entryNames.contains("classes5.dex")
                manifestBytes = zip.openEntry("AndroidManifest.xml")?.read()

                val cacheDir = context.cacheDir.absolutePath

                if (!patched) (1..3).forEach { i ->
                    val entry = zip.openEntry("classes${if (i == 1) "" else i}.dex")!!

                    File(cacheDir, "classes${i + 1}.dex").apply {
                        exists() || createNewFile()
                        writeBytes(entry.read())
                    }
                }
            }

            val assetManager = context.assets

            ZipWriter(outputApk, true).use { zip ->
                val toDelete = if (patched) mutableListOf(
                    "classes.dex",
                    "classes5.dex",
                    "classes6.dex",
                    "AndroidManifest.xml",
                ) else mutableListOf(
                    "classes.dex",
                    "classes2.dex",
                    "classes3.dex",
                    "AndroidManifest.xml",
                )

                for (arch in arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")) {
                    for (lib in arrayOf("/libaliuhook.so", "/liblsplant.so", "/libc++_shared.so")) {
                        toDelete += "lib/$arch$lib"
                    }
                }
                zip.deleteEntries(*toDelete.toTypedArray())

                if (!patched) for (i in 2..4) {
                    val name = "classes$i.dex"
                    val cacheFile = context.cacheDir.resolve(name)
                    zip.writeEntry(name, cacheFile.readBytes())
                    cacheFile.delete()
                }

                zip.writeEntry("classes.dex", injector.readBytes())
                zip.writeEntry("classes5.dex", assetManager.open("aliuhook/classes.dex").readBytes())
                zip.writeEntry("classes6.dex", assetManager.open("kotlin/classes.dex").readBytes())

                for (arch in arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")) {
                    for (lib in arrayOf("/libaliuhook.so", "/liblsplant.so", "/libc++_shared.so")) {
                        val stream = assetManager.open("aliuhook/$arch$lib")
                        zip.writeEntry("lib/$arch$lib", stream.readBytes())
                    }
                }

                manifestBytes?.let {
                    log += "Patching manifest\n"
                    val newManifest = patchManifest(it)
                    zip.writeEntry("AndroidManifest.xml", newManifest)
                }
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

                ZipWriter(outputApk, true).use { zip ->
                    val toDelete = icon1Entries.map { "res/$it" } + icon2Entries.map { "res/$it" }
                    zip.deleteEntries(*toDelete.toTypedArray())

                    icon1Entries.forEach { entry -> zip.writeEntry("res/$entry", icon1Bytes) }
                    icon2Entries.forEach { entry -> zip.writeEntry("res/$entry", icon2Bytes) }
                }
            }

            log += "Signing APK...\n"

            Signer.signApk(outputApk)

            log += "Signed APK\n"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

                setDataAndType(
                    FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", outputApk),
                    "application/vnd.android.package-archive"
                )
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

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(8.dp)
                .size(32.dp)
        )
    }
}
