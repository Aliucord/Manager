package com.aliucord.manager.ui.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.widget.ImageView
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
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.aliucord.manager.R

@Composable
fun discordIconDrawable(
    backgroundColor: Color,
    size: Dp = 24.dp,
): Drawable {
    val density = LocalDensity.current
    val context = LocalContext.current
    val size = with(density) { size.roundToPx() }

    val bitmap = remember(size) { createBitmap(size, size) }
    val drawable = remember(context) {
        InsetDrawable(
            /* drawable = */ ContextCompat.getDrawable(context, R.drawable.ic_discord)!!,
            /* inset = */ (size * .17f).toInt(),
        )
    }

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
        RoundedBitmapDrawableFactory.create(context.resources, bitmap).apply {
            isCircular = true
            setAntiAlias(true)
        }
    }
}

@Composable
fun ColoredDiscordAppIcon(
    drawable: Drawable,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setImageDrawable(drawable)
            }
        },
        modifier = modifier,
    )
}
