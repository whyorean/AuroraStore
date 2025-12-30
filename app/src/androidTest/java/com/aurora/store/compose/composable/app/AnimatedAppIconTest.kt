/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable.app

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.onNodeWithTag
import com.aurora.store.IsolatedTest
import org.junit.Test

class AnimatedAppIconTest : IsolatedTest() {

    @Test
    fun testAnimatedAppIconNoProgress() {
        setContent {
            AnimatedAppIcon(
                iconUrl = "https://example.com/icon.png",
                inProgress = false
            )
        }

        composeTestRule.onNodeWithTag("progressIndicator")
            .assertIsNotDisplayed()
    }

    @Test
    fun testAnimatedAppIconProgressAt0() {
        setContent {
            AnimatedAppIcon(
                iconUrl = "https://example.com/icon.png",
                progress = 0F,
                inProgress = true
            )
        }

        composeTestRule.onNodeWithTag("progressIndicator")
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun testAnimatedAppIconProgressAt50() {
        setContent {
            AnimatedAppIcon(
                iconUrl = "https://example.com/icon.png",
                progress = 50F,
                inProgress = true
            )
        }

        composeTestRule.onNodeWithTag("progressIndicator")
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5F, 0.00F..1.00F))
    }
}
