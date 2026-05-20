/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REMOVE_ANIM_DURATION_MS = 300L

/**
 * Wraps [content] in an AnimatedVisibility container that shrinks vertically and fades out
 * before invoking [onRemove]. The content lambda receives a trigger callback that, when
 * invoked, plays the exit animation and then calls [onRemove].
 */
@Composable
fun RemovableListItem(
    onRemove: () -> Unit,
    content: @Composable (triggerRemove: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(visible = visible, exit = shrinkVertically() + fadeOut()) {
        content {
            scope.launch {
                visible = false
                delay(REMOVE_ANIM_DURATION_MS)
                onRemove()
            }
        }
    }
}
