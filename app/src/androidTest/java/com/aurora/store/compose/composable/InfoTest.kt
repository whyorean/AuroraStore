/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import com.aurora.store.IsolatedTest
import com.aurora.store.R
import org.junit.Test

class InfoTest: IsolatedTest() {

    @Test
    fun testInfoWithoutClickHandling() {
        setContent {
            Info(title = AnnotatedString(text = stringResource(R.string.app_name)))
        }

        composeTestRule.onNodeWithText("Aurora Store")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsNotEnabled()
    }
}
