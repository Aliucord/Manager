package com.aliucord.manager.ui.screens.installopts.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.installopts.PackageNameState

@Composable
fun PackageNameSetting(
    name: String,
    nameIsDefault: Boolean,
    state: PackageNameState,
    onChangeName: (String) -> Unit,
    onResetName: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.installopts_pkgname_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(R.string.installopts_pkgname_desc),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(.7f),
        )

        val containerColor = MaterialTheme.colorScheme
            .surfaceColorAtElevation(LocalAbsoluteTonalElevation.current + .5.dp)

        OutlinedTextField(
            value = name,
            onValueChange = onChangeName,
            isError = state == PackageNameState.Invalid,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = containerColor,
                focusedContainerColor = containerColor,
                errorContainerColor = containerColor,
            ),
            trailingIcon = {
                ResetToDefaultButton(
                    enabled = !nameIsDefault,
                    onClick = onResetName,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        )

        PackageNameStateLabel(
            state = state,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
