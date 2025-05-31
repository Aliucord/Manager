package com.aliucord.manager.ui.components

import android.graphics.*
import android.graphics.drawable.*
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.patchopts.PatchOptions.IconReplacement

// Sizing information is obtained from here: https://medium.com/google-design/designing-adaptive-icons-515af294c783

@Composable
fun discordIconDrawable(
    backgroundColor: Color,
    oldLogo: Boolean = false,
    size: Dp = 24.dp,
): Drawable {
    val density = LocalDensity.current
    val context = LocalContext.current
    val size = with(density) { size.roundToPx() }

    val drawable = remember(context, oldLogo) {
        val drawableId = when (oldLogo) {
            false -> R.drawable.ic_discord
            true -> R.drawable.ic_discord_old
        }

        InsetDrawable(
            /* drawable = */ ContextCompat.getDrawable(context, drawableId)!!,
            /* inset = */ (size * .17f).toInt(),
        ).apply {
            setTint(Color.White.toArgb())
        }
    }

    val bitmap = remember(size) { createBitmap(size, size) }
    DisposableEffect(size) {
        onDispose {
            bitmap.recycle()
        }
    }

    DisposableEffect(backgroundColor, bitmap, drawable) {
        bitmap.applyCanvas {
            val paint = Paint().apply {
                style = Paint.Style.FILL
                setColor(backgroundColor.toArgb())
            }
            drawRect(Rect(0, 0, width, height), paint)

            drawable.setBounds(0, 0, width, height)
            drawable.draw(this)
        }

        onDispose {}
    }

    return remember(context, bitmap) {
        bitmap.toDrawable(context.resources)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun customIconDrawable(
    foregroundIcon: ByteArray,
    backgroundColor: Color = IconReplacement.BlurpleColor,
): Drawable {
    val context = LocalContext.current

    return remember(context, foregroundIcon) {
        val foregroundIconBitmap = BitmapFactory.decodeByteArray(foregroundIcon, 0, foregroundIcon.size)

        AdaptiveIconDrawable(
            /* backgroundDrawable = */ backgroundColor.toArgb().toDrawable(),
            /* foregroundDrawable = */ foregroundIconBitmap.toDrawable(context.resources),
        )
    }
}

@Composable
fun Drawable(
    drawable: Drawable,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setImageDrawable(drawable)
            }
        },
        update = { imageView ->
            imageView.setImageDrawable(drawable)
        },
        modifier = modifier,
    )
}
