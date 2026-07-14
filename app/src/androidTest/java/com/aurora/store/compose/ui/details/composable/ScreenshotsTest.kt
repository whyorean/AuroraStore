/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performSemanticsAction
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.IsolatedTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenshotsTest : IsolatedTest() {

    private val artwork = Artwork(url = "https://example.com/screenshot.png")
    private val duplicateArtwork = Artwork(url = "https://example.com/screenshot.png")
    private val otherArtwork = Artwork(url = "https://example.com/other.png")

    @Test
    fun testDuplicateScreenshotsAreDisplayedOnce() {
        setContent {
            Screenshots(screenshots = listOf(artwork, duplicateArtwork, otherArtwork))
        }

        composeTestRule.onAllNodes(hasClickAction())
            .assertCountEquals(2)
    }

    @Test
    fun testNavigationIndicesMatchDedupedList() {
        val clickedIndices = mutableSetOf<Int>()
        setContent {
            Screenshots(
                screenshots = listOf(artwork, duplicateArtwork, otherArtwork),
                onNavigateToScreenshot = { index -> clickedIndices.add(index) }
            )
        }

        val screenshots = composeTestRule.onAllNodes(hasClickAction())
            .assertCountEquals(2)
        screenshots[0].performSemanticsAction(SemanticsActions.OnClick)
        screenshots[1].performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(setOf(0, 1), clickedIndices)
    }
}
