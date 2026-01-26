package com.aliucord.manager.ui.screens.componentopts

import android.os.Parcelable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.componentopts.components.*
import com.aliucord.manager.ui.util.ScreenWithResult
import com.aliucord.manager.ui.util.paddings.*
import com.aliucord.manager.util.back
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

@Parcelize
class ComponentOptionsScreen(
    /**
     * The type of custom component that this screen will be selecting.
     */
    private val componentType: PatchComponent.Type,
    /**
     * A previously selected custom component that should be pre-selected on this screen.
     */
    private val default: PatchComponent?,
) : ScreenWithResult<PatchComponent?>(), Parcelable {
    @IgnoredOnParcel
    override val key = "ComponentOptions-$componentType"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<ComponentOptionsModel> { parametersOf(this.resultKey) }

        LaunchedEffect(Unit) {
            model.components.clear()
            withContext(Dispatchers.IO) {
                model.refreshComponents(componentType)
            }
            if (default in model.components) {
                model.selectComponent(default)
            }
        }

        ComponentOptionsScreenContent(
            componentType = componentType,
            components = model.components.toImmutableList(),
            selected = model.selected,
            onSelectComponent = model::selectComponent,
            onDeleteComponent = model::deleteComponent,
            onBackPressed = { navigator.back(null) },
        )
    }
}

@Composable
fun ComponentOptionsScreenContent(
    componentType: PatchComponent.Type,
    components: ImmutableList<PatchComponent>,
    selected: PatchComponent?,
    onSelectComponent: (PatchComponent?) -> Unit,
    onDeleteComponent: (PatchComponent) -> Unit,
    onBackPressed: () -> Unit,
) {
    Scaffold(
        topBar = { ComponentOptionsAppBar(componentType = componentType) },
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = paddingValues
                .exclude(PaddingValuesSides.Horizontal + PaddingValuesSides.Top)
                .add(PaddingValues(16.dp)),
            modifier = Modifier
                .padding(paddingValues.exclude(PaddingValuesSides.Bottom)),
        ) {
            item(key = "NONE") {
                PatchComponentCardBase(
                    selected = selected == null,
                    onSelect = { onSelectComponent(null) },
                ) {
                    Text(
                        text = stringResource(R.string.componentopts_selected_none),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            items(
                items = components,
                contentType = { "COMPONENT" },
                key = { it },
            ) { component ->
                PatchComponentCard(
                    version = component.version,
                    timestamp = component.timestamp,
                    selected = selected == component,
                    onSelect = { onSelectComponent(component) },
                    onDelete = { onDeleteComponent(component) },
                )
            }

            item("EXIT_BTN") {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    FilledTonalButton(
                        onClick = onBackPressed,
                    ) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            }
        }
    }
}
