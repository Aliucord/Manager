package com.aliucord.manager.ui.viewmodels

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.libzip.Zip
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

class InstallViewModel(application: Application) : AndroidViewModel(application) {

    private val _returnToHome = MutableSharedFlow<Boolean>()
    val returnToHome = _returnToHome.asSharedFlow()

    private var installationRunning: Boolean = false

    val logs = mutableStateListOf<String>()

    fun startInstallation(apk: File?) {
        if (installationRunning) return

        viewModelScope.launch(Dispatchers.IO) {
            installationRunning = true
            val supportedVersion = Github.version.versionCode

            val discordApk = apk ?: run {
                logs.add("Checking for cached APK...")

                getApplication<Application>().externalCacheDir!!.resolve("discord-${supportedVersion}.apk").also { file ->
                    val archiveInfo = getApplication<Application>().packageManager.getPackageArchiveInfo(file.path, 0)

                    if (archiveInfo?.versionCode.toString().startsWith(supportedVersion)) return@also

                    logs.add("Downloading Discord APK...")

                    DownloadUtils.downloadDiscordApk(getApplication<Application>(), supportedVersion)

                    logs.add("Done")
                }
            }

            val injector = getApplication<Application>().externalCacheDir!!.resolve("Injector.dex").also { file ->
                if (file.exists()) return@also

                logs.add("Downloading injector...")

                DownloadUtils.downloadInjector(getApplication<Application>())

                logs.add("Done")
            }

            val outputDir = aliucordDir.also {
                if (!it.exists() && !it.mkdirs()) throw FileNotFoundException()
            }

            val outputApk = outputDir.resolve("Aliucord.apk")
            discordApk.copyTo(outputApk, true)

            logs.add("Repacking APK")

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

                val cacheDir = getApplication<Application>().cacheDir.absolutePath

                if (!patched) (1..3).forEach { i ->
                    openEntry("classes${if (i == 1) "" else i}.dex")
                    extractEntry("$cacheDir/classes${i + 1}.dex")
                    closeEntry()
                }

                openEntry("AndroidManifest.xml")
                manifestBytes = readEntry()
                closeEntry()

                close()
            }

            val assetManager = getApplication<Application>().assets

            with(Zip(outputApk.absolutePath, 6, 'a')) {
                if (patched) {
                    deleteEntry("classes.dex")
                    deleteEntry("classes5.dex")
                    deleteEntry("classes6.dex")
                } else {
                    (1..3).forEach { i -> deleteEntry("classes${if (i == 1) "" else i}.dex") }
                }

                deleteEntry("AndroidManifest.xml")

                listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64").forEach { arch ->
                    listOf("/libaliuhook.so", "/liblsplant.so", "/libc++_shared.so").forEach { file ->
                        deleteEntry("lib/$arch$file")
                    }
                }

                if (!patched) for (i in 2..4) {
                    val name = "classes$i.dex"
                    val cacheFile = getApplication<Application>().cacheDir.resolve(name)

                    openEntry(name)
                    compressFile(cacheFile.absolutePath)
                    closeEntry()
                    cacheFile.delete()
                }

                openEntry("classes.dex")
                compressFile(injector.absolutePath)
                closeEntry()

                writeEntry("classes5.dex", assetManager.open("aliuhook/classes.dex"))

                listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64").forEach { arch ->
                    listOf("/libaliuhook.so", "/liblsplant.so", "/libc++_shared.so").forEach { file ->
                        writeEntry("lib/$arch$file", assetManager.open("aliuhook/$arch$file"))
                    }
                }

                writeEntry("classes6.dex", assetManager.open("kotlin/classes.dex"))

                manifestBytes?.let { bytes ->
                    logs.add("Patching manifest")
                    val newManifestBytes = patchManifest(bytes)
                    openEntry("AndroidManifest.xml")
                    writeEntry(newManifestBytes, newManifestBytes.size.toLong())
                    closeEntry()
                }

                close()
            }

            if (Prefs.replaceBg.get()) {
                logs.add("Replacing icon background")

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

            logs.add("Signing APK...")

            Signer.signApk(outputApk)

            logs.add("Signed APK")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

                setDataAndType(
                    FileProvider.getUriForFile(getApplication<Application>(), "${BuildConfig.APPLICATION_ID}.provider", outputApk),
                    "application/vnd.android.package-archive"
                )
            }

            getApplication<Application>().startActivity(intent)

            _returnToHome.emit(true)
            installationRunning = false
        }
    }

}
