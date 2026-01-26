package com.aliucord.manager.ui.previews.screens

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.*
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.ui.screens.componentopts.ComponentOptionsScreenContent
import com.aliucord.manager.ui.screens.componentopts.PatchComponent
import com.aliucord.manager.ui.theme.ManagerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// This preview has scrollable/interactable content that cannot be tested from an IDE preview

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ComponentOptionsScreenPreview(
    @PreviewParameter(ComponentOptionsParametersProvider::class)
    parameters: ComponentOptionsParameters,
) {
    ManagerTheme {
        ComponentOptionsScreenContent(
            componentType = parameters.componentType,
            components = parameters.components,
            selected = parameters.selected,
            onSelectComponent = {},
            onDeleteComponent = {},
            onBackPressed = {},
        )
    }
}

private data class ComponentOptionsParameters(
    val componentType: PatchComponent.Type,
    val components: ImmutableList<PatchComponent>,
    val selected: PatchComponent?,
)

private class ComponentOptionsParametersProvider : PreviewParameterProvider<ComponentOptionsParameters> {
    private val components = persistentListOf(
        PatchComponent(
            type = PatchComponent.Type.Injector,
            version = SemVer(1, 2, 3),
            timestamp = Clock.System.now(),
        ),
        PatchComponent(
            type = PatchComponent.Type.Injector,
            version = SemVer(2, 3, 1),
            timestamp = Clock.System.now() - 10.minutes,
        ),
        PatchComponent(
            type = PatchComponent.Type.Injector,
            version = SemVer(2, 3, 1),
            timestamp = Clock.System.now() - 1.days,
        ),
        PatchComponent(
            type = PatchComponent.Type.Injector,
            version = SemVer(0, 0, 1),
            timestamp = Clock.System.now() - 10.hours,
        ),
        PatchComponent(
            type = PatchComponent.Type.Injector,
            version = SemVer(3, 0, 2),
            timestamp = Clock.System.now() - 7.days,
        ),
    )

    override val values = sequenceOf(
        ComponentOptionsParameters(
            componentType = PatchComponent.Type.Injector,
            components = components,
            selected = null,
        ),
    )
}
