/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import com.aurora.store.R
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ScrollHint(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    autoHideOnScroll: Boolean = true,
    enableBounce: Boolean = true,
    onClickScrollOffset: Float = 300f
) {
    val coroutineScope = rememberCoroutineScope()

    val canScrollForward by remember {
        derivedStateOf { listState.canScrollForward }
    }

    var userScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolled = true
        }
    }

    val visible = canScrollForward && (!autoHideOnScroll || !userScrolled)

    val offsetY by if (enableBounce && visible) {
        val transition = rememberInfiniteTransition(label = "scrollHintBounce")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "offsetY"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                    dimensionResource(R.dimen.scroll_hint_padding)
            ),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut()
    ) {
        Box(
            contentAlignment = Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollBy(onClickScrollOffset)
                    }
                },
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .alpha(0.92f),
                shapes = IconButtonDefaults.shapes(
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
                ),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = stringResource(R.string.action_scroll_down)
                )
            }
        }
    }
}
