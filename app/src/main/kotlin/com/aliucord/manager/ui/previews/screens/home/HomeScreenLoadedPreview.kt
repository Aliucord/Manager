package com.aliucord.manager.ui.previews.screens.home

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.tooling.preview.*
import com.aliucord.manager.ui.components.ManagerTheme
import com.aliucord.manager.ui.screens.home.*
import com.aliucord.manager.ui.screens.home.components.HomeAppBar
import com.aliucord.manager.ui.util.DiscordVersion
import io.ktor.util.decodeBase64Bytes
import kotlinx.collections.immutable.persistentListOf

// This preview has animations that cannot be properly viewed from an IDE preview

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun HomeScreenLoadedPreview(
    @PreviewParameter(HomeScreenParametersProvider::class)
    state: InstallsState.Fetched,
) {
    ManagerTheme {
        Scaffold(
            topBar = { HomeAppBar() },
        ) { padding ->
            HomeScreenLoadedContent(
                state = state,
                padding = padding,
                onClickInstall = {},
                onUpdate = {},
                onOpenApp = {},
                onOpenAppInfo = {},
                onOpenPlugins = {},
            )
        }
    }
}

private class HomeScreenParametersProvider : PreviewParameterProvider<InstallsState.Fetched> {
    private val stableVersion = DiscordVersion.Existing(DiscordVersion.Type.STABLE, "126.21", 126021)
    private val aliucordIconBytes =
        "/9j/4AAQSkZJRgABAQAAAQABAAD/4gHYSUNDX1BST0ZJTEUAAQEAAAHIAAAAAAQwAABtbnRyUkdCIFhZWiAH4AABAAEAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAAABRyVFJDAAABZAAAAChnVFJDAAABZAAAAChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAAAABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAAAADTLXBhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAAACAAAAAcAEcAbwBvAGcAbABlACAASQBuAGMALgAgADIAMAAxADb/2wBDAKBueIx4ZKCMgoy0qqC+8P//8Nzc8P//////////////////////////////////////////////////////////2wBDAaq0tPDS8P//////////////////////////////////////////////////////////////////////////////wAARCAC9AL0DASIAAhEBAxEB/8QAFwABAQEBAAAAAAAAAAAAAAAAAAIDAf/EACIQAQEAAgEFAAIDAAAAAAAAAAABAhEhAxIxQVETMiJhcf/EABcBAQEBAQAAAAAAAAAAAAAAAAABAgP/xAAZEQEBAQEBAQAAAAAAAAAAAAAAARECEjH/2gAMAwEAAhEDEQA/AMsce7/Gkkngk1NOoxboAIAAAAAAAAAAAAAAAAAAm4ys7NXVbJyx2LKoAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHZLfRcbJug4AABJugDtxs9OAAAAAAAAAAAAAA7jN0HccN81cxk9OityBZuaAVP459OyKBMifxwxw1dqAyBrYCpuEvhFmq1TnNwZsZgIyAAAAAAAANcJqM8ZutRrkAVoAAAAAAAAABnnNVLTObjNGKACAAAAAAL6c9rcxmsY6rcALdTYrlyk8kzlZeRGPTYThdzSlbC2TyMsruiW4vvimK+nedIkqwFaGN4rZnnP5DPSQEZAAAAAJ5BtPACug5n+tdAYi8sL6cmFqOeO9P2sk1NCtwYtk5Yb5gljNWH7Odt+NMce2IkjoCtiOp6WjqeIJfiAEYAAAACeYANgllnAroA5lbJxAdEzOe+FblDQAAAAcuUntyZW3iCaoAUT1PEUjOy8CX4gBGAAAAAACWzw0mf1mBLjYZS2eFTqfVa9O9TWv7Zu27u3EZtd7r9O/L64Brvdfrm79AHZ55asVY56mhZWjlykRc7Ui3pWWdqQGQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHMbuOsZbLw0xy2LYoAQAAAAAAAAAAAAAAAAAARnlzqGWdnEQNSP/2Q=="
            .decodeBase64Bytes()
    private val aliucordIcon = BitmapFactory
        .decodeByteArray(aliucordIconBytes, 0, aliucordIconBytes.size)
        .asImageBitmap()
        .let(::BitmapPainter)

    override val values = sequenceOf(
        InstallsState.Fetched(
            persistentListOf(
                InstallData(
                    name = "Aliucord",
                    packageName = "com.aliucord",
                    version = stableVersion,
                    icon = aliucordIcon,
                    isUpToDate = true,
                )
            )
        ),
        InstallsState.Fetched(
            persistentListOf(
                InstallData(
                    name = "Discord",
                    packageName = "com.discord",
                    version = stableVersion,
                    icon = aliucordIcon,
                    isUpToDate = false,
                )
            )
        ),
    )
}
