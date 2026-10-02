/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.IsolatedTest
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * On TV a cluster header is skipped by the D-pad so it cannot swallow the DOWN/UP press meant
 * for the next/previous row, but it must stay usable for anyone driving this with a pointer.
 */
class StreamCarouselTest : IsolatedTest() {

    private fun bundle(): StreamBundle = StreamBundle(
        id = 1,
        streamClusters = mapOf(
            1 to StreamCluster(
                id = 1,
                clusterTitle = "Top picks",
                clusterBrowseUrl = "https://play.google.com/store/apps/collection/cluster1",
                clusterAppList = List(2) { index ->
                    App(id = index, packageName = "com.example.app$index")
                }
            ),
            2 to StreamCluster(
                id = 2,
                clusterTitle = "Recommended",
                clusterBrowseUrl = "https://play.google.com/store/apps/collection/cluster2",
                clusterAppList = List(2) { index ->
                    App(id = 10 + index, packageName = "com.example.other$index")
                }
            )
        )
    )

    @Test
    fun testClusterHeaderStaysClickableOnTv() {
        val clicked = mutableListOf<StreamCluster>()

        setContent {
            CompositionLocalProvider(LocalUI provides UI.TV) {
                StreamCarousel(
                    modifier = Modifier.size(1000.dp),
                    streamBundle = bundle(),
                    onHeaderClick = { cluster -> clicked.add(cluster) }
                )
            }
        }

        composeTestRule.onNodeWithText("Top picks")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(listOf(1), clicked.map { it.id })
    }

    @Test
    fun testClusterHeaderStaysClickableOnDefaultUi() {
        val clicked = mutableListOf<StreamCluster>()

        setContent {
            StreamCarousel(
                modifier = Modifier.size(1000.dp),
                streamBundle = bundle(),
                onHeaderClick = { cluster -> clicked.add(cluster) }
            )
        }

        composeTestRule.onNodeWithText("Top picks")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(listOf(1), clicked.map { it.id })
    }
}
