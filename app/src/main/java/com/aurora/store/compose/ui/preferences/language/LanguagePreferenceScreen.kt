/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.viewmodel.preferences.LanguageViewModel
import java.util.Locale

@Composable
fun LanguagePreferenceScreen(
    viewModel: LanguageViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val available by viewModel.available.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val primaryChanged by viewModel.primaryChanged.collectAsStateWithLifecycle()

    val resources = LocalResources.current
    val focusManager = LocalFocusManager.current
    val snackBarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(primaryChanged) {
        if (!primaryChanged) return@LaunchedEffect
        val result = snackBarHostState.showSnackbar(
            message = resources.getString(R.string.force_restart_snack),
            actionLabel = resources.getString(R.string.action_restart),
            duration = SnackbarDuration.Indefinite
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.applyAppLanguage()
            onNavigateTo(Destination.Splash())
        }
    }

    fun onDrag(delta: Float) {
        val key = draggedKey ?: return
        dragOffset += delta
        val items = listState.layoutInfo.visibleItemsInfo
        val dragged = items.firstOrNull { it.key == key } ?: return
        val center = dragged.offset + dragOffset + dragged.size / 2
        val target = items.firstOrNull { item ->
            item.key != key &&
                (item.key as? String)?.startsWith(KEY_PREFIX) == true &&
                center.toInt() in item.offset until item.offset + item.size
        } ?: return
        val from = languages.indexOfFirst { keyOf(it) == key }
        val to = languages.indexOfFirst { keyOf(it) == target.key }
        if (from < 0 || to < 0) return
        dragOffset += dragged.offset - target.offset
        viewModel.move(from, to)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = { TopAppBar(title = stringResource(R.string.title_languages)) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = listState
        ) {
            item(key = "search") {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.spacing_medium)),
                    value = query,
                    onValueChange = viewModel::search,
                    placeholder = { Text(stringResource(R.string.language_search_hint)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_search),
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.search("") }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_cancel),
                                    contentDescription = stringResource(R.string.action_clear)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            if (query.isBlank()) {
                item(key = "header-selected") {
                    SectionHeader(title = stringResource(R.string.language_selected))
                }
                itemsIndexed(items = languages, key = { _, locale ->
                    keyOf(locale)
                }) { index, locale ->
                    val key = keyOf(locale)
                    val isDragged = draggedKey == key
                    ListItem(
                        modifier = Modifier
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                            .pointerInput(key) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedKey = key
                                        dragOffset = 0f
                                    },
                                    onDragEnd = { draggedKey = null },
                                    onDragCancel = { draggedKey = null },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        onDrag(amount.y)
                                    }
                                )
                            },
                        colors = if (isDragged) {
                            ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        } else {
                            ListItemDefaults.colors()
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_drag_indicator),
                                contentDescription = null
                            )
                        },
                        headlineContent = { Text(locale.displayName) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (index == 0) {
                                        R.string.language_primary
                                    } else {
                                        R.string.language_secondary
                                    }
                                )
                            )
                        },
                        trailingContent = if (languages.size > 1) {
                            {
                                IconButton(onClick = { viewModel.remove(locale) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_cancel),
                                        contentDescription = stringResource(
                                            R.string.language_remove
                                        )
                                    )
                                }
                            }
                        } else {
                            null
                        }
                    )
                }
            }

            item(key = "header-available") {
                SectionHeader(title = stringResource(R.string.language_add))
            }
            if (available.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium)),
                        text = stringResource(R.string.language_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(items = available, key = { "add-${it.toLanguageTag()}" }) { locale ->
                ListItem(
                    modifier = Modifier.clickable {
                        viewModel.add(locale)
                        viewModel.search("")
                        focusManager.clearFocus()
                    },
                    headlineContent = { Text(locale.displayName) },
                    supportingContent = {
                        val native = locale.getDisplayName(locale)
                        if (native != locale.displayName) Text(native)
                    }
                )
            }
        }
    }
}

private const val KEY_PREFIX = "language-"

private fun keyOf(locale: Locale) = KEY_PREFIX + locale.toLanguageTag()
