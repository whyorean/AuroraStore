/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import com.aurora.store.IsolatedTest
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.data.model.PermissionType
import org.junit.Test

class PermissionListItemTest : IsolatedTest() {

    private val permission: Permission
        @Composable
        get() = Permission(
            PermissionType.STORAGE_MANAGER,
            stringResource(R.string.onboarding_permission_esm),
            stringResource(R.string.onboarding_permission_esa_desc),
        )

    @Test
    fun testPermissionNotGranted() {
        setContent {
            PermissionListItem(permission = permission.copy(isGranted = false))
        }

        composeTestRule.onNodeWithText("Grant")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun testPermissionGranted() {
        setContent {
            PermissionListItem(permission = permission.copy(isGranted = true))
        }

        composeTestRule.onNodeWithText("Granted")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}
