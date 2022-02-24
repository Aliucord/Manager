package com.aliucord.manager.ui.screens

import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.aliucord.libzip.Zip
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.Signer
import com.aliucord.manager.preferences.replaceBg
import com.aliucord.manager.ui.Screen
import com.aliucord.manager.utils.DownloadUtils
import com.aliucord.manager.utils.Github
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallerScreen(navController: NavController) {
    var log by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val supportedVersion = Github.version.versionCode

            log += "Checking for cached APK...\n"

            val discordApk = File(context.externalCacheDir, "discord-${supportedVersion}.apk").also { file ->
                if (file.exists()) return@also

                val archiveInfo = context.packageManager.getPackageArchiveInfo(file.path, 0)

                if (archiveInfo?.versionCode.toString().startsWith(supportedVersion)) return@also

                log += "Downloading APK...\n"

                DownloadUtils.downloadDiscordApk(context, supportedVersion)

                log += "Finished downloading APK\n"
            }

            val manifest = File(context.externalCacheDir, "AndroidManifest.xml").also { file ->
                if (file.exists()) return@also

                log += "Downloading patched AndroidManifest.xml...\n"

                DownloadUtils.downloadManifest(context)

                log += "Finished downloading manifest\n"
            }

            val injector = File(context.externalCacheDir, "Injector.dex").also { file ->
                if (file.exists()) return@also

                log += "Downloading injector...\n"

                DownloadUtils.downloadInjector(context)

                log += "Finished downloading injector\n"
            }

            log += "Copying original APK ()\n"


            val outputDir = File(Environment.getExternalStorageDirectory(), "Aliucord").also {
                if (!it.exists() && !it.mkdirs()) throw FileNotFoundException()
            }

            val outputApk = File(outputDir, "Aliucord.apk")
            discordApk.copyTo(outputApk, true)

            log += "Repacking APK\n"

            var patched = false

            with(Zip(outputApk.absolutePath, 6, 'r')) {
                repeat(totalEntries) { index ->
                    openEntryByIndex(index)
                    val name = entryName
                    if (name == "classes5.dex") patched = true
                    closeEntry()
                }

                val cacheDir = context.cacheDir.absolutePath

                if (!patched) (1..3).forEach { i ->
                    openEntry("classes" + (if (i == 1) "" else i) + ".dex")
                    extractEntry(cacheDir + "/classes" + (i + 1) + ".dex")
                    closeEntry()
                }

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

                openEntry("AndroidManifest.xml")
                compressFile(manifest.absolutePath)
                closeEntry()

                writeEntry("lib/arm64-v8a/libpine.so", assetManager.open("pine/arm64-v8a/libpine.so"))
                writeEntry("lib/armeabi-v7a/libpine.so", assetManager.open("pine/armeabi-v7a/libpine.so"))

                writeEntry("classes6.dex", assetManager.open("kotlin/classes.dex"))

                close()
            }

            if (replaceBg.get()) {
                log += "Replacing icon background\n"

                val icon1Bytes = assetManager.open("icon1.png").readBytes()
                val icon2Bytes = assetManager.open("icon2.png").readBytes()

                // use androguard to figure out entries
                // androguard arsc resources.arsc --id 0x7f0f0000 (icon1)
                // androguard arsc resources.arsc --id 0x7f0f0002 and androguard arsc resources.arsc --id 0x7f0f0006 (icon2)
                val icon1Entries = listOf("MbV.png", "kbF.png", "_eu.png", "EtS.png")
                val icon2Entries = listOf("_h_.png", "9MB.png", "Dy7.png", "kC0.png", "oEH.png", "RG0.png", "ud_.png", "W_3.png")

                with(Zip(outputApk.absolutePath, 0, 'a')) {
                    icon1Entries.forEach { entry -> deleteEntry("res/$entry") }
                    icon2Entries.forEach { entry -> deleteEntry("res/$entry") }

                    icon1Entries.forEach { entry -> writeEntry("res/$entry", icon1Bytes) }
                    icon2Entries.forEach { entry -> writeEntry("res/$entry", icon2Bytes) }

                    close()
                }
            }

            log += "Signing APK\n"

            Signer.newKeystore(File(context.filesDir, "ks.keystore"))
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
                navController.navigate(Screen.Home.route)
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
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            progress = 0.5f
        )
    }
}

private fun Zip.writeEntry(entryName: String, stream: InputStream) {
    openEntry(entryName)
    stream.readBytes().let { bytes -> writeEntry(bytes, bytes.size.toLong()) }
    closeEntry()
}

private fun Zip.writeEntry(entryName: String, bytes: ByteArray) {
    openEntry(entryName)
    writeEntry(bytes, bytes.size.toLong())
    closeEntry()
}