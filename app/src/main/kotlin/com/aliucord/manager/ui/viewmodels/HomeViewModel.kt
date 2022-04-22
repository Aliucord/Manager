package com.aliucord.manager.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aliucord.manager.models.github.Version
import com.aliucord.manager.preferences.Prefs
import com.aliucord.manager.utils.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val packageManager = getApplication<Application>().packageManager

    var supportedVersion by mutableStateOf("")
        private set

    var installedVersion by mutableStateOf("-")
        private set

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val version = json.decodeFromString<Version>(httpClient.get(Github.dataUrl).bodyAsText())

            supportedVersion = "${version.versionName} - " + when (version.versionCode[3]) {
                '0' -> "Stable"
                '1' -> "Beta"
                '2' -> "Alpha"
                else -> throw NoWhenBranchMatchedException()
            }
            installedVersion = try {
                packageManager.getPackageInfo(Prefs.packageName.get(), 0).versionName
            } catch (th: Throwable) {
                "-"
            }
        }
    }

    fun launchAliucord() {
        packageManager.getLaunchIntentForPackage(Prefs.packageName.get())?.let {
            getApplication<Application>().startActivity(it)
        } ?: Toast.makeText(getApplication(), "Failed to launch Aliucord", Toast.LENGTH_LONG)
            .show()
    }

    fun uninstallAliucord() {
        val packageURI = Uri.parse("package:${Prefs.packageName.get()}")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        getApplication<Application>().startActivity(uninstallIntent)
    }

    init {
        load()
    }

}
