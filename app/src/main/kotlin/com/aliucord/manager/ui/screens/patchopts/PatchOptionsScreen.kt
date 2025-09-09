package com.aliucord.manager.ui.screens.patchopts

import android.os.Parcelable
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.aliucord.manager.R
import com.aliucord.manager.ui.components.*
import com.aliucord.manager.ui.components.dialogs.NetworkWarningDialog
import com.aliucord.manager.ui.screens.iconopts.*
import com.aliucord.manager.ui.screens.patching.PatchingScreen
import com.aliucord.manager.ui.screens.patchopts.components.PackageNameStateLabel
import com.aliucord.manager.ui.screens.patchopts.components.PatchOptionsAppBar
import com.aliucord.manager.ui.screens.patchopts.components.options.*
import com.aliucord.manager.ui.util.spacedByLastAtBottom
import com.aliucord.manager.util.showToast
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

@Parcelize
class PatchOptionsScreen(
    private val prefilledOptions: PatchOptions? = null,
) : Screen, Parcelable {
    @IgnoredOnParcel
    override val key = "PatchOptions"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<PatchOptionsModel> { parametersOf(prefilledOptions ?: PatchOptions.Default) }
        val iconModel = koinScreenModel<IconOptionsModel> { parametersOf((prefilledOptions ?: PatchOptions.Default).iconReplacement) }

        if (model.showNetworkWarningDialog) {
            NetworkWarningDialog(
                onConfirm = model::hideNetworkWarning,
                onDismiss = { neverShow ->
                    model.hideNetworkWarning(neverShow)
                    navigator.pop()
                },
            )
        }

        PatchOptionsScreenContent(
            isUpdate = prefilledOptions != null,
            isDevMode = model.isDevMode,

            debuggable = model.debuggable,
            setDebuggable = model::changeDebuggable,

            selectedColor = when (iconModel.mode) {
                IconOptionsMode.Original -> PatchOptions.IconReplacement.BlurpleColor
                IconOptionsMode.OldDiscord -> PatchOptions.IconReplacement.OldBlurpleColor
                IconOptionsMode.Aliucord -> PatchOptions.IconReplacement.AliucordColor
                IconOptionsMode.CustomColor -> iconModel.selectedColor.toColor()
                IconOptionsMode.CustomImage -> null
            },
            oldLogo = iconModel.mode == IconOptionsMode.OldDiscord,
            selectedImage = { iconModel.selectedImage },
            onOpenIconOptions = { navigator.push(IconOptionsScreen()) },

            appName = model.appName,
            appNameIsError = model.appNameIsError,
            setAppName = model::changeAppName,

            packageName = model.packageName,
            packageNameState = model.packageNameState,
            setPackageName = model::changePackageName,

            isConfigValid = model.isConfigValid,
            onInstall = onInstall@{
                val iconConfig = iconModel.generateConfig()
                if (iconConfig == null) {
                    context.showToast(R.string.patchopts_warning_invalid_iconopts)
                    return@onInstall
                }

                val patchConfig = model.generateConfig(iconConfig)
                navigator.push(PatchingScreen(patchConfig))
            },
        )
    }
}

@Composable
fun PatchOptionsScreenContent(
    isUpdate: Boolean,
    isDevMode: Boolean,

    debuggable: Boolean,
    setDebuggable: (Boolean) -> Unit,

    oldLogo: Boolean,
    selectedColor: Color?,
    selectedImage: () -> ByteArray?,
    onOpenIconOptions: () -> Unit,

    appName: String,
    appNameIsError: Boolean,
    setAppName: (String) -> Unit,

    packageName: String,
    packageNameState: PackageNameState,
    setPackageName: (String) -> Unit,

    isConfigValid: Boolean,
    onInstall: () -> Unit,
) {
    Scaffold(
        topBar = { PatchOptionsAppBar(isUpdate = isUpdate) },
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedByLastAtBottom(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.patchopts_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            TextDivider(text = stringResource(R.string.patchopts_divider_basic))

            IconPatchOption(
                icon = painterResource(R.drawable.ic_app_shortcut),
                name = stringResource(R.string.patchopts_icon_title),
                description = stringResource(R.string.patchopts_icon_desc),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember(::MutableInteractionSource),
                        onClick = onOpenIconOptions,
                        indication = null,
                        role = Role.Switch,
                    )
                    .padding(bottom = 8.dp),
            ) {
                val drawable = when {
                    selectedColor != null -> discordIconDrawable(
                        backgroundColor = selectedColor,
                        oldLogo = oldLogo,
                        size = 34.dp,
                    )

                    selectedImage() != null -> customIconDrawable(
                        foregroundIcon = selectedImage()!!,
                    )

                    else -> discordIconDrawable(
                        backgroundColor = Color.Black,
                        oldLogo = false,
                        size = 34.dp,
                    )
                }

                Drawable(
                    drawable = drawable,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 14.dp)
                        .size(34.dp)
                        .clip(CircleShape),
                )
            }

            val appNameIsDefault by remember {
                derivedStateOf {
                    appName == PatchOptions.Default.appName
                }
            }
            TextPatchOption(
                name = stringResource(R.string.patchopts_appname_title),
                description = stringResource(R.string.patchopts_appname_desc),
                value = appName,
                valueIsError = appNameIsError,
                valueIsDefault = appNameIsDefault,
                onValueChange = setAppName,
                onValueReset = { setAppName(PatchOptions.Default.appName) },
            )

            if (!isUpdate) {
                val packageNameIsDefault by remember {
                    derivedStateOf {
                        packageName == PatchOptions.Default.packageName
                    }
                }
                TextPatchOption(
                    name = stringResource(R.string.patchopts_pkgname_title),
                    description = stringResource(R.string.patchopts_pkgname_desc),
                    value = packageName,
                    valueIsError = packageNameState == PackageNameState.Invalid,
                    valueIsDefault = packageNameIsDefault,
                    onValueChange = setPackageName,
                    onValueReset = { setPackageName(PatchOptions.Default.packageName) },
                ) {
                    PackageNameStateLabel(
                        state = packageNameState,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            if (isDevMode) {
                TextDivider(
                    text = stringResource(R.string.patchopts_divider_advanced),
                    modifier = Modifier.padding(top = 12.dp),
                )

                SwitchPatchOption(
                    icon = painterResource(R.drawable.ic_bug),
                    name = stringResource(R.string.patchopts_debuggable_title),
                    description = stringResource(R.string.patchopts_debuggable_desc),
                    value = debuggable,
                    onValueChange = setDebuggable,
                )
            }

            Spacer(Modifier.weight(1f))

            FilledTonalButton(
                enabled = isConfigValid,
                onClick = onInstall,
                colors = ButtonDefaults.filledTonalButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .align(Alignment.End),
            ) {
                Text(stringResource(R.string.action_install))
            }
        }
    }
}
