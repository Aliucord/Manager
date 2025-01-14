package com.aliucord.manager.ui.screens.patchopts.components.options

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliucord.manager.ui.components.ResetToDefaultButton

@Composable
fun TextPatchOption(
    name: String,
    description: String,
    value: String,
    valueIsError: Boolean,
    valueIsDefault: Boolean,
    onValueChange: (String) -> Unit,
    onValueReset: () -> Unit,
    modifier: Modifier = Modifier,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    PatchOption(
        name = name,
        description = description,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = valueIsError,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            trailingIcon = {
                ResetToDefaultButton(
                    enabled = !valueIsDefault,
                    onClick = onValueReset,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )

        extra?.invoke(this)
    }
}
