/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.compose.preview.PreviewTemplate

@Composable
fun AppsScreen() {
    ScreenContent()
}

@Composable
private fun ScreenContent() {
}

@Preview(showBackground = true)
@Composable
private fun AppsScreenPreview() {
    PreviewTemplate {
        ScreenContent()
    }
}
