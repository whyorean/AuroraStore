/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.spoof

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.DeviceListItem
import com.aurora.store.compose.composable.TextDividerComposable
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.viewmodel.spoof.SpoofViewModel
import java.util.Properties
import kotlin.random.Random

@Composable
fun DevicePage(
    onRequestNavigateToSplash: () -> Unit,
    viewModel: SpoofViewModel = hiltViewModel(),
) {
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val currentDevice by viewModel.currentDevice.collectAsStateWithLifecycle()

    PageContent(
        devices = availableDevices,
        defaultDevice = viewModel.defaultProperties,
        isDeviceSelected = { device -> device == currentDevice },
        onDeviceSelected = { properties ->
            viewModel.onDeviceSelected(properties)
            onRequestNavigateToSplash()
        }
    )
}

@Composable
private fun PageContent(
    defaultDevice: Properties = Properties(),
    devices: List<Properties> = emptyList(),
    isDeviceSelected: (properties: Properties) -> Boolean = { false },
    onDeviceSelected: (properties: Properties) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
    ) {
        stickyHeader {
            Surface(modifier = Modifier.fillMaxWidth()) {
                TextDividerComposable(
                    title = stringResource(R.string.default_spoof)
                )
            }
        }

        item {
            DeviceListItem(
                userReadableName = defaultDevice.getProperty("UserReadableName"),
                manufacturer = defaultDevice.getProperty("Build.MANUFACTURER"),
                androidVersionSdk = defaultDevice.getProperty("Build.VERSION.SDK_INT"),
                platforms = defaultDevice.getProperty("Platforms"),
                isChecked = isDeviceSelected(defaultDevice),
                onClick = { onDeviceSelected(defaultDevice) }
            )
        }

        stickyHeader {
            Surface(modifier = Modifier.fillMaxWidth()) {
                TextDividerComposable(
                    title = stringResource(R.string.available_spoof)
                )
            }
        }

        items(items = devices, key = { device -> device.getProperty("Build.PRODUCT") }) { device ->
            DeviceListItem(
                userReadableName = device.getProperty("UserReadableName"),
                manufacturer = device.getProperty("Build.MANUFACTURER"),
                androidVersionSdk = device.getProperty("Build.VERSION.SDK_INT"),
                platforms = device.getProperty("Platforms"),
                isChecked = isDeviceSelected(device),
                onClick = { onDeviceSelected(device) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DevicePagePreview() {
    fun getDevice(): Properties {
        return Properties().apply {
            setProperty("UserReadableName", "Google Pixel 9a")
            setProperty("Build.VERSION.SDK_INT", "35")
            setProperty("Build.MANUFACTURER", "Google")
            setProperty("Platforms", "arm64-v8a")
            setProperty("Build.PRODUCT", Random.nextInt().toString())
        }
    }

    PreviewTemplate {
        val defaultDevice = getDevice()
        PageContent(
            defaultDevice = defaultDevice,
            devices = List(10) { getDevice() },
            isDeviceSelected = { device -> defaultDevice == device }
        )
    }
}
