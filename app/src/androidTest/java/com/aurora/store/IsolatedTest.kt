/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.aurora.store.compose.theme.AuroraTheme
import org.junit.Rule

/**
 * Class that provides helper methods to test isolated composable
 */
abstract class IsolatedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Sets given composable as content with default theme
     */
    fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            AuroraTheme(content = content)
        }
    }
}
