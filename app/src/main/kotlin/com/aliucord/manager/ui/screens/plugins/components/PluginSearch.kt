package com.aliucord.manager.ui.screens.plugins.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.ResetToDefaultButton

@Composable
fun PluginSearch(
    currentFilter: State<String>,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier.Companion,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = currentFilter.value,
        onValueChange = onFilterChange,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        label = { Text(stringResource(R.string.action_search)) },
        trailingIcon = {
            val isFilterBlank by remember { derivedStateOf { currentFilter.value.isEmpty() } }

            ResetToDefaultButton(
                enabled = !isFilterBlank,
                onClick = { onFilterChange("") },
                modifier = Modifier.padding(end = 4.dp),
            )
        },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            imeAction = ImeAction.Companion.Search
        ),
        keyboardActions = KeyboardActions { focusManager.clearFocus() },
        modifier = modifier,
    )
}
