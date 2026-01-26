package com.aliucord.manager.ui.screens.componentopts

import android.app.Application
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.screenModelScope
import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.ui.util.ScreenModelWithResult
import com.aliucord.manager.ui.util.ScreenResultKey
import com.aliucord.manager.util.*
import kotlinx.coroutines.launch
import kotlin.time.Instant

class ComponentOptionsModel(
    screenResultKey: ScreenResultKey,
    private val paths: PathManager,
    private val context: Application,
) : ScreenModelWithResult<PatchComponent?>(screenResultKey) {
    val components = mutableStateListOf<PatchComponent>()
    var selected by mutableStateOf<PatchComponent?>(null)
        private set

    fun selectComponent(component: PatchComponent?) {
        selected = component
    }

    fun deleteComponent(component: PatchComponent) = screenModelScope.launchIO {
        component.getFile(paths).delete()

        mainThread {
            components.remove(component)
            context.showToast(R.string.componentopts_deleted)
        }
    }

    /**
     * Loads the available imported custom components for a specified type.
     */
    suspend fun refreshComponents(type: PatchComponent.Type) {
        val files = when (type) {
            PatchComponent.Type.Injector -> paths.customInjectors()
            PatchComponent.Type.Patches -> paths.customSmaliPatches()
        }

        // ${timestamp}_${componentVersion}.${componentFile.extension}
        val componentNameRegex = """^(\d+)_(\d+\.\d+.\d+)\.\w+$""".toRegex()

        for (file in files) {
            val match = componentNameRegex.find(file.name) ?: continue
            val (_, timestamp, version) = match.groupValues

            val component = PatchComponent(
                type = type,
                version = SemVer.parse(version),
                timestamp = Instant.fromEpochMilliseconds(timestamp.toLong()),
            )

            mainThread { components += component }
        }
    }

    override fun onDispose() {
        screenModelScope.launch { setResult(selected) }
    }
}
