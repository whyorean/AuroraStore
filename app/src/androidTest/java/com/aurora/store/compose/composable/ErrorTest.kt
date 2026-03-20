/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import com.aurora.store.IsolatedTest
import com.aurora.store.R
import org.junit.Test

class ErrorTest : IsolatedTest() {

    @Test
    fun testErrorWithoutActionHandling() {
        setContent {
            Error(
                painter = painterResource(R.drawable.ic_apps_outage),
                message = "An error occurred!",
                actionMessage = "Retry"
            )
        }

        composeTestRule.onNodeWithText("Retry")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsNotEnabled()
    }
}
