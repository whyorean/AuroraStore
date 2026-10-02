/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aurora.gplayapi.data.models.App
import com.aurora.store.IsolatedTest
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI
import com.aurora.store.compose.theme.AuroraTheme
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The focus highlight is drawn on top of the item's own click target, so the clickable area
 * itself must keep working when it is applied.
 */
class AppListItemTest : IsolatedTest() {

    private val app = App(id = 1, packageName = "com.example.app", displayName = "Example")

    @Test
    fun testClickIsForwardedOnTv() {
        var clicks = 0

        composeTestRule.setContent {
            AuroraTheme {
                CompositionLocalProvider(LocalUI provides UI.TV) {
                    AppListItem(
                        modifier = Modifier.width(100.dp),
                        app = app,
                        onClick = { clicks++ }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Example")
            .assertHasClickAction()
            .performClick()

        assertEquals(1, clicks)
    }
}
