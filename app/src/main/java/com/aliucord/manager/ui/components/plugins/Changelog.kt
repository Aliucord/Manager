package com.aliucord.manager.ui.components.plugins

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aliucord.manager.BuildConfig
import com.aliucord.manager.R
import com.aliucord.manager.utils.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL
import java.util.regex.Pattern

private val hyperLinkPattern = Pattern.compile("\\[(.+?)]\\((.+?\\))")

@Suppress("RegExpRedundantEscape") // It is very much not redundant and causes a crash lol
private val headerStylePattern = Pattern.compile("\\{(improved|added|fixed)( marginTop)?\\}")

@SuppressLint("ComposableNaming") // Can't use MaterialTheme without Composable, but this is not a component
@Composable
private fun AnnotatedString.Builder.hyperlink(content: String) {
    var idx = 0

    with(hyperLinkPattern.matcher(content)) {
        while (find()) {
            val start = start()
            val end = end()
            val title = group(1)!!
            val url = group(2)!!

            append(content.substring(idx, start))
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                pushStringAnnotation(title, url)
                append(title)
                pop()
            }
            idx = end
        }
    }

    if (idx < content.length) append(content.substring(idx))
}

@Composable
fun Changelog(plugin: Plugin, onDismiss: () -> Unit) {
    val imageBitmap = remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    if (imageBitmap.value == null && plugin.manifest.changelogMedia != null) {
        LaunchedEffect(null) {
            launch(Dispatchers.IO) {
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    imageBitmap.value = URL(plugin.manifest.changelogMedia).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }.asImageBitmap()
                } catch (err: Throwable) {
                    Log.e(
                        BuildConfig.TAG,
                        "Failed to load changelogMedia ${plugin.manifest.changelogMedia}",
                        err
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxHeight(0.9f)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(plugin.manifest.name, style = MaterialTheme.typography.headlineLarge)
        Divider(color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))

        imageBitmap.value?.let {
            Image(it, "", modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxHeight(0.92f)
        ) {
            items(plugin.manifest.changelog!!.lines()) {
                var line = it.trim()
                if (line.isNotEmpty()) {
                    when (line[0]) {
                        '#' -> {
                            do {
                                line = line.substring(1)
                            } while (line.startsWith("#"))

                            Text(
                                line.trimStart(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                            )
                        }
                        '*' -> {
                            LinkText(
                                buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("● ")
                                    }

                                    hyperlink(line.substring(1))
                                },
                                Modifier.padding(bottom = 2.dp)
                            )
                        }
                        else -> {
                            when {
                                line.endsWith("marginTop}") -> {
                                    val color = MaterialTheme.colorScheme.onSurface
                                    Text(
                                        line,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                    )
                                }
                                line.all { c -> c == '=' } -> {} // Discord ignores =======
                                else -> {
                                    LinkText(
                                        buildAnnotatedString {
                                            hyperlink(line)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End).height(IntrinsicSize.Min)
        ) {
            Text(stringResource(R.string.close))
        }
    }

}

@Composable
private fun LinkText(annotatedString: AnnotatedString, modifier: Modifier = Modifier) {
    val urlHandler = LocalUriHandler.current

    ClickableText(
        text = annotatedString,
        style = TextStyle(color = LocalContentColor.current),
        onClick = { offset ->
            annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let {
                urlHandler.openUri(it.item)
            }
        },
        modifier = modifier
    )
}