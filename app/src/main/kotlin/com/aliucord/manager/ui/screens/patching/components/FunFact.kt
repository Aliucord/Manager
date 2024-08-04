package com.aliucord.manager.ui.screens.patching.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliucord.manager.R
import com.aliucord.manager.ui.screens.patching.PatchingScreenState
import com.aliucord.manager.ui.screens.patching.VERTICAL_PADDING

@Composable
fun FunFact(
    text: String,
    state: PatchingScreenState,
) {
    AnimatedVisibility(
        visible = state !is PatchingScreenState.Failed,
        enter = fadeIn() + slideInVertically { it * 2 },
        exit = fadeOut() + slideOutVertically { it * 2 },
        label = "fun fact visibility"
    ) {
        AnimatedContent(
            targetState = text,
            label = "fun fact change",
            transitionSpec = {
                val inSpec = fadeIn(tween(220, delayMillis = 90)) + slideInHorizontally { it * -2 }
                val outSpec = fadeOut(tween(90)) + slideOutHorizontally { it * 2 }
                inSpec togetherWith outSpec
            }
        ) { text ->
            Text(
                text = stringResource(R.string.fun_fact_prefix, text),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = VERTICAL_PADDING, bottom = 25.dp, start = VERTICAL_PADDING, end = VERTICAL_PADDING)
                    .fillMaxWidth()
                    .alpha(.6f)
            )
        }
    }
}
