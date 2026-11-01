/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aurora.gplayapi.device.DeviceProfile
import com.aurora.gplayapi.device.DeviceRegistry
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import java.util.Properties
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthProviderTest {

    @get:Rule
    var hiltAndroidRule = HiltAndroidRule(this)

    @Inject
    lateinit var authProvider: AuthProvider

    @Inject
    lateinit var spoofProvider: SpoofProvider

    private lateinit var devices: List<Properties>

    @Before
    fun setup() {
        hiltAndroidRule.inject()
        spoofProvider.removeSpoofLocale()
        spoofProvider.removeSpoofDeviceProperties()
        devices = spoofProvider.availableSpoofDeviceProperties
        assertThat(devices.size).isAtLeast(2)
    }

    @After
    fun tearDown() {
        spoofProvider.removeSpoofLocale()
        spoofProvider.removeSpoofDeviceProperties()
    }

    private fun deviceFor(properties: Properties): DeviceProfile =
        DeviceRegistry.parse(properties.getProperty("Build.PRODUCT", "test"), properties)

    @Test
    fun sessionBuiltForTheActiveSpoofIsAccepted() {
        spoofProvider.setSpoofDeviceProperties(devices.first())

        assertThat(
            authProvider.matchesActiveSpoof(deviceFor(devices.first()), Locale.getDefault())
        ).isTrue()
    }

    @Test
    fun sessionBuiltForAnotherDeviceIsRejected() {
        spoofProvider.setSpoofDeviceProperties(devices.first())

        assertThat(
            authProvider.matchesActiveSpoof(deviceFor(devices.last()), Locale.getDefault())
        ).isFalse()
    }

    @Test
    fun sessionBuiltBeforeTheSpoofWasEnabledIsRejected() {
        val native = deviceFor(spoofProvider.deviceProperties)
        assertThat(authProvider.matchesActiveSpoof(native, Locale.getDefault())).isTrue()

        spoofProvider.setSpoofDeviceProperties(devices.first())

        assertThat(authProvider.matchesActiveSpoof(native, Locale.getDefault())).isFalse()
    }

    @Test
    fun sessionBuiltForAnotherLocaleIsRejected() {
        spoofProvider.setSpoofDeviceProperties(devices.first())
        spoofProvider.setSpoofLocale(Locale.JAPAN)

        assertThat(
            authProvider.matchesActiveSpoof(deviceFor(devices.first()), Locale.getDefault())
        ).isFalse()
        assertThat(
            authProvider.matchesActiveSpoof(deviceFor(devices.first()), Locale.JAPAN)
        ).isTrue()
    }

    @Test
    fun sessionWithoutDeviceInfoIsRejected() {
        assertThat(authProvider.matchesActiveSpoof(null, Locale.getDefault())).isFalse()
    }
}
