package com.aurora.store.data.providers

import androidx.test.ext.junit.runners.AndroidJUnit4
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
class SpoofProviderTest {

    @get:Rule
    var hiltAndroidRule = HiltAndroidRule(this)

    @Inject
    lateinit var spoofProvider: SpoofProvider

    @Before
    fun setup() {
        hiltAndroidRule.inject()
    }

    @After
    fun tearDown() {
        spoofProvider.removeSpoofLocale()
        spoofProvider.removeSpoofDeviceProperties()
    }

    @Test
    fun testSpoofingDeviceLocale() {
        assertThat(spoofProvider.isLocaleSpoofEnabled()).isFalse()

        spoofProvider.setSpoofLocale(Locale.JAPAN)
        assertThat(spoofProvider.isLocaleSpoofEnabled()).isTrue()
        assertThat(spoofProvider.getSpoofLocale() == Locale.JAPAN).isTrue()
    }

    @Test
    fun testSpoofingDeviceProperties() {
        assertThat(spoofProvider.isDeviceSpoofEnabled()).isFalse()

        val properties = Properties().apply {
            setProperty("UserReadableName", "Test")
        }
        spoofProvider.setSpoofDeviceProperties(properties)
        assertThat(spoofProvider.isDeviceSpoofEnabled()).isTrue()
        assertThat(spoofProvider.getSpoofDeviceProperties() == properties).isTrue()
    }
}
